package com.reactnativepaypal

import android.content.SharedPreferences

/**
 * Persists Braintree's pending request across the browser switch.
 *
 * Android can kill the app while the buyer is in PayPal, so the request cannot
 * live in memory. The flip side is that a request which outlives its flow must
 * never be handled twice: [consume] hands it out at most once, and [clear]
 * drops one left behind by an earlier flow.
 */
internal class PendingRequestStore(private val preferences: SharedPreferences) {
  fun store(pendingRequestString: String) {
    preferences.edit().putString(KEY, pendingRequestString).apply()
  }

  fun clear() {
    preferences.edit().remove(KEY).apply()
  }

  /** Returns the stored request and removes it, so it is handled at most once. */
  fun consume(): String? {
    val pendingRequestString = preferences.getString(KEY, null) ?: return null
    clear()
    return pendingRequestString
  }

  private companion object {
    const val KEY = "pending_request"
  }
}
