package mdk.gsm.state.traverser

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.IVertex
import mdk.gsm.state.TransitionState

/**
 * Interface for reading the current state and traversal history of a graph-based traverser.
 *
 * This interface provides read-only access to the traverser's current state and path history,
 * allowing clients to observe the traverser without exposing a way of modifying it which is conducive to unidirectional data flow architectures.
 *
 * @param V The type of vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param G The type of traversal guard state, which controls conditional edge traversal. Must implement [mdk.gsm.state.ITransitionGuardState].
 * @param A The type of action arguments that can be passed when dispatching actions.
 *
 * @see TransitionState
 * @see Traverser
 * @see TraverserDispatcher
 */
interface TraverserState<V, I, G, A> where V : IVertex<I> {
    /**
     * A [StateFlow] that publishes the current state of the traverser as a [TransitionState]
     */
    val current : StateFlow<TransitionState<V, I, A>>

    /**
     * Returns the history of vertices traversed by the traverser.
     *
     * This method provides the sequence of vertices that the traverser has visited,
     * in chronological order from the start vertex to the current vertex. This can be useful
     * for auditing, logging, or displaying a breadcrumb trail of the traverser's path.
     *
     * Note there may be vertices included in this result that were not published if using intermediate states.
     * @return A list of vertices representing the traversal path, ordered from start to current
     */
    fun tracePath() : List<V>
}
