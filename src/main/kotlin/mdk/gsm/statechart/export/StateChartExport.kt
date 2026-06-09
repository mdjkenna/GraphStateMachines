@file:Suppress("unused")

package mdk.gsm.statechart.export

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.model.CompoundStateNode
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.FinalStateNode
import mdk.gsm.statechart.model.LocalGraphId
import mdk.gsm.statechart.model.ParallelStateNode
import mdk.gsm.statechart.model.PseudoKind
import mdk.gsm.statechart.model.StateChartDefinition
import mdk.gsm.statechart.model.StateNode
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TriggerDefinition

fun <V, I, G, E> StateChartDefinition<V, I, G, E>.toDot(): String where V : Vertex<I> {
    val builder = StringBuilder()
    builder.appendLine("digraph StateChart {")
    builder.appendLine("  compound=true;")
    builder.appendLine("  node [shape=rounded];")

    statesById.values.sortedBy { it.declarationOrdinal }.forEach { state ->
        val shape = when (state) {
            is CompoundStateNode -> "box"
            is ParallelStateNode -> "component"
            else -> "ellipse"
        }
        builder.appendLine("  ${dotId(state.id)} [label=${dotString(state.id.toString())}, shape=$shape];")
    }

    finalStatesById.values.sortedBy { it.declarationOrdinal }.forEach { state ->
        builder.appendLine("  ${dotId(state.id)} [label=${dotString(state.id.toString())}, shape=doublecircle];")
    }

    historyMarkersById.values.sortedBy { it.declarationOrdinal }.forEach { marker ->
        builder.appendLine("  ${dotId(marker.id)} [label=${dotString(marker.id.toString())}, shape=Msquare];")
    }

    pseudoNodesById.values.sortedBy { it.declarationOrdinal }.forEach { pseudo ->
        val label = when (pseudo.kind) {
            PseudoKind.Initial -> "initial:${pseudo.id}"
            PseudoKind.Choice -> "choice:${pseudo.id}"
            PseudoKind.Junction -> "junction:${pseudo.id}"
            PseudoKind.Fork -> "fork:${pseudo.id}"
            PseudoKind.Join -> "join:${pseudo.id}"
        }
        builder.appendLine("  ${dotId(pseudo.id)} [label=${dotString(label)}, shape=diamond];")
    }

    localGraphsById.values.sortedBy { it.declarationOrdinal }.forEach { graph ->
        val initialId = "__initial_${graph.id.value}"
        builder.appendLine("  $initialId [label=\"\", shape=point];")
        builder.appendLine("  $initialId -> ${dotId(entryTargetId(graph.initial))} [label=\"initial\"];")
    }

    transitionsById.values.sortedBy { it.declarationOrdinal }.forEach { transition ->
        targetIds(transition.target).ifEmpty { listOf(null) }.forEach { targetId ->
            val target = targetId?.let { dotId(it) } ?: dotId("__targetless_${transition.id.value}")
            if (targetId == null) {
                builder.appendLine("  $target [label=\"\", shape=point];")
            }
            builder.appendLine(
                "  ${dotId(transition.sourceId)} -> $target [label=${dotString(triggerLabel(transition.trigger))}];"
            )
        }
    }

    builder.appendLine("}")
    return builder.toString()
}

fun <V, I, G, E> StateChartDefinition<V, I, G, E>.toScxmlString(
    name: String = "statechart"
): String where V : Vertex<I> {
    val builder = StringBuilder()
    builder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
    builder.appendLine(
        "<scxml xmlns=\"http://www.w3.org/2005/07/scxml\" version=\"1.0\" name=${xmlString(name)} initial=${xmlString(entryTargetId(localGraphsById.getValue(rootGraphId).initial).toString())}>"
    )
    appendGraphAsScxml(builder, rootGraphId, indent = "  ")
    builder.appendLine("</scxml>")
    return builder.toString()
}

private fun <V, I, G, E> StateChartDefinition<V, I, G, E>.appendGraphAsScxml(
    builder: StringBuilder,
    graphId: LocalGraphId,
    indent: String
) where V : Vertex<I> {
    val graph = localGraphsById.getValue(graphId)
    graph.childIds.forEach { childId ->
        finalStatesById[childId]?.let { final ->
            appendFinalAsScxml(builder, final, indent)
            return@forEach
        }

        val state = statesById.getValue(childId)
        appendStateAsScxml(builder, state, indent)
    }
}

private fun <V, I, G, E> StateChartDefinition<V, I, G, E>.appendStateAsScxml(
    builder: StringBuilder,
    state: StateNode<V, I, G, E>,
    indent: String
) where V : Vertex<I> {
    when (state) {
        is CompoundStateNode -> {
            val graph = localGraphsById.getValue(state.childGraphId)
            builder.appendLine("$indent<state id=${xmlString(state.id.toString())} initial=${xmlString(entryTargetId(graph.initial).toString())}>")
            appendTransitionsAsScxml(builder, state.id, "$indent  ")
            appendGraphAsScxml(builder, state.childGraphId, "$indent  ")
            builder.appendLine("$indent</state>")
        }

        is ParallelStateNode -> {
            builder.appendLine("$indent<parallel id=${xmlString(state.id.toString())}>")
            appendTransitionsAsScxml(builder, state.id, "$indent  ")
            state.regionIds.forEach { regionId ->
                val region = regionsById.getValue(regionId)
                builder.appendLine("$indent  <state id=${xmlString(region.id.name)} initial=${xmlString(entryTargetId(localGraphsById.getValue(region.graphId).initial).toString())}>")
                appendGraphAsScxml(builder, region.graphId, "$indent    ")
                builder.appendLine("$indent  </state>")
            }
            builder.appendLine("$indent</parallel>")
        }

        else -> {
            builder.appendLine("$indent<state id=${xmlString(state.id.toString())}>")
            appendTransitionsAsScxml(builder, state.id, "$indent  ")
            builder.appendLine("$indent</state>")
        }
    }
}

private fun <V, I, G, E> StateChartDefinition<V, I, G, E>.appendFinalAsScxml(
    builder: StringBuilder,
    state: FinalStateNode<V, I, G, E>,
    indent: String
) where V : Vertex<I> {
    builder.appendLine("$indent<final id=${xmlString(state.id.toString())}/>")
}

private fun <V, I, G, E> StateChartDefinition<V, I, G, E>.appendTransitionsAsScxml(
    builder: StringBuilder,
    sourceId: I,
    indent: String
) where V : Vertex<I> {
    transitionsBySourceId[sourceId].orEmpty().forEach { transitionId ->
        val transition = transitionsById.getValue(transitionId)
        val targetIds = targetIds(transition.target)
        val targetAttribute = if (targetIds.isEmpty()) {
            ""
        } else {
            " target=${xmlString(targetIds.joinToString(" ") { it.toString() })}"
        }
        val eventAttribute = when (val trigger = transition.trigger) {
            is TriggerDefinition.Exact -> " event=${xmlString(trigger.event.toString())}"
            is TriggerDefinition.DoneState -> " event=${xmlString("done.state.${trigger.stateId}")}"
            is TriggerDefinition.DoneInvoke -> " event=${xmlString("done.invoke.${trigger.invokeId}")}"
            is TriggerDefinition.ErrorInvoke -> " event=${xmlString("error.invoke.${trigger.invokeId}")}"
            is TriggerDefinition.Timer -> " event=${xmlString(trigger.timerId)}"
            TriggerDefinition.Eventless,
            is TriggerDefinition.Predicate -> ""
        }
        builder.appendLine("$indent<transition$eventAttribute$targetAttribute/>")
    }
}

private fun <I> targetIds(target: TargetSpec<I>): List<I> {
    return when (target) {
        TargetSpec.None -> emptyList()
        is TargetSpec.Single -> listOf(target.id)
        is TargetSpec.Multiple -> target.ids
    }
}

private fun <I> entryTargetId(entryTarget: EntryTarget<I>): I {
    return when (entryTarget) {
        is EntryTarget.State -> entryTarget.stateId
        is EntryTarget.History -> entryTarget.historyId
        is EntryTarget.Pseudo -> entryTarget.pseudoId
    }
}

private fun <E, I> triggerLabel(trigger: TriggerDefinition<E, I>): String {
    return when (trigger) {
        is TriggerDefinition.Exact -> trigger.event.toString()
        is TriggerDefinition.Predicate -> "predicate"
        TriggerDefinition.Eventless -> "eventless"
        is TriggerDefinition.DoneState -> "done.state.${trigger.stateId}"
        is TriggerDefinition.DoneInvoke -> "done.invoke.${trigger.invokeId}"
        is TriggerDefinition.ErrorInvoke -> "error.invoke.${trigger.invokeId}"
        is TriggerDefinition.Timer -> "timer.${trigger.timerId}"
    }
}

private fun dotId(value: Any?): String {
    return "n_" + value.toString()
        .map { character ->
            if (character.isLetterOrDigit() || character == '_') character else '_'
        }
        .joinToString("")
}

private fun dotString(value: String): String {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
}

private fun xmlString(value: String): String {
    return "\"" + value
        .replace("&", "&amp;")
        .replace("\"", "&quot;")
        .replace("<", "&lt;")
        .replace(">", "&gt;") + "\""
}
