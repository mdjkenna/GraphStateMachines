package mdk.gsm.statechart

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.model.StateChartDefinition

interface StateChart<V, I, G, E> :
    StateChartStateReader<V, I>,
    StateChartDispatcher<V, I, G, E>
        where V : Vertex<I> {

    val definition: StateChartDefinition<V, I, G, E>

    val stateReader: StateChartStateReader<V, I>

    val dispatcher: StateChartDispatcher<V, I, G, E>

    operator fun component1(): StateChartStateReader<V, I> = stateReader

    operator fun component2(): StateChartDispatcher<V, I, G, E> = dispatcher
}
