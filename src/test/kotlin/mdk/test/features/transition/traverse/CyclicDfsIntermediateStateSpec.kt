package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.builder.buildGuardedTraverser
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.util.StringVertex
import mdk.test.utils.AssertionUtils.assertTracedPathWithCurrentState
import mdk.test.utils.TestTransitionGuardState

class CyclicDfsIntermediateStateSpec : BehaviorSpec({

    Given("A cyclic DFS traverser with intermediate auto-advancing states") {
        val beforeVisitCalls = mutableListOf<String>()
        val guardState = TestTransitionGuardState()

        val start = StringVertex("start")
        val intermediate1 = StringVertex("intermediate1")
        val regular1 = StringVertex("regular1")
        val intermediate2 = StringVertex("intermediate2")
        val regular2 = StringVertex("regular2")
        val end = StringVertex("end")

        val traverser = buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSCyclic)

            buildGraph(start) {
                addVertex(start) {
                    addEdge {
                        setTo(intermediate1)
                    }
                }
                addVertex(intermediate1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge {
                        setTo(regular1)
                    }
                }
                addVertex(regular1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                    addEdge {
                        setTo(intermediate2)
                    }
                }
                addVertex(intermediate2) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge {
                        setTo(regular2)
                    }
                }
                addVertex(regular2) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                    addEdge {
                        setTo(end)
                    }
                }
                addVertex(end) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                }
            }
        }

        When("A NEXT action advances through an intermediate state") {
            val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The intermediate state is skipped and the regular state is published") {
                result.vertex.id shouldBe "regular1"
                beforeVisitCalls shouldBe listOf("intermediate1", "regular1")
            }

            Then("The traced path includes the intermediate state") {
                assertTracedPathWithCurrentState(
                    listOf("start", "intermediate1", "regular1"),
                    traverser
                )
            }
        }

        When("A second NEXT action advances through another intermediate state") {
            val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The second intermediate state is skipped") {
                result.vertex.id shouldBe "regular2"
            }

            Then("The traced path includes both intermediate states") {
                assertTracedPathWithCurrentState(
                    listOf("start", "intermediate1", "regular1", "intermediate2", "regular2"),
                    traverser
                )
            }
        }

        When("A PREVIOUS action is dispatched from regular2") {
            val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

            Then("The traverser skips the intermediate state and lands on regular1") {
                result.vertex.id shouldBe "regular1"
            }
        }

        When("A NEXT action is dispatched after moving previous") {
            val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The traverser advances through the intermediate state again") {
                result.vertex.id shouldBe "regular2"
            }
        }
    }

    Given("A cyclic DFS traverser with explicit transition into bounds enabled") {
        val guardState = TestTransitionGuardState()

        val v1 = StringVertex("1")
        val v2 = StringVertex("2")

        val traverser = buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSCyclic)
            setExplicitTransitionIntoBounds(true)

            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                }
                addVertex(v2)
            }
        }

        When("The traverser advances beyond the last vertex") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            val beyondResult = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The traverser is beyond last bounds") {
                beyondResult.isBeyondLast shouldBe true
                beyondResult.vertex.id shouldBe "2"
            }
        }

        When("A PREVIOUS action is dispatched while beyond last") {
            val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

            Then("The traverser transitions back into bounds on the same vertex") {
                result.isWithinBounds shouldBe true
                result.vertex.id shouldBe "2"
            }
        }
    }
})
