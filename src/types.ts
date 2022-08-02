import type { PressableProps, StyleProp, ViewStyle } from "react-native"
import type { SvgProps } from "react-native-svg"

export type RequestBillingAgreementOptions = {
  clientToken: string
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
