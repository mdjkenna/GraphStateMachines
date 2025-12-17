package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.datatest.withData
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.state.traverser.Traverser
import mdk.test.utils.AssertionUtils
import mdk.test.utils.AssertionUtils.assertTracedPathWithCurrentState
import mdk.test.utils.TestBuilderUtils
import mdk.test.utils.TestBuilderUtils.v1
import mdk.test.utils.TestBuilderUtils.v2
import mdk.test.utils.TestBuilderUtils.v3
import mdk.test.utils.TestBuilderUtils.v4
import mdk.test.utils.TestBuilderUtils.v5
import mdk.test.utils.TestBuilderUtils.v6
import mdk.test.utils.TestBuilderUtils.v7
import mdk.test.utils.TestBuilderUtils.v8
import mdk.test.utils.TestTransitionGuardState

class GraphBidirectionalTraversalEquivalenceSpec : BehaviorSpec({

    Given("A graph state machine which is subjected to traversal in both next and previous directions") {

        withData(nameFn = { it.title },
            listOf<TestParameters>(
                createParam1("11 Vertex DAG Acyclic", EdgeTraversalType.DFSAcyclic),
                createParam1("11 Vertex DAG Cyclic", EdgeTraversalType.DFSCyclic),
                createParam2("8 Vertex DAG Forward Acyclic", EdgeTraversalType.DFSAcyclic),
                createParam2("8 Vertex DAG Forward Cyclic", EdgeTraversalType.DFSCyclic)
            )
        ) { param: TestParameters ->

            When("Traversing the entire graph forward") {
                param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

                Then("the traversal should visit all vertices in the expected order") {
                    AssertionUtils.assertExpectedPathGoingNextUntilEnd(
                        param.traverser,
                        param.expectedForwardPath
                    )
                }
            }

            When("Traversing the entire graph backward once the end is reached after traversing forwards") {
                param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

                Then("the traversal should visit all vertices in reverse order") {
                    AssertionUtils.assertExpectedPathGoingNextUntilEnd(
                        param.traverser,
                        param.expectedForwardPath
                    )

                    AssertionUtils.assertExpectedPathGoingPreviousUntilStart(
                        param.traverser,
                        param.expectedForwardPath.reversed()
                    )
                }
            }

            When("Performing staggered navigation with Next-Next-Previous pattern after traversing all the way previous from the end") {
                val expectedPathSize = param.expectedForwardPath.size
                val expectedForwardPath = param.expectedForwardPath
                val lastIndex = expectedPathSize - 1

                Then("Baseline at each index matches the expected path") {
                    for (i in 0 until lastIndex) {
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var step = 0
                        while (step < i) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            step++
                        }
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..i),
                            param.traverser
                        )
                    }
                }

                Then("After first Next at each index matches the expected path") {
                    for (i in 0 until lastIndex) {
                        val next1Index = i + 1
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var step = 0
                        while (step < next1Index) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            step++
                        }
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..next1Index),
                            param.traverser
                        )
                    }
                }

                Then("After second Next at each index matches the expected path") {
                    for (i in 0 until lastIndex) {
                        val next1Index = i + 1
                        val next2Index = if (i < lastIndex - 1) next1Index + 1 else next1Index
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var step = 0
                        while (step < next2Index) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            step++
                        }
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..next2Index),
                            param.traverser
                        )
                    }
                }

                Then("After Previous at each index matches the expected path") {
                    for (i in 0 until lastIndex) {
                        val next1Index = i + 1
                        val next2Index = if (i < lastIndex - 1) next1Index + 1 else next1Index
                        val prevIndex = if (i < lastIndex - 1) next1Index else i
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var step = 0
                        while (step < next2Index) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            step++
                        }
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..prevIndex),
                            param.traverser
                        )
                    }
                }
            }

            When("Performing staggered backward navigation with Previous-Previous-Next pattern") {
                val expectedPathSize = param.expectedForwardPath.size
                val expectedForwardPath = param.expectedForwardPath

                Then("Baseline from end to each index matches the expected path") {
                    for (i in expectedPathSize.dec() downTo 1) {
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var count = 0
                        do {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            count++
                        } while (param.traverser.current.value.isWithinBounds && count < expectedPathSize)
                        while (param.traverser.tracePath().size - 1 > i) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        }
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..i),
                            param.traverser
                        )
                    }
                }

                Then("After first Previous at each index matches the expected path") {
                    for (i in expectedPathSize.dec() downTo 1) {
                        val prev1Index = if (i > 1) i - 1 else 0
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var count = 0
                        do {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            count++
                        } while (param.traverser.current.value.isWithinBounds && count < expectedPathSize)

                        while (param.traverser.tracePath().size - 1 > i) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        }

                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..prev1Index),
                            param.traverser
                        )
                    }
                }

                Then("After second Previous at applicable indices matches the expected path") {
                    for (i in expectedPathSize.dec() downTo 1) {
                        if (i > 1) {
                            val prev1Index = i - 1
                            val prev2Index = prev1Index - 1
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                            var count = 0
                            do {
                                param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                                count++
                            } while (param.traverser.current.value.isWithinBounds && count < expectedPathSize)

                            while (param.traverser.tracePath().size - 1 > i) {
                                param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                            }

                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

                            assertTracedPathWithCurrentState(
                                expectedForwardPath.slice(0..prev2Index),
                                param.traverser
                            )
                        }
                    }
                }

                Then("After Next at each index matches the expected path") {
                    for (i in expectedPathSize.dec() downTo 1) {
                        val prev1Index = if (i > 1) i - 1 else 0
                        val nextIndex = if (i > 1) prev1Index else i
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                        var count = 0
                        do {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                            count++
                        } while (param.traverser.current.value.isWithinBounds && count < expectedPathSize)
                        while (param.traverser.tracePath().size - 1 > i) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        }
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        if (i > 1) {
                            param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                        }
                        param.traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                        assertTracedPathWithCurrentState(
                            expectedForwardPath.slice(0..nextIndex),
                            param.traverser
                        )
                    }
                }
            }
        }
    }
}) {
    companion object {
        data class TestParameters(
            val title: String,
            val traverser: Traverser<out IVertex<String>, String, out ITransitionGuardState, Nothing>,
            val expectedForwardPath: List<String>
        )

        private fun createParam2(title: String, edgeTraversalType: EdgeTraversalType): TestParameters {
            val traverser = TestBuilderUtils.build8VertexGraphStateMachine(TestTransitionGuardState(), edgeTraversalType)

            return TestParameters(
                title,
                traverser,
                listOf(v1, v2, v4, v8, v3, v5, v7, v6).map(IVertex<String>::id)
            )
        }

        private fun createParam1(title: String, edgeTraversalType: EdgeTraversalType): TestParameters {
            val traverser = mdk.test.scenarios.GraphScenarios.elevenVertexDAGTraverser(
                TestTransitionGuardState(),
                edgeTraversalType
            )

            return TestParameters(
                title,
                traverser,
                listOf("1", "2A", "3A", "4A", "5", "7", "3B", "4B", "6", "2B", "3C")
            )
        }
    }
}
