package mdk.gsm.graph

import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.state.TransitionGuard
import mdk.gsm.state.TransitionGuardScope

/**
 * Represents a directed outgoing edge within a graph connecting a source vertex [from] to a destination vertex [to].
 * Edges define the possible transitions between states in the graph state machine.
 *
 * Edges are defined as outgoing from a vertex, and outgoing edges are traversed according to their [order] value,
 * used to prioritize transitions when multiple edges originate from the same vertex.
 * A vertex's edges are traversed in ascending order of [order].
 *
 * The [transitionGuard] acts as a dynamic runtime condition. If the [TransitionGuard] function returns `true`, the traversal is allowed;
 * otherwise, it's blocked. If no [transitionGuard] has been set then traversal will not be blocked.
 *
 * @param V The type of the vertices (states). Must implement [IVertex].
 * @param I The type of the vertex identifiers.
 * @param G The type of the edge traversal guard. Must implement [ITransitionGuardState].
 * @param order The priority of this edge during traversal. Lower values are evaluated first.
 * @param from The source vertex of this edge.
 * @param to The identifier of the destination vertex.
 * @param transitionGuard An optional function that controls whether this edge can be traversed at runtime.
 */
class Edge<out V, I, G, A> internal constructor(
    val order: Int,
    val from: V,
    val to: I,
    private val transitionGuard: TransitionGuard<V, I, G, A>?
) where V : IVertex<I> {

    internal suspend fun canProceed(
        flags: G?,
        args : A?,
    ): Boolean {
        return if (transitionGuard == null) {
            true
        } else {
            val scope = TransitionGuardScope(from, flags, args)
            transitionGuard.invoke(scope)
        }
    }
}
