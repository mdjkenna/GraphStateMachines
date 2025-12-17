@file:Suppress("unused")

package mdk.gsm.state

import kotlinx.coroutines.flow.MutableStateFlow
import mdk.gsm.action.CompletableAction
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.transition.TransitionMediator

internal class GsmController<V, I, G, A> internal constructor(
    private val graphTransitionMediator: TransitionMediator<V, I, G, A>,
    internal val graph: mdk.gsm.graph.Graph<V, I, G, A>
) where V : IVertex<I> {

    val stateOut = MutableStateFlow<TransitionState<V, I, A>>(
        graphTransitionMediator.initialReadStartVertex()
    )

    fun tracePath(): List<V> {
        return graphTransitionMediator.tracePath()
    }

    suspend fun dispatch(completableAction: CompletableAction<V, I, A>) {

        when (val action = completableAction.action) {
            GraphStateMachineAction.Next -> {
                updateState(completableAction) {
                    graphTransitionMediator.handleNext()
                }
            }

            GraphStateMachineAction.Previous -> {
                updateState(completableAction) {
                   graphTransitionMediator.handlePrevious()
                }
            }

            GraphStateMachineAction.Reset -> {
                updateState(completableAction) {
                   graphTransitionMediator.handleReset()
                }
            }

            is GraphStateMachineAction.NextArgs<A> -> {
                updateState(completableAction) {
                    graphTransitionMediator.handleNext(action.args)
                }
            }
        }
    }

    private suspend inline fun updateState(
        completableAction: CompletableAction<V, I, A>,
        crossinline update : suspend () -> TransitionState<V, I, A>
    ) {
        val newState = update()
        stateOut.value = newState

        completableAction.deferred.complete(newState)
    }
}


internal class GsmConfig(
    val explicitlyMoveIntoBounds : Boolean
)
