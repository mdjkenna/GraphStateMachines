@file:Suppress("unused")

package mdk.gsm.builder

import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.VertexContainer

@DslMarker
internal annotation class GsmBuilderScope

/**
 * Builds a graph independently of any state machine.
 *
 * Use this when you want to construct a reusable [Graph] and / or supply it later to a
 * traverser/walker builder (for example, via `setWorkflowGraph(...)`). The graph contains
 * vertices and their outgoing edges with optional transition guards.
 *
 * Example:
 * ```kotlin
 * val graph = buildGraphOnly<MyVertex, String, Flags, Nothing> {
 *     addVertex(MyVertex.Start) {
 *         addEdge { setTo(MyVertex.Step1) }
 *     }
 *     addVertex(MyVertex.Step1) {
 *         addEdge { setTo(MyVertex.Done) }
 *     }
 *     addVertex(MyVertex.Done)
 * }
 * ```
 *
 * @param V The type of vertices (states). Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param G The type of transition guard state. Defaults to Nothing.
 * @param A The type of per-action arguments carried by actions (if used by the state machine).
 * @param scopeConsumer DSL for declaring vertices and edges within the graph being built.
 * @return A fully constructed [Graph].
 */
@GsmBuilderScope
fun <V, I, G, A> buildGraphOnly(
    scopeConsumer : GraphBuilderScope<V, I, G, A>.() -> Unit
) : Graph<V, I, G, A> where V : IVertex<I> {
    val graphBuilder = GraphBuilder<V, I, G, A>()
    val graphBuilderScope = GraphBuilderScope(graphBuilder)
    scopeConsumer(graphBuilderScope)

    return graphBuilder.build()
}

/**
 * Builds a graph independently of any state machine.
 *
 * Use this when you want to construct a reusable [Graph] and / or supply it later to a
 * traverser/walker builder (for example, via `setWorkflowGraph(...)`). The graph contains
 * vertices and their outgoing edges with optional transition guards.
 *
 * @param V The type of vertices (states). Must implement [IVertex].
 * @param I The type of vertex identifiers.
 * @param scopeConsumer DSL for declaring vertices and edges within the graph being built.
 * @return A fully constructed [Graph].
 */
fun <V, I> buildPlainGraphOnly(
    scopeConsumer : GraphBuilderScope<V, I, Nothing, Nothing>.() -> Unit
) : Graph<V, I, Nothing, Nothing> where V : IVertex<I> {
    val graphBuilder = GraphBuilder<V, I, Nothing, Nothing>()
    val graphBuilderScope = GraphBuilderScope(graphBuilder)
    scopeConsumer(graphBuilderScope)

    return graphBuilder.build()
}

@GsmBuilderScope
/**
 * DSL scope for building a [Graph] by adding vertices and configuring each vertex.
 *
 * Within this scope you can:
 * - Add vertices with [addVertex] or [v]
 * - For each vertex, configure outgoing edges and handlers using [VertexBuilderScope]
 *
 * Each vertex must have a unique `id`; attempting to add a duplicate will fail.
 *
 * Example:
 * ```kotlin
 * buildGraphOnly<MyVertex, String, Flags, Nothing> {
 *     addVertex(MyVertex.Start) {
 *         addEdge { setTo(MyVertex.Next) }
 *     }
 * }
 * ```
 */
class GraphBuilderScope<V, I, G, A> internal constructor(
    private val workflowGraphGraphBuilder: GraphBuilder<V, I, G, A>
) where V : IVertex<I> {
    /**
     * Adds a vertex to the graph with optional DSL to configure its outgoing edges and handlers.
     *
     * Requirements:
     * - The vertex's `id` must be unique in the graph; duplicates cause an error.
     *
     * Example:
     * ```kotlin
     * addVertex(MyVertex.Step1) {
     *     // configure edges for Step1
     *     addEdge { setTo(MyVertex.Step2) }
     *     addEdge(autoOrder = false) {
     *         setOrder(10)
     *         setTo(MyVertex.Step3)
     *     }
     * }
     * ```
     *
     * @param vertex The vertex to add.
     * @param scopeConsumer Optional DSL to configure the vertex via [VertexBuilderScope].
     */
    fun addVertex(
        vertex: V,
        scopeConsumer: VertexBuilderScope<V, I, G, A>.() -> Unit = {}
    ) {
        val vertexContainerBuilder = VertexBuilder<V, I, G, A>(vertex)
        val vertexBuilderScope = VertexBuilderScope(vertexContainerBuilder)
        scopeConsumer(vertexBuilderScope)

        workflowGraphGraphBuilder.add(vertexContainerBuilder.build())
    }

    /**
     * Shorthand for adding a vertex to the graph with the same semantics as [addVertex].
     * Accepts the same parameters and options.
     */
    fun v(
        vertex: V,
        scopeConsumer: VertexBuilderScope<V, I, G, A>.() -> Unit = {}
    ) = addVertex(vertex, scopeConsumer)
}

internal class GraphBuilder<V, I, G, A> where V : IVertex<I> {
    private val map = HashMap<I, VertexContainer<V, I, G, A>>()

    fun add(vertexContainer: VertexContainer<V, I, G, A>) {
        val existingValue = map.put(vertexContainer.vertex.id, vertexContainer)
        check(existingValue == null) {
            "A vertex with the id ${vertexContainer.vertex.id} already exists in the graph."
        }
    }

    fun build() : Graph<V, I, G, A> {
        assertNoDanglingEdges()

        return Graph(map)
    }

    private fun assertNoDanglingEdges() {
        map.forEach { (_, vertexContainer) ->
            vertexContainer.adjacentOrdered.forEach { edge ->
                checkNotNull(map[edge.to]) {
                    "The vertex pointed to by the edge from ${vertexContainer.vertex.id} to ${edge.to} is missing"
                }
            }
        }
    }
}