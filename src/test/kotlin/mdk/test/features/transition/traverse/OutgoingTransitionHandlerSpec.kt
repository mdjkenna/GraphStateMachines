package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.OutgoingTransitionTestGuardState

class OutgoingTransitionHandlerSpec : BehaviorSpec({
    given("A graph with vertices that use onOutgoingTransition handlers") {
        val ids = object {
            val START = "start"
            val MIDDLE = "middle"
            val END = "end"
        }
        val guardState = OutgoingTransitionTestGuardState()
        val transitionAttempts = mutableListOf<String>()
        val publishedStates = mutableListOf<String>()

        val walker = GraphScenarios.outgoingTransitionHandlerWalker(
            guardState,
            StateMachineScopeFactory.newScope()
        ) { vertexId -> transitionAttempts.add(vertexId) }

        publishedStates.add(walker.current.value.vertex.id)

        `when`("A NEXT action is received and the current vertex has an onOutgoingTransition handler that prevents traversal") {
            // The START vertex has an onOutgoingTransition handler that prevents traversal when a condition is met
            guardState.shouldPreventTransition = true

            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            publishedStates.add(result.vertex.id)

            then("The traversal is prevented and the state remains the same") {
                transitionAttempts shouldContainExactly listOf(ids.START)
                publishedStates shouldContainExactly listOf(ids.START, ids.START)
                walker.current.value.vertex.id shouldBe ids.START
            }
        }

        `when`("A NEXT action is received and the current vertex has an onOutgoingTransition handler that allows traversal") {
            // Now allow the traversal
            guardState.shouldPreventTransition = false

            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            publishedStates.add(result.vertex.id)

            then("The traversal occurs normally") {
                transitionAttempts shouldContainExactly listOf(ids.START, ids.START)
                publishedStates shouldContainExactly listOf(ids.START, ids.START, ids.MIDDLE)
                walker.current.value.vertex.id shouldBe ids.MIDDLE
            }
        }

        `when`("Another NEXT action is received and the current vertex has no onOutgoingTransition handler") {
            // The MIDDLE vertex has no onOutgoingTransition handler, so traversal should always occur
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            publishedStates.add(result.vertex.id)

            then("The traversal occurs normally") {
                transitionAttempts shouldContainExactly listOf(ids.START, ids.START)
                publishedStates shouldContainExactly listOf(ids.START, ids.START, ids.MIDDLE, ids.END)
                walker.current.value.vertex.id shouldBe ids.END
            }
        }
    }
})