package mdk.gsm.statechart

import mdk.gsm.graph.Vertex

typealias StateChartGuardHook<V, I, G, E> =
        suspend StateChartGuardScope<V, I, G, E>.() -> Boolean

typealias StateChartBeforeTransitionHook<V, I, G, E> =
        suspend StateChartBeforeTransitionScope<V, I, G, E>.() -> Unit

typealias StateChartActionHook<V, I, G, E> =
        suspend StateChartActionScope<V, I, G, E>.() -> Unit

typealias StateChartTransitionActionHook<V, I, G, E> =
        suspend StateChartTransitionActionScope<V, I, G, E>.() -> Unit

typealias StateChartInvokeHook<G, E> =
        suspend StateChartInvokeScope<G, E>.() -> Unit

class StateChartGuardScope<V, I, G, E>(
    val source: V,
    val event: E?,
    val machineContext: G?
) where V : Vertex<I>

class StateChartBeforeTransitionScope<V, I, G, E>(
    val source: V,
    val event: E?,
    val machineContext: G?
) where V : Vertex<I> {

    internal var blocked: Boolean = false
        private set

    fun blockTransition() {
        blocked = true
    }
}

class StateChartActionScope<V, I, G, E>(
    val state: V,
    val event: E?,
    val machineContext: G?,
    private val raiseInternalEvent: (E) -> Unit,
    private val raiseTimerEvent: (String) -> Unit = {},
    private val raiseDoneInvokeEvent: (String) -> Unit = {},
    private val raiseErrorInvokeEvent: (String, Throwable?) -> Unit = { _, _ -> },
    private val scheduleTimerEvent: (String, Long) -> Unit = { _, _ -> },
    private val cancelTimerEvent: (String) -> Unit = {}
) where V : Vertex<I> {

    fun raise(event: E) {
        raiseInternalEvent(event)
    }

    fun raiseTimer(timerId: String) {
        raiseTimerEvent(timerId)
    }

    fun raiseDoneInvoke(invokeId: String) {
        raiseDoneInvokeEvent(invokeId)
    }

    fun raiseErrorInvoke(invokeId: String, cause: Throwable? = null) {
        raiseErrorInvokeEvent(invokeId, cause)
    }

    fun scheduleTimer(timerId: String, delayMillis: Long) {
        scheduleTimerEvent(timerId, delayMillis)
    }

    fun cancelTimer(timerId: String) {
        cancelTimerEvent(timerId)
    }
}

class StateChartTransitionActionScope<V, I, G, E>(
    val source: V,
    val targetIds: List<I>,
    val event: E?,
    val machineContext: G?,
    private val raiseInternalEvent: (E) -> Unit,
    private val raiseTimerEvent: (String) -> Unit = {},
    private val raiseDoneInvokeEvent: (String) -> Unit = {},
    private val raiseErrorInvokeEvent: (String, Throwable?) -> Unit = { _, _ -> },
    private val scheduleTimerEvent: (String, Long) -> Unit = { _, _ -> },
    private val cancelTimerEvent: (String) -> Unit = {}
) where V : Vertex<I> {

    fun raise(event: E) {
        raiseInternalEvent(event)
    }

    fun raiseTimer(timerId: String) {
        raiseTimerEvent(timerId)
    }

    fun raiseDoneInvoke(invokeId: String) {
        raiseDoneInvokeEvent(invokeId)
    }

    fun raiseErrorInvoke(invokeId: String, cause: Throwable? = null) {
        raiseErrorInvokeEvent(invokeId, cause)
    }

    fun scheduleTimer(timerId: String, delayMillis: Long) {
        scheduleTimerEvent(timerId, delayMillis)
    }

    fun cancelTimer(timerId: String) {
        cancelTimerEvent(timerId)
    }
}


class StateChartInvokeScope<G, E>(
    val invokeId: String,
    val machineContext: G?,
    private val sendEvent: (E) -> Unit
) {
    fun send(event: E) {
        sendEvent(event)
    }

    fun raise(event: E) {
        send(event)
    }
}
