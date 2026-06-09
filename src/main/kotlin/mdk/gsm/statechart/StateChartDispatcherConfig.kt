package mdk.gsm.statechart

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import mdk.gsm.graph.Vertex

data class StateChartDispatcherConfig<E>(
    val capacity: Int = Channel.BUFFERED,
    val onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    val onUndeliveredElement: (StateChartDispatch<E>) -> Unit = {}
) {
    internal fun <V, I> toChannel(): Channel<CompletableStateChartDispatch<V, I, E>>
            where V : Vertex<I> {
        return Channel(
            capacity = capacity,
            onBufferOverflow = onBufferOverflow,
            onUndeliveredElement = { undelivered ->
                undelivered.deferred.completeExceptionally(
                    StateChartException.DispatchUndelivered()
                )
                onUndeliveredElement(undelivered.dispatch)
            }
        )
    }
}
