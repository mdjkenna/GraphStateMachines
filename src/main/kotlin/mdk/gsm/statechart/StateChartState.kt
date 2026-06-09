package mdk.gsm.statechart

import mdk.gsm.graph.Vertex

data class StateChartState<out V, I>(
    val activeTree: ActiveStateTree<V, I>
) where V : Vertex<I>

data class ActiveStateTree<out V, I>(
    val root: ActiveStateNode<V, I>
) where V : Vertex<I>

sealed interface ActiveStateNode<out V, I> where V : Vertex<I> {
    val vertex: V
}

data class ActiveAtomicState<out V, I>(
    override val vertex: V
) : ActiveStateNode<V, I> where V : Vertex<I>

data class ActiveFinalState<out V, I>(
    override val vertex: V
) : ActiveStateNode<V, I> where V : Vertex<I>

data class ActiveOrState<out V, I>(
    override val vertex: V,
    val activeChild: ActiveStateNode<V, I>
) : ActiveStateNode<V, I> where V : Vertex<I>

data class ActiveParallelState<out V, I>(
    override val vertex: V,
    val activeRegions: List<ActiveRegion<V, I>>
) : ActiveStateNode<V, I> where V : Vertex<I>

data class ActiveRegion<out V, I>(
    val name: String,
    val activeChild: ActiveStateNode<V, I>
) where V : Vertex<I>
