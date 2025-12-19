# Observing State Changes

For both walkers and traversers, the current state is published through a `StateFlow`. A `StateFlow` is part of the standard Kotlin coroutines API. 
It is extremely flexible and there are many ways it can be consumed, such as being collected:

```kotlin
val traverser = buildTraverser<Vertex, String> {
    // graph implementation
}

scope.launch {
  traverser.current.collect { traversalState ->
        // consume state ...
    }
}
```

---

[← Navigate Back](./index.md)