# Actions and Navigation

## Basic Actions

Actions are dispatched to a state machine and usually cause state transitions.

Traversers accept actions for: 

1. Transitioning to the next state (next vertex in the graph) as defined by DFS traversal - `Next`

2. Transitioning to the previous published state which they were in - `Previous`

3. Resetting to start again as if just initialised - `Reset`

Walkers accept actions for: 

1. Transitioning to the next state (next vertex in the graph) as defined by a simple graph walk with greedy edge selection - `Next`

2. Resetting to start again as if just initialised - `Reset`

Walkers do not accept `Previous` actions as they do not retain a history of visited states.

Below are examples of dispatching actions for a `Traverser`. Walkers have identical options minus `Previous` actions.

```kotlin
// Asynchronous dispatch without waiting (fire and forget)
traverser.launchDispatch(GraphStateMachineAction.Next)
traverser.launchDispatch(GraphStateMachineAction.Previous)
traverser.launchDispatch(GraphStateMachineAction.Reset)

// Suspend until the action is received (but don't wait for completion)
scope.launch {
    traverser.dispatch(GraphStateMachineAction.Next)
    traverser.dispatch(GraphStateMachineAction.Previous)
    traverser.dispatch(GraphStateMachineAction.Reset)
}

// Dispatch and await the new state
scope.launch {
    val result = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
}
```

## Dispatch Suspension Behavior

GraphStateMachines integrates directly with the Kotlin coroutines API. 
Internally, actions are sent to a coroutines `Channel` and processed sequentially (actor-style) on the state machine's single-threaded scope.

That means the suspension and throughput behavior of `dispatch` is the same as `Channel.send`. The state machine surfaces this through `DispatcherConfig`.

For the detailed reference on channels, see the Kotlin coroutines documentation:

- **Channel**: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-channel/
- **BufferOverflow**: https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.channels/-buffer-overflow/

### When `dispatch` Suspends

`dispatch` suspends only until the action is accepted by the channel. It does not wait for the state machine to finish processing the action.

In practice, there are two distinct causes of suspension:

- **No buffering (`Channel.RENDEZVOUS`)**: every `dispatch` suspends until the state machine's event loop receives the action.
- **Bounded buffering (`Channel.BUFFERED` or a positive capacity) with `BufferOverflow.SUSPEND`**: `dispatch` runs without suspension until the buffer fills, then suspends to apply backpressure.

The default configuration (`Channel.BUFFERED` with `BufferOverflow.SUSPEND`) is based on the default values for coroutine channels.
The use case and context need consideration to optimise these values. For example - `Channel.CONFLATED` will often be more appropriate depending on the context.
Referring to the Kotlin coroutines API is essential here.

### Customizing Channel Behavior

The Kotlin Coroutines API is used to configure how actions are consumed for a state machine. 

The properties used for building the coroutines `channel` used internally by the state machine are surfaced via the `DispatcherConfig` parameter.
These properties are used to configure the behaviour of the state machine in a transparent manner.
Below is an illustration of a different action consuming behaviour where all producers will suspend if the state machine is processing an action.

```kotlin

val traverser = buildTraverser(
    dispatcherConfig = DispatcherConfig(
        capacity = Channel.RENDEZVOUS,
        onBufferOverflow = BufferOverflow.SUSPEND,
    )
) {
    buildGraph(startVertex) {
        // graph definition
    }
}
```


### Comparison of Dispatch Methods

| Method | Suspension Behavior|
|--------|---------------------|
| `launchDispatch` | Never suspends the caller (fire and forget)
| `dispatch` | Suspends if the buffer is full and it is configured to during buffer overflow
| `dispatchAndAwaitResult` | Suspends until the result of the specific action sent by the caller can be returned

## Actions with Arguments

The `NextArgs` action allows you to pass contextual data along with a transition request. This enables dynamic transitions where the path through the graph depends on runtime values.

Arguments can be any type. The argument type is specified as a type parameter when building:

```kotlin
data class ActionArgs(val targetId: String, val payload: Any? = null)

val traverser = buildTraverserWithActions<Vertex, String, GuardState, ActionArgs>(guardState) {
    buildGraph(startVertex) {
        // graph definition
    }
}
```

Dispatch using `GraphStateMachineAction.NextArgs`:

```kotlin
traverser.dispatchAndAwaitResult(
    GraphStateMachineAction.NextArgs(ActionArgs("target-a", payload = someData))
)
```

The standard `GraphStateMachineAction.Next` can still be used when arguments are not needed.

### Accessing Arguments

Arguments are accessible via the `args` property in several contexts:

- **Transition guards** (`TransitionGuardScope.args`)
- **Before-visit handlers** (`BeforeVisitScope.args`)
- **Published state** (`TransitionState.args`)

```kotlin
addVertex(vertexA) {
    addEdge {
        setTo(vertexB)
        setEdgeTransitionGuard {
            args?.targetId == "target-b"
        }
    }

    addEdge {
        setTo(vertexC)
    }
}
```

If the transition guard function returns false that edge is skipped

### Arguments and Previous Actions

When using a `Traverser`, arguments are stored with each visited state in the traversal history. 
Dispatching a `Previous` action returns the state with the arguments from the original forward transition to that vertex.

---

[← Back to README](../README.md)
