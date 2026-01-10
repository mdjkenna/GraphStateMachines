package mdk.gsm.graph.transition

import mdk.gsm.graph.Vertex
import mdk.gsm.graph.VertexContainer
import mdk.gsm.state.*

internal class TransitionMediator<V, I, G, A>(
    private val forwardTransition: IForwardTransition<V, I, G, A>,
    private val previousTransition: IPreviousTransition<V, I, G, A>,
    private val resettable: IResettable<V>,
    private val pathTraceable: IPathTraceable<V>,
    private val machineContext: G?,
    private val gsmConfig: GsmConfig
) where V : Vertex<I> {

    var currentBounds = TransitionBounds.WithinBounds
    var currentArgs: A? = null

    private suspend inline fun returnTransitionStateResult(
        crossinline transitionStateProvider : suspend () -> TransitionStateResult<V, I, G, A>
    ) : TransitionStateResult<V, I, G, A> {
        val result = transitionStateProvider()
        currentBounds = result.transitionState.transitionBounds
        currentArgs = result.transitionState.args

        return result
    }

    fun initialReadStartVertex() : TransitionState<V, I, A> {
        return TransitionState(
            vertex = forwardTransition.currentStep(),
            transitionBounds = currentBounds,
            args = null
        )
    }

    private fun createResult(
        transitionState: TransitionState<V, I, A>,
        vertexContainer: VertexContainer<V, I, G, A>?
    ): TransitionStateResult<V, I, G, A> {
        return TransitionStateResult(
            transitionState = transitionState,
            stateEntryListener = vertexContainer,
            machineContext = machineContext
        )
    }

    suspend fun handleNext(
        args : A? = null
    ): TransitionStateResult<V, I, G, A> = returnTransitionStateResult {

        if (gsmConfig.explicitlyMoveIntoBounds && currentBounds == TransitionBounds.BeforeFirst) {
            val container = forwardTransition.getVertexContainer(forwardTransition.currentStep().id)
            return@returnTransitionStateResult createResult(
                TransitionState(
                    vertex = forwardTransition.currentStep(),
                    transitionBounds = TransitionBounds.WithinBounds,
                    args = args
                ),
                container
            )
        }

        var autoAdvanceIteration = -1
        var result : TransitionStateResult<V, I, G, A>?

        while (true) {
            autoAdvanceIteration++
            val container = forwardTransition.getVertexContainer(forwardTransition.currentStep().id)

            if (container == null) {
                result = createResult(
                    TransitionState(
                        vertex = forwardTransition.currentStep(),
                        transitionBounds = TransitionBounds.BeyondLast,
                        args = args
                    ),
                    null
                )
                break
            }

            if (container.beforeExitHook != null) {
                val beforeExitScope = BeforeExitScope(args, machineContext, container.vertex)
                container.beforeExitHook.invoke(beforeExitScope)

                if (beforeExitScope.noChange) {
                    return@returnTransitionStateResult createResult(
                        TransitionState(
                            vertex = forwardTransition.currentStep(),
                            transitionBounds = currentBounds,
                            args = currentArgs
                        ),
                        null
                    )
                }
            }

            val traversalNode = forwardTransition.moveNext(
                machineContext = machineContext,
                autoAdvance = autoAdvanceIteration > 0,
                args = args
            )

            if (traversalNode == null) {
                result = createResult(
                    TransitionState(
                        vertex = forwardTransition.currentStep(),
                        transitionBounds = TransitionBounds.BeyondLast,
                        args = args
                    ),
                    null
                )
                break
            }

            val nextVertex = traversalNode.vertex
            val nextContainer = forwardTransition.getVertexContainer(nextVertex.id)
            val beforeEntryHook = nextContainer?.beforeEntryHook

            if (beforeEntryHook != null) {
                val scope = BeforeEntryScope(
                    vertex = nextVertex,
                    machineContext = machineContext,
                    args = args
                )

                beforeEntryHook.invoke(scope)
                if (scope.autoAdvanceTrigger) {
                    continue
                }
            }

            result = createResult(
                TransitionState(
                    vertex = nextVertex,
                    transitionBounds = TransitionBounds.WithinBounds,
                    args = args
                ),
                nextContainer
            )

            break
        }

        return@returnTransitionStateResult result
    }

    suspend fun handlePrevious(): TransitionStateResult<V, I, G, A> = returnTransitionStateResult {
        if (gsmConfig.explicitlyMoveIntoBounds && currentBounds == TransitionBounds.BeyondLast) {
            val container = forwardTransition.getVertexContainer(forwardTransition.currentStep().id)
            return@returnTransitionStateResult createResult(
                TransitionState(
                    vertex = forwardTransition.currentStep(),
                    transitionBounds = TransitionBounds.WithinBounds,
                    args = forwardTransition.head().args
                ),
                container
            )
        }

        val previous = previousTransition.movePrevious()
        if (previous == null) {
            createResult(
                TransitionState(
                    vertex = forwardTransition.currentStep(),
                    transitionBounds = TransitionBounds.BeforeFirst,
                    args = null
                ),
                null
            )
        } else {
            val container = forwardTransition.getVertexContainer(previous.vertex.id)
            createResult(
                TransitionState(
                    vertex = previous.vertex,
                    transitionBounds = TransitionBounds.WithinBounds,
                    args = previous.args
                ),
                container
            )
        }
    }

    fun handleReset() : TransitionStateResult<V, I, G, A> {
        (machineContext as? MachineContext)?.onReset()
        val resetVertex = resettable.reset()
        val container = forwardTransition.getVertexContainer(resetVertex.id)
        return createResult(
            TransitionState(
                vertex = resetVertex,
                transitionBounds = TransitionBounds.WithinBounds,
                args = null
            ),
            container
        )
    }

    fun tracePath(): List<V> {
        return pathTraceable.tracePath()
    }

    companion object {
        fun <V, I, G, A> create(
            capabilities: TransitionCapabilities<V, I, G, A>,
            machineContext: G?,
            gsmConfig: GsmConfig
        ): TransitionMediator<V, I, G, A> where V : Vertex<I> {
            return TransitionMediator(
                forwardTransition = capabilities.forward,
                previousTransition = capabilities.previous,
                resettable = capabilities.resettable,
                pathTraceable = capabilities.pathTraceable,
                machineContext = machineContext,
                gsmConfig = gsmConfig
            )
        }
    }
}