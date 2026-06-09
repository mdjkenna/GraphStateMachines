package mdk.gsm.statechart.model

@JvmInline
value class LocalGraphId(val value: Int)

@JvmInline
value class TransitionId(val value: Int)

data class RegionId<I>(
    val parallelStateId: I,
    val name: String
)
