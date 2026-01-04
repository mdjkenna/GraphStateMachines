package mdk.gsm.state

import mdk.gsm.builder.GsmBuilderScope
import mdk.gsm.graph.IVertex

/**
 * A hook function that controls whether a traversal along an edge is permitted during graph traversal.
 *
 * A [BeforeTransitionHook] function is associated with an [mdk.gsm.graph.Edge] and is evaluated when the state machine
 * attempts to traverse that edge.
 *
 * It acts as a dynamic runtime condition that can prevent certain transitions
 * based on the current state of the system, effectively implementing conditional navigation logic.
 *
 * As a lambda with receiver the functions implementation occurs within a [BeforeTransitionScope],
 * which can be used to make decisions about traversal. To block the transition, call [BeforeTransitionScope.blockTransition]
 * within the hook. If `blockTransition()` is not called, the transition is allowed by default.
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of machine context shared across the state machine. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions to influence traversal decisions.
 *
 * @see mdk.gsm.graph.Edge
 * @see BeforeTransitionScope
 * @see BeforeTransitionScope.blockTransition
 * @see mdk.gsm.builder.EdgeBuilderScope.onBeforeTransition
 */
typealias BeforeTransitionHook<V, I, G, A> =
        suspend BeforeTransitionScope<V, I, G, A>.() -> Unit

/**
 * Provides context and data to a [BeforeTransitionHook] function during edge traversal evaluation.
 *
 * This scope is automatically created and passed to a [BeforeTransitionHook] function when the state machine
 * evaluates whether an edge can be traversed.
 *
 * This scope enables transition hooks to make decisions based on:
 * - Properties of the source vertex
 * - Machine context that may have been modified by previous transitions
 * - Arguments provided with the current action
 *
 * To block a transition, call [blockTransition] within the hook. If `blockTransition()` is not called,
 * the transition is allowed by default.
 *
 * @property from The source vertex of the edge being evaluated. This is the vertex the edge is outgoing from.
 * @property machineContext The shared machine context for the entire state machine. This single instance
 *                     is shared across all edge evaluations, allowing for communication between edges and
 *                     persistence of state across transitions.
 * @property args Any arguments passed with the current action, typically from a [GraphStateMachineAction.NextArgs].
 *               May be null if no arguments were provided.
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of machine context. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see BeforeTransitionHook
 * @see MachineContext
 * @see GraphStateMachineAction.NextArgs
 */
@GsmBuilderScope
class BeforeTransitionScope<V, I, G, A>(
    val from : V,
    val machineContext : G?,
    val args : A?,
) where V : IVertex<I> {

    @Volatile
    internal var isTransitionBlocked: Boolean = false
        private set

    /**
     * Blocks the current transition from proceeding.
     *
     * When called within a [BeforeTransitionHook], this prevents the state machine from traversing
     * the associated edge. The state machine will then attempt to traverse the next available edge
     * according to edge ordering.
     *
     * If this function is not called, the transition is allowed by default.
     *
     * @see BeforeTransitionHook
     */
    fun blockTransition() {
        isTransitionBlocked = true
    }
}