package mdk.gsm.graph.transition

import mdk.gsm.graph.IVertex
import mdk.gsm.state.*

internal class TransitionMediator<V, I, G, A>(
    private val forwardTransition: IForwardTransition<V, I, G, A>,
    private val previousTransition: IPreviousTransition<V, I, G, A>,
    private val resettable: IResettable<V>,
    private val pathTraceable: IPathTraceable<V>,
    private val transitionGuardState: G?,
    private val gsmConfig: GsmConfig
) where V : IVertex<I> {

    var currentBounds = TransitionBounds.WithinBounds
    var currentArgs: A? = null

    private suspend inline fun returnTransitionState(
        crossinline transitionStateProvider : suspend () -> TransitionState<V, I, A>
    ) : TransitionState<V, I, A> {
        val transitionState = transitionStateProvider()
        currentBounds = transitionState.transitionBounds
        currentArgs = transitionState.args

        return transitionState
    }

    fun initialReadStartVertex() : TransitionState<V, I, A> {
        return TransitionState(
            vertex = forwardTransition.currentStep(),
            transitionBounds = currentBounds,
            args = null
        )
    }

    suspend fun handleNext(
        args : A? = null
    ): TransitionState<V, I, A> = returnTransitionState {

        if (gsmConfig.explicitlyMoveIntoBounds && currentBounds == TransitionBounds.BeforeFirst) {
            return@returnTransitionState TransitionState(
                vertex = forwardTransition.currentStep(),
                transitionBounds = TransitionBounds.WithinBounds,
                args = args
            )
        }

        var autoAdvanceIteration = -1
        var result : TransitionState<V, I, A>?

        while (true) {
            autoAdvanceIteration++
            val container = forwardTransition.getVertexContainer(forwardTransition.currentStep().id)

            if (container == null) {
                result = TransitionState(
                    vertex = forwardTransition.currentStep(),
                    transitionBounds = TransitionBounds.BeyondLast,
                    args = args
                )
                break
            }

            if (container.outgoingTransitionHandler != null) {
                val outgoingTransitionScope = OutgoingTransitionScope(args, transitionGuardState, container.vertex)
                container.outgoingTransitionHandler(outgoingTransitionScope)

                if (outgoingTransitionScope.noChange) {
                    return@returnTransitionState TransitionState(
                        vertex = forwardTransition.currentStep(),
                        transitionBounds = currentBounds,
                        args = currentArgs
                    )
                }
            }

            val traversalNode = forwardTransition.moveNext(
                guardState = transitionGuardState,
                autoAdvance = autoAdvanceIteration > 0,
                args = args
            )

            if (traversalNode == null) {
                result = TransitionState(
                    vertex = forwardTransition.currentStep(),
                    transitionBounds = TransitionBounds.BeyondLast,
                    args = args
                )
                break
            }

            val nextVertex = traversalNode.vertex
            val handler = forwardTransition.getVertexContainer(nextVertex.id)
                ?.beforeVisitHandler

            if (handler != null) {
                val scope = BeforeVisitScope(
                    vertex = nextVertex,
                    guardState = transitionGuardState,
                    args = args
                )

                handler.invoke(scope)
                if (scope.autoAdvanceTrigger) {
                    continue
                }
            }

            result = TransitionState(
                vertex = nextVertex,
                transitionBounds = TransitionBounds.WithinBounds,
                args = args
            )

            break
        }

        return@returnTransitionState result
    }

    suspend fun handlePrevious(): TransitionState<V, I, A> = returnTransitionState {
        if (gsmConfig.explicitlyMoveIntoBounds && currentBounds == TransitionBounds.BeyondLast) {
            return@returnTransitionState TransitionState(
                vertex = forwardTransition.currentStep(),
                transitionBounds = TransitionBounds.WithinBounds,
                args = forwardTransition.head().args
            )
        }

        val previous = previousTransition.movePrevious()
        if (previous == null) {
            TransitionState(
                vertex = forwardTransition.currentStep(),
                transitionBounds = TransitionBounds.BeforeFirst,
                args = null
            )
        } else {
            TransitionState(
                vertex = previous.vertex,
                transitionBounds = TransitionBounds.WithinBounds,
                args = previous.args
            )
        }
    }

    fun handleReset() : TransitionState<V, I, A> {
        (transitionGuardState as? ITransitionGuardState)?.onReset()
        return TransitionState(
            vertex = resettable.reset(),
            transitionBounds = TransitionBounds.WithinBounds,
            args = null
        )
    }

    fun tracePath(): List<V> {
        return pathTraceable.tracePath()
    }

    companion object {
        fun <V, I, G, A> create(
            capabilities: TransitionCapabilities<V, I, G, A>,
            transitionGuardState: G?,
            gsmConfig: GsmConfig
        ): TransitionMediator<V, I, G, A> where V : IVertex<I> {
            return TransitionMediator(
                forwardTransition = capabilities.forward,
                previousTransition = capabilities.previous,
                resettable = capabilities.resettable,
                pathTraceable = capabilities.pathTraceable,
                transitionGuardState = transitionGuardState,
                gsmConfig = gsmConfig
            )
        }
    }
}