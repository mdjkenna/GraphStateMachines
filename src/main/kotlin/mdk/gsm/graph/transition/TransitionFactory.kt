package mdk.gsm.graph.transition

import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.transition.traverse.AcyclicDfsTraversal
import mdk.gsm.graph.transition.traverse.CyclicDfsGraphTraversal
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.graph.transition.walk.StatelessGraphWalk

internal object TransitionFactory {
    fun <V, I, G, A> create(
        graph: Graph<V, I, G, A>,
        startVertex: V,
        useStatelessWalk: Boolean,
        traversalType: EdgeTraversalType = EdgeTraversalType.DFSAcyclic
    ): TransitionCapabilities<V, I, G, A> where V : IVertex<I> {
        return if (useStatelessWalk) {
            val engine = StatelessGraphWalk<V, I, G, A>(graph, startVertex)
            TransitionCapabilities(
                forward = engine,
                previous = UnsupportedPrevious(),
                resettable = engine,
                pathTraceable = UnsupportedPathTraceable()
            )
        } else if (traversalType == EdgeTraversalType.DFSCyclic) {
            val engine = CyclicDfsGraphTraversal<V, I, G, A>(graph, startVertex)
            TransitionCapabilities(
                forward = engine,
                previous = engine,
                resettable = engine,
                pathTraceable = engine
            )
        } else {
            val engine = AcyclicDfsTraversal<V, I, G, A>(graph, startVertex)
            TransitionCapabilities(
                forward = engine,
                previous = engine,
                resettable = engine,
                pathTraceable = engine
            )
        }
    }
}