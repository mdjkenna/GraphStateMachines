package mdk.test.utils

import mdk.gsm.state.MachineContext

class OutgoingTransitionTestGuardState : MachineContext {
    @Volatile
    var shouldPreventTransition = false

    override fun onReset() {
        shouldPreventTransition = false
    }
}