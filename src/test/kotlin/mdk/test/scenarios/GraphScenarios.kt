package mdk.test.scenarios

import kotlinx.coroutines.CoroutineScope
import mdk.gsm.builder.buildGuardedTraverser
import mdk.gsm.builder.buildGuardedWalker
import mdk.gsm.builder.buildTraverserWithActions
import mdk.gsm.builder.buildWalkerWithActions
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.state.traverser.Traverser
import mdk.gsm.state.walker.Walker
import mdk.gsm.util.LongVertex
import mdk.gsm.util.StringVertex
import mdk.test.utils.*

/**
 * Graph Scenarios for testing.
 * 
 * Each function creates a FRESH instance of a graph state machine with a specific topology.
 * These are reusable graph builders that share logic, not state.
 */
object GraphScenarios {
    
    /**
     * Creates a simple 3-vertex linear graph: 1 -> 2 -> 3
     * Useful for testing basic traversal and sequential navigation.
     */
    fun linearThreeVertexTraverser(
        guardState: TestTransitionGuardState = TestTransitionGuardState()
    ): Traverser<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedTraverser(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            
            buildGraph(v1) {
                addVertex(v1) { addEdge { setTo(v2) } }
                addVertex(v2) { addEdge { setTo(v3) } }
                addVertex(v3)
            }
        }
    }
    
    /**
     * Creates a simple 3-vertex linear graph as a Walker: 1 -> 2 -> 3
     */
    fun linearThreeVertexWalker(
        guardState: TestTransitionGuardState = TestTransitionGuardState()
    ): Walker<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedWalker(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            
            buildGraph(v1) {
                addVertex(v1) { addEdge { setTo(v2) } }
                addVertex(v2) { addEdge { setTo(v3) } }
                addVertex(v3)
            }
        }
    }
    
    /**
     * Creates a 4-vertex linear graph: 1 -> 2 -> 3 -> 4
     */
    fun linearFourVertexTraverser(
        guardState: TestTransitionGuardState = TestTransitionGuardState()
    ): Traverser<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedTraverser(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            
            buildGraph(v1) {
                addVertex(v1) { addEdge { setTo(v2) } }
                addVertex(v2) { addEdge { setTo(v3) } }
                addVertex(v3) { addEdge { setTo(v4) } }
                addVertex(v4)
            }
        }
    }
    
    /**
     * Creates an 8-vertex graph with conditional paths controlled by guard state flags.
     * Topology:
     *     1 -> 2 -> 4 -> 8
     *     └─> 3 -> 5 -> 7 -> 8
     *          └─> 6 -> 7
     * 
     * Guards can block transitions to vertices 2, 3, 5, and 7.
     * Vertex 5 is a SubTestVertex with additional test fields.
     * Optional 7->3 cycle can be enabled.
     */
    fun conditionalEightVertexTraverser(
        guardState: TestTransitionGuardState,
        edgeTraversalType: EdgeTraversalType = EdgeTraversalType.DFSAcyclic,
        add7to3Cycle: Boolean = false
    ): Traverser<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedTraverser(guardState) {
            setTraversalType(edgeTraversalType)
            
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = SubTestVertex("5")
            val v6 = TestVertex("6")
            val v7 = TestVertex("7")
            val v8 = TestVertex("8")

            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo2 }
                    }
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo3 }
                    }
                }

                addVertex(v2) {
                    addEdge { setTo(v4) }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v5)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo5 }
                    }
                    addEdge { setTo(v6) }
                }

                addVertex(v4) {
                    addEdge { setTo(v8) }
                }

                addVertex(v5) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            !guardState.blockedGoingTo7 && 
                            (from as SubTestVertex).testField &&
                            v5.testField
                        }
                    }
                }

                addVertex(v6) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo7 }
                    }
                }

                addVertex(v7) {
                    addEdge { setTo(v8) }
                    if (add7to3Cycle) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard { !guardState.blockedGoingTo3 }
                        }
                    }
                }

                addVertex(v8)
            }
        }
    }
    
    /**
     * Creates an 8-vertex walker with conditional paths (Walker version of conditionalEightVertexTraverser).
     */
    fun conditionalEightVertexWalker(
        guardState: TestTransitionGuardState,
        add7to3Cycle: Boolean = false
    ): Walker<TestVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedWalker(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = SubTestVertex("5")
            val v6 = TestVertex("6")
            val v7 = TestVertex("7")
            val v8 = TestVertex("8")

            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo2 }
                    }
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo3 }
                    }
                }

                addVertex(v2) {
                    addEdge { setTo(v4) }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v5)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo5 }
                    }
                    addEdge { setTo(v6) }
                }

                addVertex(v4) {
                    addEdge { setTo(v8) }
                }

                addVertex(v5) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard {
                            !guardState.blockedGoingTo7 && 
                            (from as SubTestVertex).testField &&
                            v5.testField
                        }
                    }
                }

                addVertex(v6) {
                    addEdge {
                        setTo(v7)
                        setEdgeTransitionGuard { !guardState.blockedGoingTo7 }
                    }
                }

                addVertex(v7) {
                    addEdge { setTo(v8) }
                    if (add7to3Cycle) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard { !guardState.blockedGoingTo3 }
                        }
                    }
                }

                addVertex(v8)
            }
        }
    }
    
    /**
     * Creates a complex 15-vertex graph with multiple cycles and conditional paths.
     * Used for testing complex traversal scenarios and DFS/BFS behavior.
     */
    fun complex15VertexTraverser(
        guardState: Test15VertexTransitionArgs,
        edgeTraversalType: EdgeTraversalType = EdgeTraversalType.DFSAcyclic
    ): Traverser<TestVertex, String, Test15VertexTransitionArgs, Nothing> {
        return buildGuardedTraverser(guardState) {
            setTransitionGuardState(guardState)
            setTraversalType(edgeTraversalType)

            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = TestVertex("5")
            val v6 = TestVertex("6")
            val v7 = TestVertex("7")
            val v8 = TestVertex("8")
            val v9 = TestVertex("9")
            val v10 = TestVertex("10")
            val v11 = TestVertex("11")
            val v12 = TestVertex("12")
            val v13 = TestVertex("13")
            val v14 = TestVertex("14")
            val v15 = TestVertex("15")

            buildGraph(v1) {
                addVertex(v1) {
                    addEdge { setTo(v2) }
                    addEdge { setTo(v3) }
                }

                addVertex(v2) {
                    addEdge { setTo(v4) }
                }

                addVertex(v3) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { !guardState.blockedFrom3To2 }
                    }
                    addEdge { setTo(v7) }
                    addEdge { setTo(v5) }
                }

                addVertex(v4) {
                    addEdge { setTo(v6) }
                }

                addVertex(v5) {
                    addEdge { setTo(v8) }
                }

                addVertex(v6) {
                    addEdge { setTo(v3) }
                    addEdge { setTo(v8) }
                }

                addVertex(v7) {
                    addEdge { setTo(v8) }
                }

                addVertex(v8) {
                    addEdge {
                        setTo(v9)
                        setEdgeTransitionGuard { !guardState.blockedFrom8To9 }
                    }
                    addEdge { setTo(v10) }
                }

                addVertex(v9) {
                    addEdge { setTo(v11) }
                }

                addVertex(v10) {
                    addEdge { setTo(v11) }
                }

                addVertex(v11) {
                    addEdge { setTo(v12) }
                    addEdge { setTo(v13) }
                }

                addVertex(v12) {
                    addEdge { setTo(v14) }
                }

                addVertex(v13) {
                    addEdge { setTo(v14) }
                }

                addVertex(v14) {
                    addEdge { setTo(v5) }
                    addEdge { setTo(v15) }
                }

                addVertex(v15) {
                    addEdge { setTo(v2) }
                }
            }
        }
    }
    
    /**
     * Creates a complex 15-vertex walker with multiple cycles and conditional paths.
     * Walker version of complex15VertexTraverser.
     */
    fun complex15VertexWalker(
        guardState: Test15VertexTransitionArgs
    ): Walker<TestVertex, String, Test15VertexTransitionArgs, Nothing> {
        return buildGuardedWalker(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = TestVertex("5")
            val v6 = TestVertex("6")
            val v7 = TestVertex("7")
            val v8 = TestVertex("8")
            val v9 = TestVertex("9")
            val v10 = TestVertex("10")
            val v11 = TestVertex("11")
            val v12 = TestVertex("12")
            val v13 = TestVertex("13")
            val v14 = TestVertex("14")
            val v15 = TestVertex("15")

            buildGraph(v1) {
                addVertex(v1) {
                    addEdge { setTo(v2) }
                    addEdge { setTo(v3) }
                }
                addVertex(v2) { addEdge { setTo(v4) } }
                addVertex(v3) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { !guardState.blockedFrom3To2 }
                    }
                    addEdge { setTo(v7) }
                    addEdge { setTo(v5) }
                }
                addVertex(v4) { addEdge { setTo(v6) } }
                addVertex(v5) { addEdge { setTo(v8) } }
                addVertex(v6) {
                    addEdge { setTo(v3) }
                    addEdge { setTo(v8) }
                }
                addVertex(v7) { addEdge { setTo(v8) } }
                addVertex(v8) {
                    addEdge {
                        setTo(v9)
                        setEdgeTransitionGuard { !guardState.blockedFrom8To9 }
                    }
                    addEdge { setTo(v10) }
                }
                addVertex(v9) { addEdge { setTo(v11) } }
                addVertex(v10) { addEdge { setTo(v11) } }
                addVertex(v11) {
                    addEdge { setTo(v12) }
                    addEdge { setTo(v13) }
                }
                addVertex(v12) { addEdge { setTo(v14) } }
                addVertex(v13) { addEdge { setTo(v14) } }
                addVertex(v14) {
                    addEdge { setTo(v5) }
                    addEdge { setTo(v15) }
                }
                addVertex(v15) { addEdge { setTo(v2) } }
            }
        }
    }
    
    /**
     * Creates a graph with intermediate states that auto-advance.
     * Tests onBeforeVisit handlers and automatic state transitions.
     * 
     * Topology: start -> intermediate1 (auto) -> regular1 -> intermediate2 (auto) -> regular2 -> end (auto)
     */
    fun <G> intermediateAutoAdvanceTraverser(
        guardState: G,
        scope: CoroutineScope = StateMachineScopeFactory.newScope(),
        beforeVisitCalls: MutableList<String>
    ): Traverser<StringVertex, String, G, Nothing> {
        return buildGuardedTraverser(guardState, scope) {
            val start = StringVertex("start")
            val intermediate1 = StringVertex("intermediate1")
            val regular1 = StringVertex("regular1")
            val intermediate2 = StringVertex("intermediate2")
            val regular2 = StringVertex("regular2")
            val end = StringVertex("end")

            buildGraph(start) {
                addVertex(start) {
                    addEdge { setTo(intermediate1) }
                }

                addVertex(intermediate1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(regular1) }
                }

                addVertex(regular1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                    addEdge { setTo(intermediate2) }
                }

                addVertex(intermediate2) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(regular2) }
                }

                addVertex(regular2) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                    addEdge { setTo(end) }
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
    
    /**
     * Creates a graph with a conditional intermediate state.
     * The conditional vertex auto-advances only if the provided condition is true.
     */
    fun <G> conditionalAutoAdvanceTraverser(
        guardState: G,
        beforeVisitCalls: MutableList<String>,
        shouldAutoAdvanceProvider: () -> Boolean
    ): Traverser<StringVertex, String, G, Nothing> {
        return buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSAcyclic)

            val start = StringVertex("start")
            val conditional = StringVertex("conditional")
            val end = StringVertex("end")

            buildGraph(start) {
                addVertex(start) {
                    addEdge { setTo(conditional) }
                }

                addVertex(conditional) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        if (shouldAutoAdvanceProvider()) {
                            autoAdvance()
                        }
                    }
                    addEdge { setTo(end) }
                }

                addVertex(end) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                }
            }
        }
    }
    
    /**
     * Creates a graph with a chain of intermediate states that all auto-advance.
     * Topology: start -> intermediate1 (auto) -> intermediate2 (auto) -> intermediate3 (auto) -> end
     */
    fun <G> chainedIntermediateTraverser(
        guardState: G,
        beforeVisitCalls: MutableList<String>
    ): Traverser<StringVertex, String, G, Nothing> {
        return buildGuardedTraverser(guardState) {
            setTraversalType(EdgeTraversalType.DFSAcyclic)

            val start = StringVertex("start")
            val intermediate1 = StringVertex("intermediate1")
            val intermediate2 = StringVertex("intermediate2")
            val intermediate3 = StringVertex("intermediate3")
            val end = StringVertex("end")

            buildGraph(start) {
                addVertex(start) {
                    addEdge { setTo(intermediate1) }
                }

                addVertex(intermediate1) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(intermediate2) }
                }

                addVertex(intermediate2) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(intermediate3) }
                }

                addVertex(intermediate3) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(end) }
                }

                addVertex(end) {
                    onBeforeVisit {
                        beforeVisitCalls.add(vertex.id)
                    }
                }
            }
        }
    }
    
    /**
     * Creates a traverser with action arguments for testing NextArgs behavior.
     * Conditional paths based on action arguments.
     */
    fun <A> conditionalWithArgsTraverser(
        guardState: TestTransitionGuardState,
        argMatcher: (A?) -> Boolean,
        vararg vertices: TestVertex
    ): Traverser<TestVertex, String, TestTransitionGuardState, A> {
        require(vertices.size >= 2) { "Need at least 2 vertices" }
        
        return buildTraverserWithActions(guardState) {
            buildGraph(vertices[0]) {
                vertices.forEachIndexed { index, vertex ->
                    if (index < vertices.size - 1) {
                        addVertex(vertex) {
                            addEdge {
                                setTo(vertices[index + 1])
                                setEdgeTransitionGuard { argMatcher(args) }
                            }
                        }
                    } else {
                        addVertex(vertex)
                    }
                }
            }
        }
    }
    
    /**
     * Creates a walker with action arguments for testing NextArgs behavior.
     */
    fun <A> conditionalWithArgsWalker(
        guardState: TestTransitionGuardState,
        argMatcher: (A?) -> Boolean,
        vararg vertices: TestVertex
    ): Walker<TestVertex, String, TestTransitionGuardState, A> {
        require(vertices.size >= 2) { "Need at least 2 vertices" }
        
        return buildWalkerWithActions(guardState) {
            buildGraph(vertices[0]) {
                vertices.forEachIndexed { index, vertex ->
                    if (index < vertices.size - 1) {
                        addVertex(vertex) {
                            addEdge {
                                setTo(vertices[index + 1])
                                setEdgeTransitionGuard { argMatcher(args) }
                            }
                        }
                    } else {
                        addVertex(vertex)
                    }
                }
            }
        }
    }
    
    /**
     * Creates a specialized traverser for testing Next actions with arguments.
     * This graph has conditional paths based on action arguments and an intermediate state with auto-advance.
     * 
     * Topology: v1 -> v2 -> [v3 OR v4] -> [v6 OR v5]
     * - From v2, edge to v3 requires args.id == 3
     * - From v2, edge to v4 requires args.id == 4
     * - v4 auto-advances if args.id == 4
     * - v3 -> v6
     * - v4 -> v5
     */
    fun <A> nextActionWithArgsTraverser(
        guardState: TestTransitionGuardState,
        argsGoTo3: A,
        argsGoTo4: A
    ): Traverser<TestVertex, String, TestTransitionGuardState, A> {
        return buildTraverserWithActions(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = TestVertex("5")
            val v6 = TestVertex("6")

            setExplicitTransitionIntoBounds(true)

            buildGraph(startAtVertex = v1) {
                addVertex(v1) {
                    addEdge { setTo(v2) }
                }
                addVertex(v2) {
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard {
                            args == argsGoTo3
                        }
                    }
                    addEdge {
                        setTo(v4)
                        setEdgeTransitionGuard {
                            args == argsGoTo4
                        }
                    }
                }
                addVertex(v4) {
                    onBeforeVisit {
                        if (args == argsGoTo4) {
                            autoAdvance()
                        }
                    }
                    addEdge { setTo(v5) }
                }
                addVertex(v3) {
                    addEdge { setTo(v6) }
                }
                addVertex(v5)
                addVertex(v6)
            }
        }
    }
    
    /**
     * Creates a specialized walker for testing Next actions with arguments.
     * Walker version of nextActionWithArgsTraverser.
     */
    fun <A> nextActionWithArgsWalker(
        guardState: TestTransitionGuardState,
        argsGoTo3: A,
        argsGoTo4: A
    ): Walker<TestVertex, String, TestTransitionGuardState, A> {
        return buildWalkerWithActions(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            val v5 = TestVertex("5")
            val v6 = TestVertex("6")

            setExplicitTransitionIntoBounds(true)

            buildGraph(startAtVertex = v1) {
                addVertex(v1) {
                    addEdge { setTo(v2) }
                }
                addVertex(v2) {
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard {
                            args == argsGoTo3
                        }
                    }
                    addEdge {
                        setTo(v4)
                        setEdgeTransitionGuard {
                            args == argsGoTo4
                        }
                    }
                }
                addVertex(v4) {
                    onBeforeVisit {
                        if (args == argsGoTo4) {
                            autoAdvance()
                        }
                    }
                    addEdge { setTo(v5) }
                }
                addVertex(v3) {
                    addEdge { setTo(v6) }
                }
                addVertex(v5)
                addVertex(v6)
            }
        }
    }

    /**
     * Creates a walker for testing intermediate states with auto-advance functionality.
     * Uses callbacks to track beforeVisit calls.
     */
    fun intermediateStateWalker(
        guardState: TestTransitionGuardState,
        scope: CoroutineScope,
        beforeVisitCallback: (String) -> Unit
    ): Walker<StringVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedWalker(guardState, scope) {
            val start = StringVertex("start")
            val intermediate1 = StringVertex("intermediate1")
            val regular1 = StringVertex("regular1")
            val intermediate2 = StringVertex("intermediate2")
            val regular2 = StringVertex("regular2")
            val end = StringVertex("end")

            buildGraph(start) {
                addVertex(start) {
                    addEdge { setTo(intermediate1) }
                }
                addVertex(intermediate1) {
                    onBeforeVisit {
                        beforeVisitCallback(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(regular1) }
                }
                addVertex(regular1) {
                    onBeforeVisit {
                        beforeVisitCallback(vertex.id)
                    }
                    addEdge { setTo(intermediate2) }
                }
                addVertex(intermediate2) {
                    onBeforeVisit {
                        beforeVisitCallback(vertex.id)
                        autoAdvance()
                    }
                    addEdge { setTo(regular2) }
                }
                addVertex(regular2) {
                    onBeforeVisit {
                        beforeVisitCallback(vertex.id)
                    }
                    addEdge { setTo(end) }
                }
                addVertex(end) {
                    onBeforeVisit {
                        beforeVisitCallback(vertex.id)
                        autoAdvance()
                    }
                }
            }
        }
    }

    /**
     * Creates a 2-vertex traverser for testing transition bounds with explicit step into bounds.
     */
    fun twoVertexBoundsTraverser(
        guardState: ITransitionGuardState? = null
    ): Traverser<LongVertex, Long, ITransitionGuardState?, Nothing> {
        return buildGuardedTraverser(guardState) {
            val v1 = LongVertex(1L)
            val v2 = LongVertex(2L)

            setExplicitTransitionIntoBounds(true)

            buildGraph(v1) {
                v(v1) { e { setTo(v2) } }
                v(v2)
            }
        }
    }

    /**
     * Creates a 2-vertex walker for testing traversal bounds with explicit step into bounds.
     */
    fun twoVertexBoundsWalker(
        guardState: ITransitionGuardState? = null
    ): Walker<LongVertex, Long, ITransitionGuardState?, Nothing> {
        return buildGuardedWalker(guardState) {
            val v1 = LongVertex(1L)
            val v2 = LongVertex(2L)

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
    }

    /**
     * Creates an 11-vertex DAG traverser for bidirectional traversal testing.
     * Graph structure:
     * 1 -> 2A, 2B
     * 2A -> 3A, 3B
     * 2B -> 3C
     * 3A -> 4A
     * 3B, 3C -> 4B
     * 4A -> 5 -> 7
     * 4B -> 6 -> 7
     */
    fun elevenVertexDAGTraverser(
        guardState: TestTransitionGuardState,
        edgeTraversalType: EdgeTraversalType
    ): Traverser<StringVertex, String, TestTransitionGuardState, Nothing> {
        return buildGuardedTraverser(guardState) {
            val step1 = StringVertex("1")
            setTraversalType(edgeTraversalType)

            buildGraph(step1) {
                val step2a = StringVertex("2A")
                val step2b = StringVertex("2B")
                val step3a = StringVertex("3A")
                val step3b = StringVertex("3B")
                val step3c = StringVertex("3C")
                val step4a = StringVertex("4A")
                val step4b = StringVertex("4B")
                val step5 = StringVertex("5")
                val step6 = StringVertex("6")
                val step7 = StringVertex("7")

                addVertex(step1) {
                    addEdge { setTo(step2a) }
                    addEdge { setTo(step2b) }
                }

                addVertex(step2a) {
                    addEdge { setTo(step3a) }
                    addEdge { setTo(step3b) }
                }

                addVertex(step2b) {
                    addEdge { setTo(step3c) }
                }

                addVertex(step3a) {
                    addEdge { setTo(step4a) }
                }

                addVertex(step3b) {
                    addEdge { setTo(step4b) }
                }

                addVertex(step3c) {
                    addEdge { setTo(step4b) }
                }

                addVertex(step4a) {
                    addEdge { setTo(step5) }
                }

                addVertex(step4b) {
                    addEdge { setTo(step6) }
                }

                addVertex(step5) {
                    addEdge { setTo(step7) }
                }

                addVertex(step6) {
                    addEdge { setTo(step7) }
                }

                addVertex(step7) {}
            }
        }
    }

    /**
     * Creates a walker for testing outgoing transition handlers with noTransition().
     * Uses callbacks to track transition attempts.
     */
    fun outgoingTransitionHandlerWalker(
        guardState: OutgoingTransitionTestGuardState,
        scope: CoroutineScope,
        transitionAttemptCallback: (String) -> Unit
    ): Walker<StringVertex, String, OutgoingTransitionTestGuardState, Nothing> {
        return buildGuardedWalker(guardState, scope) {
            val start = StringVertex("start")
            val middle = StringVertex("middle")
            val end = StringVertex("end")

            buildGraph(start) {
                addVertex(start) {
                    onOutgoingTransition {
                        transitionAttemptCallback(vertex.id)
                        if (guardState.shouldPreventTransition) {
                            noTransition()
                        }
                    }
                    addEdge { setTo(middle) }
                }
                addVertex(middle) {
                    addEdge { setTo(end) }
                }
                addVertex(end)
            }
        }
    }
}
