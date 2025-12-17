package mdk.test.features.action

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import mdk.gsm.builder.DispatcherConfig
import mdk.gsm.builder.buildTraverserWithActions
import mdk.gsm.builder.buildWalkerWithActions
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.utils.TestTransitionGuardState
import mdk.test.utils.TestVertex

class AtomicActionsSpec : BehaviorSpec(
    body = {
        Given("A traverser with a buffered channel and delayed edge guards") {
            val receivedActions = ArrayList<Int>()

            val traverser = buildTraverserWithActions<TestVertex, String, TestTransitionGuardState, Int>(guardState = TestTransitionGuardState()) {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                val v5 = TestVertex("5")

                buildGraph(startAtVertex = v1) {
                    addVertex(v1) {
                        addEdge {
                            setTo(v2)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }

                        onOutgoingTransition {
                            receivedActions.add(args ?: -1)
                        }
                    }

                    addVertex(v2) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }

                        onOutgoingTransition {
                            receivedActions.add(args ?: -1)
                        }
                    }

                    addVertex(v3) {
                        addEdge {
                            setTo(v5)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }

                        onOutgoingTransition {
                            receivedActions.add(args ?: -1)
                        }
                    }

                    addVertex(v5)
                }
            }

            When("Multiple NextArgs actions are dispatched without awaiting results") {
                traverser.launchDispatch(GraphStateMachineAction.NextArgs(1))
                traverser.launchDispatch(GraphStateMachineAction.NextArgs(2))
                traverser.launchDispatch(GraphStateMachineAction.NextArgs(3))

                withTimeout(3000) {
                    while (traverser.current.value.vertex.id != "5") {
                        delay(10)
                    }
                }

                Then("All transitions complete sequentially and arguments are preserved") {
                    val tracedPath = traverser.tracePath()
                    tracedPath.map { it.id } shouldBe listOf("1", "2", "3", "5")

                    traverser.current.value.vertex.id shouldBe "5"
                    traverser.current.value.args shouldBe 3
                    receivedActions.toList() shouldBeEqual listOf(1, 2, 3)
                }
            }
        }

        Given("A traverser with delayed edge guards for awaited dispatch") {
            val traverser = buildTraverserWithActions<TestVertex, String, TestTransitionGuardState, Int>(guardState = TestTransitionGuardState()) {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                val v5 = TestVertex("5")

                buildGraph(startAtVertex = v1) {
                    addVertex(v1) {
                        addEdge {
                            setTo(v2)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }
                    }
                    addVertex(v2) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }
                    }
                    addVertex(v3) {
                        addEdge {
                            setTo(v5)
                            setEdgeTransitionGuard {
                                delay(500)
                                true
                            }
                        }
                    }
                    addVertex(v5)
                }
            }

            When("NextArgs actions are dispatched with results awaited between each call") {
                val result1 = traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(10))
                result1.vertex.id shouldBe "2"
                result1.args shouldBe 10

                val result2 = traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(20))
                result2.vertex.id shouldBe "3"
                result2.args shouldBe 20

                val result3 = traverser.dispatchAndAwaitResult(GraphStateMachineAction.NextArgs(30))
                result3.vertex.id shouldBe "5"
                result3.args shouldBe 30

                Then("Each transition completes in order with correct arguments") {
                    val tracedPath = traverser.tracePath().map { it.id }
                    tracedPath shouldBe listOf("1", "2", "3", "5")

                    traverser.current.value.vertex.id shouldBe "5"
                    traverser.current.value.args shouldBe 30
                }
            }
        }

        Given("A walker configured with a conflated dispatcher channel") {
            val dispatcherConfig = DispatcherConfig<Int>(capacity = Channel.CONFLATED)
            val walker = buildWalkerWithActions(
                guardState = TestTransitionGuardState(),
                dispatcherConfig = dispatcherConfig
            ) {
                val v1 = TestVertex("1")
                val v2 = TestVertex("2")
                val v3 = TestVertex("3")
                val v5 = TestVertex("5")

                buildGraph(startAtVertex = v1) {
                    addVertex(v1) {
                        addEdge {
                            setTo(v2)
                            setEdgeTransitionGuard { true }
                        }
                    }
                    addVertex(v2) {
                        addEdge {
                            setTo(v3)
                            setEdgeTransitionGuard { true }
                        }
                    }
                    addVertex(v3) {
                        addEdge {
                            setTo(v5)
                            setEdgeTransitionGuard { true }
                        }
                    }
                    addVertex(v5)
                }
            }

            When("NextArgs actions are dispatched using the suspend dispatch method") {
                suspend fun dispatchSequence() {
                    walker.dispatch(GraphStateMachineAction.NextArgs(100))
                    delay(1)
                    walker.current.value.vertex.id shouldBe "2"
                    walker.current.value.args shouldBe 100

                    walker.dispatch(GraphStateMachineAction.NextArgs(200))
                    delay(1)
                    walker.current.value.vertex.id shouldBe "3"
                    walker.current.value.args shouldBe 200

                    walker.dispatch(GraphStateMachineAction.NextArgs(300))
                    delay(1)
                    walker.current.value.vertex.id shouldBe "5"
                    walker.current.value.args shouldBe 300
                }

                dispatchSequence()

                Then("State updates are immediately observable due to channel conflation causing dispatch suspension") {
                    walker.current.value.vertex.id shouldBe "5"
                    walker.current.value.args shouldBe 300
                }
            }
        }
    }
)
