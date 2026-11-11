import {
  requestBillingAgreement,
  RequestBillingAgreementOptions,
  RequestBillingAgreementResponse,
} from "@bounceapp/react-native-paypal"
import { useState } from "react"
import { Button, ScrollView, StyleSheet, Text, View } from "react-native"
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context"

// Placeholder values: with a real client token and an App Link served by your
// domain, the PayPal flow would complete.
const options: RequestBillingAgreementOptions = {
  clientToken: "CLIENT_TOKEN",
  appLinkReturnUrl: "https://example.com/paypal",
  hasUserLocationConsent: false,
  billingAgreementDescription: "Example billing agreement",
  localeCode: "en_US",
  shippingAddressRequired: false,
  displayName: "Example app",
}

export default function App() {
  const [loading, setLoading] = useState(false)
  const [logs, setLogs] = useState<string[]>([])

  const log = (message: string) =>
    setLogs(previous => [
      `${new Date().toLocaleTimeString()} ${message}`,
      ...previous,
    ])

  const onPress = async () => {
    setLoading(true)
    log("requestBillingAgreement called")

    try {
      const res: RequestBillingAgreementResponse =
        await requestBillingAgreement(options)

      if (res.error) {
        log(`error result: ${JSON.stringify(res.error, null, 4)}`)
      } else {
        log(`payload: ${JSON.stringify(res.payload, null, 4)}`)
      }
    } catch (error) {
      log(`threw: ${String(error)}`)
    }

    setLoading(false)
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.screen}>
        <ScrollView style={styles.content}>
          {logs.map((entry, index) => (
            <Text key={`${logs.length - index}`} style={styles.result}>
              {entry}
            </Text>
          ))}
        </ScrollView>
        <View style={styles.bottomBar}>
          <Button
            title="Pay with PayPal"
            onPress={onPress}
            disabled={loading}
          />
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  )
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  bottomBar: {
    padding: 20,
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
  result: {
    marginTop: 24,
  },
})
