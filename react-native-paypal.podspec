require "json"

package = JSON.parse(File.read(File.join(__dir__, "package.json")))

Pod::Spec.new do |s|
  s.name           = "react-native-paypal"
  s.version        = package["version"]
  s.summary        = package["description"]
  s.homepage       = package["homepage"]
  s.license        = package["license"]
  s.authors        = package["author"]

  # Braintree iOS v7 requires 16.0. Expo SDK 56 apps are already at 16.4.
  s.platforms      = { :ios => "16.0" }
  s.swift_version  = "5.9"
  # release-it tags releases as v${version}.
  s.source         = { :git => "https://github.com/Bounceapp/react-native-paypal.git", :tag => "v#{s.version}" }
  s.static_framework = true

  s.dependency "ExpoModulesCore"
  s.dependency "Braintree", "~> 7.12.0"

  # Swift/Objective-C compatibility
  s.pod_target_xcconfig = {
    "DEFINES_MODULE" => "YES",
  }

  s.source_files = "ios/**/*.{h,m,mm,swift}"
end
