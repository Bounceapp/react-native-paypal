const path = require("path")

const { getDefaultConfig } = require("expo/metro-config")

const projectRoot = __dirname

const config = getDefaultConfig(__dirname)

// Force Metro to resolve react and react-native from the example app
config.resolver.resolveRequest = (context, moduleName, platform) => {
  // Always use the example app's copy of these critical packages
  if (
    moduleName === "react" ||
    moduleName === "react-native" ||
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
