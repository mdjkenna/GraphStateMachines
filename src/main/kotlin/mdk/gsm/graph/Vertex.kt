package mdk.gsm.graph

/**
 * Represents a vertex.
 * Note the [id] property should have an immutable type.
 *
 * @param I The type of the vertex ID, which should be completely immutable. The [id] must be unique within the graph.
 * @property id The unique identifier for the vertex of type [I]
 */
interface Vertex<I> {
    val id : I
}

