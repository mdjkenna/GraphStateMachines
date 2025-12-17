package mdk.gsm.state.walker

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mdk.gsm.action.CompletableAction
import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.GsmController
import mdk.gsm.state.TransitionState

/**
 * Implementation of the [WalkerDispatcher] interface.
 *
 * This class provides methods to dispatch forward-only actions to the walker.
 * Unlike [mdk.gsm.state.traverser.TraverserDispatcher], it only supports Next, NextArgs, and Reset actions.
 *
 * @param V The type of vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param G The type of traversal guard state, which controls conditional edge traversal. Must implement [mdk.gsm.state.ITransitionGuardState].
 * @param A The type of action arguments that can be passed when dispatching actions.
 */
internal class WalkerDispatcherImplementation<V, I, G, A> private constructor(
    private val scope: CoroutineScope,
    private val actionChannel: Channel<CompletableAction<V, I, A>>
) : WalkerDispatcher<V, I, G, A> where V : IVertex<I> {

    override fun launchDispatch(action: GraphStateMachineAction.Next) {
        scope.launch {
            dispatch(action)
        }
    }

    override fun launchDispatch(action: GraphStateMachineAction.NextArgs<A>) {
        scope.launch {
            dispatch(action)
        }
    }

    override fun launchDispatch(action: GraphStateMachineAction.Reset) {
        scope.launch {
            dispatch(action)
        }
    }

    override suspend fun dispatch(action: GraphStateMachineAction.Next) {
        actionChannel.send(CompletableAction(action, CompletableDeferred()))
    }

    override suspend fun dispatch(action: GraphStateMachineAction.NextArgs<A>) {
        actionChannel.send(CompletableAction(action, CompletableDeferred()))
    }

    override suspend fun dispatch(action: GraphStateMachineAction.Reset) {
        actionChannel.send(CompletableAction(action, CompletableDeferred()))
    }

    override suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.Next): TransitionState<V, I, A> {
        val completableAction = CompletableAction<V, I, A>(action, CompletableDeferred())
        dispatchInternal(completableAction)
        return completableAction.deferred.await()
    }

    override suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.NextArgs<A>): TransitionState<V, I, A> {
        val completableAction = CompletableAction<V, I, A>(action, CompletableDeferred())
        dispatchInternal(completableAction)
        return completableAction.deferred.await()
    }

    override suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.Reset): TransitionState<V, I, A> {
        val completableAction = CompletableAction<V, I, A>(action, CompletableDeferred())
        dispatchInternal(completableAction)
        return completableAction.deferred.await()
    }

    private suspend fun dispatchInternal(completableAction: CompletableAction<V, I, A>) {
        actionChannel.send(completableAction)
    }

    override fun tearDown() {
        scope.cancel()
    }

    private suspend inline fun withAction(
        crossinline block: suspend () -> Unit
    ) {
        block()
    }

    companion object {
        internal fun <V, I, G, A> create(
            gsm: GsmController<V, I, G, A>,
            singleThreadedScope: CoroutineScope,
            actionChannel: Channel<CompletableAction<V, I, A>> = Channel(Channel.UNLIMITED)
        ): WalkerDispatcherImplementation<V, I, G, A> where V : IVertex<I> {

            val walkerDispatcherImplementation: WalkerDispatcherImplementation<V, I, G, A> = WalkerDispatcherImplementation(
                singleThreadedScope,
                actionChannel
            )

            singleThreadedScope.launch {
                for (action in actionChannel) {
                    walkerDispatcherImplementation.withAction() {
                        gsm.dispatch(action)
                    }
                }
            }

            return walkerDispatcherImplementation
        }
    }
}
