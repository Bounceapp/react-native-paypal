# @bounceapp/react-native-paypal

[![Package version](https://img.shields.io/npm/v/@bounceapp/react-native-paypal?style=for-the-badge&labelColor=000000)](https://www.npmjs.com/package/@bounceapp/react-native-paypal)
[![MIT license](https://img.shields.io/badge/License-MIT-brightgreen.svg?style=for-the-badge&labelColor=000000)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-hotpink.svg?style=for-the-badge&labelColor=000000)](https://github.com/dcangulo/@bounceapp/react-native-paypal/pulls)

React Native wrapper to bridge PayPal iOS and Android SDK,
support only `requestBillingAgreement` for the moment

### Platform Compatibility

| Android Device | Android Emulator | iOS Device | iOS Simulator | Expo managed | Web |
| -------------- | ---------------- | ---------- | ------------- | ------------ | --- |
| ✅             | ✅               | ✅         | ✅            | ❌           | ❌  |

## Documentation

[API Reference](https://bounceapp.github.io/react-native-paypal/).

## Installation

```sh
yarn add @bounceapp/react-native-paypal react-native-svg
or
npm install @bounceapp/react-native-paypal react-native-svg
```

### Bare Workflow

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

### Expo

```ts
// app.json
{
  "expo": {
    "android": {
      "intentFilters": [
        {
          "action": "VIEW",
          "data": [
            {
              "scheme": "${applicationId}.braintree"
            }
          ],
          "category": ["BROWSABLE", "DEFAULT"]
        }
      ]
    }
  }
}
```

## Usage example

```js
// App.tsx
import React, { useState } from "react"
import { Button } from "react-native"
import {
  requestBillingAgreement,
  PaypalButton,
} from "@bounceapp/react-native-paypal"

export default function App() {
  const [loading, setLoading] = useState(false)

  const onPress = async () => {
    const res = await requestBillingAgreement({
      clientToken: "CLIENT_TOKEN",
    })

    if (res?.error) {
      console.error(res?.error)
      return
    }

    setLoading(false)
  }

  return <PaypalButton onPress={onPress} disabled={loading} />
}
```

## Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

This project is [MIT](https://img.shields.io/github/license/stripe/stripe-react-native) licensed.
