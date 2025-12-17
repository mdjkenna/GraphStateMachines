package mdk.gsm.state.traverser

import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphSupplier

/**
 * Primary interface for interacting with a graph-based traverser. This interface combines the capabilities
 * of both [TraverserDispatcher] and [TraverserState], providing a unified API for both
 * dispatching actions to and reading state from the traverser.
 *
 * The traverser represents states as vertices in a directed graph, with edges representing
 * possible transitions between states. The traverser can be traversed by dispatching actions,
 * which cause transitions along the edges of the graph according to the defined traversal rules.
 *
 * This interface is typically obtained by using one of the builder functions in [mdk.gsm.builder]:
 * - [mdk.gsm.builder.buildTraverser]
 * - [mdk.gsm.builder.buildTraverserWithActions]
 *
 * @param V The type of vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of vertex identifiers used in the graph.
 * @param G The type of traversal guard state, which controls conditional edge traversal. Must implement [mdk.gsm.state.ITransitionGuardState].
 * @param A The type of action arguments that can be passed when dispatching actions.
 *
 * @see TraverserDispatcher For dispatching actions to the traverser
 * @see TraverserState For reading the current state and path history
 * @see mdk.gsm.builder.buildTraverser
 * @see mdk.gsm.builder.buildTraverserWithActions
 */
interface Traverser<V, I, G, A> : TraverserDispatcher<V, I, G, A>, TraverserState<V, I, G, A>, GraphSupplier<V, I, G, A>
        where V : IVertex<I>
{
    /**
     * Provides read-only access to the traverser's current state and traversal path.
     * This property allows accessing the traverser's data without the ability to modify it.
     */
    val traverserState : TraverserState<V, I, G, A>

    /**
     * Provides the ability to dispatch actions to the traverser, causing state transitions.
     * This property allows modifying the traverser's state through controlled actions.
     */
    val traverserDispatcher : TraverserDispatcher<V, I, G, A>

    operator fun component1(): TraverserState<V, I, G, A> = traverserState

    operator fun component2(): TraverserDispatcher<V, I, G, A> = traverserDispatcher
}
