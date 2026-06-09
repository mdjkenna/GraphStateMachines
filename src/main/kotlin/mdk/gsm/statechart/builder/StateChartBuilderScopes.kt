@file:Suppress("unused")

package mdk.gsm.statechart.builder

import mdk.gsm.graph.Vertex
import mdk.gsm.statechart.StateChartActionHook
import mdk.gsm.statechart.StateChartBeforeTransitionHook
import mdk.gsm.statechart.StateChartBeforeTransitionScope
import mdk.gsm.statechart.StateChartGuardHook
import mdk.gsm.statechart.StateChartInvokeHook
import mdk.gsm.statechart.StateChartTransitionActionHook
import mdk.gsm.statechart.model.EntryTarget
import mdk.gsm.statechart.model.HistoryType
import mdk.gsm.statechart.model.PseudoKind
import mdk.gsm.statechart.model.TargetSpec
import mdk.gsm.statechart.model.TransitionType
import mdk.gsm.statechart.model.TriggerDefinition

@StateChartDsl
class StateChartBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>
) where V : Vertex<I> {

    fun initial(vertex: V) {
        initial(vertex.id)
    }

    fun initial(stateId: I) {
        rawChart.root.initial = EntryTarget.State(stateId)
    }

    fun initial(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawChart.root, vertex, PseudoKind.Initial, scopeConsumer)
        rawChart.root.initial = EntryTarget.Pseudo(vertex.id)
    }

    fun state(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        addState(vertex, RawStateKind.State, scopeConsumer)
    }

    fun v(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun orState(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun parallel(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val rawState = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Parallel,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = ParallelBuilderScope(rawChart, rawState)
        scopeConsumer(scope)
        rawChart.root.children.add(rawState)
    }

    fun andState(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        parallel(vertex, scopeConsumer)
    }

    fun final(vertex: V, scopeConsumer: FinalBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val rawState = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Final,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = FinalBuilderScope(rawChart, rawState)
        scopeConsumer(scope)
        rawChart.root.children.add(rawState)
    }

    fun choice(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawChart.root, vertex, PseudoKind.Choice, scopeConsumer)
    }

    fun junction(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawChart.root, vertex, PseudoKind.Junction, scopeConsumer)
    }

    fun fork(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawChart.root, vertex, PseudoKind.Fork, scopeConsumer)
    }

    fun join(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawChart.root, vertex, PseudoKind.Join, scopeConsumer)
    }

    private fun addState(
        vertex: V,
        kind: RawStateKind,
        scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit
    ) {
        val rawState = RawState<V, I, G, E>(
            vertex = vertex,
            kind = kind,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = StateBuilderScope(rawChart, rawState)
        scopeConsumer(scope)
        rawChart.root.children.add(rawState)
    }
}

@StateChartDsl
class StateBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>,
    private val rawState: RawState<V, I, G, E>
) where V : Vertex<I> {

    fun initial(vertex: V) {
        initial(vertex.id)
    }

    fun initial(stateId: I) {
        rawState.childGraph.initial = EntryTarget.State(stateId)
    }

    fun initial(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawState.childGraph, vertex, PseudoKind.Initial, scopeConsumer)
        rawState.childGraph.initial = EntryTarget.Pseudo(vertex.id)
    }

    fun state(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        addChildState(vertex, RawStateKind.State, scopeConsumer)
    }

    fun v(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun orState(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun parallel(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Parallel,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = ParallelBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawState.childGraph.children.add(child)
    }

    fun andState(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        parallel(vertex, scopeConsumer)
    }

    fun final(vertex: V, scopeConsumer: FinalBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Final,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = FinalBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawState.childGraph.children.add(child)
    }

    fun choice(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawState.childGraph, vertex, PseudoKind.Choice, scopeConsumer)
    }

    fun junction(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawState.childGraph, vertex, PseudoKind.Junction, scopeConsumer)
    }

    fun fork(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawState.childGraph, vertex, PseudoKind.Fork, scopeConsumer)
    }

    fun join(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawState.childGraph, vertex, PseudoKind.Join, scopeConsumer)
    }

    fun transition(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Exact(on), autoOrder, transitionBuilder)
    }

    fun transition(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Predicate(whenEvent), autoOrder, transitionBuilder)
    }

    fun transition(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Eventless, autoOrder, transitionBuilder)
    }

    fun onDone(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.DoneState(rawState.vertex.id), autoOrder, transitionBuilder)
    }

    fun transitionOnDone(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        onDone(autoOrder, transitionBuilder)
    }

    fun after(
        timerId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Timer(timerId), autoOrder, transitionBuilder)
    }

    fun after(
        delayMillis: Long,
        timerId: String = "${rawState.vertex.id}.after.${rawState.transitions.size}",
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Timer(timerId, delayMillis), autoOrder, transitionBuilder)
    }

    fun onDoneInvoke(
        invokeId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.DoneInvoke(invokeId), autoOrder, transitionBuilder)
    }

    fun onErrorInvoke(
        invokeId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.ErrorInvoke(invokeId), autoOrder, transitionBuilder)
    }

    fun e(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(on, autoOrder, transitionBuilder)
    }

    fun e(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(whenEvent, autoOrder, transitionBuilder)
    }

    fun e(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(autoOrder, transitionBuilder)
    }

    fun history(
        vertex: V,
        type: HistoryType = HistoryType.Deep,
        scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}
    ) {
        val scope = HistoryBuilderScope<V, I>()
        scopeConsumer(scope)
        rawState.histories.add(
            RawHistory(
                vertex = vertex,
                ownerStateId = rawState.vertex.id,
                type = type,
                defaultTarget = scope.defaultTarget,
                declarationOrdinal = rawChart.nextOrdinal()
            )
        )
    }

    fun hStar(vertex: V, scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}) {
        history(vertex, HistoryType.Deep, scopeConsumer)
    }

    fun h(vertex: V, scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}) {
        history(vertex, HistoryType.Shallow, scopeConsumer)
    }

    fun onEntry(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onEntry.add(RawAction(rawState.onEntry.size, handler))
    }

    fun onExit(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onExit.add(RawAction(rawState.onExit.size, handler))
    }


    fun invoke(invokeId: String, handler: StateChartInvokeHook<G, E>) {
        rawState.invokes.add(RawInvoke(invokeId, rawState.invokes.size, handler))
    }

    private fun addChildState(
        vertex: V,
        kind: RawStateKind,
        scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit
    ) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = kind,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = StateBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawState.childGraph.children.add(child)
    }

    private fun addTransition(
        trigger: TriggerDefinition<E, I>,
        autoOrder: Boolean,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addRawTransition(rawChart, rawState.vertex.id, rawState.transitions, trigger, autoOrder, null, transitionBuilder)
    }
}

@StateChartDsl
class ParallelBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>,
    private val rawState: RawState<V, I, G, E>
) where V : Vertex<I> {

    fun region(name: String, scopeConsumer: RegionBuilderScope<V, I, G, E>.() -> Unit) {
        val region = RawRegion<V, I, G, E>(
            name = name,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = RegionBuilderScope(rawChart, region)
        scopeConsumer(scope)
        rawState.regions.add(region)
    }

    fun transition(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Exact(on), autoOrder, transitionBuilder)
    }

    fun transition(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Predicate(whenEvent), autoOrder, transitionBuilder)
    }

    fun transition(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Eventless, autoOrder, transitionBuilder)
    }

    fun onDone(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.DoneState(rawState.vertex.id), autoOrder, transitionBuilder)
    }

    fun transitionOnDone(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        onDone(autoOrder, transitionBuilder)
    }

    fun after(
        timerId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Timer(timerId), autoOrder, transitionBuilder)
    }

    fun after(
        delayMillis: Long,
        timerId: String = "${rawState.vertex.id}.after.${rawState.transitions.size}",
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Timer(timerId, delayMillis), autoOrder, transitionBuilder)
    }

    fun onDoneInvoke(
        invokeId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.DoneInvoke(invokeId), autoOrder, transitionBuilder)
    }

    fun onErrorInvoke(
        invokeId: String,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.ErrorInvoke(invokeId), autoOrder, transitionBuilder)
    }

    fun e(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(on, autoOrder, transitionBuilder)
    }

    fun e(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(whenEvent, autoOrder, transitionBuilder)
    }

    fun e(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(autoOrder, transitionBuilder)
    }

    fun history(
        vertex: V,
        type: HistoryType = HistoryType.Deep,
        scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}
    ) {
        val scope = HistoryBuilderScope<V, I>()
        scopeConsumer(scope)
        rawState.histories.add(
            RawHistory(
                vertex = vertex,
                ownerStateId = rawState.vertex.id,
                type = type,
                defaultTarget = scope.defaultTarget,
                declarationOrdinal = rawChart.nextOrdinal()
            )
        )
    }

    fun hStar(vertex: V, scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}) {
        history(vertex, HistoryType.Deep, scopeConsumer)
    }

    fun h(vertex: V, scopeConsumer: HistoryBuilderScope<V, I>.() -> Unit = {}) {
        history(vertex, HistoryType.Shallow, scopeConsumer)
    }

    fun onEntry(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onEntry.add(RawAction(rawState.onEntry.size, handler))
    }

    fun onExit(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onExit.add(RawAction(rawState.onExit.size, handler))
    }


    fun invoke(invokeId: String, handler: StateChartInvokeHook<G, E>) {
        rawState.invokes.add(RawInvoke(invokeId, rawState.invokes.size, handler))
    }

    private fun addTransition(
        trigger: TriggerDefinition<E, I>,
        autoOrder: Boolean,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addRawTransition(rawChart, rawState.vertex.id, rawState.transitions, trigger, autoOrder, null, transitionBuilder)
    }
}

@StateChartDsl
class RegionBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>,
    private val rawRegion: RawRegion<V, I, G, E>
) where V : Vertex<I> {

    fun initial(vertex: V) {
        initial(vertex.id)
    }

    fun initial(stateId: I) {
        rawRegion.graph.initial = EntryTarget.State(stateId)
    }

    fun initial(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawRegion.graph, vertex, PseudoKind.Initial, scopeConsumer)
        rawRegion.graph.initial = EntryTarget.Pseudo(vertex.id)
    }

    fun state(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        addState(vertex, RawStateKind.State, scopeConsumer)
    }

    fun v(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun orState(vertex: V, scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit = {}) {
        state(vertex, scopeConsumer)
    }

    fun parallel(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Parallel,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = ParallelBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawRegion.graph.children.add(child)
    }

    fun andState(vertex: V, scopeConsumer: ParallelBuilderScope<V, I, G, E>.() -> Unit = {}) {
        parallel(vertex, scopeConsumer)
    }

    fun final(vertex: V, scopeConsumer: FinalBuilderScope<V, I, G, E>.() -> Unit = {}) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = RawStateKind.Final,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = FinalBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawRegion.graph.children.add(child)
    }

    fun choice(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawRegion.graph, vertex, PseudoKind.Choice, scopeConsumer)
    }

    fun junction(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawRegion.graph, vertex, PseudoKind.Junction, scopeConsumer)
    }

    fun fork(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawRegion.graph, vertex, PseudoKind.Fork, scopeConsumer)
    }

    fun join(vertex: V, scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit) {
        addPseudo(rawChart, rawRegion.graph, vertex, PseudoKind.Join, scopeConsumer)
    }

    private fun addState(
        vertex: V,
        kind: RawStateKind,
        scopeConsumer: StateBuilderScope<V, I, G, E>.() -> Unit
    ) {
        val child = RawState<V, I, G, E>(
            vertex = vertex,
            kind = kind,
            declarationOrdinal = rawChart.nextOrdinal()
        )
        val scope = StateBuilderScope(rawChart, child)
        scopeConsumer(scope)
        rawRegion.graph.children.add(child)
    }
}

@StateChartDsl
class PseudoBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>,
    private val rawPseudo: RawPseudo<V, I, G, E>
) where V : Vertex<I> {

    fun transition(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Eventless, autoOrder, null, transitionBuilder)
    }

    fun transition(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Exact(on), autoOrder, null, transitionBuilder)
    }

    fun transition(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addTransition(TriggerDefinition.Predicate(whenEvent), autoOrder, null, transitionBuilder)
    }

    fun otherwise(transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit) {
        addTransition(TriggerDefinition.Eventless, autoOrder = false, forcedOrder = Int.MAX_VALUE, transitionBuilder)
    }

    fun e(
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(autoOrder, transitionBuilder)
    }

    fun e(
        on: E,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(on, autoOrder, transitionBuilder)
    }

    fun e(
        whenEvent: (E) -> Boolean,
        autoOrder: Boolean = true,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        transition(whenEvent, autoOrder, transitionBuilder)
    }

    private fun addTransition(
        trigger: TriggerDefinition<E, I>,
        autoOrder: Boolean,
        forcedOrder: Int?,
        transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
    ) {
        addRawTransition(rawChart, rawPseudo.vertex.id, rawPseudo.transitions, trigger, autoOrder, forcedOrder, transitionBuilder)
    }
}

@StateChartDsl
class FinalBuilderScope<V, I, G, E> internal constructor(
    private val rawChart: RawStateChart<V, I, G, E>,
    private val rawState: RawState<V, I, G, E>
) where V : Vertex<I> {

    fun onEntry(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onEntry.add(RawAction(rawState.onEntry.size, handler))
    }

    fun onExit(handler: StateChartActionHook<V, I, G, E>) {
        rawState.onExit.add(RawAction(rawState.onExit.size, handler))
    }
}

@StateChartDsl
class HistoryBuilderScope<V, I> internal constructor() where V : Vertex<I> {
    internal var defaultTarget: EntryTarget<I>? = null

    fun default(scopeConsumer: HistoryDefaultBuilderScope<V, I>.() -> Unit) {
        val scope = HistoryDefaultBuilderScope<V, I>()
        scopeConsumer(scope)
        defaultTarget = scope.target
    }
}

@StateChartDsl
class HistoryDefaultBuilderScope<V, I> internal constructor() where V : Vertex<I> {
    internal var target: EntryTarget<I>? = null

    fun target(vertex: V) {
        target(vertex.id)
    }

    fun target(stateId: I) {
        target = EntryTarget.State(stateId)
    }

    fun targetHistory(historyVertex: V) {
        target = EntryTarget.History(historyVertex.id)
    }

    fun targetHistory(historyId: I) {
        target = EntryTarget.History(historyId)
    }

    fun targetPseudo(pseudoVertex: V) {
        target = EntryTarget.Pseudo(pseudoVertex.id)
    }

    fun targetPseudo(pseudoId: I) {
        target = EntryTarget.Pseudo(pseudoId)
    }

    fun setTo(vertex: V) {
        target(vertex)
    }

    fun setTo(stateId: I) {
        target(stateId)
    }
}

@StateChartDsl
class TransitionBuilderScope<V, I, G, E> internal constructor() where V : Vertex<I> {
    internal var target: TargetSpec<I> = TargetSpec.None
    internal var order: Int? = null
    internal var type: TransitionType = TransitionType.External
    internal val guards = ArrayList<RawGuard<V, I, G, E>>()
    internal val actions = ArrayList<RawTransitionAction<V, I, G, E>>()

    fun target(vertex: V) {
        target(vertex.id)
    }

    fun target(stateId: I) {
        target = TargetSpec.Single(stateId)
    }

    fun targetId(stateId: I) {
        target(stateId)
    }

    fun targetHistory(historyVertex: V) {
        target(historyVertex.id)
    }

    fun targetHistory(historyId: I) {
        target(historyId)
    }

    fun targetPseudo(pseudoVertex: V) {
        target(pseudoVertex.id)
    }

    fun targetPseudo(pseudoId: I) {
        target(pseudoId)
    }

    fun setTo(vertex: V) {
        target(vertex)
    }

    fun setTo(stateId: I) {
        target(stateId)
    }

    fun targets(vararg vertices: V) {
        target = TargetSpec.Multiple(vertices.map { it.id })
    }

    fun targetIds(vararg stateIds: I) {
        target = TargetSpec.Multiple(stateIds.toList())
    }

    fun noTarget() {
        target = TargetSpec.None
    }

    fun guard(handler: StateChartGuardHook<V, I, G, E>) {
        guards.add(RawGuard(guards.size, handler))
    }

    fun onBeforeTransition(handler: StateChartBeforeTransitionHook<V, I, G, E>) {
        guards.add(
            RawGuard(guards.size) {
                val scope = StateChartBeforeTransitionScope(
                    source = source,
                    event = event,
                    machineContext = machineContext
                )
                handler.invoke(scope)
                !scope.blocked
            }
        )
    }

    fun action(handler: StateChartTransitionActionHook<V, I, G, E>) {
        actions.add(RawTransitionAction(actions.size, handler))
    }

    fun onTransition(handler: StateChartTransitionActionHook<V, I, G, E>) {
        action(handler)
    }

    fun order(value: Int) {
        order = value
    }

    fun setOrder(value: Int) {
        order(value)
    }

    fun external() {
        type = TransitionType.External
    }

    fun internal() {
        type = TransitionType.Internal
    }
}

private fun <V, I, G, E> addPseudo(
    rawChart: RawStateChart<V, I, G, E>,
    graph: RawExclusiveGraph<V, I, G, E>,
    vertex: V,
    kind: PseudoKind,
    scopeConsumer: PseudoBuilderScope<V, I, G, E>.() -> Unit
) where V : Vertex<I> {
    val pseudo = RawPseudo<V, I, G, E>(
        vertex = vertex,
        kind = kind,
        declarationOrdinal = rawChart.nextOrdinal()
    )
    val scope = PseudoBuilderScope(rawChart, pseudo)
    scopeConsumer(scope)
    graph.pseudos.add(pseudo)
}

private fun <V, I, G, E> addRawTransition(
    rawChart: RawStateChart<V, I, G, E>,
    sourceId: I,
    transitions: MutableList<RawTransition<V, I, G, E>>,
    trigger: TriggerDefinition<E, I>,
    autoOrder: Boolean,
    forcedOrder: Int?,
    transitionBuilder: TransitionBuilderScope<V, I, G, E>.() -> Unit
) where V : Vertex<I> {
    val builderScope = TransitionBuilderScope<V, I, G, E>()
    transitionBuilder(builderScope)

    val order = when {
        builderScope.order != null -> builderScope.order!!
        forcedOrder != null -> forcedOrder
        autoOrder -> transitions.size
        else -> 0
    }

    transitions.add(
        RawTransition(
            sourceId = sourceId,
            trigger = trigger,
            target = builderScope.target,
            guards = builderScope.guards.toList(),
            actions = builderScope.actions.toList(),
            type = builderScope.type,
            order = order,
            declarationOrdinal = rawChart.nextOrdinal()
        )
    )
}
