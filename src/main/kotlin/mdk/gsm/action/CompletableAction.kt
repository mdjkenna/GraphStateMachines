package mdk.gsm.action

import kotlinx.coroutines.CompletableDeferred
import mdk.gsm.graph.Vertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.TransitionState

internal data class CompletableAction<V, I, A>(
    val action: GraphStateMachineAction<A>,
    val deferred: CompletableDeferred<TransitionState<V, I, A>>
) where V : Vertex<I>