/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertEquals

class MetaPostTest {
    @Test
    fun openCurveMatchesOfficialControlPoints() {
        val result = MetaPost.curve(referencePoints())

        assertCoordinates(
            expected = doubleArrayOf(
                -3.0,
                -1.9965915203212292,
                -0.9512851323226681,
                0.0,
                1.4302952533432818,
                2.4166437688022535,
                4.0,
                5.264443313090963,
                5.7297941775084,
                6.0,
            ),
            actual = result.x,
        )
        assertCoordinates(
            expected = doubleArrayOf(
                -3.0,
                -2.8079385084787885,
                -2.6219811391998196,
                -3.0,
                -3.5683664801494444,
                -5.339667473546103,
                -5.0,
                -4.7287469129578135,
                -3.317624516847668,
                -2.0,
            ),
            actual = result.y,
        )
    }

    @Test
    fun closedCurveMatchesOfficialControlPoints() {
        val result = MetaPost.curve(
            pointList = listOf(
                MetaPostPoint(-2.0, -1.0),
                MetaPostPoint(2.0, -1.0),
                MetaPostPoint(1.0, 3.0),
            ),
            controls = MetaPostControls(isClosed = true),
        )

        assertCoordinates(
            expected = doubleArrayOf(
                -2.0,
                -1.0480160634066966,
                0.9419488633326374,
                2.0,
                3.111673617913538,
                2.6138138588268784,
                1.0,
                -1.4124468954863048,
                -3.4925527107164616,
                -2.0,
            ),
            actual = result.x,
        )
        assertCoordinates(
            expected = doubleArrayOf(
                -1.0,
                -2.3324565499011394,
                -2.313403654941914,
                -1.0,
                0.3799675103313749,
                2.4288084157505443,
                3.0,
                3.853858923452413,
                1.0890705809424475,
                -1.0,
            ),
            actual = result.y,
        )
    }

    @Test
    fun pointDirectionAndTensionMatchOfficialControlPoints() {
        val result = MetaPost.curve(
            pointList = referencePoints(),
            controls = MetaPostControls(
                pointControls = listOf(
                    MetaPostPointControl(
                        index = 1,
                        direction = MetaPostControlPair(
                            left = -30.0,
                            right = 45.0,
                        ),
                        tension = MetaPostControlPair(
                            left = 2.0,
                            right = 3.0,
                        ),
                    ),
                ),
            ),
        )

        assertCoordinates(
            expected = doubleArrayOf(
                -3.0,
                -1.974227776419943,
                -0.45072163263964493,
                0.0,
                0.5555440198211982,
                2.583730407853098,
                4.0,
                5.314840653841305,
                5.832552391134155,
                6.0,
            ),
            actual = result.x,
        )
        assertCoordinates(
            expected = doubleArrayOf(
                -3.0,
                -2.8921869948838457,
                -2.7397757440659136,
                -3.0,
                -2.4444559801788017,
                -5.055013717722158,
                -5.0,
                -4.948926198104421,
                -3.436808042380946,
                -2.0,
            ),
            actual = result.y,
        )
    }

    @Test
    fun duplicateKnotsMatchOfficialDegenerateSegments() {
        val result = MetaPost.curve(
            listOf(
                MetaPostPoint(0.0, 0.0),
                MetaPostPoint(0.0, 0.0),
                MetaPostPoint(2.0, 1.0),
            ),
        )

        assertCoordinates(
            expected = doubleArrayOf(
                0.0,
                0.0,
                0.0,
                0.0,
                2.0 / 3.0,
                4.0 / 3.0,
                2.0,
            ),
            actual = result.x,
        )
        assertCoordinates(
            expected = doubleArrayOf(
                0.0,
                0.0,
                0.0,
                0.0,
                1.0 / 3.0,
                2.0 / 3.0,
                1.0,
            ),
            actual = result.y,
        )
    }

    private fun referencePoints(): List<MetaPostPoint> =
        listOf(
            MetaPostPoint(-3.0, -3.0),
            MetaPostPoint(0.0, -3.0),
            MetaPostPoint(4.0, -5.0),
            MetaPostPoint(6.0, -2.0),
        )

    private fun assertCoordinates(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected = expected[index],
                actual = actual[index],
                absoluteTolerance = 1.0e-14,
                message = "coordinate[$index]",
            )
        }
    }
}
