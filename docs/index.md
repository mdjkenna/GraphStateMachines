---
layout: default
title: Home
---

# GraphStateMachines Documentation

A Kotlin library for creating state machines using directed graphs where all possible transitions between states are defined explicitly as directed edges between vertices.

[View on GitHub](https://github.com/mdjkenna/GraphStateMachines) | [Maven Central](https://central.sonatype.com/artifact/io.github.mdjkenna/graph-state-machines)

## Getting Started

- [Defining a State Machine](./defining-a-state-machine.md) - Learn how to define states, vertices, and edges using the graph builder DSL.
- [Walkers and Traversers](./walkers-and-traversers.md) - Understand the differences between Traversers and Walkers, their use cases, and when to use each.

## Core Concepts

- [Actions and Transitions](./actions-and-navigation.md) - Dispatch actions to control state transitions, including basic actions and actions with arguments.
- [Transition Guards](./transition-guards.md) - Dynamically constrain transitions using transition guards and manage shared guard state.
- [Handlers](./handlers.md) - Execute custom logic at specific points during state transitions.
- [Intermediate States](./intermediate-states.md) - Represent side effects as explicit states within the graph model.

## Advanced Topics

- [Using Cycles](./cycles.md) - Implement cyclic graphs and manage infinite loops using transition guards.
- [Concurrency](./concurrency.md) - Understand the actor model, single-threaded execution, and coroutine scope configuration.
- [Observing State Changes](./observing-state-changes.md) - Observe state changes using StateFlow and integrate with Kotlin coroutines.
- [Destructuring](./destructuring.md) - Separate state observation from action dispatching using Kotlin destructuring syntax.

## Tools

- [Visualization](./visualization.md) - Generate DOT language representations and visualize state machines.
