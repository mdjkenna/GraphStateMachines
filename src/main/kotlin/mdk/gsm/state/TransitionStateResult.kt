package mdk.gsm.state

import mdk.gsm.graph.Vertex

internal class TransitionStateResult<V, I, G, A>(
    val transitionState: TransitionState<V, I, A>,
    val stateEntryListener: StateEntryListener<V, I, G, A>?,
    val machineContext: G?
) where V : Vertex<I>
