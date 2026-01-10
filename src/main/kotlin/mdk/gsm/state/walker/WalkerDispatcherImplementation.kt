package mdk.gsm.state.walker

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mdk.gsm.action.CompletableAction
import mdk.gsm.graph.Vertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.GsmController
import mdk.gsm.state.TransitionState

internal class WalkerDispatcherImplementation<V, I, G, A> private constructor(
    private val scope: CoroutineScope,
    private val actionChannel: Channel<CompletableAction<V, I, A>>
) : WalkerDispatcher<V, I, G, A> where V : Vertex<I> {

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
        ): WalkerDispatcherImplementation<V, I, G, A> where V : Vertex<I> {

            val walkerDispatcherImplementation: WalkerDispatcherImplementation<V, I, G, A> = WalkerDispatcherImplementation(
                singleThreadedScope,
                actionChannel
            )

            singleThreadedScope.launch {
                for (action in actionChannel) {
                    walkerDispatcherImplementation.withAction {
                        gsm.dispatch(action)
                    }
                }
            }

            return walkerDispatcherImplementation
        }
    }
}
