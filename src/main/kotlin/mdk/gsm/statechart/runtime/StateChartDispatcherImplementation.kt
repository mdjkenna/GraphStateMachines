package mdk.gsm.statechart.runtime

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.CompletableStateChartDispatch
import mdk.gsm.statechart.StateChartDispatch
import mdk.gsm.statechart.StateChartDispatcher
import mdk.gsm.statechart.StateChartState

internal class StateChartDispatcherImplementation<V, I, G, E> private constructor(
    private val scope: CoroutineScope,
    private val commandChannel: Channel<CompletableStateChartDispatch<V, I, E>>,
    private val timerScheduler: StateChartTimerScheduler<V, I, E>,
    private val invokeScheduler: StateChartInvokeScheduler<V, I, G, E>
) : StateChartDispatcher<V, I, G, E> where V : Vertex<I> {

    override fun launchDispatch(event: E) {
        scope.launch {
            dispatch(event)
        }
    }

    override fun launchReset() {
        scope.launch {
            dispatchReset()
        }
    }

    override fun launchTimer(timerId: String) {
        scope.launch {
            dispatchTimer(timerId)
        }
    }

    override fun launchDoneInvoke(invokeId: String) {
        scope.launch {
            dispatchDoneInvoke(invokeId)
        }
    }

    override fun launchErrorInvoke(invokeId: String, cause: Throwable?) {
        scope.launch {
            dispatchErrorInvoke(invokeId, cause)
        }
    }

    override fun scheduleTimer(timerId: String, delayMillis: Long) {
        timerScheduler.schedule(timerId, delayMillis)
    }

    override fun cancelTimer(timerId: String) {
        timerScheduler.cancel(timerId)
    }

    override suspend fun dispatch(event: E) {
        commandChannel.send(
            CompletableStateChartDispatch(
                dispatch = StateChartDispatch.Event(event),
                deferred = CompletableDeferred()
            )
        )
    }

    override suspend fun dispatchReset() {
        commandChannel.send(
            CompletableStateChartDispatch(
                dispatch = StateChartDispatch.Reset,
                deferred = CompletableDeferred()
            )
        )
    }

    override suspend fun dispatchTimer(timerId: String) {
        commandChannel.send(
            CompletableStateChartDispatch(
                dispatch = StateChartDispatch.Timer(timerId),
                deferred = CompletableDeferred()
            )
        )
    }

    override suspend fun dispatchDoneInvoke(invokeId: String) {
        commandChannel.send(
            CompletableStateChartDispatch(
                dispatch = StateChartDispatch.DoneInvoke(invokeId),
                deferred = CompletableDeferred()
            )
        )
    }

    override suspend fun dispatchErrorInvoke(invokeId: String, cause: Throwable?) {
        commandChannel.send(
            CompletableStateChartDispatch(
                dispatch = StateChartDispatch.ErrorInvoke(invokeId, cause),
                deferred = CompletableDeferred()
            )
        )
    }

    override suspend fun dispatchAndAwaitResult(event: E): StateChartState<V, I> {
        val dispatch = CompletableStateChartDispatch<V, I, E>(
            dispatch = StateChartDispatch.Event(event),
            deferred = CompletableDeferred()
        )
        commandChannel.send(dispatch)
        return dispatch.deferred.await()
    }

    override suspend fun timerAndAwaitResult(timerId: String): StateChartState<V, I> {
        val dispatch = CompletableStateChartDispatch<V, I, E>(
            dispatch = StateChartDispatch.Timer(timerId),
            deferred = CompletableDeferred()
        )
        commandChannel.send(dispatch)
        return dispatch.deferred.await()
    }

    override suspend fun doneInvokeAndAwaitResult(invokeId: String): StateChartState<V, I> {
        val dispatch = CompletableStateChartDispatch<V, I, E>(
            dispatch = StateChartDispatch.DoneInvoke(invokeId),
            deferred = CompletableDeferred()
        )
        commandChannel.send(dispatch)
        return dispatch.deferred.await()
    }

    override suspend fun errorInvokeAndAwaitResult(invokeId: String, cause: Throwable?): StateChartState<V, I> {
        val dispatch = CompletableStateChartDispatch<V, I, E>(
            dispatch = StateChartDispatch.ErrorInvoke(invokeId, cause),
            deferred = CompletableDeferred()
        )
        commandChannel.send(dispatch)
        return dispatch.deferred.await()
    }

    override suspend fun resetAndAwaitResult(): StateChartState<V, I> {
        val dispatch = CompletableStateChartDispatch<V, I, E>(
            dispatch = StateChartDispatch.Reset,
            deferred = CompletableDeferred()
        )
        commandChannel.send(dispatch)
        return dispatch.deferred.await()
    }

    override fun tearDown() {
        timerScheduler.cancelAll()
        invokeScheduler.cancelAll()
        commandChannel.cancel()
        scope.cancel()
    }

    companion object {
        internal fun <V, I, G, E> create(
            runtime: StateChartRuntime<V, I, G, E>,
            singleThreadedScope: CoroutineScope,
            commandChannel: Channel<CompletableStateChartDispatch<V, I, E>>,
            timerScheduler: StateChartTimerScheduler<V, I, E>,
            invokeScheduler: StateChartInvokeScheduler<V, I, G, E>
        ): StateChartDispatcherImplementation<V, I, G, E> where V : Vertex<I> {
            val dispatcher = StateChartDispatcherImplementation<V, I, G, E>(
                scope = singleThreadedScope,
                commandChannel = commandChannel,
                timerScheduler = timerScheduler,
                invokeScheduler = invokeScheduler
            )

            singleThreadedScope.launch {
                for (command in commandChannel) {
                    try {
                        val state = runtime.dispatch(command.dispatch)
                        command.deferred.complete(state)
                    } catch (throwable: Throwable) {
                        command.deferred.completeExceptionally(throwable)
                        val cancellation = CancellationException("The statechart interpreter failed.")
                        cancellation.initCause(throwable)
                        commandChannel.cancel(cancellation)
                        throw throwable
                    }
                }
            }

            return dispatcher
        }
    }
}
