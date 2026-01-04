package mdk.gsm.state

/**
 * Machine contexts are used to share state across transitions in order to persist flags and
 * data across all edge [BeforeTransitionHook] implementations in the graph state machine.
 *
 * This is set on the graph state machine and then the same instance is made available to all edge [BeforeTransitionHook] functions.
 * Use this to store transition decisions that depend on previous events or external state.
 *
 * e.g. If storing that a network call was not successful, and this should impact transition behaviour, store that result in an implementer of this interface.
 * This will then be accessible across all transition hooks.
 *
 * @see mdk.gsm.builder.EdgeBuilderScope.onBeforeTransition
 */
interface MachineContext {

    /**
     * Called if a [GraphStateMachineAction.Reset] action is received by the state machine.
     * Use this to reset your [MachineContext] implementation if required.
     */
    fun onReset()
}
