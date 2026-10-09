package com.reactnativepaypal

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import com.braintreepayments.api.paypal.PayPalClient
import com.braintreepayments.api.paypal.PayPalLauncher
import com.braintreepayments.api.paypal.PayPalPaymentAuthRequest
import com.braintreepayments.api.paypal.PayPalPaymentAuthResult
import com.braintreepayments.api.paypal.PayPalPendingRequest
import com.braintreepayments.api.paypal.PayPalResult
import com.braintreepayments.api.paypal.PayPalVaultRequest
import com.facebook.react.bridge.ActivityEventListener
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.reactnativepaypal.utils.ErrorType
import com.reactnativepaypal.utils.createError

class PaypalModule(reactContext: ReactApplicationContext) :
  ReactContextBaseJavaModule(reactContext), ActivityEventListener, LifecycleEventListener {

  private var mPromise: Promise? = null
  private var mPayPalClient: PayPalClient? = null
  // The activity's intent when the browser switch started. Browser-switch
  // matches a return on scheme and host only, so if this intent is itself an
  // old return URL (a previous flow's success, kept by setIntent() or by a
  // process restart) it would be replayed as this flow's result.
  private var mIntentAtLaunch: Intent? = null
  private val mPayPalLauncher = PayPalLauncher()

  init {
    reactContext.addActivityEventListener(this)
    reactContext.addLifecycleEventListener(this)
  }

  override fun getName(): String {
    return "Paypal"
  }

  @ReactMethod
  fun requestBillingAgreement(options: ReadableMap, promise: Promise) {
    mPromise = promise

    val clientToken = options.getString("clientToken")
    if (clientToken == null) {
      resolveError(ErrorType.Failed, "You must provide the clientToken")
      return
    }

    // v5 returns to the app through an Android App Link rather than deriving a
    // browser-switch scheme itself, so the merchant's App Link has to be
    // supplied by the caller -- PayPalClient has no constructor without it.
    val appLinkReturnUrl = options.getString("appLinkReturnUrl")
    if (appLinkReturnUrl == null) {
      resolveError(ErrorType.Failed, "You must provide the appLinkReturnUrl")
      return
    }

    val billingAgreementDescription = options.getString("billingAgreementDescription")
    if (billingAgreementDescription == null) {
      resolveError(ErrorType.Failed, "You must provide the billingAgreementDescription")
      return
    }

    val payPalClient = PayPalClient(
      context = reactApplicationContext,
      authorization = clientToken,
      appLinkReturnUrl = Uri.parse(appLinkReturnUrl),
      // Matches the `${applicationId}.braintree` intent-filter the README
      // already asks merchants to register, with underscores stripped as
      // Braintree does for its own default scheme -- they are not valid in a
      // URI scheme. Used when a buyer has turned off "Open supported links" and
      // App Link return is unavailable.
      deepLinkFallbackUrlScheme = "${reactApplicationContext.packageName.replace("_", "")}.braintree"
    )
    mPayPalClient = payPalClient

    val request = PayPalVaultRequest(
      hasUserLocationConsent = if (options.hasKey("hasUserLocationConsent")) {
        options.getBoolean("hasUserLocationConsent")
      } else {
        false
      },
      billingAgreementDescription = billingAgreementDescription
    )

    if (options.hasKey("merchantAccountID")) {
      request.merchantAccountId = options.getString("merchantAccountID")
    }
    if (options.hasKey("displayName")) {
      request.displayName = options.getString("displayName")
    }
    if (options.hasKey("localeCode")) {
      request.localeCode = options.getString("localeCode")
    }
    if (options.hasKey("shippingAddressRequired")) {
      request.isShippingAddressRequired = options.getBoolean("shippingAddressRequired")
    }

    payPalClient.createPaymentAuthRequest(reactApplicationContext, request) { paymentAuthRequest ->
      when (paymentAuthRequest) {
        // Read the activity only now: it may have been recreated while the
        // request was on the network.
        is PayPalPaymentAuthRequest.ReadyToLaunch -> {
          val activity = reactApplicationContext.currentActivity as? ComponentActivity
          if (activity == null) {
            resolveError(ErrorType.Failed, "The activity is not available")
          } else {
            launch(activity, paymentAuthRequest)
          }
        }
        is PayPalPaymentAuthRequest.Failure ->
          resolveError(ErrorType.Failed, paymentAuthRequest.error.message)
      }
    }
  }

  private fun launch(
    activity: ComponentActivity,
    paymentAuthRequest: PayPalPaymentAuthRequest.ReadyToLaunch
  ) {
    when (val pendingRequest = mPayPalLauncher.launch(activity, paymentAuthRequest)) {
      // The browser switch can outlive this process, so the pending request is
      // persisted rather than held in memory.
      is PayPalPendingRequest.Started -> {
        mIntentAtLaunch = activity.intent
        storePendingRequest(pendingRequest.pendingRequestString)
      }
      is PayPalPendingRequest.Failure ->
        resolveError(ErrorType.Failed, pendingRequest.error.message)
    }
  }

  // A singleTask or singleTop activity -- Expo's default -- receives the PayPal
  // return here. React Native's ReactActivity does not call setIntent(), so the
  // activity's own intent would still be the original launch intent by the time
  // onHostResume runs.
  override fun onNewIntent(intent: Intent) {
    handleReturnToApp(intent)
  }

  // Covers every other launch mode, where the return starts or recreates the
  // activity with the return intent. For singleTask this runs after
  // onNewIntent has already consumed the pending request, so it returns early.
  // An intent that was already there at launch is not a return, so it is
  // swapped for an empty one, which settles as NoResult.
  override fun onHostResume() {
    val intent = reactApplicationContext.currentActivity?.intent ?: return
    handleReturnToApp(if (intent === mIntentAtLaunch) Intent() else intent)
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    data: Intent?
  ) {
    //NOTE: empty implementation
  }

  private fun handleReturnToApp(intent: Intent) {
    val pendingRequestString = getPendingRequest() ?: return
    val pendingRequest = PayPalPendingRequest.Started(pendingRequestString)
    mIntentAtLaunch = null

    when (val authResult = mPayPalLauncher.handleReturnToApp(pendingRequest, intent)) {
      is PayPalPaymentAuthResult.Success -> {
        clearPendingRequest()
        mPayPalClient?.tokenize(authResult) { result ->
          when (result) {
            is PayPalResult.Success -> resolveNonce(result)
            is PayPalResult.Failure -> resolveError(ErrorType.Failed, result.error.message)
            is PayPalResult.Cancel ->
              resolveError(ErrorType.Canceled, "User cancelled billing agreement request")
          }
        }
      }

      is PayPalPaymentAuthResult.Failure -> {
        clearPendingRequest()
        resolveError(ErrorType.Failed, authResult.error.message)
      }

      // The buyer came back without finishing -- closed the browser or pressed
      // back. Braintree also reports NoResult when the Custom Tab is minimized
      // to picture-in-picture, where the buyer could still approve afterwards;
      // that approval is then dropped, as JS has already been told Canceled.
      // Settle as a cancel rather than leaving the promise pending, and drop
      // the stored request so it cannot be replayed on a later launch.
      is PayPalPaymentAuthResult.NoResult -> {
        clearPendingRequest()
        resolveError(ErrorType.Canceled, "User cancelled billing agreement request")
      }
    }
  }

  private fun resolveNonce(result: PayPalResult.Success) {
    val payload = Arguments.createMap()
    val details = Arguments.createMap()

    payload.putString("nonce", result.nonce.string)

    details.putString("payerId", result.nonce.payerId)
    details.putString("email", result.nonce.email)
    details.putString("firstName", result.nonce.firstName)
    details.putString("lastName", result.nonce.lastName)
    details.putString("phone", result.nonce.phone)

    payload.putMap("details", details)

    val map = Arguments.createMap()
    map.putMap("payload", payload)

    mPromise?.resolve(map)
    mPromise = null
  }

  private fun resolveError(type: ErrorType, message: String?) {
    mPromise?.resolve(createError(type.toString(), message))
    mPromise = null
  }

  private fun sharedPreferences() =
    reactApplicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  private fun storePendingRequest(pendingRequestString: String) {
    sharedPreferences().edit().putString(PENDING_REQUEST_KEY, pendingRequestString).apply()
  }

  private fun getPendingRequest(): String? =
    sharedPreferences().getString(PENDING_REQUEST_KEY, null)

  private fun clearPendingRequest() {
    sharedPreferences().edit().remove(PENDING_REQUEST_KEY).apply()
  }

  override fun onHostPause() {
    //NOTE: empty implementation
  }

  override fun onHostDestroy() {
    //NOTE: empty implementation
  }

  companion object {
    private const val PREFS_NAME = "com.reactnativepaypal.PENDING_REQUEST"
    private const val PENDING_REQUEST_KEY = "pending_request"
  }
}
