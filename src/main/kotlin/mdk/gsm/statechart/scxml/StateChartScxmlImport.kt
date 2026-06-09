@file:Suppress("unused")

package mdk.gsm.statechart.scxml

import java.io.StringReader
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.coroutines.CoroutineScope
import mdk.gsm.graph.Vertex
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.statechart.StateChart
import mdk.gsm.statechart.StateChartDispatcherConfig
import mdk.gsm.statechart.StateChartRuntimeConfig
import mdk.gsm.statechart.builder.RawAction
import mdk.gsm.statechart.builder.RawExclusiveGraph
import mdk.gsm.statechart.builder.RawHistory
import mdk.gsm.statechart.builder.RawRegion
import mdk.gsm.statechart.builder.RawState
import mdk.gsm.statechart.builder.RawStateChart
import mdk.gsm.statechart.builder.RawStateKind
import mdk.gsm.statechart.builder.RawTransition
import mdk.gsm.statechart.builder.StateChartCompiler
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.HistoryType
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TransitionType
import mdk.gsm.statechart.model.TriggerDefinition
import mdk.gsm.statechart.runtime.StateChartDispatcherImplementation
import mdk.gsm.statechart.runtime.StateChartImplementation
import mdk.gsm.statechart.runtime.StateChartInvokeScheduler
import mdk.gsm.statechart.runtime.StateChartRuntime
import mdk.gsm.statechart.runtime.StateChartTimerScheduler
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource

data class ScxmlVertex(override val id: String) : Vertex<String>

fun buildStateChartFromScxml(
    scxml: String,
    scope: CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: StateChartDispatcherConfig<String> = StateChartDispatcherConfig(),
    runtimeConfig: StateChartRuntimeConfig = StateChartRuntimeConfig()
): StateChart<ScxmlVertex, String, Nothing, String> {
    val document = documentBuilderFactory().newDocumentBuilder().parse(InputSource(StringReader(scxml)))
    val rootElement = document.documentElement
    check(rootElement.tag() == "scxml") {
        "SCXML document root must be <scxml>."
    }

    val rawChart = RawStateChart<ScxmlVertex, String, Nothing, String>()
    val importer = ScxmlImporter(rawChart)
    importer.importRoot(rootElement)

    val definition = StateChartCompiler(rawChart).compile()
    val commandChannel = dispatcherConfig.toChannel<ScxmlVertex, String>()
    val timerScheduler = StateChartTimerScheduler<ScxmlVertex, String, String>(scope, commandChannel)
    val invokeScheduler = StateChartInvokeScheduler<ScxmlVertex, String, Nothing, String>(scope, commandChannel)
    val runtime = StateChartRuntime(
        definition = definition,
        machineContext = null,
        runtimeConfig = runtimeConfig,
        scheduleTimer = timerScheduler::schedule,
        cancelTimer = timerScheduler::cancel,
        cancelAllTimers = timerScheduler::cancelAll,
        startInvoke = invokeScheduler::start,
        cancelInvoke = invokeScheduler::cancel,
        cancelAllInvokes = invokeScheduler::cancelAll
    )
    val dispatcher = StateChartDispatcherImplementation.create(
        runtime = runtime,
        singleThreadedScope = scope,
        commandChannel = commandChannel,
        timerScheduler = timerScheduler,
        invokeScheduler = invokeScheduler
    )

    return StateChartImplementation(
        definition = definition,
        runtime = runtime,
        stateChartDispatcher = dispatcher
    )
}

private class ScxmlImporter(
    private val rawChart: RawStateChart<ScxmlVertex, String, Nothing, String>
) {
    fun importRoot(root: Element) {
        importExclusiveGraph(root, rawChart.root)
        root.attributeOrNull("initial")?.let { initialId ->
            rawChart.root.initial = EntryTarget.State(initialId)
        }
    }

    private fun importExclusiveGraph(ownerElement: Element, graph: RawExclusiveGraph<ScxmlVertex, String, Nothing, String>) {
        ownerElement.attributeOrNull("initial")?.let { initialId ->
            graph.initial = EntryTarget.State(initialId)
        }

        ownerElement.childElements().forEach { child ->
            when (child.tag()) {
                "initial" -> readInitial(child)?.let { graph.initial = it }
                "state" -> graph.children.add(importState(child, RawStateKind.State))
                "parallel" -> graph.children.add(importParallel(child))
                "final" -> graph.children.add(importState(child, RawStateKind.Final))
            }
        }
    }

    private fun importState(element: Element, kind: RawStateKind): RawState<ScxmlVertex, String, Nothing, String> {
        val id = element.requiredId()
        val state = RawState<ScxmlVertex, String, Nothing, String>(
            vertex = ScxmlVertex(id),
            kind = kind,
            declarationOrdinal = rawChart.nextOrdinal()
        )

        element.childElements().forEach { child ->
            when (child.tag()) {
                "transition" -> state.transitions.add(readTransition(id, child))
                "onentry" -> Unit
                "onexit" -> Unit
                "history" -> state.histories.add(readHistory(id, child))
                "initial" -> readInitial(child)?.let { state.childGraph.initial = it }
                "state" -> state.childGraph.children.add(importState(child, RawStateKind.State))
                "parallel" -> state.childGraph.children.add(importParallel(child))
                "final" -> state.childGraph.children.add(importState(child, RawStateKind.Final))
            }
        }

        element.attributeOrNull("initial")?.let { initialId ->
            state.childGraph.initial = EntryTarget.State(initialId)
        }

        return state
    }

    private fun importParallel(element: Element): RawState<ScxmlVertex, String, Nothing, String> {
        val id = element.requiredId()
        val state = RawState<ScxmlVertex, String, Nothing, String>(
            vertex = ScxmlVertex(id),
            kind = RawStateKind.Parallel,
            declarationOrdinal = rawChart.nextOrdinal()
        )

        element.childElements().forEach { child ->
            when (child.tag()) {
                "transition" -> state.transitions.add(readTransition(id, child))
                "history" -> state.histories.add(readHistory(id, child))
                "state",
                "parallel",
                "final" -> state.regions.add(importParallelRegion(child))
            }
        }

        return state
    }

    private fun importParallelRegion(element: Element): RawRegion<ScxmlVertex, String, Nothing, String> {
        val regionName = element.attributeOrNull("id") ?: "region${rawChart.nextOrdinal()}"
        val region = RawRegion<ScxmlVertex, String, Nothing, String>(
            name = regionName,
            declarationOrdinal = rawChart.nextOrdinal()
        )

        val nestedStateChildren = element.childElements().filter { child ->
            child.tag() in setOf("state", "parallel", "final")
        }

        if (nestedStateChildren.isEmpty()) {
            val state = when (element.tag()) {
                "parallel" -> importParallel(element)
                "final" -> importState(element, RawStateKind.Final)
                else -> importState(element, RawStateKind.State)
            }
            region.graph.children.add(state)
            region.graph.initial = EntryTarget.State(state.vertex.id)
        } else {
            importExclusiveGraph(element, region.graph)
        }

        return region
    }

    private fun readInitial(element: Element): EntryTarget<String>? {
        val transition = element.childElements().firstOrNull { it.tag() == "transition" }
            ?: return null
        val target = transition.attributeOrNull("target")
            ?: return null
        return EntryTarget.State(target.splitTargetIds().first())
    }

    private fun readHistory(ownerStateId: String, element: Element): RawHistory<ScxmlVertex, String> {
        val id = element.attributeOrNull("id") ?: "${ownerStateId}.history.${rawChart.nextOrdinal()}"
        val type = when (element.attributeOrNull("type")) {
            "shallow" -> HistoryType.Shallow
            else -> HistoryType.Deep
        }
        val defaultTarget = element.childElements()
            .firstOrNull { it.tag() == "transition" }
            ?.attributeOrNull("target")
            ?.splitTargetIds()
            ?.firstOrNull()
            ?.let { EntryTarget.State(it) }

        return RawHistory(
            vertex = ScxmlVertex(id),
            ownerStateId = ownerStateId,
            type = type,
            defaultTarget = defaultTarget,
            declarationOrdinal = rawChart.nextOrdinal()
        )
    }

    private fun readTransition(sourceId: String, element: Element): RawTransition<ScxmlVertex, String, Nothing, String> {
        val trigger = element.attributeOrNull("event")?.let { event ->
            TriggerDefinition.Exact(event)
        } ?: TriggerDefinition.Eventless
        val targetIds = element.attributeOrNull("target")?.splitTargetIds().orEmpty()
        val target = when (targetIds.size) {
            0 -> TargetSpec.None
            1 -> TargetSpec.Single(targetIds.single())
            else -> TargetSpec.Multiple(targetIds)
        }

        return RawTransition(
            sourceId = sourceId,
            trigger = trigger,
            target = target,
            guards = emptyList(),
            actions = emptyList(),
            type = TransitionType.External,
            order = 0,
            declarationOrdinal = rawChart.nextOrdinal()
        )
    }
}

private fun documentBuilderFactory(): DocumentBuilderFactory {
    return DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
        runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
    }
}

private fun Element.requiredId(): String {
    return attributeOrNull("id") ?: error("SCXML <${tag()}> element is missing an id.")
}

private fun Element.attributeOrNull(name: String): String? {
    val value = getAttribute(name)
    return value.takeIf { it.isNotBlank() }
}

private fun Element.childElements(): List<Element> {
    val result = ArrayList<Element>()
    val nodes = childNodes
    for (index in 0 until nodes.length) {
        val node = nodes.item(index)
        if (node.nodeType == Node.ELEMENT_NODE) {
            result.add(node as Element)
        }
    }
    return result
}

private fun Element.tag(): String {
    return localName ?: tagName
}

private fun String.splitTargetIds(): List<String> {
    return trim().split(Regex("\\s+")).filter { it.isNotBlank() }
}
