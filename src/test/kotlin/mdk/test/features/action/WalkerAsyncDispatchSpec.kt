package mdk.test.features.action

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerAsyncDispatchSpec : BehaviorSpec({
    
    Given("A four-vertex linear walker") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearFourVertexTraverser(guardState)
        
        When("A Next action is dispatched and awaited") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("The walker transitions to the next vertex") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("Multiple Next actions are dispatched sequentially with awaiting") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("The walker completes all transitions in order") {
                walker.current.value.vertex.id shouldBe "4"
            }
        }
        
        When("A Reset action is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            }
            
            Then("The walker returns to the start vertex") {
                walker.current.value.vertex.id shouldBe "1"
                walker.current.value.isWithinBounds shouldBe true
            }
        }
        
        When("A Next action is dispatched after reset") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("The walker transitions forward from the start vertex") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
    }
    
    Given("A walker with conditional edges based on action arguments") {
        val guardState = TestTransitionGuardState()
        val walker = mdk.gsm.builder.buildWalkerWithActions<mdk.test.utils.TestVertex, String, TestTransitionGuardState, Int>(guardState) {
            val v1 = mdk.test.utils.TestVertex("1")
            val v2 = mdk.test.utils.TestVertex("2")
            val v3 = mdk.test.utils.TestVertex("3")
            val v4 = mdk.test.utils.TestVertex("4")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { args != null && args == 100 }
                    }
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard { args != null && args == 200 }
                    }
                }
                addVertex(v2) {
                    addEdge { setTo(v4) }
                }
                addVertex(v3) {
                    addEdge { setTo(v4) }
                }
                addVertex(v4)
            }
        }
        
        When("A NextArgs action with argument 100 is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(100))
            }
            
            Then("The walker follows the conditional path to vertex 2") {
                walker.current.value.vertex.id shouldBe "2"
                walker.current.value.args shouldBe 100
            }
        }
        
        When("A Reset action is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            }
            
            Then("The walker returns to the start vertex") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }
        
        When("A NextArgs action with argument 200 is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(200))
            }
            
            Then("The walker follows the alternate conditional path to vertex 3") {
                walker.current.value.vertex.id shouldBe "3"
                walker.current.value.args shouldBe 200
            }
        }
    }
    
    Given("A walker supporting component destructuring") {
        val guardState = TestTransitionGuardState()
        val start = mdk.test.utils.TestVertex("start")
        val end = mdk.test.utils.TestVertex("end")
        val walker = GraphScenarios.conditionalWithArgsWalker<Nothing>(guardState, { true }, start, end)
        
        When("The walker is destructured using component operators") {
            val (state, dispatcher) = walker
            
            Then("The state component provides read-only access to current state") {
                state.current.value.vertex.id shouldBe "start"
            }
            
            Then("The dispatcher component allows action dispatching with state updates") {
                runBlocking {
                    dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                    state.current.value.vertex.id shouldBe "end"
                }
            }
        }
        
        When("The walkerState property is accessed directly") {
            val walkerState = walker.walkerState
            
            Then("The property provides read-only access to current state") {
                walkerState.current.value.vertex.id shouldBe "end"
            }
        }
        
        When("The walkerDispatcher property is accessed directly") {
            val walkerDispatcher = walker.walkerDispatcher
            
            Then("The dispatcher allows state manipulation via Reset action") {
                runBlocking {
                    walkerDispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                    walker.current.value.vertex.id shouldBe "start"
                }
            }
        }
    }
    
    Given("A walker with teardown lifecycle management") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)
        
        When("Actions are dispatched during normal operation") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("State transitions complete successfully") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("The tearDown method is invoked") {
            walker.tearDown()
            
            Then("The walker coroutine scope is cancelled but state remains readable") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
    }
})
