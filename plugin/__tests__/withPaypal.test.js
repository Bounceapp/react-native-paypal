const { addPaypalIntentFilters } = require("../withPaypal")

const manifestWithMainActivity = () => ({
  manifest: {
    application: [
      {
        $: { "android:name": ".MainApplication" },
        activity: [
          {
            $: { "android:name": ".MainActivity" },
            "intent-filter": [
              {
                action: [
                  { $: { "android:name": "android.intent.action.MAIN" } },
                ],
                category: [
                  { $: { "android:name": "android.intent.category.LAUNCHER" } },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
})

const filtersOf = manifest =>
  manifest.manifest.application[0].activity[0]["intent-filter"]

const dataOf = manifest =>
  filtersOf(manifest)
    .flatMap(filter => filter.data ?? [])
    .map(entry => entry.$)

describe("addPaypalIntentFilters", () => {
  it("adds a verified App Link scoped to the URL's path", () => {
    const manifest = addPaypalIntentFilters(manifestWithMainActivity(), {
      packageName: "com.example.app",
      appLinkReturnUrl: "https://example.com/paypal",
    })

    const appLink = filtersOf(manifest).find(
      filter => filter.data?.[0].$["android:scheme"] === "https",
    )
    expect(appLink.$).toEqual({ "android:autoVerify": "true" })
    expect(appLink.data[0].$).toEqual({
      "android:scheme": "https",
      "android:host": "example.com",
      "android:pathPrefix": "/paypal",
    })
    expect(appLink.category.map(c => c.$["android:name"])).toEqual([
      "android.intent.category.DEFAULT",
      "android.intent.category.BROWSABLE",
    ])
  })

  it("adds the .braintree fallback scheme the module passes to Braintree", () => {
    const manifest = addPaypalIntentFilters(manifestWithMainActivity(), {
      packageName: "com.my_app",
      appLinkReturnUrl: "https://example.com/paypal",
    })

    // Underscores stripped, as in PaypalModule.kt.
    expect(dataOf(manifest)).toContainEqual({
      "android:scheme": "com.myapp.braintree",
    })
  })

  it("leaves out pathPrefix for a URL without a path", () => {
    const manifest = addPaypalIntentFilters(manifestWithMainActivity(), {
      packageName: "com.example.app",
      appLinkReturnUrl: "https://example.com/",
    })

    expect(dataOf(manifest)).toContainEqual({
      "android:scheme": "https",
      "android:host": "example.com",
    })
  })

  it("ignores a trailing slash in the path", () => {
    const manifest = addPaypalIntentFilters(manifestWithMainActivity(), {
      packageName: "com.example.app",
      appLinkReturnUrl: "https://example.com/paypal/",
    })

    expect(dataOf(manifest)).toContainEqual({
      "android:scheme": "https",
      "android:host": "example.com",
      "android:pathPrefix": "/paypal",
    })
  })

  it("keeps the activity's existing filters", () => {
    const manifest = addPaypalIntentFilters(manifestWithMainActivity(), {
      packageName: "com.example.app",
      appLinkReturnUrl: "https://example.com/paypal",
    })

    expect(filtersOf(manifest)).toHaveLength(3)
    expect(filtersOf(manifest)[0].action[0].$["android:name"]).toBe(
      "android.intent.action.MAIN",
    )
  })

  it("does not add duplicates when run again", () => {
    const options = {
      packageName: "com.example.app",
      appLinkReturnUrl: "https://example.com/paypal",
    }
    const manifest = addPaypalIntentFilters(
      addPaypalIntentFilters(manifestWithMainActivity(), options),
      options,
    )

    expect(filtersOf(manifest)).toHaveLength(3)
  })

  it.each([
    [undefined, /needs `appLinkReturnUrl`/],
    ["not a url", /not a valid URL/],
    ["http://example.com/paypal", /must be an https URL/],
  ])("rejects appLinkReturnUrl %p", (appLinkReturnUrl, message) => {
    expect(() =>
      addPaypalIntentFilters(manifestWithMainActivity(), {
        packageName: "com.example.app",
        appLinkReturnUrl,
      }),
    ).toThrow(message)
  })
})
