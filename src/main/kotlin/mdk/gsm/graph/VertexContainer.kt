package mdk.gsm.graph

import mdk.gsm.state.BeforeEntryHook
import mdk.gsm.state.BeforeExitHook

@PublishedApi
internal class VertexContainer<V, I, G, A> internal constructor(
    val vertex: V,
    val adjacentOrdered: List<Edge<V, I, G, A>>,
    val beforeEntryHook: BeforeEntryHook<V, I, G, A>?,
    val beforeExitHook: BeforeExitHook<V, I, G, A>?
) where V : IVertex<I>
