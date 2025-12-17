@file:Suppress("unused")

package mdk.gsm.builder

import mdk.gsm.graph.Edge
import mdk.gsm.graph.IVertex
import mdk.gsm.state.TransitionGuard

@GsmBuilderScope
/**
 * DSL scope for configuring a single outgoing [Edge] from a vertex.
 *
 * Example:
 * ```kotlin
 * addEdge(autoOrder = false) {
 *     setOrder(5)
 *     setTo(MyVertex.Next)
 *     setEdgeTransitionGuard { /* return true to allow transition */ true }
 * }
 * ```
 */
class EdgeBuilderScope<V, I, G, A> internal constructor(
    private val edgeBuilder: EdgeBuilder<V, I, G, A>
) where V : IVertex<I> {

    /**
     * Sets the traversal order for this edge. Lower values are evaluated first.
     * @param order The numeric order used to sort edges for traversal.
     */
    fun setOrder(order: Int) {
        edgeBuilder.order = order
    }

    /**
     * Sets the target vertex of the edge by its identifier.
     * @param to The id of the target vertex of the edge.
     */
    fun setTo(to: I) {
        edgeBuilder.to = to
    }

    /**
     * Sets the target vertex of the edge using a vertex instance.
     * @param to The vertex instance to target.
     */
    fun setTo(to : V) {
        edgeBuilder.to = to.id
    }

    /**
     * Sets the [TransitionGuard] for this edge.
     * @see TransitionGuard
     *
     * @param transitionGuard The function controlling traversal across this edge. Receives a
     *   [mdk.gsm.state.TransitionGuardScope] and returns `true` to allow the traversal, `false` to prevent it.
     */
    fun setEdgeTransitionGuard(transitionGuard : TransitionGuard<V, I, G, A>) {
        edgeBuilder.transitionGuard = transitionGuard
    }
}

internal class EdgeBuilder<V, I, G, A>(
    private val from : V
) where V : IVertex<I> {
    var order = 0
    var to : I? = null
    var transitionGuard : TransitionGuard<V, I, G, A>? = null

    fun build() : Edge<V, I, G, A> {

        val localTo = to
        check(localTo != null)

        return Edge(
            order = order,
            from = from,
            to = localTo,
            transitionGuard = transitionGuard
        )
    }
}
