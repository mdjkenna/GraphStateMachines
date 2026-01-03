package mdk.test.utils

import mdk.gsm.state.MachineContext

class OutgoingTransitionTestMachineContext : MachineContext {
    @Volatile
    var shouldPreventTransition = false

    override fun onReset() {
        shouldPreventTransition = false
    }
}