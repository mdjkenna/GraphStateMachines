package mdk.gsm.statechart.runtime

import mdk.gsm.statechart.immutableStateChartSet

internal class ActiveConfiguration<I> {
    private val activeStateIds = LinkedHashSet<I>()

    fun isActive(id: I): Boolean {
        return activeStateIds.contains(id)
    }

    fun activate(id: I) {
        activeStateIds.add(id)
    }

    fun deactivate(id: I) {
        activeStateIds.remove(id)
    }

    fun clear() {
        activeStateIds.clear()
    }

    fun snapshotIds(): Set<I> {
        return immutableStateChartSet(activeStateIds)
    }
}
