import type { PressableProps, StyleProp, ViewStyle } from "react-native"
import type { SvgProps } from "react-native-svg"

export type RequestBillingAgreementOptions = {
  clientToken: string
  /**
   * Android only. The Android App Link associated with your app, used to return
   * from the PayPal flow. Required by Braintree Android v5, which has no client
   * constructor without it. The domain must serve an `assetlinks.json` listing
   * your app. Ignored on iOS.
   */
  appLinkReturnUrl: string
  /**
   * Android only. Whether you have collected the buyer's consent to share their
   * location, which Braintree passes on for risk assessment. Defaults to
   * `false`, asserting no consent has been obtained.
   */
  hasUserLocationConsent?: boolean
  billingAgreementDescription?: string
  localeCode?:
    | "da_DK"
    | "de_DE"
    | "en_AU"
    | "en_GB"
    | "en_US"
    | "es_ES"
    | "es_XC"
    | "fr_CA"
    | "fr_FR"
    | "fr_XC"
    | "id_ID"
    | "it_IT"
    | "ja_JP"
    | "ko_KR"
    | "nl_NL"
    | "no_NO"
    | "pl_PL"
    | "pt_BR"
    | "pt_PT"
    | "ru_RU"
    | "sv_SE"
    | "th_TH"
    | "tr_TR"
    | "zh_CN"
    | "zh_HK"
    | "zh_TW"
    | "zh_XC"
  merchantAccountID?: string
  displayName?: string
}

export type RequestBillingAgreementError = {
  code: string
  message?: string
}

export type RequestBillingAgreementPayload = {
  nonce: string
  details: {
    payerId: string
    email: string
    firstName: string
    lastName: string
    phone?: string
  }
}

export type RequestBillingAgreementResponse =
  | {
      payload: undefined
      error: RequestBillingAgreementError
    }
  | {
      payload: RequestBillingAgreementPayload
      error: undefined
    }

export type PaypalButtonProps = {
  style?: StyleProp<ViewStyle>
  disabled?: boolean
  onPress?: PressableProps["onPress"]
}

export type PaypalLogoProps = SvgProps
