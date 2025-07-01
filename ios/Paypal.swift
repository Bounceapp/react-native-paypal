@objc(Paypal)
class Paypal: NSObject {
  var braintreeClient: BTAPIClient?

  @objc(requestBillingAgreement:resolver:rejecter:)
  func requestBillingAgreement(
    options: NSDictionary, resolver resolve: @escaping RCTPromiseResolveBlock,
    rejecter reject: @escaping RCTPromiseRejectBlock
  ) {
    guard let clientToken = options["clientToken"] as? String else {
      resolve(Errors.createError(ErrorType.Failed, "You must provide the clientToken"))
      return
    }
    guard let billingAgreementDescription = options["billingAgreementDescription"] as? String else {
      resolve(Errors.createError(ErrorType.Failed, "You must provide the billingAgreementDescription"))
      return
    }
    let merchantAccountID = options["merchantAccountID"] as? String
    let displayName = options["displayName"] as? String
    let localeCode = options["localeCode"] as? String
    let isShippingAddressRequired = options["shippingAddressRequired"] as? Bool

    braintreeClient = BTAPIClient(authorization: clientToken)!
    let payPalClient = BTPayPalClient(apiClient: braintreeClient!)
    let request = BTPayPalVaultRequest()
    request.billingAgreementDescription = billingAgreementDescription
    if let merchantAccountID = merchantAccountID {
      request.merchantAccountID = merchantAccountID
    }
    if let displayName = displayName {
      request.displayName = displayName
    }
    if let localeCode = localeCode {
      request.localeCode = BTPayPalLocaleCode(rawValue: localeCode) ?? .none
    }
    if let isShippingAddressRequired = isShippingAddressRequired {
      request.isShippingAddressRequired = isShippingAddressRequired
    }

    payPalClient.tokenize(request) { (tokenizedPayPalAccount, error) -> Void in
      if let tokenizedPayPalAccount = tokenizedPayPalAccount {
        let result: NSDictionary = [
          "nonce": tokenizedPayPalAccount.nonce,
          "details": [
            "payerId": tokenizedPayPalAccount.payerID ?? "",
            "email": tokenizedPayPalAccount.email ?? "",
            "firstName": tokenizedPayPalAccount.firstName ?? "",
            "lastName": tokenizedPayPalAccount.lastName ?? "",
            "phone": tokenizedPayPalAccount.phone ?? ""
          ]
        ]

        resolve(["payload": result])
      } else if let error = error {
        resolve(Errors.createError(ErrorType.Failed, "The billing agreement request failed"))
      } else {
        resolve(Errors.createError(ErrorType.Canceled, "User cancelled billing agreement request"))
      }
    }
  }
}
