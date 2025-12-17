package mdk.test.utils

import io.kotest.matchers.shouldBe
import mdk.gsm.graph.IVertex
import mdk.gsm.state.traverser.Traverser
import mdk.gsm.state.traverser.TraverserState

object AssertionUtils {
    fun <V, I, G, A> assertTracedPathWithCurrentState(
        expectedPath: List<I>,
        traverser: TraverserState<V, I, G, A>
    ) where V : IVertex<I> {
        traverser.tracePath().map { it.id } shouldBe expectedPath
        traverser.current.value.vertex.id shouldBe expectedPath.last()
    }

    fun <V : IVertex<I>, I, G, A> assertBounds(
        traverser: Traverser<V, I, G, A>,
        within: Boolean,
        beyond: Boolean,
        before: Boolean
    ) {

        val currentState = traverser.current.value
        currentState.isWithinBounds shouldBe within
        currentState.isBeyondLast shouldBe beyond
        currentState.isBeforeFirst shouldBe before
        currentState.isNotBeforeFirst shouldBe !before
        currentState.isNotBeyondLast shouldBe !beyond
    }

    suspend fun <V, I, G, A> assertExpectedPathGoingNextUntilEnd(
        traverser: Traverser<V, I, G, A>,
        expectedPath: List<I>
    ) where V : IVertex<I> {
        traverser.goNextAndRecordPublishedStatesUntilEnd().map { state ->
            state.vertex.id
        } shouldBe expectedPath
    }

    suspend fun <V, I, G, A> assertExpectedPathGoingPreviousUntilStart(
        traverser: Traverser<V, I, G, A>,
        expectedPath : List<I>,
    ) where V : IVertex<I> {
        traverser.goPreviousAndRecordPublishedStatesUntilStart().map { state ->
            state.vertex.id
        } shouldBe expectedPath
    }
}
