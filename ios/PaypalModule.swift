import Braintree
import ExpoModulesCore

struct RequestBillingAgreementOptions: Record {
  @Field var clientToken: String?
  @Field var billingAgreementDescription: String?
  @Field var merchantAccountID: String?
  @Field var displayName: String?
  @Field var localeCode: String?
  @Field var shippingAddressRequired: Bool = false
}

public class PaypalModule: Module {
  // Main-actor confined, like the Android module's state on Queues.MAIN.
  //
  // The client is retained for the whole flow: it hands control to the PayPal
  // app or a browser and calls back later, and Braintree reports `.deallocated`
  // if it is released in between.
  @MainActor private var payPalClient: BTPayPalClient?

  public func definition() -> ModuleDefinition {
    Name("Paypal")

    AsyncFunction("requestBillingAgreement") { (options: RequestBillingAgreementOptions) async -> [String: Any] in
      await self.requestBillingAgreement(options)
    }
  }

  @MainActor
  private func requestBillingAgreement(_ options: RequestBillingAgreementOptions) async -> [String: Any] {
    // Checked and set without an intervening suspension, so a second call made
    // while one is running is rejected rather than replacing the first.
    guard payPalClient == nil else {
      return error(ErrorCode.failed, "A billing agreement request is already in progress")
    }
    guard let clientToken = options.clientToken else {
      return error(ErrorCode.failed, "You must provide the clientToken")
    }
    guard let billingAgreementDescription = options.billingAgreementDescription else {
      return error(ErrorCode.failed, "You must provide the billingAgreementDescription")
    }

    let payPalClient = BTPayPalClient(authorization: clientToken)
    self.payPalClient = payPalClient
    defer { self.payPalClient = nil }

    let request = BTPayPalVaultRequest(
      billingAgreementDescription: billingAgreementDescription,
      displayName: options.displayName,
      isShippingAddressRequired: options.shippingAddressRequired,
      localeCode: options.localeCode.map(localeCodeFromString) ?? .none,
      merchantAccountID: options.merchantAccountID
    )

    do {
      let nonce = try await payPalClient.tokenize(request)
      return [
        "payload": [
          "nonce": nonce.nonce,
          "details": [
            "payerId": nonce.payerID ?? "",
            "email": nonce.email ?? "",
            "firstName": nonce.firstName ?? "",
            "lastName": nonce.lastName ?? "",
            "phone": nonce.phone ?? "",
          ],
        ]
      ]
    } catch BTPayPalError.canceled {
      // v7 reports a cancel as a thrown error; v6 signalled it with a nil nonce
      // and a nil error.
      return error(ErrorCode.canceled, "User cancelled billing agreement request")
    } catch {
      return self.error(ErrorCode.failed, error.localizedDescription)
    }
  }

  private func error(_ code: String, _ message: String) -> [String: Any] {
    return ["error": ["code": code, "message": message]]
  }

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

private enum ErrorCode {
  static let failed = "Failed"
  static let canceled = "Canceled"
}
