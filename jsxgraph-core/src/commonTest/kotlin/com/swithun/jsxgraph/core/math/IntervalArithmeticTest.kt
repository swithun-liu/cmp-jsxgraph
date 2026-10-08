/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class IntervalArithmeticTest {
    @AfterTest
    fun restorePlotV4RoundingMode() {
        IntervalArithmetic.disable()
    }

    @Test
    fun algebraAndSetOperationsMatchOfficialFixture() {
        IntervalArithmetic.disable()

        assertInterval(
            PlotInterval(4.0, 7.0),
            IntervalArithmetic.add(
                PlotInterval(1.0, 2.0),
                PlotInterval(3.0, 5.0),
            ),
        )
        assertInterval(
            IntervalArithmetic.whole,
            IntervalArithmetic.divide(
                PlotInterval(2.0, 3.0),
                PlotInterval(-1.0, 1.0),
            ),
        )
        assertInterval(
            PlotInterval(0.25, 0.5),
            IntervalArithmetic.multiplicativeInverse(
                PlotInterval(2.0, 4.0),
            ),
        )
        assertInterval(
            PlotInterval(-2.0, -1.0),
            IntervalArithmetic.nthRoot(
                PlotInterval(-8.0, -1.0),
                3.0,
            ),
        )
        assertInterval(
            PlotInterval(0.0, 3.0),
            IntervalArithmetic.nthRoot(
                PlotInterval(-4.0, 9.0),
                2.0,
            ),
        )
        assertInterval(
            PlotInterval(Double.NEGATIVE_INFINITY, 1.0),
            IntervalArithmetic.log(
                PlotInterval(-1.0, kotlin.math.E),
            ),
        )
        assertInterval(
            PlotInterval(1.0, 5.0),
            IntervalArithmetic.hull(
                PlotInterval(1.0, 2.0),
                PlotInterval(4.0, 5.0),
            ),
        )
        assertInterval(
            PlotInterval(3.0, 4.0),
            IntervalArithmetic.intersection(
                PlotInterval(1.0, 4.0),
                PlotInterval(3.0, 5.0),
            ),
        )
    }

    @Test
    fun unionAndDifferenceContainExpectedFailures() {
        IntervalArithmetic.disable()

        val union = assertIs<GMResult.Ok<PlotInterval>>(
            IntervalArithmetic.union(
                PlotInterval(1.0, 4.0),
                PlotInterval(3.0, 5.0),
            ),
        )
        assertInterval(PlotInterval(1.0, 5.0), union.value)
        assertIs<
            GMResult.Err<IntervalArithmeticError.NonOverlappingUnion>
            >(
            IntervalArithmetic.union(
                PlotInterval(1.0, 2.0),
                PlotInterval(3.0, 4.0),
            ),
        )

        val leftDifference = assertIs<GMResult.Ok<PlotInterval>>(
            IntervalArithmetic.difference(
                PlotInterval(1.0, 5.0),
                PlotInterval(-1.0, 3.0),
            ),
        )
        assertInterval(
            PlotInterval(3.0, 5.0),
            leftDifference.value,
        )
        assertIs<
            GMResult.Err<IntervalArithmeticError.DisconnectedDifference>
            >(
            IntervalArithmetic.difference(
                PlotInterval(1.0, 5.0),
                PlotInterval(2.0, 3.0),
            ),
        )
    }

    @Test
    fun transcendentalAndComparisonOperationsMatchOfficialFixture() {
        IntervalArithmetic.disable()

        assertInterval(
            PlotInterval(1.0, 3.7621956910836314),
            IntervalArithmetic.cosh(PlotInterval(-2.0, 1.0)),
        )
        assertTrue(
            IntervalArithmetic.equal(
                IntervalArithmetic.empty,
                IntervalArithmetic.empty,
            ),
        )
        assertTrue(
            IntervalArithmetic.notEqual(
                PlotInterval(1.0, 2.0),
                PlotInterval(3.0, 4.0),
            ),
        )
        assertTrue(
            IntervalArithmetic.lessThan(
                PlotInterval(1.0, 2.0),
                PlotInterval(3.0, 4.0),
            ),
        )
        assertTrue(
            IntervalArithmetic.greaterThanOrEqual(
                PlotInterval(4.0, 5.0),
                PlotInterval(2.0, 4.0),
            ),
        )
    }

    @Test
    fun enabledRoundingExpandsBoundsByOneRepresentableValue() {
        IntervalArithmetic.enable()

        assertInterval(
            PlotInterval(0.3, 0.3000000000000001),
            IntervalArithmetic.add(
                PlotInterval(0.1, 0.1),
                PlotInterval(0.2, 0.2),
            ),
        )
    }

    private fun assertInterval(
        expected: PlotInterval,
        actual: PlotInterval,
    ) {
        assertEquals(expected.lo, actual.lo)
        assertEquals(expected.hi, actual.hi)
    }
}
