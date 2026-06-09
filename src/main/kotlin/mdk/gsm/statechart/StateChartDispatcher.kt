package mdk.gsm.statechart

import mdk.gsm.graph.Vertex

interface StateChartDispatcher<V, I, G, E> where V : Vertex<I> {

    fun launchDispatch(event: E)

    fun launchReset()

    fun launchTimer(timerId: String)

    fun launchDoneInvoke(invokeId: String)

    fun launchErrorInvoke(invokeId: String, cause: Throwable? = null)

    fun scheduleTimer(timerId: String, delayMillis: Long)

    fun cancelTimer(timerId: String)

    suspend fun dispatch(event: E)

    suspend fun dispatchReset()

    suspend fun dispatchTimer(timerId: String)

    suspend fun dispatchDoneInvoke(invokeId: String)

    suspend fun dispatchErrorInvoke(invokeId: String, cause: Throwable? = null)

    suspend fun dispatchAndAwaitResult(event: E): StateChartState<V, I>

    suspend fun timerAndAwaitResult(timerId: String): StateChartState<V, I>

    suspend fun doneInvokeAndAwaitResult(invokeId: String): StateChartState<V, I>

    suspend fun errorInvokeAndAwaitResult(invokeId: String, cause: Throwable? = null): StateChartState<V, I>

    suspend fun resetAndAwaitResult(): StateChartState<V, I>

    fun tearDown()
}
