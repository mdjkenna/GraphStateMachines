package mdk.gsm.graph

import mdk.gsm.state.BeforeVisitHandler
import mdk.gsm.state.OutgoingTransitionHandler

@PublishedApi
internal class VertexContainer<V, I, G, A> internal constructor(
    val vertex: V,
    val adjacentOrdered: List<Edge<V, I, G, A>>,
    val beforeVisitHandler: BeforeVisitHandler<V, I, G, A>?,
    val outgoingTransitionHandler: OutgoingTransitionHandler<V, I, G, A>? = null
) where V : IVertex<I>
