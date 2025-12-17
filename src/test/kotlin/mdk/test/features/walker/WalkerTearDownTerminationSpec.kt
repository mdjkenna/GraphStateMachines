package mdk.test.features.walker

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerTearDownTerminationSpec : BehaviorSpec({
    Given("A walker that dispatches actions on a coroutine channel loop") {
        val guardState = TestTransitionGuardState()
        val walker = GraphScenarios.linearThreeVertexWalker(guardState)

        When("The walker is advanced once and then torn down") {
            runBlocking {
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            }
            walker.current.value.vertex.id shouldBe "2"
            walker.tearDown()

            Then("Subsequent dispatches cannot complete because the channel loop is terminated") {
                shouldThrow<TimeoutCancellationException> {
                    runBlocking {
                        withTimeout(200) {
                            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
                        }
                    }
                }
                walker.current.value.vertex.id shouldBe "2"
            }
        }
    }
})
