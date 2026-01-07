package mdk.gsm.state

import mdk.gsm.builder.GsmBuilderScope
import mdk.gsm.graph.IVertex

/**
 * Provides context to vertex entry hooks that execute after a state has been fully entered and published.
 *
 * This scope is automatically created and passed to an [OnEntryHook] function after the state machine
 * has transitioned to a vertex and published the new state. It provides access to:
 *
 * @property vertex The vertex that has been entered (the new current state).
 * @property machineContext The shared machine context for the entire state machine.
 * @property args Any arguments passed with the current action. May be null if no arguments were provided.
 *
 * @param V The type of vertices in the graph. Must implement [mdk.gsm.graph.IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of machine context. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see OnEntryHook
 * @see mdk.gsm.graph.VertexContainer
 */
@GsmBuilderScope
class OnEntryScope<V, I, G, A>(
    val vertex: V,
    val machineContext: G?,
    val args: A?
) where V : IVertex<I>

/**
 * A hook invoked after a vertex has been fully entered and published as the current state.
 *
 * Unlike [BeforeEntryHook], this hook executes after the state transition is complete and the new state
 * has been published to observers. This is useful for performing side effects that should only occur
 * after the state change is visible to the rest of the application.
 *
 * @param V The type of vertices in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of machine context. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions.
 *
 * @see OnEntryScope
 * @see mdk.gsm.builder.VertexBuilderScope.onEntry
 */
typealias OnEntryHook<V, I, G, A> = suspend OnEntryScope<V, I, G, A>.() -> Unit
