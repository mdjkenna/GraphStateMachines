package mdk.gsm.state

import mdk.gsm.graph.Vertex

/**
 * Represents the current state of a graph transition in the graph state machine.
 * 
 * This class encapsulates the current position within the graph transition, including the current vertex,
 * boundary status, and any arguments passed with the action that caused this state to be published.
 * 
 * The transition state can be used to determine whether the transition has reached a boundary condition
 * (such as the beginning or end of a directed acyclic graph), which is particularly useful for
 * workflow-like applications where there is a definite start and end.
 *
 * @param V The vertex type, which must implement [Vertex] with identifier type [I]
 * @param I The type of the vertex identifier
 * @param A The type of arguments that can be passed during transition actions
 * 
 * @property vertex The current vertex in the transition
 * @property transitionBounds Indicates whether the transition has reached a boundary condition
 * @property args Optional arguments associated with the current transition state, typically provided during a [GraphStateMachineAction.NextArgs] action
 * 
 * @see TransitionBounds
 * @see GsmController
 * @see GraphStateMachineAction
 */
data class TransitionState<out V, I, out A>(
    val vertex : V,
    val transitionBounds: TransitionBounds = TransitionBounds.WithinBounds,
    val args : A?
) where V : Vertex<I> {

    /**
     * Indicates whether the transition is within the normal bounds of the graph.
     * Returns true when [transitionBounds] is [TransitionBounds.WithinBounds].
     */
    val isWithinBounds : Boolean get() = transitionBounds == TransitionBounds.WithinBounds

    /**
     * Indicates whether the transition has attempted to go before the first vertex.
     * Returns true when [transitionBounds] is [TransitionBounds.BeforeFirst].
     */
    val isBeforeFirst : Boolean get() = transitionBounds == TransitionBounds.BeforeFirst

    /**
     * Indicates whether the transition has attempted to go beyond the last vertex.
     * Returns true when [transitionBounds] is [TransitionBounds.BeyondLast].
     */
    val isBeyondLast : Boolean get() = transitionBounds == TransitionBounds.BeyondLast

    /**
     * Convenience property that returns true when not before the first vertex.
     * Equivalent to `!isBeforeFirst`.
     */
    val isNotBeforeFirst : Boolean get() = !isBeforeFirst

    /**
     * Convenience property that returns true when not beyond the last vertex.
     * Equivalent to `!isBeyondLast`.
     */
    val isNotBeyondLast : Boolean get() = !isBeyondLast
}