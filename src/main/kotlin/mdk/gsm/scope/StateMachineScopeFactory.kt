package mdk.gsm.scope

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.*
import java.util.concurrent.Executors

object StateMachineScopeFactory {
    private val StateMachineDispatcher by lazy {
        val singleThreadExecutor = Executors.newSingleThreadExecutor { r ->
            Thread(r, "GraphStateMachine-Thread-${UUID.randomUUID()}")
        }

        singleThreadExecutor.asCoroutineDispatcher()
    }

    fun newScope(dispatcher : CoroutineDispatcher? = null): CoroutineScope {
        val coroutineDispatcher = dispatcher
            ?: StateMachineDispatcher

        return CoroutineScope(coroutineDispatcher + SupervisorJob())
    }
}