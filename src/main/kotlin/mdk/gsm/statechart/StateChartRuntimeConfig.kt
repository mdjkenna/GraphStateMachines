package mdk.gsm.statechart

data class StateChartRuntimeConfig(
    val maxMicrostepsPerMacrostep: Int = 10_000
)
