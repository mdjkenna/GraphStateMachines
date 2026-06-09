package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex

enum class PseudoKind {
    Initial,
    Choice,
    Junction,
    Fork,
    Join
}

data class PseudoNode<V, I, G, E>(
    val vertex: V,
    val kind: PseudoKind,
    val parent: ParentLocation<I>,
    val outgoingTransitionIds: List<TransitionId>,
    val declarationOrdinal: Int
) where V : Vertex<I> {
    val id: I
        get() = vertex.id
}
