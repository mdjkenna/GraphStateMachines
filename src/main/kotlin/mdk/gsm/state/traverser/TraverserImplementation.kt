package mdk.gsm.state.traverser

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.TransitionState

internal class TraverserImplementation<V, I, G, A> (
    private val traverserStateImpl: TraverserStateImplementation<V, I, G, A>,
    override val traverserDispatcher: TraverserDispatcher<V, I, G, A>
) : Traverser<V, I, G, A> where V : IVertex<I> {

    override val traverserState: TraverserState<V, I, G, A>
        get() = traverserStateImpl

    override val graph: Graph<V, I, G, A>
        get() = traverserStateImpl.graph

    override val current: StateFlow<TransitionState<V, I, A>>
        get() = traverserState.current

    override fun tracePath(): List<V> {
        return traverserState.tracePath()
    }

    override fun launchDispatch(action: GraphStateMachineAction<A>) {
        traverserDispatcher.launchDispatch(action)
    }

    override suspend fun dispatch(action: GraphStateMachineAction<A>) {
        traverserDispatcher.dispatch(action)
    }

    override suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction<A>): TransitionState<V, I, A> {
        return traverserDispatcher.dispatchAndAwaitResult(action)
    }

    override fun tearDown() {
        traverserDispatcher.tearDown()
    }
}
