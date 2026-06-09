package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex

sealed interface ParentLocation<out I> {
    data object Root : ParentLocation<Nothing>

    data class Compound<I>(val ownerStateId: I) : ParentLocation<I>

    data class Region<I>(val regionId: RegionId<I>) : ParentLocation<I>
}

sealed interface StateNode<V, I, G, E> where V : Vertex<I> {
    val vertex: V
    val id: I
        get() = vertex.id
    val parent: ParentLocation<I>
    val outgoingTransitionIds: List<TransitionId>
    val onEntry: List<ActionDefinition<V, I, G, E>>
    val onExit: List<ActionDefinition<V, I, G, E>>
    val declarationOrdinal: Int
}

data class AtomicStateNode<V, I, G, E>(
    override val vertex: V,
    override val parent: ParentLocation<I>,
    override val outgoingTransitionIds: List<TransitionId>,
    override val onEntry: List<ActionDefinition<V, I, G, E>>,
    override val onExit: List<ActionDefinition<V, I, G, E>>,
    override val declarationOrdinal: Int
) : StateNode<V, I, G, E> where V : Vertex<I>

data class CompoundStateNode<V, I, G, E>(
    override val vertex: V,
    val childGraphId: LocalGraphId,
    val historyMarkerIds: List<I>,
    override val parent: ParentLocation<I>,
    override val outgoingTransitionIds: List<TransitionId>,
    override val onEntry: List<ActionDefinition<V, I, G, E>>,
    override val onExit: List<ActionDefinition<V, I, G, E>>,
    override val declarationOrdinal: Int
) : StateNode<V, I, G, E> where V : Vertex<I>

data class ParallelStateNode<V, I, G, E>(
    override val vertex: V,
    val regionIds: List<RegionId<I>>,
    val historyMarkerIds: List<I>,
    override val parent: ParentLocation<I>,
    override val outgoingTransitionIds: List<TransitionId>,
    override val onEntry: List<ActionDefinition<V, I, G, E>>,
    override val onExit: List<ActionDefinition<V, I, G, E>>,
    override val declarationOrdinal: Int
) : StateNode<V, I, G, E> where V : Vertex<I>

data class FinalStateNode<V, I, G, E>(
    val vertex: V,
    val parent: ParentLocation<I>,
    val onEntry: List<ActionDefinition<V, I, G, E>>,
    val onExit: List<ActionDefinition<V, I, G, E>>,
    val declarationOrdinal: Int
) where V : Vertex<I> {
    val id: I
        get() = vertex.id
}
