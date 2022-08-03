import { NativeModules, Platform } from "react-native"

import type {
  RequestBillingAgreementOptions,
  RequestBillingAgreementResponse,
} from "./types"

const LINKING_ERROR =
  `The package '@bounceapp/react-native-paypal' doesn't seem to be linked. Make sure: \n\n` +
  Platform.select({ ios: "- You have run 'pod install'\n", default: "" }) +
  "- You rebuilt the app after installing the package\n" +
  "- You are not using Expo managed workflow\n"

const logger = (message: string, ...rest: unknown[]) =>
  console.info(`[@bounceapp/react-native-paypal] ${message}`, ...rest)

const isLinked = !!NativeModules.Paypal
if (!isLinked) logger(LINKING_ERROR)

const Paypal = isLinked ? NativeModules.Paypal : {}

export const requestBillingAgreement = (
  options: RequestBillingAgreementOptions,
): Promise<RequestBillingAgreementResponse> => {
  const DEFAULT_OPTIONS = {
    billingAgreementDescription: "",
    shippingAddressRequired: false,
  }
  const composedOptions = { ...DEFAULT_OPTIONS, ...options }

  return Paypal.requestBillingAgreement(composedOptions)
}

export default Paypal
