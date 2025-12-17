package mdk.test.features.dispatcher

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mdk.gsm.builder.DispatcherConfig
import mdk.gsm.builder.buildTraverserWithActions
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.utils.TestTransitionGuardState
import mdk.test.utils.TestVertex

class DispatcherConfigSpec : BehaviorSpec({
    
    Given("A traverser using the default dispatcher settings") {
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = DispatcherConfig<Nothing>()
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            
            buildGraph(v1) {
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
        }
        
        When("A Next action is dispatched and awaited") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("Traversal advances to the next vertex") {
                traverser.current.value.vertex.id shouldBe "2"
            }
        }
    }
    
    Given("A traverser configured with DROP_OLDEST buffer overflow") {
        val droppedActions = mutableListOf<GraphStateMachineAction<Int>>()
        val config = DispatcherConfig<Int>(
            capacity = 2,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = { action ->
                droppedActions.add(action)
            }
        )
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = config
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard {
                            delay(100)
                            true
                        }
                    }
                }
                addVertex(v2) {
                    addEdge {
                        setTo(v3)
                    }
                }
                addVertex(v3) {
                    addEdge {
                        setTo(v4)
                    }
                }
                addVertex(v4)
            }
        }
        
        When("Two NextArgs actions are dispatched sequentially") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(1))
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(2))
            }
            
            Then("Traversal reaches vertex 3 after processing both actions") {
                traverser.current.value.vertex.id shouldBe "3"
            }
        }
    }
    
    Given("A traverser configured with DROP_LATEST buffer overflow") {
        val config = DispatcherConfig<Int>(
            capacity = 1,
            onBufferOverflow = BufferOverflow.DROP_LATEST
        )
        
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = config
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                        setEdgeTransitionGuard {
                            delay(100)
                            true
                        }
                    }
                }
                addVertex(v2) {
                    addEdge {
                        setTo(v3)
                    }
                }
                addVertex(v3)
            }
        }
        
        When("A NextArgs action is dispatched and awaited") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(1))
            }
            
            Then("Traversal advances to vertex 2") {
                traverser.current.value.vertex.id shouldBe "2"
            }
        }
    }
    
    Given("A traverser with UNLIMITED capacity") {
        val config = DispatcherConfig<Int>(
            capacity = Channel.UNLIMITED
        )
        
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = config
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            val v3 = TestVertex("3")
            val v4 = TestVertex("4")
            
            buildGraph(v1) {
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
                addVertex(v3) {
                    addEdge {
                        setTo(v4)
                    }
                }
                addVertex(v4)
            }
        }
        
        When("Three NextArgs actions are dispatched sequentially") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(1))
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(2))
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(3))
            }
            
            Then("Traversal reaches vertex 4 after three actions") {
                traverser.current.value.vertex.id shouldBe "4"
            }
        }
    }
    
    Given("A traverser with RENDEZVOUS capacity (no buffering)") {
        val config = DispatcherConfig<Nothing>(
            capacity = Channel.RENDEZVOUS
        )
        
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = config
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                }
                addVertex(v2)
            }
        }
        
        When("A Next action is dispatched and awaited") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            
            Then("Traversal advances to vertex 2") {
                traverser.current.value.vertex.id shouldBe "2"
            }
        }
    }
    
    Given("A traverser with onUndeliveredElement callback") {
        val undeliveredActions = mutableListOf<GraphStateMachineAction<String>>()
        val config = DispatcherConfig<String>(
            capacity = 2,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
            onUndeliveredElement = { action ->
                undeliveredActions.add(action)
            }
        )
        
        val guardState = TestTransitionGuardState()
        val traverser = buildTraverserWithActions(
            guardState = guardState,
            dispatcherConfig = config
        ) {
            val v1 = TestVertex("1")
            val v2 = TestVertex("2")
            
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v2)
                    }
                }
                addVertex(v2)
            }
        }
        
        When("The traverser is torn down after processing a Next action") {
            runBlocking {
                traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                traverser.tearDown()
            }
            
            Then("Teardown completes and state remains consistent") {
                traverser.current.value.vertex.id shouldBe "2"
            }
        }
    }
    
    Given("DispatcherConfig data class properties") {
        
        When("Creating a config with custom capacity") {
            val config = DispatcherConfig<Int>(capacity = 10)
            
            Then("The capacity should be set correctly") {
                config.capacity shouldBe 10
            }
        }
        
        When("Creating a config with custom overflow strategy") {
            val config = DispatcherConfig<Int>(
                onBufferOverflow = BufferOverflow.DROP_LATEST
            )
            
            Then("The overflow strategy should be set correctly") {
                config.onBufferOverflow shouldBe BufferOverflow.DROP_LATEST
            }
        }
        
        When("Creating a config with all custom parameters") {
            val callback: (GraphStateMachineAction<String>) -> Unit = { }
            val config = DispatcherConfig(
                capacity = 5,
                onBufferOverflow = BufferOverflow.DROP_OLDEST,
                onUndeliveredElement = callback
            )
            
            Then("All properties should be set correctly") {
                config.capacity shouldBe 5
                config.onBufferOverflow shouldBe BufferOverflow.DROP_OLDEST
                config.onUndeliveredElement shouldBe callback
            }
        }
    }
})
