package mdk.test.features.state.traverse

import io.kotest.core.spec.style.BehaviorSpec
import mdk.gsm.state.GraphStateMachineAction
import mdk.test.scenarios.GraphScenarios
import mdk.test.utils.AssertionUtils

class TransitionBoundsSpec : BehaviorSpec({
    Given("""
        A state machine with simple 2 vertex graph that has one edge from the first to the second,
        which has been configured to explicitly step into bounds in the builder
    """.trimIndent()) {

        val traverser = GraphScenarios.twoVertexBoundsTraverser()

        When("GSM has just been initialised without having yet received any actions") {

            Then("The state machines current state is the start vertex, which is within bounds, not beyond last and not before first") {
                AssertionUtils.assertBounds(traverser, within = true, beyond = false, before = false)
            }
        }

        When("Dispatching a NEXT action when on the start vertex") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("The state machine moves to the next vertex, with the new state still within bounds") {
                AssertionUtils.assertBounds(traverser, within = true, beyond = false, before = false)
            }
        }

        When("Dispatching a NEXT action when on a vertex with no other states available (a 'dead end')") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("""
                The state machines state updates with the same vertex but is now flagged as not within bounds. 
                The beyond last check is true. The before first check is false.
            """) {

                AssertionUtils.assertBounds(traverser, within = false, beyond = true, before = false)
            }
        }

        When("GSM is not within bounds and beyond last, and a PREVIOUS action is received") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

            Then("""
                GSM explicitly steps back into bounds with a dedicated traversal. 
                It updates with the same vertex but is now flagged as within bounds.
                The beyond last check is false.
            """) {
                AssertionUtils.assertBounds(traverser, within = true, beyond = false, before = false)
                AssertionUtils.assertTracedPathWithCurrentState(listOf(1L, 2L), traverser)
            }
        }

        When("GSM is within bounds on the second vertex, and, two PREVIOUS actions are received") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Previous)

            Then("""
               GSM moves back to the first vertex, then before first.
               It is flagged as before first. Beyond last is false. It is not within bounds.
            """) {
                AssertionUtils.assertBounds(traverser, within = false, beyond = false, before = true)
                AssertionUtils.assertTracedPathWithCurrentState(listOf(1L), traverser)
            }
        }

        When("GSM is not within bounds and is 'beforeFirst' and a NEXT action is received") {
            traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

            Then("""
                GSM explicitly steps forward into bounds with a dedicated traversal. 
                It updates with the same vertex but is now flagged as within bounds.
                The 'beforeFirst' check is now false.
            """) {
                AssertionUtils.assertBounds(traverser, within = true, beyond = false, before = false)
                AssertionUtils.assertTracedPathWithCurrentState(listOf(1L), traverser)
            }

        }
    }
})