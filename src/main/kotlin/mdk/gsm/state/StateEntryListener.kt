package mdk.gsm.state

import mdk.gsm.graph.IVertex

internal interface StateEntryListener<V, I, G, A> where V : IVertex<I> {
    suspend fun onStateEntered(machineContext: G?, args: A?)
}
