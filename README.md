[![Build](https://github.com/mdjkenna/GraphStateMachines/actions/workflows/buildAndTest.yml/badge.svg)](https://github.com/mdjkenna/GraphStateMachines/actions/workflows/buildAndTest.yml)
[![codecov](https://codecov.io/gh/mdjkenna/GraphStateMachines/branch/master/graph/badge.svg)](https://codecov.io/gh/mdjkenna/GraphStateMachines)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mdjkenna/graph-state-machines.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mdjkenna/graph-state-machines)
![GitHub](https://img.shields.io/github/license/mdjkenna/GraphStateMachines)
![GitHub last commit](https://img.shields.io/github/last-commit/mdjkenna/GraphStateMachines)
[![GitHub top language](https://img.shields.io/github/languages/top/mdjkenna/GraphStateMachines.svg)](https://github.com/mdjkenna/GraphStateMachines)

# GraphStateMachines

This is a Kotlin library for creating state machines using directed graphs where all the possible transitions between states are defined explicitly as directed edges between vertices.

State transitions are defined declaratively through a domain specific language. 
This structural foundation offers several advantages:

*   **Validation:** Invalid transitions are implicitly prevented by the absence of an edge in the graph.
*   **Declarative State Modeling:** The Kotlin DSL avoids complex procedural code that can be difficult to maintain.
*   **Visualization and Communication:** Generate DOT language representations of state machines to visualize and verify possible state transitions.
*   **Flexibility:** The library's features offer flexible ways to structure state machines and customize their behavior.
*   **Focus:** A focused implementation that does not add transitive dependencies to your project

## Features at a Glance

-   **Configurable traversal through the graph model:** Different strategies for transitioning through the graph structure are available.
-   **Supports transitioning to previous states:** Maintain full path history to enable bidirectional transitions through visited states.
-   **Cycles and conditional transitions through the graph are fully supported:** Graphs can contain cycles and transition guards can be used to dynamically control transitioning through cyclic paths.
-   **Effects are supported within the state model itself:** Intermediate states allow side effects to be represented as explicit vertices within the graph.
-   **Action arguments can be dispatched to a state machine allowing for conditional transition decisions:** Actions support custom arguments accessible to transition guards and handlers for dynamic, context-aware transitions.
-   **Predictable and atomic state transitions via a single-threaded actor model:** State machines process actions sequentially on a coroutine scope with a single-threaded dispatcher. Optionally bring your own scope to integrate the state machine with your own structured concurrency needs such as application lifecycle.

There are currently two types of state machine in the library: `Traverser` and `Walker`. Each transitions through the graph differently, with its own set of advantages.

## Installation

Add Maven Central and then add the package as a dependency. Replace `<version>` with the latest version.

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mdjkenna:graph-state-machines:<version>")
}
```

## Quick Start

This section demonstrates the definition and usage of a graph state machine.

### 1. Defining States

States are represented as vertices within the graph, implementing the `IVertex` interface.

```kotlin
data class Vertex(override val id: String) : IVertex<String>

val A = Vertex("A")
val B = Vertex("B")
val C = Vertex("C")
val D = Vertex("D")
```

### 2. Building Traversers

A `Traverser` maintains a history of visited vertices, enabling transitions both forward and backward through the graph.

Traversers implement Depth-First Search. When a vertex with no valid outgoing edges is reached, 
the traverser backtracks through ancestor vertices to find unvisited paths.

In this example, the `Traverser` transitions from `A` to `B`, and subsequently to `D`.

```kotlin
fun main() = runBlocking {
    val traverser = buildTraverser {
        buildGraph(A) {
            addVertex(A) {
                addEdge {
                    setTo(B)
                }

                addEdge {
                    setTo(C)
                }
            }

            addVertex(B) {
                addEdge {
                    setTo(D)
                }
            }

            addVertex(C) {
                addEdge {
                    setTo(D)
                }
            }

            addVertex(D)
        }
    }

    val stateAfterFirst = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)
    val finalState = traverser.dispatchAndAwaitResult(GraphStateMachineAction.Next)

    println("Traverser ended at: ${finalState.vertex.id}")
    println("Traverser path: ${finalState.path.map { it.vertex.id }}")
}
```

### 3. Building Walkers

A `Walker` performs a graph walk, transitioning through the first available valid edge at each vertex without retaining history.

When a walker reaches a vertex with no valid outgoing edges, it stops. This behavior differs from the traverser's backtracking approach and is suited to scenarios where forward-only progression is desired, such as indefinite cycles or straightforward sequential flows.

The following example utilizes a `TransitionGuard` to conditionally determine the path.

```kotlin
data class GuardState(var isPathBUnlocked: Boolean = false) : ITransitionGuardState

fun main() = runBlocking {
    val guardState = GuardState()

    val walker = buildGuardedWalker(guardState) {
        buildGraph(A) {
            addVertex(A) {
                addEdge {
                    setTo(B)
                    withGuard {
                        isPathBUnlocked
                    }
                }

                addEdge {
                    setTo(C)
                }
            }

            addVertex(B) {
                addEdge {
                    setTo(D)
                }
            }

            addVertex(C) {
                addEdge {
                    setTo(D)
                }
            }

            addVertex(D)
        }
    }

    val firstMove = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
    println("Walker took path: ${firstMove.vertex.id}")

    walker.dispatchAndAwaitResult(GraphStateMachineAction.Reset)
    guardState.isPathBUnlocked = true

    val secondMove = walker.dispatchAndAwaitResult(GraphStateMachineAction.Next)
    println("Walker took path: ${secondMove.vertex.id}")
}
```

### 4. Graph Visualization

The library provides a `DotGenerator` capable of exporting the state machine structure to the DOT language. 
This representation can be rendered by tools such as Graphviz to produce visual diagrams.

This capability ensures that the documentation of the state machine's logic remains synchronized with its implementation, providing a reliable reference for debugging and analysis. 
With this approach naturally diagrams remain accurate as the state machine evolves.

#### Basic Export

```kotlin
val dotGraph = DotGenerator.generateDotFromMachine(traverser)
println(dotGraph)
```

The resulting output is a DOT graph definition that can be visualized using Graphviz or online DOT viewers.

#### Customization

The appearance of generated diagrams can be customized through configuration and decoration:

```kotlin
val customizedDot = DotGenerator.generateDotFromMachine(traverser) {
    decorateVertex("A", VertexDecoration(fillColor = "green"))
    decorateEdge("A", "B", EdgeDecoration(color = "blue"))
}
```

Configuration options control layout direction, edge ordering display, and other visual properties. 
Decorations allow for styling individual vertices and edges with custom descriptions and colors.

#### Use Cases

Graph visualization serves multiple purposes:

- **Documentation:** Generate accurate diagrams directly from executable code, eliminating the risk of documentation drift.
- **Debugging:** Visualize the complete state structure to identify missing transitions, unreachable states, or unintended cycles.
- **Communication:** Share visual representations with team members to verify design intent and discuss state flow behavior.
- **Verification:** Confirm that the implemented graph matches the intended design before deployment.

## Documentation

For detailed guides and API documentation, visit the [Documentation Site](https://mdjkenna.github.io/GraphStateMachines/).
