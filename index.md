---
layout: default
title: Home
---

# GraphStateMachines Documentation

A Kotlin library for creating state machines using directed graphs where all possible transitions between states are defined explicitly as directed edges between vertices.

[View on GitHub](https://github.com/mdjkenna/GraphStateMachines) | [Maven Central](https://central.sonatype.com/artifact/io.github.mdjkenna/graph-state-machines)

## Basics: Building State Machines

- [Defining a State Machine](docs/defining-a-state-machine.md) - Learn how to define states, vertices, and edges using the graph builder DSL.
- [Walkers and Traversers](docs/walkers-and-traversers.md) - Understand the differences between Traversers and Walkers, their use cases, and when to use each.

## Designing State Machines

- [Using Cycles](docs/cycles.md) - Implement cyclic graphs and manage infinite loops using transition guards.
- [Intermediate States](docs/intermediate-states.md) - Represent side effects as explicit states within the graph model.
- [Transition Guards](docs/transition-guards.md) - Dynamically constrain transitions using transition guards and manage shared guard state.
- [Handlers](docs/handlers.md) - Execute custom logic at specific points during state transitions.

## Using State Machines

- [Actions and Transitions](docs/actions-and-navigation.md) - Dispatch actions to control state transitions, including basic actions and actions with arguments.
- [Concurrency](docs/concurrency.md) - Understand the actor model, single-threaded execution, and coroutine scope configuration.
- [Observing State Changes](docs/observing-state-changes.md) - Observe state changes using StateFlow and integrate with Kotlin coroutines.
- [Destructuring](docs/destructuring.md) - Separate state observation from action dispatching using Kotlin destructuring syntax.

## Tools

- [Visualization](docs/visualization.md) - Generate DOT language representations and visualize state machines.
