package mdk.gsm.statechart.model

import mdk.gsm.graph.Vertex

data class StateChartDefinition<V, I, G, E>(
    val rootGraphId: LocalGraphId,
    val localGraphsById: Map<LocalGraphId, LocalStateGraph<I>>,
    val statesById: Map<I, StateNode<V, I, G, E>>,
    val finalStatesById: Map<I, FinalStateNode<V, I, G, E>>,
    val historyMarkersById: Map<I, HistoryMarker<V, I>>,
    val pseudoNodesById: Map<I, PseudoNode<V, I, G, E>>,
    val regionsById: Map<RegionId<I>, RegionDefinition<I>>,
    val transitionsById: Map<TransitionId, TransitionDefinition<V, I, G, E>>,
    val transitionsBySourceId: Map<I, List<TransitionId>>,
    val invokesByStateId: Map<I, List<InvokeDefinition<G, E>>>,
    val targetIndex: TargetIndex<V, I, G, E>,
    val containmentIndex: ContainmentIndex<I>,
    val entryIndex: EntryIndex<I>,
    val declarationIndex: DeclarationIndex<I>
) where V : Vertex<I>
