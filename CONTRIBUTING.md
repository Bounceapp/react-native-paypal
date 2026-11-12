# Contributing

We want this community to be friendly and respectful to each other. Please follow it in all your interactions with the project.

## Development workflow

To get started with the project, install the tools the repo pins, then the dependencies:

```sh
mise install
corepack enable
pnpm install
```

[`mise`](https://mise.jdx.dev/) installs the Node version from `.tool-versions`, and `corepack enable` makes `pnpm` resolve to the version in `package.json`. `pnpm install` installs the dependencies of the library and of the example app (a pnpm workspace package), builds `lib/` and installs the git hooks.

While developing, you can run the [example app](/example/) to test your changes. It needs a development build, because the library has native code that Expo Go does not include: `pnpm example ios` and `pnpm example android` build one and install it on a simulator or emulator. Any changes you make in your library's JavaScript code are reflected in the example app without a rebuild, because Metro bundles the library from `src/`. If you change any native code, then you'll need to rebuild the example app.

To start the packager for a development build that is already installed:

```sh
pnpm example start
```

To run the example app on Android:

```sh
pnpm example android
```

To run the example app on iOS:

```sh
pnpm example ios
```

PayPal does not work on web, so `pnpm example web` is only useful to check that importing the library does not break a web bundle.

Make sure your code passes TypeScript, [oxlint](https://oxc.rs/docs/guide/usage/linter) and [oxfmt](https://oxc.rs/docs/guide/usage/formatter). Run the following to verify:

```sh
pnpm typecheck
pnpm lint
```

To fix lint and formatting errors, run the following:

```sh
pnpm lint:fix
```

To lint the Kotlin files with [detekt](https://detekt.dev/), run the following (it needs a JDK):

```sh
pnpm lint:kotlin
```

Remember to add tests for your change if possible. Run the unit tests by:

```sh
pnpm test
```

### End-to-end tests

The E2E tests drive the example app. The [Maestro](https://maestro.dev/) flows in `example/e2e/flows` run on iOS and Android, and the [Playwright](https://playwright.dev/) tests in `example/e2e/web` run against the web build. CI runs them all from `.github/workflows/e2e.yml`.

To run the Maestro flows, [install Maestro](https://docs.maestro.dev/getting-started/installing-maestro) (it needs a JDK), install the example app on a simulator or emulator with `pnpm example ios` or `pnpm example android`, keep `pnpm example start` running, and run:

```sh
pnpm e2e
```

The flows tagged `sandbox` open the real PayPal flow, so they need a [Braintree sandbox](https://sandbox.braintreegateway.com/) tokenization key for a merchant with PayPal enabled. Put it in `example/.env.local` (git-ignored), rebuild the app, and run them with `pnpm e2e:sandbox`:

```sh
EXPO_PUBLIC_PAYPAL_CLIENT_TOKEN=sandbox_xxxxxxxx_xxxxxxxxxxxxxxxx
```

On CI the key comes from the `PAYPAL_SANDBOX_TOKENIZATION_KEY` repository secret, and the sandbox flows are skipped without it.

To run the web tests, install Chromium once with `pnpm example exec playwright install chromium`, then:

```sh
pnpm e2e:web
```

To edit the Swift files, run `pnpm example ios` once to generate the native project, then open `example/ios/reactnativepaypalexample.xcworkspace` in Xcode and find the source files at `Pods > Development Pods > react-native-paypal`.

To edit the Kotlin files, run `pnpm example android` once to generate the native project, then open `example/android` in Android Studio and find the source files at `reactnativepaypal` under `Android`.

### Commit message convention

We follow the [conventional commits specification](https://www.conventionalcommits.org/en) for our commit messages:

- `fix`: bug fixes, e.g. fix crash due to deprecated method.
- `feat`: new features, e.g. add new method to the module.
- `refactor`: code refactor, e.g. migrate from class components to hooks.
- `docs`: changes into documentation, e.g. add usage example for the module..
- `test`: adding or updating tests, e.g. add integration tests using detox.
- `chore`: tooling changes, e.g. change CI config.

Our commit-msg hook (commitlint) verifies that your commit message matches this format when committing.

### Linting and tests

We use [TypeScript](https://www.typescriptlang.org/) for type checking, [oxlint](https://oxc.rs/docs/guide/usage/linter) for linting, [oxfmt](https://oxc.rs/docs/guide/usage/formatter) for formatting the code, [detekt](https://detekt.dev/) for linting the Kotlin code, and [Jest](https://jestjs.io/) for testing.

[lefthook](https://lefthook.dev/) runs the git hooks, configured in `lefthook.yml`. Our pre-commit hook runs oxlint and oxfmt on the staged files, then verifies that type checking and the tests pass. The Kotlin lint is not part of the hooks; run it yourself when you change Android code.

### Publishing to npm

We use [release-it](https://github.com/release-it/release-it) to make it easier to publish new versions. It handles common tasks like bumping version based on semver, creating tags and releases etc.

To publish new versions, run the following:

```sh
pnpm release
```

### Scripts

The `package.json` file contains various scripts for common tasks:

- `pnpm install` (or `pnpm bootstrap`): install all dependencies and build `lib/`.
- `pnpm typecheck`: type-check files with TypeScript.
- `pnpm lint`: lint files with oxlint and check formatting with oxfmt.
- `pnpm lint:fix`: fix lint and formatting errors.
- `pnpm lint:kotlin`: lint the Kotlin files with detekt.
- `pnpm test`: run unit tests with Jest.
- `pnpm e2e`: run the Maestro E2E flows against the installed example app (`pnpm e2e:sandbox` for the flows that need a Braintree sandbox key).
- `pnpm e2e:web`: run the Playwright E2E tests against the web build.
- `pnpm run docs`: generate the API reference with typedoc.
- `pnpm example start`: start the Metro server for the example app.
- `pnpm example android`: run the example app on Android.
- `pnpm example ios`: run the example app on iOS.

### Sending a pull request

> **Working on your first pull request?** You can learn how from this _free_ series: [How to Contribute to an Open Source Project on GitHub](https://app.egghead.io/playlists/how-to-contribute-to-an-open-source-project-on-github).

When you're sending a pull request:

- Prefer small pull requests focused on one change.
- Verify that linters and tests are passing.
- Review the documentation to make sure it looks good.
- Follow the pull request template when opening a pull request.
- For pull requests that change the API or implementation, discuss with maintainers first by opening an issue.

## Code of Conduct

### Our Pledge

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone, regardless of age, body size, visible or invisible disability, ethnicity, sex characteristics, gender identity and expression, level of experience, education, socio-economic status, nationality, personal appearance, race, religion, or sexual identity and orientation.

We pledge to act and interact in ways that contribute to an open, welcoming, diverse, inclusive, and healthy community.

### Our Standards

Examples of behavior that contributes to a positive environment for our community include:

- Demonstrating empathy and kindness toward other people
- Being respectful of differing opinions, viewpoints, and experiences
- Giving and gracefully accepting constructive feedback
- Accepting responsibility and apologizing to those affected by our mistakes, and learning from the experience
- Focusing on what is best not just for us as individuals, but for the overall community

Examples of unacceptable behavior include:

- The use of sexualized language or imagery, and sexual attention or
  advances of any kind
- Trolling, insulting or derogatory comments, and personal or political attacks
- Public or private harassment
- Publishing others' private information, such as a physical or email
  address, without their explicit permission
- Other conduct which could reasonably be considered inappropriate in a
  professional setting

### Enforcement Responsibilities

Community leaders are responsible for clarifying and enforcing our standards of acceptable behavior and will take appropriate and fair corrective action in response to any behavior that they deem inappropriate, threatening, offensive, or harmful.

Community leaders have the right and responsibility to remove, edit, or reject comments, commits, code, wiki edits, issues, and other contributions that are not aligned to this Code of Conduct, and will communicate reasons for moderation decisions when appropriate.

### Scope

This Code of Conduct applies within all community spaces, and also applies when an individual is officially representing the community in public spaces. Examples of representing our community include using an official e-mail address, posting via an official social media account, or acting as an appointed representative at an online or offline event.

### Enforcement

Instances of abusive, harassing, or otherwise unacceptable behavior may be reported to the community leaders responsible for enforcement at [INSERT CONTACT METHOD]. All complaints will be reviewed and investigated promptly and fairly.

All community leaders are obligated to respect the privacy and security of the reporter of any incident.

### Enforcement Guidelines

Community leaders will follow these Community Impact Guidelines in determining the consequences for any action they deem in violation of this Code of Conduct:

#### 1. Correction

**Community Impact**: Use of inappropriate language or other behavior deemed unprofessional or unwelcome in the community.

**Consequence**: A private, written warning from community leaders, providing clarity around the nature of the violation and an explanation of why the behavior was inappropriate. A public apology may be requested.

#### 2. Warning

**Community Impact**: A violation through a single incident or series of actions.

**Consequence**: A warning with consequences for continued behavior. No interaction with the people involved, including unsolicited interaction with those enforcing the Code of Conduct, for a specified period of time. This includes avoiding interactions in community spaces as well as external channels like social media. Violating these terms may lead to a temporary or permanent ban.

#### 3. Temporary Ban

**Community Impact**: A serious violation of community standards, including sustained inappropriate behavior.

**Consequence**: A temporary ban from any sort of interaction or public communication with the community for a specified period of time. No public or private interaction with the people involved, including unsolicited interaction with those enforcing the Code of Conduct, is allowed during this period. Violating these terms may lead to a permanent ban.

#### 4. Permanent Ban

**Community Impact**: Demonstrating a pattern of violation of community standards, including sustained inappropriate behavior, harassment of an individual, or aggression toward or disparagement of classes of individuals.

**Consequence**: A permanent ban from any sort of public interaction within the community.

### Attribution

This Code of Conduct is adapted from the [Contributor Covenant][homepage], version 2.0,
available at https://www.contributor-covenant.org/version/2/0/code_of_conduct.html.

Community Impact Guidelines were inspired by [Mozilla's code of conduct enforcement ladder](https://github.com/mozilla/diversity).

[homepage]: https://www.contributor-covenant.org

For answers to common questions about this code of conduct, see the FAQ at
https://www.contributor-covenant.org/faq. Translations are available at https://www.contributor-covenant.org/translations.
