package mdk.gsm.state

/**
 * Transition guard states are used to share states across edges in order to persist flags and
 * data across all edge [TransitionGuard] implementations in the graph state machine.
 *
 * This is set on the graph state machine and then the same instance is made available to all edge [TransitionGuard] functions.
 * Use this to store traversal action results that constrain which edges can be traversed at any given time.
 *
 * e.g. If storing that a network call was not successful, and this should impact traversal behaviour, store that result in an implementer of this interface inside the traversal guard.
 * This will then share the result across all edges' traversal guard functions.
 *
 * @see mdk.gsm.builder.EdgeBuilderScope.setEdgeTransitionGuard
 */
interface ITransitionGuardState {

    /**
     * Called if a [GraphStateMachineAction.Reset] action is received by the state machine.
     * Use this to reset your [ITransitionGuardState] implementation if required.
     */
    fun onReset()
}
