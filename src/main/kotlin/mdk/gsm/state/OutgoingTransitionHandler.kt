    package mdk.gsm.state

import mdk.gsm.builder.GsmBuilderScope
import mdk.gsm.graph.IVertex

/**
 * @property args Any arguments passed with the current action. May be null if no arguments were provided.
 * @property guardState The shared transition guard state for the entire state machine.
 * @property vertex The current vertex from which transitions are being considered.
 *
 * @param V The type of vertices in the graph. Must implement [mdk.gsm.graph.IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see BeforeExitHook
 */
@GsmBuilderScope
class BeforeExitScope<V, I, G, A>(
    val args: A?,
    val guardState: G?,
    val vertex: V
) where V : IVertex<I> {

    internal var noChange = false

    /**
     * Signals that the state machine should not transition at all,
     * effectively keeping the current vertex as the state.
     *
     * When called, the state machine will skip exploring any outgoing edges
     * and remain in the current state.
     *
     * @see BeforeExitHook
     */
    fun noTransition() {
        noChange = true
    }
}

/**
 * This handler can be associated with a specific vertex during graph construction.
 * It is a function that executes custom logic before any outgoing transitions are explored.
 * The handler is invoked when the state machine is about to consider transitions from the current vertex.
 * It receives an [BeforeExitScope] as its receiver, providing access to the current vertex,
 * the shared transition guard state, and any arguments passed with the current action.
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see BeforeExitScope
 * @see mdk.gsm.builder.VertexBuilderScope.onBeforeExit
 */
typealias BeforeExitHook<V, I, G, A> = suspend BeforeExitScope<V, I, G, A>.() -> Unit