# Handling Before Incoming and Outgoing Transitions

GraphStateMachine provides `BeforeVisitHandler` and `OutgoingTransitionHandler` to execute custom logic at specific points during state transitions.

## BeforeVisitHandler - Before Arriving

`BeforeVisitHandler` executes logic immediately before a vertex is visited and published as the current state. 
It is useful for setup operations or validating preconditions before the new state is officially reached.

```kotlin
addVertex(loadingState) {
    onBeforeVisit {
        println("About to visit: ${vertex.id}")

        args?.let { arguments ->
            println("Action arguments: $arguments")
        }
    }

    addEdge {
        setTo(nextState)
    }
}
```

The `BeforeVisitHandler` receives a `BeforeVisitScope` which provides access to:
- The vertex that is about to be visited
- The shared guard state for the entire state machine
- Any arguments passed with the current action

The `BeforeVisitHandler` can call `autoAdvance()`, which signals the state machine to automatically advance to the next state without publishing the current vertex as the state, allowing for automatic progression through certain vertices.

## OutgoingTransitionHandler - Before Leaving

`OutgoingTransitionHandler` executes logic before any outgoing transitions from the current vertex are explored. 
It can prevent transitions altogether, making it ideal for conditional navigation.

```kotlin
addVertex(conditionalState) {
    onOutgoingTransition {
        println("Considering transitions from: ${vertex.id}")

        if (args?.shouldStayInCurrentState == true) {
            noTransition()
        }

        updateTransitionMetrics()
    }

    addEdge {
        setTo(nextState)
    }
}
```

The `OutgoingTransitionHandler` receives an `OutgoingTransitionScope` which provides access to:
- The current vertex from which transitions are being considered
- The shared guard state for the entire state machine  
- Any arguments passed with the current action

The `OutgoingTransitionHandler` can call `noTransition()`, which prevents the state machine from exploring any outgoing edges and keeps the current vertex as the state. 
This is particularly useful for implementing conditional logic that determines whether state transitions should occur based on runtime conditions.

Both handlers are suspend functions, allowing them to perform asynchronous operations as needed. 
They integrate seamlessly with the state machine's single-threaded actor model, ensuring predictable and atomic execution.

---

[← Back to README](../README.md)
