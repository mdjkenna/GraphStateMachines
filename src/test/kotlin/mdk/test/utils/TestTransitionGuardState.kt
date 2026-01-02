package mdk.test.utils

import mdk.gsm.state.MachineContext

class TestTransitionGuardState : MachineContext {
    @Volatile
    var blockedGoingTo2 = false

    @Volatile
    var blockedGoingTo3 = false

    @Volatile
    var blockedGoingTo7 = false

    @Volatile
    var blockedGoingTo5 = false

    override fun onReset() {
        blockedGoingTo2 = false
        blockedGoingTo3 = false
        blockedGoingTo7 = false
        blockedGoingTo5 = false
    }
}