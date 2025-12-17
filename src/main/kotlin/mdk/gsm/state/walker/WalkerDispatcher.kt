package mdk.gsm.state.walker

import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.TransitionState

/**
 * Interface for dispatching actions to a graph-based walker, causing state transitions.
 *
 * This interface provides methods to dispatch forward-only actions to the walker,
 * which trigger transitions between states (vertices) according to the defined graph structure and
 * traversal rules. Unlike [mdk.gsm.state.traverser.TraverserDispatcher], this interface only supports forward movement
 * (Next and NextArgs actions) and reset.
 *
 * The dispatcher is responsible for:
 * - Ensuring actions are processed in the order they are received
 * - Managing the state transitions triggered by actions
 * - Providing mechanisms to await action completion
 * - Handling the lifecycle of the walker's processing resources
 *
 * @param V The type of vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param G The type of traversal guard state, which controls conditional edge traversal. Must implement [mdk.gsm.state.ITransitionGuardState].
 * @param A The type of action arguments that can be passed when dispatching actions.
 *
 * @see mdk.gsm.state.GraphStateMachineAction
 * @see Walker
 * @see WalkerState
 */
interface WalkerDispatcher<V, I, G, A> where V : IVertex<I> {
    /**
     * Asynchronously dispatches a Next action to the walker without waiting for it to complete.
     *
     * This method launches a coroutine to dispatch the action and immediately returns, allowing
     * the caller to continue execution while the action is processed in the background.
     */
    fun launchDispatch(action: GraphStateMachineAction.Next)

    /**
     * Asynchronously dispatches a NextArgs action to the walker without waiting for it to complete.
     *
     * This method launches a coroutine to dispatch the action and immediately returns, allowing
     * the caller to continue execution while the action is processed in the background.
     *
     * @param action The [GraphStateMachineAction.NextArgs] to dispatch to the walker
     */
    fun launchDispatch(action: GraphStateMachineAction.NextArgs<A>)

    /**
     * Asynchronously dispatches a Reset action to the walker without waiting for it to complete.
     *
     * This method launches a coroutine to dispatch the action and immediately returns, allowing
     * the caller to continue execution while the action is processed in the background.
     */
    fun launchDispatch(action: GraphStateMachineAction.Reset)

    /**
     * Suspends the current coroutine and dispatches a Next action to the walker.
     *
     * This method suspends until the action is received by the walker, but does not
     * wait for the action to be fully processed or for the state transition to complete.
     */
    suspend fun dispatch(action: GraphStateMachineAction.Next)

    /**
     * Suspends the current coroutine and dispatches a NextArgs action to the walker.
     *
     * This method suspends until the action is received by the walker, but does not
     * wait for the action to be fully processed or for the state transition to complete.
     *
     * @param action The [GraphStateMachineAction.NextArgs] to dispatch to the walker
     */
    suspend fun dispatch(action: GraphStateMachineAction.NextArgs<A>)

    /**
     * Suspends the current coroutine and dispatches a Reset action to the walker.
     *
     * This method suspends until the action is received by the walker, but does not
     * wait for the action to be fully processed or for the state transition to complete.
     */
    suspend fun dispatch(action: GraphStateMachineAction.Reset)

    /**
     * Dispatches a Next action to the walker and awaits the resulting state.
     *
     * This method suspends until the action is fully processed and the state transition is complete,
     * then returns the new state of the walker.
     *
     * @return The [TransitionState] representing the new state after the action is processed
     */
    suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.Next): TransitionState<V, I, A>

    /**
     * Dispatches a NextArgs action to the walker and awaits the resulting state.
     *
     * This method suspends until the action is fully processed and the state transition is complete,
     * then returns the new state of the walker.
     *
     * @param action The [GraphStateMachineAction.NextArgs] to dispatch to the walker
     * @return The [TransitionState] representing the new state after the action is processed
     */
    suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.NextArgs<A>): TransitionState<V, I, A>

    /**
     * Dispatches a Reset action to the walker and awaits the resulting state.
     *
     * This method suspends until the action is fully processed and the state transition is complete,
     * then returns the new state of the walker.
     *
     * @return The [TransitionState] representing the new state after the action is processed
     */
    suspend fun dispatchAndAwaitResult(action: GraphStateMachineAction.Reset): TransitionState<V, I, A>

    /**
     * Tears down the walker, cancelling all associated jobs.
     * This is irreversible, and the walker can not be used afterwards.
     *
     * Note this will directly cancel the walker's coroutine scope.
     * Be mindful of this if you have used your own.
     *
     * This method should be called when the walker is no longer needed to ensure
     * proper cleanup of resources, such as coroutines and channels.
     */
    fun tearDown()
}
