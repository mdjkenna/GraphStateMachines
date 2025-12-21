---
layout: default
title: Intermediate States
---

# Intermediate States

Intermediate states are "in-between" states that are not published or observed. 
They are automatically advanced through without being published as the current state.
Their main purpose is to represent side effects however they are not limited to this.

State machine libraries often contain constructs called "effects" to represent side effects which do not change the main state. 
Intermediate states are equivalent to "effects", however they are treated as a special type of state within the graph model itself.

This approach integrates effects and operations directly into the state machine's architecture in a more traditional sense, where all "operations" and "processes" including effects converge onto the state machines main state.
They are positioned within the landscape of possible states, giving them a specific context in which they can run.

- **Effects as State**: Represent effect operations as explicit states that can only occur within specific contexts.
- **Control Flow Clarity**: Make application flow visible in the graph structure itself.
- **Perform Operations with Guarantees**: Clearly guarantee particular tasks will only be executed in certain scenarios and easily visualise what those scenarios are.

The core benefit of this approach is that the *entire* behavior of the system is explicitly defined and visualized in the graph. There are no 'hidden' operations occurring between states. This leads to:
-   **Enhanced Testability**: The control flow logic of a state machine can be tested without executing actual side effects. For instance, verifying that a specific action correctly leads to the `PerformNetworkRequest` intermediate state does not require making a real network call.
-   **Improved Visibility**: When side effects are vertices in the graph, the complete flow of the application is self-documenting. This makes it easier for new developers to understand the system and for anyone to debug issues, as the graph visualization tells the whole story.
-   **Simplified Maintainability**: As application logic evolves, modifying the flow becomes a matter of rewiring the graph. Adding, removing, or reordering operations is more straightforward than refactoring complex imperative code blocks that might handle side effects outside of the state machine.

## How Intermediate States Work

When a vertex is marked as an intermediate state:

1. Just before a vertex `V1` is visited, its `onBeforeVisit` handler is executed, and the `autoAdvance` function is invoked within the `BeforeVisitScope`.
2. If using a traverser - the vertex is recorded in the traced path but never published as the current state (Walkers don't retain history).
3. The state machine immediately advances to the next state and `V1` was never published, making it an intermediate state.

Note when processing previous actions the intermediate states are skipped over.

## Creating Intermediate States

To mark a vertex as an intermediate state, call `autoAdvance()` within its `onBeforeVisit` handler:

```kotlin
addVertex(loadingState) {
    onBeforeVisit {
        showLoading()
        withContext(Dispatchers.IO) {
            diskOperation()
        }
        hideLoading()

        autoAdvance()
    }

    addEdge {
        setTo(dataLoadedState)
    }
}
```

`onBeforeVisit` is called just before a vertex will be arrived at after a successful transition, but before that vertex is published as the current state.
In this example, the loading state is marked as intermediate by calling `autoAdvance()` - advancing to the next state before publishing `loadingState`. 
As a result `loadingState` is never perceived by observers, it will immediately advance to the data loaded state once the operation completes.

Intermediate states solve common problems in a more traditional state machine oriented fashion:

- **Effect Usage For Screen State**: As in the above example, perform generic side effects or other UI updates
- **Multistep Operations and Custom Protocols**: Create chains of operations that execute in sequence without exposing intermediate steps, potentially having complex conditional paths or perpetual transitions

---

[← Navigate Back](../index.md)
