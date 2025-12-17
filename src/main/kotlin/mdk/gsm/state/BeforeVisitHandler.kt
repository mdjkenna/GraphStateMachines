package mdk.gsm.state

import mdk.gsm.builder.GsmBuilderScope
import mdk.gsm.graph.IVertex

/**
 * Provides context and control capabilities to vertex pre-visit handlers.
 *
 * This scope is automatically created and passed to a [BeforeVisitHandler] function when the state machine
 * is about to transition to a vertex. It provides access to:
 *
 * @property vertex The vertex that is about to be visited (become the current state).
 * @property guardState The shared transition guard state for the entire state machine.
 * @property args Any arguments passed with the current action. May be null if no arguments were provided.
 *
 * @param V The type of vertices in the graph. Must implement [mdk.gsm.graph.IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Must implement [ITransitionGuardState].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see BeforeVisitHandler
 * @see mdk.gsm.graph.VertexContainer
 */
@GsmBuilderScope
class BeforeVisitScope<V, I, G, A>(
    val vertex: V,
    val guardState: G?,
    val args : A?
) where V : IVertex<I> {

    internal var autoAdvanceTrigger = false

    /**
     * Calling this function signals that the vertex state about to be visited should be skipped
     * without publishing this vertex as the current state.
     *
     * When called, the handler's vertex becomes an 'intermediate state'.
     *
     * The subsequent state in the state graph will be adopted as the current state instead.
     * Intermediate states are 'in-between' states that function as 'effects' that are explicitly represented in the state machine.
     * They are never observed as state.
     *
     * Note: Intermediate states are skipped over during previous actions.
     *
     * @see BeforeVisitHandler
     */
    fun autoAdvance() {
        autoAdvanceTrigger = true
    }
}

/**
 *
 * The handler is invoked when the state machine is about to transition to a new vertex, but before
 * that vertex is published as the current state. It receives a [BeforeVisitScope] as its receiver,
 * providing access to the vertex being visited, the shared transition guard state, and any arguments
 * passed with the current action.
 *
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Must implement [ITransitionGuardState].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see BeforeVisitScope
 * @see mdk.gsm.builder.VertexBuilderScope.onBeforeVisit
 */
typealias BeforeVisitHandler<V, I, G, A> = suspend BeforeVisitScope<V, I, G, A>.() -> Unit
