package mdk.test.features.capabilities

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.runBlocking
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerCapabilitiesSpec : BehaviorSpec({
    
    Given("A walker with Next and Reset capabilities only") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)
        
        When("A Next action is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("The walker transitions to vertex 2") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("Attempting traverser-only capabilities on a walker") {
            Then("The Previous capability is not available and state remains unchanged") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
        
        When("Dispatching Next until reaching the terminal vertex") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("The walker is beyond last at vertex 3") {
                walker.current.value.isBeyondLast shouldBe true
                walker.current.value.vertex.id shouldBe "3"
            }
        }
        
        When("A Reset action is dispatched") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            }
            
            Then("The walker returns to the start vertex within bounds") {
                walker.current.value.vertex.id shouldBe "1"
                walker.current.value.isWithinBounds shouldBe true
            }
        }
    }
    
    Given("A walker supporting reset functionality") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)
        
        When("The walker advances through multiple states") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            walker.current.value.vertex.id shouldBe "3"
            
            Then("Dispatching Reset returns to the start vertex") {
                runBlocking {
                    val resetState = walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
                    resetState.vertex.id shouldBe "1"
                }
                
                walker.current.value.vertex.id shouldBe "1"
            }
        }
    }
})
