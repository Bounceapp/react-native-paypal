package com.reactnativepaypal

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import com.braintreepayments.api.*
import com.facebook.react.bridge.*
import com.facebook.react.bridge.Arguments

import com.facebook.react.bridge.WritableMap
import com.braintreepayments.api.PayPalAccountNonce
import com.braintreepayments.api.PostalAddress

import com.reactnativepaypal.utils.*
import com.reactnativepaypal.utils.createError

class PaypalModule(reactContext: ReactApplicationContext): ReactContextBaseJavaModule(reactContext), ActivityEventListener, LifecycleEventListener {
  private val mContext: Context
  private var mCurrentActivity: FragmentActivity? = null
  private var mPromise: Promise? = null
  private var mToken: String? = null
  private var mBraintreeClient: BraintreeClient? = null
  private var mPayPalClient: PayPalClient? = null

  override fun getName(): String {
    return "Paypal"
  }

  override fun onActivityResult(
    activity: Activity,
    requestCode: Int,
    resultCode: Int,
    intent: Intent?
  ) {
  }

  override fun onNewIntent(intent: Intent) {
    if (mCurrentActivity != null) {
      mCurrentActivity!!.intent = intent
    }
  }

  override fun onHostResume() {
    if (mBraintreeClient != null && mCurrentActivity != null) {
      val browserSwitchResult = mBraintreeClient!!.deliverBrowserSwitchResult(
        mCurrentActivity!!
      )
      if (browserSwitchResult != null) {
        when (browserSwitchResult.requestCode) {
          BraintreeRequestCodes.PAYPAL -> if (mPayPalClient != null) {
            mPayPalClient!!.onBrowserSwitchResult(
              browserSwitchResult
            ) { payPalAccountNonce: PayPalAccountNonce?, error: Exception? ->
              handlePayPalResult(
                payPalAccountNonce,
                error
              )
            }
          }
        }
      }
    }
  }

  @ReactMethod
  fun requestBillingAgreement(
    options: ReadableMap,
    promise: Promise
  ) {
    mPromise = promise

    if (!options.hasKey("clientToken")) {
      mPromise!!.resolve(createError(ErrorType.Failed.toString(), "You must provide the clientToken"))
    }
    if (!options.hasKey("billingAgreementDescription")) {
      mPromise!!.resolve(createError(ErrorType.Failed.toString(), "You must provide the billingAgreementDescription"))
    }
    val clientToken = options.getString("clientToken")
    val billingAgreementDescription = options.getString("billingAgreementDescription")

    setup(clientToken)
    if (mCurrentActivity != null) {
      mPayPalClient = PayPalClient(mBraintreeClient!!)
      val request = PayPalVaultRequest()
      request.billingAgreementDescription = billingAgreementDescription

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

      mPayPalClient!!.tokenizePayPalAccount(
        mCurrentActivity!!,
        request
      ) { e: Exception? -> handlePayPalResult(null, e) }
    }
  }

  private fun handlePayPalResult(
    payPalAccountNonce: PayPalAccountNonce?,
    error: Exception?
  ) {
    if (error != null) {
      handleError(error)
      return
    }
    if (payPalAccountNonce != null) {
      val result = Arguments.createMap()
      val payload = Arguments.createMap()
      val details = Arguments.createMap()

      payload.putString("nonce", payPalAccountNonce.string)

      details.putString("payerId", payPalAccountNonce.payerId)
      details.putString("email", payPalAccountNonce.email)
      details.putString("firstName", payPalAccountNonce.firstName)
      details.putString("lastName", payPalAccountNonce.lastName)
      details.putString("phone", payPalAccountNonce.phone)

      payload.putMap("details", details)
      result.putMap("payload", payload)

      if (mPromise != null) {
        mPromise!!.resolve(result)
      }
    }
  }

  private fun setup(token: String?) {
    if (mBraintreeClient == null || token != mToken) {
      mCurrentActivity = currentActivity as FragmentActivity?
      mBraintreeClient = BraintreeClient(mContext, token!!)
      mToken = token
    }
  }

  private fun handleError(error: Exception) {
    if (mPromise != null) {
      if (error is UserCanceledException) {
        mPromise!!.resolve(createError(ErrorType.Canceled.toString(), "User cancelled billing agreement request"))
      }
    mPromise!!.resolve(createError(ErrorType.Failed.toString(), "The billing agreement request failed"))
    }
  }

  override fun onHostPause() {
    //NOTE: empty implementation
  }

  override fun onHostDestroy() {
    //NOTE: empty implementation
  }

  init {
    mContext = reactContext
    reactContext.addLifecycleEventListener(this)
    reactContext.addActivityEventListener(this)
  }
}
