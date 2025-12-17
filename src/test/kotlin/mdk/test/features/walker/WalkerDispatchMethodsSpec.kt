package mdk.test.features.walker

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mdk.gsm.builder.buildWalkerWithActions
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState
import mdk.test.utils.TestVertex

class WalkerDispatchMethodsSpec : BehaviorSpec({
    
    Given("A walker for testing all dispatch method overloads") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)
        
        When("launchDispatch(Next) is called for fire-and-forget execution") {
            walker.launchDispatch(GraphStateMachineAction.Next)
            
            Then("The action eventually processes without blocking the caller") {
                runBlocking {
                    withTimeout(1000) {
                        var attempts = 0
                        while (walker.current.value.vertex.id == "1" && attempts < 50) {
                            delay(20)
                            attempts++
                        }
                        walker.current.value.vertex.id shouldBe "2"
                    }
                }
            }
        }
        
        When("launchDispatch(Reset) is called to reset asynchronously") {
            walker.launchDispatch(GraphStateMachineAction.Reset)
            
            Then("The walker resets to start without blocking") {
                runBlocking {
                    withTimeout(1000) {
                        var attempts = 0
                        while (walker.current.value.vertex.id != "1" && attempts < 50) {
                            delay(20)
                            attempts++
                        }
                        walker.current.value.vertex.id shouldBe "1"
                    }
                }
            }
        }
        
        When("suspend dispatch(Next) is called without awaiting result") {
            runBlocking {
                walker.dispatch(GraphStateMachineAction.Next)
                delay(100)
            }
            
            Then("The walker should have transitioned") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("suspend dispatch(Reset) is called") {
            runBlocking {
                walker.dispatch(GraphStateMachineAction.Reset)
                delay(100)
            }
            
            Then("The walker should have reset") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }
    }
    
    Given("A walker with action arguments for testing NextArgs dispatch variants") {
        val guardState = TestTransitionGuardState()
        val walker = buildWalkerWithActions<TestVertex, String, TestTransitionGuardState, Int>(guardState) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard { args == 100 }
                    }
                    addEdge {
                        setTo(v3)
                        setEdgeTransitionGuard { args == 200 }
                    }
                }
                addVertex(v2)
                addVertex(v3)
            }
        }
        
        When("launchDispatch(NextArgs) is called with argument 100") {
            walker.launchDispatch(GraphStateMachineAction.NextArgs(100))
            
            Then("The walker takes the correct path based on the argument") {
                runBlocking {
                    withTimeout(1000) {
                        var attempts = 0
                        while (walker.current.value.vertex.id == "1" && attempts < 50) {
                            delay(20)
                            attempts++
                        }
                        walker.current.value.vertex.id shouldBe "2"
                        walker.current.value.args shouldBe 100
                    }
                }
            }
        }
        
        When("Reset is called to return to start") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            }
            
            Then("Walker is back at start") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }
        
        When("suspend dispatch(NextArgs) is called with argument 200") {
            runBlocking {
                walker.dispatch(GraphStateMachineAction.NextArgs(200))
                delay(100)
            }
            
            Then("The walker takes the alternate path") {
                walker.current.value.vertex.id shouldBe "3"
                walker.current.value.args shouldBe 200
            }
        }
    }
    
    Given("A walker for testing component destructuring operators") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)
        
        When("The walker is destructured using component1() and component2()") {
            val (state, dispatcher) = walker
            
            Then("component1() returns the WalkerState") {
                state.current.value.vertex.id shouldBe "1"
            }
            
            Then("component2() returns the WalkerDispatcher") {
                runBlocking {
                    dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                }
                state.current.value.vertex.id shouldBe "2"
            }
            
            Then("State reflects changes made through dispatcher") {
                runBlocking {
                    dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                }
                state.current.value.vertex.id shouldBe "3"
            }
        }
    }
    
    Given("A walker for testing walkerState and walkerDispatcher properties") {
        val guardState = TestTransitionGuardState()
        val v1 = TestVertex("1")
        val v2 = TestVertex("2")
        val walker = GraphScenarios.conditionalWithArgsWalker<Nothing>(guardState, { true }, v1, v2)
        
        When("Accessing walkerState property") {
            val state = walker.walkerState
            
            Then("It provides read access to current state") {
                state.current.value.vertex.id shouldBe "1"
            }
        }
        
        When("Accessing walkerDispatcher property") {
            val dispatcher = walker.walkerDispatcher
            
            Then("It allows dispatching actions") {
                runBlocking {
                    dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                }
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("Using both properties together") {
            val state = walker.walkerState
            val dispatcher = walker.walkerDispatcher
            
            runBlocking {
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            }
            
            Then("State reflects dispatcher actions") {
                state.current.value.vertex.id shouldBe "1"
            }
        }
    }
})
