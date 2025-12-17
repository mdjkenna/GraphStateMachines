package mdk.test.features.state.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios

class WalkerTraversalBoundsSpec : BehaviorSpec({
    Given("""
        A walker with simple 2 vertex graph that has one edge from the first to the second,
        which has been configured to explicitly step into bounds in the builder
    """.trimIndent()) {

        val walker = GraphScenarios.twoVertexBoundsWalker()

        When("Walker has just been initialised without having yet received any actions") {
            Then("The walker's current state is the start vertex, which is within bounds and not beyond last") {
                walker.current.value.isWithinBounds shouldBe true
                walker.current.value.isBeyondLast shouldBe false
            }
        }

        When("Dispatching a NEXT action when on the start vertex") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker moves to the next vertex, with the new state still within bounds") {
                result.isWithinBounds shouldBe true
                result.isBeyondLast shouldBe false
                result.vertex.id shouldBe 2L
            }
        }

        When("Dispatching a NEXT action when on a vertex with no other states available (a 'dead end')") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("""
                The walker's state updates with the same vertex but is now flagged as not within bounds. 
                The beyond last check is true.
            """) {
                result.isWithinBounds shouldBe false
                result.isBeyondLast shouldBe true
                result.vertex.id shouldBe 2L
            }
        }

        When("Walker is not within bounds and beyond last, and a RESET action is received") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

            Then("""
                Walker resets to the start vertex and is now flagged as within bounds.
                The beyond last check is false.
            """) {
                result.isWithinBounds shouldBe true
                result.isBeyondLast shouldBe false
                result.vertex.id shouldBe 1L
            }
        }

        When("Walker is reset and a NEXT action is received") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("Walker moves to the next vertex and remains within bounds") {
                result.isWithinBounds shouldBe true
                result.isBeyondLast shouldBe false
                result.vertex.id shouldBe 2L
            }
        }

        When("Walker is at the last vertex and a NEXT action is received again") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("Walker goes beyond last bounds") {
                result.isWithinBounds shouldBe false
                result.isBeyondLast shouldBe true
                result.vertex.id shouldBe 2L
            }
        }
    }
})