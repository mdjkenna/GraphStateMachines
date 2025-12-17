package mdk.test.features.walker

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.coroutines.runBlocking
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerGraphSupplierSpec : BehaviorSpec({
    Given("A linear walker exposing its graph via GraphSupplier") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)

        When("Reading graph vertices and edges from the walker") {
            val graph = walker.graph
            val startVertex = graph.getVertex("1")
            val secondVertex = graph.getVertex("2")
            val thirdVertex = graph.getVertex("3")
            val startEdges = graph.getOutgoingEdgesSorted(startVertex!!)
            val secondEdges = graph.getOutgoingEdgesSorted(secondVertex!!)
            val thirdEdges = graph.getOutgoingEdgesSorted(thirdVertex!!)

            Then("The graph contains all vertices and correct outgoing edges") {
                listOf(startVertex.id, secondVertex.id, thirdVertex.id) shouldContainAll listOf("1", "2", "3")
                startEdges shouldNotBe null
                startEdges!!.map { it.to } shouldBe listOf("2")
                secondEdges shouldNotBe null
                secondEdges!!.map { it.to } shouldBe listOf("3")
                thirdEdges shouldNotBe null
                thirdEdges!!.isEmpty() shouldBe true
                graph.containsVertexId(walker.current.value.vertex.id) shouldBe true
            }
        }

        When("Transitioning state while observing the graph") {
            val graph = walker.graph
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }

            Then("The current vertex remains part of the same graph") {
                val currentId = walker.current.value.vertex.id
                graph.containsVertexId(currentId) shouldBe true
                val outgoing = graph.getOutgoingEdgesSorted(walker.current.value.vertex)
                outgoing shouldNotBe null
                outgoing!!.isEmpty() shouldBe true
            }
        }
    }
})
