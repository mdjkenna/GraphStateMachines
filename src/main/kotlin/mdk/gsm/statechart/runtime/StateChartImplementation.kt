package mdk.gsm.statechart.runtime

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.StateChart
import mdk.gsm.statechart.StateChartDispatcher
import mdk.gsm.statechart.StateChartState
import mdk.gsm.statechart.StateChartStateReader
import mdk.gsm.statechart.model.StateChartDefinition

internal class StateChartImplementation<V, I, G, E>(
    override val definition: StateChartDefinition<V, I, G, E>,
    private val runtime: StateChartRuntime<V, I, G, E>,
    private val stateChartDispatcher: StateChartDispatcher<V, I, G, E>
) : StateChart<V, I, G, E>, StateChartStateReader<V, I> where V : Vertex<I> {

    override val stateReader: StateChartStateReader<V, I>
        get() = this

    override val dispatcher: StateChartDispatcher<V, I, G, E>
        get() = stateChartDispatcher

    override val current: StateFlow<StateChartState<V, I>>
        get() = runtime.stateOut

    override fun launchDispatch(event: E) {
        stateChartDispatcher.launchDispatch(event)
    }

    override fun launchReset() {
        stateChartDispatcher.launchReset()
    }

    override fun launchTimer(timerId: String) {
        stateChartDispatcher.launchTimer(timerId)
    }

    override fun launchDoneInvoke(invokeId: String) {
        stateChartDispatcher.launchDoneInvoke(invokeId)
    }

    override fun launchErrorInvoke(invokeId: String, cause: Throwable?) {
        stateChartDispatcher.launchErrorInvoke(invokeId, cause)
    }

    override fun scheduleTimer(timerId: String, delayMillis: Long) {
        stateChartDispatcher.scheduleTimer(timerId, delayMillis)
    }

    override fun cancelTimer(timerId: String) {
        stateChartDispatcher.cancelTimer(timerId)
    }

    override suspend fun dispatch(event: E) {
        stateChartDispatcher.dispatch(event)
    }

    override suspend fun dispatchReset() {
        stateChartDispatcher.dispatchReset()
    }

    override suspend fun dispatchTimer(timerId: String) {
        stateChartDispatcher.dispatchTimer(timerId)
    }

    override suspend fun dispatchDoneInvoke(invokeId: String) {
        stateChartDispatcher.dispatchDoneInvoke(invokeId)
    }

    override suspend fun dispatchErrorInvoke(invokeId: String, cause: Throwable?) {
        stateChartDispatcher.dispatchErrorInvoke(invokeId, cause)
    }

    override suspend fun dispatchAndAwaitResult(event: E): StateChartState<V, I> {
        return stateChartDispatcher.dispatchAndAwaitResult(event)
    }

    override suspend fun timerAndAwaitResult(timerId: String): StateChartState<V, I> {
        return stateChartDispatcher.timerAndAwaitResult(timerId)
    }

    override suspend fun doneInvokeAndAwaitResult(invokeId: String): StateChartState<V, I> {
        return stateChartDispatcher.doneInvokeAndAwaitResult(invokeId)
    }

    override suspend fun errorInvokeAndAwaitResult(invokeId: String, cause: Throwable?): StateChartState<V, I> {
        return stateChartDispatcher.errorInvokeAndAwaitResult(invokeId, cause)
    }

    override suspend fun resetAndAwaitResult(): StateChartState<V, I> {
        return stateChartDispatcher.resetAndAwaitResult()
    }

    override fun tearDown() {
        stateChartDispatcher.tearDown()
    }
}
