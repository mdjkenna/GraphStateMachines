package mdk.gsm.state

import mdk.gsm.graph.Vertex

internal interface StateEntryListener<V, I, G, A> where V : Vertex<I> {
    suspend fun onStateEntered(machineContext: G?, args: A?)
}
