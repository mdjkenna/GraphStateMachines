package mdk.gsm.state.traverser

import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.TransitionState

/**
 * Interface for dispatching actions to a graph-based traverser, causing state transitions.
 *
 * This interface provides methods to dispatch various [mdk.gsm.state.GraphStateMachineAction]s to the traverser,
 * which trigger transitions between states (vertices) according to the defined graph structure and
 * traversal rules. Actions can be dispatched either asynchronously or synchronously with result awaiting.
 *
 * The dispatcher is responsible for:
 * - Ensuring actions are processed in the order they are received
 * - Managing the state transitions triggered by actions
 * - Providing mechanisms to await action completion
 * - Handling the lifecycle of the traverser's processing resources
 *
 * @param V The type of vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param G The type of traversal guard state, which controls conditional edge traversal. Must implement [mdk.gsm.state.ITransitionGuardState].
 * @param A The type of action arguments that can be passed when dispatching actions.
 *
 * @see mdk.gsm.state.GraphStateMachineAction
 * @see Traverser
 * @see TraverserState
 */
interface TraverserDispatcher<V, I, G, A> where V : IVertex<I> {
    /**
     * Asynchronously dispatches an action to the traverser without waiting for it to complete.
     *
     * This method launches a coroutine to dispatch the action and immediately returns, allowing
     * the caller to continue execution while the action is processed in the background.
     *
     * @param action The [mdk.gsm.state.GraphStateMachineAction] to dispatch to the traverser
     */
    fun launchDispatch(action: GraphStateMachineAction<A>)

    /**
     * Suspends the current coroutine and dispatches an action to the traverser.
     *
     * This method suspends until the action is received by the traverser, but does not
     * wait for the action to be fully processed or for the state transition to complete.
     *
     * @param action The [GraphStateMachineAction] to dispatch to the traverser
     */
    suspend fun dispatch(action: GraphStateMachineAction<A>)

    /**
     * Dispatches an action to the traverser and awaits the resulting state.
     *
     * This method suspends until the action is fully processed and the state transition is complete,
     * then returns the new state of the traverser.
     *
     * @param action The [GraphStateMachineAction] to dispatch to the traverser
     * @return The [TransitionState] representing the new state after the action is processed
     */
    suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction<A>): TransitionState<V, I, A>

    /**
     * Tears down the traverser, cancelling all associated jobs.
     * This is irreversible, and the traverser can not be used afterwards.
     *
     * Note this will directly cancel the traverser's coroutine scope.
     * Be mindful of this if you have used your own.
     *
     * This method should be called when the traverser is no longer needed to ensure
     * proper cleanup of resources, such as coroutines and channels.
     */
    fun tearDown()
}
