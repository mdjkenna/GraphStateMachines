package mdk.gsm.statechart.runtime

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.CompletableStateChartDispatch
import mdk.gsm.statechart.StateChartDispatch
import mdk.gsm.statechart.StateChartInvokeScope
import mdk.gsm.statechart.model.InvokeDefinition

internal class StateChartInvokeScheduler<V, I, G, E>(
    private val scope: CoroutineScope,
    private val commandChannel: Channel<CompletableStateChartDispatch<V, I, E>>
) where V : Vertex<I> {
    private val jobs = LinkedHashMap<String, Job>()

    fun start(invoke: InvokeDefinition<G, E>, machineContext: G?) {
        cancel(invoke.invokeId)
        jobs[invoke.invokeId] = scope.launch {
            try {
                val invokeScope = StateChartInvokeScope<G, E>(
                    invokeId = invoke.invokeId,
                    machineContext = machineContext,
                    sendEvent = { event: E ->
                        scope.launch { send(StateChartDispatch.Event(event)) }
                    }
                )
                invoke.execute.invoke(invokeScope)
                send(StateChartDispatch.DoneInvoke(invoke.invokeId))
            } catch (throwable: Throwable) {
                send(StateChartDispatch.ErrorInvoke(invoke.invokeId, throwable))
            } finally {
                jobs.remove(invoke.invokeId)
            }
        }
    }

    fun cancel(invokeId: String) {
        jobs.remove(invokeId)?.cancel()
    }

    fun cancelAll() {
        jobs.values.forEach { job -> job.cancel() }
        jobs.clear()
    }

    private suspend fun send(dispatch: StateChartDispatch<E>) {
        runCatching {
            commandChannel.send(
                CompletableStateChartDispatch(
                    dispatch = dispatch,
                    deferred = CompletableDeferred()
                )
            )
        }
    }
}
