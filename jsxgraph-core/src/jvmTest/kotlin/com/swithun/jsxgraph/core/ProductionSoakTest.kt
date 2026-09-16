/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.generated.ProductionCorpusCase
import com.swithun.jsxgraph.core.generated.productionCorpusCases
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProductionSoakTest {
    @Test
    fun repeatedlyRendersProductionCorpusWithinJvmBudget() {
        repeat(WARMUP_ROUNDS) {
            productionCorpusCases.forEach(::render)
        }
        forceGc()
        val memoryBefore = usedHeapBytes()
        val durations = mutableListOf<Long>()
        val startedAt = System.nanoTime()

        repeat(MEASURED_ROUNDS) {
            productionCorpusCases.forEach { case ->
                val renderStartedAt = System.nanoTime()
                render(case)
                durations += System.nanoTime() - renderStartedAt
            }
        }

        val totalMillis = nanosToMillis(System.nanoTime() - startedAt)
        forceGc()
        val retainedHeapBytes =
            (usedHeapBytes() - memoryBefore).coerceAtLeast(0L)
        val sortedDurations = durations.sorted()
        val percentileIndex = ceil(sortedDurations.size * 0.95)
            .toInt()
            .coerceIn(1, sortedDurations.size) - 1
        val p95Nanos = sortedDurations[percentileIndex]
        val p95Micros = p95Nanos / 1_000L

        println(
            "Production corpus soak: renders=${durations.size}, " +
                "totalMs=$totalMillis, p95Micros=$p95Micros, " +
                "retainedHeapBytes=$retainedHeapBytes",
        )
        assertTrue(
            totalMillis <= MAX_TOTAL_MILLIS,
            "Production corpus took ${totalMillis}ms; " +
                "budget is ${MAX_TOTAL_MILLIS}ms",
        )
        assertTrue(
            p95Nanos <= MAX_P95_NANOS,
            "Production corpus P95 was ${p95Micros}us; " +
                "budget is ${MAX_P95_NANOS / 1_000_000L}ms",
        )
        assertTrue(
            retainedHeapBytes <= MAX_RETAINED_HEAP_BYTES,
            "Production corpus retained $retainedHeapBytes bytes; " +
                "budget is $MAX_RETAINED_HEAP_BYTES bytes",
        )
    }

    private fun render(case: ProductionCorpusCase) {
        assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(case.source),
            "${case.id} failed during the production soak",
        )
    }

    private fun forceGc() {
        repeat(3) {
            System.gc()
            Thread.sleep(25)
        }
    }

    private fun usedHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }

    private fun nanosToMillis(nanos: Long): Long = nanos / 1_000_000L

    private companion object {
        const val WARMUP_ROUNDS = 3
        const val MEASURED_ROUNDS = 30
        const val MAX_TOTAL_MILLIS = 45_000L
        const val MAX_P95_NANOS = 500L * 1_000_000L
        const val MAX_RETAINED_HEAP_BYTES = 64L * 1024L * 1024L
    }
}
