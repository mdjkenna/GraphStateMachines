package mdk.test.features.state.walk

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.TestTransitionGuardState

class WalkerIntermediateStateSpec : BehaviorSpec({

    Given("A walker with intermediate states") {
        val ids = WalkerTestingIds
        val publishedStates = mutableListOf<String>()
        val beforeVisitCalls = mutableListOf<String>()
        val guardState = TestTransitionGuardState()
        
        val walker = GraphScenarios.intermediateStateWalker(
            guardState,
            StateMachineScopeFactory.newScope()
        ) { vertexId -> beforeVisitCalls.add(vertexId) }

        publishedStates.add(walker.current.value.vertex.id)

        When("""
                A NEXT navigation action is received across a chain of vertices: 
                [regular state] -> [intermediate state] - [regular state]
            """.trimIndent()
        ) {

            publishedStates.add(
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(
                """
                Intermediate states are not published. They are automatically advanced through.
                The current state demonstrates auto advance works correctly by containing the regular state after the intermediate state.
            """.trimIndent()
            ) {
                walker.current.value.vertex.id shouldBe ids.REGULAR_1
            }

            Then("The sequence of published states excludes intermediate states, as these are not published") {
                publishedStates shouldContainExactly listOf(ids.START, ids.REGULAR_1)
            }

            Then(
                """
                    'onBeforeVisit' has been called on the vertices which have been visited by progressing forwards using NEXT action.
                    """.trimIndent()
            ) {
                beforeVisitCalls shouldContainExactly listOf(ids.INTERMEDIATE_1, ids.REGULAR_1)
            }
        }

        When("A second NEXT navigation action is received and the next vertex is an intermediate state again") {
            publishedStates.add(
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(
                "Again, the current state is a regular state, the intermediate state is auto-advanced through"
            ) {
                walker.current.value.vertex.id shouldBe ids.REGULAR_2
            }

            Then(
                "The traversed intermediate state has been excluded from publishing"
            ) {
                publishedStates shouldContainExactly listOf(ids.START, ids.REGULAR_1, ids.REGULAR_2)
            }

            Then(
                "'onBeforeVisit' has been called on all vertices that have been arrived at by a next action"
            ) {
                beforeVisitCalls shouldContainExactly listOf(ids.INTERMEDIATE_1, ids.REGULAR_1, ids.INTERMEDIATE_2, ids.REGULAR_2)
            }
        }

        When(
            """
            A NEXT action is received with the next vertex auto advancing while being the last available vertex. 
            The END vertex, which is the next and last vertex, with no more vertices left in the graph to traverse, is an intermediate state in the sense that it 'auto-advances'. 
            However, given that there are no more vertices to traverse, there is no state to auto-advance to.
            """.trimMargin()
        ) {

            publishedStates.add(
                walker.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(
                ("The walker publishes the END vertex state which is out of bounds. " +
                  "For a state to be an 'intermediate-state' it has to be skippable currently. " +
                  "In this situation, although calling auto-advance, the state is not skippable as there are no states to traversal to left.")
                    .trimMargin()
            ) {
                walker.current.value.isBeyondLast shouldBe true
                walker.current.value.vertex.id shouldBe ids.END

                publishedStates shouldContainExactly listOf(
                    ids.START,
                    ids.REGULAR_1,
                    ids.REGULAR_2,
                    ids.END
                )
            }
        }
    }
}) {
    companion object {
        object WalkerTestingIds {
            const val START = "start"
            const val INTERMEDIATE_1 = "intermediate1"
            const val INTERMEDIATE_2 = "intermediate2"
            const val REGULAR_1 = "regular1"
            const val REGULAR_2 = "regular2"
            const val END = "end"
        }
    }
}