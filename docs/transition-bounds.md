---
layout: default
title: Transition Bounds
---

# Transition Bounds

When transitioning through the graph a state machine can encounter a dead end.
A dead end occurs when a `Next` action is dispatched but no eligible outgoing edge can be taken from the current vertex.
A similar boundary occurs when a `Previous` action is dispatched while already at the first vertex.

These situations are represented through the `TransitionBounds` value on the published `TransitionState`.

## TransitionBounds

`TransitionBounds` is an enum with three values:

- `WithinBounds`
- `BeforeFirst`
- `BeyondLast`

A value other than `WithinBounds` is used to represent an out-of-bounds condition while still retaining the last in-bounds vertex.
The bounds value can be treated as a null-like condition where required, without losing information about where the state machine last was.

## Published State

The published state contains both the current vertex and the bounds flag:

- `TransitionState.vertex`
- `TransitionState.transitionBounds`

Convenience properties exist on `TransitionState`:

- `isWithinBounds`
- `isBeforeFirst`
- `isBeyondLast`

## BeyondLast: no eligible outgoing transition

When a `Next` action is dispatched and no eligible outgoing edge exists
the current published `TransitionState` will remain on the same vertex.

The `TransitionState` in this case will have its `transitionBounds` property updated to `TransitionBounds.BeyondLast` 
to signify that there is nowhere for the state machine to go.

## BeforeFirst: previous from the start

When a `Previous` action is dispatched from the first vertex (and there is no earlier vertex to return to), 
the state machine publishes a `TransitionState` with the current vertex unchanged

The `TransitionState` in this case will have its `transitionBounds` property updated to `TransitionBounds.BeforeFirst`.

## Explicit transition back into bounds

Walkers and traversers support an optional configuration flag that treats returning into bounds as a distinct transition.
This flag is configured through: `setExplicitTransitionIntoBounds(true)`

When enabled:

- If the current state is `BeforeFirst`, a subsequent `Next` publishes `WithinBounds` on the same vertex.
- If the current state is `BeyondLast`, a subsequent `Previous` publishes `WithinBounds` on the same vertex.

This provides a simple way to represent “uninitialized” and “finished” states as separate published states without introducing additional vertices into the graph.

## Interpreting states that are out of bounds

Workflows or anything where a series of steps reaches a state of being 

## Example

The following example demonstrates reaching `BeyondLast` on a workflow graph by explicitly blocking outgoing edges from the final vertex. It then publishes a distinct transition back into bounds.

```kotlin
data class Vertex(override val id: String) : Vertex<String>

val start = Vertex("start")
val end = Vertex("end")

val traverser = buildTraverser {
    setExplicitTransitionIntoBounds(true)
    buildGraph(start) {
        addVertex(start) {
            addEdge {
                setTo(end)
            }
        }
        addVertex(end) {
            addEdge {
                setTo(end)
                onBeforeTransition {
                    blockTransition()
                }
            }
        }
    }
}

val inBounds1 = traverser.dispatchAndAwaitResult(
    GraphStateMachineAction.Next
)

val outOfBounds = traverser.dispatchAndAwaitResult(
    GraphStateMachineAction.Next
)

val backIntoBounds = traverser.dispatchAndAwaitResult(
    GraphStateMachineAction.Previous
)
```

In this flow:

- `inBounds1.transitionBounds` is `WithinBounds`.
- `outOfBounds.transitionBounds` is `BeyondLast`.
- `backIntoBounds.transitionBounds` is `WithinBounds` and `backIntoBounds.vertex` is unchanged.

---

[← Navigate Back](../index.md)
