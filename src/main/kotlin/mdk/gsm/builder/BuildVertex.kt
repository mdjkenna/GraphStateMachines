@file:Suppress("unused")

package mdk.gsm.builder

import mdk.gsm.graph.Edge
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.VertexContainer
import mdk.gsm.state.BeforeVisitHandler
import mdk.gsm.state.OutgoingTransitionHandler

@GsmBuilderScope
/**
 * DSL scope for configuring a single vertex in the graph.
 *
 */
class VertexBuilderScope<V, I, G, A> internal constructor(
    private val vertexContainerBuilder: VertexBuilder<V, I, G, A>
) where V : IVertex<I> {

    /**
     * Adds an outgoing edge from this vertex to a destination vertex.
     *
     * Ordering:
     * - When [autoOrder] is `true` (default), [Edge.order] is assigned automatically in insertion order.
     * - When `false`, you must set the order explicitly via [EdgeBuilderScope.setOrder].
     *
     * Destination:
     * - Set the target via [EdgeBuilderScope.setTo] using an `id` or a vertex instance.
     * - Optionally set a transition guard with [EdgeBuilderScope.setEdgeTransitionGuard].
     *
     * Example:
     * ```kotlin
     * // Auto-ordered edge
     * addEdge {
     *     setTo(MyVertex.Next)
     * }
     *
     * // Manually ordered edge
     * addEdge(autoOrder = false) {
     *     setOrder(10)
     *     setTo(MyVertex.Done)
     * }
     * ```
     *
     * @param autoOrder If `true`, assign [Edge.order] automatically; otherwise set it manually.
     * @param edgeBuilderScope DSL to configure the edge via [EdgeBuilderScope].
     */
    fun addEdge(
        autoOrder : Boolean = true,
        edgeBuilderScope : EdgeBuilderScope<V, I, G, A>.() -> Unit
    ) {
        val edgeBuilder = EdgeBuilder<V, I, G, A>(vertexContainerBuilder.stepInstance)

        edgeBuilderScope(EdgeBuilderScope(edgeBuilder))
        if (autoOrder) {
            edgeBuilder.order = vertexContainerBuilder.numberOfEdges
        }

        vertexContainerBuilder.addOutgoingEdge(
            edgeBuilder.build()
        )
    }

    /**
     * Shorthand for [addEdge] with identical semantics and parameters.
     */
    fun e(
        autoOrder: Boolean = true,
        edgeBuilderScope: EdgeBuilderScope<V, I, G, A>.() -> Unit
    ) = addEdge(autoOrder, edgeBuilderScope)

    /**
     * Sets a [BeforeVisitHandler] that runs just before this vertex is published as the current state.
     *
     * Typical uses:
     * - Perform setup work before observers see this state
     * - Mark the vertex as an intermediate state by calling `autoAdvance()` in the handler, so it is not published
     *
     * Example:
     * ```kotlin
     * onBeforeVisit {
     *     // perform setup
     *     // optionally skip publishing this vertex
     *     // autoAdvance()
     * }
     * ```
     *
     * @see BeforeVisitHandler
     * @see mdk.gsm.state.BeforeVisitScope
     */
    fun onBeforeVisit(handler: BeforeVisitHandler<V, I, G, A>) {
        vertexContainerBuilder.beforeVisitHandler = handler
    }

    /**
     * Sets an [OutgoingTransitionHandler] that runs before any outgoing transitions are explored.
     *
     * Use this to prevent transitions entirely by calling `noTransition()`, keeping the state machine at
     * the current vertex.
     *
     * Example:
     * ```kotlin
     * onOutgoingTransition {
     *     val allowed = /* validate */ true
     *     if (!allowed) noTransition()
     * }
     * ```
     *
     * @see OutgoingTransitionHandler
     * @see mdk.gsm.state.OutgoingTransitionScope
     */
    fun onOutgoingTransition(handler: OutgoingTransitionHandler<V, I, G, A>) {
        vertexContainerBuilder.outgoingTransitionHandler = handler
    }
}

internal class VertexBuilder<V, I, G, A>(
    internal val stepInstance: V
) where V : IVertex<I> {

    private val adjacent = HashMap<I, Edge<V, I, G, A>>()
    var beforeVisitHandler: BeforeVisitHandler<V, I, G, A>? = null
    var outgoingTransitionHandler: OutgoingTransitionHandler<V, I, G, A>? = null

    val numberOfEdges: Int
        get() = adjacent.size

    fun addOutgoingEdge(edge: Edge<V, I, G, A>) {
        adjacent[edge.to] = edge
    }

    fun build(): VertexContainer<V, I, G, A> {
        val sortedEdges : List<Edge<V, I, G, A>> = adjacent.values.sortedBy {
            it.order
        }

        return VertexContainer(
            vertex = stepInstance,
            adjacentOrdered = sortedEdges,
            beforeVisitHandler = beforeVisitHandler,
            outgoingTransitionHandler = outgoingTransitionHandler
        )
    }
}
