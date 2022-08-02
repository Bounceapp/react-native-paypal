import React, { useState } from "react"
import {
  ActivityIndicator,
  Button,
  SafeAreaView,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from "react-native"
import {
  requestBillingAgreement,
  RequestBillingAgreementResponse,
} from "@bounce/react-native-paypal"

export default function App() {
  const [loading, setLoading] = useState(false)
  const [payload, setPayload] =
    useState<RequestBillingAgreementResponse | null>()

  const onPress = async () => {
    setLoading(true)
    try {
      const res = await requestBillingAgreement({
        clientToken: "sandbox_csp74fwf_wyd36y8xkbhj2c28",
      })
      setPayload(res)
    } catch (error) {
      setPayload(null)
    }

    setLoading(false)
  }

  return (
    <SafeAreaView style={styles.screen}>
      <ScrollView style={styles.content}>
        {payload && (
          <Text style={styles.result}>{JSON.stringify(payload, null, 4)}</Text>
        )}
      </ScrollView>
      <View style={styles.bottomBar}>
        {loading ? (
          <ActivityIndicator />
        ) : (
          <Button onPress={onPress} title="Request Billing Agreement" />
        )}
      </View>
    </SafeAreaView>
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
