---
layout: default
title: Destructuring
---

# Destructuring

Both traversers and walkers support Kotlin's destructuring syntax, 
allowing you to separate the state reading and action dispatching capabilities.

```kotlin
val (traverserState, traverserDispatcher) = traverser

val (walkerState, walkerDispatcher) = walker
```

The above enables controlled access and can be conducive to separation of concerns:
- `TraverserState`/`WalkerState` provides read-only access to the current state via `current` StateFlow
- `TraverserDispatcher`/`WalkerDispatcher` provides methods to dispatch actions that modify state

---

[← Navigate Back](../index.md)
