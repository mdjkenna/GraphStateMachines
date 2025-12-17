package mdk.gsm.graph.transition

import mdk.gsm.graph.IVertex
import mdk.gsm.graph.VertexContainer
import mdk.gsm.graph.transition.traverse.TraversalPathNode

internal interface IForwardTransition<V, I, G, A> where V : IVertex<I> {
    suspend fun moveNext(guardState : G?, autoAdvance: Boolean, args: A?): TraversalPathNode<V, A>?

    fun currentStep(): V

    fun getVertexContainer(id: I): VertexContainer<V, I, G, A>?

    fun head(): TraversalPathNode<V, A>
}

internal interface IPreviousTransition<V, I, G, A> where V : IVertex<I> {
    suspend fun movePrevious(): TraversalPathNode<V, A>?
}

internal interface IResettable<V> {
    fun reset(): V
}

internal interface IPathTraceable<V> {
    fun tracePath(): List<V>
}

