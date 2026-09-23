package com.reactnativepaypal

import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingRequestStoreTest {
  private val preferences = InMemorySharedPreferences()
  private val store = PendingRequestStore(preferences)

  @Test
  fun `consume hands out a stored request exactly once`() {
    store.store("request")

    assertEquals("request", store.consume())
    // A later launch must not replay a request that was already handled.
    assertNull(store.consume())
  }

  @Test
  fun `clear drops a request left behind by an earlier flow`() {
    store.store("stale")

    store.clear()

    assertNull(store.consume())
  }

  @Test
  fun `a request survives into a new store, as it must across a process death`() {
    store.store("request")

    val afterRelaunch = PendingRequestStore(preferences)

    assertEquals("request", afterRelaunch.consume())
  }

  @Test
  fun `a new request replaces the previous one`() {
    store.store("first")
    store.store("second")

    assertEquals("second", store.consume())
  }

  @Test
  fun `consume with nothing stored returns null`() {
    assertNull(store.consume())
  }
}

/** Only the string operations PendingRequestStore uses are implemented. */
private class InMemorySharedPreferences : SharedPreferences {
  private val values = mutableMapOf<String, String>()

  override fun getString(key: String, defValue: String?): String? = values[key] ?: defValue

  override fun contains(key: String): Boolean = key in values

  override fun edit(): SharedPreferences.Editor = Editor()

  override fun getAll(): MutableMap<String, *> = values.toMutableMap()

  override fun getStringSet(key: String, defValues: MutableSet<String>?) = unsupported()

  override fun getInt(key: String, defValue: Int) = unsupported()

  override fun getLong(key: String, defValue: Long) = unsupported()

  override fun getFloat(key: String, defValue: Float) = unsupported()

  override fun getBoolean(key: String, defValue: Boolean) = unsupported()

  override fun registerOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) = unsupported()

  override fun unregisterOnSharedPreferenceChangeListener(
    listener: SharedPreferences.OnSharedPreferenceChangeListener
  ) = unsupported()

  private inner class Editor : SharedPreferences.Editor {
    private val puts = mutableMapOf<String, String>()
    private val removals = mutableSetOf<String>()

    override fun putString(key: String, value: String?) = apply {
      if (value == null) removals += key else puts[key] = value
    }

    override fun remove(key: String) = apply { removals += key }

    override fun apply() {
      commit()
    }

    override fun commit(): Boolean {
      removals.forEach(values::remove)
      values.putAll(puts)
      return true
    }

    override fun putStringSet(key: String, values: MutableSet<String>?) = unsupported()

    override fun putInt(key: String, value: Int) = unsupported()

    override fun putLong(key: String, value: Long) = unsupported()

    override fun putFloat(key: String, value: Float) = unsupported()

    override fun putBoolean(key: String, value: Boolean) = unsupported()

    override fun clear() = unsupported()
  }
}

private fun unsupported(): Nothing = throw UnsupportedOperationException("not needed by PendingRequestStore")
