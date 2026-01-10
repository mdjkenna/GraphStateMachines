package mdk.gsm.state.walker

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Graph
import mdk.gsm.graph.Vertex
import mdk.gsm.state.GsmController
import mdk.gsm.state.TransitionState


internal class WalkerStateImplementation<V, I, G, A> private constructor(
    val gsm: GsmController<V, I, G, A>
) : WalkerState<V, I, A> where V : Vertex<I> {

    val graph: Graph<V, I, G, A>
        get() = gsm.graph

    override val current: StateFlow<TransitionState<V, I, A>>
        get() = gsm.stateOut

    companion object {
        internal fun <V, I, G, A> create(
            gsm: GsmController<V, I, G, A>
        ): WalkerStateImplementation<V, I, G, A> where V : Vertex<I> {
            return WalkerStateImplementation(gsm)
        }
    }
}