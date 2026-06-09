package mdk.gsm.statechart.validation

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.model.CompoundStateNode
import mdk.gsm.statechart.model.LocalGraphId
import mdk.gsm.statechart.model.ParallelStateNode
import mdk.gsm.statechart.model.StateChartDefinition
import mdk.gsm.statechart.runtime.ActiveConfiguration

internal fun <V, I, G, E> assertLegalConfiguration(
    definition: StateChartDefinition<V, I, G, E>,
    active: ActiveConfiguration<I>
) where V : Vertex<I> {
    assertGraphHasOneActiveChild(definition, active, definition.rootGraphId)

    active.snapshotIds().forEach { stateId ->
        check(definition.statesById.containsKey(stateId) || definition.finalStatesById.containsKey(stateId)) {
            "Active statechart id $stateId is not a concrete state or final state."
        }

        val state = definition.statesById[stateId]
            ?: return@forEach

        when (state) {
            is CompoundStateNode -> {
                assertGraphHasOneActiveChild(definition, active, state.childGraphId)
            }

            is ParallelStateNode -> {
                state.regionIds.forEach { regionId ->
                    val region = definition.regionsById.getValue(regionId)
                    assertGraphHasOneActiveChild(definition, active, region.graphId)
                }
            }

            else -> Unit
        }
    }
}

private fun <V, I, G, E> assertGraphHasOneActiveChild(
    definition: StateChartDefinition<V, I, G, E>,
    active: ActiveConfiguration<I>,
    graphId: LocalGraphId
) where V : Vertex<I> {
    val graph = definition.localGraphsById.getValue(graphId)
    val activeChildCount = graph.childIds.count { childId ->
        active.isActive(childId)
    }

    check(activeChildCount == 1) {
        "Local graph $graphId must have exactly one active child, found $activeChildCount."
    }
}
