/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PointReflectionsTest {
    @Test
    fun pointReflectionMetadataDependenciesAndUpdatesMatchOfficialCreators() {
        val board = board()
        val source = point(board, -3.0, 1.0, "source")
        val mirror = point(board, 1.0, -1.0, "mirror")
        val axisFirst = point(board, 0.0, -4.0, "axisFirst")
        val axisSecond = point(board, 0.0, 4.0, "axisSecond")
        val axis = line(board, axisFirst, axisSecond, "axis")
        val frozen = reflected(
            PointReflections.createReflection(
                board = board,
                source = source,
                line = axis,
                id = "frozen",
                name = "frozen-name",
                needsRegularUpdate = false,
                fixed = true,
            ),
        )
        val dynamic = reflected(
            PointReflections.createReflection(
                board = board,
                source = source,
                line = axis,
                id = "dynamic",
            ),
        )
        val mirrored = reflected(
            PointReflections.createMirrorElement(
                board = board,
                source = source,
                mirror = mirror,
                id = "mirrored",
            ),
        )
        val mirrorAlias = reflected(
            PointReflections.createMirrorPoint(
                board = board,
                source = source,
                mirror = mirror,
                id = "mirrorAlias",
            ),
        )

        assertEquals("reflection", frozen.elType)
        assertEquals("frozen-name", frozen.name)
        assertFalse(frozen.needsRegularUpdate)
        assertTrue(frozen.isFixed)
        assertFalse(frozen.isDraggable)
        assertSame(source, frozen.baseElement)
        assertEquals(
            listOf(TransformationType.REFLECT),
            frozen.transformations.map(Transformation::transformationType),
        )
        assertEquals(listOf("source", "axis"), frozen.parents)
        assertFalse("frozen" in source.childElements)
        assertSame(frozen, axis.childElements["frozen"])
        assertPoint(frozen, 3.0, 1.0)

        assertEquals("mirrorelement", mirrored.elType)
        assertEquals("mirrorpoint", mirrorAlias.elType)
        assertTrue(mirrored.isFixed)
        assertFalse(mirrored.isDraggable)
        assertEquals(listOf("source", "mirror"), mirrored.parents)
        assertSame(mirrored, mirror.childElements["mirrored"])
        assertSame(mirrorAlias, mirror.childElements["mirrorAlias"])
        assertFalse("mirrored" in source.childElements)
        assertPoint(mirrored, 5.0, -3.0)
        assertPoint(mirrorAlias, 5.0, -3.0)

        source.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 3.0),
        )
        mirror.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 0.0),
        )
        axisSecond.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 4.0),
        )
        board.update()

        assertPoint(frozen, 3.0, 1.0)
        assertPoint(
            dynamic,
            5.0588235294117645,
            1.2352941176470589,
        )
        assertPoint(mirrored, 2.0, -3.0)
        assertPoint(mirrorAlias, 2.0, -3.0)
    }

    @Test
    fun recursiveRemovalFollowsReflectorRatherThanSource() {
        val reflectionBoard = board("reflection")
        val reflectionSource =
            point(reflectionBoard, -3.0, 1.0, "reflectionSource")
        val axisFirst =
            point(reflectionBoard, 0.0, -4.0, "axisFirst")
        val axisSecond =
            point(reflectionBoard, 0.0, 4.0, "axisSecond")
        val axis = line(
            reflectionBoard,
            axisFirst,
            axisSecond,
            "axis",
        )
        val reflection = reflected(
            PointReflections.createReflection(
                board = reflectionBoard,
                source = reflectionSource,
                line = axis,
                id = "reflection",
            ),
        )

        reflectionBoard.removeObject(reflectionSource)
        assertFalse("reflectionSource" in reflectionBoard.objects)
        assertSame(reflection, reflectionBoard.objects["reflection"])
        reflectionBoard.removeObject(axis)
        assertFalse("axis" in reflectionBoard.objects)
        assertFalse("reflection" in reflectionBoard.objects)

        val mirrorBoard = board("mirror")
        val mirrorSource = point(mirrorBoard, -3.0, 1.0, "mirrorSource")
        val mirrorCenter = point(mirrorBoard, 1.0, -1.0, "mirrorCenter")
        val mirrorOutput = reflected(
            PointReflections.createMirrorElement(
                board = mirrorBoard,
                source = mirrorSource,
                mirror = mirrorCenter,
                id = "mirrorOutput",
            ),
        )

        mirrorBoard.removeObject(mirrorSource)
        assertFalse("mirrorSource" in mirrorBoard.objects)
        assertSame(mirrorOutput, mirrorBoard.objects["mirrorOutput"])
        mirrorBoard.removeObject(mirrorCenter)
        assertFalse("mirrorCenter" in mirrorBoard.objects)
        assertFalse("mirrorOutput" in mirrorBoard.objects)
    }

    @Test
    fun removingOutputKeepsBothExistingParents() {
        val board = board()
        val source = point(board, -3.0, 1.0, "source")
        val mirror = point(board, 1.0, -1.0, "mirror")
        val output = reflected(
            PointReflections.createMirrorPoint(
                board = board,
                source = source,
                mirror = mirror,
                id = "output",
            ),
        )

        board.removeObject(output)

        assertSame(source, board.objects["source"])
        assertSame(mirror, board.objects["mirror"])
        assertFalse("output" in board.objects)
    }

    @Test
    fun factoriesRejectForeignUnregisteredAndDuplicateParentsStructurally() {
        val board = board("registered")
        val otherBoard = board("foreign")
        val source = point(board, -3.0, 1.0, "source")
        val mirror = point(board, 1.0, -1.0, "mirror")
        val foreign = point(otherBoard, 0.0, 0.0, "foreign")
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            id = "unregistered",
        )

        assertEquals(
            PointReflectionError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<PointReflectionError.ParentBoardMismatch>>(
                PointReflections.createMirrorElement(
                    board = board,
                    source = source,
                    mirror = foreign,
                ),
            ).error,
        )
        assertEquals(
            PointReflectionError.ParentNotRegistered(
                parentIndex = 1,
                id = "unregistered",
            ),
            assertIs<
                GMResult.Err<PointReflectionError.ParentNotRegistered>,
                >(
                PointReflections.createMirrorPoint(
                    board = board,
                    source = source,
                    mirror = unregistered,
                ),
            ).error,
        )

        val objectCount = board.objects.size
        val duplicate = reflected(
            PointReflections.createMirrorPoint(
                board = board,
                source = source,
                mirror = mirror,
                id = "duplicate",
            ),
        )
        assertEquals(objectCount + 1, board.objects.size)
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("duplicate"),
            ),
            assertIs<GMResult.Err<PointReflectionError.PointFactory>>(
                PointReflections.createMirrorPoint(
                    board = board,
                    source = source,
                    mirror = mirror,
                    id = "duplicate",
                ),
            ).error.error,
        )
        assertEquals(objectCount + 1, board.objects.size)
        assertSame(duplicate, board.objects["duplicate"])
    }

    private fun board(id: String = "board"): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = id,
        )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun line(
        board: Board,
        first: Point,
        second: Point,
        id: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = first,
                point2 = second,
                id = id,
                name = "",
            ),
        ).value

    private fun reflected(
        result: GMResult<Point, PointReflectionError>,
    ): Point = assertIs<GMResult.Ok<Point>>(result).value

    private fun assertPoint(
        point: Point,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            point.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            expectedY,
            point.Y(),
            absoluteTolerance = 1.0e-12,
        )
    }
}
