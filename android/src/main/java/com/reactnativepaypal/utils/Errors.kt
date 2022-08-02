package com.reactnativepaypal.utils

import com.facebook.react.bridge.WritableMap
import com.facebook.react.bridge.WritableNativeMap

enum class ErrorType {
  Failed, Canceled, Unknown
}

internal fun createError(code: String, message: String?): WritableMap {
  val map: WritableMap = WritableNativeMap()
  val details: WritableMap = WritableNativeMap()
  details.putString("code", code)
  details.putString("message", message)

  map.putMap("error", details)
  return map
}
