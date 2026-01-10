---
layout: default
title: Home
---

# GraphStateMachines Documentation

A Kotlin library for creating state machines using directed graphs where all possible transitions between states are defined explicitly as directed edges between vertices.

[View on GitHub](https://github.com/mdjkenna/GraphStateMachines) | [Maven Central](https://central.sonatype.com/artifact/io.github.mdjkenna/graph-state-machines)

## Building State Machines

- [Defining a State Machine](docs/defining-a-state-machine.md) - Learn how to define states, vertices, and edges using the graph builder DSL.
- [Walkers and Traversers](docs/walkers-and-traversers.md) - Understand the differences between Traversers and Walkers, their use cases, and when to use each.

## Designing State Machines

- [Hooks, Effects, and Intermediate States](docs/hooks-and-intermediate-states.md) - Hook timing and control, intermediate states, and how effects and control flow are modelled.
- [Using Cycles](docs/cycles.md) - Use cyclic graphs with traversers and control loop behaviour using edge constraints.
- [Transition Bounds](docs/transition-bounds.md) - Boundary states for `Next` and `Previous` when no transition is possible.
- [Machine Context](docs/machine-context.md) - Shared state via a user-defined `MachineContext`.

## Using State Machines

- [Actions and Transitions](docs/actions-and-navigation.md) - Dispatch actions to control state transitions, including basic actions and actions with arguments.
- [Concurrency](docs/concurrency.md) - Understand the actor model, single-threaded execution, and coroutine scope configuration.
- [Observing State Changes](docs/observing-state-changes.md) - Observe state changes using StateFlow and integrate with Kotlin coroutines.
- [Destructuring](docs/destructuring.md) - Separate state observation from action dispatching using Kotlin destructuring syntax.

## Tools

- [Visualization](docs/visualization.md) - Generate DOT language representations and visualize state machines.
