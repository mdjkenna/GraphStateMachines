package mdk.test.features.statechart

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.ActiveParallelState
import mdk.gsm.statechart.activeAtomicStates
import mdk.gsm.statechart.activeStatesDepthFirst
import mdk.gsm.statechart.builder.buildStateChart
import mdk.gsm.statechart.containsStateId
import mdk.gsm.statechart.export.toDot
import mdk.gsm.statechart.export.toScxmlString
import mdk.gsm.statechart.scxml.ScxmlVertex
import mdk.gsm.statechart.scxml.buildStateChartFromScxml
import mdk.test.utils.TestVertex

class StateChartSpec : BehaviorSpec({

    Given("a simple statechart definition") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    target(ChartState.B)
                }
            }

            state(ChartState.B)
        }

        Then("the chart builds a structured definition") {
            chart.definition.statesById.containsKey(ChartState.A.id) shouldBe true
            chart.definition.statesById.containsKey(ChartState.B.id) shouldBe true
            chart.definition.transitionsBySourceId.getValue(ChartState.A.id).size shouldBe 1
        }
    }

    Given("statechart build validation") {
        Then("duplicate ids are rejected") {
            shouldThrow<IllegalStateException> {
                buildStateChart<TestVertex, String, ChartEvent> {
                    val first = TestVertex("same")
                    val second = TestVertex("same")

                    initial(first)
                    state(first)
                    state(second)
                }
            }
        }

        Then("a missing root initial is rejected") {
            shouldThrow<IllegalStateException> {
                buildStateChart<ChartState, String, ChartEvent> {
                    state(ChartState.A)
                    state(ChartState.B)
                }
            }
        }

        Then("a compound state missing an initial is rejected") {
            shouldThrow<IllegalStateException> {
                buildStateChart<ChartState, String, ChartEvent> {
                    initial(ChartState.Parent)

                    state(ChartState.Parent) {
                        state(ChartState.ChildA)
                        state(ChartState.ChildB)
                    }
                }
            }
        }
    }

    Given("root default entry") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)
            state(ChartState.A)
            state(ChartState.B)
        }

        Then("the root initial state is active immediately") {
            chart.current.value.activeTree.containsStateId(ChartState.A.id) shouldBe true
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.A)
        }
    }

    Given("a simple event transition") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    target(ChartState.B)
                }
            }

            state(ChartState.B)
        }

        When("the triggering event is dispatched") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }

            Then("the target state becomes active") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
            }
        }
    }

    Given("multiple transitions to the same target with different events") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    target(ChartState.B)
                }

                transition(on = ChartEvent.Skip) {
                    target(ChartState.B)
                }
            }

            state(ChartState.B)
        }

        Then("both transitions are preserved and selectable") {
            chart.definition.transitionsBySourceId.getValue(ChartState.A.id).size shouldBe 2

            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Skip)
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)

                chart.resetAndAwaitResult()
                chart.dispatchAndAwaitResult(ChartEvent.Go)
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
            }
        }
    }

    Given("a nested OR state") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)
                state(ChartState.ChildA)
                state(ChartState.ChildB)
            }
        }

        Then("default entry activates the parent and initial child") {
            chart.current.value.activeTree.activeStatesDepthFirst() shouldBe listOf(
                ChartState.Parent,
                ChartState.ChildA
            )
        }
    }

    Given("a parent-scoped transition") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)

                transition(on = ChartEvent.Cancel) {
                    target(ChartState.B)
                }

                state(ChartState.ChildA)
                state(ChartState.ChildB)
            }

            state(ChartState.B)
        }

        When("the event is dispatched while a child is active") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Cancel)
            }

            Then("the parent transition exits the compound state") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
            }
        }
    }

    Given("a cross-boundary transition") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)

                state(ChartState.ChildA) {
                    transition(on = ChartEvent.Go) {
                        target(ChartState.B)
                    }
                }

                state(ChartState.ChildB)
            }

            state(ChartState.B)
        }

        When("a nested child targets a top-level sibling") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }

            Then("the runtime performs the global transition") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
            }
        }
    }

    Given("a parallel state") {
        val chart = buildParallelChart()

        Then("entry activates every region") {
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(
                ChartState.Spinning,
                ChartState.Unlocked,
                ChartState.Balanced
            )
        }

        When("one event is compatible with transitions in multiple regions") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.UnbalancedLoad)
            }

            Then("both region transitions are applied in one stable publication") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(
                    ChartState.Redistributing,
                    ChartState.Unlocked,
                    ChartState.Unbalanced
                )
            }
        }
    }

    Given("statechart publication") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Operational)

            parallel(ChartState.Operational) {
                region("cycle") {
                    initial(ChartState.Spinning)

                    state(ChartState.Spinning) {
                        transition(on = ChartEvent.Go) {
                            target(ChartState.Redistributing)
                            action {
                                raise(ChartEvent.Lock)
                            }
                        }
                    }

                    state(ChartState.Redistributing)
                }

                region("door") {
                    initial(ChartState.Unlocked)

                    state(ChartState.Unlocked) {
                        transition(on = ChartEvent.Lock) {
                            target(ChartState.Locked)
                        }
                    }

                    state(ChartState.Locked)
                }
            }
        }

        When("an action raises an internal event") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }

            Then("observers see the macrostep-stable tree") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(
                    ChartState.Redistributing,
                    ChartState.Locked
                )
            }

            Then("published region lists are immutable") {
                val root = chart.current.value.activeTree.root as ActiveParallelState
                shouldThrow<UnsupportedOperationException> {
                    @Suppress("UNCHECKED_CAST")
                    (root.activeRegions as MutableList<Any?>).add(null)
                }
            }
        }
    }


    Given("an eventless transition") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition {
                    target(ChartState.B)
                }
            }

            state(ChartState.B)
        }

        Then("initial macrostep stabilization applies it before publication") {
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
        }
    }

    Given("a completed compound state") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)

                onDone {
                    target(ChartState.B)
                }

                state(ChartState.ChildA) {
                    transition(on = ChartEvent.Finish) {
                        target(ChartState.Done)
                    }
                }

                final(ChartState.Done)
            }

            state(ChartState.B)
        }

        When("a child reaches final") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Finish)
            }

            Then("a done event drives the parent transition in the same macrostep") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
            }
        }
    }

    Given("a compound state with deep history") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)

                hStar(ChartState.History) {
                    default {
                        target(ChartState.ChildA)
                    }
                }

                transition(on = ChartEvent.Pause) {
                    target(ChartState.Paused)
                }

                state(ChartState.ChildA) {
                    transition(on = ChartEvent.Go) {
                        target(ChartState.ChildB)
                    }
                }

                state(ChartState.ChildB)
            }

            state(ChartState.Paused) {
                transition(on = ChartEvent.Resume) {
                    targetHistory(ChartState.History)
                }
            }
        }

        When("the compound state is exited and re-entered through history") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
                chart.dispatchAndAwaitResult(ChartEvent.Pause)
                chart.dispatchAndAwaitResult(ChartEvent.Resume)
            }

            Then("the previous descendant configuration is restored") {
                chart.current.value.activeTree.activeStatesDepthFirst() shouldBe listOf(
                    ChartState.Parent,
                    ChartState.ChildB
                )
            }
        }
    }

    Given("an internal parent transition") {
        var parentExits = 0
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Parent)

            state(ChartState.Parent) {
                initial(ChartState.ChildA)
                onExit { parentExits++ }

                transition(on = ChartEvent.Go) {
                    internal()
                    target(ChartState.ChildB)
                }

                state(ChartState.ChildA)
                state(ChartState.ChildB)
            }
        }

        When("the parent internally targets a descendant") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }

            Then("the child changes without exiting the parent") {
                parentExits shouldBe 0
                chart.current.value.activeTree.activeStatesDepthFirst() shouldBe listOf(
                    ChartState.Parent,
                    ChartState.ChildB
                )
            }
        }
    }

    Given("a choice pseudostate") {
        var chooseFast = false
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    targetPseudo(ChartState.Choice)
                }
            }

            choice(ChartState.Choice) {
                transition {
                    guard { chooseFast }
                    target(ChartState.Fast)
                }

                otherwise {
                    target(ChartState.Slow)
                }
            }

            state(ChartState.Fast)
            state(ChartState.Slow)
        }

        Then("unguarded fallback routing is used when guards fail") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Slow)
        }

        Then("conditional routing is used when a choice guard passes") {
            runBlocking {
                chart.resetAndAwaitResult()
                chooseFast = true
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Fast)
        }
    }

    Given("timer and invoke trigger dispatch") {
        val timerChart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                after("timeout") {
                    target(ChartState.Timeout)
                }
            }

            state(ChartState.Timeout)
        }

        val invokeChart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                onDoneInvoke("job") {
                    target(ChartState.InvokeDone)
                }

                onErrorInvoke("job") {
                    target(ChartState.InvokeError)
                }
            }

            state(ChartState.InvokeDone)
            state(ChartState.InvokeError)
        }

        Then("timer dispatch selects timer-triggered transitions") {
            runBlocking {
                timerChart.timerAndAwaitResult("timeout")
            }
            timerChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Timeout)
        }

        Then("invoke dispatch selects invoke-triggered transitions") {
            runBlocking {
                invokeChart.doneInvokeAndAwaitResult("job")
            }
            invokeChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.InvokeDone)
        }
    }

    Given("an action raises a timer event") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    noTarget()
                    action {
                        raiseTimer("timeout")
                    }
                }

                after("timeout") {
                    target(ChartState.Timeout)
                }
            }

            state(ChartState.Timeout)
        }

        When("the action runs") {
            runBlocking {
                chart.dispatchAndAwaitResult(ChartEvent.Go)
            }

            Then("the raised runtime event is processed before publication") {
                chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Timeout)
            }
        }
    }


    Given("fork and join pseudostates") {
        val forkChart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    targetPseudo(ChartState.Fork)
                }
            }

            fork(ChartState.Fork) {
                transition { target(ChartState.Spinning) }
                transition { target(ChartState.Locked) }
            }

            parallel(ChartState.Operational) {
                region("cycle") {
                    initial(ChartState.Redistributing)
                    state(ChartState.Spinning)
                    state(ChartState.Redistributing)
                }

                region("door") {
                    initial(ChartState.Unlocked)
                    state(ChartState.Unlocked)
                    state(ChartState.Locked)
                }
            }
        }

        val joinChart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.Operational)

            parallel(ChartState.Operational) {
                region("cycle") {
                    initial(ChartState.Spinning)
                    state(ChartState.Spinning) {
                        transition(on = ChartEvent.Go) {
                            targetPseudo(ChartState.Join)
                        }
                    }
                }

                region("door") {
                    initial(ChartState.Unlocked)
                    state(ChartState.Unlocked) {
                        transition(on = ChartEvent.Go) {
                            targetPseudo(ChartState.Join)
                        }
                    }
                }
            }

            join(ChartState.Join) {
                transition {
                    target(ChartState.B)
                }
            }

            state(ChartState.B)
        }

        Then("fork routes to multiple orthogonal targets") {
            runBlocking {
                forkChart.dispatchAndAwaitResult(ChartEvent.Go)
            }
            forkChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(
                ChartState.Spinning,
                ChartState.Locked
            )
        }

        Then("join routes only when all incoming sources are eligible") {
            runBlocking {
                joinChart.dispatchAndAwaitResult(ChartEvent.Go)
            }
            joinChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.B)
        }
    }



    Given("a lifecycle invoke") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                invoke("job") {
                    // returning from the invoke sends done.invoke.job
                }

                onDoneInvoke("job") {
                    target(ChartState.InvokeDone)
                }
            }

            state(ChartState.InvokeDone)
        }

        Then("normal invoke completion raises done.invoke") {
            runBlocking {
                delay(50L)
            }
            chart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.InvokeDone)
            chart.tearDown()
        }
    }

    Given("lifecycle-managed after timers") {
        val firingChart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)

            state(ChartState.A) {
                after(delayMillis = 25L, timerId = "auto-timeout") {
                    target(ChartState.Timeout)
                }
            }

            state(ChartState.Timeout)
        }

        Then("a delayed after transition fires after entry") {
            runBlocking {
                delay(80L)
            }
            firingChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Timeout)
            firingChart.tearDown()
        }

        Then("exiting the source state cancels its delayed timer") {
            val cancelChart = buildStateChart<ChartState, String, ChartEvent> {
                initial(ChartState.A)

                state(ChartState.A) {
                    after(delayMillis = 100L, timerId = "cancelled-timeout") {
                        target(ChartState.Timeout)
                    }

                    transition(on = ChartEvent.Cancel) {
                        target(ChartState.Paused)
                    }
                }

                state(ChartState.Timeout)
                state(ChartState.Paused)
            }

            runBlocking {
                cancelChart.dispatchAndAwaitResult(ChartEvent.Cancel)
                delay(150L)
            }
            cancelChart.current.value.activeTree.activeAtomicStates() shouldBe listOf(ChartState.Paused)
            cancelChart.tearDown()
        }
    }

    Given("statechart export and SCXML import") {
        val chart = buildStateChart<ChartState, String, ChartEvent> {
            initial(ChartState.A)
            state(ChartState.A) {
                transition(on = ChartEvent.Go) {
                    target(ChartState.B)
                }
            }
            state(ChartState.B)
        }

        Then("DOT and SCXML exporters produce structural output") {
            chart.definition.toDot().contains("digraph StateChart") shouldBe true
            chart.definition.toScxmlString().contains("<scxml") shouldBe true
        }

        Then("a basic SCXML chart can be imported and executed") {
            val imported = buildStateChartFromScxml(
                """
                <scxml xmlns="http://www.w3.org/2005/07/scxml" initial="A">
                  <state id="A">
                    <transition event="go" target="B"/>
                  </state>
                  <state id="B"/>
                </scxml>
                """.trimIndent()
            )

            runBlocking {
                imported.dispatchAndAwaitResult("go")
            }
            imported.current.value.activeTree.activeAtomicStates() shouldBe listOf(ScxmlVertex("B"))
        }
    }

})

private fun buildParallelChart() = buildStateChart<ChartState, String, ChartEvent> {
    initial(ChartState.Operational)

    parallel(ChartState.Operational) {
        region("cycle") {
            initial(ChartState.Spinning)

            state(ChartState.Spinning) {
                transition(on = ChartEvent.UnbalancedLoad) {
                    target(ChartState.Redistributing)
                }
            }

            state(ChartState.Redistributing)
        }

        region("door") {
            initial(ChartState.Unlocked)
            state(ChartState.Unlocked)
            state(ChartState.Locked)
        }

        region("balance") {
            initial(ChartState.Balanced)

            state(ChartState.Balanced) {
                transition(on = ChartEvent.UnbalancedLoad) {
                    target(ChartState.Unbalanced)
                }
            }

            state(ChartState.Unbalanced)
        }
    }
}

private enum class ChartState(override val id: String) : Vertex<String> {
    A("A"),
    B("B"),
    Parent("Parent"),
    ChildA("ChildA"),
    ChildB("ChildB"),
    Operational("Operational"),
    Spinning("Spinning"),
    Redistributing("Redistributing"),
    Unlocked("Unlocked"),
    Locked("Locked"),
    Balanced("Balanced"),
    Unbalanced("Unbalanced"),
    Done("Done"),
    History("History"),
    Paused("Paused"),
    Choice("Choice"),
    Fast("Fast"),
    Slow("Slow"),
    Timeout("Timeout"),
    InvokeDone("InvokeDone"),
    InvokeError("InvokeError"),
    Fork("Fork"),
    Join("Join")
}

private sealed interface ChartEvent {
    data object Go : ChartEvent
    data object Skip : ChartEvent
    data object Cancel : ChartEvent
    data object UnbalancedLoad : ChartEvent
    data object Lock : ChartEvent
    data object Finish : ChartEvent
    data object Pause : ChartEvent
    data object Resume : ChartEvent
}
