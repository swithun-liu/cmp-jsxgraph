/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame

class ExpectTest {
    @Test
    fun pointAndCoordinateNormalizationMatchesOfficialFixture() {
        val board = Board(
            originX = 250.0,
            originY = 200.0,
            unitX = 50.0,
            unitY = 40.0,
        )
        val point = assertIs<GMResult.Ok<Point>>(
            Point.create(board, doubleArrayOf(2.0, 3.0)),
        ).value

        assertSame(point.coords, Expect.coords(point))
        val copied = Expect.coords(point, copy = true)
        assertNotSame(point.coords, copied)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 3.0),
            copied.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 350.0, 80.0),
            copied.scrCoords,
        )
    }

    @Test
    fun mutableCoordinateArraysPreserveUnshiftAndCopySemantics() {
        val short = mutableListOf(2.0, 3.0)
        val shortResult = Expect.coordsArray(short)
        assertSame(short, shortResult)
        assertEquals(listOf(1.0, 2.0, 3.0), short)

        val copiedSource = mutableListOf(4.0, 5.0)
        val copied = Expect.coordsArray(copiedSource, copy = true)
        assertEquals(listOf(1.0, 4.0, 5.0), copiedSource)
        assertEquals(listOf(1.0, 4.0, 5.0), copied)
        assertNotSame(copiedSource, copied)

        val long = mutableListOf(1.0, 2.0, 3.0, 4.0)
        assertEquals(
            listOf(1.0, 2.0, 3.0),
            Expect.coordsArray(long, copy = true),
        )
        assertSame(long, Expect.coordsArray(long))
    }

    @Test
    fun fixedArraysAndEachUseTypedKotlinAdaptation() {
        val short = doubleArrayOf(2.0, 3.0)
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 3.0),
            Expect.coordsArray(short),
        )
        assertContentEquals(doubleArrayOf(2.0, 3.0), short)

        val normalized = Expect.each(
            values = listOf(
                mutableListOf(1.0, 2.0),
                mutableListOf(3.0, 4.0),
            ),
            copy = true,
            format = Expect::coordsArray,
        )
        assertEquals(
            listOf(
                listOf(1.0, 1.0, 2.0),
                listOf(1.0, 3.0, 4.0),
            ),
            normalized,
        )
    }
}
