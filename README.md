# @bounceapp/react-native-paypal

[![Package version](https://img.shields.io/npm/v/@bounceapp/react-native-paypal?style=for-the-badge&labelColor=000000)](https://www.npmjs.com/package/@bounceapp/react-native-paypal)
[![MIT license](https://img.shields.io/badge/License-MIT-brightgreen.svg?style=for-the-badge&labelColor=000000)](LICENSE)
[![PRs Welcome](https://img.shields.io/badge/PRs-welcome-hotpink.svg?style=for-the-badge&labelColor=000000)](https://github.com/dcangulo/@bounceapp/react-native-paypal/pulls)

React Native wrapper to bridge PayPal iOS and Android SDK,
support only `requestBillingAgreement` for the moment

### Platform Compatibility

| Android Device | Android Emulator | iOS Device | iOS Simulator | Expo GO | Web |
| -------------- | ---------------- | ---------- | ------------- | ------- | --- |
| ✅             | ✅               | ✅         | ✅            | ❌      | ❌  |

## Documentation

[API Reference](https://bounceapp.github.io/react-native-paypal/).

> **Upgrading from 0.8.x?** 1.0.0 moves to Braintree Android v5 and iOS v7 and
> is a breaking release. See the [migration guide](MIGRATION.md).

## Installation

```sh
yarn add @bounceapp/react-native-paypal
```

This is an [Expo module](https://docs.expo.dev/modules/overview/), so your app
needs the `expo` package. Bare React Native apps can add it with
`npx install-expo-modules@latest`. It is developed and tested against Expo SDK 56.

### Requirements

|         | Minimum                       |
| ------- | ----------------------------- |
| iOS     | 16.0 (Braintree iOS v7)       |
| Android | API 23 (Braintree Android v5) |

### Android return URLs

Braintree Android v5 returns from the PayPal flow through an [Android App
Link](https://developer.android.com/training/app-links), so you need a domain
you control serving an `assetlinks.json` that lists your app. Pass that URL as
`appLinkReturnUrl` on every `requestBillingAgreement` call.

The `${applicationId}.braintree` scheme is still used, now as the fallback for
buyers who have turned off "Open supported links". This library derives it from
your application ID automatically — you only need to keep declaring it.

### Bare Workflow

```xml
// android/app/src/main/AndroidManifest.xml
<activity
  android:name=".MainActivity"
  // ...
  >
  // ...
  <!-- App Link: the primary return path. -->
  <intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="your-app.example.com" />
  </intent-filter>
  <!-- Deep link: the fallback return path. -->
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
          "autoVerify": true,
          "data": [
            {
              "scheme": "https",
              "host": "your-app.example.com"
            }
          ],
          "category": ["BROWSABLE", "DEFAULT"]
        },
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
import { useState } from "react"
import { Button } from "react-native"
import { requestBillingAgreement } from "@bounceapp/react-native-paypal"

export default function App() {
  const [loading, setLoading] = useState(false)

  const onPress = async () => {
    setLoading(true)
    const res = await requestBillingAgreement({
      clientToken: "CLIENT_TOKEN",
      // Android only: your verified App Link. Ignored on iOS.
      appLinkReturnUrl: "https://your-app.example.com",
    })

    setLoading(false)

    if (res.error) {
      console.error(res.error)
      return
    }
  }

  return <Button title="Pay with PayPal" onPress={onPress} disabled={loading} />
}
```

## 👏 Contributing

See the [contributing guide](CONTRIBUTING.md) to learn how to contribute to the repository and the development workflow.

## License

The source code is made available under the [MIT license](LICENSE). Some of the dependencies can be licensed differently, with the BSD license, for example.
