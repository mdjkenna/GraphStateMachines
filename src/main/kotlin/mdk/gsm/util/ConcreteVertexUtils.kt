@file:Suppress("unused")

package mdk.gsm.util

import mdk.gsm.graph.Vertex

/**
 * A vertex with an [id] of type [String].
 * @property id The [String] identifier of the vertex.
 */
data class StringVertex(
    override val id: String,
) : Vertex<String>

/**
 * A vertex with an [id] of type [Int].
 * @property id The [Int] identifier of the vertex.
 */
data class IntVertex(
    override val id: Int
) : Vertex<Int>

/**
 * A vertex with an [id] of type [Long].
 * @property id The [Long] identifier of the vertex.
 */
data class LongVertex(
    override val id: Long
) : Vertex<Long>

/**
 * A vertex with an [id] of type [Char].
 * @property id The [Char] identifier of the vertex.
 */
data class CharVertex(
    override val id: Char
) : Vertex<Char>

/**
 * A vertex with an [id] of type [Byte].
 * @property id The [Byte] identifier of the vertex.
 */
data class ByteVertex(
    override val id: Byte
) : Vertex<Byte>

/**
 * A vertex with an [id] of type [Short].
 * @property id The [Short] identifier of the vertex.
 */
data class ShortVertex(
    override val id: Short
) : Vertex<Short>