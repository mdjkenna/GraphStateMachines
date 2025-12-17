package mdk.gsm.state.traverser

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.state.GsmController
import mdk.gsm.state.TransitionState


internal class TraverserStateImplementation<V, I, G, A> private constructor(
    val gsm: GsmController<V, I, G, A>
) : TraverserState<V, I, G, A> where V : IVertex<I> {

    val graph: Graph<V, I, G, A>
        get() = gsm.graph

    override val current: StateFlow<TransitionState<V, I, A>>
        get() = gsm.stateOut

    override fun tracePath(): List<V> {
        return gsm.tracePath()
    }

    companion object {
        internal fun <V, I, G, A> create(
            gsm: GsmController<V, I, G, A>
        ): TraverserStateImplementation<V, I, G, A> where V : IVertex<I> {
            return TraverserStateImplementation(gsm)
        }
    }
}
