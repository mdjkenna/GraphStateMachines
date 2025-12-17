package mdk.test.features.state.traverse

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mdk.gsm.state.GraphStateMachineAction
import mdk.gsm.state.ITransitionGuardState
import mdk.test.utils.AssertionUtils.assertTracedPathWithCurrentState
import mdk.test.utils.TestBuilderUtils


class TestTransitionGuardState : ITransitionGuardState {
    override fun onReset() { /* No state to reset */ }
}

class IntermediateStateSpec : BehaviorSpec({

    Given("A graph state machine with intermediate states") {
        val ids = TestBuilderUtils.IntermediateTestingIds
        val publishedStates = mutableListOf<String>()
        val beforeVisitCalls = mutableListOf<String>()
        val guardState = TestTransitionGuardState()
        
        val (state, dispatcher) =
            TestBuilderUtils.buildIntermediateTestGraph(
                guardState = guardState,
                beforeVisitCalls = beforeVisitCalls
            )

        publishedStates.add(state.current.value.vertex.id)

        When("""
                A NEXT navigation action is received across a chain of vertices: 
                [regular state] -> [intermediate state] - [regular state]
            """.trimIndent()
        ) {

            publishedStates.add(
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(
                """
                Intermediate states are not published. They are automatically advanced through.
                The traced path demonstrates auto advance works correctly by containing the three vertices contains the correct path through all intermediate states.
            """.trimIndent()
            ) {

                assertTracedPathWithCurrentState(
                    listOf(ids.START, ids.INTERMEDIATE_1, ids.REGULAR_1),
                    state
                )
            }

            Then("The sequence of published states excludes intermediate states, as these are not published") {
                publishedStates shouldContainExactly listOf(ids.START, ids.REGULAR_1)
            }

            Then(
                """
                    'onBeforeVisit' has only been called on the vertices which have been visited by progressing forwards using NEXT action.
                    This means the start vertex should be excluded, as it was not arrived at by a NEXT action.
                    """.trimIndent()
            ) {
                beforeVisitCalls shouldContainExactly listOf(ids.INTERMEDIATE_1, ids.REGULAR_1)
            }
        }

        When("A second NEXT navigation action is received and the next vertex is an intermediate state again") {
            publishedStates.add(
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(

                "Again, the current state is a regular state, the visited intermediate state is present on the path"
            ) {
                assertTracedPathWithCurrentState(
                    listOf(
                        ids.START, ids.INTERMEDIATE_1, ids.REGULAR_1, ids.INTERMEDIATE_2, ids.REGULAR_2
                    ),
                    state
                )
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

        When("A PREVIOUS action is received and the previous state in the path is an intermediate state") {
            publishedStates.add(
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Previous).vertex.id
            )

            Then("The previous state on the path which is intermediate is not published. Instead, the previous published state is published") {
                assertTracedPathWithCurrentState(
                    listOf(
                        ids.START, ids.INTERMEDIATE_1, ids.REGULAR_1
                    ),
                    state
                )
            }
        }

        When("A NEXT action is received after moving PREVIOUS, where the PREVIOUS action jumped back across an intermediate state") {
            publishedStates.add(
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then("The NEXT action moves the state forward just like the first time it was called on the current vertex, with the intermediate state being advanced through without publishing") {
                assertTracedPathWithCurrentState(
                    listOf(ids.START, ids.INTERMEDIATE_1, ids.REGULAR_1, ids.INTERMEDIATE_2, ids.REGULAR_2),
                    state
                )
            }

            Then("'onBeforeVisit' hook has been invoked again on the forward traversal") {
                beforeVisitCalls shouldContainExactly listOf(ids.INTERMEDIATE_1, ids.REGULAR_1, ids.INTERMEDIATE_2, ids.REGULAR_2, ids.INTERMEDIATE_2, ids.REGULAR_2)
            }

            Then(
                "The traversed intermediate states have been excluded from publishing"
            ) {
                publishedStates shouldContainExactly listOf(ids.START, ids.REGULAR_1, ids.REGULAR_2, ids.REGULAR_1, ids.REGULAR_2)
            }
        }

        When("""
            |A NEXT action is received with the next vertex auto advancing while being the last available vertex. 
            |The END vertex, which is the next and last vertex, with no more vertices left in the graph to traverse, is an intermediate state in the sense that it 'auto-advances'.
            |However, given that there are no more vertices to traverse, there is no state to auto-advance to.
        """.trimMargin()) {

            publishedStates.add(
                dispatcher.dispatchAndAwaitResult(GraphStateMachineAction.Next).vertex.id
            )

            Then(
                 ("Critically, the GSM publishes the END vertex state which is out of bounds. The library user can choose what the significance this holds through usage." +
                         "The significance of the result here depends on the library users interpretation of 'out of bounds' states." +
                         "IF YOU DO NOT INTERPRET OUT OF BOUNDS AS A NULL REPRESENTATION - then: The END vertex, which is the next and last vertex, is not strictly an intermediate state since it ends up being published. " +
                          "For a state to be an 'intermediate-state' it has to be skippable currently. In this situation, although calling auto-advance, the state is not skippable as there are no states to traversal to left" +
                         "IF YOU DO INTERPRET THE OUT OF BOUNDS AS A NULL REPRESENTATION  - then: The END vertex, which is the next and last vertex, is still an intermediate state since it does not end up being published as an in bounds state. " +
                         "Despite being the last vertex it is skippable, since it can auto-advance out of bounds to be 'null' ")
                    .trimMargin()
            ) {

                state.current.value.isBeyondLast shouldBe true
                state.current.value.vertex.id shouldBe ids.END

                publishedStates shouldContainExactly listOf(
                    ids.START,
                    ids.REGULAR_1,
                    ids.REGULAR_2,
                    ids.REGULAR_1,
                    ids.REGULAR_2,
                    ids.END
                )

            }
        }
    }
})
