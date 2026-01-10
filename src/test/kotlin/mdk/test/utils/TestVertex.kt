package mdk.test.utils

import mdk.gsm.graph.Vertex

open class TestVertex(override val id: String) : Vertex<String> {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TestVertex) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }

    override fun toString(): String {
        return id
    }
}

class SubTestVertex(stepId: String) : TestVertex(stepId) {
    val testField : Boolean = true
}