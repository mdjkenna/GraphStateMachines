package mdk.gsm.statechart.runtime

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.ActiveAtomicState
import mdk.gsm.statechart.ActiveFinalState
import mdk.gsm.statechart.ActiveOrState
import mdk.gsm.statechart.ActiveParallelState
import mdk.gsm.statechart.ActiveRegion
import mdk.gsm.statechart.ActiveStateNode
import mdk.gsm.statechart.ActiveStateTree
import mdk.gsm.statechart.immutableStateChartList
import mdk.gsm.statechart.model.CompoundStateNode
import mdk.gsm.statechart.model.LocalGraphId
import mdk.gsm.statechart.model.ParallelStateNode
import mdk.gsm.statechart.model.StateChartDefinition

internal class ActiveTreeFactory<V, I, G, E>(
    private val definition: StateChartDefinition<V, I, G, E>,
    private val active: ActiveConfiguration<I>
) where V : Vertex<I> {

    fun build(): ActiveStateTree<V, I> {
        val rootGraph = definition.localGraphsById.getValue(definition.rootGraphId)
        val rootChildId = activeChildOf(rootGraph.id)
        return ActiveStateTree(root = buildNode(rootChildId))
    }

    private fun buildNode(stateId: I): ActiveStateNode<V, I> {
        definition.finalStatesById[stateId]?.let { final ->
            return ActiveFinalState(final.vertex)
        }

        val state = definition.statesById.getValue(stateId)
        return when (state) {
            is CompoundStateNode -> {
                ActiveOrState(
                    vertex = state.vertex,
                    activeChild = buildNode(activeChildOf(state.childGraphId))
                )
            }

            is ParallelStateNode -> {
                val regions = state.regionIds.map { regionId ->
                    val region = definition.regionsById.getValue(regionId)
                    ActiveRegion(
                        name = region.id.name,
                        activeChild = buildNode(activeChildOf(region.graphId))
                    )
                }

                ActiveParallelState(
                    vertex = state.vertex,
                    activeRegions = immutableStateChartList(regions)
                )
            }

            else -> ActiveAtomicState(state.vertex)
        }
    }

    private fun activeChildOf(graphId: LocalGraphId): I {
        val graph = definition.localGraphsById.getValue(graphId)
        val activeChildren = graph.childIds.filter { childId ->
            active.isActive(childId)
        }

        check(activeChildren.size == 1) {
            "Local graph $graphId must have exactly one active child, found ${activeChildren.size}."
        }

        return activeChildren.first()
    }
}
