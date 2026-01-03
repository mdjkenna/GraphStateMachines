@file:Suppress("unused")

package mdk.gsm.builder

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import mdk.gsm.action.CompletableAction
import mdk.gsm.builder.DispatcherConfig.Companion.toChannel
import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.GsmController
import mdk.gsm.state.MachineContext
import mdk.gsm.state.TransitionBounds
import mdk.gsm.state.walker.Walker
import mdk.gsm.state.walker.WalkerDispatcherImplementation
import mdk.gsm.state.walker.WalkerImplementation
import mdk.gsm.state.walker.WalkerStateImplementation

/**
 * Builds a graph based walker with a custom machine context and typed per-action arguments.
 *
 * A walker only supports forward movement and does not keep a history of visited states.
 * Use this overload when you need to pass contextual data with actions via
 * [GraphStateMachineAction.NextArgs] of type [A]. The value is delivered to transition hooks
 * and stored on the resulting [mdk.gsm.state.TransitionState.args].
 *
 * Overload selection:
 * - Choose this overload if you need both a custom machine context ([G]) and per-action arguments ([A]).
 * - If you do not need per-action arguments, prefer [buildWalker] without [A].
 * - If you also do not need a custom machine context, prefer the simplest [buildWalker] overload.
 *
 * Parameters:
 * - [machineContext]: The initial machine context shared by all transition hooks.
 * - [coroutineScope]: Scope used for dispatch; defaults to [StateMachineScopeFactory.newScope].
 * - [dispatcherConfig]: Controls channel capacity/overflow for action dispatching.
 * - [builderFunction]: DSL to declare vertices, edges, and options.
 *
 * Returns: A configured [mdk.gsm.state.walker.Walker].
 *
 * Throws: [IllegalStateException] if the graph/start vertex is not configured.
 *
 * Example:
 * ```kotlin
 * val walker = buildWalkerWithActions<MyVertex, String, Flags, Long>(
 *     machineContext = Flags()
 * ) {
 *     buildGraph(startAtVertex = MyVertex.Start) {
 *         // define vertices and edges
 *     }
 * }
 * ```
 *
 * @param V The type of vertices (states). Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of machine context. Must implement [MachineContext].
 * @param A The type of per-action arguments used with [GraphStateMachineAction.NextArgs].
 */
fun <V, I, G, A> buildWalkerWithActions(
    machineContext : G,
    coroutineScope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<A> = DispatcherConfig(),
    builderFunction : WalkerBuilderScope<V, I, G, A>.() -> Unit
) : Walker<V, I, G, A> where V : IVertex<I> {
    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, G, A>()

    val walkerBuilderScope = WalkerBuilderScope(graphStateMachineBuilder)
    builderFunction(walkerBuilderScope)

    graphStateMachineBuilder.machineContext = machineContext

    val gsm = graphStateMachineBuilder.buildForWalker()

    val channel : Channel<CompletableAction<V, I, A>> = dispatcherConfig.toChannel()

    return WalkerImplementation(
        WalkerStateImplementation.create(gsm),
        WalkerDispatcherImplementation.create(gsm, coroutineScope, channel)
    )
}

/**
 * Builds a graph-backed walker with a custom machine context and no per-action arguments.
 *
 * A walker only supports forward movement and does not keep a history of visited states.
 * Use this overload when you want hooks backed by machine context ([G]) but you do not need to pass
 * values with each action.
 *
 * Overload selection:
 * - Choose this overload if you need machine context ([G]) but not per-action arguments.
 * - If you need per-action arguments as well, prefer [buildWalkerWithActions].
 * - If you need neither, prefer the simplest [buildWalker] overload.
 *
 * Example:
 * ```kotlin
 * val walker = buildWalkerWithContext<MyVertex, String, Flags>(
 *     machineContext = Flags()
 * ) {
 *     buildGraph(startAtVertex = MyVertex.Start) {
 *         // define vertices and edges
 *     }
 * }
 * ```
 *
 * @param V The type of the vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param G The machine context shared across edges.
 * @param machineContext The initial machine context, shared across all edges.
 * @param coroutineScope The coroutine scope used for dispatching actions. Defaults to [StateMachineScopeFactory.newScope].
 * @param dispatcherConfig Configuration for the dispatcher channel. Controls buffering and overflow behavior.
 * @param builderFunction The builder scope function for configuring the walker.
 * @return A fully configured [Walker] instance.
 * @throws IllegalStateException If the walker is not configured correctly when attempting to build.
 */
@GsmBuilderScope
fun <V, I, G> buildWalkerWithContext(
    machineContext : G,
    coroutineScope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<Nothing> = DispatcherConfig(),
    builderFunction : WalkerBuilderScope<V, I, G, Nothing>.() -> Unit
) : Walker<V, I, G, Nothing>
    where V : IVertex<I> {

    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, G, Nothing>()

    val walkerBuilderScope = WalkerBuilderScope(graphStateMachineBuilder)
    builderFunction(walkerBuilderScope)

    graphStateMachineBuilder.machineContext = machineContext

    val gsm = graphStateMachineBuilder.buildForWalker()

    val channel : Channel<CompletableAction<V, I, Nothing>> = dispatcherConfig.toChannel()

    return WalkerImplementation(
        WalkerStateImplementation.create(gsm),
        WalkerDispatcherImplementation.create(gsm, coroutineScope, channel)
    )
}

/**
 * Builds a walker without machine context and without per-action arguments (the simplest overload).
 *
 * A walker supports forward-only movement and does not keep a history of visited states. Use this when your
 * transitions depend solely on the graph structure. A no-op machine context is used internally and the action
 * argument type is [Nothing].
 *
 * Overload selection:
 * - Choose this overload if you need neither machine context nor per-action arguments.
 * - If you need machine context, prefer [buildWalkerWithContext].
 * - If you also need per-action arguments, prefer [buildWalkerWithActions].
 *
 * Example:
 * ```kotlin
 * val walker = buildWalker<MyVertex, String> {
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
 * @param builderFunction The builder scope function for configuring the walker.
 * @return A fully configured [Walker] instance.
 * @throws IllegalStateException If the walker is not configured correctly when attempting to build.
 */
@GsmBuilderScope
fun <V, I> buildWalker(
    scope : CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: DispatcherConfig<Nothing> = DispatcherConfig(),
    builderFunction : WalkerBuilderScope<V, I, Nothing, Nothing>.() -> Unit
) : Walker<V, I, Nothing, Nothing> where V : IVertex<I> {

    val graphStateMachineBuilder = GraphStateMachineBuilder<V, I, Nothing, Nothing>()
    graphStateMachineBuilder.machineContext = null

    val walkerBuilderScope = WalkerBuilderScope(graphStateMachineBuilder)
    builderFunction(walkerBuilderScope)

    val gsm = graphStateMachineBuilder.buildForWalker()

    val channel : Channel<CompletableAction<V, I, Nothing>> = dispatcherConfig.toChannel()

    return WalkerImplementation(
        WalkerStateImplementation.create(gsm),
        WalkerDispatcherImplementation.create(gsm, scope, channel)
    )
}

/**
 * Builder scope class for configuring a walker.
 *
 * This class provides a DSL (Domain Specific Language) for configuring a walker.
 * It exposes methods for setting up the graph, defining the start vertex, and other walker properties.
 * Unlike [TraverserBuilderScope], it does not expose methods for configuring the edge exploration strategy,
 * as walkers only support forward movement.
 *
 * Instances of this class are created by the [buildWalker], [buildWalkerWithContext], and [buildWalkerWithActions]
 * functions and passed to the builder function provided to those functions.
 *
 * @param V The type of the vertices (states) in the graph. Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param G The machine context shared across edges. Must implement [MachineContext].
 * @param A The type of arguments that can be passed with actions to influence transition decisions.
 *
 * @see buildWalker
 * @see buildWalkerWithContext
 * @see buildWalkerWithActions
 * @see GraphStateMachineBuilder
 */
@GsmBuilderScope
class WalkerBuilderScope<V, I, G, A> @PublishedApi internal constructor(
    internal val graphStateMachineBuilder: GraphStateMachineBuilder<V, I, G, A>
) where V : IVertex<I> {

    /**
     * Assigns an already-built [Graph] and sets the start vertex for the walker.
     *
     * Use this when you have constructed a graph separately (e.g., via [buildGraph]) and
     * want to reuse it. The [startAtVertex] should exist in the provided [graph].
     *
     * Example:
     * ```kotlin
     * val graph = buildGraph<MyVertex, String, Flags, Nothing> { /* ... */ }
     * setWorkflowGraph(startAtVertex = MyVertex.Start, graph = graph)
     * ```
     *
     * @param startAtVertex The start vertex. Must exist in [graph].
     * @param graph The graph to assign to this walker.
     */
    fun setWorkflowGraph(startAtVertex : V, graph: Graph<V, I, G, A>) {
        graphStateMachineBuilder.graph = graph
        graphStateMachineBuilder.startVertex = startAtVertex
    }

    /**
     * Builds and assigns a graph for the walker using the provided DSL.
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
     * Sets the machine context for the entire walker.
     * Can be ignored if not building a walker with transition hooks; however, it must not be null if specified as a type parameter.
     */
    fun setMachineContext(machineContext: G) {
        graphStateMachineBuilder.machineContext = machineContext
    }

    /**
     * Sets the start vertex for the walker.
     * The start vertex must be set before the walker can be built.
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
     * Being out of bounds (no valid outgoing transition) for the walker — indicated by the
     * [mdk.gsm.state.TransitionBounds] property on [mdk.gsm.state.TransitionState] — can be viewed as a
     * null-like condition, but with awareness of the last vertex and the direction of movement.
     *
     * If [setExplicitTransitionIntoBounds] is `true`, the walker will treat moving back into bounds as a
     * standalone distinct transition, yielding an in-bounds transition state on the same vertex upon the next
     * dispatched action. In other words, the [mdk.gsm.state.TransitionBounds] changes to
     * [mdk.gsm.state.TransitionBounds.WithinBounds] while the vertex remains the same.
     *
     * This can be useful to demarcate a process as “uninitialized” or “finished” (for example, in a DAG workflow with a
     * definitive end), allowing callers to react when the workflow completes.
     *
     * @param explicitlyTransitionIntoBounds `true` to enable automatic transitions back into a valid state on the same vertex when
     *        out of bounds, `false` to keep the walker at the out-of-bounds state (default).
     */
    fun setExplicitTransitionIntoBounds(explicitlyTransitionIntoBounds : Boolean) {
        graphStateMachineBuilder.explicitlyTransitionIntoBounds = explicitlyTransitionIntoBounds
    }
}

@PublishedApi
internal fun <V, I, G, A> GraphStateMachineBuilder<V, I, G, A>.buildForWalker(): GsmController<V, I, G, A> where V : IVertex<I> {
    useStatelessWalk = true

    return build()
}
