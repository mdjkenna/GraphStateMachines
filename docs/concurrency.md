---
layout: default
title: Concurrency
---

# Concurrency

Both `Traversers` and `Walkers` use an actor model, processing actions sequentially on a single-threaded event loop.

## Single-Threaded Execution

The graph state machine operates on a coroutine scope with a single-threaded dispatcher.
All user-defined handlers (transition guards, onBeforeVisit handlers) are suspend functions which are invoked on this same thread, providing several benefits.

A coroutine scope is generated as a default parameter when building a `Traverser` or `Walker`, but a user provided one can be included.

GraphStateMachine processes one action at a time in a sequential manner. 

When actions are dispatched to the state machine:
The state machine processes actions atomically, completing each action to a complete result before beginning to process the subsequent action.

## StateFlow for State Updates

The current state is published through a `StateFlow`

```kotlin
scope.launch {
  traverser.current.collect { traversalState ->
        updateUI(traversalState.vertex)
    }
}
```

## GraphStateMachineScopeFactory

The `GraphStateMachineScopeFactory` provides a factory method to create a new `CoroutineScope` with the appropriate single-threaded dispatcher:

```kotlin
val scope = GraphStateMachineScopeFactory.newScope()
```

Each `Traverser` or `Walker` instance must have its own separate scope, 
but the underlying dispatcher can be shared across multiple instances, allowing them to operate on the same thread if needed, such as an application main thread. 

The factory provides a convenient default configuration with a single-threaded dispatcher. 
Note all the default scopes created using this factory share the same underlying single-threaded dispatcher.

Naturally the state machine can have actions dispatched from multiple threads,
but if customising its internal coroutine scope be aware that it **must** process actions on a single thread.
---

[← Navigate Back](../index.md)
