package mdk.gsm.statechart

import kotlinx.coroutines.flow.StateFlow
import mdk.gsm.graph.Vertex

interface StateChartStateReader<V, I> where V : Vertex<I> {
    val current: StateFlow<StateChartState<V, I>>
}
