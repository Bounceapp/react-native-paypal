import type {
  RequestBillingAgreementOptions,
  RequestBillingAgreementResponse,
} from "./types"

// There is no PayPal native module on web. requireNativeModule would throw at
// import time, so importing the package would crash any app that also targets
// web; resolve with the usual error shape instead.
export const requestBillingAgreement = async (
  _options: RequestBillingAgreementOptions,
): Promise<RequestBillingAgreementResponse> => ({
  payload: undefined,
  error: {
    code: "Failed",
    message: "PayPal billing agreements are not supported on web",
  },
})
