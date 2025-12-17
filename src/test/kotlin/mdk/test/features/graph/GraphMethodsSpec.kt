package mdk.test.features.graph

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mdk.gsm.builder.buildGraphOnly
import mdk.gsm.state.ITransitionGuardState
import mdk.test.utils.TestVertex

class GraphMethodsSpec : BehaviorSpec({
    
    Given("A graph built directly using buildGraphOnly for testing graph query methods") {
        val v1 = TestVertex("1")
        val v2 = TestVertex("2")
        val v3 = TestVertex("3")
        val v4 = TestVertex("4")
        
        val graph = buildGraphOnly<TestVertex, String, ITransitionGuardState, Nothing> {
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
                    setTo(v4)
                }
            }
            addVertex(v4)
        }
        
        When("Checking if graph contains a vertex by instance") {
            val containsV1 = graph.containsVertex(v1)
            val containsV2 = graph.containsVertex(v2)
            val nonExistentVertex = TestVertex("999")
            val containsNonExistent = graph.containsVertex(nonExistentVertex)
            
            Then("It correctly identifies vertices that exist in the graph") {
                containsV1 shouldBe true
                containsV2 shouldBe true
                containsNonExistent shouldBe false
            }
        }
        
        When("Checking if graph contains a vertex by ID") {
            val containsId1 = graph.containsVertexId("1")
            val containsId2 = graph.containsVertexId("2")
            val containsNonExistentId = graph.containsVertexId("999")
            
            Then("It correctly identifies vertex IDs that exist") {
                containsId1 shouldBe true
                containsId2 shouldBe true
                containsNonExistentId shouldBe false
            }
        }
        
        When("Getting a vertex by its ID") {
            val retrievedV1 = graph.getVertex("1")
            val retrievedV2 = graph.getVertex("2")
            val retrievedNonExistent = graph.getVertex("999")
            
            Then("It returns the correct vertex instance or null") {
                retrievedV1 shouldBe v1
                retrievedV2 shouldBe v2
                retrievedNonExistent shouldBe null
            }
        }
        
        When("Getting outgoing edges for a vertex") {
            val v1Edges = graph.getOutgoingEdgesSorted(v1)
            val v4Edges = graph.getOutgoingEdgesSorted(v4)
            val nonExistentVertex = TestVertex("999")
            val nonExistentEdges = graph.getOutgoingEdgesSorted(nonExistentVertex)
            
            Then("It returns the correct list of outgoing edges") {
                v1Edges shouldNotBe null
                v1Edges!! shouldHaveSize 2
                
                v4Edges shouldNotBe null
                v4Edges!! shouldHaveSize 0
                
                nonExistentEdges shouldBe null
            }
        }
    }
    
    Given("Multiple separate graphs for testing vertex and edge isolation") {
        val v1 = TestVertex("A")
        val v2 = TestVertex("B")
        val v3 = TestVertex("C")
        
        val graph1 = buildGraphOnly<TestVertex, String, ITransitionGuardState, Nothing> {
            addVertex(v1) {
                addEdge {
                    setTo(v2)
                }
            }
            addVertex(v2)
        }
        
        val graph2 = buildGraphOnly<TestVertex, String, ITransitionGuardState, Nothing> {
            addVertex(v2) {
                addEdge {
                    setTo(v3)
                }
            }
            addVertex(v3)
        }
        
        When("Checking vertex containment across different graphs") {
            val graph1ContainsV1 = graph1.containsVertex(v1)
            val graph1ContainsV2 = graph1.containsVertex(v2)
            val graph1ContainsV3 = graph1.containsVertex(v3)
            
            val graph2ContainsV1 = graph2.containsVertex(v1)
            val graph2ContainsV2 = graph2.containsVertex(v2)
            val graph2ContainsV3 = graph2.containsVertex(v3)
            
            Then("Each graph contains only its own vertices") {
                graph1ContainsV1 shouldBe true
                graph1ContainsV2 shouldBe true
                graph1ContainsV3 shouldBe false
                
                graph2ContainsV1 shouldBe false
                graph2ContainsV2 shouldBe true
                graph2ContainsV3 shouldBe true
            }
        }
        
        When("Checking edge containment") {
            val graph1V1Edges = graph1.getOutgoingEdgesSorted(v1)
            val graph2V2Edges = graph2.getOutgoingEdgesSorted(v2)
            
            Then("Each graph has its own edge configuration") {
                graph1V1Edges shouldNotBe null
                graph1V1Edges!! shouldHaveSize 1
                
                graph2V2Edges shouldNotBe null
                graph2V2Edges!! shouldHaveSize 1
            }
        }
    }
    
    Given("A graph with complex edge configurations") {
        val start = TestVertex("start")
        val pathA = TestVertex("pathA")
        val pathB = TestVertex("pathB")
        val end = TestVertex("end")
        
        val graph = buildGraphOnly<TestVertex, String, ITransitionGuardState, Nothing> {
            addVertex(start) {
                addEdge {
                    setTo(pathA)
                    setEdgeTransitionGuard {
                        true
                    }
                }
                addEdge {
                    setTo(pathB)
                    setEdgeTransitionGuard {
                        false
                    }
                }
            }
            addVertex(pathA) {
                addEdge {
                    setTo(end)
                }
            }
            addVertex(pathB) {
                addEdge {
                    setTo(end)
                }
            }
            addVertex(end)
        }
        
        When("Querying vertices in complex graph") {
            val hasStart = graph.containsVertexId("start")
            val hasPathA = graph.containsVertexId("pathA")
            val hasPathB = graph.containsVertexId("pathB")
            val hasEnd = graph.containsVertexId("end")
            
            Then("All vertices are accessible by ID") {
                hasStart shouldBe true
                hasPathA shouldBe true
                hasPathB shouldBe true
                hasEnd shouldBe true
            }
        }
        
        When("Getting edges from the start vertex") {
            val startEdges = graph.getOutgoingEdgesSorted(start)
            
            Then("Both conditional edges are present") {
                startEdges shouldNotBe null
                startEdges!! shouldHaveSize 2
            }
        }
        
        When("Retrieving terminal vertex") {
            val endVertex = graph.getVertex("end")
            val endEdges = graph.getOutgoingEdgesSorted(endVertex!!)
            
            Then("Terminal vertex has no outgoing edges") {
                endVertex shouldBe end
                endEdges shouldNotBe null
                endEdges!! shouldHaveSize 0
            }
        }
    }
})
