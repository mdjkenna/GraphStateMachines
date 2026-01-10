package mdk.gsm.state

import mdk.gsm.graph.Graph
import mdk.gsm.graph.Vertex
import org.jetbrains.annotations.ApiStatus

@ApiStatus.Internal
interface GraphSupplier<V, I, G, A> where V : Vertex<I> {
   val graph: Graph<V, I, G, A>
}
