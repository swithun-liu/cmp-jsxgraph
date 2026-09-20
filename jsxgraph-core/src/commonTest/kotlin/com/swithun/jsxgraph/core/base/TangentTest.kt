/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TangentTest {
    @Test
    fun officialAliasesParentOrderMetadataAndUpdatesArePreserved() {
        val board = board("geometry")
        val center = point(board, 1.0, 1.0, "center")
        val radiusPoint = point(board, 4.0, 1.0, "radiusPoint")
        val circle = circle(board, center, radiusPoint, "sourceCircle")
        val sourcePoint = point(board, 5.0, 4.0, "offCircle")
        val tangentCircleFirst = tangent(
            Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = sourcePoint,
                id = "tangentCircleFirst",
                name = "",
                point1Id = "tangentCircleFirstPoint1",
                point1Name = "",
                point2Id = "tangentCircleFirstPoint2",
                point2Name = "",
            ),
        )
        val tangentPointFirst = tangent(
            Tangent.create(
                board = board,
                firstParent = sourcePoint,
                secondParent = circle,
                id = "tangentPointFirst",
                name = "",
                point1Id = "tangentPointFirstPoint1",
                point1Name = "",
                point2Id = "tangentPointFirstPoint2",
                point2Name = "",
            ),
        )
        val polarAlias = tangent(
            Tangent.create(
                board = board,
                firstParent = sourcePoint,
                secondParent = circle,
                id = "polarAlias",
                name = "",
                point1Id = "polarAliasPoint1",
                point1Name = "",
                point2Id = "polarAliasPoint2",
                point2Name = "",
            ),
        )
        val polarLineCircleFirst = tangent(
            Tangent.createPolarLine(
                board = board,
                firstParent = circle,
                secondParent = sourcePoint,
                id = "polarLineCircleFirst",
                name = "",
                point1Id = "polarLineCircleFirstPoint1",
                point1Name = "",
                point2Id = "polarLineCircleFirstPoint2",
                point2Name = "",
            ),
        )
        val polarLinePointFirst = tangent(
            Tangent.createPolarLine(
                board = board,
                firstParent = sourcePoint,
                secondParent = circle,
                id = "polarLinePointFirst",
                name = "",
                point1Id = "polarLinePointFirstPoint1",
                point1Name = "",
                point2Id = "polarLinePointFirstPoint2",
                point2Name = "",
            ),
        )
        board.update()

        val lines = listOf(
            tangentCircleFirst,
            tangentPointFirst,
            polarAlias,
            polarLineCircleFirst,
            polarLinePointFirst,
        )
        for (line in lines) {
            assertEquals(Const.OBJECT_TYPE_TANGENT, line.type)
            assertEquals(Const.OBJECT_CLASS_LINE, line.elementClass)
            assertTrue(line.constrained)
            assertFalse(line.isDraggable)
            assertTrue(line.straightFirst)
            assertTrue(line.straightLast)
            assertSame(sourcePoint, line.glider)
            assertEquals(
                listOf<GeometryElement>(line.point1, line.point2),
                line.inherits,
            )
            assertHelper(
                point = line.point1,
                expected = doubleArrayOf(1.0, 2.8, 1.6),
            )
            assertHelper(
                point = line.point2,
                expected = doubleArrayOf(1.0, 2.68, 1.76),
            )
            assertArrayMatches(
                expected = doubleArrayOf(
                    3.200000000000003,
                    -0.8000000000000007,
                    -0.5999999999999991,
                    0.0,
                    1.0,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                ),
                actual = line.stdform,
            )
            assertEquals(
                setOf(line.point1.id, line.point2.id, sourcePoint.id),
                line.ancestors.keys,
            )
        }
        assertEquals("tangent", tangentCircleFirst.elType)
        assertEquals("tangent", tangentPointFirst.elType)
        assertEquals("tangent", polarAlias.elType)
        assertEquals("polarline", polarLineCircleFirst.elType)
        assertEquals("polarline", polarLinePointFirst.elType)
        assertEquals(
            listOf("sourceCircle", "offCircle"),
            tangentCircleFirst.parents,
        )
        assertEquals(
            listOf("offCircle", "sourceCircle"),
            tangentPointFirst.parents,
        )
        assertEquals(
            listOf("offCircle", "sourceCircle"),
            polarAlias.parents,
        )
        assertEquals(
            listOf("sourceCircle", "offCircle"),
            polarLineCircleFirst.parents,
        )
        assertEquals(
            listOf("sourceCircle", "offCircle"),
            polarLinePointFirst.parents,
        )
        assertTrue(circle.childElements.isEmpty())
        assertEquals(lines.map(Line::id).toSet(), sourcePoint.childElements.keys)
        assertEquals(
            listOf(
                "center",
                "radiusPoint",
                "sourceCircle",
                "offCircle",
                "tangentCircleFirstPoint1",
                "tangentCircleFirstPoint2",
                "tangentCircleFirst",
                "tangentPointFirstPoint1",
                "tangentPointFirstPoint2",
                "tangentPointFirst",
                "polarAliasPoint1",
                "polarAliasPoint2",
                "polarAlias",
                "polarLineCircleFirstPoint1",
                "polarLineCircleFirstPoint2",
                "polarLineCircleFirst",
                "polarLinePointFirstPoint1",
                "polarLinePointFirstPoint2",
                "polarLinePointFirst",
            ),
            board.objectsList.map(GeometryElement::id),
        )

        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        radiusPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        sourcePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -2.0),
        )
        board.update()

        for (line in lines) {
            assertArrayMatches(
                doubleArrayOf(1.0, -0.25, -0.25),
                line.point1.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(1.0, -0.125, -0.125),
                line.point2.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(
                    0.0,
                    -0.7071067811865475,
                    0.7071067811865475,
                    0.0,
                    1.0,
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                ),
                line.stdform,
            )
        }
    }

    @Test
    fun onCircleOffCircleCenterAndDegenerateGeometryMatchOfficial() {
        val board = board("special-geometry")
        val center = point(board, 0.0, 0.0, "center")
        val circle = circle(
            board,
            center,
            point(board, 3.0, 0.0, "radius"),
            "circle",
        )
        val onCircle = tangent(
            Tangent.create(
                board = board,
                firstParent = point(board, 0.0, 3.0, "onCirclePoint"),
                secondParent = circle,
                id = "onCircle",
                point1Id = "onCirclePoint1",
                point2Id = "onCirclePoint2",
            ),
        )
        val offCircle = tangent(
            Tangent.create(
                board = board,
                firstParent = point(board, 4.0, 2.0, "offCirclePoint"),
                secondParent = circle,
                id = "offCircle",
                point1Id = "offCirclePoint1",
                point2Id = "offCirclePoint2",
            ),
        )
        val centerLine = tangent(
            Tangent.create(
                board = board,
                firstParent = point(board, 0.0, 0.0, "centerPoint"),
                secondParent = circle,
                id = "centerLine",
                point1Id = "centerLinePoint1",
                point2Id = "centerLinePoint2",
            ),
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(3.0, 0.0, -1.0),
            onCircle.stdform.copyOfRange(0, 3),
        )
        assertArrayMatches(
            doubleArrayOf(
                2.0124611797498106,
                -0.8944271909999157,
                -0.44721359549995837,
            ),
            offCircle.stdform.copyOfRange(0, 3),
        )
        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            centerLine.point1.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            centerLine.point2.coords.usrCoords,
        )
        assertTrue(centerLine.stdform.take(3).all(Double::isNaN))

        val degenerateBoard = board("degenerate")
        val degenerateCenter = point(
            degenerateBoard,
            2.0,
            -1.0,
            "degenerateCenter",
        )
        val degenerateCircle = circle(
            degenerateBoard,
            degenerateCenter,
            point(
                degenerateBoard,
                2.0,
                -1.0,
                "degenerateRadius",
            ),
            "degenerateCircle",
        )
        val degenerateLine = tangent(
            Tangent.create(
                board = degenerateBoard,
                firstParent = degenerateCircle,
                secondParent = point(
                    degenerateBoard,
                    4.0,
                    3.0,
                    "degeneratePoint",
                ),
                id = "degenerateLine",
                point1Id = "degenerateLinePoint1",
                point2Id = "degenerateLinePoint2",
            ),
        )
        degenerateBoard.update()

        assertArrayMatches(
            doubleArrayOf(
                0.0,
                -0.4472135954999579,
                -0.8944271909999159,
            ),
            degenerateLine.stdform.copyOfRange(0, 3),
        )
    }

    @Test
    fun directAndParentRemovalMatchOfficialAsymmetricLifecycle() {
        val board = board("direct-removal")
        val circle = circle(
            board,
            point(board, 0.0, 0.0, "center"),
            point(board, 2.0, 0.0, "radius"),
            "circle",
        )
        val sourcePoint = point(board, 3.0, 2.0, "sourcePoint")
        val line = tangent(
            Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = sourcePoint,
                id = "line",
                point1Id = "linePoint1",
                point2Id = "linePoint2",
            ),
        )
        val point1 = line.point1
        val point2 = line.point2

        board.removeObject(line)

        assertEquals(null, board.elementById(line.id))
        assertSame(point1, board.elementById(point1.id))
        assertSame(point2, board.elementById(point2.id))
        assertTrue(circle.childElements.isEmpty())
        assertTrue(sourcePoint.childElements.isEmpty())
        assertTrue(point1.childElements.isEmpty())
        assertTrue(point2.childElements.isEmpty())

        val circleRemovalBoard = board("circle-removal")
        val removedCircle = circle(
            circleRemovalBoard,
            point(circleRemovalBoard, 0.0, 0.0, "circleCenter"),
            point(circleRemovalBoard, 2.0, 0.0, "circleRadius"),
            "removedCircle",
        )
        val retainedPoint = point(
            circleRemovalBoard,
            3.0,
            2.0,
            "retainedPoint",
        )
        val retainedLine = tangent(
            Tangent.create(
                board = circleRemovalBoard,
                firstParent = removedCircle,
                secondParent = retainedPoint,
                id = "retainedLine",
                point1Id = "retainedLinePoint1",
                point2Id = "retainedLinePoint2",
            ),
        )

        circleRemovalBoard.removeObject(removedCircle)

        assertEquals(null, circleRemovalBoard.elementById(removedCircle.id))
        assertSame(retainedLine, circleRemovalBoard.elementById(retainedLine.id))
        assertSame(retainedPoint, circleRemovalBoard.elementById(retainedPoint.id))

        val pointRemovalBoard = board("point-removal")
        val retainedCircle = circle(
            pointRemovalBoard,
            point(pointRemovalBoard, 0.0, 0.0, "retainedCenter"),
            point(pointRemovalBoard, 2.0, 0.0, "retainedRadius"),
            "retainedCircle",
        )
        val removedPoint = point(
            pointRemovalBoard,
            3.0,
            2.0,
            "removedPoint",
        )
        val removedLine = tangent(
            Tangent.create(
                board = pointRemovalBoard,
                firstParent = retainedCircle,
                secondParent = removedPoint,
                id = "removedLine",
                point1Id = "removedLinePoint1",
                point2Id = "removedLinePoint2",
            ),
        )

        pointRemovalBoard.removeObject(removedPoint)

        assertSame(
            retainedCircle,
            pointRemovalBoard.elementById(retainedCircle.id),
        )
        assertEquals(null, pointRemovalBoard.elementById(removedPoint.id))
        assertEquals(null, pointRemovalBoard.elementById(removedLine.id))
        assertSame(
            removedLine.point1,
            pointRemovalBoard.elementById(removedLine.point1.id),
        )
        assertSame(
            removedLine.point2,
            pointRemovalBoard.elementById(removedLine.point2.id),
        )
    }

    @Test
    fun lineBranchReusesEndpointsAndMatchesOfficialMetadata() {
        val board = board("line-geometry")
        val point1 = point(board, -3.0, -1.0, "A")
        val point2 = point(board, 4.0, 2.0, "B")
        val sourceLine = line(board, point1, point2, "source")
        val sourcePoint = point(board, 1.0, 5.0, "P")
        val lineFirst = tangent(
            Tangent.create(
                board = board,
                firstParent = sourceLine,
                secondParent = sourcePoint,
                id = "lineFirst",
                straightFirst = false,
                straightLast = true,
                point1Id = "ignoredPoint1",
                point1Name = "ignored-point-1",
                point2Id = "ignoredPoint2",
                point2Name = "ignored-point-2",
            ),
        )
        val pointFirst = tangent(
            Tangent.create(
                board = board,
                firstParent = sourcePoint,
                secondParent = sourceLine,
                id = "pointFirst",
                straightFirst = true,
                straightLast = false,
            ),
        )
        val polarAlias = tangent(
            Tangent.create(
                board = board,
                firstParent = sourcePoint,
                secondParent = sourceLine,
                id = "polarAlias",
            ),
        )
        board.update()

        for (output in listOf(lineFirst, pointFirst, polarAlias)) {
            assertEquals("tangent", output.elType)
            assertEquals(Const.OBJECT_TYPE_TANGENT, output.type)
            assertEquals(Const.OBJECT_CLASS_LINE, output.elementClass)
            assertFalse(output.constrained)
            assertTrue(output.isDraggable)
            assertSame(sourcePoint, output.glider)
            assertSame(point1, output.point1)
            assertSame(point2, output.point2)
            assertEquals(
                listOf<GeometryElement>(point1, point2),
                output.inherits,
            )
            assertEquals(setOf("A", "B"), output.ancestors.keys)
            assertArrayMatches(sourceLine.stdform, output.stdform)
        }
        assertFalse(lineFirst.straightFirst)
        assertTrue(lineFirst.straightLast)
        assertTrue(pointFirst.straightFirst)
        assertFalse(pointFirst.straightLast)
        assertTrue(polarAlias.straightFirst)
        assertTrue(polarAlias.straightLast)
        assertEquals(listOf("source", "P"), lineFirst.parents)
        assertEquals(listOf("P", "source"), pointFirst.parents)
        assertEquals(listOf("P", "source"), polarAlias.parents)
        assertTrue(sourceLine.childElements.isEmpty())
        assertTrue(sourcePoint.childElements.isEmpty())
        assertEquals(
            setOf("source", "lineFirst", "pointFirst", "polarAlias"),
            point1.childElements.keys,
        )
        assertEquals(
            setOf("source", "lineFirst", "pointFirst", "polarAlias"),
            point2.childElements.keys,
        )
        assertEquals(null, board.elementById("ignoredPoint1"))
        assertEquals(null, board.elementById("ignoredPoint2"))
        assertEquals(
            listOf(
                "A",
                "B",
                "source",
                "P",
                "lineFirst",
                "pointFirst",
                "polarAlias",
            ),
            board.objectsList.map(GeometryElement::id),
        )

        sourcePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(7.0, 1.0),
        )
        board.update()
        for (output in listOf(lineFirst, pointFirst, polarAlias)) {
            assertArrayMatches(sourceLine.stdform, output.stdform)
        }

        point1.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-5.0, 3.0),
        )
        point2.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, -4.0),
        )
        board.update()
        assertArrayMatches(
            doubleArrayOf(
                1.4142135623730951,
                0.7071067811865476,
                0.7071067811865476,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            sourceLine.stdform,
        )
        for (output in listOf(lineFirst, pointFirst, polarAlias)) {
            assertArrayMatches(sourceLine.stdform, output.stdform)
        }
    }

    @Test
    fun lineBranchRemovalMatchesOfficialEndpointOwnership() {
        val directBoard = board("line-direct-removal")
        val directPoint1 = point(directBoard, -2.0, 0.0, "directA")
        val directPoint2 = point(directBoard, 3.0, 1.0, "directB")
        val directSource = line(
            directBoard,
            directPoint1,
            directPoint2,
            "directSource",
        )
        val directParameter = point(
            directBoard,
            0.0,
            4.0,
            "directParameter",
        )
        val directTangent = tangent(
            Tangent.create(
                board = directBoard,
                firstParent = directSource,
                secondParent = directParameter,
                id = "directTangent",
            ),
        )

        directBoard.removeObject(directTangent)

        assertSame(directSource, directBoard.elementById(directSource.id))
        assertSame(directParameter, directBoard.elementById(directParameter.id))
        assertEquals(null, directBoard.elementById(directTangent.id))
        assertEquals(setOf("directSource"), directPoint1.childElements.keys)
        assertEquals(setOf("directSource"), directPoint2.childElements.keys)

        val sourceBoard = board("line-source-removal")
        val sourcePoint1 = point(sourceBoard, -2.0, 0.0, "sourceA")
        val sourcePoint2 = point(sourceBoard, 3.0, 1.0, "sourceB")
        val removedSource = line(
            sourceBoard,
            sourcePoint1,
            sourcePoint2,
            "removedSource",
        )
        val retainedParameter = point(
            sourceBoard,
            0.0,
            4.0,
            "retainedParameter",
        )
        val retainedTangent = tangent(
            Tangent.create(
                board = sourceBoard,
                firstParent = removedSource,
                secondParent = retainedParameter,
                id = "retainedTangent",
            ),
        )

        sourceBoard.removeObject(removedSource)

        assertEquals(null, sourceBoard.elementById(removedSource.id))
        assertSame(retainedTangent, sourceBoard.elementById(retainedTangent.id))
        assertSame(sourcePoint1, retainedTangent.point1)
        assertSame(sourcePoint2, retainedTangent.point2)
        assertEquals(
            setOf("retainedTangent"),
            sourcePoint1.childElements.keys,
        )
        assertEquals(
            setOf("retainedTangent"),
            sourcePoint2.childElements.keys,
        )

        val parameterBoard = board("line-parameter-removal")
        val parameterPoint1 = point(
            parameterBoard,
            -2.0,
            0.0,
            "parameterA",
        )
        val parameterPoint2 = point(
            parameterBoard,
            3.0,
            1.0,
            "parameterB",
        )
        val retainedSource = line(
            parameterBoard,
            parameterPoint1,
            parameterPoint2,
            "retainedSource",
        )
        val removedParameter = point(
            parameterBoard,
            0.0,
            4.0,
            "removedParameter",
        )
        val pointRetainedTangent = tangent(
            Tangent.create(
                board = parameterBoard,
                firstParent = retainedSource,
                secondParent = removedParameter,
                id = "pointRetainedTangent",
            ),
        )

        parameterBoard.removeObject(removedParameter)

        assertSame(
            retainedSource,
            parameterBoard.elementById(retainedSource.id),
        )
        assertSame(
            pointRetainedTangent,
            parameterBoard.elementById(pointRetainedTangent.id),
        )
        assertEquals(
            setOf("parameterA", "parameterB"),
            pointRetainedTangent.ancestors.keys,
        )

        val endpointBoard = board("line-endpoint-removal")
        val removedEndpoint = point(
            endpointBoard,
            -2.0,
            0.0,
            "removedEndpoint",
        )
        val retainedEndpoint = point(
            endpointBoard,
            3.0,
            1.0,
            "retainedEndpoint",
        )
        val endpointSource = line(
            endpointBoard,
            removedEndpoint,
            retainedEndpoint,
            "endpointSource",
        )
        val endpointParameter = point(
            endpointBoard,
            0.0,
            4.0,
            "endpointParameter",
        )
        val endpointTangent = tangent(
            Tangent.create(
                board = endpointBoard,
                firstParent = endpointSource,
                secondParent = endpointParameter,
                id = "endpointTangent",
            ),
        )

        endpointBoard.removeObject(removedEndpoint)

        assertEquals(null, endpointBoard.elementById(removedEndpoint.id))
        assertSame(
            retainedEndpoint,
            endpointBoard.elementById(retainedEndpoint.id),
        )
        assertEquals(null, endpointBoard.elementById(endpointSource.id))
        assertSame(
            endpointParameter,
            endpointBoard.elementById(endpointParameter.id),
        )
        assertEquals(null, endpointBoard.elementById(endpointTangent.id))
        assertTrue(retainedEndpoint.childElements.isEmpty())
    }

    @Test
    fun lineBranchDegenerateAndFailureBehaviorIsStructuredAndAtomic() {
        val board = board("line-errors")
        val point1 = point(board, 2.0, -1.0, "A")
        val point2 = point(board, 2.0, -1.0, "B")
        val sourceLine = line(board, point1, point2, "source")
        val sourcePoint = point(board, 8.0, 8.0, "P")
        val degenerate = tangent(
            Tangent.create(
                board = board,
                firstParent = sourcePoint,
                secondParent = sourceLine,
                id = "degenerate",
                point1Id = sourcePoint.id,
                point2Id = sourceLine.id,
            ),
        )
        board.update()

        assertSame(point1, degenerate.point1)
        assertSame(point2, degenerate.point2)
        assertTrue(degenerate.stdform.take(3).all(Double::isNaN))
        assertEquals(
            setOf("A", "B", "source", "P", "degenerate"),
            board.objects.keys,
        )

        assertEquals(
            TangentError.UnsupportedParents(
                listOf("line", "point"),
            ),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.createPolarLine(
                    board = board,
                    firstParent = sourceLine,
                    secondParent = sourcePoint,
                ),
            ).error,
        )

        val otherBoard = board("line-other")
        val foreignPoint = point(otherBoard, 1.0, 1.0, "foreignPoint")
        val foreignLine = line(
            otherBoard,
            point(otherBoard, 0.0, 0.0, "foreignA"),
            point(otherBoard, 0.0, 2.0, "foreignB"),
            "foreignLine",
        )
        assertEquals(
            TangentError.ParentBoardMismatch(0),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = foreignLine,
                    secondParent = sourcePoint,
                ),
            ).error,
        )
        assertEquals(
            TangentError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = sourceLine,
                    secondParent = foreignPoint,
                ),
            ).error,
        )

        val beforeDuplicate = board.objects.keys.toSet()
        val point1ChildrenBefore = point1.childElements.keys.toSet()
        val point2ChildrenBefore = point2.childElements.keys.toSet()
        val duplicate = assertIs<
            GMResult.Err<TangentError.LineCreation>,
            >(
            Tangent.create(
                board = board,
                firstParent = sourceLine,
                secondParent = sourcePoint,
                id = sourcePoint.id,
            ),
        ).error
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId(sourcePoint.id),
            ),
            duplicate.error,
        )
        assertEquals(beforeDuplicate, board.objects.keys.toSet())
        assertEquals(point1ChildrenBefore, point1.childElements.keys)
        assertEquals(point2ChildrenBefore, point2.childElements.keys)
    }

    @Test
    fun curveBranchMatchesOfficialGeometryAliasesAndDynamicUpdates() {
        val board = board("curve-geometry")
        val functionCurve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x - 1",
                minimumSource = "-4",
                maximumSource = "4",
                id = "functionCurve",
                name = "",
            ),
        )
        val functionPoint = point(
            board,
            2.0,
            5.0,
            "functionPoint",
        )
        val functionTangent = tangent(
            Tangent.create(
                board = board,
                firstParent = functionCurve,
                secondParent = functionPoint,
                id = "functionTangent",
                name = "",
                straightFirst = false,
                straightLast = true,
                point1Id = "functionTangentPoint1",
                point1Name = "",
                point2Id = "functionTangentPoint2",
                point2Name = "",
            ),
        )
        val functionPolar = tangent(
            Tangent.create(
                board = board,
                firstParent = functionPoint,
                secondParent = functionCurve,
                id = "functionPolar",
                name = "",
                straightFirst = true,
                straightLast = false,
                point1Id = "functionPolarPoint1",
                point1Name = "",
                point2Id = "functionPolarPoint2",
                point2Name = "",
            ),
        )
        val parametricCurve = curve(
            Curve.createParametric(
                board = board,
                xSource = "2 * cos(x)",
                ySource = "sin(x)",
                minimumSource = "0",
                maximumSource = "6.283185307179586",
                id = "parametricCurve",
                name = "",
            ),
        )
        val parametricPoint = point(
            board,
            3.0,
            0.75,
            "parametricPoint",
        )
        val parametricTangent = tangent(
            Tangent.create(
                board = board,
                firstParent = parametricPoint,
                secondParent = parametricCurve,
                id = "parametricTangent",
                name = "",
                point1Id = "parametricTangentPoint1",
                point1Name = "",
                point2Id = "parametricTangentPoint2",
                point2Name = "",
            ),
        )
        val shiftedParametricCurve = curve(
            Curve.createParametric(
                board = board,
                xSource = "2 * cos(x) + 3.5",
                ySource = "1.4 * sin(x) - 1",
                minimumSource = "0",
                maximumSource = "6.283185307179586",
                id = "shiftedParametricCurve",
                name = "",
            ),
        )
        val shiftedParametricPoint = point(
            board,
            6.0,
            1.2,
            "shiftedParametricPoint",
        )
        val shiftedParametricTangent = tangent(
            Tangent.create(
                board = board,
                firstParent = shiftedParametricPoint,
                secondParent = shiftedParametricCurve,
                id = "shiftedParametricTangent",
                name = "",
                point1Id = "shiftedParametricTangentPoint1",
                point1Name = "",
                point2Id = "shiftedParametricTangentPoint2",
                point2Name = "",
            ),
        )
        val plotCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 5.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 3.0),
                id = "plotCurve",
                name = "",
            ),
        )
        val plotPoint = point(board, 0.25, 2.5, "plotPoint")
        val plotTangent = tangent(
            Tangent.create(
                board = board,
                firstParent = plotCurve,
                secondParent = plotPoint,
                id = "plotTangent",
                name = "",
                straightFirst = false,
                straightLast = false,
                point1Id = "plotTangentPoint1",
                point1Name = "",
                point2Id = "plotTangentPoint2",
                point2Name = "",
            ),
        )
        board.update()

        val tangents = listOf(
            functionTangent,
            functionPolar,
            parametricTangent,
            shiftedParametricTangent,
            plotTangent,
        )
        for (line in tangents) {
            assertEquals("tangent", line.elType)
            assertEquals(Const.OBJECT_TYPE_TANGENT, line.type)
            assertEquals(Const.OBJECT_CLASS_LINE, line.elementClass)
            assertTrue(line.constrained)
            assertFalse(line.isDraggable)
            assertEquals(
                listOf<GeometryElement>(line.point1, line.point2),
                line.inherits,
            )
            assertHelper(line.point1, line.point1.coords.usrCoords)
            assertHelper(line.point2, line.point2.coords.usrCoords)
        }
        assertFalse(functionTangent.straightFirst)
        assertTrue(functionTangent.straightLast)
        assertTrue(functionPolar.straightFirst)
        assertFalse(functionPolar.straightLast)
        assertFalse(plotTangent.straightFirst)
        assertFalse(plotTangent.straightLast)
        assertEquals(
            listOf("functionCurve", "functionPoint"),
            functionTangent.parents,
        )
        assertEquals(
            listOf("functionPoint", "functionCurve"),
            functionPolar.parents,
        )
        assertEquals(
            listOf("parametricPoint", "parametricCurve"),
            parametricTangent.parents,
        )
        assertSame(functionPoint, functionTangent.glider)
        assertSame(functionPoint, functionPolar.glider)
        assertSame(parametricPoint, parametricTangent.glider)
        assertSame(
            shiftedParametricPoint,
            shiftedParametricTangent.glider,
        )
        assertSame(plotPoint, plotTangent.glider)
        assertTrue(functionCurve.childElements.isEmpty())
        assertTrue(parametricCurve.childElements.isEmpty())
        assertTrue(shiftedParametricCurve.childElements.isEmpty())
        assertTrue(plotCurve.childElements.isEmpty())
        assertEquals(
            setOf("functionTangent", "functionPolar"),
            functionPoint.childElements.keys,
        )
        assertEquals(
            setOf("parametricTangent"),
            parametricPoint.childElements.keys,
        )
        assertEquals(
            setOf("shiftedParametricTangent"),
            shiftedParametricPoint.childElements.keys,
        )
        assertEquals(
            setOf("plotTangent"),
            plotPoint.childElements.keys,
        )
        assertArrayMatches(
            doubleArrayOf(
                0.727606875108999,
                -0.9701425001453319,
                0.24253562503633308,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            functionTangent.stdform,
        )
        assertArrayMatches(functionTangent.stdform, functionPolar.stdform)
        assertArrayMatches(
            doubleArrayOf(
                3.0259331583782143,
                -0.899323297570899,
                -0.4372843542206881,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
            ),
            parametricTangent.stdform,
            tolerance = CURVE_PROJECTION_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(
                4.659686662762056,
                -0.6196366910964116,
                -0.7848887634863236,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
            ),
            shiftedParametricTangent.stdform,
            tolerance = CURVE_PROJECTION_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(
                -0.7071067811865475,
                0.7071067811865477,
                0.7071067811865474,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            plotTangent.stdform,
        )

        functionPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.0, 4.0),
        )
        parametricPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.5, -1.25),
        )
        plotPoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(4.0, 4.0),
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(
                -0.894427190999916,
                0.8944271909999159,
                0.4472135954999579,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            functionTangent.stdform,
        )
        assertArrayMatches(functionTangent.stdform, functionPolar.stdform)
        assertArrayMatches(
            doubleArrayOf(
                2.6232847390421026,
                0.6850799541785333,
                0.7284678828766158,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            parametricTangent.stdform,
            tolerance = CURVE_PROJECTION_TOLERANCE,
        )
        assertArrayMatches(
            doubleArrayOf(
                2.199999999999999,
                -0.8000000000000002,
                0.5999999999999998,
                0.0,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            plotTangent.stdform,
        )
    }

    @Test
    fun curveBranchRemovalMatchesOfficialAsymmetricOwnership() {
        val directBoard = board("curve-direct-removal")
        val directCurve = functionCurve(directBoard, "directCurve")
        val directPoint = point(directBoard, 1.0, 1.0, "directPoint")
        val directTangent = curveTangent(
            directBoard,
            directCurve,
            directPoint,
            "directTangent",
        )
        val directHelper1 = directTangent.point1
        val directHelper2 = directTangent.point2

        directBoard.removeObject(directTangent)

        assertSame(directCurve, directBoard.elementById(directCurve.id))
        assertSame(directPoint, directBoard.elementById(directPoint.id))
        assertEquals(null, directBoard.elementById(directTangent.id))
        assertSame(directHelper1, directBoard.elementById(directHelper1.id))
        assertSame(directHelper2, directBoard.elementById(directHelper2.id))
        assertTrue(directPoint.childElements.isEmpty())

        val pointBoard = board("curve-point-removal")
        val pointCurve = functionCurve(pointBoard, "pointCurve")
        val removedPoint = point(pointBoard, 1.0, 1.0, "removedPoint")
        val pointTangent = curveTangent(
            pointBoard,
            pointCurve,
            removedPoint,
            "pointTangent",
        )
        val pointHelper1 = pointTangent.point1
        val pointHelper2 = pointTangent.point2

        pointBoard.removeObject(removedPoint)

        assertSame(pointCurve, pointBoard.elementById(pointCurve.id))
        assertEquals(null, pointBoard.elementById(removedPoint.id))
        assertEquals(null, pointBoard.elementById(pointTangent.id))
        assertSame(pointHelper1, pointBoard.elementById(pointHelper1.id))
        assertSame(pointHelper2, pointBoard.elementById(pointHelper2.id))

        val curveBoard = board("curve-parent-removal")
        val removedCurve = functionCurve(curveBoard, "removedCurve")
        val curvePoint = point(curveBoard, 1.0, 1.0, "curvePoint")
        val retainedTangent = curveTangent(
            curveBoard,
            removedCurve,
            curvePoint,
            "retainedTangent",
        )

        curveBoard.removeObject(removedCurve)

        assertEquals(null, curveBoard.elementById(removedCurve.id))
        assertSame(curvePoint, curveBoard.elementById(curvePoint.id))
        assertSame(
            retainedTangent,
            curveBoard.elementById(retainedTangent.id),
        )
        assertSame(
            retainedTangent.point1,
            curveBoard.elementById(retainedTangent.point1.id),
        )
        assertSame(
            retainedTangent.point2,
            curveBoard.elementById(retainedTangent.point2.id),
        )
        assertEquals(
            setOf(retainedTangent.id),
            curvePoint.childElements.keys,
        )
    }

    @Test
    fun curveBranchDegenerateAndFailureBehaviorIsStructuredAndAtomic() {
        val board = board("curve-errors")
        val duplicateCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(2.0, 2.0),
                dataY = doubleArrayOf(-1.0, -1.0),
                id = "duplicateCurve",
                name = "",
            ),
        )
        val duplicatePoint = point(
            board,
            5.0,
            5.0,
            "duplicatePoint",
        )
        val duplicateTangent = curveTangent(
            board,
            duplicateCurve,
            duplicatePoint,
            "duplicateTangent",
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            duplicateTangent.point1.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            duplicateTangent.point2.coords.usrCoords,
        )
        assertTrue(duplicateTangent.stdform.take(3).all(Double::isNaN))

        val onePointCurve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(1.0),
                dataY = doubleArrayOf(2.0),
                id = "onePointCurve",
                name = "",
            ),
        )
        val onePoint = point(board, 1.0, 2.0, "onePoint")
        val beforeOnePoint = board.objects.keys.toSet()
        assertEquals(
            TangentError.InvalidCurvePointCount(1),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = onePointCurve,
                    secondParent = onePoint,
                    id = "onePointTangent",
                    point1Id = "onePointHelper1",
                    point2Id = "onePointHelper2",
                ),
            ).error,
        )
        assertEquals(beforeOnePoint, board.objects.keys.toSet())

        assertEquals(
            TangentError.UnsupportedParents(
                listOf("curve", "point"),
            ),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.createPolarLine(
                    board = board,
                    firstParent = duplicateCurve,
                    secondParent = duplicatePoint,
                ),
            ).error,
        )

        val otherBoard = board("curve-other")
        val foreignCurve = functionCurve(otherBoard, "foreignCurve")
        val foreignPoint = point(otherBoard, 2.0, 4.0, "foreignPoint")
        assertEquals(
            TangentError.ParentBoardMismatch(0),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = foreignCurve,
                    secondParent = duplicatePoint,
                ),
            ).error,
        )
        assertEquals(
            TangentError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = duplicateCurve,
                    secondParent = foreignPoint,
                ),
            ).error,
        )

        val taken = point(board, 8.0, 8.0, "taken")
        val beforeHelperFailure = board.objects.keys.toSet()
        val helperFailure = assertIs<
            GMResult.Err<TangentError.PointCreation>,
            >(
            Tangent.create(
                board = board,
                firstParent = duplicateCurve,
                secondParent = duplicatePoint,
                id = "unused",
                point1Id = "temporary",
                point2Id = taken.id,
            ),
        ).error
        assertEquals(1, helperFailure.pointIndex)
        assertEquals(beforeHelperFailure, board.objects.keys.toSet())
        assertEquals(null, board.elementById("temporary"))

        val beforeLineFailure = board.objects.keys.toSet()
        val lineFailure = assertIs<
            GMResult.Err<TangentError.LineCreation>,
            >(
            Tangent.create(
                board = board,
                firstParent = duplicateCurve,
                secondParent = duplicatePoint,
                id = taken.id,
                point1Id = "temporary1",
                point2Id = "temporary2",
            ),
        ).error
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId(taken.id),
            ),
            lineFailure.error,
        )
        assertEquals(beforeLineFailure, board.objects.keys.toSet())
        assertEquals(null, board.elementById("temporary1"))
        assertEquals(null, board.elementById("temporary2"))
    }

    @Test
    fun invalidParentsAndDuplicateIdsAreStructuredAndAtomic() {
        val board = board("errors")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 3.0, 1.0, "sourcePoint")
        val otherPoint = point(board, -2.0, 1.0, "otherPoint")
        val otherCircle = circle(board, center, otherPoint, "otherCircle")

        assertEquals(
            TangentError.UnsupportedParents(
                listOf("circle", "circle"),
            ),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = circle,
                    secondParent = otherCircle,
                ),
            ).error,
        )
        assertEquals(
            TangentError.UnsupportedParents(
                listOf("point", "point"),
            ),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.createPolarLine(
                    board = board,
                    firstParent = sourcePoint,
                    secondParent = otherPoint,
                ),
            ).error,
        )

        val otherBoard = board("other")
        val foreignPoint = point(
            otherBoard,
            4.0,
            2.0,
            "foreignPoint",
        )
        assertEquals(
            TangentError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = circle,
                    secondParent = foreignPoint,
                ),
            ).error,
        )

        board.removeObject(sourcePoint)
        assertEquals(
            TangentError.ParentNotRegistered(1, "sourcePoint"),
            assertIs<GMResult.Err<TangentError>>(
                Tangent.create(
                    board = board,
                    firstParent = circle,
                    secondParent = sourcePoint,
                ),
            ).error,
        )

        val replacementPoint = point(
            board,
            3.0,
            1.0,
            "replacementPoint",
        )
        val taken = point(board, 8.0, 8.0, "taken")
        val beforeHelperFailure = board.objects.keys.toSet()
        val helperFailure = assertIs<
            GMResult.Err<TangentError.PointCreation>,
            >(
            Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = replacementPoint,
                id = "unused",
                point1Id = "temporary",
                point2Id = taken.id,
            ),
        ).error
        assertEquals(1, helperFailure.pointIndex)
        assertEquals(
            PointError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            helperFailure.error,
        )
        assertEquals(beforeHelperFailure, board.objects.keys.toSet())

        val beforeLineFailure = board.objects.keys.toSet()
        val lineFailure = assertIs<
            GMResult.Err<TangentError.LineCreation>,
            >(
            Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = replacementPoint,
                id = taken.id,
                point1Id = "temporary1",
                point2Id = "temporary2",
            ),
        ).error
        assertEquals(
            LineError.Registration(
                BoardError.DuplicateElementId("taken"),
            ),
            lineFailure.error,
        )
        assertEquals(beforeLineFailure, board.objects.keys.toSet())
        assertSame(taken, board.elementById(taken.id))
        assertTrue(circle.childElements.isEmpty())
        assertTrue(replacementPoint.childElements.isEmpty())
    }

    private fun board(id: String): Board =
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

    private fun circle(
        board: Board,
        center: Point,
        radiusPoint: Point,
        id: String,
    ): Circle =
        assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = center,
                point2 = radiusPoint,
                id = id,
                name = "",
            ),
        ).value

    private fun line(
        board: Board,
        point1: Point,
        point2: Point,
        id: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                id = id,
                name = "",
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun functionCurve(
        board: Board,
        id: String,
    ): Curve =
        curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x",
                minimumSource = "-4",
                maximumSource = "4",
                id = id,
                name = "",
            ),
        )

    private fun curveTangent(
        board: Board,
        curve: Curve,
        point: Point,
        id: String,
    ): Line =
        tangent(
            Tangent.create(
                board = board,
                firstParent = curve,
                secondParent = point,
                id = id,
                name = "",
                point1Id = "${id}Point1",
                point1Name = "",
                point2Id = "${id}Point2",
                point2Name = "",
            ),
        )

    private fun tangent(
        result: GMResult<Line, TangentError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun assertHelper(
        point: Point,
        expected: DoubleArray,
    ) {
        assertEquals(Const.OBJECT_TYPE_CAS, point.type)
        assertTrue(point.isConstrained)
        assertFalse(point.isDraggable)
        assertFalse(point.isFixed)
        assertTrue(point.parents.isEmpty())
        assertArrayMatches(expected, point.coords.usrCoords)
    }

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
        tolerance: Double = TOLERANCE,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            val expectedValue = expected[index]
            val actualValue = actual[index]
            when {
                expectedValue.isNaN() ->
                    assertTrue(actualValue.isNaN())
                expectedValue == Double.POSITIVE_INFINITY ->
                    assertEquals(Double.POSITIVE_INFINITY, actualValue)
                expectedValue == Double.NEGATIVE_INFINITY ->
                    assertEquals(Double.NEGATIVE_INFINITY, actualValue)
                else ->
                    assertTrue(
                        abs(expectedValue - actualValue) <= tolerance,
                        "Expected $expectedValue at $index, got $actualValue",
                    )
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
        const val CURVE_PROJECTION_TOLERANCE = 1.0e-9
    }
}
