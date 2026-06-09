package mdk.gsm.statechart.builder

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.immutableStateChartList
import mdk.gsm.statechart.immutableStateChartMap
import mdk.gsm.statechart.immutableStateChartSet
import mdk.gsm.statechart.model.ActionDefinition
import mdk.gsm.statechart.model.AtomicStateNode
import mdk.gsm.statechart.model.CompoundStateNode
import mdk.gsm.statechart.model.ContainmentIndex
import mdk.gsm.statechart.model.DeclarationIndex
import mdk.gsm.statechart.model.EntryIndex
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.FinalStateNode
import mdk.gsm.statechart.model.GuardDefinition
import mdk.gsm.statechart.model.HistoryMarker
import mdk.gsm.statechart.model.InvokeDefinition
import mdk.gsm.statechart.model.LocalGraphId
import mdk.gsm.statechart.model.LocalGraphOwner
import mdk.gsm.statechart.model.LocalStateGraph
import mdk.gsm.statechart.model.ParallelStateNode
import mdk.gsm.statechart.model.ParentLocation
import mdk.gsm.statechart.model.PseudoKind
import mdk.gsm.statechart.model.PseudoNode
import mdk.gsm.statechart.model.RegionDefinition
import mdk.gsm.statechart.model.RegionId
import mdk.gsm.statechart.model.StateChartDefinition
import mdk.gsm.statechart.model.StateNode
import mdk.gsm.statechart.model.StatePath
import mdk.gsm.statechart.model.TargetIndex
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TransitionActionDefinition
import mdk.gsm.statechart.model.TransitionDefinition
import mdk.gsm.statechart.model.TransitionId
import mdk.gsm.statechart.model.TransitionType

internal class StateChartCompiler<V, I, G, E>(
    private val rawChart: RawStateChart<V, I, G, E>
) where V : Vertex<I> {

    private var nextGraphId = 0
    private var nextTransitionId = 0

    private val localGraphsById = LinkedHashMap<LocalGraphId, LocalStateGraph<I>>()
    private val statesById = LinkedHashMap<I, StateNode<V, I, G, E>>()
    private val finalStatesById = LinkedHashMap<I, FinalStateNode<V, I, G, E>>()
    private val historyMarkersById = LinkedHashMap<I, HistoryMarker<V, I>>()
    private val pseudoNodesById = LinkedHashMap<I, PseudoNode<V, I, G, E>>()
    private val regionsById = LinkedHashMap<RegionId<I>, RegionDefinition<I>>()
    private val transitionsById = LinkedHashMap<TransitionId, TransitionDefinition<V, I, G, E>>()
    private val transitionsBySourceId = LinkedHashMap<I, MutableList<TransitionId>>()
    private val invokesByStateId = LinkedHashMap<I, MutableList<InvokeDefinition<G, E>>>()
    private val registeredTargetIds = LinkedHashSet<I>()

    private val parentByStateId = LinkedHashMap<I, ParentLocation<I>>()
    private val owningGraphByChildId = LinkedHashMap<I, LocalGraphId>()
    private val graphByCompoundStateId = LinkedHashMap<I, LocalGraphId>()
    private val graphByRegionId = LinkedHashMap<RegionId<I>, LocalGraphId>()
    private val regionsByParallelStateId = LinkedHashMap<I, MutableList<RegionId<I>>>()

    private val stateOrdinalById = LinkedHashMap<I, Int>()
    private val transitionOrdinalById = LinkedHashMap<TransitionId, Int>()
    private val regionOrdinalById = LinkedHashMap<RegionId<I>, Int>()
    private val localGraphOrdinalById = LinkedHashMap<LocalGraphId, Int>()

    fun compile(): StateChartDefinition<V, I, G, E> {
        val rootGraphId = compileGraph(
            graph = rawChart.root,
            owner = LocalGraphOwner.Root,
            parentLocation = ParentLocation.Root,
            declarationOrdinal = 0
        )

        validateTargets()

        val pathByStateId = buildPaths()
        validateInternalTransitions(pathByStateId)
        val descendantsByStateId = buildDescendants(pathByStateId)

        val targetIndex = TargetIndex(
            states = immutableStateChartMap(statesById),
            finals = immutableStateChartMap(finalStatesById),
            histories = immutableStateChartMap(historyMarkersById),
            pseudos = immutableStateChartMap(pseudoNodesById)
        )

        return StateChartDefinition(
            rootGraphId = rootGraphId,
            localGraphsById = immutableStateChartMap(localGraphsById),
            statesById = immutableStateChartMap(statesById),
            finalStatesById = immutableStateChartMap(finalStatesById),
            historyMarkersById = immutableStateChartMap(historyMarkersById),
            pseudoNodesById = immutableStateChartMap(pseudoNodesById),
            regionsById = immutableStateChartMap(regionsById),
            transitionsById = immutableStateChartMap(transitionsById),
            transitionsBySourceId = immutableStateChartMap(
                transitionsBySourceId.mapValues { (_, transitionIds) ->
                    immutableStateChartList(transitionIds)
                }
            ),
            invokesByStateId = immutableStateChartMap(
                invokesByStateId.mapValues { (_, invokes) ->
                    immutableStateChartList(invokes)
                }
            ),
            targetIndex = targetIndex,
            containmentIndex = ContainmentIndex(
                parentByStateId = immutableStateChartMap(parentByStateId),
                owningGraphByChildId = immutableStateChartMap(owningGraphByChildId),
                pathByStateId = immutableStateChartMap(pathByStateId),
                descendantsByStateId = immutableStateChartMap(descendantsByStateId),
                regionsByParallelStateId = immutableStateChartMap(
                    regionsByParallelStateId.mapValues { (_, regionIds) ->
                        immutableStateChartList(regionIds)
                    }
                )
            ),
            entryIndex = EntryIndex(
                initialTargetByGraphId = immutableStateChartMap(
                    localGraphsById.mapValues { (_, graph) -> graph.initial }
                ),
                graphByCompoundStateId = immutableStateChartMap(graphByCompoundStateId),
                regionsByParallelStateId = immutableStateChartMap(
                    regionsByParallelStateId.mapValues { (_, regionIds) ->
                        immutableStateChartList(regionIds)
                    }
                ),
                graphByRegionId = immutableStateChartMap(graphByRegionId)
            ),
            declarationIndex = DeclarationIndex(
                stateOrdinalById = immutableStateChartMap(stateOrdinalById),
                transitionOrdinalById = immutableStateChartMap(transitionOrdinalById),
                regionOrdinalById = immutableStateChartMap(regionOrdinalById),
                localGraphOrdinalById = immutableStateChartMap(localGraphOrdinalById)
            )
        )
    }

    private fun compileGraph(
        graph: RawExclusiveGraph<V, I, G, E>,
        owner: LocalGraphOwner<I>,
        parentLocation: ParentLocation<I>,
        declarationOrdinal: Int
    ): LocalGraphId {
        check(graph.children.isNotEmpty()) {
            "A local state graph must declare at least one child state."
        }

        val graphId = LocalGraphId(nextGraphId++)
        localGraphOrdinalById[graphId] = declarationOrdinal

        val childIds = ArrayList<I>()
        val finalChildIds = LinkedHashSet<I>()
        val pseudoChildIds = LinkedHashSet<I>()

        graph.children.forEach { child ->
            childIds.add(child.vertex.id)
            owningGraphByChildId[child.vertex.id] = graphId
            compileState(child, parentLocation)
            if (child.kind == RawStateKind.Final) {
                finalChildIds.add(child.vertex.id)
            }
        }

        graph.pseudos.forEach { pseudo ->
            pseudoChildIds.add(pseudo.vertex.id)
            compilePseudo(pseudo, parentLocation)
        }

        val initial = graph.initial
        check(initial != null) {
            "A local state graph must declare an initial state."
        }

        checkInitialTargetsLocal(initial, childIds, pseudoChildIds)

        val localGraph = LocalStateGraph(
            id = graphId,
            owner = owner,
            initial = initial,
            childIds = immutableStateChartList(childIds),
            finalChildIds = immutableStateChartSet(finalChildIds),
            pseudoChildIds = immutableStateChartSet(pseudoChildIds),
            declarationOrdinal = declarationOrdinal
        )

        localGraphsById[graphId] = localGraph
        return graphId
    }

    private fun compileState(
        rawState: RawState<V, I, G, E>,
        parentLocation: ParentLocation<I>
    ) {
        registerUniqueTargetId(rawState.vertex.id)
        parentByStateId[rawState.vertex.id] = parentLocation
        stateOrdinalById[rawState.vertex.id] = rawState.declarationOrdinal

        val outgoingTransitionIds = compileTransitions(rawState)
        val onEntry = rawState.onEntry.map { raw ->
            ActionDefinition(raw.ordinal, raw.execute)
        }
        val onExit = rawState.onExit.map { raw ->
            ActionDefinition(raw.ordinal, raw.execute)
        }
        compileInvokes(rawState)

        when (rawState.kind) {
            RawStateKind.Final -> {
                check(rawState.transitions.isEmpty()) {
                    "Final state ${rawState.vertex.id} must not declare transitions."
                }
                check(rawState.childGraph.children.isEmpty() && rawState.regions.isEmpty()) {
                    "Final state ${rawState.vertex.id} must not declare child states or regions."
                }

                finalStatesById[rawState.vertex.id] = FinalStateNode(
                    vertex = rawState.vertex,
                    parent = parentLocation,
                    onEntry = immutableStateChartList(onEntry),
                    onExit = immutableStateChartList(onExit),
                    declarationOrdinal = rawState.declarationOrdinal
                )
            }

            RawStateKind.State -> {
                if (rawState.childGraph.children.isEmpty()) {
                    check(rawState.histories.isEmpty()) {
                        "History markers require a compound or parallel owner state."
                    }

                    statesById[rawState.vertex.id] = AtomicStateNode(
                        vertex = rawState.vertex,
                        parent = parentLocation,
                        outgoingTransitionIds = immutableStateChartList(outgoingTransitionIds),
                        onEntry = immutableStateChartList(onEntry),
                        onExit = immutableStateChartList(onExit),
                        declarationOrdinal = rawState.declarationOrdinal
                    )
                } else {
                    rawState.histories.forEach { history ->
                        compileHistory(history)
                    }

                    val childGraphId = compileGraph(
                        graph = rawState.childGraph,
                        owner = LocalGraphOwner.CompoundState(rawState.vertex.id),
                        parentLocation = ParentLocation.Compound(rawState.vertex.id),
                        declarationOrdinal = rawState.declarationOrdinal
                    )
                    graphByCompoundStateId[rawState.vertex.id] = childGraphId

                    statesById[rawState.vertex.id] = CompoundStateNode(
                        vertex = rawState.vertex,
                        childGraphId = childGraphId,
                        historyMarkerIds = immutableStateChartList(rawState.histories.map { it.vertex.id }),
                        parent = parentLocation,
                        outgoingTransitionIds = immutableStateChartList(outgoingTransitionIds),
                        onEntry = immutableStateChartList(onEntry),
                        onExit = immutableStateChartList(onExit),
                        declarationOrdinal = rawState.declarationOrdinal
                    )
                }
            }

            RawStateKind.Parallel -> {
                check(rawState.regions.isNotEmpty()) {
                    "Parallel state ${rawState.vertex.id} must declare at least one region."
                }

                val regionNames = HashSet<String>()
                val regionIds = ArrayList<RegionId<I>>()

                rawState.histories.forEach { history ->
                    compileHistory(history)
                }

                rawState.regions.forEach { region ->
                    check(regionNames.add(region.name)) {
                        "Region ${region.name} is declared more than once in parallel state ${rawState.vertex.id}."
                    }

                    val regionId = RegionId(rawState.vertex.id, region.name)
                    val graphId = compileGraph(
                        graph = region.graph,
                        owner = LocalGraphOwner.Region(regionId),
                        parentLocation = ParentLocation.Region(regionId),
                        declarationOrdinal = region.declarationOrdinal
                    )

                    val regionDefinition = RegionDefinition(
                        id = regionId,
                        graphId = graphId,
                        declarationOrdinal = region.declarationOrdinal
                    )
                    regionsById[regionId] = regionDefinition
                    graphByRegionId[regionId] = graphId
                    regionOrdinalById[regionId] = region.declarationOrdinal
                    regionIds.add(regionId)
                }

                regionsByParallelStateId[rawState.vertex.id] = regionIds

                statesById[rawState.vertex.id] = ParallelStateNode(
                    vertex = rawState.vertex,
                    regionIds = immutableStateChartList(regionIds),
                    historyMarkerIds = immutableStateChartList(rawState.histories.map { it.vertex.id }),
                    parent = parentLocation,
                    outgoingTransitionIds = immutableStateChartList(outgoingTransitionIds),
                    onEntry = immutableStateChartList(onEntry),
                    onExit = immutableStateChartList(onExit),
                    declarationOrdinal = rawState.declarationOrdinal
                )
            }
        }
    }


    private fun compileInvokes(rawState: RawState<V, I, G, E>) {
        if (rawState.invokes.isEmpty()) {
            return
        }

        invokesByStateId[rawState.vertex.id] = rawState.invokes.map { rawInvoke ->
            InvokeDefinition(
                invokeId = rawInvoke.invokeId,
                execute = rawInvoke.execute,
                ordinal = rawInvoke.ordinal
            )
        }.toMutableList()
    }

    private fun compileHistory(history: RawHistory<V, I>) {
        registerUniqueTargetId(history.vertex.id)
        historyMarkersById[history.vertex.id] = HistoryMarker(
            vertex = history.vertex,
            ownerStateId = history.ownerStateId,
            type = history.type,
            defaultTarget = history.defaultTarget,
            declarationOrdinal = history.declarationOrdinal
        )
    }

    private fun compilePseudo(
        rawPseudo: RawPseudo<V, I, G, E>,
        parentLocation: ParentLocation<I>
    ) {
        registerUniqueTargetId(rawPseudo.vertex.id)
        val outgoingTransitionIds = compileTransitions(rawPseudo.vertex.id, rawPseudo.transitions)

        check(outgoingTransitionIds.isNotEmpty()) {
            "Pseudostate ${rawPseudo.vertex.id} must declare at least one outgoing transition."
        }

        when (rawPseudo.kind) {
            PseudoKind.Initial -> check(outgoingTransitionIds.size == 1) {
                "Initial pseudostate ${rawPseudo.vertex.id} must declare exactly one outgoing transition."
            }

            PseudoKind.Join -> check(outgoingTransitionIds.size == 1) {
                "Join pseudostate ${rawPseudo.vertex.id} must declare exactly one outgoing transition."
            }

            PseudoKind.Fork -> check(outgoingTransitionIds.size >= 2) {
                "Fork pseudostate ${rawPseudo.vertex.id} must declare at least two outgoing transitions."
            }

            PseudoKind.Choice,
            PseudoKind.Junction -> Unit
        }

        pseudoNodesById[rawPseudo.vertex.id] = PseudoNode(
            vertex = rawPseudo.vertex,
            kind = rawPseudo.kind,
            parent = parentLocation,
            outgoingTransitionIds = immutableStateChartList(outgoingTransitionIds),
            declarationOrdinal = rawPseudo.declarationOrdinal
        )
    }

    private fun compileTransitions(rawState: RawState<V, I, G, E>): List<TransitionId> {
        return compileTransitions(rawState.vertex.id, rawState.transitions)
    }

    private fun compileTransitions(sourceId: I, rawTransitions: List<RawTransition<V, I, G, E>>): List<TransitionId> {
        val transitionIds = ArrayList<TransitionId>()

        rawTransitions.forEach { rawTransition ->
            val transitionId = TransitionId(nextTransitionId++)
            val transition = TransitionDefinition(
                id = transitionId,
                sourceId = rawTransition.sourceId,
                trigger = rawTransition.trigger,
                target = rawTransition.target,
                type = rawTransition.type,
                guards = immutableStateChartList(
                    rawTransition.guards.map { raw ->
                        GuardDefinition(raw.ordinal, raw.evaluate)
                    }
                ),
                actions = immutableStateChartList(
                    rawTransition.actions.map { raw ->
                        TransitionActionDefinition(raw.ordinal, raw.execute)
                    }
                ),
                order = rawTransition.order,
                declarationOrdinal = rawTransition.declarationOrdinal
            )

            transitionsById[transitionId] = transition
            transitionsBySourceId.getOrPut(rawTransition.sourceId) { ArrayList() }.add(transitionId)
            transitionOrdinalById[transitionId] = rawTransition.declarationOrdinal
            transitionIds.add(transitionId)
        }

        transitionsBySourceId[sourceId]?.sortWith { left, right ->
            val leftTransition = transitionsById.getValue(left)
            val rightTransition = transitionsById.getValue(right)
            compareValuesBy(
                leftTransition,
                rightTransition,
                { it.order },
                { it.declarationOrdinal }
            )
        }

        return transitionIds.sortedWith { left, right ->
            val leftTransition = transitionsById.getValue(left)
            val rightTransition = transitionsById.getValue(right)
            compareValuesBy(
                leftTransition,
                rightTransition,
                { it.order },
                { it.declarationOrdinal }
            )
        }
    }

    private fun checkInitialTargetsLocal(initial: EntryTarget<I>, childIds: List<I>, pseudoChildIds: Set<I>) {
        when (initial) {
            is EntryTarget.State -> check(childIds.contains(initial.stateId)) {
                "Initial target ${initial.stateId} must be a direct child of its local graph."
            }

            is EntryTarget.History -> check(historyMarkersById.containsKey(initial.historyId)) {
                "Initial history target ${initial.historyId} does not exist."
            }

            is EntryTarget.Pseudo -> check(pseudoChildIds.contains(initial.pseudoId)) {
                "Initial pseudostate target ${initial.pseudoId} must be declared in its local graph."
            }
        }
    }

    private fun registerUniqueTargetId(id: I) {
        check(registeredTargetIds.add(id)) {
            "A statechart target with the id $id is already declared."
        }
    }

    private fun validateTargets() {
        transitionsById.values.forEach { transition ->
            when (val target = transition.target) {
                TargetSpec.None -> Unit
                is TargetSpec.Single -> checkTargetExists(target.id, transition.sourceId)
                is TargetSpec.Multiple -> {
                    check(target.ids.distinct().size == target.ids.size) {
                        "Transition from ${transition.sourceId} declares duplicate target ids."
                    }
                    target.ids.forEach { targetId ->
                        checkTargetExists(targetId, transition.sourceId)
                    }
                }
            }
        }

        historyMarkersById.values.forEach { marker ->
            when (val defaultTarget = marker.defaultTarget) {
                null -> Unit
                is EntryTarget.State -> checkTargetExists(defaultTarget.stateId, marker.ownerStateId)
                is EntryTarget.History -> checkTargetExists(defaultTarget.historyId, marker.ownerStateId)
                is EntryTarget.Pseudo -> checkTargetExists(defaultTarget.pseudoId, marker.ownerStateId)
            }
        }
    }


    private fun validateInternalTransitions(pathByStateId: Map<I, StatePath<I>>) {
        transitionsById.values.forEach { transition ->
            if (transition.type != TransitionType.Internal || transition.target == TargetSpec.None) {
                return@forEach
            }

            val sourcePath = pathByStateId[transition.sourceId]?.stateIds
                ?: return@forEach
            val targetIds = when (val target = transition.target) {
                TargetSpec.None -> emptyList()
                is TargetSpec.Single -> listOf(target.id)
                is TargetSpec.Multiple -> target.ids
            }

            targetIds.forEach { targetId ->
                val targetPath = pathForValidationTarget(targetId, pathByStateId)
                    ?: return@forEach
                check(targetPath.startsWith(sourcePath)) {
                    "Internal transition from ${transition.sourceId} must target a descendant of its source state."
                }
            }
        }
    }

    private fun pathForValidationTarget(targetId: I, pathByStateId: Map<I, StatePath<I>>): List<I>? {
        historyMarkersById[targetId]?.let { marker ->
            return pathByStateId.getValue(marker.ownerStateId).stateIds
        }

        if (pseudoNodesById.containsKey(targetId)) {
            return null
        }

        return pathByStateId[targetId]?.stateIds
    }

    private fun checkTargetExists(targetId: I, sourceId: I) {
        val exists = statesById.containsKey(targetId) ||
                finalStatesById.containsKey(targetId) ||
                historyMarkersById.containsKey(targetId) ||
                pseudoNodesById.containsKey(targetId)

        check(exists) {
            "Transition from $sourceId targets missing statechart id $targetId."
        }
    }

    private fun <T> List<T>.startsWith(prefix: List<T>): Boolean {
        if (prefix.size > size) {
            return false
        }

        return prefix.indices.all { index ->
            this[index] == prefix[index]
        }
    }

    private fun buildPaths(): Map<I, StatePath<I>> {
        val paths = LinkedHashMap<I, StatePath<I>>()
        val allIds = stateOrdinalById.keys

        allIds.forEach { id ->
            paths[id] = StatePath(buildPathFor(id))
        }

        return paths
    }

    private fun buildPathFor(id: I): List<I> {
        val parent = parentByStateId.getValue(id)
        return when (parent) {
            ParentLocation.Root -> listOf(id)
            is ParentLocation.Compound -> buildPathFor(parent.ownerStateId) + id
            is ParentLocation.Region -> buildPathFor(parent.regionId.parallelStateId) + id
        }
    }

    private fun buildDescendants(pathByStateId: Map<I, StatePath<I>>): Map<I, Set<I>> {
        val descendants = LinkedHashMap<I, MutableSet<I>>()

        pathByStateId.keys.forEach { id ->
            descendants[id] = LinkedHashSet()
        }

        pathByStateId.forEach { (stateId, path) ->
            path.stateIds.dropLast(1).forEach { ancestorId ->
                descendants.getOrPut(ancestorId) { LinkedHashSet() }.add(stateId)
            }
        }

        return descendants.mapValues { (_, values) ->
            immutableStateChartSet(values)
        }
    }
}
