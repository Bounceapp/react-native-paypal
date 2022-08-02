# @bounce/react-native-paypal

[![Version](https://img.shields.io/npm/v/@bounce/react-native-paypal.svg)](https://www.npmjs.com/package/@bounce/react-native-paypal) [![Documentation](https://img.shields.io/badge/documentation-yes-brightgreen.svg)](https://bounceapp.github.io/react-native-paypal/) [![License](https://img.shields.io/github/license/stripe/stripe-react-native)](https://github.com/Bounceapp/react-native-paypal/blob/master/LICENSE)

React Native wrapper to bridge PayPal iOS and Android SDK,
support only `requestBillingAgreement` for the moment

[📘 browse the SDK reference](https://bounceapp.github.io/react-native-paypal/).

## Installation

```sh
yarn add @bounce/react-native-paypal
or
npm install @bounce/react-native-paypal
```

### Android

```xml
// android/app/src/main/AndroidManifest.xml
<activity
  android:name=".MainActivity"
  // ...
  >
  // ...
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="${applicationId}.braintree" />
  </intent-filter>
</activity>
```

## Usage example

```js
// App.tsx
import React from "react"
import { Button } from "react-native"
import { requestBillingAgreement } from "@bounce/react-native-paypal"

export default function App() {
  const onPress = async () => {
    try {
      const res = await requestBillingAgreement({
        clientToken: "sandbox_csp74fwf_wyd36y8xkbhj2c28",
      })

      if (res?.error) throw new Error(res.error?.message ?? res.error.code)

      console.log(res.payload)
    } catch (error) {
      console.error(error)
    }
  }

  return <Button onPress={onPress} title="Request Billing Agreement" />
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

This project is [MIT](https://img.shields.io/github/license/stripe/stripe-react-native) licensed.
