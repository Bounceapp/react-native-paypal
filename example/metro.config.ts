import path from "node:path"

import { getDefaultConfig, type MetroConfig } from "expo/metro-config"

const projectRoot = import.meta.dirname
const workspaceRoot = path.resolve(projectRoot, "..")

const defaultConfig = getDefaultConfig(projectRoot)

// Packages that must exist once: React, React Native, and the library's peers.
const singletons = new Set([
  "react",
  "react-dom",
  "react-native",
  "react-native-web",
  "expo",
])
const appEntry = path.join(projectRoot, "index.ts")

const config: MetroConfig = {
  ...defaultConfig,
  // The library's source lives outside the app. Watching the whole repo root breaks
  // the file map under pnpm (the example's node_modules symlinks point into the root
  // one), so add only the source folder to the folders Expo already computes.
  watchFolders: [
    ...defaultConfig.watchFolders,
    path.join(workspaceRoot, "src"),
  ],
  resolver: {
    ...defaultConfig.resolver,
    resolveRequest: (context, moduleName, platform) => {
      const name = moduleName.startsWith("@")
        ? moduleName.split("/").slice(0, 2).join("/")
        : moduleName.split("/")[0]
      if (name && singletons.has(name)) {
        // Resolve as if the app imported it, so every file gets the app's copy.
        return context.resolveRequest(
          { ...context, originModulePath: appEntry },
          moduleName,
          platform,
        )
      }
      return context.resolveRequest(context, moduleName, platform)
    },
  },
}

export default config
