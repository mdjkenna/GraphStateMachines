---
layout: default
title: Hooks, Effects, and Intermediate States
---

# Hooks, Effects, and Intermediate States

Hooks are an important control primitive for these state machines.

They serve multiple purposes.

From coordinating conditional edge exploration to outputting effects, hooks are a vital control primitive for non-trivial state machine use cases.

They are implemented within the state machine builder DSL.

Hooks are scoped to individual states and transitions.

Hooks execute custom logic at specific points during state transitions and offer a means of implementing side effects and general control.

---

## Hooks Overview

### onEntry

The `onEntry` hook executes immediately after a vertex has been entered and published as the current state. 
Note this hook is invoked after state publication.

```kotlin
addVertex(Washing) {
    onEntry {
        startWashingTimer()
    }
}
```

The `onEntry` hook receives an `OnEntryScope` which provides access to the current vertex, the shared machine context, and any arguments passed with the action that triggered the transition. 
As a suspend function, `onEntry` can perform asynchronous operations without blocking the state machine's execution thread.
Note that `onEntry` is not called for the initial state when the state machine is first created.

### onBeforeExit

The `onBeforeExit` hook executes before any outgoing transitions from the current vertex are explored.

When an action is dispatched, the state machine first invokes `onBeforeExit` on the current vertex before exploring any outgoing edges.
The key control mechanism in `onBeforeExit` is **`noTransition()`**. 

Calling this function prevents the state machine from exploring any outgoing edges and keeps the current vertex as the state. 
The transition is effectively vetoed before it begins.

```kotlin
addVertex(EditingDocument) {
    onBeforeExit {
        if (machineContext?.hasUnsavedChanges == true) {
            showSaveDialog()
            noTransition()
        }
    }
}
```

The `onBeforeExit` hook receives a `BeforeExitScope` providing access to the current vertex, machine context, and action arguments. 
Like all hooks, it's a suspend function that can perform asynchronous work.

### onBeforeTransition

Unlike the vertex-level hooks, `onBeforeTransition` is attached to specific edges and executes during the transition exploration phase. 
This hook is the place for edge eligibility checks, where the validity of a particular path depends on runtime conditions, action arguments, or machine context.

When the state machine explores outgoing edges from a vertex, it invokes `onBeforeTransition` for each candidate edge before committing to that path. 
This allows edges to dynamically constrain which transitions are valid based on the current circumstances.

The control mechanism here is **`blockTransition()`**.

Calling `blockTransition()` within the transition hook function blocks exploration across its edge.
If `blockTransition()` is not called, the transition is allowed by default.

Transition hook functions have a `BeforeTransitionScope` receiver, which provides data to the implementer,
such as `machineContext` and action arguments. 

Transition hooks can also access arguments passed with actions:

```kotlin
addEdge {
    setTo(Approved)
    onBeforeTransition {
        if (args?.userRole != "admin") {
            blockTransition()
        }
    }
}
```

If `blockTransition()` is not called the transition is allowed. 
When it is blocked the state machine will explore alternative edges until it finds an available transition.
This allows individual paths to be enabled or disabled based on runtime conditions.

### onBeforeEntry

The `onBeforeEntry` hook executes immediately before a vertex is entered and published as the current state.

`onBeforeEntry` is invoked after the state machine has selected a valid outgoing edge and committed to a transition, 
but before the new vertex is published to observers.

#### Control Mechanism: autoAdvance()

The distinctive control mechanism in `onBeforeEntry` is **`autoAdvance()`**. 
Calling this function signals the state machine to skip publishing the current vertex and immediately advance to the next state. 
When a vertex calls `autoAdvance()` in its `onBeforeEntry` hook, it becomes an **intermediate state** (also known as a transient or pseudo-state).


Intermediate states allow you to model operations or side effects as explicit vertices in your graph while keeping them invisible to observers. 
The vertex is recorded in the traversal path history but never published as the current state. 
Intermediate states are skipped over when moving to previous vertices in a `Traverser`.

```kotlin
addVertex(InitiateFill) {
    onBeforeEntry {
        machineContext?.openWaterValve()
        autoAdvance()
    }
    
    addEdge {
        setTo(Washing)
    }
}
```

In this example, `InitiateFill` performs the water valve operation and immediately advances to `Washing`. 
Observers never see `InitiateFill` as the current state—they only observe `Washing`. 
However, the operation is guaranteed to execute in the correct sequence because it's explicitly modelled in the graph structure.

This is a mechanism for representing transient work as vertices without changing what is observable as the current state. 
It is used when there is value in graph-level sequencing, trace visibility, or run-to-completion semantics without publishing each operational step.
This approach effectively puts side effects into the state model. 

There is an historical precedent for this particularly in older or more low level state machine approaches where formal guarantees about operation sequences, locking, or exclusivity were required.
Intermediate states can be a powerful way of ensuring transparent guarantees about operations which can also be easily visualised as part of the wider state machine visualisation.

See the [Intermediate States: Deeper Discussion](#intermediate-states-deeper-discussion) section for a comprehensive exploration of this pattern.

The `onBeforeEntry` hook receives a `BeforeEntryScope` with access to the vertex being entered, machine context, and action arguments.

---

## Intermediate States: Deeper Discussion

As introduced in the [`onBeforeEntry`](#onbeforeentry) section, intermediate states are vertices that call `autoAdvance()` to skip publication and immediately advance to the next state. 
This is not separate machinery; it is a specific use of `onBeforeEntry`. 
The point is structural: operations are made explicit in the graph instead of being implicit inside callbacks.

Making operations vertices gives a sequencing guarantee that is hard to obtain from ad-hoc imperative code. 
If the graph says `Unlock -> Open -> Check`, then those steps execute in that order because there is no other route through the work.

It also forces exclusivity. The machine is only ever “at” one vertex at a time. 
If an operation is represented as a vertex, it cannot overlap with other vertices’ work unless the graph explicitly allows it.

Visibility and testability come from the same place: the work is part of the traversed path. 
Graph visualisation shows the operational steps, and a traverser can assert that a path included those steps without needing to couple tests to the side effects.

Maintainability is a side effect of the above. 
Reordering, inserting, or removing steps is graph editing rather than refactoring control-flow code that has grown around a sequence.

### Run-to-Completion Semantics

There is no limit to how many intermediate states can be chained together. 
The state machine can be self-driven across an interval of states for an arbitrary length of time.

The intermediate state pattern can be used to implement **run-to-completion** semantics: the state machine may move through several transient steps to perform work, 
but it must not stop until it reaches a stable, observable state.

This pattern draws inspiration from traditional control oriented approaches to state machines.

By using `autoAdvance()` in `onBeforeEntry`, you implement this same pattern: transient work vertices that execute in sequence but never appear as the published current state. 

### Example: Washing Machine with Sequenced Operations

Consider a simplified washing machine model where "Filling" and "Rinsing" are critical operations that must execute in a specific sequence. 
By modelling these as intermediate states using `autoAdvance()` in `onBeforeEntry`, the graph guarantees execution order while keeping these operational steps invisible to observers.

```kotlin
val walker = buildWalkerWithContext<Vertex, String, WashingMachineContext>(/*params*/) {
    buildGraph(Idle) {
        addVertex(Idle) {
            addEdge {
                setTo(InitiateFill)
            }
        }

        addVertex(InitiateFill) {
            onBeforeEntry {
                machineContext?.awaitOpenWaterValve()
                autoAdvance()
            }
            
            addEdge {
                setTo(Washing)
            }
        }

        addVertex(Washing) {
            addEdge {
                setTo(InitiateRinse)
            }
        }

        addVertex(InitiateRinse) {
            onBeforeEntry {
                machineContext?.awaitStartHighSpeedSpin()
                autoAdvance()
            }
            
            addEdge {
                setTo(Idle)
            }
        }
    }
}
```

In this model, an observer only sees `Idle` or `Washing`. 
However, the graph guarantees that the valve is opened and the pump is started in the correct order.

Note a traverser's `tracePath()` method will include `InitiateFill` and `InitiateRinse` in the path history, 
even though they were never published as the current state. 

This allows you to verify in tests and audits that these operations occurred in the correct sequence.

---

## Summary: Control Mechanisms in Hooks

GraphStateMachine hooks provide three primary control mechanisms:

- **`autoAdvance()`** (in `onBeforeEntry`): Skip publishing the current vertex and immediately advance to the next state, creating intermediate states for sequenced operations.
- **`noTransition()`** (in `onBeforeExit`): Prevent the state machine from exploring any outgoing edges, vetoing all transitions from the current state.
- **`blockTransition()`** (in `onBeforeTransition`): Prevent traversal along a specific edge, enabling conditional routing based on runtime conditions.

These mechanisms work together to control which transitions are eligible and which vertices are published as state. 
Intermediate states (using `autoAdvance()`) are one pattern built on top of the hook system, 
allowing operations to be represented as explicit vertices while keeping published state stable.
