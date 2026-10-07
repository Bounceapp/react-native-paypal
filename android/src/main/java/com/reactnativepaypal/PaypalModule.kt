package com.reactnativepaypal

import android.content.Context
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

// Concurrency rule: all state is confined to the main thread. The function
// runs on Queues.MAIN and the lifecycle hooks are delivered on main, so there
// are no locks and no @Volatile. Settle-once comes from the function returning
// a value and from CompletableDeferred.complete() being a no-op after the first
// call -- never from the order in which main happens to run things.
//
// Expected failures resolve with `{ error }` rather than rejecting, so callers
// handle every outcome the same way.
class PaypalModule : Module() {
  private val payPalLauncher = PayPalLauncher()

  // Only built once the React context exists: the function checks for it
  // first, and the lifecycle hooks only run while there is an activity.
  private val flow by lazy {
    val context = appContext.reactContext ?: throw Exceptions.ReactContextLost()
    PayPalFlow(
      PendingRequestStore(context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))
    ) { pendingRequestString, intent ->
      payPalLauncher.handleReturnToApp(PayPalPendingRequest.Started(pendingRequestString), intent)
    }
  }

  override fun definition() = ModuleDefinition {
    Name("Paypal")

    (AsyncFunction("requestBillingAgreement") Coroutine { options: RequestBillingAgreementOptions ->
      requestBillingAgreement(options)
    }).runOnQueue(Queues.MAIN)

    // A singleTask or singleTop activity -- Expo's default -- receives the
    // PayPal return here. React Native's ReactActivity does not call
    // setIntent(), so unless the app's activity does, the activity's own
    // intent is still the launch intent.
    OnNewIntent { intent -> flow.onNewIntent(intent) }

    // Covers every other launch mode, and the buyer closing the browser
    // without finishing. For singleTask this runs after OnNewIntent has
    // already consumed the pending request, so it returns early.
    OnActivityEntersForeground {
      appContext.currentActivity?.intent?.let { flow.onForeground(it) }
    }
  }

  private suspend fun requestBillingAgreement(
    options: RequestBillingAgreementOptions
  ): Map<String, Any?> {
    val clientToken = options.clientToken
      ?: return failure(FAILED, "You must provide the clientToken")
    // Braintree v5 returns through an Android App Link, and PayPalClient has no
    // constructor without one, so it has to come from the caller.
    val appLinkReturnUrl = options.appLinkReturnUrl
      ?: return failure(FAILED, "You must provide the appLinkReturnUrl")
    val context = appContext.reactContext
      ?: return failure(FAILED, "The React context is not available")

    // Started before the first suspension point, so a second call made while
    // this one runs is rejected.
    val authResult = flow.start()
      ?: return failure(FAILED, "A billing agreement request is already in progress")

    try {
      val payPalClient = PayPalClient(
        context = context,
        authorization = clientToken,
        appLinkReturnUrl = Uri.parse(appLinkReturnUrl),
        // Matches the `${applicationId}.braintree` intent-filter merchants
        // already register. Used when a buyer has turned off "Open supported
        // links" and App Link return is unavailable.
        // Underscores are stripped as Braintree does for its own default
        // scheme: they are not valid in a URI scheme.
        deepLinkFallbackUrlScheme = "${context.packageName.replace("_", "")}.braintree"
      )
      val request = PayPalVaultRequest(
        hasUserLocationConsent = options.hasUserLocationConsent,
        billingAgreementDescription = options.billingAgreementDescription,
        merchantAccountId = options.merchantAccountID,
        displayName = options.displayName,
        localeCode = options.localeCode,
        isShippingAddressRequired = options.shippingAddressRequired
      )

      val readyToLaunch = when (val authRequest = payPalClient.createPaymentAuthRequest(context, request)) {
        is PayPalPaymentAuthRequest.ReadyToLaunch -> authRequest
        is PayPalPaymentAuthRequest.Failure -> return failure(FAILED, authRequest.error.message)
      }

      // Read only now, after the network call: the activity may have been
      // recreated in the meantime. `as?` is the safe cast: null for a missing
      // activity and for one of the wrong type alike.
      val activity = appContext.currentActivity as? ComponentActivity
        ?: return failure(FAILED, "The activity is not available")

      when (val pendingRequest = payPalLauncher.launch(activity, readyToLaunch)) {
        // In-process, the result is awaited below.
        is PayPalPendingRequest.Started -> flow.launched(pendingRequest.pendingRequestString, activity.intent)
        is PayPalPendingRequest.Failure -> return failure(FAILED, pendingRequest.error.message)
      }

      return toResponse(payPalClient, authResult.await())
    } finally {
      flow.finish(authResult)
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
      is PayPalResult.Failure -> failure(FAILED, tokenized.error.message)
      is PayPalResult.Cancel -> failure(CANCELED, CANCELED_MESSAGE)
    }
    is PayPalPaymentAuthResult.Failure -> failure(FAILED, result.error.message)
    // The buyer came back without finishing: closed the browser or pressed
    // back. Braintree also reports NoResult when the Custom Tab is minimized to
    // picture-in-picture, where the buyer could still approve afterwards; that
    // approval is then dropped, as JS has already been told Canceled.
    is PayPalPaymentAuthResult.NoResult -> failure(CANCELED, CANCELED_MESSAGE)
  }

  private fun payload(nonce: PayPalAccountNonce): Map<String, Any?> = mapOf(
    "payload" to mapOf(
      "nonce" to nonce.string,
      // Empty rather than null for a missing detail, as on iOS and as the
      // TypeScript types declare.
      "details" to mapOf(
        "payerId" to nonce.payerId.orEmpty(),
        "email" to nonce.email.orEmpty(),
        "firstName" to nonce.firstName.orEmpty(),
        "lastName" to nonce.lastName.orEmpty(),
        "phone" to nonce.phone.orEmpty()
      )
    )
  )

  private fun failure(code: String, message: String?): Map<String, Any?> =
    mapOf("error" to mapOf("code" to code, "message" to (message ?: "Unknown error")))

  companion object {
    private const val FAILED = "Failed"
    private const val CANCELED = "Canceled"
    private const val CANCELED_MESSAGE = "User canceled billing agreement request"

    private const val PREFS_NAME = "com.reactnativepaypal.PENDING_REQUEST"
  }
}
