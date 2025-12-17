@file:Suppress("unused")

package mdk.gsm.builder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import mdk.gsm.action.CompletableAction
import mdk.gsm.builder.DispatcherConfig.Companion.toChannel
import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.state.TransitionBounds
import mdk.gsm.state.traverser.Traverser
import mdk.gsm.state.traverser.TraverserDispatcherImplementation
import mdk.gsm.state.traverser.TraverserImplementation
import mdk.gsm.state.traverser.TraverserStateImplementation


/**
 * Builds a graph-backed traverser with a custom transition guard state and typed per-action arguments.
 *
 * Use this overload when you need to pass contextual data with an action via
 * [GraphStateMachineAction.NextArgs] of type [A]. The value is delivered to transition guards
 * and stored on the resulting [mdk.gsm.state.TransitionState.args]. The traverser supports
 * forward and backward navigation and maintains transition history.
 *
 * Overload selection:
 * - Choose this overload if you need both a custom guard state ([G]) and per-action arguments ([A]).
 * - If you do not need per-action arguments, prefer [buildTraverser] without [A].
 * - If you also do not need a custom guard state, prefer the simplest [buildTraverser] overload.
 *
 * Parameters:
 * - [guardState]: The initial state shared by all transition guards.
 * - [coroutineScope]: Scope used for dispatch; defaults to [StateMachineScopeFactory.newScope].
 * - [dispatcherConfig]: Controls channel capacity/overflow for action dispatching.
 * - [builderFunction]: DSL to declare vertices, edges, and options.
 *
 * Returns: A configured [mdk.gsm.state.traverser.Traverser].
 *
 * Throws: [IllegalStateException] if the graph/start vertex is not configured.
 *
 * Example:
 * ```kotlin
 * val traverser = buildTraverserWithActions<MyVertex, String, Flags, Long>(
 *     guardState = Flags()
 * ) {
 *     buildGraph(startAtVertex = MyVertex.Start) {
 *         // define vertices and edges
 *     }
 * }
 * // Later: dispatch an action with contextual arguments
 * // traverser.dispatcher.launchDispatch(GraphStateMachineAction.NextArgs(42L))
 * ```
 *
 * @param V The type of vertices (states). Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Must implement [ITransitionGuardState].
 * @param A The type of per-action arguments used with [GraphStateMachineAction.NextArgs].
 */
fun <V, I, G, A> buildTraverserWithActions(
    guardState : G,
    coroutineScope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<A> = DispatcherConfig(),
    builderFunction : TraverserBuilderScope<V, I, G, A>.() -> Unit
) : Traverser<V, I, G, A> where V : IVertex<I> {
    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, G, A>()

    val traverserBuilderScope = TraverserBuilderScope(graphStateMachineBuilder)
    builderFunction(traverserBuilderScope)

    graphStateMachineBuilder.transitionGuardState = guardState

    val gsm = graphStateMachineBuilder.build()

    val channel : Channel<CompletableAction<V, I, A>> = dispatcherConfig.toChannel()

    return TraverserImplementation(
        TraverserStateImplementation.create(gsm),
        TraverserDispatcherImplementation.create(gsm, coroutineScope, channel)
    )
}

/**
 * Builds a graph-backed traverser with a custom transition guard state and no per-action arguments.
 *
 * Use this overload when you want guards backed by shared state ([G]), but you do not need to pass
 * values with each action. The traverser supports forward and backward navigation and maintains
 * transition history.
 *
 * Overload selection:
 * - Choose this overload if you need custom guard state ([G]) but not per-action arguments.
 * - If you need per-action arguments as well, prefer [buildTraverserWithActions].
 * - If you need neither, prefer the simplest [buildTraverser] overload.
 *
 * Example:
 * ```kotlin
 * val traverser = buildGuardedTraverser<MyVertex, String, Flags>(
 *     guardState = Flags()
 * ) {
 *     buildGraph(startAtVertex = MyVertex.Start) {
 *         // define vertices and edges
 *     }
 * }
 * ```
 *
 * @param V The type of the vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param G The transition guard state shared across edges.
 * @param guardState The initial state for transition guards, shared across all edges.
 * @param coroutineScope The coroutine scope used for dispatching actions. Defaults to [StateMachineScopeFactory.newScope].
 * @param dispatcherConfig Configuration for the dispatcher channel. Controls buffering and overflow behavior.
 * @param builderFunction The builder scope function for configuring the traverser.
 * @return A fully configured [Traverser] instance.
 * @throws IllegalStateException If the traverser is not configured correctly when attempting to build.
 */
@GsmBuilderScope
fun <V, I, G> buildGuardedTraverser(
    guardState : G,
    coroutineScope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<Nothing> = DispatcherConfig(),
    builderFunction : TraverserBuilderScope<V, I, G, Nothing>.() -> Unit
) : Traverser<V, I, G, Nothing>
    where V : IVertex<I> {

    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, G, Nothing>()

    val traverserBuilderScope = TraverserBuilderScope(graphStateMachineBuilder)
    builderFunction(traverserBuilderScope)

    graphStateMachineBuilder.transitionGuardState = guardState

    val gsm = graphStateMachineBuilder.build()

    val channel : Channel<CompletableAction<V, I, Nothing>> = dispatcherConfig.toChannel()

    return TraverserImplementation(
        TraverserStateImplementation.create(gsm),
        TraverserDispatcherImplementation.create(gsm, coroutineScope, channel)
    )
}

/**
 * Builds a traverser without custom guard state and without per-action arguments (the simplest overload).
 *
 * Use this when your transitions depend only on the graph structure and you do not require guard state
 * or passing values with actions. The traverser supports forward and backward navigation and maintains
 * transition history. A no-op guard state is used internally and the action argument type is [Nothing].
 *
 * Overload selection:
 * - Choose this overload if you need neither custom guard state nor per-action arguments.
 * - If you need guard state, prefer [buildGuardedTraverser].
 * - If you also need per-action arguments, prefer [buildTraverserWithActions].
 *
 * Example:
 * ```kotlin
 * val traverser = buildTraverser<MyVertex, String> {
 *     buildGraph(startAtVertex = MyVertex.Start) {
 *         // define vertices and edges
 *     }
 * }
 * ```
 *
 * @param V The type of the vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param scope The coroutine scope used for dispatching actions. Defaults to [StateMachineScopeFactory.newScope].
 * @param dispatcherConfig Configuration for the dispatcher channel. Controls buffering and overflow behavior.
 * @param builderFunction The builder scope function for configuring the traverser.
 * @return A fully configured [Traverser] instance.
 * @throws IllegalStateException If the traverser is not configured correctly when attempting to build.
 */

@GsmBuilderScope
fun <V, I> buildTraverser(
    scope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<Nothing> = DispatcherConfig(),
    builderFunction : TraverserBuilderScope<V, I, Nothing, Nothing>.() -> Unit
) : Traverser<V, I, Nothing, Nothing> where V : IVertex<I> {

    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, Nothing, Nothing>()
    graphStateMachineBuilder.transitionGuardState = null

    val traverserBuilderScope = TraverserBuilderScope(graphStateMachineBuilder)
    builderFunction(traverserBuilderScope)

    val gsm = graphStateMachineBuilder.build()

    val channel : Channel<CompletableAction<V, I, Nothing>> = dispatcherConfig.toChannel()

    return TraverserImplementation(
        TraverserStateImplementation.create(gsm),
        TraverserDispatcherImplementation.create(gsm, scope, channel)
    )
}

/**
 * Builder scope class for configuring a traverser.
 *
 * This class provides a DSL (Domain Specific Language) for configuring a traverser.
 * It exposes methods for setting up the graph, defining the start vertex, configuring traversal behavior,
 * and other traverser properties.
 *
 * Instances of this class are created by the [buildTraverser], [buildGuardedTraverser], and [buildTraverserWithActions]
 * functions and passed to the builder function provided to those functions.
 *
 * @param V The type of the vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param G The traversal guard state shared across edges. Must implement [ITransitionGuardState].
 * @param A The type of arguments that can be passed with actions to influence traversal decisions.
 *
 * @see buildTraverser
 * @see buildGuardedTraverser
 * @see buildTraverserWithActions
 * @see GraphStateMachineBuilder
 */
@GsmBuilderScope
class TraverserBuilderScope<V, I, G, A> @PublishedApi internal constructor(
    internal val graphStateMachineBuilder: GraphStateMachineBuilder<V, I, G, A>
) where V : IVertex<I> {

    /**
     * Assigns an already-built [Graph] and sets the start vertex for the traverser.
     *
     * Use this when you have constructed a graph separately (e.g., via [buildGraphOnly]) and
     * want to reuse it. The [startAtVertex] should exist in the provided [graph].
     *
     * Example:
     * ```kotlin
     * val graph = buildGraphOnly<MyVertex, String, Flags, Nothing> { /* ... */ }
     * setWorkflowGraph(startAtVertex = MyVertex.Start, graph = graph)
     * ```
     *
     * @param startAtVertex The start vertex. Must exist in [graph].
     * @param graph The graph to assign to this traverser.
     */
    fun setWorkflowGraph(startAtVertex : V, graph: Graph<V, I, G, A>) {
        graphStateMachineBuilder.graph = graph
        graphStateMachineBuilder.startVertex = startAtVertex
    }

    /**
     * Builds and assigns a graph for the traverser using the provided DSL.
     *
     * This creates an internal [GraphBuilder] and invokes [scopeConsumer] with a [GraphBuilderScope]
     * to declare vertices and edges. The resulting graph is validated (no duplicate vertex ids and
     * no dangling edges) and set on the builder, and [startAtVertex] becomes the start vertex.
     *
     * Example:
     * ```kotlin
     * buildGraph(startAtVertex = MyVertex.Start) {
     *     addVertex(MyVertex.Start) { addEdge { setTo(MyVertex.Next) } }
     *     addVertex(MyVertex.Next)
     * }
     * ```
     *
     * @param startAtVertex The vertex to start at. Must exist in the graph after building.
     * @param scopeConsumer A DSL that configures the graph via [GraphBuilderScope].
     */
    fun buildGraph(startAtVertex : V, scopeConsumer : GraphBuilderScope<V, I, G, A>.() -> Unit) {
        val graphGraphBuilder = GraphBuilder<V, I, G, A>()
        val graphBuilderScope = GraphBuilderScope(graphGraphBuilder)
        scopeConsumer(graphBuilderScope)
        graphStateMachineBuilder.graph = graphGraphBuilder.build()
        graphStateMachineBuilder.startVertex = startAtVertex
    }

    /**
     * Shorthand to build and assign a graph with the same semantics as [buildGraph].
     *
     * Accepts the same parameters and builds the graph using a [GraphBuilderScope], then sets
     * [startAtVertex] as the start vertex.
     */
    fun g(startAtVertex : V, scopeConsumer : GraphBuilderScope<V, I, G, A>.() -> Unit) {
        buildGraph(startAtVertex, scopeConsumer)
    }

    /**
     * Sets the edge exploration strategy for the traverser.
     *
     * @see EdgeTraversalType
     */
    fun setTraversalType(edgeTraversalType: EdgeTraversalType) {
        graphStateMachineBuilder.traversalType = edgeTraversalType
    }

    /**
     * Sets the transition guard state for the entire traverser.
     * Can be ignored if not building a traverser with transition guards; however, it must not be null if specified as a type parameter.
     */
    fun setTransitionGuardState(guardState: G) {
        graphStateMachineBuilder.transitionGuardState = guardState
    }

    /**
     * Sets the start vertex for the traverser.
     * The start vertex must be set before the traverser can be built.
     *
     * @param vertex The vertex to start at. Must be a vertex in the graph.
     */
    fun startAtVertex(vertex: V) {
        graphStateMachineBuilder.startVertex = vertex
    }


    /**
     * The default is `false`, in which case transition behavior is not impacted and the concept of transition bounds can be ignored.
     * This can be safely ignored unless your use case requires treating “nowhere else to go” as a distinct state via [TransitionBounds].
     *
     * Being out of bounds (no valid outgoing transition) for the traverser — indicated by the
     * [mdk.gsm.state.TransitionBounds] property on [mdk.gsm.state.TransitionState] — can be viewed as a
     * null-like condition, but with awareness of the last vertex and the direction of movement.
     *
     * If [setExplicitTransitionIntoBounds] is `true`, the traverser will treat moving back into bounds as a
     * standalone distinct transition, yielding an in-bounds transition state on the same vertex upon the next
     * dispatched action. In other words, the [mdk.gsm.state.TransitionBounds] changes to
     * [mdk.gsm.state.TransitionBounds.WithinBounds] while the vertex remains the same.
     *
     * This can be useful to demarcate a process as “uninitialized” or “finished” (for example, in a DAG workflow with a
     * definitive end), allowing callers to react when the workflow completes.
     *
     * @param explicitlyTransitionIntoBounds `true` to enable automatic transitions back into a valid state on the same vertex when
     *        out of bounds, `false` to keep the traverser at the out-of-bounds state (default).
     */
    fun setExplicitTransitionIntoBounds(explicitlyTransitionIntoBounds : Boolean) {
        graphStateMachineBuilder.explicitlyTransitionIntoBounds = explicitlyTransitionIntoBounds
    }
}
