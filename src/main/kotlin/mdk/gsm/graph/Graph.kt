@file:Suppress("unused")

package mdk.gsm.graph

import mdk.gsm.state.MachineContext

/**
 * Represents a graph data structure used for state machine transitions. This graph
 * stores vertices and their associated outgoing edges, enabling transition planning for
 * state progression. Vertices are identified by unique IDs of type `I`.
 *
 * The `Graph` class facilitates efficient lookups of vertices and their connected
 * edges. Defining all possible state transitions, [Graph] forms the main scaffolding for
 * the state machine's transition logic.
 *
 * The [Graph] class is immutable if the vertex implementations are immutable.
 *
 * @param V The type of vertices stored in this graph. Must implement [Vertex].
 * @param I The type of the vertex ID. Must correspond to the type parameter of [Vertex] implemented by [V].
 * @param G The type of machine context used for edge transitions. Must implement [MachineContext].
 * @param A The type of action argument
 */
class Graph<V, I, G, A> internal constructor(
    private val map: Map<I, VertexContainer<V, I, G, A>>
) where V : Vertex<I> {

    fun containsVertex(vertex: V): Boolean {
        return map.containsKey(vertex.id)
    }

    fun containsVertexId(vertexId: I): Boolean {
        return map.containsKey(vertexId)
    }

    fun getVertex(id: I): V? {
        return map[id]?.vertex
    }

    fun getOutgoingEdgesSorted(vertex: V): List<Edge<V, I, G, A>>? {
        return map[vertex.id]?.adjacentOrdered
    }

    internal fun getVertexContainer(id: I): VertexContainer<V, I, G, A>? {
        return map[id]
    }

    fun getAllVertices(): List<V> {
        return map.values.map { it.vertex }
    }

    fun getAllEdges(): List<Edge<V, I, G, A>> {
        return map.values.flatMap {
            it.adjacentOrdered
        }
    }
}
