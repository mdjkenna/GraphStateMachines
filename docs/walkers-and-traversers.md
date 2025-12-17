# Walkers and Traversers: Use Cases

The library provides two types of graph state machine: `Traverser` and `Walker`. 
The most efficient and practical choice between a `Traverser` or `Walker` depends on the use case.

## Traversers

Traversers implement standard depth-first search (DFS) which naturally includes backtracking,
meaning they backtrack through ancestor vertices to find unvisited paths.
When a traverser reaches a vertex with no valid outgoing edges, it will backtrack to find alternative paths.
They also support moving to previous states.

The history of visited states on Traversers can be accessed using the `tracePath()` method:

```kotlin
val path = traverser.tracePath()
```

This returns a list of vertices representing the traversal path, ordered from start to current.

### Considerations if using a traverser

Traversers maintain a history (breadcrumbs) to support their backtracking and bidirectional abilities.
As a result, their memory usage is not constant and increases with the traversal depth.
This is only significant in specific scenarios.   
Note that moving to previous vertices does the opposite - removing breadcrumbs from the current traversal path.

### Use cases for Traversers

Traversers are naturally suited to scenarios where DFS traversal through a state model is desired i.e. backtracking.
For example: An application wizard or workflow, navigation through screens, or a finite custom protocol for handling data validation.
The `tracePath()` method mentioned above is particularly useful for processing wizard or workflow results.

## Walkers

Walkers transition through the first available, unblocked edge.
When a walker reaches a vertex with no valid outgoing edges it simply stops as it doesn't retain breadcrumbs to support backtracking.

### Use cases for Walkers

Walkers can be a more straightforward choice if backtracking or moving to previous states is not required.
Additionally, they might be preferred when designing graphs that benefit from the straightforward nature of walker movement.  
They are suited to scenarios where many or effectively infinite transitions can occur, such as looping around a cycle indefinitely.
For example: Indefinitely running automatic tasks on the cloud / server, forward navigation through screens using cycles for back movement, ongoing tasks

| Feature       | Traversers                                       | Walkers                               |
|---------------|--------------------------------------------------|---------------------------------------|
| Direction     | Bidirectional (Next/Previous)                    | Forward-only (Next)                   |
| History       | Maintains full path history for DFS backtracking | No history                            |
| Memory Usage  | Increases with path length over time             | Constant                              |
| Cycle Support | Support is configurable in builder               | Always supported                      |
| Use Cases     | Wizards, finite workflows, undo operations       | Long-running or high throughput tasks |

The memory usage difference between `Walkers` and `Traversers` is negligible in most scenarios. It only becomes a consideration for very long-running processes or those with extremely high throughput.

## Traversers: Resetting Edge Traversal Progression

**Note this is only applicable to traversers:**

Each time a traverser arrives at a vertex, it re-evaluates outgoing edges from the beginning of their defined order. 
It does not resume from where it left off on a previous visit. 

There are two scenarios where a vertex that has already been the current state can become the current state again:
1. When the state machine revisits a vertex as part of forward traversal (a cycle)
2. When arriving at a vertex from a `Previous` action (in traversers only)

This means that cycles in the graph are potentially infinite loops by default,
requiring transition guards to break out of cycles when needed.

---

[← Back to README](../README.md)
