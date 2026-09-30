/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.math

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoxQuadtreeTest {
    @Test
    fun overlappingItemsFollowOfficialSubdivisionAndDuplicateHitRules() {
        val northWest = item(
            id = "north-west",
            xlb = -3.0,
            xub = -2.0,
            ylb = 2.0,
            yub = 3.0,
        )
        val north = item(
            id = "north",
            xlb = -1.0,
            xub = 1.0,
            ylb = 2.0,
            yub = 3.0,
        )
        val center = item(
            id = "center",
            xlb = -1.0,
            xub = 1.0,
            ylb = -1.0,
            yub = 1.0,
        )
        val tree = BoxQuadtree<TestItem>(
            depth = 3,
            capacity = 3,
            boundingBox = doubleArrayOf(-4.0, 4.0, 4.0, -4.0),
        )

        tree.insert(listOf(northWest, north, center))

        assertEquals(
            listOf("center", "north-west", "north", "north"),
            tree.find(doubleArrayOf(-4.0, 4.0, 4.0, -4.0))
                .map(TestItem::id),
        )
        assertEquals(
            listOf("center", "north-west", "north", "north"),
            tree.find(doubleArrayOf(-4.0, 4.0, 0.0, 0.0))
                .map(TestItem::id),
        )
        assertEquals(
            BoxQuadtreeStats(numberItems = 4, depth = 2),
            tree.analyzeTree(),
        )

        val plot = tree.plot()
        assertEquals(18, plot.dataX.size)
        assertEquals(18, plot.dataY.size)
        assertContentEquals(
            doubleArrayOf(-4.0, 4.0, 4.0, -4.0, -4.0),
            plot.dataX.copyOfRange(0, 5),
        )
        assertTrue(plot.dataX[5].isNaN())
        assertTrue(plot.dataY[5].isNaN())
    }

    @Test
    fun capacityThresholdIsStrictAndExistingItemsStayAtCurrentNode() {
        val tree = BoxQuadtree<TestItem>(
            depth = 2,
            capacity = 3,
        )
        tree.insert(
            listOf(
                item("first", -4.0, -3.0, 3.0, 4.0),
                item("second", 3.0, 4.0, -4.0, -3.0),
            ),
        )
        tree.insertItem(
            item("third", 3.0, 4.0, 3.0, 4.0),
        )

        assertEquals(
            listOf("first", "second", "third"),
            tree.find(doubleArrayOf(-4.0, 4.0, 4.0, -4.0))
                .map(TestItem::id),
        )
        assertEquals(
            BoxQuadtreeStats(numberItems = 3, depth = 2),
            tree.analyzeTree(),
        )
        assertContentEquals(
            doubleArrayOf(-4.0, 4.0, 4.0, -4.0, -4.0),
            tree.plot().dataX.copyOfRange(0, 5),
        )
    }

    private fun item(
        id: String,
        xlb: Double,
        xub: Double,
        ylb: Double,
        yub: Double,
    ): TestItem = TestItem(
        id = id,
        xlb = xlb,
        xub = xub,
        ylb = ylb,
        yub = yub,
    )

    private data class TestItem(
        val id: String,
        override val xlb: Double,
        override val xub: Double,
        override val ylb: Double,
        override val yub: Double,
    ) : BoxQuadtreeItem
}
