package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.AssertionUtils
import mdk.test.utils.TestMachineContext

class ConditionalPathSelectionSpec : BehaviorSpec({

    Given("An 8-vertex graph with transition hooks for blocking specific paths") {
        val machineContext = TestMachineContext()
        val traverser = GraphScenarios.conditionalEightVertexTraverser(
            machineContext = machineContext,
            edgeTraversalType = EdgeTraversalType.DFSAcyclic
        )

        When("Traversal to vertices 2 and 7 is blocked") {
            machineContext.blockedGoingTo2 = true
            machineContext.blockedGoingTo7 = true

            Then("Traversal takes the unblocked path through vertices 3, 5, 6") {
                AssertionUtils.assertExpectedPathGoingNextUntilEnd(
                    traverser,
                    listOf("1", "3", "5", "6")
                )

                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = listOf("1", "3", "5", "6"),
                    traverser = traverser
                )
            }
        }

        When("Graph is reset and traversal to vertex 3 is blocked") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            machineContext.blockedGoingTo3 = true

            Then("Traversal takes the alternate path through vertices 2, 4, 8") {
                AssertionUtils.assertExpectedPathGoingNextUntilEnd(
                    expectedPath = listOf("1", "2", "4", "8"),
                    traverser = traverser
                )

                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = listOf("1", "2", "4", "8"),
                    traverser = traverser
                )
            }
        }
    }
})