package mdk.test.utils

import kotlinx.coroutines.CoroutineScope
import mdk.gsm.builder.buildGuardedTraverser
import mdk.gsm.builder.buildGuardedWalker
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.traverser.Traverser
import mdk.gsm.state.walker.Walker
import mdk.gsm.util.StringVertex

object TestBuilderUtils {

    object IntermediateTestingIds {
        const val START = "start"
        const val INTERMEDIATE_1 = "intermediate1"
        const val INTERMEDIATE_2 = "intermediate2"
        const val INTERMEDIATE_3 = "intermediate3"
        const val REGULAR_1 = "regular1"
        const val REGULAR_2 = "regular2"
        const val CONDITIONAL = "conditional"
        const val END = "end"
    }

    val v1 = TestVertex("1")
    val v2 = TestVertex("2")
    val v3 = TestVertex("3")
    val v4 = TestVertex("4")
    val v5 = SubTestVertex("5")
    val v6  = TestVertex("6")
    val v7  = TestVertex("7")
    val v8  = TestVertex("8")
    val v9  = TestVertex("9")
    val v10 = TestVertex("10")
    val v11 = TestVertex("11")
    val v12 = TestVertex("12")
    val v13 = TestVertex("13")
    val v14 = TestVertex("14")
    val v15 = TestVertex("15")

    fun build8VertexGraphStateMachine(
        testProgressionFlags: TestTransitionGuardState,
        edgeTraversalType: EdgeTraversalType,
        add7to3cycle : Boolean = false,
    ): Traverser<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedTraverser(testProgressionFlags) {
            setTraversalType(edgeTraversalType)

            buildGraph(v1) {

                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo2 != true
                        }
                    }

                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo3 != true
                        }
                    }
                }

                addVertex(v2) {
                    addEdge {
                        setTo(v4)
                    }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v5)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo5 != true
                        }
                    }

                    addEdge {
                        setTo(v6)
                    }
                }

                addVertex(v4) {
                    addEdge {
                        setTo(v8)
                    }
                }

                addVertex(v5) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo7 != true && (from as SubTestVertex).testField // cast for subclass access
                                    && v5.testField // alternatively capturing lambda, these are the two options
                        }
                    }
                }

                addVertex(v6) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo7 != true
                        }
                    }
                }

                addVertex(v7) {
                    addEdge {
                        setTo(v8)
                    }

                    if (add7to3cycle) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard {
                                guardState?.blockedGoingTo3 != true
                            }
                        }
                    }
                }

                addVertex(v8) {}
            }
        }
    }

    fun build8VertexWalker(
        testProgressionFlags: TestTransitionGuardState,
        add7to3cycle : Boolean = false,
    ): Walker<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedWalker(testProgressionFlags) {
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo2 != true
                        }
                    }

                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo3 != true
                        }
                    }
                }

                addVertex(v2) {
                    addEdge {
                        setTo(v4)
                    }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v5)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo5 != true
                        }
                    }

                    addEdge {
                        setTo(v6)
                    }
                }

                addVertex(v4) {
                    addEdge {
                        setTo(v8)
                    }
                }

                addVertex(v5) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo7 != true && (from as SubTestVertex).testField // cast for subclass access
                                    && v5.testField // alternatively capturing lambda, these are the two options
                        }
                    }
                }

                addVertex(v6) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            guardState?.blockedGoingTo7 != true
                        }
                    }
                }

                addVertex(v7) {
                    addEdge {
                        setTo(v8)
                    }

                    if (add7to3cycle) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard {
                                guardState?.blockedGoingTo3 != true
                            }
                        }
                    }
                }

                addVertex(v8) {}
            }
        }
    }

    fun build15VertexGraphStateMachine(
        transitionGuardState: Test15VertexTransitionArgs,
        edgeTraversalType: EdgeTraversalType
    ): Traverser<TestVertex, String, Test15VertexTransitionArgs, Nothing> {
        return buildGuardedTraverser(transitionGuardState) {
            setTransitionGuardState(transitionGuardState)
            setTraversalType(edgeTraversalType)

            buildGraph(v1) {

                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                    addEdge {
                        setTo(v3)
                    }
                }

                addVertex(v2) {
                    addEdge {
                        setTo(v4)
                    }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard {
                            guardState?.blockedFrom3To2 != true
                        }
                    }

                    addEdge {
                        setTo(v7)
                    }

                    addEdge {
                        setTo(v5)
                    }
                }

                addVertex(v4) {
                    addEdge {
                        setTo(v6)
                    }
                }

                addVertex(v5) {
                    addEdge {
                        setTo(v8)
                    }
                }

                addVertex(v6) {
                    addEdge {
                        setTo(v3)
                    }
                    addEdge {
                        setTo(v8)
                    }
                }

                addVertex(v7) {
                    addEdge {
                        setTo(v8)
                    }
                }

                addVertex(v8) {
                    addEdge {
                        setTo(v9)
                        setEdgeTransitionGuard {
                            guardState?.blockedFrom8To9 != true
                        }
                    }
                    addEdge {
                        setTo(v10)
                    }
                }

                addVertex(v9) {
                    addEdge {
                        setTo(v11)
                    }
                }

                addVertex(v10) {
                    addEdge {
                        setTo(v11)
                    }
                }

                addVertex(v11) {
                    addEdge {
                        setTo(v12)
                    }
                    addEdge {
                        setTo(v13)
                    }
                }

                addVertex(v12) {
                    addEdge {
                        setTo(v14)
                    }
                }

                addVertex(v13) {
                    addEdge {
                        setTo(v14)
                    }
                }

                addVertex(v14) {
                    addEdge {
                        setTo(v5)
                    }
                    addEdge {
                        setTo(v15)
                    }
                }

                addVertex(v15) {
                    addEdge {
                        setTo(v2)
                    }
                }
            }
        }
    }

    fun <G> buildIntermediateTestGraph(
        guardState: G,
        scope : CoroutineScope = StateMachineScopeFactory.newScope(),
        beforeVisitCalls: MutableList<String>
    ): Traverser<StringVertex, String, G, Nothing> {
        val ids = IntermediateTestingIds

        return buildGuardedTraverser(guardState, scope) {

            val start = StringVertex(ids.START)
            val intermediate1 = StringVertex(ids.INTERMEDIATE_1)
            val regular1 = StringVertex(ids.REGULAR_1)
            val intermediate2 = StringVertex(ids.INTERMEDIATE_2)
            val regular2 = StringVertex(ids.REGULAR_2)
            val end = StringVertex(ids.END)

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
                        autoAdvance()
                    }
                }
            }
        }
    }


    fun <G> buildConditionalTestGraph(
        guardState: G,
        beforeVisitCalls: MutableList<String>,
        shouldAutoAdvanceProvider: () -> Boolean
    ): Traverser<StringVertex, String, G, Nothing> {
        val ids = IntermediateTestingIds

        return buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSAcyclic)

            val start = StringVertex(ids.START)
            val conditional = StringVertex(ids.CONDITIONAL)
            val end = StringVertex(ids.END)

            buildGraph(start) {
                addVertex(start) {
                    addEdge {
                        setTo(conditional)
                    }
                }

                // This state conditionally auto-advances
                addVertex(conditional) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        if (shouldAutoAdvanceProvider()) {
                            autoAdvance() // Skip this state based on condition
                        }
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
    }

    fun <G> buildChainedIntermediateTestGraph(
        guardState: G,
        beforeVisitCalls: MutableList<String>
    ): Traverser<StringVertex, String, G, Nothing> {
        val ids = IntermediateTestingIds

        return buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSAcyclic)

            val start = StringVertex(ids.START)
            val intermediate1 = StringVertex(ids.INTERMEDIATE_1)
            val intermediate2 = StringVertex(ids.INTERMEDIATE_2)
            val intermediate3 = StringVertex(ids.INTERMEDIATE_3)
            val end = StringVertex(ids.END)

            buildGraph(start) {
                addVertex(start) {
                    addEdge {
                        setTo(intermediate1)
                    }
                }

                // Chain of intermediate states
                addVertex(intermediate1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
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
                        setTo(intermediate3)
                    }
                }

                addVertex(intermediate3) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
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
    }
}
