import { NativeModules, Platform } from "react-native";

const LINKING_ERROR =
  `The package 'react-native-paypal' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: "" }) +
  "- You rebuilt the app after installing the package\n" +
  "- You are not using Expo managed workflow\n";

export type RequestBillingAgreementOptions = {
  clientToken: string;
  billingAgreementDescription?: string;
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
    | "zh_XC";
  merchantAccountID?: string;
  displayName?: string;
};

export type RequestBillingAgreementError = {
  code: string;
  message?: string;
};

export type RequestBillingAgreementPayload = {
  nonce: string;
  details: {
    payerId: string;
    email: string;
    firstName: string;
    lastName: string;
    phone?: string;
  };
};

export type RequestBillingAgreementResponse =
  | {
      payload: undefined;
      error: RequestBillingAgreementError;
    }
  | {
      payload: RequestBillingAgreementPayload;
      error: undefined;
    };

const Paypal = NativeModules.Paypal
  ? NativeModules.Paypal
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR);
        },
      }
    );

const DEFAULT_OPTIONS = {
  billingAgreementDescription: "",
  shippingAddressRequired: false,
};

export function requestBillingAgreement(
  options: RequestBillingAgreementOptions
): Promise<RequestBillingAgreementResponse> {
  const composedOptions = { ...DEFAULT_OPTIONS, ...options };
  return Paypal.requestBillingAgreement(composedOptions);
}
