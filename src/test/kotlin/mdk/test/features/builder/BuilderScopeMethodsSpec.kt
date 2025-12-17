@file:Suppress("RunBlockingInSuspendFunction")

package mdk.test.features.builder

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mdk.gsm.builder.*
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.test.utils.TestVertex

class BuilderScopeMethodsSpec : BehaviorSpec({
    
    Given("A graph with edges configured using explicit ordering") {
        
        When("Edges are added with explicit order values via setOrder") {
            val traverser = buildTraverser {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                val v4 = TestVertex("4")
                
                buildGraph(v1) {
                    addVertex(v1) {
                        addEdge(autoOrder = false) {
                            setOrder(10)
                            setTo(v3)
                        }
                        addEdge(autoOrder = false) {
                            setOrder(5)
                            setTo(v2)
                        }
                        addEdge(autoOrder = false) {
                            setOrder(15)
                            setTo(v4)
                        }
                    }
                    addVertex(v2)
                    addVertex(v3)
                    addVertex(v4)
                }
            }
            
            Then("Traversal follows explicit order rather than declaration order") {
                runBlocking {
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    traverser.current.value.vertex.id shouldBe "2"
                }
            }
        }
        
        When("Edges are configured using setTo with vertex ID strings") {
            val walker = buildWalker {
                val start = TestVertex("start")
                val end = TestVertex("end")
                
                buildGraph(start) {
                    addVertex(start) {
                        addEdge {
                            setTo("end")
                        }
                    }
                    addVertex(end)
                }
            }
            
            Then("The edge targets the correct vertex by ID") {
                runBlocking {
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "end"
                }
            }
        }
        
        When("Edges are configured using setTo with vertex instances") {
            val walker = buildWalker {
                val start = TestVertex("start")
                val middle = TestVertex("middle")
                val end = TestVertex("end")
                
                buildGraph(start) {
                    addVertex(start) {
                        addEdge {
                            setTo(middle)
                        }
                    }
                    addVertex(middle) {
                        addEdge {
                            setTo(end)
                        }
                    }
                    addVertex(end)
                }
            }
            
            Then("Traversal follows the edges configured with vertex instances") {
                runBlocking {
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "middle"
                    
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "end"
                }
            }
        }
    }
    
    Given("A graph built using DSL shorthand methods") {
        
        When("Vertices and edges are defined using v() and e() shorthand") {
            val traverser = buildTraverser {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                
                buildGraph(v1) {
                    v(v1) { 
                        e { setTo(v2) }
                    }
                    v(v2) { 
                        e { setTo(v3) }
                    }
                    v(v3)
                }
            }
            
            Then("The graph structure is equivalent to verbose syntax") {
                runBlocking {
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    traverser.current.value.vertex.id shouldBe "2"
                    
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    traverser.current.value.vertex.id shouldBe "3"
                }
            }
        }
        
        When("A vertex is defined with multiple edges using shorthand") {
            val walker = buildWalker {
                val start = TestVertex("start")
                val pathA = TestVertex("pathA")
                val pathB = TestVertex("pathB")
                
                buildGraph(start) {
                    v(start) {
                        e { setTo(pathA) }
                        e { setTo(pathB) }
                    }
                    v(pathA)
                    v(pathB)
                }
            }
            
            Then("The first declared edge is selected during traversal") {
                runBlocking {
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "pathA"
                }
            }
        }
    }
    
    Given("A graph with conditional edges using transition guards") {
        
        When("Multiple edges have mutually exclusive transition guards") {
            var allowPath1 = true
            val traverser = buildTraverser {
                val start = TestVertex("start")
                val path1 = TestVertex("path1")
                val path2 = TestVertex("path2")
                
                buildGraph(start) {
                    addVertex(start) {
                        addEdge {
                            setTo(path1)
                            setEdgeTransitionGuard {
                                allowPath1
                            }
                        }
                        addEdge {
                            setTo(path2)
                            setEdgeTransitionGuard {
                                !allowPath1
                            }
                        }
                    }
                    addVertex(path1)
                    addVertex(path2)
                }
            }
            
            Then("Traversal selects edges based on guard evaluation") {
                runBlocking {
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    traverser.current.value.vertex.id shouldBe "path1"
                    
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
                    allowPath1 = false
                    
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    traverser.current.value.vertex.id shouldBe "path2"
                }
            }
        }
    }
    
    Given("A walker with bounds transition configuration") {
        
        When("The walker is configured with explicit bounds transitions enabled") {
            val walker = buildWalker {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                
                setExplicitTransitionIntoBounds(true)
                
                buildGraph(v1) {
                    v(v1) { e { setTo(v2) } }
                    v(v2)
                }
            }
            
            Then("The walker transitions beyond last when reaching terminal vertices") {
                runBlocking {
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "2"
                    
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.isBeyondLast shouldBe true
                    walker.current.value.isWithinBounds shouldBe false
                }
            }
        }
        
        When("The walker is configured with explicit bounds transitions disabled") {
            val walker = buildWalker {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                
                setExplicitTransitionIntoBounds(false)
                
                buildGraph(v1) {
                    v(v1) { e { setTo(v2) } }
                    v(v2)
                }
            }
            
            Then("The walker remains within bounds at terminal vertices") {
                walker.current.value.vertex.id shouldBe "1"
                walker.current.value.isWithinBounds shouldBe true
            }
        }
    }
    
    Given("A graph with onBeforeVisit vertex lifecycle handlers") {
        
        When("Vertices are configured with onBeforeVisit callbacks") {
            val visitedVertices = mutableListOf<String>()
            val walker = buildWalker {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                
                buildGraph(v1) {
                    addVertex(v1) {
                        onBeforeVisit {
                            visitedVertices.add("v1")
                        }
                        addEdge { setTo(v2) }
                    }
                    addVertex(v2) {
                        onBeforeVisit {
                            visitedVertices.add("v2")
                        }
                        addEdge { setTo(v3) }
                    }
                    addVertex(v3) {
                        onBeforeVisit {
                            visitedVertices.add("v3")
                        }
                    }
                }
            }
            
            Then("The callbacks are invoked when entering each vertex") {
                runBlocking {
                    visitedVertices.size shouldBe 0
                    
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    visitedVertices shouldBe listOf("v2")
                    
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    visitedVertices shouldBe listOf("v2", "v3")
                }
            }
        }
    }
    
    Given("A graph with onOutgoingTransition vertex lifecycle handlers") {
        
        When("Vertices are configured with onOutgoingTransition callbacks") {
            val transitionLog = mutableListOf<String>()
            val traverser = buildTraverser {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                
                buildGraph(v1) {
                    addVertex(v1) {
                        onOutgoingTransition {
                            transitionLog.add("leaving-v1")
                        }
                        addEdge { setTo(v2) }
                    }
                    addVertex(v2) {
                        onOutgoingTransition {
                            transitionLog.add("leaving-v2")
                        }
                        addEdge { setTo(v3) }
                    }
                    addVertex(v3)
                }
            }
            
            Then("The callbacks are invoked when leaving each vertex") {
                runBlocking {
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    transitionLog shouldBe listOf("leaving-v1")
                    
                    traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    transitionLog shouldBe listOf("leaving-v1", "leaving-v2")
                }
            }
        }
    }

    Given("A walker built using setWorkflowGraph with a pre-built graph") {

        When("A graph is built separately and assigned via setWorkflowGraph") {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")

            val preBuiltGraph = buildGraphOnly<TestVertex, String, Nothing, Nothing> {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                }
                addVertex(v2) {
                    addEdge {
                        setTo(v3)
                    }
                }
                addVertex(v3)
            }

            val walker = buildWalker {
                setWorkflowGraph(v1, preBuiltGraph)
            }

            Then("The walker uses the pre-built graph for traversal") {
                runBlocking {
                    walker.current.value.vertex.id shouldBe "1"
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "2"
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "3"
                }
            }
        }
    }

    Given("A walker built using the g() shorthand method") {

        When("The graph is defined using g() instead of buildGraph()") {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")

            val walker = buildWalker {
                g(v1) {
                    addVertex(v1) {
                        addEdge {
                            setTo(v2)
                        }
                    }
                    addVertex(v2)
                }
            }

            Then("The walker is built correctly with the shorthand syntax") {
                runBlocking {
                    walker.current.value.vertex.id shouldBe "1"
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "2"
                }
            }
        }
    }

    Given("A walker built using setTraversalGuardState") {

        When("The guard state is set explicitly via setTraversalGuardState") {
            val guardState = object : ITransitionGuardState {
                var allowTransition = true
                override fun onReset() {
                    allowTransition = true
                }
            }

            val v1 = TestVertex("1")
            val v2 = TestVertex("2")

            val walker = buildGuardedWalker(guardState) {
                setTraversalGuardState(guardState)
                buildGraph(v1) {
                    addVertex(v1) {
                        addEdge {
                            setTo(v2)
                            setEdgeTransitionGuard {
                                this.guardState?.let { true }
                                    ?: false
                            }
                        }
                    }
                    addVertex(v2)
                }
            }

            Then("The walker uses the guard state for transition decisions") {
                runBlocking {
                    walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    walker.current.value.vertex.id shouldBe "2"
                }
            }
        }
    }

    Given("A graph built using buildGraphOnly with simplified type parameters") {

        When("buildGraphOnly is called with only vertex and ID type parameters") {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")

            val graph = buildPlainGraphOnly {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                }
                addVertex(v2)
            }

            Then("The graph is built correctly with default type parameters") {
                graph.containsVertex(v1) shouldBe true
                graph.containsVertex(v2) shouldBe true
                graph.getOutgoingEdgesSorted(v1)?.size shouldBe 1
            }
        }
    }

    Given("A graph builder with edge validation") {

        When("An edge is added without calling setTo") {
            Then("The build fails with a validation error") {
                shouldThrow<IllegalStateException> {
                    buildWalker {
                        val v1 = TestVertex("1")
                        buildGraph(v1) {
                            addVertex(v1) {
                                addEdge {
                                }
                            }
                        }
                    }
                }
            }
        }
    }
})
