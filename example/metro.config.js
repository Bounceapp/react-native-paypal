const path = require("path")

const { getDefaultConfig } = require("expo/metro-config")

const projectRoot = __dirname
const workspaceRoot = path.resolve(projectRoot, "..")

const config = getDefaultConfig(__dirname)

// Watch all files within the monorepo
config.watchFolders = [workspaceRoot]

// Let Metro know where to resolve packages
config.resolver.nodeModulesPaths = [
  path.resolve(projectRoot, "node_modules"),
  path.resolve(workspaceRoot, "node_modules"),
]

// Force Metro to resolve react, react-native, and react-native-svg from example app
config.resolver.resolveRequest = (context, moduleName, platform) => {
  // Always use the example app's copy of these critical packages
  if (
    moduleName === "react" ||
    moduleName === "react-native" ||
    moduleName === "react-native-svg" ||
    moduleName.startsWith("react/") ||
    moduleName.startsWith("react-native/")
  ) {
    const exampleNodeModules = path.resolve(projectRoot, "node_modules")

    return {
      type: "sourceFile",
      filePath: require.resolve(moduleName, {
        paths: [exampleNodeModules],
      }),
    }
  }

  // For all other modules, use the default resolution
  return context.resolveRequest(context, moduleName, platform)
}

module.exports = config
