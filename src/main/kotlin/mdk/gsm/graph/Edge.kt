package mdk.gsm.graph

import mdk.gsm.state.BeforeTransitionHook
import mdk.gsm.state.BeforeTransitionScope
import mdk.gsm.state.MachineContext

/**
 * Represents a directed outgoing edge within a graph connecting a source vertex [from] to a destination vertex [to].
 * Edges define the possible transitions between states in the graph state machine.
 *
 * Edges are defined as outgoing from a vertex, and outgoing edges are traversed according to their [order] value,
 * used to prioritize transitions when multiple edges originate from the same vertex.
 * A vertex's edges are traversed in ascending order of [order].
 *
 * The [beforeTransitionHook] acts as a dynamic runtime condition. If [BeforeTransitionScope.blockTransition] is called
 * within the hook, the traversal is blocked; otherwise, it's allowed. If no [beforeTransitionHook] has been set then
 * traversal will not be blocked.
 *
 * @param V The type of the vertices (states). Must implement [Vertex].
 * @param I The type of the vertex identifiers.
 * @param G The type of the machine context. Must implement [MachineContext].
 * @param order The priority of this edge during traversal. Lower values are evaluated first.
 * @param from The source vertex of this edge.
 * @param to The identifier of the destination vertex.
 * @param beforeTransitionHook An optional function that controls whether this edge can be traversed at runtime.
 */
class Edge<out V, I, G, A> internal constructor(
    val order: Int,
    val from: V,
    val to: I,
    private val beforeTransitionHook: BeforeTransitionHook<V, I, G, A>?
) where V : Vertex<I> {

    internal suspend fun canProceed(
        flags: G?,
        args : A?,
    ): Boolean {
        return if (beforeTransitionHook == null) {
            true
        } else {
            val scope = BeforeTransitionScope(from, flags, args)
            beforeTransitionHook.invoke(scope)
            !scope.isTransitionBlocked
        }
    }
}
