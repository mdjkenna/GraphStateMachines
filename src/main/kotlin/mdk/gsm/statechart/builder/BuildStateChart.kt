@file:Suppress("unused")

package mdk.gsm.statechart.builder

import kotlinx.coroutines.CoroutineScope
import mdk.gsm.graph.Vertex
import mdk.gsm.scope.StateMachineScopeFactory
import mdk.gsm.statechart.StateChart
import mdk.gsm.statechart.StateChartDispatcherConfig
import mdk.gsm.statechart.StateChartRuntimeConfig
import mdk.gsm.statechart.runtime.StateChartDispatcherImplementation
import mdk.gsm.statechart.runtime.StateChartImplementation
import mdk.gsm.statechart.runtime.StateChartInvokeScheduler
import mdk.gsm.statechart.runtime.StateChartRuntime
import mdk.gsm.statechart.runtime.StateChartTimerScheduler

fun <V, I, E> buildStateChart(
    scope: CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: StateChartDispatcherConfig<E> = StateChartDispatcherConfig(),
    runtimeConfig: StateChartRuntimeConfig = StateChartRuntimeConfig(),
    builderFunction: StateChartBuilderScope<V, I, Nothing, E>.() -> Unit
): StateChart<V, I, Nothing, E> where V : Vertex<I> {
    val rawChart = RawStateChart<V, I, Nothing, E>()
    val builderScope = StateChartBuilderScope(rawChart)
    builderFunction(builderScope)

    val definition = StateChartCompiler(rawChart).compile()
    val commandChannel = dispatcherConfig.toChannel<V, I>()
    val timerScheduler = StateChartTimerScheduler<V, I, E>(scope, commandChannel)
    val invokeScheduler = StateChartInvokeScheduler<V, I, Nothing, E>(scope, commandChannel)
    val runtime = StateChartRuntime(
        definition = definition,
        machineContext = null,
        runtimeConfig = runtimeConfig,
        scheduleTimer = timerScheduler::schedule,
        cancelTimer = timerScheduler::cancel,
        cancelAllTimers = timerScheduler::cancelAll,
        startInvoke = invokeScheduler::start,
        cancelInvoke = invokeScheduler::cancel,
        cancelAllInvokes = invokeScheduler::cancelAll
    )
    val dispatcher = StateChartDispatcherImplementation.create(
        runtime = runtime,
        singleThreadedScope = scope,
        commandChannel = commandChannel,
        timerScheduler = timerScheduler,
        invokeScheduler = invokeScheduler
    )

    return StateChartImplementation(
        definition = definition,
        runtime = runtime,
        stateChartDispatcher = dispatcher
    )
}

fun <V, I, G, E> buildStateChartWithContext(
    machineContext: G,
    scope: CoroutineScope = StateMachineScopeFactory.newScope(),
    dispatcherConfig: StateChartDispatcherConfig<E> = StateChartDispatcherConfig(),
    runtimeConfig: StateChartRuntimeConfig = StateChartRuntimeConfig(),
    builderFunction: StateChartBuilderScope<V, I, G, E>.() -> Unit
): StateChart<V, I, G, E> where V : Vertex<I> {
    val rawChart = RawStateChart<V, I, G, E>()
    val builderScope = StateChartBuilderScope(rawChart)
    builderFunction(builderScope)

    val definition = StateChartCompiler(rawChart).compile()
    val commandChannel = dispatcherConfig.toChannel<V, I>()
    val timerScheduler = StateChartTimerScheduler<V, I, E>(scope, commandChannel)
    val invokeScheduler = StateChartInvokeScheduler<V, I, G, E>(scope, commandChannel)
    val runtime = StateChartRuntime(
        definition = definition,
        machineContext = machineContext,
        runtimeConfig = runtimeConfig,
        scheduleTimer = timerScheduler::schedule,
        cancelTimer = timerScheduler::cancel,
        cancelAllTimers = timerScheduler::cancelAll,
        startInvoke = invokeScheduler::start,
        cancelInvoke = invokeScheduler::cancel,
        cancelAllInvokes = invokeScheduler::cancelAll
    )
    val dispatcher = StateChartDispatcherImplementation.create(
        runtime = runtime,
        singleThreadedScope = scope,
        commandChannel = commandChannel,
        timerScheduler = timerScheduler,
        invokeScheduler = invokeScheduler
    )

    return StateChartImplementation(
        definition = definition,
        runtime = runtime,
        stateChartDispatcher = dispatcher
    )
}
