package mdk.gsm.graph.transition

import mdk.gsm.graph.Vertex
import mdk.gsm.util.GsmException

internal class UnsupportedPrevious<V, I, G, A> : IPreviousTransition<V, I, G, A>
        where V : Vertex<I> {
    override suspend fun movePrevious() = throw GsmException.PreviousActionUnsupported()
}

internal class UnsupportedPathTraceable<V> : IPathTraceable<V> {
    override fun tracePath(): List<V> = throw GsmException.TracePathUnsupported()
}

