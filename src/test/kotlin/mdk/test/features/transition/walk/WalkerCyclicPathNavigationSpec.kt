package mdk.test.features.transition.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.Test15VertexTransitionArgs

class WalkerCyclicPathNavigationSpec : BehaviorSpec({
    Given("A 15-vertex walker with multiple cyclic paths") {
        val transitionGuardState = Test15VertexTransitionArgs()
        val walker = GraphScenarios.complex15VertexWalker(transitionGuardState)

        When("The walker has multiple NEXT actions dispatched that take it through a cyclic path which is unblocked") {
            // First cycle: 1 -> 2 -> 4 -> 6 -> 3 -> 2 (repeat)

            // Move to vertex 2
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 4
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 6
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 3
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 2 (completing the cycle)
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should have navigated through the cycle and returned to vertex 2") {
                result.vertex.id shouldBe "2"
            }
        }

        When("The walker continues through the cycle multiple times") {
            // Continue from vertex 2 to vertex 4
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 6
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 3
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 2 (completing the cycle again)
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should have navigated through the cycle again and returned to vertex 2") {
                result.vertex.id shouldBe "2"
            }
        }

        When("A traversal guard breaks the cycle's infinite loop") {
            transitionGuardState.blockedFrom3To2 = true

            // Continue from vertex 2 to vertex 4
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 6
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 3
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Try to move to vertex 2, but it's blocked, so move to vertex 7 instead
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should have broken out of the cycle and moved to vertex 7") {
                result.vertex.id shouldBe "7"
            }
        }

        When("The walker navigates to another cycle") {
            // Continue from vertex 7 to vertex 8
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 9
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 11
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 12
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 14
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 5
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            // Move to vertex 8 (completing the second cycle)
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should have navigated through the second cycle and returned to vertex 8") {
                result.vertex.id shouldBe "8"
            }
        }

        When("A traversal guard breaks the second cycle") {
            transitionGuardState.blockedFrom8To9 = true

            // Try to move to vertex 9, but it's blocked, so move to vertex 10 instead
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should have broken out of the second cycle and moved to vertex 10") {
                result.vertex.id shouldBe "10"
            }
        }

        When("The walker is reset") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

            Then("The walker should return to the start vertex") {
                result.vertex.id shouldBe "1"
            }
        }
    }
})