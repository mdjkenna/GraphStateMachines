package mdk.gsm.statechart.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.CompletableStateChartDispatch
import mdk.gsm.statechart.StateChartDispatch

internal class StateChartTimerScheduler<V, I, E>(
    private val scope: CoroutineScope,
    private val commandChannel: Channel<CompletableStateChartDispatch<V, I, E>>
) where V : Vertex<I> {
    private val jobs = LinkedHashMap<String, Job>()

    fun schedule(timerId: String, delayMillis: Long) {
        cancel(timerId)
        jobs[timerId] = scope.launch {
            delay(delayMillis)
            jobs.remove(timerId)
            runCatching {
                commandChannel.send(
                    CompletableStateChartDispatch(
                        dispatch = StateChartDispatch.Timer(timerId),
                        deferred = CompletableDeferred()
                    )
                )
            }
        }
    }

    fun cancel(timerId: String) {
        jobs.remove(timerId)?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { job -> job.cancel() }
        jobs.clear()
    }
}
