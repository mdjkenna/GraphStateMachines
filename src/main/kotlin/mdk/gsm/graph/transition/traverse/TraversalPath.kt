package mdk.gsm.graph.transition.traverse

import mdk.gsm.graph.Vertex

internal class TraversalPath<V, I, A>
        where V : Vertex<I> {

    var pathHead: TraversalPathNode<V, A>
        private set

    constructor(startVertex: V) {
        pathHead = TraversalPathNode(left = null, vertex = startVertex)
    }

    val currentVertex: V
        get() = pathHead.vertex

    fun appendPathHead(
        vertex: V,
        isAutoAdvancing: Boolean = false,
        args: A?
    ): TraversalPathNode<V, A> {
        val oldPathHead = if (isAutoAdvancing && !pathHead.isIntermediate ) {
            pathHead.copy(isIntermediate = true)
        } else {
            pathHead
        }

        val newPathHead = TraversalPathNode(
            vertex = vertex,
            left = oldPathHead,
            args = args
        )

        pathHead = newPathHead

        return newPathHead
    }

    fun setPathHead(node: TraversalPathNode<V, A>) {
        pathHead = node
    }

    fun tracePath(): List<V> {
        val path = ArrayList<V>()
        var current : TraversalPathNode<V, A>? = pathHead

        while (current != null) {
            path.add(current.vertex)
            current = current.left
        }

        path.reverse()

        return path
    }

    fun reset(startVertex: V): V {
        pathHead = TraversalPathNode(vertex = startVertex)
        return startVertex
    }
}

internal data class TraversalPathNode<V : Vertex<*>, out A>(
    val vertex: V,
    val left: TraversalPathNode<V, A>? = null,
    val isIntermediate: Boolean = false,
    val args : A? = null
) {
    override fun toString(): String {
        return "TraversalPathNode(vertex=${vertex.id}, isIntermediate=$isIntermediate, left=${left?.vertex?.id})"
    }
}