import {
  requestBillingAgreement,
  type RequestBillingAgreementOptions,
  type RequestBillingAgreementResponse,
} from "@bounceapp/react-native-paypal"
import { useState } from "react"
import { Button, ScrollView, StyleSheet, Text, View } from "react-native"
import { SafeAreaProvider, SafeAreaView } from "react-native-safe-area-context"

// A Braintree sandbox tokenization key or client token, from example/.env.local
// (see CONTRIBUTING.md). Without one, "Pay with PayPal" resolves with an
// authorization error. With one, the PayPal flow opens, but only completes if
// your domain also serves the App Link below.
const clientToken =
  process.env.EXPO_PUBLIC_PAYPAL_CLIENT_TOKEN || "INVALID_CLIENT_TOKEN"

// Well formed, so Braintree fetches its configuration before failing on the
// unknown merchant. That network round trip keeps a request in flight long
// enough for a second one to reach the native module.
const UNKNOWN_TOKENIZATION_KEY = "sandbox_00000000_0000000000000000"

const optionsFor = (token: string): RequestBillingAgreementOptions => ({
  clientToken: token,
  appLinkReturnUrl: "https://example.com/paypal",
  hasUserLocationConsent: false,
  billingAgreementDescription: "Example billing agreement",
  localeCode: "en_US",
  shippingAddressRequired: false,
  displayName: "Example app",
})

type Scenario = {
  id: string
  title: string
  run: () => Promise<RequestBillingAgreementResponse[]>
}

// The E2E flows in e2e/ tap these by id and read the outcome from the status
// line, so keep the ids and the status format in sync with them.
const scenarios: Scenario[] = [
  {
    id: "pay",
    title: "Pay with PayPal",
    run: () => Promise.all([requestBillingAgreement(optionsFor(clientToken))]),
  },
  {
    id: "invalid-token",
    title: "Invalid client token",
    run: () =>
      Promise.all([
        requestBillingAgreement(optionsFor("INVALID_CLIENT_TOKEN")),
      ]),
  },
  {
    id: "concurrent",
    title: "Two requests at once",
    run: () =>
      Promise.all([
        requestBillingAgreement(optionsFor(UNKNOWN_TOKENIZATION_KEY)),
        requestBillingAgreement(optionsFor(UNKNOWN_TOKENIZATION_KEY)),
      ]),
  },
]

const outcome = (response: RequestBillingAgreementResponse) =>
  response.error ? response.error.code : "Success"

export default function App() {
  const [running, setRunning] = useState(false)
  // "<scenario id>:<outcome of each request, in call order>", e.g.
  // "concurrent:Failed,Failed".
  const [status, setStatus] = useState("idle")
  const [logs, setLogs] = useState<string[]>([])

  // One line per entry, so the E2E flows can match an entry with a regex.
  const log = (message: string) =>
    setLogs(previous => [
      `${new Date().toLocaleTimeString()} ${message}`,
      ...previous,
    ])

  const run = async ({ id, run }: Scenario) => {
    setRunning(true)
    setStatus(`${id}:running`)
    log(`${id}: started`)

    try {
      const responses = await run()
      responses.forEach((response, index) => {
        const label = `${id} #${index + 1}`
        if (response.error) {
          log(`${label} ${response.error.code}: ${response.error.message}`)
        } else {
          log(`${label} Success: ${JSON.stringify(response.payload)}`)
        }
      })
      setStatus(`${id}:${responses.map(outcome).join(",")}`)
    } catch (error) {
      log(`${id} threw: ${String(error)}`)
      setStatus(`${id}:threw`)
    }

    setRunning(false)
  }

  return (
    <SafeAreaProvider>
      <SafeAreaView style={styles.screen}>
        <Text testID="status" style={styles.status}>
          {status}
        </Text>
        <ScrollView style={styles.content}>
          {logs.map((entry, index) => (
            <Text key={`${logs.length - index}`} style={styles.result}>
              {entry}
            </Text>
          ))}
        </ScrollView>
        <View style={styles.bottomBar}>
          {scenarios.map(scenario => (
            <Button
              key={scenario.id}
              testID={scenario.id}
              title={scenario.title}
              onPress={() => run(scenario)}
              disabled={running}
            />
          ))}
        </View>
      </SafeAreaView>
    </SafeAreaProvider>
  )
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
  },
  status: {
    paddingHorizontal: 20,
    paddingTop: 12,
    fontWeight: "bold",
  },
  bottomBar: {
    padding: 20,
    gap: 8,
  },
  content: {
    flex: 1,
    paddingHorizontal: 20,
  },
  result: {
    marginTop: 16,
  },
})
