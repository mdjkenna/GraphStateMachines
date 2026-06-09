package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex

enum class HistoryType {
    Shallow,
    Deep
}

data class HistoryMarker<V, I>(
    val vertex: V,
    val ownerStateId: I,
    val type: HistoryType,
    val defaultTarget: EntryTarget<I>?,
    val declarationOrdinal: Int
) where V : Vertex<I> {
    val id: I
        get() = vertex.id
}
