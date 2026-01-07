package mdk.gsm.graph

import mdk.gsm.state.*

@PublishedApi
internal class VertexContainer<V, I, G, A> internal constructor(
    val vertex: V,
    val adjacentOrdered: List<Edge<V, I, G, A>>,
    val beforeEntryHook: BeforeEntryHook<V, I, G, A>?,
    val beforeExitHook: BeforeExitHook<V, I, G, A>?,
    val onEntryHook: OnEntryHook<V, I, G, A>?
) : StateEntryListener<V, I, G, A> where V : IVertex<I> {

    override suspend fun onStateEntered(machineContext: G?, args: A?) {
        onEntryHook?.let { hook ->
            val scope = OnEntryScope(
                vertex = vertex,
                machineContext = machineContext,
                args = args
            )
            hook.invoke(scope)
        }
    }
}
