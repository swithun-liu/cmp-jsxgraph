/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.math.RandomSource
import kotlin.test.Test
import kotlin.test.assertEquals

class UUIDTest {
    @Test
    fun zeroRandomSourceAndPrefixesMatchOfficialFixture() {
        val zeroRandom = RandomSource { 0.0 }

        assertEquals(
            "00000000-0000-4000-8000-000000000000",
            genUUID(randomSource = zeroRandom),
        )
        assertEquals(
            "canvas-00000000-0000-4000-8000-000000000000",
            genUUID(prefix = "canvas", randomSource = zeroRandom),
        )
        assertEquals(
            "canvas-00000000-0000-4000-8000-000000000000",
            genUUID(prefix = "canvas-", randomSource = zeroRandom),
        )
    }

    @Test
    fun randomPoolConsumptionMatchesOfficialFixture() {
        val values = listOf(0.0, 0.25, 0.5, 0.75, 0.999999)
        var calls = 0
        val randomSource = RandomSource {
            values[calls++ % values.size]
        }

        assertEquals(
            "00000000-0004-4000-8080-0000CFEFFFF0",
            genUUID(randomSource = randomSource),
        )
        assertEquals(6, calls)
    }
}
