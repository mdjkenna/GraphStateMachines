# Defining a State Machine

The following 8 vertex directed acyclic graph can be represented easily in the graph builder DSL:

<!--suppress CheckImageSize -->
<img src="../8VertexDAG.png" alt="Example Image" width="200"/>

_The above graph image was made using a dot language representation of the 8 vertex DAG in the example below and inputting this into GraphViz_ 

GraphStateMachines provides a DSL for defining vertices (states) and edges (transitions) of your state machine graph. 
Vertices must implement the `IVertex<I>` interface, and edges define the allowed transitions between states.
Edges are traversed in the order they're added unless specified otherwise with an `order` parameter.

The following example creates a `Traverser` using the 8 vertex DAG in the image above using the graph builder DSL:

```kotlin
data class Vertex(override val id: String) : IVertex<String>

fun main() {
    val one = Vertex("1")
    val two = Vertex("2")
    val three = Vertex("3")
    val four = Vertex("4")
    val five = Vertex("5")
    val six = Vertex("6")
    val seven = Vertex("7")
    val eight = Vertex("8")

    val traverser = buildTraverser {
        buildGraph(one) {

            addVertex(one) {
                addEdge {
                    setTo(two)
                }

                addEdge {
                    setTo(three)
                }
            }

            addVertex(two) {
                addEdge {
                    setTo(four)
                }
            }

            addVertex(three) {
                addEdge {
                    setTo(five)
                }

                addEdge {
                    setTo(six)
                }
            }

            addVertex(four) {
                addEdge {
                    setTo(eight)
                }
            }

            addVertex(five) {
                addEdge {
                    setTo(seven)
                }
            }

            addVertex(six) {
                addEdge {
                    setTo(seven)
                }
            }

            addVertex(seven) {
                addEdge {
                    setTo(eight)
                }
            }

            addVertex(eight)
        }
    }
}
```

In this example, edges are traversed using DFS, with neighbouring edges explored in the order they are added to a vertex. 
For vertex "one", the edge to "two" will be tried first, followed by the edge to "three". 
The traversal order can also be explicitly set using the `order` parameter in `addEdge`.
Note that several factory functions exist for different use cases: `buildTraverser` (simplest, no guard state), `buildGuardedTraverser` (with guard state), and `buildTraverserWithActions` (with guard state and action arguments). The simplest form is shown here.

## Implementations of IVertex

Vertices added to the graph must implement the `IVertex<I>` interface.
The vertex id must be unique within the graph. Adding duplicate ids when building the graph results in an error.

Any valid `IVertex<I>` implementation can be used as a graph vertex.
The `id` field is of type `I`.
The library provides predefined simple vertex implementations for convenience.
Custom vertex implementations with user-defined types for `I` can also be used.

## Adding outgoing edges

Add edges to the graph as directed outgoing edges _from_ a vertex.
Once the graph is built, edges have a fixed traversal order to ensure predictable and consistent edge visitation.

Edges can be defined to vertices before those vertices have been formally added to the graph builder. 
This allows for flexibility in ordering definitions. However, all vertices must be added to the graph by the time the builder function completes to avoid an error.

---

[← Back to README](../README.md)
