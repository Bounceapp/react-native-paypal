# Migration guide

## 0.8.x → 1.0.0

This release moves both platforms onto the current Braintree SDKs: **Android
v4 → v5** and **iOS v6 → v7**. It also rewrites the native code as an Expo
module and removes the bundled UI components.

It is a breaking release. Android v4 goes unsupported in **October 2026** and
iOS v6 deprecates in **November 2026**, so this is not optional for long.

### At a glance

| Change                                      | Platform | Action                                                                |
| ------------------------------------------- | -------- | --------------------------------------------------------------------- |
| `appLinkReturnUrl` is now a required option | Android  | Pass it to `requestBillingAgreement`                                  |
| App Link must be configured and registered  | Android  | Host `assetlinks.json`, add an intent-filter, register with Braintree |
| `minSdkVersion` 21 → 23                     | Android  | Raise your `minSdkVersion`                                            |
| iOS deployment target 14.0 → 16.0           | iOS      | Raise your deployment target                                          |
| Native code is now an Expo module           | Both     | Install `expo`                                                        |
| `PaypalButton` and `PaypalLogo` removed     | Both     | Render your own button                                                |

The response shape is unchanged and every existing option behaves the same.
A few specific results do change -- see [Behaviour changes](#behaviour-changes).

---

## Expo is now required

The native code is an [Expo module](https://docs.expo.dev/modules/overview/)
rather than a legacy React Native bridge module, so it no longer depends on the
bridgeless interop layer. Your app needs the `expo` package. Expo apps already
have it; bare React Native apps can add it with:

```sh
npx install-expo-modules@latest
```

## `PaypalButton` and `PaypalLogo` are removed

The package now exports only `requestBillingAgreement`, and `react-native-svg`
is no longer a peer dependency. Render your own button and call
`requestBillingAgreement` from it. PayPal's brand guidelines still apply to the
logo and colours you use.

If nothing else in your app uses `react-native-svg`, you can remove it.

---

## Android

### 1. Set up an App Link

Braintree v5 returns from the PayPal flow through an [Android App
Link](https://developer.android.com/training/app-links) rather than deriving a
browser-switch scheme itself. `PayPalClient` has a single constructor and its
`appLinkReturnUrl` parameter is non-null, so this cannot be skipped.

You need a domain you control, serving `/.well-known/assetlinks.json`:

```json
[
  {
    "relation": ["delegate_permission/common.handle_all_urls"],
    "target": {
      "namespace": "android_app",
      "package_name": "com.your.app",
      "sha256_cert_fingerprints": ["YOUR:APP:SIGNING:FINGERPRINT"]
    }
  }
]
```

### 2. Register the App Link with Braintree

Add the URL in your **Braintree Control Panel**. This is a required step — an
App Link that is not registered will not be accepted.

### 3. Declare the intent-filter

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<activity android:name=".MainActivity">
  <!-- App Link: the primary return path. -->
  <intent-filter android:autoVerify="true">
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="https" android:host="your-app.example.com" />
  </intent-filter>

  <!-- Deep link: the fallback return path. Keep this. -->
  <intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="${applicationId}.braintree" />
  </intent-filter>
</activity>
```

On Expo, express the same thing in `app.json` under
`expo.android.intentFilters` — see the README for that form.

> **If your App Link URL contains a path**, add a matching
> `android:pathPrefix` to the intent-filter. Unlike iOS, the Android SDK uses
> the URL exactly as given and does not append a path of its own.

**Keep the `${applicationId}.braintree` filter you already have.** It is no
longer the primary return path, but it is still used as the fallback when a
buyer has turned off "Open supported links" on their device. This library
derives that scheme from your application ID automatically, so there is
nothing new to configure for it.

### 4. Raise `minSdkVersion` to 23

Braintree v5 requires API 23. If you support Android 5.0 or 5.1 (API 21–22),
those devices can no longer use this library.

### 5. Pass `appLinkReturnUrl`

```diff
 const res = await requestBillingAgreement({
   clientToken: "CLIENT_TOKEN",
+  appLinkReturnUrl: "https://your-app.example.com",
 })
```

The option is typed as required, so TypeScript will point you at every call
site rather than letting it fail at runtime inside a payment.

---

## iOS

### Raise the deployment target to 16.0

Braintree iOS v7 requires iOS 16. There is no v7 release at a lower target.

**Expo SDK 56 and later already meet this** — the SDK's own minimum is 16.4, so
there is nothing to do. On older SDKs, raise it via `expo-build-properties`:

```json
{
  "expo": {
    "plugins": [
      ["expo-build-properties", { "ios": { "deploymentTarget": "16.0" } }]
    ]
  }
}
```

If you skip this, `pod install` fails at resolve time rather than at build
time:

```
[!] CocoaPods could not find compatible versions for pod "Braintree":
    Specs satisfying the `Braintree (~> 7.12.0)` dependency were found,
    but they required a higher minimum deployment target.
```

No JavaScript changes are needed for iOS. `appLinkReturnUrl` is Android-only
and is ignored there.

---

## Behaviour changes

These don't change the response shape, but they do change what you get back in
specific cases:

- **A second call while one is running** now resolves `Failed` with
  "A billing agreement request is already in progress". Before, it replaced
  the first call, whose promise then never settled.
- **Returning to the app without finishing** — closing the browser or pressing
  back — now resolves `Canceled`. On Android this used to leave the promise
  pending.
- **Cancelling on iOS** resolves `Canceled` as before. Braintree v7 reports it
  differently under the hood; this is handled for you.
- **Android error messages are now Braintree's own**, matching iOS. Android used
  to return the fixed string "The billing agreement request failed" for every
  failure. If you show `error.message` to users, check the wording is
  acceptable.
- **Web** now resolves `Failed` with "PayPal billing agreements are not
  supported on web" instead of failing to find the native module. Importing the
  package in a web build no longer crashes.
- **If Android kills your app while the buyer is in PayPal**, the pending
  request is cleaned up on relaunch and nothing is delivered — the call that
  started it no longer exists. The buyer starts again.

---

## Optional: `hasUserLocationConsent`

Braintree v5 added a `hasUserLocationConsent` flag, passed on for risk
assessment. This library defaults it to `false`, which asserts that no consent
has been obtained. If you do collect location consent from buyers, set it:

```ts
await requestBillingAgreement({
  clientToken: "CLIENT_TOKEN",
  appLinkReturnUrl: "https://your-app.example.com",
  hasUserLocationConsent: true,
})
```

---

## Checklist

- [ ] Domain serves `/.well-known/assetlinks.json` with your app's signing fingerprint
- [ ] App Link registered in the Braintree Control Panel
- [ ] `autoVerify` intent-filter added, `${applicationId}.braintree` filter kept
- [ ] `minSdkVersion` at 23 or higher
- [ ] iOS deployment target at 16.0 or higher (automatic on Expo SDK 56+)
- [ ] `expo` installed
- [ ] `PaypalButton` / `PaypalLogo` usages replaced with your own button
- [ ] Any UI that shows `error.message` checked against the new Android wording
- [ ] `appLinkReturnUrl` passed at every `requestBillingAgreement` call site
- [ ] Billing agreement flow tested on a real device, including cancelling
