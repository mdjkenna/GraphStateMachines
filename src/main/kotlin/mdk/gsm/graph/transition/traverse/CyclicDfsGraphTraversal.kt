package mdk.gsm.graph.transition.traverse

import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.VertexContainer
import mdk.gsm.graph.transition.IForwardTransition
import mdk.gsm.graph.transition.IPathTraceable
import mdk.gsm.graph.transition.IPreviousTransition
import mdk.gsm.graph.transition.IResettable

internal class CyclicDfsGraphTraversal<V, I, G, A>(
    private val graph: Graph<V, I, G, A>,
    private val startVertex: V,
) : IForwardTransition<V, I, G, A>, IPreviousTransition<V, I, G, A>, IResettable<V>, IPathTraceable<V> where V : IVertex<I> {

    private var traversalPath = TraversalPath<V, I, A>(startVertex)

    private val visited: HashMap<I, Int> = HashMap()

    override suspend fun moveNext(
        guardState: G?,
        autoAdvance: Boolean,
        args: A?
    ): TraversalPathNode<V, A>? {
        visited[traversalPath.currentVertex.id] = VertexColor.GRAY

        val nextAdjacentStep = nextVertexOrNull(traversalPath.currentVertex, guardState, args)
        if (nextAdjacentStep != null) {
            return traversalPath.appendPathHead(
                vertex = nextAdjacentStep,
                isAutoAdvancing = autoAdvance,
                args = args
            )
        }

        var current = traversalPath.pathHead.left
        while (current != null) {
            val next = nextVertexOrNull(current.vertex, guardState, args)
            if (next == null) {
                current = current.left
            } else {
                return traversalPath.appendPathHead(
                    vertex = next,
                    isAutoAdvancing = autoAdvance,
                    args = args
                )
            }
        }

        // If no valid edge is found, return null
        return null
    }

    private suspend fun nextVertexOrNull(
        vertex: V,
        flags: G?,
        args : A?
    ): V? {

        val sortedEdges = graph.getOutgoingEdgesSorted(vertex).orEmpty()

        for (i in sortedEdges.indices) {
            val edge = sortedEdges[i]

            val toVertexColor = visited.getOrDefault(edge.to, 0)
            if (VertexColor.isBlack(toVertexColor)) {
                continue
            }

            if (!edge.canProceed(flags, args)) {
                continue
            }

            return graph.getVertex(edge.to)
        }

        visited[vertex.id] = VertexColor.BLACK
        return null
    }

    override fun currentStep(): V {
        return traversalPath.currentVertex
    }

    override suspend fun movePrevious(): TraversalPathNode<V, A>? {
        var left = traversalPath.pathHead.left
            ?: return null // Return null if no previous node

        visited.remove(traversalPath.currentVertex.id)

        while (true) {
            visited.remove(left.vertex.id)
            if (left.isIntermediate && left.left != null) {
                left = left.left
            } else {
                break
            }
        }

        traversalPath.setPathHead(left)
        return left
    }

    override fun reset(): V {
        visited.clear()
        traversalPath.reset(startVertex)

        return startVertex
    }

    override fun head() : TraversalPathNode<V, A> {
        return traversalPath.pathHead
    }

    override fun tracePath(): List<V> {
        return traversalPath.tracePath()
    }

    override fun getVertexContainer(id: I): VertexContainer<V, I, G, A>? {
        return graph.getVertexContainer(id)
    }
}
