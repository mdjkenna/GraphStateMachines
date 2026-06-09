package mdk.gsm.statechart

import java.util.Collections

internal fun <T> immutableStateChartList(values: Iterable<T>): List<T> {
    return Collections.unmodifiableList(values.toList())
}

internal fun <K, V> immutableStateChartMap(values: Map<K, V>): Map<K, V> {
    return Collections.unmodifiableMap(LinkedHashMap(values))
}

internal fun <T> immutableStateChartSet(values: Iterable<T>): Set<T> {
    return Collections.unmodifiableSet(LinkedHashSet(values.toList()))
}
