package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex

data class TargetIndex<V, I, G, E>(
    val states: Map<I, StateNode<V, I, G, E>>,
    val finals: Map<I, FinalStateNode<V, I, G, E>>,
    val histories: Map<I, HistoryMarker<V, I>>,
    val pseudos: Map<I, PseudoNode<V, I, G, E>>
) where V : Vertex<I> {

    fun resolve(id: I): TargetResolution<V, I, G, E>? {
        states[id]?.let { return TargetResolution.State(it) }
        finals[id]?.let { return TargetResolution.Final(it) }
        histories[id]?.let { return TargetResolution.History(it) }
        pseudos[id]?.let { return TargetResolution.Pseudo(it) }
        return null
    }
}

sealed interface TargetResolution<V, I, G, E> where V : Vertex<I> {
    data class State<V, I, G, E>(
        val node: StateNode<V, I, G, E>
    ) : TargetResolution<V, I, G, E> where V : Vertex<I>

    data class Final<V, I, G, E>(
        val node: FinalStateNode<V, I, G, E>
    ) : TargetResolution<V, I, G, E> where V : Vertex<I>

    data class History<V, I, G, E>(
        val marker: HistoryMarker<V, I>
    ) : TargetResolution<V, I, G, E> where V : Vertex<I>

    data class Pseudo<V, I, G, E>(
        val node: PseudoNode<V, I, G, E>
    ) : TargetResolution<V, I, G, E> where V : Vertex<I>
}

data class ContainmentIndex<I>(
    val parentByStateId: Map<I, ParentLocation<I>>,
    val owningGraphByChildId: Map<I, LocalGraphId>,
    val pathByStateId: Map<I, StatePath<I>>,
    val descendantsByStateId: Map<I, Set<I>>,
    val regionsByParallelStateId: Map<I, List<RegionId<I>>>
)

data class StatePath<I>(
    val stateIds: List<I>
) {
    val leafStateId: I
        get() = stateIds.last()
}

data class EntryIndex<I>(
    val initialTargetByGraphId: Map<LocalGraphId, EntryTarget<I>>,
    val graphByCompoundStateId: Map<I, LocalGraphId>,
    val regionsByParallelStateId: Map<I, List<RegionId<I>>>,
    val graphByRegionId: Map<RegionId<I>, LocalGraphId>
)

data class DeclarationIndex<I>(
    val stateOrdinalById: Map<I, Int>,
    val transitionOrdinalById: Map<TransitionId, Int>,
    val regionOrdinalById: Map<RegionId<I>, Int>,
    val localGraphOrdinalById: Map<LocalGraphId, Int>
)
