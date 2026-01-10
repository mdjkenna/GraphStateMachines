package mdk.test.features.transition.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestMachineContext

class WalkerDeadEndNavigationSpec : BehaviorSpec({

    Given("A walker with dead ends and transition hooks") {
        val machineContext = TestMachineContext()
        val walker = GraphScenarios.conditionalEightVertexWalker(machineContext)

        When("The walker navigates to a vertex with no outgoing edges") {
            // Path: 1 -> 2 -> 4 -> 8 (dead end)
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next) // 1 -> 2
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next) // 2 -> 4
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next) // 4 -> 8

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
            When("A transition hook blocks one outgoing edge (1->2), but another is available (1->3)") {
                machineContext.blockedGoingTo2 = true

                val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

                Then("The walker should move to the unblocked edge (vertex 3)") {
                    result.vertex.id shouldBe "3"
                }
            }
        }

        When("The walker tries to navigate further to vertex 5") {
            val nextResult = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should move to vertex 5") {
                nextResult.vertex.id shouldBe "5"
            }
        }
    }
})