package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.StateChartActionHook
import mdk.gsm.statechart.StateChartGuardHook
import mdk.gsm.statechart.StateChartTransitionActionHook

data class TransitionDefinition<V, I, G, E>(
    val id: TransitionId,
    val sourceId: I,
    val trigger: TriggerDefinition<E, I>,
    val target: TargetSpec<I>,
    val type: TransitionType,
    val guards: List<GuardDefinition<V, I, G, E>>,
    val actions: List<TransitionActionDefinition<V, I, G, E>>,
    val order: Int,
    val declarationOrdinal: Int
) where V : Vertex<I>

sealed interface TargetSpec<out I> {
    data object None : TargetSpec<Nothing>

    data class Single<I>(val id: I) : TargetSpec<I>

    data class Multiple<I>(val ids: List<I>) : TargetSpec<I>
}

sealed interface TriggerDefinition<out E, out I> {
    data class Exact<E>(val event: E) : TriggerDefinition<E, Nothing>

    data class Predicate<E>(val predicate: (E) -> Boolean) : TriggerDefinition<E, Nothing>

    data object Eventless : TriggerDefinition<Nothing, Nothing>

    data class DoneState<I>(val stateId: I) : TriggerDefinition<Nothing, I>

    data class DoneInvoke(val invokeId: String) : TriggerDefinition<Nothing, Nothing>

    data class ErrorInvoke(val invokeId: String) : TriggerDefinition<Nothing, Nothing>

    data class Timer(val timerId: String, val delayMillis: Long? = null) : TriggerDefinition<Nothing, Nothing>
}

enum class TransitionType {
    External,
    Internal
}

data class GuardDefinition<V, I, G, E>(
    val ordinal: Int,
    val evaluate: StateChartGuardHook<V, I, G, E>
) where V : Vertex<I>

data class ActionDefinition<V, I, G, E>(
    val ordinal: Int,
    val execute: StateChartActionHook<V, I, G, E>
) where V : Vertex<I>

data class TransitionActionDefinition<V, I, G, E>(
    val ordinal: Int,
    val execute: StateChartTransitionActionHook<V, I, G, E>
) where V : Vertex<I>
