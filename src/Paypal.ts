import { NativeModule, requireNativeModule } from "expo"

import type {
  RequestBillingAgreementOptions,
  RequestBillingAgreementResponse,
} from "./types"

declare class PaypalModule extends NativeModule {
  requestBillingAgreement(
    options: RequestBillingAgreementOptions,
  ): Promise<RequestBillingAgreementResponse>
}

const Paypal = requireNativeModule<PaypalModule>("Paypal")

export const requestBillingAgreement = (
  options: RequestBillingAgreementOptions,
): Promise<RequestBillingAgreementResponse> =>
  Paypal.requestBillingAgreement({
    billingAgreementDescription: "",
    shippingAddressRequired: false,
    ...options,
  })
