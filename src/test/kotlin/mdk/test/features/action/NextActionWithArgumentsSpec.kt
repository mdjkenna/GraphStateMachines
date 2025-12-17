package mdk.test.features.action

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.AssertionUtils
import mdk.test.utils.TestTransitionGuardState

class NextActionWithArgumentsSpec : BehaviorSpec(
    body = {
        Given("A traverser with conditional edges based on NextArgs values") {
            val guardState = TestTransitionGuardState()
            val traverser = GraphScenarios.nextActionWithArgsTraverser(
                guardState = guardState,
                argsGoTo3 = TestArgs(TestArgs.ARGS_GO_TO_3),
                argsGoTo4 = TestArgs(TestArgs.ARGS_GO_TO_4)
            )

            When("A Next action is dispatched without arguments") {

                val noArgsActionResult =
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

                Then("Traversal proceeds from vertex 1 to vertex 2") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2"),
                        traverser
                    )
                }

                Then("The resulting state contains no arguments") {
                    noArgsActionResult.args shouldBe null
                }
            }

            When("A NextArgs action is dispatched with an argument matching the edge guard to vertex 4") {
                val argsActionResult = traverser.dispatchAndAwaitResult(
                    GraphStateMachineAction.NextArgs(
                        TestArgs(TestArgs.ARGS_GO_TO_4)
                    )
                )

                Then("Traversal follows the conditional path to vertex 4 and auto-advances to vertex 5") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2", "4", "5"),
                        traverser
                    )
                }

                Then("The resulting state preserves the action arguments") {
                    argsActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_4)
                }
            }

            When("A Previous action is dispatched after traversing with arguments") {
                val previousActionResult = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

                Then("Traversal returns to vertex 2 where the NextArgs action was originally received") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2"),
                        traverser
                    )
                }

                Then("The resulting state contains no arguments as the original forward transition had none") {
                    previousActionResult.args shouldBe null
                }
            }

            When("A NextArgs action is dispatched with an argument matching the edge guard to vertex 3") {
                val nextActionResult = traverser.dispatchAndAwaitResult(
                    GraphStateMachineAction.NextArgs(
                        TestArgs(TestArgs.ARGS_GO_TO_3)
                    )
                )

                Then("Traversal follows the alternate conditional path to vertex 3") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2", "3"),
                        traverser
                    )
                }

                Then("The resulting state preserves the action arguments") {
                    nextActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_3)
                }
            }

            When("A NextArgs action is dispatched with arguments that do not affect available edges") {
                val nextActionResult = traverser.dispatchAndAwaitResult(
                    GraphStateMachineAction.NextArgs(
                        TestArgs(TestArgs.ARGS_GO_TO_4)
                    )
                )

                Then("Traversal follows the only available edge to vertex 6") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2", "3", "6"),
                        traverser
                    )
                }

                Then("The resulting state preserves the action arguments") {
                    nextActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_4)
                }
            }

            When("A Next action is dispatched from a terminal vertex") {
                val nextActionResult = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

                Then("The resulting state is beyond last") {
                    nextActionResult.isBeyondLast shouldBe true
                }

                Then("The resulting state contains no arguments") {
                    nextActionResult.args shouldBe null
                }
            }

            When("A Previous action is dispatched while beyond last") {
                val previousActionResult = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

                Then("Traversal returns to the last in-bounds state with expected path") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2", "3", "6"),
                        traverser
                    )
                }

                Then("The resulting state preserves arguments from the previous in-bounds transition") {
                    previousActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_4)
                }
            }

            When("A Previous action is dispatched when the prior forward transition had arguments") {
                val previousActionResult = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

                Then("Traversal returns to the previous state with expected path") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        listOf("1", "2", "3"),
                        traverser
                    )
                }

                Then("The resulting state preserves arguments from the prior forward transition") {
                    previousActionResult.args shouldBe TestArgs(TestArgs.ARGS_GO_TO_3)
                }
            }
        }
    }
)


data class TestArgs(val id: Int) {
    companion object {
        const val ARGS_GO_TO_3 = 3
        const val ARGS_GO_TO_4 = 4
    }
}
