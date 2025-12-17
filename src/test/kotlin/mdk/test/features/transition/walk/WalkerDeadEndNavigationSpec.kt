package mdk.test.features.transition.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerDeadEndNavigationSpec : BehaviorSpec({

    Given("An 8-vertex walker with a graph containing dead ends") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.conditionalEightVertexWalker(guardState)

        When("The walker navigates to a vertex with no outgoing edges") {
            // Move from vertex 1 to vertex 2
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            // Move from vertex 2 to vertex 4
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move from vertex 4 to vertex 8 (dead end - no outgoing edges)
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should be at the dead end vertex") {
                result.vertex.id shouldBe "8"
            }

            When("The walker tries to navigate further from the dead end") {
                val nextResult = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

                Then("The walker should remain at the same vertex but be marked as beyond last") {
                    nextResult.vertex.id shouldBe "8"
                    nextResult.isBeyondLast shouldBe true
                }
            }
        }

        When("The walker is reset and navigates to a vertex with multiple outgoing edges, some blocked") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            guardState.blockedGoingTo7 = false
            guardState.blockedGoingTo2 = true

            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should be at vertex 3") {
                result.vertex.id shouldBe "3"
            }
        }

        When("The walker tries to navigate further") {
            val nextResult = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should move to the unblocked edge's destination") {
                nextResult.vertex.id shouldBe "5"
            }
        }
    }
})