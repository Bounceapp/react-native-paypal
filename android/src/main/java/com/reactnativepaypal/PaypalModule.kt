package com.reactnativepaypal

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.paypal.PayPalAccountNonce
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalVaultRequest
import expo.modules.kotlin.exception.Exceptions
import expo.modules.kotlin.functions.Coroutine
import expo.modules.kotlin.functions.Queues
import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record
import kotlinx.coroutines.CompletableDeferred

class RequestBillingAgreementOptions : Record {
  @Field val clientToken: String? = null
  @Field val appLinkReturnUrl: String? = null
  @Field val billingAgreementDescription: String? = null
  @Field val hasUserLocationConsent: Boolean = false
  @Field val merchantAccountID: String? = null
  @Field val displayName: String? = null
  @Field val localeCode: String? = null
  @Field val shippingAddressRequired: Boolean = false
}

// Concurrency rule: all state below is confined to the main thread. The
// function runs on Queues.MAIN and the lifecycle hooks are delivered on main,
// so there are no locks and no @Volatile. Settle-once comes from the function
// returning a value and from CompletableDeferred.complete() being a no-op after
// the first call -- never from the order in which main happens to run things.
class PaypalModule : Module() {
  private val payPalLauncher = PayPalLauncher()

  // Non-null from the moment a request starts until it settles. Set before the
  // first suspension point, so a second call made in the meantime is rejected
  // rather than silently replacing the first.
  private var inFlight: CompletableDeferred<PayPalPaymentAuthResult>? = null

  private val context: Context
    get() = appContext.reactContext ?: throw Exceptions.ReactContextLost()

  override fun definition() = ModuleDefinition {
    Name("Paypal")

    (AsyncFunction("requestBillingAgreement") Coroutine { options: RequestBillingAgreementOptions ->
      requestBillingAgreement(options)
    }).runOnQueue(Queues.MAIN)

    // A singleTask or singleTop activity -- Expo's default -- receives the
    // PayPal return here. React Native's ReactActivity does not call
    // setIntent(), so the activity's own intent is still the launch intent.
    OnNewIntent { intent -> handleReturnToApp(intent) }

    // Covers every other launch mode, and the buyer closing the browser
    // without finishing. For singleTask this runs after OnNewIntent has
    // already consumed the pending request, so it returns early.
    OnActivityEntersForeground {
      appContext.currentActivity?.intent?.let { handleReturnToApp(it) }
    }

    OnDestroy {
      inFlight?.cancel()
      inFlight = null
    }
  }

  private suspend fun requestBillingAgreement(
    options: RequestBillingAgreementOptions
  ): Map<String, Any?> {
    if (inFlight != null) {
      return error(FAILED, "A billing agreement request is already in progress")
    }
    val clientToken = options.clientToken
      ?: return error(FAILED, "You must provide the clientToken")
    // Braintree v5 returns through an Android App Link, and PayPalClient has no
    // constructor without one, so it has to come from the caller.
    val appLinkReturnUrl = options.appLinkReturnUrl
      ?: return error(FAILED, "You must provide the appLinkReturnUrl")
    val billingAgreementDescription = options.billingAgreementDescription
      ?: return error(FAILED, "You must provide the billingAgreementDescription")
    // `as?` is the safe cast: null for a missing activity and for one of the
    // wrong type alike. (`as ComponentActivity?` would throw on the latter.)
    val activity = appContext.currentActivity as? ComponentActivity
      ?: return error(FAILED, "The activity is not available")

    val authResult = CompletableDeferred<PayPalPaymentAuthResult>()
    inFlight = authResult
    // A request left behind by an earlier flow must not be mistaken for this
    // one while it is still being created.
    clearPendingRequest()

    try {
      val payPalClient = PayPalClient(
        context = context,
        authorization = clientToken,
        appLinkReturnUrl = Uri.parse(appLinkReturnUrl),
        // Matches the `${applicationId}.braintree` intent-filter merchants
        // already register. Used when a buyer has turned off "Open supported
        // links" and App Link return is unavailable.
        deepLinkFallbackUrlScheme = "${context.packageName}.braintree"
      )
      val request = PayPalVaultRequest(
        hasUserLocationConsent = options.hasUserLocationConsent,
        billingAgreementDescription = billingAgreementDescription,
        merchantAccountId = options.merchantAccountID,
        displayName = options.displayName,
        localeCode = options.localeCode,
        isShippingAddressRequired = options.shippingAddressRequired
      )

      val readyToLaunch = when (val authRequest = payPalClient.createPaymentAuthRequest(context, request)) {
        is PayPalPaymentAuthRequest.ReadyToLaunch -> authRequest
        is PayPalPaymentAuthRequest.Failure -> return error(FAILED, authRequest.error.message)
      }

      when (val pendingRequest = payPalLauncher.launch(activity, readyToLaunch)) {
        // Persisted only so that a process death during the browser flow can
        // be cleaned up on relaunch. In-process, the result is awaited below.
        is PayPalPendingRequest.Started -> storePendingRequest(pendingRequest.pendingRequestString)
        is PayPalPendingRequest.Failure -> return error(FAILED, pendingRequest.error.message)
      }

      return toResponse(payPalClient, authResult.await())
    } finally {
      if (inFlight === authResult) {
        inFlight = null
      }
    }
  }

  // Maps Braintree's result onto the response shape the JavaScript side has
  // always received.
  private suspend fun toResponse(
    payPalClient: PayPalClient,
    result: PayPalPaymentAuthResult
  ): Map<String, Any?> = when (result) {
    is PayPalPaymentAuthResult.Success -> when (val tokenized = payPalClient.tokenize(result)) {
      is PayPalResult.Success -> payload(tokenized.nonce)
      is PayPalResult.Failure -> error(FAILED, tokenized.error.message)
      is PayPalResult.Cancel -> error(CANCELED, CANCELED_MESSAGE)
    }
    is PayPalPaymentAuthResult.Failure -> error(FAILED, result.error.message)
    // The buyer came back without finishing: closed the browser or pressed
    // back.
    is PayPalPaymentAuthResult.NoResult -> error(CANCELED, CANCELED_MESSAGE)
  }

  private fun handleReturnToApp(intent: Intent) {
    val pendingRequestString = getPendingRequest() ?: return
    // Consumed exactly once, whatever the outcome, so a stale request can never
    // be replayed on a later launch.
    clearPendingRequest()

    // Nothing is waiting after a process death: the JS that made the call is
    // gone, so the request is dropped and the buyer simply starts again.
    val authResult = inFlight ?: return
    authResult.complete(
      payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(pendingRequestString), intent)
    )
  }

  private fun payload(nonce: PayPalAccountNonce): Map<String, Any?> = mapOf(
    "payload" to mapOf(
      "nonce" to nonce.string,
      "details" to mapOf(
        "payerId" to nonce.payerId,
        "email" to nonce.email,
        "firstName" to nonce.firstName,
        "lastName" to nonce.lastName,
        "phone" to nonce.phone
      )
    )
  )

  private fun error(code: String, message: String?): Map<String, Any?> =
    mapOf("error" to mapOf("code" to code, "message" to message))

  private fun sharedPreferences() =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun storePendingRequest(pendingRequestString: String) {
    sharedPreferences().edit().putString(PENDING_REQUEST_KEY, pendingRequestString).apply()
  }

  private fun getPendingRequest(): String? =
    sharedPreferences().getString(PENDING_REQUEST_KEY, null)

  private fun clearPendingRequest() {
    sharedPreferences().edit().remove(PENDING_REQUEST_KEY).apply()
  }

  companion object {
    private const val FAILED = "Failed"
    private const val CANCELED = "Canceled"
    private const val CANCELED_MESSAGE = "User cancelled billing agreement request"

    private const val PREFS_NAME = "com.reactnativepaypal.PENDING_REQUEST"
    private const val PENDING_REQUEST_KEY = "pending_request"
  }
}
