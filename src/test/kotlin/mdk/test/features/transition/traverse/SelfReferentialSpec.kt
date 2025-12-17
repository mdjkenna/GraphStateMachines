package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import mdk.gsm.builder.buildGuardedTraverser
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.util.StringVertex
import mdk.test.utils.AssertionUtils

class SelfReferentialSpec : BehaviorSpec({

    Given("A graph with a self-referential vertex limited to 7 cycles by guard state") {
        var cycleCount = 0
        val transitionGuardState = object : ITransitionGuardState {
            override fun onReset() { cycleCount = 0 }
        }

        val traverser = buildGuardedTraverser(transitionGuardState) {
            val v1 = StringVertex("1")
            val v2 = StringVertex("2")
            setTraversalType(EdgeTraversalType.DFSCyclic)
            
            buildGraph(v1) {
                v(v1) {
                    e {
                        setTo(v1)
                        setEdgeTransitionGuard {
                            if (cycleCount < 7) {
                                cycleCount++
                                true
                            } else {
                                false
                            }
                        }
                    }

                    e {
                        setTo(v2)
                    }
                }

                v(v2)
            }
        }

        When("Advancing the state machine forward seven times") {
            repeat(7) {
                traverser.dispatch(GraphStateMachineAction.Next)
            }

            Then("The traversal path should contain eight occurrences of vertex '1'") {
                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = List(8) { "1" },
                    traverser = traverser
                )
            }
        }

        When("Advancing once more after reaching the cycle guard limit") {
            traverser.dispatch(GraphStateMachineAction.Next)

            Then("The traversal should proceed to vertex '2' instead of continuing the cycle") {
                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = List(8) { "1" } + "2",
                    traverser = traverser
                )
            }
        }

        When("Reversing the state machine six steps backward") {
            repeat(6) {
                traverser.dispatch(GraphStateMachineAction.Previous)
            }

            Then("The path should be reduced to only three occurrences of vertex '1'") {
                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = List(3) { "1" },
                    traverser = traverser
                )
            }
        }

        When("Manually setting the cycle count to 2 and advancing six more times") {
            cycleCount = 2

            repeat(6) {
                traverser.dispatch(GraphStateMachineAction.Next)
            }

            Then("The path should contain eight '1' vertices followed by vertex '2'") {
                AssertionUtils.assertTracedPathWithCurrentState(
                    expectedPath = List(8) { "1" } + "2",
                    traverser = traverser
                )
            }
        }
    }
})