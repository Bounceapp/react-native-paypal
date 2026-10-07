package com.reactnativepaypal

import android.content.Intent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PayPalFlowTest {
  private val preferences = InMemorySharedPreferences()
  private val store = PendingRequestStore(preferences)

  // What the fake Braintree was handed, and what it answers with.
  private val returns = mutableListOf<Pair<String, Intent>>()
  private var nextResult = "no result"

  private val flow = PayPalFlow(store) { pendingRequestString, intent ->
    returns += pendingRequestString to intent
    nextResult
  }

  @Test
  fun `a return settles the flow with Braintree's result`() = runTest {
    val authResult = requireNotNull(flow.start())
    flow.launched("request", Intent())
    nextResult = "declined"

    flow.onNewIntent(Intent())

    assertEquals("declined", authResult.await())
    assertEquals("request", returns.single().first)
  }

  @Test
  fun `a second call while one is running is rejected`() {
    requireNotNull(flow.start())

    assertNull(flow.start())
  }

  @Test
  fun `a finished flow lets the next one start`() {
    val authResult = requireNotNull(flow.start())
    flow.finish(authResult)

    requireNotNull(flow.start())
  }

  @Test
  fun `finishing an old flow does not end the current one`() {
    val old = requireNotNull(flow.start())
    flow.finish(old)
    requireNotNull(flow.start())

    flow.finish(old)

    assertNull(flow.start())
  }

  @Test
  fun `start drops a request left behind by an earlier flow`() {
    store.store("stale")

    flow.start()
    flow.onNewIntent(Intent())

    assertTrue(returns.isEmpty())
  }

  @Test
  fun `the request is consumed even when nothing is waiting, as after a process death`() {
    store.store("from the killed process")

    flow.onForeground(Intent())

    assertNull(store.consume())
    assertTrue(returns.isEmpty())
  }

  @Test
  fun `the flow settles once, even if both hooks fire`() = runTest {
    val authResult = requireNotNull(flow.start())
    flow.launched("request", Intent())
    nextResult = "declined"

    flow.onNewIntent(Intent())
    nextResult = "no result"
    flow.onForeground(Intent())

    assertEquals("declined", authResult.await())
    assertEquals(1, returns.size)
  }

  @Test
  fun `the intent the activity had at launch is not taken as the return`() {
    // e.g. a previous flow's success URL, kept by setIntent().
    val intentAtLaunch = Intent()
    requireNotNull(flow.start())
    flow.launched("request", intentAtLaunch)

    flow.onForeground(intentAtLaunch)

    assertNotSame(intentAtLaunch, returns.single().second)
  }

  @Test
  fun `a new intent on foreground is handed over as the return`() {
    requireNotNull(flow.start())
    flow.launched("request", Intent())
    val returnIntent = Intent()

    flow.onForeground(returnIntent)

    assertSame(returnIntent, returns.single().second)
  }

  @Test
  fun `a return without a launched request is ignored`() {
    val authResult = requireNotNull(flow.start())

    flow.onNewIntent(Intent())

    assertTrue(returns.isEmpty())
    assertFalse(authResult.isCompleted)
  }
}
