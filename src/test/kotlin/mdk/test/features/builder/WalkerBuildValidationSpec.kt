package mdk.test.features.builder

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import mdk.gsm.builder.buildWalker
import mdk.gsm.util.IntVertex
import mdk.test.utils.TestVertex

class WalkerBuildValidationSpec : BehaviorSpec({

    Given("A walker builder with validation constraints") {

        When("A graph is built with edges referencing non-existent vertices") {
            Then("The build fails with a dangling edge validation error") {
                shouldThrow<IllegalStateException> {
                    buildWalker {
                        buildGraph(TestVertex("1")) {
                            addVertex(TestVertex("1")) {
                                addEdge {
                                    setTo(TestVertex("2"))
                                }
                            }
                        }
                    }
                }
            }
        }

        When("A graph is built with duplicate vertex IDs") {
            Then("The build fails with a duplicate vertex validation error") {
                shouldThrow<IllegalStateException> {
                    buildWalker {
                        buildGraph(TestVertex("1")) {
                            addVertex(TestVertex("1")) {
                                addEdge {
                                    setTo(TestVertex("2"))
                                }
                            }

                            addVertex(TestVertex("2"))

                            addVertex(TestVertex("1")) {
                                addEdge {
                                    setTo(TestVertex("3"))
                                }
                            }

                            addVertex(TestVertex("3"))
                        }
                    }
                }
            }
        }

        When("A graph is built without specifying traversal flags") {
            Then("The build succeeds with default flag configuration") {
                val result = runCatching {
                    buildWalker {
                        buildGraph(TestVertex("1")) {
                            addVertex(TestVertex("1")) {
                                addEdge {
                                    setTo(TestVertex("2"))
                                }
                            }

                            addVertex(TestVertex("2"))
                        }
                    }
                }
                result.isSuccess shouldBe true
            }
        }

        When("A walker is built without calling buildGraph") {
            Then("The build fails with a missing graph validation error") {
                shouldThrow<IllegalStateException> {
                    buildWalker<TestVertex, String> {}
                }
            }
        }

        When("The start vertex is changed to a non-existent vertex after graph construction") {
            Then("The build fails with a vertex not found validation error") {
                shouldThrow<IllegalStateException> {
                    buildWalker {
                        buildGraph(IntVertex(1)) {
                            addVertex(IntVertex(1)) {
                                addEdge {
                                    setTo(IntVertex(2))
                                }
                            }

                            addVertex(IntVertex(2))
                        }

                        startAtVertex(IntVertex(3))
                    }
                }
            }
        }
    }
})