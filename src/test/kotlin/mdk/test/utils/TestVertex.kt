package mdk.test.utils

import mdk.gsm.graph.IVertex

open class TestVertex(override val id: String) : IVertex<String> {
    override fun toString(): String {
        return id
    }
}

class SubTestVertex(stepId: String) : TestVertex(stepId) {
    val testField : Boolean = true
}