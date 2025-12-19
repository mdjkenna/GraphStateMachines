# Visualising State Machines

The `DotGenerator` class can generate DOT language representations of the graph state machines. 
DOT is a text-based graph description language that can be visualized with various tools.

The 8-vertex DAG shown in the [Defining a State Machine](./defining-a-state-machine.md) section was created using this feature.

## Basic Usage

Pass a traverser or walker to `DotGenerator.generateDotFromMachine()`:

```kotlin
val dotContent = DotGenerator.generateDotFromMachine(traverser)
```

## Customization

The appearance of the graph can be customized with decorations using the optional lambda:

```kotlin
val dotContent = DotGenerator.generateDotFromMachine(traverser) {
    decorateVertex("start", VertexDecoration(
        description = "Start State",
        fillColor = "green"
    ))
    decorateEdge("start", "processing", EdgeDecoration(
        description = "Begin Processing",
        color = "blue"
    ))
}
```

Decoration classes allow styling of vertices, edges, and transition guards. 
For advanced customization, refer to DOT language documentation.

## Visualization

Once generated, the state machine can be visualized using:
- Graphviz (used for the example at the top of this README)
- Online DOT viewers
- IDE plugins
- Python or Kotlin notebooks with appropriate libraries
- Terminal tools

This visualization helps in understanding, documenting, communicating and debugging state machines by providing a clear representation of application state flow.

---

[← Navigate Back](./index.md)
