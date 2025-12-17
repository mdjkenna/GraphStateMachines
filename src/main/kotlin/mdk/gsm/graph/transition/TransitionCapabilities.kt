package mdk.gsm.graph.transition

import mdk.gsm.graph.IVertex

internal data class TransitionCapabilities<V, I, G, A>(
    val forward: IForwardTransition<V, I, G, A>,
    val previous: IPreviousTransition<V, I, G, A>,
    val resettable: IResettable<V>,
    val pathTraceable: IPathTraceable<V>
) where V : IVertex<I>

