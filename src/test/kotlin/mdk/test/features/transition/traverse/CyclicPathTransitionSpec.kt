package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.AssertionUtils
import mdk.test.utils.Test15VertexTransitionArgs

class CyclicPathTransitionSpec : BehaviorSpec({

    Given("A 15-vertex graph with multiple cyclic paths using cyclic DFS traversal") {
        val transitionGuardState = Test15VertexTransitionArgs()
        val traverser = GraphScenarios.complex15VertexTraverser(
            guardState = transitionGuardState,
            edgeTraversalType = EdgeTraversalType.DFSCyclic
        )
        
        var expectedPath = "1, 2, 4, 6, 3, 2, 4, 6, 3, 2, 4, 6, 3".split(", ")

        When("Multiple NEXT actions are dispatched that explore cyclic paths") {
            repeat(12) {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }

            Then("The traced path should contain multiple loops around the cycle") {
                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = expectedPath,
                    traverser = traverser
                )
            }
        }

        When("A traversal guard breaks the cycle's infinite loop") {
            transitionGuardState.blockedFrom3To2 = true

            repeat(14) {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }

            Then("GSM transitions across the edge immediately after the blocked one and continues through the graph to another cycle") {
                expectedPath += ("7, 8, 9, 11, 12, 14, 5, 8, 9, 11, 12, 14, 5, 8".split(", "))

                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = expectedPath,
                    traverser = traverser
                )
            }
        }

        When("A traversal guard brakes the second loop encountered after the first loop was exited") {

            transitionGuardState.blockedFrom8To9 = true

            repeat(2) {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }


            Then("The state machines transitions skip the blocked edge and traverses the edge immediately after") {
                expectedPath += "10, 11".split(", ")

                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = expectedPath,
                    traverser = traverser
                )
            }
        }

        When("There are a series of PREVIOUS actions received by the state machine") {

            for (i in expectedPath.lastIndex downTo 0) {
                Then("The previous state is becomes the current one, with the last element removed from the traced path: $i") {
                    AssertionUtils.assertTracedPathWithCurrentState(
                        expectedPath = expectedPath.slice(0..i),
                        traverser = traverser
                    )

                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                }
            }
        }
    }
})