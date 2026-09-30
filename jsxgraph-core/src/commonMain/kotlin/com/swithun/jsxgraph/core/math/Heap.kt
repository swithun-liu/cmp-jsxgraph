/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/unused/heap.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult

internal interface HeapNode {
    val v: Double
}

internal sealed interface HeapError {
    data object EmptyHeap : HeapError
}

/**
 * Translation of the historical, unused `JXG.Math.Heap`.
 *
 * Its zero-based parent and child formulas are intentionally preserved even
 * though they do not implement a conventional binary max heap.
 */
internal class Heap<T : HeapNode> {
    internal val pq = mutableListOf<T>()
    internal var N: Int = 0
        private set

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.empty.
    internal fun empty() {
        pq.clear()
        N = 0
    }

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.insert.
    internal fun insert(node: T) {
        if (N < pq.size) {
            pq[N] = node
        } else {
            pq += node
        }
        N += 1
        fixUp(N)
    }

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.delmax.
    internal fun delmax(): GMResult<T, HeapError> {
        if (N == 0) {
            return GMResult.Err(HeapError.EmptyHeap)
        }
        exchange(0, N - 1)
        fixDown(0, N - 1)
        N -= 1
        return GMResult.Ok(pq[N])
    }

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.fixUp.
    private fun fixUp(k: Int) {
        var index = k - 1
        while (
            index > 0 &&
            pq[index / 2].v < pq[index].v
        ) {
            exchange(index / 2, index)
            index /= 2
        }
    }

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.fixDown.
    private fun fixDown(
        k: Int,
        n: Int,
    ) {
        var index = k
        while (2 * index < n) {
            var child = 2 * index
            if (
                child < n &&
                pq[child].v < pq[child + 1].v
            ) {
                child += 1
            }
            if (pq[index].v >= pq[child].v) {
                break
            }
            exchange(index, child)
            index = child
        }
    }

    // JSXGraph 1.13.3: src/unused/heap.js -> Heap.exchange.
    private fun exchange(
        first: Int,
        second: Int,
    ) {
        val temporary = pq[first]
        pq[first] = pq[second]
        pq[second] = temporary
    }
}
