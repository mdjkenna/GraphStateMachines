---
layout: default
title: Concurrency
---
# Concurrency

Both `Traversers` and `Walkers` use an actor model, processing dispatched actions sequentially on a single-threaded event loop.

## Single-Threaded Execution

The graph state machine operates on a coroutine scope with a single-threaded dispatcher.
All user-defined hooks (before transition hooks, before entry hooks, before exit hooks) are suspend functions which are invoked on this same thread, 
providing several benefits.

For convenience there is a default coroutine scope created per state machine instance as a default parameter. 
This can be replaced with a user provided coroutine scope. This might be preferable depending on the general context.

It is important to note that all default coroutine scopes created by the library will share the same underlying single threaded ``Dispatcher`` 
lazily created by the library.

Like any event loop thread, the main thread of the state machine should ideally not be blocked. 
This will prevent issues if sharing the thread or using a scope which is backed by a main application thread for example.

Hooks can await long-running operations on a different scope using core coroutine utilities. 
The use case will dictate the best course of action here, however.

In summary, when actions are dispatched to the state machine:
The state machine processes actions atomically, completing each action to a complete result before beginning to process the subsequent action.

## StateFlow for State Updates

The current state is published through a `StateFlow`. 
This is part of the core Kotlin coroutines library. There are many ways to consume this type of object with a typical example shown below:

```kotlin
scope.launch {
  traverser.current.collect { traversalState ->
        updateUI(traversalState.vertex)
    }
}
```

## GraphStateMachineScopeFactory

As mentioned above, the underlying coroutine scope and source of the state machine's "main-thread" of event loop thread is fully configurable.
The `GraphStateMachineScopeFactory` provides a factory method to create a new `CoroutineScope` with the appropriate single-threaded dispatcher:

```kotlin
val scope = GraphStateMachineScopeFactory.newScope()
```

Each `Traverser` or `Walker` instance must have its own separate scope, 
but the underlying dispatcher can be shared across multiple instances, allowing them to operate on the same thread if needed, such as an application main thread. 

The factory provides a convenient default configuration with a single-threaded dispatcher. 
Note all the default scopes created using this factory share the same underlying single-threaded dispatcher.

IF YOU USE YOUR OWN CUSTOM COROUTINE SCOPE, ENSURE THE UNDERLYING DISPATCHER IS SINGLE THREADED

---

[← Navigate Back](../index.md)
