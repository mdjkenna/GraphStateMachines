package mdk.gsm.statechart

import kotlinx.coroutines.CompletableDeferred
import mdk.gsm.graph.Vertex

sealed interface StateChartDispatch<out E> {
    data class Event<E>(val event: E) : StateChartDispatch<E>

    data class Timer(val timerId: String) : StateChartDispatch<Nothing>

    data class DoneInvoke(val invokeId: String) : StateChartDispatch<Nothing>

    data class ErrorInvoke(val invokeId: String, val cause: Throwable? = null) : StateChartDispatch<Nothing>

    data object Reset : StateChartDispatch<Nothing>
}

internal data class CompletableStateChartDispatch<V, I, E>(
    val dispatch: StateChartDispatch<E>,
    val deferred: CompletableDeferred<StateChartState<V, I>>
) where V : Vertex<I>
