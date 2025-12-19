# Transition Guards and Guard State

Transition guards can block transitions across edges based on custom runtime conditions which are defined in the state machine builder. 
Transition guards are used to dynamically constrain state transitions to a subset of those defined in the graph.
Transition guards are defined in edge builders and apply to that specific edge.

```kotlin
addEdge {
    setTo(exampleVertex)
    setEdgeTransitionGuard {
        !guardState.isExampleTransitionBlocked 
    }
}
```

Returning `false` in the transition guard function blocks exploration across its edge.
Transition guard functions have a `TransitionGuardScope` receiver, which provides data to the implementer,
such as `guardState` shown above. 

Transition guards can also access arguments passed with actions:

```kotlin
setEdgeTransitionGuard {
    args != null && args.targetId == "details-screen"
}
```

In the above example, the transition guard only allows traversal if the action arguments specify a particular target ID.
As suspend functions, transition guards can also invoke other suspend functions which might be needed to keep the current thread from being blocked, for example:

```kotlin
setEdgeTransitionGuard {
    val isAllowed = checkPermissions()
    isAllowed
}
```

## Guard State

The guard state is a user-defined implementation of the `ITransitionGuardState` interface.
There is a single instance per graph, which can be passed as a parameter into one of the builder functions.
It can also be omitted, in which case no `ITransitionGuardState` implementation is needed.

```kotlin
class GuardState(
    var isSomeTransitionBlocked: Boolean = false
) : ITransitionGuardState

val traverser = buildGuardedTraverser(GuardState()) {
    buildGraph(startVertex) {
        addVertex(startVertex) {
            addEdge {
                setTo(nextVertex)
                setEdgeTransitionGuard {
                    !guardState.isSomeTransitionBlocked
                }
            }
        }
    }
}
```

The guard state is passed to the builder function and made available to all transition guards.
The `ITransitionGuardState` instance is made available to `TransitionGuardScope` functions via their `TransitionGuardScope` receiver.

This shared state can be used to:
- Store information that affects multiple transitions
- Implement complex transition logic that depends on the history of transitions
- Share data between different parts of the state machine

---

[← Navigate Back](./index.md)
