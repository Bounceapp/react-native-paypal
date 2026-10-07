package com.reactnativepaypal

import android.content.Intent
import kotlinx.coroutines.CompletableDeferred

/**
 * The state of a billing agreement flow across the browser switch, and the
 * rules for settling it. Kept apart from [PaypalModule] so those rules can be
 * tested without Braintree or an activity: [handleReturn] stands in for
 * `PayPalLauncher.handleReturnToApp`, and [R] for its result.
 *
 * Confined to the main thread, like the module.
 */
internal class PayPalFlow<R>(
  private val pendingRequests: PendingRequestStore,
  private val handleReturn: (pendingRequestString: String, intent: Intent) -> R
) {
  // Non-null from the moment a request starts until it settles, so a second
  // call made in the meantime is rejected rather than silently replacing the
  // first.
  private var inFlight: CompletableDeferred<R>? = null

  // The activity's intent when the browser switch started. Browser-switch
  // matches a return on scheme and host only, so if this intent is itself an
  // old return URL -- a previous flow's success, kept by an activity that calls
  // setIntent(), or the launch intent after a process restart -- it would be
  // taken as this flow's result.
  private var intentAtLaunch: Intent? = null

  /** Starts a flow, or returns null if one is already running. */
  fun start(): CompletableDeferred<R>? {
    if (inFlight != null) {
      return null
    }
    val authResult = CompletableDeferred<R>()
    inFlight = authResult
    // A request left behind by an earlier flow must not be mistaken for this
    // one while it is still being created.
    pendingRequests.clear()
    return authResult
  }

  /**
   * Records a started browser switch. The request is persisted only so that a
   * process death during the browser flow can be cleaned up on relaunch.
   */
  fun launched(pendingRequestString: String, activityIntent: Intent?) {
    intentAtLaunch = activityIntent
    pendingRequests.store(pendingRequestString)
  }

  /** Ends the flow that [start] returned [authResult] for. */
  fun finish(authResult: CompletableDeferred<R>) {
    if (inFlight === authResult) {
      inFlight = null
      intentAtLaunch = null
    }
  }

  /** A singleTask or singleTop activity receives the return here. */
  fun onNewIntent(intent: Intent) {
    handleReturnToApp(intent)
  }

  /**
   * Every other launch mode returns here, as does a buyer who closes the
   * browser without finishing. An intent the activity already had at launch is
   * not a return, so it is swapped for an empty one, which Braintree
   * settles as `NoResult`.
   */
  fun onForeground(activityIntent: Intent) {
    handleReturnToApp(if (activityIntent === intentAtLaunch) Intent() else activityIntent)
  }

  private fun handleReturnToApp(intent: Intent) {
    // Consumed exactly once, whatever the outcome, so a stale request can never
    // be replayed on a later launch.
    val pendingRequestString = pendingRequests.consume() ?: return

    // Nothing is waiting after a process death: the JS that made the call is
    // gone, so the request is dropped and the buyer simply starts again.
    val authResult = inFlight ?: return
    authResult.complete(handleReturn(pendingRequestString, intent))
  }
}
