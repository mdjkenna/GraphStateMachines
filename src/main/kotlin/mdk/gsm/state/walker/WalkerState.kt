package mdk.gsm.state.walker

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Vertex
import mdk.gsm.state.TransitionState

/**
 * Interface for reading the current state of a graph-based walker.
 *
 * This interface provides read-only access to the walker's current state,
 * allowing clients to observe the walker without exposing a way of modifying it which is conducive to unidirectional data flow architectures.
 * Unlike [mdk.gsm.state.traverser.TraverserState], this interface does not provide access to traversal history as walkers do not maintain history.
 *
 * @param V The type of vertices (states) in the graph. Must implement [Vertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param A The type of action arguments that can be passed when dispatching actions.
 *
 * @see TransitionState
 * @see Walker
 * @see WalkerDispatcher
 */
interface WalkerState<V, I, A> where V : Vertex<I> {
    /**
     * A [StateFlow] that publishes the current state of the walker as a [TransitionState]
     */
    val current : StateFlow<TransitionState<V, I, A>>
}