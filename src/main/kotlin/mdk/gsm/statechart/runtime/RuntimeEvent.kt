package mdk.gsm.statechart.runtime

internal sealed interface RuntimeEvent<out E, out I> {
    data object Eventless : RuntimeEvent<Nothing, Nothing>

    data class External<E>(val event: E) : RuntimeEvent<E, Nothing>

    data class Raised<E>(val event: E) : RuntimeEvent<E, Nothing>

    data class DoneState<I>(val stateId: I) : RuntimeEvent<Nothing, I>

    data class DoneInvoke(val invokeId: String) : RuntimeEvent<Nothing, Nothing>

    data class ErrorInvoke(val invokeId: String, val cause: Throwable?) : RuntimeEvent<Nothing, Nothing>

    data class Timer(val timerId: String) : RuntimeEvent<Nothing, Nothing>
}
