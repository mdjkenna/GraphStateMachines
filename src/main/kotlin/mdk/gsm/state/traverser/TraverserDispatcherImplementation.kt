package mdk.gsm.state.traverser

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

internal class TraverserDispatcherImplementation<V, I, G, A> private constructor(
    private val scope: CoroutineScope,
    private val actionChannel: Channel<CompletableAction<V, I, A>>
) : TraverserDispatcher<V, I, G, A> where V : IVertex<I> {

    override fun launchDispatch(action: GraphStateMachineAction<A>) {
        scope.launch {
            dispatch(action)
        }
    }

    override suspend fun dispatch(action: GraphStateMachineAction<A>) {
        actionChannel.send(CompletableAction(action, CompletableDeferred()))
    }

    override suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction<A>) : TransitionState<V, I, A> {
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
            actionChannel: Channel<CompletableAction<V, I, A>>,
        ) : TraverserDispatcherImplementation<V, I, G, A> where V : IVertex<I> {

            val gsmDispatcherImpl: TraverserDispatcherImplementation<V, I, G, A> = TraverserDispatcherImplementation(
                singleThreadedScope,
                actionChannel
            )

            singleThreadedScope.launch {
                for (action in actionChannel) {
                    gsmDispatcherImpl.withAction() {
                        gsm.dispatch(action)
                    }
                }
            }

            return gsmDispatcherImpl
        }
    }
}
