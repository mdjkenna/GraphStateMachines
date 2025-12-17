package mdk.test.features.action

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerActionWithArgumentsSpec : BehaviorSpec({
    Given("A walker with conditional edges based on action arguments") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.nextActionWithArgsWalker(
            guardState = guardState,
            argsGoTo3 = TestArgs(TestArgs.ARGS_GO_TO_3),
            argsGoTo4 = TestArgs(TestArgs.ARGS_GO_TO_4)
        )

        When("A Next action is dispatched without arguments") {
            val noArgsActionResult = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker transitions to vertex 2") {
                walker.current.value.vertex.id shouldBe "2"
            }

            Then("The resulting state contains no arguments") {
                noArgsActionResult.args shouldBe null
            }
        }

        When("A NextArgs action with an argument for vertex 4 is dispatched") {
            val argsActionResult = walker.dispatchAndAwaitResult(
                GraphStateMachineAction.NextArgs(TestArgs(TestArgs.ARGS_GO_TO_4))
            )

            Then("The walker follows the conditional path to vertex 4 and auto-advances to vertex 5") {
                walker.current.value.vertex.id shouldBe "5"
            }

            Then("The resulting state preserves the action arguments") {
                argsActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_4)
            }
        }

        When("A Reset action is dispatched") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

            Then("The walker returns to the start vertex") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }

        When("A NextArgs action with an argument for vertex 3 is dispatched") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            val nextActionResult = walker.dispatchAndAwaitResult(
                GraphStateMachineAction.NextArgs(TestArgs(TestArgs.ARGS_GO_TO_3))
            )

            Then("The walker follows the conditional path to vertex 3") {
                walker.current.value.vertex.id shouldBe "3"
            }

            Then("The resulting state preserves the action arguments") {
                nextActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_3)
            }
        }
    }
})