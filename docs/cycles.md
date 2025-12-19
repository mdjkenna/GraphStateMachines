# Using Cycles

The graph can contain any number of cycles and these are supported.
When using a `Walker` cycles are always followed.
When using a `Traverser` cycles are ignored by default but can be traversed by setting the traversal type to: `EdgeTraversalType.DFSCyclic` in the traverser builder. 

There are two points to consider when designing a `Traverser` on a graph with cycles:

- **Edge Index Reset**: When the traverser arrives at a vertex, it resets that vertex's edge index to zero.
  Even if the `Traverser` previously left that vertex via edge 0, or edge 1 - ∞, it will attempt to traverse edge 0 again upon revisiting the vertex.

- **Infinite Loops**: As a result of the above point - cycles can create infinite loops. 
  To avoid infinite loops through cycles, the user must coordinate cycle behavior using transition guards to break cycles as needed. 
  This offers full control without the library getting in the way of required behaviour, allowing state machines to loop through cycles as often as needed.

Here's a simple example of using a transition guard to limit the number of times a cycle is taken:

```kotlin
class GuardState(
    var cycleCount: Int = 0
) : ITransitionGuardState {
    override fun onReset() {
        cycleCount = 0
    }
}

buildGuardedTraverser(GuardState()) {
    setTraversalType(EdgeTraversalType.DFSCyclic)
    buildGraph(stateOne) {
        addVertex(stateOne) {
            addEdge {
                setTo(stateOne)
                setEdgeTransitionGuard {
                    if (guardState.cycleCount < 3) {
                        guardState.cycleCount++
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }
}
```

In this example, the vertex has an edge pointing back to itself, creating a cycle. 
The transition guard allows the cycle to be taken up to 3 times before blocking further traversal, demonstrating how to control infinite loops in cyclic graphs.

---

[← Navigate Back](./index.md)
