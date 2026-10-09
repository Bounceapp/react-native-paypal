const {
  AndroidConfig,
  createRunOncePlugin,
  withAndroidManifest,
} = require("expo/config-plugins")

const pkg = require("../package.json")

/**
 * @typedef {object} PaypalPluginProps
 * @property {string} appLinkReturnUrl The same URL passed to
 *   `requestBillingAgreement`. Its host and path scope the App Link
 *   intent-filter.
 */

const VIEW = "android.intent.action.VIEW"
const CATEGORIES = [
  "android.intent.category.DEFAULT",
  "android.intent.category.BROWSABLE",
]

/**
 * The scheme the Android module passes to Braintree as
 * `deepLinkFallbackUrlScheme`. Underscores are stripped there, as Braintree
 * does for its own default scheme, so they must be stripped here too.
 *
 * @param {string} packageName
 */
const fallbackScheme = packageName =>
  `${packageName.replace(/_/g, "")}.braintree`

/**
 * @param {Record<string, string>} data
 * @param {boolean} autoVerify
 */
const intentFilter = (data, autoVerify) => ({
  ...(autoVerify ? { $: { "android:autoVerify": "true" } } : {}),
  action: [{ $: { "android:name": VIEW } }],
  category: CATEGORIES.map(name => ({ $: { "android:name": name } })),
  data: [{ $: data }],
})

/**
 * Whether the activity already has a filter for exactly this data, so that
 * running prebuild again without `--clean` does not add a duplicate.
 *
 * @param {any[]} filters
 * @param {Record<string, string>} data
 */
const hasFilterFor = (filters, data) =>
  filters.some(filter =>
    (filter.data ?? []).some(entry => {
      const attributes = entry.$ ?? {}
      const keys = Object.keys(data)
      return (
        Object.keys(attributes).length === keys.length &&
        keys.every(key => attributes[key] === data[key])
      )
    }),
  )

/**
 * Adds the two intent-filters the PayPal return needs to the main activity:
 * the App Link, and the `.braintree` deep link Braintree falls back to when a
 * buyer has turned off "Open supported links".
 *
 * @param {any} manifest The AndroidManifest as parsed by `@expo/config-plugins`.
 * @param {{ packageName: string, appLinkReturnUrl: string }} options
 */
const addPaypalIntentFilters = (
  manifest,
  { packageName, appLinkReturnUrl },
) => {
  const url = parseAppLinkReturnUrl(appLinkReturnUrl)
  const activity = AndroidConfig.Manifest.getMainActivityOrThrow(manifest)
  activity["intent-filter"] = activity["intent-filter"] ?? []
  const filters = activity["intent-filter"]

  /** @type {Record<string, string>} */
  const appLink = { "android:scheme": "https", "android:host": url.hostname }
  // A path keeps the filter from claiming every link on the host.
  if (url.pathname !== "/") {
    appLink["android:pathPrefix"] = url.pathname
  }
  const deepLink = { "android:scheme": fallbackScheme(packageName) }

  if (!hasFilterFor(filters, appLink)) {
    filters.push(intentFilter(appLink, true))
  }
  if (!hasFilterFor(filters, deepLink)) {
    filters.push(intentFilter(deepLink, false))
  }
  return manifest
}

/** @param {unknown} appLinkReturnUrl */
const parseAppLinkReturnUrl = appLinkReturnUrl => {
  if (typeof appLinkReturnUrl !== "string") {
    throw new Error(
      "@bounceapp/react-native-paypal: the config plugin needs `appLinkReturnUrl`, the same URL you pass to requestBillingAgreement.",
    )
  }
  let url
  try {
    url = new URL(appLinkReturnUrl)
  } catch {
    throw new Error(
      `@bounceapp/react-native-paypal: \`appLinkReturnUrl\` is not a valid URL: ${appLinkReturnUrl}`,
    )
  }
  if (url.protocol !== "https:") {
    throw new Error(
      `@bounceapp/react-native-paypal: \`appLinkReturnUrl\` must be an https URL for an Android App Link: ${appLinkReturnUrl}`,
    )
  }
  // Android matches pathPrefix against the path as is, so a trailing slash
  // would stop "/paypal/" from matching "/paypal".
  url.pathname = url.pathname.replace(/\/+$/, "") || "/"
  return url
}

/**
 * @param {import("expo/config-plugins").ExpoConfig} config
 * @param {PaypalPluginProps} props
 */
const withPaypal = (config, props) => {
  // Validated up front, so a bad option fails when the config is read rather
  // than partway through prebuild.
  parseAppLinkReturnUrl(props?.appLinkReturnUrl)

  return withAndroidManifest(config, config => {
    const packageName = config.android?.package
    if (!packageName) {
      throw new Error(
        "@bounceapp/react-native-paypal: set `android.package` in your app config, which the `.braintree` return scheme is derived from.",
      )
    }
    addPaypalIntentFilters(config.modResults, {
      packageName,
      appLinkReturnUrl: props.appLinkReturnUrl,
    })
    return config
  })
}

module.exports = createRunOncePlugin(withPaypal, pkg.name, pkg.version)
module.exports.addPaypalIntentFilters = addPaypalIntentFilters
