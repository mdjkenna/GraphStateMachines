package mdk.test.features.transition.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.builder.buildGuardedWalker
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.gsm.util.StringVertex

class WalkerSelfReferentialSpec : BehaviorSpec({

    Given("A walker with a self-referential vertex limited to 7 cycles by guard state") {
        var cycleCount = 0
        val transitionGuardState = object : ITransitionGuardState {
            override fun onReset() { cycleCount = 0 }
        }

        val v1 = StringVertex("1")
        val v2 = StringVertex("2")

        val walker = buildGuardedWalker(transitionGuardState, StateMachineScopeFactory.newScope()) {
            buildGraph(v1) {
                addVertex(v1) {
                    addEdge {
                        setTo(v1)
                        setEdgeTransitionGuard {
                            if (cycleCount < 7) {
                                cycleCount++
                                true
                            } else {
                                false
                            }
                        }
                    }

                    addEdge {
                        setTo(v2)
                    }
                }

                addVertex(v2)
            }
        }

        When("Advancing the walker forward seven times") {
            repeat(7) {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }

            Then("The walker should still be at vertex '1' after seven cycles") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }

        When("Advancing once more after reaching the cycle guard limit") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should proceed to vertex '2' instead of continuing the cycle") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }

        When("Resetting the walker and manually setting the cycle count to 2") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
            cycleCount = 2

            Then("The walker should be back at the start vertex") {
                walker.current.value.vertex.id shouldBe "1"
            }
        }

        When("Advancing six more times after reset") {
            repeat(5) {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The walker should proceed to vertex '2' after 5 more cycles (total of 7)") {
                walker.current.value.vertex.id shouldBe "2"
            }
        }
    }
})