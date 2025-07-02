import Braintree

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
      request.localeCode = localeCodeFromString(localeCode)
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
        resolve(Errors.createError(ErrorType.Failed, error.localizedDescription))
      } else {
        resolve(Errors.createError(ErrorType.Canceled, "User cancelled billing agreement request"))
      }
    }
  }

  // Helper function to convert string to BTPayPalLocaleCode
  private func localeCodeFromString(_ localeString: String) -> BTPayPalLocaleCode {
    switch localeString {
      case "da_DK": return .da_DK
      case "de_DE": return .de_DE
      case "en_AU": return .en_AU
      case "en_GB": return .en_GB
      case "en_US": return .en_US
      case "es_ES": return .es_ES
      case "es_XC": return .es_XC
      case "fr_CA": return .fr_CA
      case "fr_FR": return .fr_FR
      case "fr_XC": return .fr_XC
      case "id_ID": return .id_ID
      case "it_IT": return .it_IT
      case "ja_JP": return .ja_JP
      case "ko_KR": return .ko_KR
      case "nl_NL": return .nl_NL
      case "no_NO": return .no_NO
      case "pl_PL": return .pl_PL
      case "pt_BR": return .pt_BR
      case "pt_PT": return .pt_PT
      case "ru_RU": return .ru_RU
      case "sv_SE": return .sv_SE
      case "th_TH": return .th_TH
      case "tr_TR": return .tr_TR
      case "zh_CN": return .zh_CN
      case "zh_HK": return .zh_HK
      case "zh_TW": return .zh_TW
      case "zh_XC": return .zh_XC
      default: return .none
    }
  }
}
