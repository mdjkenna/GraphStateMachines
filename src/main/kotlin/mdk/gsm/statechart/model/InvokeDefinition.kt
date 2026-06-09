package mdk.gsm.statechart.model

import mdk.gsm.statechart.StateChartInvokeHook

data class InvokeDefinition<G, E>(
    val invokeId: String,
    val execute: StateChartInvokeHook<G, E>,
    val ordinal: Int
)
