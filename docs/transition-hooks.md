---
layout: default
title: Transition Hooks
---

# Transition Hooks and Machine Context

Transition hooks can block transitions across edges based on custom runtime conditions which are defined in the state machine builder. 
Transition hooks are used to dynamically constrain state transitions to a subset of those defined in the graph.
Transition hooks are defined in edge builders and apply to that specific edge.

```kotlin
addEdge {
    setTo(exampleVertex)
    onBeforeTransition {
        if (machineContext.isExampleTransitionBlocked) blockTransition()
    }
}
```

Calling `blockTransition()` within the transition hook function blocks exploration across its edge.
If `blockTransition()` is not called, the transition is allowed by default.
Transition hook functions have a `TransitionHookScope` receiver, which provides data to the implementer,
such as `machineContext` shown above. 

Transition hooks can also access arguments passed with actions:

```kotlin
onBeforeTransition {
    if (args == null || args.targetId != "details-screen") blockTransition()
}
```

In the above example, the transition hook blocks traversal unless the action arguments specify a particular target ID.
As suspend functions, transition hooks can also invoke other suspend functions which might be needed to keep the current thread from being blocked, for example:

```kotlin
onBeforeTransition {
    val isAllowed = checkPermissions()
    if (!isAllowed) blockTransition()
}
```

## Machine Context

The machine context is a user-defined implementation of the `MachineContext` interface.
There is a single instance per graph, which can be passed as a parameter into one of the builder functions.
It can also be omitted, in which case no `MachineContext` implementation is needed.

```kotlin
class MachineContextFlags(
    var isSomeTransitionBlocked: Boolean = false
) : MachineContext {
    override fun onReset() {
        isSomeTransitionBlocked = false
    }
}

val traverser = buildTraverserWithContext(MachineContextFlags()) {
    buildGraph(startVertex) {
        addVertex(startVertex) {
            addEdge {
                setTo(nextVertex)
                onBeforeTransition {
                    if (machineContext.isSomeTransitionBlocked) blockTransition()
                }
            }
        }
    }
}
```

The machine context is passed to the builder function and made available to all transition hooks.
The `MachineContext` instance is made available to `TransitionHookScope` functions via their `TransitionHookScope` receiver.

This shared state can be used to:
- Store information that affects multiple transitions
- Implement complex transition logic that depends on the history of transitions
- Share data between different parts of the state machine

---

[← Navigate Back](../index.md)
