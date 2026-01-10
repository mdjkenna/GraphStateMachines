package mdk.test.features.transition.traverse

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestMachineContext

class OnEntryHookSpec : BehaviorSpec({
    given("A graph with vertices that use onEntry hooks") {
        val ids = object {
            val START = "start"
            val MIDDLE = "middle"
            val END = "end"
        }
        val machineContext = TestMachineContext()
        val onEntryCalls = mutableListOf<String>()
        val publishedStates = mutableListOf<String>()

        val walker = GraphScenarios.onEntryHookWalker(
            machineContext,
            StateMachineScopeFactory.newScope()
        ) { vertexId -> onEntryCalls.add(vertexId) }

        publishedStates.add(walker.current.value.vertex.id)

        `when`("The walker is initialized at the start vertex") {
            then("The onEntry hook is NOT called for the initial state") {
                onEntryCalls shouldBe emptyList()
                publishedStates shouldContainExactly listOf(ids.START)
                walker.current.value.vertex.id shouldBe ids.START
            }
        }

        `when`("A NEXT action is received and transitions to the middle vertex") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            publishedStates.add(result.vertex.id)

            then("The onEntry hook is called for the middle vertex after state is published") {
                onEntryCalls shouldContainExactly listOf(ids.MIDDLE)
                publishedStates shouldContainExactly listOf(ids.START, ids.MIDDLE)
                walker.current.value.vertex.id shouldBe ids.MIDDLE
            }
        }

        `when`("Another NEXT action is received and transitions to the end vertex") {
            val result = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            publishedStates.add(result.vertex.id)

            then("The onEntry hook is called for the end vertex") {
                onEntryCalls shouldContainExactly listOf(ids.MIDDLE, ids.END)
                publishedStates shouldContainExactly listOf(ids.START, ids.MIDDLE, ids.END)
                walker.current.value.vertex.id shouldBe ids.END
            }
        }
    }

    given("A graph where onEntry hooks receive correct context") {
        val machineContext = TestMachineContext()
        val enteredVertices = mutableListOf<String>()

        val walker = GraphScenarios.onEntryHookWalker(
            machineContext,
            StateMachineScopeFactory.newScope()
        ) { vertexId -> enteredVertices.add(vertexId) }

        `when`("Traversing through the entire graph") {
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            then("All onEntry hooks are called for each entered vertex") {
                enteredVertices shouldContainExactly listOf("middle", "end")
            }
        }

        `when`("A RESET action returns to the start vertex") {
            enteredVertices.clear()
            walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)

            then("The onEntry hook is called for the start vertex on reset") {
                enteredVertices shouldContainExactly listOf("start")
                walker.current.value.vertex.id shouldBe "start"
            }
        }
    }
})
