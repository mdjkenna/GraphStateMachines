package mdk.gsm.statechart.model

data class RegionDefinition<I>(
    val id: RegionId<I>,
    val graphId: LocalGraphId,
    val declarationOrdinal: Int
)
