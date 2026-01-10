---
layout: default
title: Machine Context
---

# Machine Context

The machine context is a user-defined implementation of the `MachineContext` interface.

A state machine has a single machine-context instance per graph. This instance is shared across the state machine and is available to hooks and other builder-scoped functions that expose `machineContext`.

Machine context is optional. If no context is required, builder functions that accept a context can be omitted in favour of builder functions that do not.

## Example

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
                    if (machineContext?.isSomeTransitionBlocked == true) {
                        blockTransition()
                    }
                }
            }
        }
    }
}
```

The machine context instance is passed to the builder and then exposed through hook scopes (for example, `BeforeTransitionScope.machineContext`).

Machine context is shared state. It is commonly used for:

- Tracking information that affects multiple transitions.
- Coordinating behaviour across multiple vertices and edges.
- Holding data that is reset when the state machine is reset.

---

[← Navigate Back](../index.md)
