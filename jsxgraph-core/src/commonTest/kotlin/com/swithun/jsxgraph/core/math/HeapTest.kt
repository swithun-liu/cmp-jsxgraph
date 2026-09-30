/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class HeapTest {
    @Test
    fun historicalIndexingAndDeletionOrderMatchOfficialFixture() {
        val heap = Heap<TestNode>()
        val nodes = listOf(
            TestNode("four", 4.0),
            TestNode("one", 1.0),
            TestNode("seven", 7.0),
            TestNode("three", 3.0),
            TestNode("nine", 9.0),
            TestNode("two", 2.0),
            TestNode("eight", 8.0),
        )
        val expectedInsertions = listOf(
            listOf(4.0),
            listOf(4.0, 1.0),
            listOf(7.0, 4.0, 1.0),
            listOf(7.0, 4.0, 1.0, 3.0),
            listOf(9.0, 7.0, 4.0, 3.0, 1.0),
            listOf(9.0, 7.0, 4.0, 3.0, 1.0, 2.0),
            listOf(9.0, 8.0, 4.0, 7.0, 1.0, 2.0, 3.0),
        )

        nodes.forEachIndexed { index, node ->
            heap.insert(node)
            assertEquals(
                expectedInsertions[index],
                heap.pq.take(heap.N).map(TestNode::v),
            )
        }

        val deleted = buildList {
            while (heap.N > 0) {
                add(
                    assertIs<GMResult.Ok<TestNode>>(
                        heap.delmax(),
                    ).value.id,
                )
            }
        }
        assertEquals(
            listOf(
                "nine",
                "two",
                "seven",
                "three",
                "eight",
                "one",
                "four",
            ),
            deleted,
        )
        assertEquals(
            listOf(4.0, 1.0, 8.0, 3.0, 7.0, 2.0, 9.0),
            heap.pq.map(TestNode::v),
        )
    }

    @Test
    fun emptyDeletionIsStructuredAndEmptyResetsStorage() {
        val heap = Heap<TestNode>()

        assertIs<GMResult.Err<HeapError.EmptyHeap>>(heap.delmax())
        assertEquals(0, heap.N)
        assertEquals(emptyList(), heap.pq)

        heap.insert(TestNode("one", 1.0))
        heap.empty()
        assertEquals(0, heap.N)
        assertEquals(emptyList(), heap.pq)
        assertIs<GMResult.Err<HeapError.EmptyHeap>>(heap.delmax())
    }

    private data class TestNode(
        val id: String,
        override val v: Double,
    ) : HeapNode
}
