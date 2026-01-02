package mdk.test.utils

import mdk.gsm.state.MachineContext

class Test15VertexTransitionArgs : MachineContext {
    @Volatile
    var blockedFrom3To2 = false

    @Volatile
    var blockedFrom8To9 = false

    override fun onReset() {
        blockedFrom3To2 = false
        blockedFrom8To9 = false
    }
}