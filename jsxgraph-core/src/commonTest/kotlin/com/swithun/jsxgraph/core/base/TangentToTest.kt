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

class TangentToTest {
    @Test
    fun officialCircleGeometryMetadataRelationsAndUpdatesArePreserved() {
        val board = board("geometry")
        val center = point(board, 1.0, 1.0, "center")
        val radius = point(board, 4.0, 1.0, "radius")
        val circle = circle(board, center, radius, "sourceCircle")
        val sourcePoint = point(board, 5.0, 4.0, "sourcePoint")
        val first = tangentTo(
            board = board,
            circle = circle,
            sourcePoint = sourcePoint,
            number = 0.0,
            prefix = "first",
        )
        val second = tangentTo(
            board = board,
            circle = circle,
            sourcePoint = sourcePoint,
            number = 1.0,
            prefix = "second",
        )
        board.update()

        assertTangentToMetadata(first, circle, sourcePoint, "first")
        assertTangentToMetadata(second, circle, sourcePoint, "second")
        assertArrayMatches(
            doubleArrayOf(1.0, 1.0000000000000073, 4.0),
            first.tangentToPoint!!.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(
                4.0000000000000036,
                -2.6645352591003757e-15,
                -1.0,
            ),
            first.stdform.take(3).toDoubleArray(),
        )
        assertArrayMatches(
            doubleArrayOf(1.0, 3.88, 0.16000000000000036),
            second.tangentToPoint!!.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(
                3.680000000000002,
                -0.9600000000000001,
                0.27999999999999997,
            ),
            second.stdform.take(3).toDoubleArray(),
        )
        for (polar in listOf(first.tangentToPolar!!, second.tangentToPolar!!)) {
            assertArrayMatches(
                doubleArrayOf(1.0, 2.8, 1.6),
                polar.point1.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(1.0, 2.68, 1.76),
                polar.point2.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(
                    3.200000000000003,
                    -0.8000000000000007,
                    -0.5999999999999991,
                ),
                polar.stdform.take(3).toDoubleArray(),
            )
        }
        assertEquals(
            listOf(
                "center",
                "radius",
                "sourceCircle",
                "sourcePoint",
                "firstPolarPoint1",
                "firstPolarPoint2",
                "firstPolar",
                "firstIntersection",
                "firstTangentPoint1",
                "firstTangentPoint2",
                "firstTangent",
                "secondPolarPoint1",
                "secondPolarPoint2",
                "secondPolar",
                "secondIntersection",
                "secondTangentPoint1",
                "secondTangentPoint2",
                "secondTangent",
            ),
            board.objectsList.map(GeometryElement::id),
        )
        assertEquals(
            setOf("firstIntersection", "secondIntersection"),
            circle.childElements.keys,
        )
        assertEquals(
            setOf("firstPolar", "secondPolar"),
            sourcePoint.childElements.keys,
        )

        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 2.0),
        )
        radius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        sourcePoint.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -3.0),
        )
        board.update()

        assertArrayMatches(
            doubleArrayOf(1.0, 1.93238075793812, 2.73238075793812),
            first.tangentToPoint!!.coords.usrCoords,
        )
        assertArrayMatches(
            doubleArrayOf(1.0, -2.73238075793812, -1.93238075793812),
            second.tangentToPoint!!.coords.usrCoords,
        )
        for (polar in listOf(first.tangentToPolar!!, second.tangentToPolar!!)) {
            assertArrayMatches(
                doubleArrayOf(1.0, -0.6, 0.2),
                polar.point1.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(1.0, -0.5, 0.3),
                polar.point2.coords.usrCoords,
            )
            assertArrayMatches(
                doubleArrayOf(
                    -0.565685424949238,
                    -0.7071067811865475,
                    0.7071067811865475,
                ),
                polar.stdform.take(3).toDoubleArray(),
            )
        }
    }

    @Test
    fun nestedIdentityAndConstructionAttributesReachEveryCreatedElement() {
        val board = board("attributes")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 3.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 5.0, 1.0, "source")
        val tangent = assertIs<GMResult.Ok<Line>>(
            TangentTo.create(
                board = board,
                conic = circle,
                pointFrom = sourcePoint,
                tangentAttributes = TangentToLineAttributes(
                    identity = TangentToIdentity(
                        id = "tangent",
                        name = "outer",
                        needsRegularUpdate = false,
                    ),
                    straightFirst = false,
                    straightLast = true,
                    point1 = TangentToIdentity(
                        id = "tangentPoint1",
                        name = "outerP1",
                        needsRegularUpdate = false,
                    ),
                    point2 = TangentToIdentity(
                        id = "tangentPoint2",
                        name = "outerP2",
                        needsRegularUpdate = false,
                    ),
                ),
                polarAttributes = TangentToLineAttributes(
                    identity = TangentToIdentity(
                        id = "polar",
                        name = "inner",
                        needsRegularUpdate = false,
                    ),
                    straightFirst = true,
                    straightLast = false,
                    point1 = TangentToIdentity(
                        id = "polarPoint1",
                        name = "innerP1",
                        needsRegularUpdate = false,
                    ),
                    point2 = TangentToIdentity(
                        id = "polarPoint2",
                        name = "innerP2",
                        needsRegularUpdate = false,
                    ),
                ),
                pointAttributes = TangentToPointAttributes(
                    identity = TangentToIdentity(
                        id = "intersection",
                        name = "contact",
                        needsRegularUpdate = false,
                    ),
                    fixed = true,
                ),
            ),
        ).value
        val polar = tangent.tangentToPolar!!
        val intersection = tangent.tangentToPoint!!

        assertEquals("outer", tangent.name)
        assertFalse(tangent.needsRegularUpdate)
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals("outerP1", tangent.point1.name)
        assertEquals("outerP2", tangent.point2.name)
        assertFalse(tangent.point1.needsRegularUpdate)
        assertFalse(tangent.point2.needsRegularUpdate)

        assertEquals("inner", polar.name)
        assertFalse(polar.needsRegularUpdate)
        assertTrue(polar.straightFirst)
        assertFalse(polar.straightLast)
        assertEquals("innerP1", polar.point1.name)
        assertEquals("innerP2", polar.point2.name)
        assertFalse(polar.point1.needsRegularUpdate)
        assertFalse(polar.point2.needsRegularUpdate)

        assertEquals("contact", intersection.name)
        assertFalse(intersection.needsRegularUpdate)
        assertTrue(intersection.isFixed)
    }

    @Test
    fun zeroAndNaNSelectFirstIntersectionAndOtherDoublesSelectSecond() {
        val board = board("indices")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 3.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 5.0, 1.0, "source")
        val cases = listOf(
            "omitted" to 0.0,
            "zero" to -0.0,
            "one" to 1.0,
            "two" to 2.0,
            "negative" to -1.0,
            "fractional" to 0.5,
            "nan" to Double.NaN,
            "positiveInfinity" to Double.POSITIVE_INFINITY,
            "negativeInfinity" to Double.NEGATIVE_INFINITY,
        )
        val outputs = cases.associate { (name, number) ->
            name to tangentTo(
                board = board,
                circle = circle,
                sourcePoint = sourcePoint,
                number = number,
                prefix = name,
            )
        }
        board.update()

        val expectedFirst = doubleArrayOf(
            1.0,
            1.255026273967192,
            2.724868630164036,
        )
        val expectedSecond = doubleArrayOf(
            1.0,
            2.2065121875712697,
            -2.0325609378563416,
        )
        assertArrayMatches(
            expectedFirst,
            outputs.getValue("omitted").tangentToPoint!!.coords.usrCoords,
        )
        assertArrayMatches(
            expectedFirst,
            outputs.getValue("zero").tangentToPoint!!.coords.usrCoords,
        )
        assertArrayMatches(
            expectedFirst,
            outputs.getValue("nan").tangentToPoint!!.coords.usrCoords,
        )
        for (
            name in listOf(
                "one",
                "two",
                "negative",
                "fractional",
                "positiveInfinity",
                "negativeInfinity",
            )
        ) {
            assertArrayMatches(
                expectedSecond,
                outputs.getValue(name).tangentToPoint!!.coords.usrCoords,
            )
        }
    }

    @Test
    fun nonRealAndDegenerateGeometryPropagatesOfficialNaNCoordinates() {
        val inside = createGeometryCase(
            id = "inside",
            radiusCoordinates = doubleArrayOf(3.0, 0.0),
            sourceCoordinates = doubleArrayOf(1.0, 1.0),
        )
        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            inside.tangentToPoint!!.coords.usrCoords,
        )
        assertTrue(inside.stdform.take(3).all(Double::isNaN))
        assertTrue(
            inside.tangentToPolar!!.stdform.take(3).all(Double::isFinite),
        )

        val centered = createGeometryCase(
            id = "centered",
            radiusCoordinates = doubleArrayOf(3.0, 0.0),
            sourceCoordinates = doubleArrayOf(0.0, 0.0),
        )
        assertArrayMatches(
            doubleArrayOf(0.0, 0.0, 0.0),
            centered.tangentToPoint!!.coords.usrCoords,
        )
        assertTrue(centered.stdform.take(3).all(Double::isNaN))
        assertTrue(
            centered.tangentToPolar!!.stdform.take(3).all(Double::isNaN),
        )

        val degenerate = createGeometryCase(
            id = "degenerate",
            centerCoordinates = doubleArrayOf(2.0, -1.0),
            radiusCoordinates = doubleArrayOf(2.0, -1.0),
            sourceCoordinates = doubleArrayOf(4.0, 3.0),
        )
        assertArrayMatches(
            doubleArrayOf(1.0, 2.0, -1.0),
            degenerate.tangentToPoint!!.coords.usrCoords,
        )
        assertTrue(degenerate.stdform.take(3).all(Double::isNaN))
        assertArrayMatches(
            doubleArrayOf(
                0.0,
                -0.4472135954999579,
                -0.8944271909999159,
            ),
            degenerate.tangentToPolar!!.stdform.take(3).toDoubleArray(),
        )
    }

    @Test
    fun directAndSourcePointRemovalMatchOfficialCompositeLifecycle() {
        val direct = construction("direct")
        val directCreated = createdElements(direct.tangent)

        direct.board.removeObject(direct.tangent)

        assertEquals(null, direct.board.elementById(direct.tangent.id))
        for (element in directCreated - direct.tangent) {
            assertSame(element, direct.board.elementById(element.id))
        }
        assertEquals(
            setOf(direct.intersection.id),
            direct.circle.childElements.keys,
        )
        assertEquals(
            setOf(direct.polar.id),
            direct.sourcePoint.childElements.keys,
        )

        val sourceRemoval = construction("sourceRemoval")
        val sourceCreated = createdElements(sourceRemoval.tangent)

        sourceRemoval.board.removeObject(sourceRemoval.sourcePoint)

        assertEquals(
            null,
            sourceRemoval.board.elementById(sourceRemoval.sourcePoint.id),
        )
        assertSame(
            sourceRemoval.circle,
            sourceRemoval.board.elementById(sourceRemoval.circle.id),
        )
        for (
            removed in listOf(
                sourceRemoval.polar,
                sourceRemoval.intersection,
                sourceRemoval.tangent,
            )
        ) {
            assertEquals(null, sourceRemoval.board.elementById(removed.id))
        }
        for (
            retained in sourceCreated.filterIsInstance<Point>()
                .filterNot { it === sourceRemoval.intersection }
        ) {
            assertSame(retained, sourceRemoval.board.elementById(retained.id))
        }
    }

    @Test
    fun unsupportedAndInvalidParentsReturnStructuredErrorsWithoutMutation() {
        val board = board("validation")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 4.0, 3.0, "source")
        val linePoint = point(board, -2.0, 0.0, "linePoint")
        val line = line(board, center, linePoint, "line")
        val beforeUnsupported = snapshot(board, circle, sourcePoint)

        assertEquals(
            TangentToError.UnsupportedParents(
                listOf("line", "point"),
            ),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(board, line, sourcePoint),
            ).error,
        )
        assertSnapshot(beforeUnsupported, board, circle, sourcePoint)

        val conic = GeometryElement(
            board = board,
            id = "conic",
            name = "",
            type = Const.OBJECT_TYPE_CONIC,
            elementClass = Const.OBJECT_CLASS_CURVE,
        ).also {
            it.elType = "ellipse"
            assertIs<GMResult.Ok<String>>(board.setId(it, "G"))
        }
        assertEquals(
            TangentToError.UnsupportedConic("ellipse"),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(board, conic, sourcePoint),
            ).error,
        )

        val otherBoard = board("other")
        val foreignPoint = point(otherBoard, 1.0, 1.0, "foreign")
        assertEquals(
            TangentToError.ParentBoardMismatch(1),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(board, circle, foreignPoint),
            ).error,
        )

        board.removeObject(sourcePoint)
        val beforeUnregistered = board.objects.keys.toSet()
        assertEquals(
            TangentToError.ParentNotRegistered(1, "source"),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(board, circle, sourcePoint),
            ).error,
        )
        assertEquals(beforeUnregistered, board.objects.keys.toSet())
    }

    @Test
    fun allExplicitIdCollisionsFailAtomicallyBeforeConstruction() {
        val slots = listOf(
            "polarPoint1",
            "polarPoint2",
            "polar",
            "intersection",
            "tangentPoint1",
            "tangentPoint2",
            "tangent",
        )
        for (slot in slots) {
            val fixture = collisionFixture(slot)
            val before = snapshot(
                fixture.board,
                fixture.circle,
                fixture.sourcePoint,
            )

            assertEquals(
                TangentToError.DuplicateElementId("taken"),
                assertIs<GMResult.Err<TangentToError>>(
                    TangentTo.create(
                        board = fixture.board,
                        conic = fixture.circle,
                        pointFrom = fixture.sourcePoint,
                        tangentAttributes = fixture.tangentAttributes,
                        polarAttributes = fixture.polarAttributes,
                        pointAttributes = fixture.pointAttributes,
                    ),
                ).error,
                "Unexpected error for $slot",
            )
            assertSnapshot(
                before,
                fixture.board,
                fixture.circle,
                fixture.sourcePoint,
            )
        }

        val board = board("crossDuplicate")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 4.0, 3.0, "source")
        val before = snapshot(board, circle, sourcePoint)
        assertEquals(
            TangentToError.DuplicateElementId("shared"),
            assertIs<GMResult.Err<TangentToError>>(
                TangentTo.create(
                    board = board,
                    conic = circle,
                    pointFrom = sourcePoint,
                    tangentAttributes = lineAttributes(
                        prefix = "tangent",
                        lineId = "shared",
                    ),
                    polarAttributes = lineAttributes(
                        prefix = "polar",
                        point1Id = "shared",
                    ),
                    pointAttributes = TangentToPointAttributes(
                        TangentToIdentity("intersection", ""),
                    ),
                ),
            ).error,
        )
        assertSnapshot(before, board, circle, sourcePoint)
    }

    private fun assertTangentToMetadata(
        tangent: Line,
        circle: Circle,
        sourcePoint: Point,
        prefix: String,
    ) {
        val intersection = tangent.tangentToPoint!!
        val polar = tangent.tangentToPolar!!

        assertEquals("tangentto", tangent.elType)
        assertEquals(Const.OBJECT_TYPE_TANGENT, tangent.type)
        assertEquals(Const.OBJECT_CLASS_LINE, tangent.elementClass)
        assertTrue(tangent.constrained)
        assertFalse(tangent.isDraggable)
        assertTrue(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertSame(intersection, tangent.glider)
        assertSame(intersection, tangent.tangentToPoint)
        assertSame(polar, tangent.tangentToPolar)
        assertTrue(tangent.subs.isEmpty())
        assertEquals(
            listOf<GeometryElement>(tangent.point1, tangent.point2),
            tangent.inherits,
        )
        assertEquals(
            listOf(circle.id, intersection.id),
            tangent.parents,
        )

        assertEquals("${prefix}Intersection", intersection.id)
        assertEquals("intersection", intersection.elType)
        assertEquals(Const.OBJECT_TYPE_INTERSECTION, intersection.type)
        assertFalse(intersection.isDraggable)
        assertEquals(listOf(polar.id, circle.id), intersection.parents)
        assertEquals(setOf(tangent.id), intersection.childElements.keys)

        assertEquals("${prefix}Polar", polar.id)
        assertEquals("tangent", polar.elType)
        assertEquals(Const.OBJECT_TYPE_TANGENT, polar.type)
        assertSame(sourcePoint, polar.glider)
        assertEquals(listOf(circle.id, sourcePoint.id), polar.parents)
        assertEquals(setOf(intersection.id), polar.childElements.keys)
        assertEquals(
            listOf<GeometryElement>(polar.point1, polar.point2),
            polar.inherits,
        )
    }

    private fun tangentTo(
        board: Board,
        circle: Circle,
        sourcePoint: Point,
        number: Double,
        prefix: String,
    ): Line =
        assertIs<GMResult.Ok<Line>>(
            TangentTo.create(
                board = board,
                conic = circle,
                pointFrom = sourcePoint,
                number = number,
                tangentAttributes = lineAttributes("${prefix}Tangent"),
                polarAttributes = lineAttributes("${prefix}Polar"),
                pointAttributes = TangentToPointAttributes(
                    TangentToIdentity(
                        id = "${prefix}Intersection",
                        name = "",
                    ),
                ),
            ),
        ).value

    private fun lineAttributes(
        prefix: String,
        lineId: String = prefix,
        point1Id: String = "${prefix}Point1",
        point2Id: String = "${prefix}Point2",
    ): TangentToLineAttributes =
        TangentToLineAttributes(
            identity = TangentToIdentity(lineId, ""),
            point1 = TangentToIdentity(point1Id, ""),
            point2 = TangentToIdentity(point2Id, ""),
        )

    private fun createGeometryCase(
        id: String,
        centerCoordinates: DoubleArray = doubleArrayOf(0.0, 0.0),
        radiusCoordinates: DoubleArray,
        sourceCoordinates: DoubleArray,
    ): Line {
        val board = board(id)
        val center = point(
            board,
            centerCoordinates[0],
            centerCoordinates[1],
            "${id}Center",
        )
        val radius = point(
            board,
            radiusCoordinates[0],
            radiusCoordinates[1],
            "${id}Radius",
        )
        val circle = circle(board, center, radius, "${id}Circle")
        val sourcePoint = point(
            board,
            sourceCoordinates[0],
            sourceCoordinates[1],
            "${id}Source",
        )
        return tangentTo(board, circle, sourcePoint, 0.0, id)
            .also { board.update() }
    }

    private fun construction(prefix: String): Construction {
        val board = board(prefix)
        val center = point(board, 0.0, 0.0, "${prefix}Center")
        val radius = point(board, 2.0, 0.0, "${prefix}Radius")
        val circle = circle(board, center, radius, "${prefix}Circle")
        val sourcePoint = point(board, 4.0, 3.0, "${prefix}Source")
        val tangent = tangentTo(
            board,
            circle,
            sourcePoint,
            0.0,
            prefix,
        )
        return Construction(
            board = board,
            circle = circle,
            sourcePoint = sourcePoint,
            tangent = tangent,
            polar = tangent.tangentToPolar!!,
            intersection = tangent.tangentToPoint!!,
        )
    }

    private fun createdElements(tangent: Line): List<GeometryElement> {
        val polar = tangent.tangentToPolar!!
        return listOf(
            polar.point1,
            polar.point2,
            polar,
            tangent.tangentToPoint!!,
            tangent.point1,
            tangent.point2,
            tangent,
        )
    }

    private fun collisionFixture(slot: String): CollisionFixture {
        val board = board("collision$slot")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val circle = circle(board, center, radius, "circle")
        val sourcePoint = point(board, 4.0, 3.0, "source")
        point(board, -7.0, -5.0, "taken")
        fun id(target: String, fallback: String): String =
            if (slot == target) "taken" else fallback
        return CollisionFixture(
            board = board,
            circle = circle,
            sourcePoint = sourcePoint,
            tangentAttributes = TangentToLineAttributes(
                identity = TangentToIdentity(
                    id("tangent", "tangent"),
                    "",
                ),
                point1 = TangentToIdentity(
                    id("tangentPoint1", "tangentPoint1"),
                    "",
                ),
                point2 = TangentToIdentity(
                    id("tangentPoint2", "tangentPoint2"),
                    "",
                ),
            ),
            polarAttributes = TangentToLineAttributes(
                identity = TangentToIdentity(
                    id("polar", "polar"),
                    "",
                ),
                point1 = TangentToIdentity(
                    id("polarPoint1", "polarPoint1"),
                    "",
                ),
                point2 = TangentToIdentity(
                    id("polarPoint2", "polarPoint2"),
                    "",
                ),
            ),
            pointAttributes = TangentToPointAttributes(
                TangentToIdentity(
                    id("intersection", "intersection"),
                    "",
                ),
            ),
        )
    }

    private fun snapshot(
        board: Board,
        circle: Circle,
        sourcePoint: Point,
    ): BoardSnapshot =
        BoardSnapshot(
            objectIds = board.objects.keys.toList(),
            objectListIds = board.objectsList.map(GeometryElement::id),
            numObjects = board.numObjects,
            circleChildren = circle.childElements.keys.toList(),
            circleDescendants = circle.descendants.keys.toList(),
            pointChildren = sourcePoint.childElements.keys.toList(),
            pointDescendants = sourcePoint.descendants.keys.toList(),
        )

    private fun assertSnapshot(
        expected: BoardSnapshot,
        board: Board,
        circle: Circle,
        sourcePoint: Point,
    ) {
        assertEquals(expected, snapshot(board, circle, sourcePoint))
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
                    assertTrue(
                        actualValue.isNaN(),
                        "Expected NaN at $index, got $actualValue",
                    )
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

    private data class Construction(
        val board: Board,
        val circle: Circle,
        val sourcePoint: Point,
        val tangent: Line,
        val polar: Line,
        val intersection: IntersectionPoint,
    )

    private data class CollisionFixture(
        val board: Board,
        val circle: Circle,
        val sourcePoint: Point,
        val tangentAttributes: TangentToLineAttributes,
        val polarAttributes: TangentToLineAttributes,
        val pointAttributes: TangentToPointAttributes,
    )

    private data class BoardSnapshot(
        val objectIds: List<String>,
        val objectListIds: List<String>,
        val numObjects: Int,
        val circleChildren: List<String>,
        val circleDescendants: List<String>,
        val pointChildren: List<String>,
        val pointDescendants: List<String>,
    )

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
