package mdk.gsm.state

import mdk.gsm.builder.GsmBuilderScope
import mdk.gsm.graph.IVertex

/**
 * A conditional function that controls whether a traversal along an edge is permitted during graph traversal.
 *
 * A [TransitionGuard] function is associated with an [mdk.gsm.graph.Edge] and is evaluated when the state machine
 * attempts to traverse that edge.
 *
 * It acts as a dynamic runtime condition that can prevent certain transitions
 * based on the current state of the system, effectively implementing conditional navigation logic.
 *
 * As a lambda with receiver the functions implementation occurs within a [TransitionGuardScope],
 * which can be used to make decisions about traversal.
 *
 * @return `true` to allow the traversal along this edge, `false` to prevent it and try the next edge
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of traversal guard state shared across the state machine. Must implement [ITransitionGuardState].
 * @param A The type of arguments that can be passed with actions to influence traversal decisions.
 *
 * @see mdk.gsm.graph.Edge
 * @see TransitionGuardScope
 * @see mdk.gsm.builder.EdgeBuilderScope.setEdgeTransitionGuard
 */
typealias TransitionGuard<V, I, G, A> =
        suspend TransitionGuardScope<V, I, G, A>.() -> Boolean

/**
 * Provides context and data to a [TransitionGuard] function during edge traversal evaluation.
 *
 * This scope is automatically created and passed to a [TransitionGuard] function when the state machine
 * evaluates whether an edge can be traversed.
 *
 * This scope enables traversal guards to make decisions based on:
 * - Properties of the source vertex
 * - Shared state that may have been modified by previous traversals
 * - Arguments provided with the current action
 *
 * @property from The source vertex of the edge being evaluated. This is the vertex the edge is outgoing from.
 * @property guardState The shared traversal guard state for the entire state machine. This single instance
 *                     is shared across all edge evaluations, allowing for communication between edges and
 *                     persistence of state across transitions.
 * @property args Any arguments passed with the current action, typically from a [GraphStateMachineAction.NextArgs].
 *               May be null if no arguments were provided.
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of traversal guard state. Must implement [ITransitionGuardState].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see TransitionGuard
 * @see ITransitionGuardState
 * @see GraphStateMachineAction.NextArgs
 */
@GsmBuilderScope
class TransitionGuardScope<V, I, G, A>(
    val from : V,
    val guardState : G?,
    val args : A?,
) where V : IVertex<I>