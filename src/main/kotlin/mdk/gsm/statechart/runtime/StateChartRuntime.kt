package mdk.gsm.statechart.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import mdk.gsm.graph.Vertex
import mdk.gsm.state.MachineContext
import mdk.gsm.statechart.StateChartActionScope
import mdk.gsm.statechart.StateChartDispatch
import mdk.gsm.statechart.StateChartException
import mdk.gsm.statechart.StateChartGuardScope
import mdk.gsm.statechart.StateChartRuntimeConfig
import mdk.gsm.statechart.StateChartState
import mdk.gsm.statechart.StateChartTransitionActionScope
import mdk.gsm.statechart.model.CompoundStateNode
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.HistoryMarker
import mdk.gsm.statechart.model.HistoryType
import mdk.gsm.statechart.model.InvokeDefinition
import mdk.gsm.statechart.model.LocalGraphId
import mdk.gsm.statechart.model.ParallelStateNode
import mdk.gsm.statechart.model.PseudoKind
import mdk.gsm.statechart.model.PseudoNode
import mdk.gsm.statechart.model.StateChartDefinition
import mdk.gsm.statechart.model.StateNode
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TransitionDefinition
import mdk.gsm.statechart.model.TransitionId
import mdk.gsm.statechart.model.TransitionType
import mdk.gsm.statechart.model.TriggerDefinition
import mdk.gsm.statechart.validation.assertLegalConfiguration

internal class StateChartRuntime<V, I, G, E>(
    val definition: StateChartDefinition<V, I, G, E>,
    private val machineContext: G?,
    private val runtimeConfig: StateChartRuntimeConfig,
    private val scheduleTimer: (String, Long) -> Unit = { _, _ -> },
    private val cancelTimer: (String) -> Unit = {},
    private val cancelAllTimers: () -> Unit = {},
    private val startInvoke: (InvokeDefinition<G, E>, G?) -> Unit = { _, _ -> },
    private val cancelInvoke: (String) -> Unit = {},
    private val cancelAllInvokes: () -> Unit = {}
) where V : Vertex<I> {

    private val active = ActiveConfiguration<I>()
    private val activeTreeFactory = ActiveTreeFactory(definition, active)
    private val internalEvents = ArrayDeque<RuntimeEvent<E, I>>()
    private val doneStatesRaisedInMacrostep = LinkedHashSet<I>()
    private val historySnapshots = LinkedHashMap<I, HistorySnapshot<I>>()
    private var suppressedDefaultEntryGraphs: Set<LocalGraphId> = emptySet()

    val stateOut: MutableStateFlow<StateChartState<V, I>>

    private var microstepsInCurrentMacrostep = 0

    init {
        runBlocking {
            enterInitialConfiguration(runEntryActions = false, event = null)
            assertLegalConfiguration(definition, active)
            stabilizeInitialMacrostep()
        }
        assertLegalConfiguration(definition, active)
        stateOut = MutableStateFlow(createState())
    }

    suspend fun dispatch(dispatch: StateChartDispatch<E>): StateChartState<V, I> {
        return when (dispatch) {
            is StateChartDispatch.Event -> processExternalEvent(dispatch.event)
            is StateChartDispatch.Timer -> processExternalRuntimeEvent(RuntimeEvent.Timer(dispatch.timerId))
            is StateChartDispatch.DoneInvoke -> processExternalRuntimeEvent(RuntimeEvent.DoneInvoke(dispatch.invokeId))
            is StateChartDispatch.ErrorInvoke -> processExternalRuntimeEvent(
                RuntimeEvent.ErrorInvoke(dispatch.invokeId, dispatch.cause)
            )
            StateChartDispatch.Reset -> reset()
        }
    }

    private suspend fun processExternalEvent(event: E): StateChartState<V, I> {
        return processExternalRuntimeEvent(RuntimeEvent.External(event))
    }

    private suspend fun processExternalRuntimeEvent(runtimeEvent: RuntimeEvent<E, I>): StateChartState<V, I> {
        beginMacrostep()
        processRuntimeEvent(runtimeEvent)
        drainStabilizationEvents()
        return publishStableState()
    }

    private suspend fun stabilizeInitialMacrostep() {
        beginMacrostep()
        enqueueCompletionEvents()
        drainStabilizationEvents()
    }

    private fun beginMacrostep() {
        microstepsInCurrentMacrostep = 0
        internalEvents.clear()
        doneStatesRaisedInMacrostep.clear()
    }

    private suspend fun drainStabilizationEvents() {
        while (true) {
            val eventlessTransitionFired = processEventlessTransitions()
            if (eventlessTransitionFired) {
                continue
            }

            val runtimeEvent = internalEvents.removeFirstOrNull()
                ?: return
            processRuntimeEvent(runtimeEvent)
        }
    }

    private suspend fun processEventlessTransitions(): Boolean {
        var fired = false

        while (true) {
            val selectedTransitions = selectTransitions(RuntimeEvent.Eventless)
            if (selectedTransitions.isEmpty()) {
                return fired
            }

            executeMicrostep(selectedTransitions, RuntimeEvent.Eventless)
            fired = true
        }
    }

    private suspend fun processRuntimeEvent(runtimeEvent: RuntimeEvent<E, I>) {
        val selectedTransitions = selectTransitions(runtimeEvent)

        if (selectedTransitions.isEmpty()) {
            return
        }

        executeMicrostep(selectedTransitions, runtimeEvent)
    }

    private suspend fun selectTransitions(
        runtimeEvent: RuntimeEvent<E, I>
    ): List<SelectedTransition<V, I, G, E>> {
        val candidates = LinkedHashMap<TransitionId, SelectedTransition<V, I, G, E>>()

        activeLeafStateIds().forEach { leafStateId ->
            val path = definition.containmentIndex.pathByStateId.getValue(leafStateId).stateIds

            for (sourceId in path.asReversed()) {
                val transitionIds = definition.transitionsBySourceId[sourceId]
                    ?: continue

                val source = definition.statesById[sourceId]
                    ?: continue

                val candidate = firstEligibleTransition(
                    source = source,
                    transitionIds = transitionIds,
                    runtimeEvent = runtimeEvent
                )

                if (candidate != null) {
                    candidates.putIfAbsent(candidate.transition.id, candidate)
                    break
                }
            }
        }

        return resolveConflicts(candidates.values.toList())
    }

    private suspend fun firstEligibleTransition(
        source: StateNode<V, I, G, E>,
        transitionIds: List<TransitionId>,
        runtimeEvent: RuntimeEvent<E, I>
    ): SelectedTransition<V, I, G, E>? {
        transitionIds.forEach { transitionId ->
            val transition = definition.transitionsById.getValue(transitionId)

            if (!triggerMatches(transition.trigger, runtimeEvent)) {
                return@forEach
            }

            val guardsAllow = transition.guards.all { guard ->
                guard.evaluate.invoke(
                    StateChartGuardScope(
                        source = source.vertex,
                        event = runtimeEvent.userEventOrNull(),
                        machineContext = machineContext
                    )
                )
            }

            if (!guardsAllow) {
                return@forEach
            }

            if (!targetJoinsReady(transition.target, runtimeEvent)) {
                return@forEach
            }

            val resolved = resolveTransitionTarget(transition, runtimeEvent)

            return SelectedTransition(
                transition = transition,
                target = resolved.target,
                executions = listOf(TransitionExecution(transition, resolved.target)) + resolved.routingExecutions,
                exitSet = computeExitSet(transition, resolved.target),
                sourceDepth = definition.containmentIndex.pathByStateId
                    .getValue(transition.sourceId)
                    .stateIds
                    .size
            )
        }

        return null
    }


    private suspend fun targetJoinsReady(target: TargetSpec<I>, runtimeEvent: RuntimeEvent<E, I>): Boolean {
        return when (target) {
            TargetSpec.None -> true
            is TargetSpec.Single -> joinReadyIfTargetIsJoin(target.id, runtimeEvent)
            is TargetSpec.Multiple -> target.ids.all { targetId ->
                joinReadyIfTargetIsJoin(targetId, runtimeEvent)
            }
        }
    }

    private suspend fun joinReadyIfTargetIsJoin(targetId: I, runtimeEvent: RuntimeEvent<E, I>): Boolean {
        val pseudoNode = definition.pseudoNodesById[targetId]
            ?: return true
        if (pseudoNode.kind != PseudoKind.Join) {
            return true
        }

        val incomingTransitions = definition.transitionsById.values.filter { transition ->
            targetIdsFor(transition.target).contains(targetId)
        }
        if (incomingTransitions.isEmpty()) {
            return false
        }

        return incomingTransitions.all { transition ->
            val source = definition.statesById[transition.sourceId]
                ?: return@all false
            active.isActive(transition.sourceId) &&
                    triggerMatches(transition.trigger, runtimeEvent) &&
                    transition.guards.all { guard ->
                        guard.evaluate.invoke(
                            StateChartGuardScope(
                                source = source.vertex,
                                event = runtimeEvent.userEventOrNull(),
                                machineContext = machineContext
                            )
                        )
                    }
        }
    }

    private fun triggerMatches(trigger: TriggerDefinition<E, I>, runtimeEvent: RuntimeEvent<E, I>): Boolean {
        return when (trigger) {
            is TriggerDefinition.Exact -> runtimeEvent.userEventOrNull() == trigger.event
            is TriggerDefinition.Predicate -> runtimeEvent.userEventOrNull()?.let(trigger.predicate) ?: false
            TriggerDefinition.Eventless -> runtimeEvent == RuntimeEvent.Eventless
            is TriggerDefinition.DoneState -> runtimeEvent is RuntimeEvent.DoneState && runtimeEvent.stateId == trigger.stateId
            is TriggerDefinition.DoneInvoke -> runtimeEvent is RuntimeEvent.DoneInvoke && runtimeEvent.invokeId == trigger.invokeId
            is TriggerDefinition.ErrorInvoke -> runtimeEvent is RuntimeEvent.ErrorInvoke && runtimeEvent.invokeId == trigger.invokeId
            is TriggerDefinition.Timer -> runtimeEvent is RuntimeEvent.Timer && runtimeEvent.timerId == trigger.timerId
        }
    }

    private fun pseudoTriggerMatches(trigger: TriggerDefinition<E, I>, runtimeEvent: RuntimeEvent<E, I>): Boolean {
        return trigger == TriggerDefinition.Eventless || triggerMatches(trigger, runtimeEvent)
    }

    private suspend fun resolveTransitionTarget(
        transition: TransitionDefinition<V, I, G, E>,
        runtimeEvent: RuntimeEvent<E, I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        return resolveTargetSpec(transition.target, runtimeEvent, LinkedHashSet())
    }

    private suspend fun resolveTargetSpec(
        target: TargetSpec<I>,
        runtimeEvent: RuntimeEvent<E, I>,
        visitedPseudos: MutableSet<I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        return when (target) {
            TargetSpec.None -> ResolvedTransitionTarget(TargetSpec.None, emptyList())
            is TargetSpec.Single -> resolveTargetId(target.id, runtimeEvent, visitedPseudos)
            is TargetSpec.Multiple -> {
                val resolvedIds = ArrayList<I>()
                val executions = ArrayList<TransitionExecution<V, I, G, E>>()

                target.ids.forEach { targetId ->
                    val resolved = resolveTargetId(targetId, runtimeEvent, LinkedHashSet(visitedPseudos))
                    resolvedIds.addAll(targetIdsFor(resolved.target))
                    executions.addAll(resolved.routingExecutions)
                }

                ResolvedTransitionTarget(
                    target = targetSpecForIds(resolvedIds),
                    routingExecutions = executions
                )
            }
        }
    }

    private suspend fun resolveTargetId(
        targetId: I,
        runtimeEvent: RuntimeEvent<E, I>,
        visitedPseudos: MutableSet<I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        if (definition.pseudoNodesById.containsKey(targetId)) {
            return resolvePseudoTarget(targetId, runtimeEvent, visitedPseudos)
        }

        return ResolvedTransitionTarget(TargetSpec.Single(targetId), emptyList())
    }

    private suspend fun resolvePseudoTarget(
        pseudoId: I,
        runtimeEvent: RuntimeEvent<E, I>,
        visitedPseudos: MutableSet<I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        if (!visitedPseudos.add(pseudoId)) {
            throw StateChartException.InvalidTransitionTarget("Pseudostate route cycle detected at $pseudoId.")
        }

        val pseudoNode = definition.pseudoNodesById.getValue(pseudoId)
        val outgoingTransitions = definition.transitionsBySourceId[pseudoId]
            ?: emptyList()
        val eligibleTransitions = ArrayList<TransitionDefinition<V, I, G, E>>()

        outgoingTransitions.forEach { transitionId ->
            val transition = definition.transitionsById.getValue(transitionId)
            if (pseudoTransitionEligible(pseudoNode, transition, runtimeEvent)) {
                eligibleTransitions.add(transition)
            }
        }

        if (eligibleTransitions.isEmpty()) {
            throw StateChartException.UnresolvedPseudostate(pseudoId)
        }

        return when (pseudoNode.kind) {
            PseudoKind.Fork -> resolveForkTargets(eligibleTransitions, runtimeEvent, visitedPseudos)
            PseudoKind.Initial,
            PseudoKind.Choice,
            PseudoKind.Junction,
            PseudoKind.Join -> resolveSinglePseudoRoute(eligibleTransitions.first(), runtimeEvent, visitedPseudos)
        }
    }

    private suspend fun pseudoTransitionEligible(
        pseudoNode: PseudoNode<V, I, G, E>,
        transition: TransitionDefinition<V, I, G, E>,
        runtimeEvent: RuntimeEvent<E, I>
    ): Boolean {
        if (!pseudoTriggerMatches(transition.trigger, runtimeEvent)) {
            return false
        }

        return transition.guards.all { guard ->
            guard.evaluate.invoke(
                StateChartGuardScope(
                    source = pseudoNode.vertex,
                    event = runtimeEvent.userEventOrNull(),
                    machineContext = machineContext
                )
            )
        }
    }

    private suspend fun resolveSinglePseudoRoute(
        transition: TransitionDefinition<V, I, G, E>,
        runtimeEvent: RuntimeEvent<E, I>,
        visitedPseudos: MutableSet<I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        val resolved = resolveTargetSpec(transition.target, runtimeEvent, visitedPseudos)
        return ResolvedTransitionTarget(
            target = resolved.target,
            routingExecutions = listOf(TransitionExecution(transition, resolved.target)) + resolved.routingExecutions
        )
    }

    private suspend fun resolveForkTargets(
        transitions: List<TransitionDefinition<V, I, G, E>>,
        runtimeEvent: RuntimeEvent<E, I>,
        visitedPseudos: MutableSet<I>
    ): ResolvedTransitionTarget<V, I, G, E> {
        val resolvedIds = ArrayList<I>()
        val executions = ArrayList<TransitionExecution<V, I, G, E>>()

        transitions.forEach { transition ->
            val resolved = resolveTargetSpec(transition.target, runtimeEvent, LinkedHashSet(visitedPseudos))
            resolvedIds.addAll(targetIdsFor(resolved.target))
            executions.add(TransitionExecution(transition, resolved.target))
            executions.addAll(resolved.routingExecutions)
        }

        return ResolvedTransitionTarget(
            target = targetSpecForIds(resolvedIds),
            routingExecutions = executions
        )
    }

    private fun targetSpecForIds(ids: List<I>): TargetSpec<I> {
        val distinctIds = ids.distinct()
        return when (distinctIds.size) {
            0 -> TargetSpec.None
            1 -> TargetSpec.Single(distinctIds.single())
            else -> TargetSpec.Multiple(distinctIds)
        }
    }

    private fun resolveConflicts(
        candidates: List<SelectedTransition<V, I, G, E>>
    ): List<SelectedTransition<V, I, G, E>> {
        val selected = ArrayList<SelectedTransition<V, I, G, E>>()
        val sorted = candidates.sortedWith(
            compareByDescending<SelectedTransition<V, I, G, E>> { it.sourceDepth }
                .thenBy { it.transition.declarationOrdinal }
        )

        sorted.forEach { candidate ->
            val conflicts = selected.any { existing ->
                existing.exitSet.any { candidate.exitSet.contains(it) }
            }

            if (!conflicts) {
                selected.add(candidate)
            }
        }

        return selected.sortedBy { it.transition.declarationOrdinal }
    }

    private suspend fun executeMicrostep(
        selectedTransitions: List<SelectedTransition<V, I, G, E>>,
        runtimeEvent: RuntimeEvent<E, I>
    ) {
        microstepsInCurrentMacrostep++
        if (microstepsInCurrentMacrostep > runtimeConfig.maxMicrostepsPerMacrostep) {
            throw StateChartException.MacrostepLimitExceeded(runtimeConfig.maxMicrostepsPerMacrostep)
        }

        val event = runtimeEvent.userEventOrNull()
        val exitSet = selectedTransitions
            .flatMap { it.exitSet }
            .distinct()
            .sortedWith { left, right ->
                val leftPath = definition.containmentIndex.pathByStateId.getValue(left).stateIds
                val rightPath = definition.containmentIndex.pathByStateId.getValue(right).stateIds

                compareValuesBy(
                    rightPath,
                    leftPath,
                    { it.size },
                    { stateOrdinalFor(it.last()) }
                )
            }

        captureHistory(exitSet)

        exitSet.forEach { stateId ->
            cancelStateTimers(stateId)
            cancelStateInvokes(stateId)
            runExitActions(stateId, event)
            active.deactivate(stateId)
        }

        selectedTransitions.forEach { selected ->
            selected.executions.forEach { execution ->
                runTransitionActions(execution.transition, execution.target, event)
            }
        }

        suppressedDefaultEntryGraphs = selectedTransitions
            .flatMap { selected -> explicitGraphIdsForTarget(selected.target) }
            .toSet()

        try {
            selectedTransitions.forEach { selected ->
                enterTargetSpec(selected.target, event, runEntryActions = true)
            }
        } finally {
            suppressedDefaultEntryGraphs = emptySet()
        }

        assertLegalConfiguration(definition, active)
        enqueueCompletionEvents()
    }

    private fun computeExitSet(
        transition: TransitionDefinition<V, I, G, E>,
        target: TargetSpec<I>
    ): List<I> {
        if (target == TargetSpec.None) {
            return emptyList()
        }

        val sourcePath = definition.containmentIndex.pathByStateId
            .getValue(transition.sourceId)
            .stateIds
        val exitIds = LinkedHashSet<I>()

        targetPathsFor(target).forEach { targetPath ->
            exitIds.addAll(computeExitSetForTargetPath(transition, sourcePath, targetPath))
        }

        return exitIds.toList()
    }

    private fun computeExitSetForTargetPath(
        transition: TransitionDefinition<V, I, G, E>,
        sourcePath: List<I>,
        targetPath: List<I>
    ): List<I> {
        if (targetPath.isEmpty()) {
            return emptyList()
        }

        if (transition.type == TransitionType.Internal && targetPath.startsWith(sourcePath)) {
            return active.snapshotIds()
                .filter { activeStateId ->
                    activeStateId != transition.sourceId &&
                            definition.containmentIndex.pathByStateId.getValue(activeStateId).stateIds.startsWith(sourcePath)
                }
        }

        val commonPrefixLength = commonPrefixLength(sourcePath, targetPath)
        val branchPrefix = if (commonPrefixLength >= sourcePath.size) {
            sourcePath
        } else {
            sourcePath.take(commonPrefixLength + 1)
        }

        return active.snapshotIds()
            .filter { activeStateId ->
                val path = definition.containmentIndex.pathByStateId.getValue(activeStateId).stateIds
                path.startsWith(branchPrefix)
            }
    }

    private fun explicitGraphIdsForTarget(target: TargetSpec<I>): Set<LocalGraphId> {
        return when (target) {
            TargetSpec.None -> emptySet()
            is TargetSpec.Single -> explicitGraphIdsForTargetId(target.id).toSet()
            is TargetSpec.Multiple -> target.ids.flatMap { targetId ->
                explicitGraphIdsForTargetId(targetId)
            }.toSet()
        }
    }

    private fun explicitGraphIdsForTargetId(targetId: I): List<LocalGraphId> {
        return pathForTargetId(targetId).mapNotNull { stateId ->
            definition.containmentIndex.owningGraphByChildId[stateId]
        }
    }

    private fun targetPathsFor(target: TargetSpec<I>): List<List<I>> {
        return when (target) {
            TargetSpec.None -> emptyList()
            is TargetSpec.Single -> listOf(pathForTargetId(target.id))
            is TargetSpec.Multiple -> target.ids.map { pathForTargetId(it) }
        }
    }

    private fun pathForTargetId(targetId: I): List<I> {
        definition.historyMarkersById[targetId]?.let { marker ->
            return definition.containmentIndex.pathByStateId
                .getValue(marker.ownerStateId)
                .stateIds
        }

        return definition.containmentIndex.pathByStateId
            .getValue(targetId)
            .stateIds
    }

    private suspend fun enterTargetSpec(
        target: TargetSpec<I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        when (target) {
            TargetSpec.None -> Unit
            is TargetSpec.Single -> enterTargetId(target.id, event, runEntryActions)
            is TargetSpec.Multiple -> target.ids.forEach { targetId ->
                enterTargetId(targetId, event, runEntryActions)
            }
        }
    }

    private suspend fun enterTargetId(targetId: I, event: E?, runEntryActions: Boolean) {
        definition.historyMarkersById[targetId]?.let { marker ->
            enterHistory(marker, event, runEntryActions)
            return
        }

        definition.pseudoNodesById[targetId]?.let {
            val runtimeEvent = if (event == null) RuntimeEvent.Eventless else RuntimeEvent.Raised(event)
            val resolved = resolveTargetSpec(TargetSpec.Single(targetId), runtimeEvent, LinkedHashSet())
            resolved.routingExecutions.forEach { execution ->
                runTransitionActions(execution.transition, execution.target, event)
            }
            enterTargetSpec(resolved.target, event, runEntryActions)
            return
        }

        enterPath(
            definition.containmentIndex.pathByStateId.getValue(targetId).stateIds,
            event,
            runEntryActions
        )
    }

    private suspend fun enterHistory(
        marker: HistoryMarker<V, I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        val snapshot = historySnapshots[marker.id]
        if (snapshot == null || snapshot.stateIds.isEmpty()) {
            enterHistoryDefault(marker, event, runEntryActions)
            return
        }

        val suppressedGraphs = graphIdsForHistoryRestore(marker, snapshot)
        withSuppressedDefaultEntryGraphs(suppressedGraphs) {
            enterPath(
                definition.containmentIndex.pathByStateId.getValue(marker.ownerStateId).stateIds,
                event,
                runEntryActions
            )

            snapshot.stateIds.forEach { stateId ->
                enterTargetId(stateId, event, runEntryActions)
            }
        }
    }

    private suspend fun enterHistoryDefault(
        marker: HistoryMarker<V, I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        val defaultTarget = marker.defaultTarget
        if (defaultTarget != null) {
            enterEntryTarget(defaultTarget, event, runEntryActions)
        } else {
            enterPath(
                definition.containmentIndex.pathByStateId.getValue(marker.ownerStateId).stateIds,
                event,
                runEntryActions
            )
        }
    }

    private suspend fun withSuppressedDefaultEntryGraphs(
        graphIds: Set<LocalGraphId>,
        block: suspend () -> Unit
    ) {
        val previous = suppressedDefaultEntryGraphs
        suppressedDefaultEntryGraphs = previous + graphIds
        try {
            block()
        } finally {
            suppressedDefaultEntryGraphs = previous
        }
    }

    private fun graphIdsForHistoryRestore(marker: HistoryMarker<V, I>, snapshot: HistorySnapshot<I>): Set<LocalGraphId> {
        val graphIds = LinkedHashSet<LocalGraphId>()
        graphIds.addAll(graphIdsOwnedByCompositeState(marker.ownerStateId))

        if (marker.type == HistoryType.Deep) {
            snapshot.stateIds.forEach { stateId ->
                graphIds.addAll(graphIdsOwnedByCompositeState(stateId))
            }
        }

        return graphIds
    }

    private fun graphIdsOwnedByCompositeState(stateId: I): Set<LocalGraphId> {
        return when (val state = definition.statesById[stateId]) {
            is CompoundStateNode -> setOf(state.childGraphId)
            is ParallelStateNode -> state.regionIds.map { regionId ->
                definition.regionsById.getValue(regionId).graphId
            }.toSet()
            else -> emptySet()
        }
    }

    private suspend fun enterEntryTarget(
        entryTarget: EntryTarget<I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        when (entryTarget) {
            is EntryTarget.State -> enterTargetId(entryTarget.stateId, event, runEntryActions)
            is EntryTarget.History -> enterTargetId(entryTarget.historyId, event, runEntryActions)
            is EntryTarget.Pseudo -> enterTargetId(entryTarget.pseudoId, event, runEntryActions)
        }
    }

    private suspend fun enterInitialConfiguration(runEntryActions: Boolean, event: E?) {
        active.clear()
        val rootGraph = definition.localGraphsById.getValue(definition.rootGraphId)
        enterGraph(rootGraph.id, emptyList(), event, runEntryActions)
    }

    private suspend fun enterPath(path: List<I>, event: E?, runEntryActions: Boolean) {
        enterGraph(definition.rootGraphId, path, event, runEntryActions)
    }

    private suspend fun enterGraph(
        graphId: LocalGraphId,
        explicitPath: List<I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        val graph = definition.localGraphsById.getValue(graphId)
        val explicitChildId = explicitPath.firstOrNull()

        if (explicitChildId != null && graph.childIds.contains(explicitChildId)) {
            enterState(
                stateId = explicitChildId,
                remainingPath = explicitPath.drop(1),
                event = event,
                runEntryActions = runEntryActions
            )
        } else {
            enterEntryTarget(graph.initial, event, runEntryActions)
        }
    }

    private suspend fun enterState(
        stateId: I,
        remainingPath: List<I>,
        event: E?,
        runEntryActions: Boolean
    ) {
        val wasInactive = !active.isActive(stateId)
        if (wasInactive) {
            active.activate(stateId)
            if (runEntryActions) {
                runEntryActions(stateId, event)
            }
            startStateTimers(stateId)
            startStateInvokes(stateId)
        }

        val state = definition.statesById[stateId]
            ?: return

        when (state) {
            is CompoundStateNode -> {
                if (remainingPath.isNotEmpty() || shouldDefaultEnterGraph(state.childGraphId, wasInactive)) {
                    enterGraph(
                        graphId = state.childGraphId,
                        explicitPath = remainingPath,
                        event = event,
                        runEntryActions = runEntryActions
                    )
                }
            }

            is ParallelStateNode -> state.regionIds.forEach { regionId ->
                val region = definition.regionsById.getValue(regionId)
                val graph = definition.localGraphsById.getValue(region.graphId)
                val regionHasExplicitPath = remainingPath.firstOrNull() in graph.childIds
                val regionExplicitPath = if (regionHasExplicitPath) {
                    remainingPath
                } else {
                    emptyList()
                }

                if (regionHasExplicitPath || shouldDefaultEnterGraph(region.graphId, wasInactive)) {
                    enterGraph(
                        graphId = region.graphId,
                        explicitPath = regionExplicitPath,
                        event = event,
                        runEntryActions = runEntryActions
                    )
                }
            }

            else -> Unit
        }
    }

    private fun shouldDefaultEnterGraph(graphId: LocalGraphId, ownerWasInactive: Boolean): Boolean {
        return (ownerWasInactive || activeChildCount(graphId) == 0) &&
                !suppressedDefaultEntryGraphs.contains(graphId)
    }

    private fun activeChildCount(graphId: LocalGraphId): Int {
        val graph = definition.localGraphsById.getValue(graphId)
        return graph.childIds.count { childId ->
            active.isActive(childId)
        }
    }

    private fun captureHistory(exitSet: List<I>) {
        if (exitSet.isEmpty()) {
            return
        }

        exitSet.forEach { stateId ->
            val state = definition.statesById[stateId]
            val markerIds = when (state) {
                is CompoundStateNode -> state.historyMarkerIds
                is ParallelStateNode -> state.historyMarkerIds
                else -> emptyList()
            }

            if (markerIds.isEmpty() || !active.isActive(stateId)) {
                return@forEach
            }

            markerIds.forEach { markerId ->
                val marker = definition.historyMarkersById.getValue(markerId)
                historySnapshots[markerId] = captureHistorySnapshot(marker)
            }
        }
    }

    private fun captureHistorySnapshot(marker: HistoryMarker<V, I>): HistorySnapshot<I> {
        val stateIds = when (marker.type) {
            HistoryType.Shallow -> shallowHistoryStateIds(marker.ownerStateId)
            HistoryType.Deep -> deepHistoryStateIds(marker.ownerStateId)
        }

        return HistorySnapshot(stateIds)
    }

    private fun shallowHistoryStateIds(ownerStateId: I): List<I> {
        return when (val owner = definition.statesById[ownerStateId]) {
            is CompoundStateNode -> activeDirectChildIds(owner.childGraphId)
            is ParallelStateNode -> owner.regionIds.flatMap { regionId ->
                activeDirectChildIds(definition.regionsById.getValue(regionId).graphId)
            }
            else -> emptyList()
        }.sortedBy { stateOrdinalFor(it) }
    }

    private fun deepHistoryStateIds(ownerStateId: I): List<I> {
        val ownerPath = definition.containmentIndex.pathByStateId.getValue(ownerStateId).stateIds
        return active.snapshotIds()
            .filter { stateId ->
                stateId != ownerStateId &&
                        definition.containmentIndex.pathByStateId.getValue(stateId).stateIds.startsWith(ownerPath)
            }
            .sortedWith { left, right ->
                val leftPath = definition.containmentIndex.pathByStateId.getValue(left).stateIds
                val rightPath = definition.containmentIndex.pathByStateId.getValue(right).stateIds
                compareValuesBy(
                    leftPath,
                    rightPath,
                    { it.size },
                    { stateOrdinalFor(it.last()) }
                )
            }
    }

    private fun activeDirectChildIds(graphId: LocalGraphId): List<I> {
        val graph = definition.localGraphsById.getValue(graphId)
        return graph.childIds.filter { childId -> active.isActive(childId) }
    }



    private fun startStateInvokes(stateId: I) {
        definition.invokesByStateId[stateId].orEmpty()
            .sortedBy { invoke -> invoke.ordinal }
            .forEach { invoke -> startInvoke(invoke, machineContext) }
    }

    private fun cancelStateInvokes(stateId: I) {
        definition.invokesByStateId[stateId].orEmpty()
            .forEach { invoke -> cancelInvoke(invoke.invokeId) }
    }

    private fun startStateTimers(stateId: I) {
        definition.transitionsBySourceId[stateId].orEmpty().forEach { transitionId ->
            val trigger = definition.transitionsById.getValue(transitionId).trigger
            if (trigger is TriggerDefinition.Timer && trigger.delayMillis != null) {
                scheduleTimer(trigger.timerId, trigger.delayMillis)
            }
        }
    }

    private fun cancelStateTimers(stateId: I) {
        definition.transitionsBySourceId[stateId].orEmpty().forEach { transitionId ->
            val trigger = definition.transitionsById.getValue(transitionId).trigger
            if (trigger is TriggerDefinition.Timer && trigger.delayMillis != null) {
                cancelTimer(trigger.timerId)
            }
        }
    }

    private fun enqueueCompletionEvents() {
        completedCompositeStateIds().forEach { stateId ->
            if (doneStatesRaisedInMacrostep.add(stateId)) {
                internalEvents.addLast(RuntimeEvent.DoneState(stateId))
            }
        }
    }

    private fun completedCompositeStateIds(): List<I> {
        return active.snapshotIds()
            .filter { stateId -> isCompositeStateDone(stateId) }
            .sortedBy { stateOrdinalFor(it) }
    }

    private fun isCompositeStateDone(stateId: I): Boolean {
        if (!active.isActive(stateId)) {
            return false
        }

        return when (val state = definition.statesById[stateId]) {
            is CompoundStateNode -> {
                val graph = definition.localGraphsById.getValue(state.childGraphId)
                graph.finalChildIds.any { finalId -> active.isActive(finalId) }
            }

            is ParallelStateNode -> state.regionIds.all { regionId ->
                val graph = definition.localGraphsById.getValue(definition.regionsById.getValue(regionId).graphId)
                graph.finalChildIds.any { finalId -> active.isActive(finalId) }
            }

            else -> false
        }
    }

    private suspend fun runExitActions(stateId: I, event: E?) {
        val actions = definition.statesById[stateId]?.onExit
            ?: definition.finalStatesById[stateId]?.onExit
            ?: emptyList()
        val vertex = vertexForTarget(stateId)

        actions.sortedBy { it.ordinal }.forEach { action ->
            action.execute.invoke(
                StateChartActionScope(
                    state = vertex,
                    event = event,
                    machineContext = machineContext,
                    raiseInternalEvent = { raised -> internalEvents.addLast(RuntimeEvent.Raised(raised)) },
                    raiseTimerEvent = { timerId -> internalEvents.addLast(RuntimeEvent.Timer(timerId)) },
                    raiseDoneInvokeEvent = { invokeId -> internalEvents.addLast(RuntimeEvent.DoneInvoke(invokeId)) },
                    raiseErrorInvokeEvent = { invokeId, cause ->
                        internalEvents.addLast(RuntimeEvent.ErrorInvoke(invokeId, cause))
                    },
                    scheduleTimerEvent = scheduleTimer,
                    cancelTimerEvent = cancelTimer
                )
            )
        }
    }

    private suspend fun runEntryActions(stateId: I, event: E?) {
        val actions = definition.statesById[stateId]?.onEntry
            ?: definition.finalStatesById[stateId]?.onEntry
            ?: emptyList()
        val vertex = vertexForTarget(stateId)

        actions.sortedBy { it.ordinal }.forEach { action ->
            action.execute.invoke(
                StateChartActionScope(
                    state = vertex,
                    event = event,
                    machineContext = machineContext,
                    raiseInternalEvent = { raised -> internalEvents.addLast(RuntimeEvent.Raised(raised)) },
                    raiseTimerEvent = { timerId -> internalEvents.addLast(RuntimeEvent.Timer(timerId)) },
                    raiseDoneInvokeEvent = { invokeId -> internalEvents.addLast(RuntimeEvent.DoneInvoke(invokeId)) },
                    raiseErrorInvokeEvent = { invokeId, cause ->
                        internalEvents.addLast(RuntimeEvent.ErrorInvoke(invokeId, cause))
                    },
                    scheduleTimerEvent = scheduleTimer,
                    cancelTimerEvent = cancelTimer
                )
            )
        }
    }

    private suspend fun runTransitionActions(
        transition: TransitionDefinition<V, I, G, E>,
        target: TargetSpec<I>,
        event: E?
    ) {
        val source = vertexForTransitionSource(transition.sourceId)
        val targetIds = targetIdsFor(target)

        transition.actions.sortedBy { it.ordinal }.forEach { action ->
            action.execute.invoke(
                StateChartTransitionActionScope(
                    source = source,
                    targetIds = targetIds,
                    event = event,
                    machineContext = machineContext,
                    raiseInternalEvent = { raised -> internalEvents.addLast(RuntimeEvent.Raised(raised)) },
                    raiseTimerEvent = { timerId -> internalEvents.addLast(RuntimeEvent.Timer(timerId)) },
                    raiseDoneInvokeEvent = { invokeId -> internalEvents.addLast(RuntimeEvent.DoneInvoke(invokeId)) },
                    raiseErrorInvokeEvent = { invokeId, cause ->
                        internalEvents.addLast(RuntimeEvent.ErrorInvoke(invokeId, cause))
                    },
                    scheduleTimerEvent = scheduleTimer,
                    cancelTimerEvent = cancelTimer
                )
            )
        }
    }

    private fun activeLeafStateIds(): List<I> {
        return active.snapshotIds()
            .filter { stateId ->
                val state = definition.statesById[stateId]
                state !is CompoundStateNode && state !is ParallelStateNode
            }
            .sortedBy { stateOrdinalFor(it) }
    }

    private fun stateOrdinalFor(stateId: I): Int {
        return definition.declarationIndex.stateOrdinalById[stateId]
            ?: Int.MAX_VALUE
    }

    private fun vertexForTarget(stateId: I): V {
        return definition.statesById[stateId]?.vertex
            ?: definition.finalStatesById[stateId]?.vertex
            ?: error("No active state vertex exists for id $stateId.")
    }

    private fun vertexForTransitionSource(sourceId: I): V {
        return definition.statesById[sourceId]?.vertex
            ?: definition.pseudoNodesById[sourceId]?.vertex
            ?: error("No transition source vertex exists for id $sourceId.")
    }

    private fun targetIdsFor(target: TargetSpec<I>): List<I> {
        return when (target) {
            TargetSpec.None -> emptyList()
            is TargetSpec.Single -> listOf(target.id)
            is TargetSpec.Multiple -> target.ids
        }
    }

    private fun RuntimeEvent<E, I>.userEventOrNull(): E? {
        return when (this) {
            is RuntimeEvent.External -> event
            is RuntimeEvent.Raised -> event
            RuntimeEvent.Eventless,
            is RuntimeEvent.DoneState,
            is RuntimeEvent.DoneInvoke,
            is RuntimeEvent.ErrorInvoke,
            is RuntimeEvent.Timer -> null
        }
    }

    private fun commonPrefixLength(left: List<I>, right: List<I>): Int {
        var index = 0
        while (index < left.size && index < right.size && left[index] == right[index]) {
            index++
        }
        return index
    }

    private fun <T> List<T>.startsWith(prefix: List<T>): Boolean {
        if (prefix.size > size) {
            return false
        }

        return prefix.indices.all { index ->
            this[index] == prefix[index]
        }
    }

    private suspend fun reset(): StateChartState<V, I> {
        beginMacrostep()
        cancelAllTimers()
        cancelAllInvokes()
        (machineContext as? MachineContext)?.onReset()
        enterInitialConfiguration(runEntryActions = false, event = null)
        assertLegalConfiguration(definition, active)
        enqueueCompletionEvents()
        drainStabilizationEvents()
        assertLegalConfiguration(definition, active)
        return publishStableState()
    }

    private fun publishStableState(): StateChartState<V, I> {
        val state = createState()
        stateOut.value = state
        return state
    }

    private fun createState(): StateChartState<V, I> {
        return StateChartState(
            activeTree = activeTreeFactory.build()
        )
    }
}

private data class SelectedTransition<V, I, G, E>(
    val transition: TransitionDefinition<V, I, G, E>,
    val target: TargetSpec<I>,
    val executions: List<TransitionExecution<V, I, G, E>>,
    val exitSet: List<I>,
    val sourceDepth: Int
) where V : Vertex<I>

private data class TransitionExecution<V, I, G, E>(
    val transition: TransitionDefinition<V, I, G, E>,
    val target: TargetSpec<I>
) where V : Vertex<I>

private data class ResolvedTransitionTarget<V, I, G, E>(
    val target: TargetSpec<I>,
    val routingExecutions: List<TransitionExecution<V, I, G, E>>
) where V : Vertex<I>

private data class HistorySnapshot<I>(
    val stateIds: List<I>
)
