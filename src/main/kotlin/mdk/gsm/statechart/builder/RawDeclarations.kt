package mdk.gsm.statechart.builder

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.HistoryType
import mdk.gsm.statechart.model.PseudoKind
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TransitionType
import mdk.gsm.statechart.model.TriggerDefinition
import mdk.gsm.statechart.StateChartActionHook
import mdk.gsm.statechart.StateChartGuardHook
import mdk.gsm.statechart.StateChartInvokeHook
import mdk.gsm.statechart.StateChartTransitionActionHook

internal class RawStateChart<V, I, G, E> where V : Vertex<I> {
    val root = RawExclusiveGraph<V, I, G, E>()

    private var nextDeclarationOrdinal = 0

    fun nextOrdinal(): Int {
        val ordinal = nextDeclarationOrdinal
        nextDeclarationOrdinal++
        return ordinal
    }
}

internal class RawExclusiveGraph<V, I, G, E> where V : Vertex<I> {
    var initial: EntryTarget<I>? = null
    val children = ArrayList<RawState<V, I, G, E>>()
    val pseudos = ArrayList<RawPseudo<V, I, G, E>>()
}

internal enum class RawStateKind {
    State,
    Parallel,
    Final
}

internal class RawState<V, I, G, E>(
    val vertex: V,
    val kind: RawStateKind,
    val declarationOrdinal: Int
) where V : Vertex<I> {
    val childGraph = RawExclusiveGraph<V, I, G, E>()
    val regions = ArrayList<RawRegion<V, I, G, E>>()
    val histories = ArrayList<RawHistory<V, I>>()
    val transitions = ArrayList<RawTransition<V, I, G, E>>()
    val onEntry = ArrayList<RawAction<V, I, G, E>>()
    val onExit = ArrayList<RawAction<V, I, G, E>>()
    val invokes = ArrayList<RawInvoke<G, E>>()
}


internal class RawPseudo<V, I, G, E>(
    val vertex: V,
    val kind: PseudoKind,
    val declarationOrdinal: Int
) where V : Vertex<I> {
    val transitions = ArrayList<RawTransition<V, I, G, E>>()
}

internal class RawRegion<V, I, G, E>(
    val name: String,
    val declarationOrdinal: Int,
    val graph: RawExclusiveGraph<V, I, G, E> = RawExclusiveGraph()
) where V : Vertex<I>

internal class RawHistory<V, I>(
    val vertex: V,
    val ownerStateId: I,
    val type: HistoryType,
    val defaultTarget: EntryTarget<I>?,
    val declarationOrdinal: Int
) where V : Vertex<I>

internal class RawTransition<V, I, G, E>(
    val sourceId: I,
    val trigger: TriggerDefinition<E, I>,
    var target: TargetSpec<I>,
    val guards: List<RawGuard<V, I, G, E>>,
    val actions: List<RawTransitionAction<V, I, G, E>>,
    val type: TransitionType,
    val order: Int,
    val declarationOrdinal: Int
) where V : Vertex<I>

internal class RawGuard<V, I, G, E>(
    val ordinal: Int,
    val evaluate: StateChartGuardHook<V, I, G, E>
) where V : Vertex<I>

internal class RawAction<V, I, G, E>(
    val ordinal: Int,
    val execute: StateChartActionHook<V, I, G, E>
) where V : Vertex<I>

internal class RawTransitionAction<V, I, G, E>(
    val ordinal: Int,
    val execute: StateChartTransitionActionHook<V, I, G, E>
) where V : Vertex<I>


internal class RawInvoke<G, E>(
    val invokeId: String,
    val ordinal: Int,
    val execute: StateChartInvokeHook<G, E>
)
