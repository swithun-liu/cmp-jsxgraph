/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class BinarySearchTreeTest {
    @Test
    fun deterministicOperationsMatchOfficialFixture() {
        val tree = BinarySearchTree<Int>()
        tree.init(randomized = false)
        listOf(4, 2, 6, 1, 3, 5, 7).forEach { value ->
            ok(tree.insert(value))
        }

        assertEquals(7.0, ok(tree.count()))
        assertEquals(
            listOf(
                4 to 7.0,
                2 to 3.0,
                1 to 1.0,
                3 to 1.0,
                6 to 3.0,
                5 to 1.0,
                7 to 1.0,
            ),
            preOrder(tree),
        )
        assertEquals(
            listOf(null, 1, 2, 3, 4, 5, 6, 7, null),
            (-1..7).map { rank -> ok(tree.select(rank)) },
        )

        val four = valueNode(ok(tree.search(4)))
        val one = valueNode(ok(tree.search(1)))
        val seven = valueNode(ok(tree.search(7)))
        assertEquals(4, four.item)
        assertIs<BinarySearchTreeNilNode<Int>>(ok(tree.search(99)))

        val root = assertNotNull(tree.head)
        assertEquals(1, valueNode(tree.minimum(root)).item)
        assertEquals(7, valueNode(tree.maximum(root)).item)
        assertEquals(3, valueNode(assertNotNull(ok(tree.prev(four)))).item)
        assertEquals(5, valueNode(assertNotNull(ok(tree.next(four)))).item)
        assertNull(ok(tree.prev(one)))
        assertNull(ok(tree.next(seven)))
        ok(tree.show())
    }

    @Test
    fun deletionPreservesOfficialStaleCountsAndContainsUnsafeBalance() {
        val tree = BinarySearchTree<Int>()
        tree.init(randomized = false)
        listOf(4, 2, 6, 1, 3, 5, 7).forEach { value ->
            ok(tree.insert(value))
        }

        ok(tree.deleteNode(1))

        assertEquals(7.0, ok(tree.count()))
        assertTrue(assertNotNull(tree.z).N.isNaN())
        assertEquals(
            listOf(
                4 to 7.0,
                2 to 3.0,
                3 to 1.0,
                6 to 3.0,
                5 to 1.0,
                7 to 1.0,
            ),
            preOrder(tree),
        )
        assertEquals(
            listOf(2, 3, null, 4, 5, 6, 7, null),
            (0..7).map { rank -> ok(tree.select(rank)) },
        )
        assertIs<
            GMResult.Err<BinarySearchTreeError.InvalidSentinelCount>
            >(tree.balance())
    }

    @Test
    fun balanceAndInsertHeadMatchOfficialRotations() {
        val tree = BinarySearchTree<Int>()
        tree.init(randomized = false)
        (1..7).forEach { value -> ok(tree.insert(value)) }

        assertEquals(7, depth(assertNotNull(tree.head)))
        ok(tree.balance())

        assertEquals(3, depth(assertNotNull(tree.head)))
        assertEquals(
            listOf(
                4 to 7.0,
                2 to 3.0,
                1 to 1.0,
                3 to 1.0,
                6 to 3.0,
                5 to 1.0,
                7 to 1.0,
            ),
            preOrder(tree),
        )

        val headInserted = BinarySearchTree<Int>()
        headInserted.init(randomized = false)
        listOf(2, 1, 3).forEach { value ->
            ok(headInserted.insert(value))
        }
        ok(headInserted.insertHead(4))
        assertEquals(
            listOf(4, 2, 1, 3),
            preOrder(headInserted).map(Pair<Int, Double>::first),
        )
    }

    @Test
    fun randomizedBranchesAndDeletionMatchOfficialFixture() {
        val sequence = listOf(
            0.0,
            0.9,
            0.2,
            0.8,
            0.4,
            0.7,
            0.1,
            0.6,
        )
        var randomCalls = 0
        val tree = BinarySearchTree<Int>(
            random = RandomSource {
                sequence[randomCalls++ % sequence.size]
            },
        )
        tree.init(randomized = true)
        listOf(4, 2, 6, 1, 3, 5, 7).forEach { value ->
            ok(tree.insert(value))
        }

        assertEquals(12, randomCalls)
        assertEquals(
            listOf(
                2 to 7.0,
                1 to 1.0,
                5 to 5.0,
                3 to 2.0,
                4 to 1.0,
                6 to 2.0,
                7 to 1.0,
            ),
            preOrder(tree),
        )

        ok(tree.deleteNode(2))

        assertEquals(13, randomCalls)
        assertEquals(1.0, ok(tree.count()))
        assertEquals(
            listOf(
                1 to 1.0,
                5 to 5.0,
                3 to 2.0,
                4 to 1.0,
                6 to 2.0,
                7 to 1.0,
            ),
            preOrder(tree),
        )
    }

    @Test
    fun operationsBeforeInitFailStructurally() {
        val tree = BinarySearchTree<Int>()

        assertIs<
            GMResult.Err<BinarySearchTreeError.NotInitialized>
            >(tree.count())
        assertIs<
            GMResult.Err<BinarySearchTreeError.NotInitialized>
            >(tree.search(1))
        assertIs<
            GMResult.Err<BinarySearchTreeError.NotInitialized>
            >(tree.insert(1))
        assertIs<
            GMResult.Err<BinarySearchTreeError.NotInitialized>
            >(tree.balance())
    }

    private fun preOrder(
        tree: BinarySearchTree<Int>,
    ): List<Pair<Int, Double>> = buildList {
        ok(tree.traverse { node -> add(node.item to node.N) })
    }

    private fun depth(node: BinarySearchTreeNode<Int>): Int =
        when (node) {
            is BinarySearchTreeNilNode -> 0
            is BinarySearchTreeValueNode ->
                1 + maxOf(depth(node.l), depth(node.r))
        }

    private fun valueNode(
        node: BinarySearchTreeNode<Int>,
    ): BinarySearchTreeValueNode<Int> =
        assertIs<BinarySearchTreeValueNode<Int>>(node)

    private fun <T, E> ok(result: GMResult<T, E>): T =
        when (result) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> fail("Expected Ok, got ${result.error}")
        }
}
