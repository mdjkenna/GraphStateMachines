@file:Suppress("unused")

package mdk.gsm.builder

import mdk.gsm.graph.Graph
import mdk.gsm.graph.IVertex
import mdk.gsm.graph.transition.TransitionFactory
import mdk.gsm.graph.transition.TransitionMediator
import mdk.gsm.graph.transition.traverse.EdgeTraversalType
import mdk.gsm.state.GsmConfig
import mdk.gsm.state.GsmController

@PublishedApi
internal class GraphStateMachineBuilder<V, I, G, A> @PublishedApi internal constructor()
        where V : IVertex<I> {

    var graph : Graph<V, I, G, A>? = null
    var startVertex : V? = null
    var transitionGuardState : G? = null
    var traversalType : EdgeTraversalType = EdgeTraversalType.DFSAcyclic
    var explicitlyTransitionIntoBounds : Boolean = false
    var useStatelessWalk : Boolean = false

    @PublishedApi
    internal fun build(): GsmController<V, I, G, A> {
        val _graph = graph
        val _startVertex = startVertex
        val _transitionGuardState = this@GraphStateMachineBuilder.transitionGuardState

        check(_graph != null) {
            "The workflow graph must be defined."
        }

        check(_startVertex != null) {
            "The start vertex must be defined."
        }

        check(_graph.containsVertex(_startVertex)) {
            buildString {
                append("The graph must contain the start vertex. ")
                appendLine()
                append("The start vertex with stepId '${_startVertex.id}' does not exist in the graph.")
            }
        }

        return GsmController(
            graphTransitionMediator = TransitionMediator.create(
                capabilities = TransitionFactory.create(
                    graph = _graph,
                    startVertex = _startVertex,
                    useStatelessWalk = useStatelessWalk,
                    traversalType = traversalType
                ),
                transitionGuardState = _transitionGuardState,
                gsmConfig = GsmConfig(explicitlyTransitionIntoBounds)
            ),
            graph = _graph
        )
    }
}
