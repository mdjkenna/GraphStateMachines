package mdk.gsm.statechart.model

sealed interface LocalGraphOwner<out I> {
    data object Root : LocalGraphOwner<Nothing>

    data class CompoundState<I>(val stateId: I) : LocalGraphOwner<I>

    data class Region<I>(val regionId: RegionId<I>) : LocalGraphOwner<I>
}

data class LocalStateGraph<I>(
    val id: LocalGraphId,
    val owner: LocalGraphOwner<I>,
    val initial: EntryTarget<I>,
    val childIds: List<I>,
    val finalChildIds: Set<I>,
    val pseudoChildIds: Set<I>,
    val declarationOrdinal: Int
)

sealed interface EntryTarget<out I> {
    data class State<I>(val stateId: I) : EntryTarget<I>

    data class History<I>(val historyId: I) : EntryTarget<I>

    data class Pseudo<I>(val pseudoId: I) : EntryTarget<I>
}
