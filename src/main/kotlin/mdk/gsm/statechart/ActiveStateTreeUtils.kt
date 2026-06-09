package mdk.gsm.statechart

import mdk.gsm.graph.Vertex

fun <V, I> ActiveStateTree<V, I>.containsStateId(id: I): Boolean
        where V : Vertex<I> {
    return root.containsStateId(id)
}

fun <V, I> ActiveStateTree<V, I>.activeAtomicStates(): List<V>
        where V : Vertex<I> {
    val states = ArrayList<V>()
    root.collectAtomicStates(states)
    return immutableStateChartList(states)
}

fun <V, I> ActiveStateTree<V, I>.activeStatesDepthFirst(): List<V>
        where V : Vertex<I> {
    val states = ArrayList<V>()
    root.collectStatesDepthFirst(states)
    return immutableStateChartList(states)
}

private fun <V, I> ActiveStateNode<V, I>.containsStateId(id: I): Boolean
        where V : Vertex<I> {
    if (vertex.id == id) {
        return true
    }

    return when (this) {
        is ActiveAtomicState -> false
        is ActiveFinalState -> false
        is ActiveOrState -> activeChild.containsStateId(id)
        is ActiveParallelState -> activeRegions.any { region ->
            region.activeChild.containsStateId(id)
        }
    }
}

private fun <V, I> ActiveStateNode<V, I>.collectAtomicStates(states: MutableList<V>)
        where V : Vertex<I> {
    when (this) {
        is ActiveAtomicState -> states.add(vertex)
        is ActiveFinalState -> states.add(vertex)
        is ActiveOrState -> activeChild.collectAtomicStates(states)
        is ActiveParallelState -> activeRegions.forEach { region ->
            region.activeChild.collectAtomicStates(states)
        }
    }
}

private fun <V, I> ActiveStateNode<V, I>.collectStatesDepthFirst(states: MutableList<V>)
        where V : Vertex<I> {
    states.add(vertex)

    when (this) {
        is ActiveAtomicState -> Unit
        is ActiveFinalState -> Unit
        is ActiveOrState -> activeChild.collectStatesDepthFirst(states)
        is ActiveParallelState -> activeRegions.forEach { region ->
            region.activeChild.collectStatesDepthFirst(states)
        }
    }
}
