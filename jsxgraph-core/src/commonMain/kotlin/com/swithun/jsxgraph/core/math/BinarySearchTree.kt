/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/unused/bst.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.floor
import kotlin.random.Random

internal sealed interface BinarySearchTreeNode<T> {
    var N: Double
}

internal class BinarySearchTreeValueNode<T>(
    internal val item: T,
    internal var l: BinarySearchTreeNode<T>,
    internal var r: BinarySearchTreeNode<T>,
    override var N: Double,
) : BinarySearchTreeNode<T>

internal class BinarySearchTreeNilNode<T>(
    override var N: Double = 0.0,
) : BinarySearchTreeNode<T>

internal sealed interface BinarySearchTreeError {
    data object NotInitialized : BinarySearchTreeError

    data class InvalidSentinelCount(
        val count: Double,
    ) : BinarySearchTreeError
}

/**
 * Safe Kotlin boundary around the historical, unused `JXG.Math.BST`.
 *
 * The upstream node counts and randomized branch formulas are preserved,
 * including stale counts after deletion. Calls that require `init` return a
 * structured error instead of reproducing JavaScript `TypeError`s.
 */
internal class BinarySearchTree<T : Comparable<T>>(
    private val random: RandomSource =
        RandomSource { Random.nextDouble() },
) {
    internal var head: BinarySearchTreeNode<T>? = null
        private set
    internal var z: BinarySearchTreeNilNode<T>? = null
        private set
    internal var randomized: Boolean = true
        private set

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.newNode.
    private fun newNode(
        item: T,
        left: BinarySearchTreeNode<T>,
        right: BinarySearchTreeNode<T>,
        n: Double,
    ): BinarySearchTreeValueNode<T> =
        BinarySearchTreeValueNode(item, left, right, n)

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.init.
    internal fun init(randomized: Boolean? = null) {
        val nil = BinarySearchTreeNilNode<T>()
        z = nil
        head = nil
        if (randomized != null) {
            this.randomized = randomized
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.count.
    internal fun count(): GMResult<Double, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> GMResult.Ok(state.value.head.N)
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.search.
    internal fun search(
        value: T,
    ): GMResult<BinarySearchTreeNode<T>, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> GMResult.Ok(
                searchR(state.value.head, value),
            )
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.insert.
    internal fun insert(
        item: T,
    ): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                head = if (randomized) {
                    insertRandR(
                        state.value.head,
                        item,
                        state.value.nil,
                    )
                } else {
                    insertR(
                        state.value.head,
                        item,
                        state.value.nil,
                    )
                }
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.traverse.
    internal fun traverse(
        node: BinarySearchTreeNode<T>,
        visit: (BinarySearchTreeValueNode<T>) -> Unit,
    ) {
        if (node !is BinarySearchTreeValueNode) {
            return
        }
        visit(node)
        traverse(node.l, visit)
        traverse(node.r, visit)
    }

    internal fun traverse(
        visit: (BinarySearchTreeValueNode<T>) -> Unit,
    ): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                traverse(state.value.head, visit)
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.insertHead.
    internal fun insertHead(
        item: T,
    ): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                head = insertT(
                    state.value.head,
                    item,
                    state.value.nil,
                )
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.deleteNode.
    internal fun deleteNode(
        value: T,
    ): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                head = deleteR(
                    state.value.head,
                    value,
                    state.value.nil,
                )
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.join.
    internal fun join(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
    ): GMResult<BinarySearchTreeNode<T>, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> GMResult.Ok(
                join(first, second, state.value.nil),
            )
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.select.
    internal fun select(
        rank: Int,
    ): GMResult<T?, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> GMResult.Ok(
                selectR(state.value.head, rank.toDouble()),
            )
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.balance.
    internal fun balance(): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                if (!state.value.nil.N.isFinite()) {
                    return GMResult.Err(
                        BinarySearchTreeError.InvalidSentinelCount(
                            state.value.nil.N,
                        ),
                    )
                }
                head = balanceR(state.value.head)
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.show.
    internal fun show(): GMResult<Unit, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                showR(state.value.head, 0)
                GMResult.Ok(Unit)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.joinRand.
    internal fun joinRand(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
    ): GMResult<BinarySearchTreeNode<T>, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> GMResult.Ok(
                joinRand(first, second, state.value.nil),
            )
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.minimum.
    internal fun minimum(
        node: BinarySearchTreeNode<T>,
    ): BinarySearchTreeNode<T> {
        var current = node
        while (
            current is BinarySearchTreeValueNode &&
            !isNil(current.l)
        ) {
            current = current.l
        }
        return current
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.maximum.
    internal fun maximum(
        node: BinarySearchTreeNode<T>,
    ): BinarySearchTreeNode<T> {
        var current = node
        while (
            current is BinarySearchTreeValueNode &&
            !isNil(current.r)
        ) {
            current = current.r
        }
        return current
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.next.
    internal fun next(
        node: BinarySearchTreeValueNode<T>,
    ): GMResult<BinarySearchTreeNode<T>?, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                var next: BinarySearchTreeNode<T>? = null
                var current = state.value.head
                if (isNil(current)) {
                    return GMResult.Ok(current)
                }
                if (!isNil(node.r)) {
                    return GMResult.Ok(minimum(node.r))
                }
                while (current is BinarySearchTreeValueNode) {
                    if (less(node.item, current.item)) {
                        next = current
                        current = current.l
                    } else if (less(current.item, node.item)) {
                        current = current.r
                    } else {
                        break
                    }
                }
                GMResult.Ok(next)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.prev.
    internal fun prev(
        node: BinarySearchTreeValueNode<T>,
    ): GMResult<BinarySearchTreeNode<T>?, BinarySearchTreeError> =
        when (val state = state()) {
            is GMResult.Ok -> {
                var previous: BinarySearchTreeNode<T>? = null
                var current = state.value.head
                if (isNil(current)) {
                    return GMResult.Ok(current)
                }
                if (!isNil(node.l)) {
                    return GMResult.Ok(maximum(node.l))
                }
                while (current is BinarySearchTreeValueNode) {
                    if (less(node.item, current.item)) {
                        current = current.l
                    } else if (less(current.item, node.item)) {
                        previous = current
                        current = current.r
                    } else {
                        break
                    }
                }
                GMResult.Ok(previous)
            }
            is GMResult.Err -> state
        }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.fixN.
    private fun fixN(node: BinarySearchTreeNode<T>) {
        node.N = when (node) {
            is BinarySearchTreeNilNode -> Double.NaN
            is BinarySearchTreeValueNode ->
                node.l.N + node.r.N + 1.0
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.isNil.
    internal fun isNil(node: BinarySearchTreeNode<T>): Boolean =
        node is BinarySearchTreeNilNode

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.searchR.
    private fun searchR(
        node: BinarySearchTreeNode<T>,
        value: T,
    ): BinarySearchTreeNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return node
        }
        if (value == node.item) {
            return node
        }
        return if (less(value, node.item)) {
            searchR(node.l, value)
        } else {
            searchR(node.r, value)
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.insertR.
    private fun insertR(
        node: BinarySearchTreeNode<T>,
        item: T,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeValueNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return newNode(item, nil, nil, 1.0)
        }
        if (less(item, node.item)) {
            node.l = insertR(node.l, item, nil)
        } else {
            node.r = insertR(node.r, item, nil)
        }
        node.N += 1.0
        return node
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.rotR.
    private fun rotR(
        node: BinarySearchTreeValueNode<T>,
    ): BinarySearchTreeValueNode<T> {
        val left = node.l
        if (left !is BinarySearchTreeValueNode) {
            return node
        }
        node.l = left.r
        left.r = node
        fixN(node)
        fixN(left)
        return left
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.rotL.
    private fun rotL(
        node: BinarySearchTreeValueNode<T>,
    ): BinarySearchTreeValueNode<T> {
        val right = node.r
        if (right !is BinarySearchTreeValueNode) {
            return node
        }
        node.r = right.l
        right.l = node
        fixN(node)
        fixN(right)
        return right
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.insertT.
    private fun insertT(
        node: BinarySearchTreeNode<T>,
        item: T,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeValueNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return newNode(item, nil, nil, 1.0)
        }
        return if (less(item, node.item)) {
            node.l = insertT(node.l, item, nil)
            rotR(node)
        } else {
            node.r = insertT(node.r, item, nil)
            rotL(node)
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.selectR.
    private fun selectR(
        node: BinarySearchTreeNode<T>,
        rank: Double,
    ): T? {
        if (node !is BinarySearchTreeValueNode) {
            return null
        }
        val leftCount = if (isNil(node.l)) 0.0 else node.l.N
        return when {
            leftCount > rank -> selectR(node.l, rank)
            leftCount < rank ->
                selectR(node.r, rank - leftCount - 1.0)
            else -> node.item
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.partR.
    private fun partR(
        node: BinarySearchTreeNode<T>,
        rank: Double,
    ): BinarySearchTreeNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return node
        }
        val leftCount = node.l.N
        var current = node
        if (leftCount > rank) {
            current.l = partR(current.l, rank)
            current = rotR(current)
            head = current
        }
        if (leftCount < rank) {
            current.r = partR(
                current.r,
                rank - leftCount - 1.0,
            )
            current = rotL(current)
            head = current
        }
        return current
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.joinLR.
    private fun joinLR(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
    ): BinarySearchTreeNode<T> {
        if (isNil(second)) {
            return first
        }
        val root = partR(second, 0.0)
        if (root !is BinarySearchTreeValueNode) {
            return first
        }
        root.l = first
        fixN(root)
        return root
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.deleteR.
    private fun deleteR(
        node: BinarySearchTreeNode<T>,
        value: T,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return nil
        }
        val item = node.item
        var current: BinarySearchTreeNode<T> = node
        if (less(value, item)) {
            node.l = deleteR(node.l, value, nil)
        }
        if (less(item, value)) {
            node.r = deleteR(node.r, value, nil)
        }
        if (item == value) {
            current = if (randomized) {
                joinRandLR(node.l, node.r)
            } else {
                joinLR(node.l, node.r)
            }
        }
        if (isNil(current)) {
            fixN(current)
        }
        return current
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.balanceR.
    private fun balanceR(
        node: BinarySearchTreeNode<T>,
    ): BinarySearchTreeNode<T> {
        if (node.N < 2.0) {
            return node
        }
        var current = partR(node, floor(node.N / 2.0))
        if (current !is BinarySearchTreeValueNode) {
            return current
        }
        current.l = balanceR(current.l)
        current.r = balanceR(current.r)
        return current
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.insertRandR.
    private fun insertRandR(
        node: BinarySearchTreeNode<T>,
        item: T,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeValueNode<T> {
        if (node !is BinarySearchTreeValueNode) {
            return newNode(item, nil, nil, 1.0)
        }
        if (random.nextDouble() < 1.0 / (node.N + 1.0)) {
            return insertT(node, item, nil)
        }
        if (less(item, node.item)) {
            node.l = insertRandR(node.l, item, nil)
        } else {
            node.r = insertRandR(node.r, item, nil)
        }
        node.N += 1.0
        return node
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.joinRandR.
    private fun joinRandR(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeNode<T> {
        if (isNil(first)) {
            return second
        }
        if (first !is BinarySearchTreeValueNode) {
            return second
        }
        var root = insertRandR(second, first.item, nil)
        root.l = joinRand(first.l, root.l, nil)
        root.r = joinRand(first.r, root.r, nil)
        fixN(root)
        return root
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.joinRandLR.
    private fun joinRandLR(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
    ): BinarySearchTreeNode<T> {
        if (isNil(first)) {
            return second
        }
        if (isNil(second)) {
            return first
        }
        if (
            first !is BinarySearchTreeValueNode ||
            second !is BinarySearchTreeValueNode
        ) {
            return first
        }
        return if (
            random.nextDouble() /
            (1.0 / (first.N + second.N) + 1.0) < first.N
        ) {
            first.r = joinRandLR(first.r, second)
            first
        } else {
            second.l = joinRandLR(first, second.l)
            second
        }
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.printnode.
    private fun printnode(
        node: BinarySearchTreeNode<T>,
        height: Int,
    ): String {
        val item = when (node) {
            is BinarySearchTreeNilNode -> "null"
            is BinarySearchTreeValueNode -> node.item.toString()
        }
        return " ".repeat(height) + "($item,${node.N})"
    }

    // JSXGraph 1.13.3: src/unused/bst.js -> BST.showR.
    private fun showR(
        node: BinarySearchTreeNode<T>,
        height: Int,
    ) {
        if (node !is BinarySearchTreeValueNode) {
            printnode(node, height)
            return
        }
        showR(node.r, height + 1)
        printnode(node, height)
        showR(node.l, height + 1)
    }

    private fun join(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeNode<T> {
        if (isNil(second)) {
            return first
        }
        if (isNil(first)) {
            return second
        }
        if (first !is BinarySearchTreeValueNode) {
            return second
        }
        var root = second
        root = insertT(
            root,
            first.item,
            nil,
        )
        root.l = join(first.l, root.l, nil)
        root.r = join(first.r, root.r, nil)
        fixN(root)
        return root
    }

    private fun joinRand(
        first: BinarySearchTreeNode<T>,
        second: BinarySearchTreeNode<T>,
        nil: BinarySearchTreeNilNode<T>,
    ): BinarySearchTreeNode<T> =
        if (
            random.nextDouble() /
            (1.0 / (first.N + second.N) + 1.0) < first.N
        ) {
            joinRandR(first, second, nil)
        } else {
            joinRandR(second, first, nil)
        }

    private fun less(
        first: T,
        second: T,
    ): Boolean = first.compareTo(second) < 0

    private fun state():
        GMResult<TreeState<T>, BinarySearchTreeError> {
        val currentHead = head
        val nil = z
        if (currentHead == null || nil == null) {
            return GMResult.Err(BinarySearchTreeError.NotInitialized)
        }
        return GMResult.Ok(TreeState(currentHead, nil))
    }

    private data class TreeState<T>(
        val head: BinarySearchTreeNode<T>,
        val nil: BinarySearchTreeNilNode<T>,
    )
}
