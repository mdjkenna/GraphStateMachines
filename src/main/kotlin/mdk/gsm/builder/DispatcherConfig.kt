package mdk.gsm.builder

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import mdk.gsm.action.CompletableAction
import mdk.gsm.graph.IVertex
import mdk.gsm.state.GraphStateMachineAction

/**
 * Configuration for the dispatcher channel used by traversers and walkers.
 *
 * This class allows customizing the behavior of the internal channel used for action dispatching,
 * which affects how actions are buffered and processed.
 *
 * The properties of the class are used to create a regular coroutines channel.
 * For more information on their significance, see resources on `Channel` in the Kotlin coroutines library.
 *
 * @param A The type of action arguments that can be passed when dispatching actions.
 * @property capacity Controls the buffering behavior of the channel:
 *   - [Channel.UNLIMITED]: The channel has unlimited capacity (bounded only by available memory)
 *   - [Channel.CONFLATED]: Only the most recently sent action is kept, overwriting previous ones
 *   - [Channel.RENDEZVOUS]: No buffering (capacity = 0). Each send suspends until received.
 *   - [Channel.BUFFERED]: Default. Uses a reasonable default capacity (currently 64).
 *   - Positive number: Creates a channel with the exact specified buffer size.
 * @property onBufferOverflow Determines the behavior when the buffer is full:
 *   - [BufferOverflow.SUSPEND]: Default. Suspends the sender until space becomes available.
 *   - [BufferOverflow.DROP_OLDEST]: Drops the oldest action in the buffer to make room.
 *   - [BufferOverflow.DROP_LATEST]: Drops the action being sent if the buffer is full.
 * @property onUndeliveredElement Callback invoked for actions that couldn't be delivered
 *   (e.g., when the channel is closed while actions are still buffered).
 *   Default is an empty handler.
 */
data class DispatcherConfig<A>(
    val capacity: Int = Channel.BUFFERED,
    val onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND,
    val onUndeliveredElement: (GraphStateMachineAction<A>) -> Unit = {}
) {

    companion object {
        internal fun <V, I, A> DispatcherConfig<A>.toChannel() : Channel<CompletableAction<V, I, A>>
        where V : IVertex<I> {
            return Channel<CompletableAction<V, I, A>>(
                capacity = capacity,
                onBufferOverflow = onBufferOverflow,
                onUndeliveredElement = {
                    onUndeliveredElement(it.action)
                }
            )
        }
    }
}
