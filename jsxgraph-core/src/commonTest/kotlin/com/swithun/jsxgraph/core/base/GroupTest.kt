/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GroupTest {
    @Test
    fun creationMatchesOfficialRegistryParentsAndFixedFiltering() {
        val board = board("creation")
        val a = point(board, 0.0, 0.0, "A")
        val b = point(board, 2.0, 0.0, "B")
        val fixed = point(board, 4.0, 4.0, "fixed", fixed = true)

        val group = group(
            board = board,
            parents = listOf(a, b, fixed),
            id = "group",
            name = "named",
            needsRegularUpdate = false,
        )

        assertEquals("group", group.elType)
        assertEquals("named", group.name)
        assertSame(group, board.groupById("group"))
        assertEquals(listOf("A", "B", "fixed"), group.getParents())
        assertEquals(listOf("A", "B"), group.groupObjects.keys.toList())
        assertEquals(listOf("A", "B"), group.translationPoints.map { it.id })
        assertEquals(listOf("group"), a.groups)
        assertEquals(listOf("group"), b.groups)
        assertTrue(fixed.groups.isEmpty())
        assertEquals(GroupCenter.Centroid, group.rotationCenter)
        assertNull(group.scaleCenter)

        val replacement = group(
            board = board,
            parents = listOf(b),
            id = "group",
            name = "",
        )
        assertSame(replacement, board.groupById("group"))
        assertEquals(listOf("B"), replacement.groupObjects.keys.toList())
    }

    @Test
    fun boardUpdateTranslatesEveryMemberFromCachedCoordinates() {
        val board = board("translation")
        val a = point(board, 0.0, 0.0, "A")
        val b = point(board, 2.0, 0.0, "B")
        val c = point(board, 0.0, 2.0, "C")
        val group = group(board, listOf(a, b, c), id = "group")

        a.setPosition(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 1.0),
        )
        board.update(a)

        assertPoint(a, 1.0, 1.0)
        assertPoint(b, 3.0, 1.0)
        assertPoint(c, 1.0, 3.0)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 1.0),
            group.coords.getValue("B"),
        )
        assertNull(group.updateError)
    }

    @Test
    fun rotationAndDirectionalScalingMatchOfficialMatrices() {
        val rotationBoard = board("rotation")
        val center = point(rotationBoard, 0.0, 0.0, "center")
        val handle = point(rotationBoard, 2.0, 0.0, "handle")
        val other = point(rotationBoard, 0.0, 2.0, "other")
        val rotationGroup = group(
            rotationBoard,
            listOf(center, handle, other),
            id = "rotation",
        )
        rotationGroup
            .setRotationCenter(GroupCenter.Element(center))
            .setRotationPoints(listOf(handle))

        handle.setPosition(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 2.0),
        )
        rotationBoard.update(handle)

        assertPoint(center, 0.0, 0.0)
        assertPoint(handle, 0.0, 2.0, tolerance = 1.0e-12)
        assertPoint(other, -2.0, 0.0, tolerance = 1.0e-12)
        assertEquals(PI * 0.5, angle(2.0, 0.0, handle.X(), handle.Y()), 1.0e-12)

        val scaleBoard = board("scale")
        val scaleCenter = point(scaleBoard, 0.0, 0.0, "center")
        val scaleHandle = point(scaleBoard, 2.0, 0.0, "handle")
        val scaleOther = point(scaleBoard, 0.0, 2.0, "other")
        val scaleGroup = group(
            scaleBoard,
            listOf(scaleCenter, scaleHandle, scaleOther),
            id = "scale",
        )
        scaleGroup
            .setScaleCenter(GroupCenter.Element(scaleCenter))
            .setScalePoints(listOf(scaleHandle), direction = "x")

        scaleHandle.setPosition(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 0.0),
        )
        scaleBoard.update(scaleHandle)

        assertPoint(scaleCenter, 0.0, 0.0)
        assertPoint(scaleHandle, 4.0, 0.0)
        assertPoint(scaleOther, 0.0, 2.0)
        assertEquals("x", scaleGroup.scaleDirections["handle"])
    }

    @Test
    fun removeAndUngroupPreserveOfficialBookkeepingQuirks() {
        val board = board("membership")
        val a = point(board, 0.0, 0.0, "A")
        val b = point(board, 2.0, 0.0, "B")
        val c = point(board, 0.0, 2.0, "C")
        val first = group(board, listOf(a), id = "first")
        val second = group(board, listOf(b), id = "second")

        first.addPoint(c)
        first.addGroup(second)
        first.removePoint(c)

        assertEquals(listOf("A", "B"), first.groupObjects.keys.toList())
        assertTrue("first" in c.groups)
        assertTrue("C" in first.coords)
        assertEquals(listOf("A", "C", "B"), first.translationPoints.map { it.id })

        first.ungroup()

        assertTrue(first.groupObjects.isEmpty())
        assertTrue("first" !in a.groups)
        assertTrue("first" !in b.groups)
        assertTrue("first" in c.groups)
        assertTrue(first.coords.isNotEmpty())
        assertTrue(first.translationPoints.isNotEmpty())
    }

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
        fixed: Boolean = false,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
                fixed = fixed,
            ),
        ).value

    private fun group(
        board: Board,
        parents: List<GeometryElement>,
        id: String,
        name: String? = null,
        needsRegularUpdate: Boolean = true,
    ): Group =
        assertIs<GMResult.Ok<Group>>(
            Group.create(
                board = board,
                parents = parents,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            ),
        ).value

    private fun assertPoint(
        point: CoordsElement,
        x: Double,
        y: Double,
        tolerance: Double = 0.0,
    ) {
        assertEquals(x, point.X(), tolerance)
        assertEquals(y, point.Y(), tolerance)
    }

    private fun angle(
        x1: Double,
        y1: Double,
        x2: Double,
        y2: Double,
    ): Double = kotlin.math.atan2(
        x1 * y2 - y1 * x2,
        x1 * x2 + y1 * y2,
    )
}
