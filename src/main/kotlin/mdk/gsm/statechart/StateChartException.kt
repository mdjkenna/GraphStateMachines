package mdk.gsm.statechart

sealed class StateChartException(message: String) : IllegalStateException(message) {
    class DispatchUndelivered : StateChartException("The statechart dispatch was not delivered.")

    class MacrostepLimitExceeded(limit: Int) : StateChartException(
        "The statechart exceeded the macrostep limit of $limit."
    )

    class UnresolvedPseudostate(id: Any?) : StateChartException(
        "Pseudostate $id did not resolve to a concrete statechart target."
    )

    class InvalidTransitionTarget(message: String) : StateChartException(message)
}
