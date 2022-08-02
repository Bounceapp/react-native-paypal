import { NativeModules, Platform } from "react-native"

import type {
  RequestBillingAgreementOptions,
  RequestBillingAgreementResponse,
} from "./types"

const LINKING_ERROR =
  `The package 'react-native-paypal' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: "" }) +
  "- You rebuilt the app after installing the package\n" +
  "- You are not using Expo managed workflow\n"

const Paypal = NativeModules.Paypal
  ? NativeModules.Paypal
  : new Proxy(
      {},
      {
        get() {
          throw new Error(LINKING_ERROR)
        },
      },
    )

const DEFAULT_OPTIONS = {
  billingAgreementDescription: "",
  shippingAddressRequired: false,
}

export const requestBillingAgreement = (
  options: RequestBillingAgreementOptions,
): Promise<RequestBillingAgreementResponse> => {
  const composedOptions = { ...DEFAULT_OPTIONS, ...options }

  return Paypal.requestBillingAgreement(composedOptions)
}

export default Paypal
