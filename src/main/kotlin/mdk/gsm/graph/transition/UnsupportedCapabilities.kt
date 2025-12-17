package mdk.gsm.graph.transition

import mdk.gsm.graph.IVertex
import mdk.gsm.util.GsmException

internal class UnsupportedPrevious<V, I, G, A> : IPreviousTransition<V, I, G, A>
        where V : IVertex<I> {
    override suspend fun movePrevious() = throw GsmException.PreviousActionUnsupported()
}

internal class UnsupportedPathTraceable<V> : IPathTraceable<V> {
    override fun tracePath(): List<V> = throw GsmException.TracePathUnsupported()
}

