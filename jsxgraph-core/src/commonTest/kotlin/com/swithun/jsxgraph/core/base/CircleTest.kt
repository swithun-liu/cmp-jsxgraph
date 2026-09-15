package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CircleTest {
    @Test
    fun twoPointFactoryRegistersIdentityDependenciesAndNumericState() {
        val board = board()
        val center = point(board, doubleArrayOf(1.0, -2.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))

        val circle = circle(Circle.create(board, center, point2))

        assertEquals("boardC2", circle.id)
        assertEquals("k_{a}", circle.name)
        assertEquals("circle", circle.elType)
        assertEquals("twoPoints", circle.method)
        assertEquals(Const.OBJECT_TYPE_CIRCLE, circle.type)
        assertEquals(Const.OBJECT_TYPE_CIRCLE, circle.originalType)
        assertEquals(Const.OBJECT_CLASS_CIRCLE, circle.elementClass)
        assertNull(circle.baseElement)
        assertSame(center, circle.center)
        assertSame(center, circle.midpoint)
        assertSame(point2, circle.point2)
        assertTrue(circle.isDraggable)
        assertSame(circle, board.select(circle.id))
        assertSame(circle, board.select(circle.name))
        assertEquals(listOf(center.id, point2.id), circle.parents)
        assertSame(circle, center.childElements[circle.id])
        assertSame(circle, point2.childElements[circle.id])
        assertSame(center, circle.ancestors[center.id])
        assertSame(point2, circle.ancestors[point2.id])

        assertEquals(5.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertEquals(5.0, circle.radius, absoluteTolerance = TOLERANCE)
        assertEquals(10.0, circle.Diameter(), absoluteTolerance = TOLERANCE)
        assertEquals(25.0 * PI, circle.Area(), absoluteTolerance = TOLERANCE)
        assertEquals(10.0 * PI, circle.Perimeter(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(doubleArrayOf(-4.0, 3.0, 6.0, -7.0), circle.bounds())
        assertEquals(1.0, circle.X(0.25), absoluteTolerance = TOLERANCE)
        assertEquals(3.0, circle.Y(0.25), absoluteTolerance = TOLERANCE)
        assertEquals(1.0, circle.Z(Double.NaN))
        assertEquals(0.0, circle.minX())
        assertEquals(1.0, circle.maxX())

        assertArrayMatches(
            doubleArrayOf(-2.0, -0.2, 0.4, 0.1, 1.0, 5.0, 1.0, -2.0),
            circle.stdform,
        )
        assertMatrixMatches(
            arrayOf(
                doubleArrayOf(-20.0, -1.0, 2.0),
                doubleArrayOf(-1.0, 1.0, 0.0),
                doubleArrayOf(2.0, 0.0, 1.0),
            ),
            circle.quadraticform,
        )
    }

    @Test
    fun updateBuildsOfficialCubicBezierApproximation() {
        val board = board()
        val circle = circle(
            Circle.create(
                board,
                point(board, doubleArrayOf(1.0, -2.0)),
                point(board, doubleArrayOf(4.0, 2.0)),
            ),
        )

        assertEquals(13, circle.numberPoints)
        assertEquals(3, circle.bezierDegree)
        assertArrayMatches(
            doubleArrayOf(
                6.0,
                6.0,
                3.75957512247,
                1.0,
                -1.75957512247,
                -4.0,
                -4.0,
                -4.0,
                -1.75957512247,
                1.0,
                3.75957512247,
                6.0,
                6.0,
            ),
            circle.dataX,
        )
        assertArrayMatches(
            doubleArrayOf(
                -2.0,
                0.75957512247,
                3.0,
                3.0,
                3.0,
                0.75957512247,
                -2.0,
                -4.75957512247,
                -7.0,
                -7.0,
                -7.0,
                -4.75957512247,
                -2.0,
            ),
            circle.dataY,
        )
        assertEquals(13, circle.points.size)
        for (index in circle.points.indices) {
            assertArrayMatches(
                doubleArrayOf(1.0, circle.dataX[index], circle.dataY[index]),
                circle.points[index].usrCoords,
            )
        }
    }

    @Test
    fun smallAndZeroRadiusNormalizationMatchOfficialResults() {
        val board = board()
        val small = circle(
            Circle.create(
                board,
                point(board, doubleArrayOf(1.0, -2.0)),
                point(board, doubleArrayOf(1.3, -1.6)),
            ),
        )

        assertEquals(0.5, small.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(2.375, -1.0, 2.0, 0.5, 0.5, 0.5, 1.0, -2.0),
            small.stdform,
        )
        assertMatrixMatches(
            arrayOf(
                doubleArrayOf(4.75, -1.0, 2.0),
                doubleArrayOf(-1.0, 1.0, 0.0),
                doubleArrayOf(2.0, 0.0, 1.0),
            ),
            small.quadraticform,
        )

        val coincidentPoint = point(board, doubleArrayOf(2.0, 3.0))
        val zero = circle(Circle.create(board, coincidentPoint, coincidentPoint))

        assertEquals(0.0, zero.Radius())
        assertArrayMatches(
            doubleArrayOf(-6.5, 2.0, 3.0, -0.5, 0.0, 0.0, 2.0, 3.0),
            zero.stdform,
        )
        assertArrayMatches(doubleArrayOf(2.0, 3.0, 2.0, 3.0), zero.bounds())
        assertTrue(zero.dataX.all { it == 2.0 })
        assertTrue(zero.dataY.all { it == 3.0 })
    }

    @Test
    fun idealCircumferencePointPreservesOfficialNonFinitePropagation() {
        val board = board()
        val circle = circle(
            Circle.create(
                board,
                point(board, doubleArrayOf(2.0, 3.0)),
                point(board, doubleArrayOf(0.0, 4.0, -5.0)),
            ),
        )

        assertTrue(circle.Radius().isNaN())
        assertTrue(circle.Diameter().isNaN())
        assertTrue(circle.Area().isNaN())
        assertTrue(circle.Perimeter().isNaN())
        assertArrayMatches(
            doubleArrayOf(
                -1.9414506867883021,
                -0.5547001962252291,
                -0.8320502943378437,
                0.0,
                1.0,
                Double.NaN,
                2.0,
                3.0,
            ),
            circle.stdform,
        )
        assertTrue(circle.quadraticform[0][0].isNaN())
        assertTrue(circle.bounds().all { it.isNaN() })
    }

    @Test
    fun updateRecomputesDerivedStateOnlyWhenRequested() {
        val board = board()
        val center = point(board, doubleArrayOf(1.0, -2.0))
        val point2 = point(board, doubleArrayOf(4.0, 2.0))
        val circle = circle(Circle.create(board, center, point2))
        val initialStandardForm = circle.stdform.copyOf()
        val initialDataX = circle.dataX.copyOf()

        point2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 1.0),
        )
        circle.needsUpdate = false
        assertSame(circle, circle.update(fromParent = true))
        assertArrayMatches(initialStandardForm, circle.stdform)
        assertArrayMatches(initialDataX, circle.dataX)

        circle.needsUpdate = true
        assertSame(circle, circle.update(fromParent = true))
        assertEquals(3.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(
                -2.0 / 3.0,
                -1.0 / 3.0,
                2.0 / 3.0,
                1.0 / 6.0,
                1.0,
                3.0,
                1.0,
                -2.0,
            ),
            circle.stdform,
        )
        assertEquals(4.0, circle.dataX.first(), absoluteTolerance = TOLERANCE)
    }

    @Test
    fun numericRadiusUsesAbsoluteValueAndDependsOnlyOnCenter() {
        val board = board()
        val center = point(board, doubleArrayOf(1.0, -2.0))

        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                radius = -3.0,
            ),
        )

        assertEquals("pointRadius", circle.method)
        assertNull(circle.point2)
        assertNull(circle.line)
        assertNull(circle.circle)
        assertEquals(listOf(center.id), circle.parents)
        assertSame(circle, center.childElements[circle.id])
        assertEquals(3.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertEquals(3.0, circle.radius, absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(
                -2.0 / 3.0,
                -1.0 / 3.0,
                2.0 / 3.0,
                1.0 / 6.0,
                1.0,
                3.0,
                1.0,
                -2.0,
            ),
            circle.stdform,
        )
        assertMatrixMatches(
            arrayOf(
                doubleArrayOf(-4.0, -1.0, 2.0),
                doubleArrayOf(-1.0, 1.0, 0.0),
                doubleArrayOf(2.0, 0.0, 1.0),
            ),
            circle.quadraticform,
        )
        assertArrayMatches(doubleArrayOf(-2.0, 1.0, 4.0, -5.0), circle.bounds())
    }

    @Test
    fun stringRadiusMatchesOfficialStableIdAndUpdateBehavior() {
        val board = board()
        val center = point(
            board,
            coordinates = doubleArrayOf(0.0, 0.0),
            name = "Center",
        )
        val driver = point(
            board,
            coordinates = doubleArrayOf(3.0, 0.0),
            name = "A",
        )

        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                radiusExpression = "A.X() + 1",
            ),
        )
        val updateRadius = assertNotNull(circle.updateRadius)

        assertEquals("A.X() + 1", updateRadius.origin)
        assertEquals(listOf(driver.id), updateRadius.dependencies.keys.toList())
        assertEquals(listOf(center.id), circle.parents)
        assertSame(circle, center.childElements[circle.id])
        assertSame(circle, driver.childElements[circle.id])
        assertSame(driver, circle.ancestors[driver.id])
        assertEquals(4.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertEquals(4.0, circle.radius, absoluteTolerance = TOLERANCE)
        assertNull(circle.radiusEvaluationError)

        driver.setName("Renamed")
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.0, 0.0),
        )
        board.update()

        assertEquals(6.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertEquals(6.0, circle.radius, absoluteTolerance = TOLERANCE)
        assertArrayMatches(doubleArrayOf(-6.0, 6.0, 6.0, -6.0), circle.bounds())
        assertNull(circle.radiusEvaluationError)
    }

    @Test
    fun stringRadiusCreationFailuresDoNotPolluteBoardOrDependencies() {
        val board = board()
        val center = point(
            board,
            coordinates = doubleArrayOf(0.0, 0.0),
            name = "Center",
        )
        val driver = point(
            board,
            coordinates = doubleArrayOf(3.0, 0.0),
            name = "A",
        )

        val parserError = assertIs<
            GMResult.Err<CircleError.RadiusExpressionCompile>
            >(
            Circle.create(
                board = board,
                center = center,
                radiusExpression = "1 +",
            ),
        ).error.error
        assertIs<JessieCodeExpressionCompileError.Parser>(parserError)

        val runtimeError = assertIs<
            GMResult.Err<CircleError.RadiusExpressionEvaluation>
            >(
            Circle.create(
                board = board,
                center = center,
                radiusExpression = "A.Unknown()",
            ),
        ).error.error
        assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
            runtimeError,
        )

        assertEquals(
            CircleError.NonNumericRadiusExpression("string"),
            assertIs<
                GMResult.Err<CircleError.NonNumericRadiusExpression>
                >(
                Circle.create(
                    board = board,
                    center = center,
                    radiusExpression = "\"radius\"",
                ),
            ).error,
        )

        assertEquals(2, board.numObjects)
        assertTrue(center.childElements.isEmpty())
        assertTrue(driver.childElements.isEmpty())
    }

    @Test
    fun missingStringRadiusDependencyReturnsErrorAndLegacyApiUsesNaN() {
        val board = board()
        val center = point(
            board,
            coordinates = doubleArrayOf(0.0, 0.0),
            name = "Center",
        )
        val driver = point(
            board,
            coordinates = doubleArrayOf(3.0, 0.0),
            name = "A",
        )
        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                radiusExpression = "A.X() + 1",
            ),
        )

        board.removeObject(driver)

        val explicitError = assertIs<
            GMResult.Err<CircleError.RadiusExpressionEvaluation>
            >(circle.radiusResult()).error
        assertIs<JessieCodeRuntimeError.UnknownProperty>(
            explicitError.error,
        )

        circle.needsUpdate = true
        circle.update()

        assertTrue(circle.radius.isNaN())
        assertTrue(circle.Radius().isNaN())
        assertIs<CircleError.RadiusExpressionEvaluation>(
            circle.radiusEvaluationError,
        )
    }

    @Test
    fun lineRadiusTracksTheDefiningLineLength() {
        val board = board()
        val linePoint1 = point(board, doubleArrayOf(0.0, 0.0))
        val linePoint2 = point(board, doubleArrayOf(0.0, 4.0))
        val radiusLine = line(Line.create(board, linePoint1, linePoint2))
        val center = point(board, doubleArrayOf(1.0, -2.0))

        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                radiusLine = radiusLine,
            ),
        )

        assertEquals("pointLine", circle.method)
        assertSame(radiusLine, circle.line)
        assertEquals(listOf(center.id, radiusLine.id), circle.parents)
        assertSame(circle, center.childElements[circle.id])
        assertSame(circle, radiusLine.childElements[circle.id])
        assertEquals(4.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(-1.375, -0.25, 0.5, 0.125, 1.0, 4.0, 1.0, -2.0),
            circle.stdform,
        )

        linePoint2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(0.0, 6.0),
        )
        circle.needsUpdate = true
        circle.update()

        assertEquals(6.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(doubleArrayOf(-5.0, 4.0, 7.0, -8.0), circle.bounds())
    }

    @Test
    fun circleRadiusTracksTheSourceCircle() {
        val board = board()
        val sourceCenter = point(board, doubleArrayOf(0.0, 0.0))
        val sourcePoint = point(board, doubleArrayOf(2.5, 0.0))
        val sourceCircle = circle(Circle.create(board, sourceCenter, sourcePoint))
        val center = point(board, doubleArrayOf(1.0, -2.0))

        val circle = circle(
            Circle.create(
                board = board,
                center = center,
                radiusCircle = sourceCircle,
            ),
        )

        assertEquals("pointCircle", circle.method)
        assertSame(sourceCircle, circle.circle)
        assertEquals(listOf(center.id, sourceCircle.id), circle.parents)
        assertSame(circle, sourceCircle.childElements[circle.id])
        assertEquals(2.5, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(
            doubleArrayOf(-0.25, -0.4, 0.8, 0.2, 1.0, 2.5, 1.0, -2.0),
            circle.stdform,
        )

        sourcePoint.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 0.0),
        )
        sourceCircle.needsUpdate = true
        sourceCircle.update()
        circle.needsUpdate = true
        circle.update()

        assertEquals(4.0, circle.Radius(), absoluteTolerance = TOLERANCE)
        assertArrayMatches(doubleArrayOf(-3.0, 2.0, 5.0, -6.0), circle.bounds())
    }

    @Test
    fun sourceElementFactoriesRejectUnregisteredAndForeignElements() {
        val board = board()
        val center = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val unregisteredLine = Line(
            board = board,
            point1 = center,
            point2 = point2,
            id = "unregistered-line",
        )
        val foreignBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "foreign",
        )
        val foreignCircle = circle(
            Circle.create(
                board = foreignBoard,
                center = point(foreignBoard, doubleArrayOf(0.0, 0.0)),
                radius = 2.0,
            ),
        )

        assertEquals(
            CircleError.ParentNotRegistered(
                parentIndex = 1,
                id = "unregistered-line",
            ),
            assertIs<GMResult.Err<CircleError.ParentNotRegistered>>(
                Circle.create(board, center, unregisteredLine),
            ).error,
        )
        assertEquals(
            CircleError.ParentBoardMismatch(parentIndex = 1),
            assertIs<GMResult.Err<CircleError.ParentBoardMismatch>>(
                Circle.create(board, center, foreignCircle),
            ).error,
        )
        assertEquals(2, board.numObjects)
        assertTrue(center.childElements.isEmpty())
        assertTrue(point2.childElements.isEmpty())
    }

    @Test
    fun factoryFailuresDoNotPolluteRegistriesOrDependencies() {
        val board = board()
        val center = point(board, doubleArrayOf(0.0, 0.0))
        val point2 = point(board, doubleArrayOf(2.0, 0.0))
        val unregistered = Point(
            board = board,
            coordinates = doubleArrayOf(1.0, 1.0),
            id = "unregistered",
        )
        val foreignBoard = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "foreign",
        )
        val foreign = point(foreignBoard, doubleArrayOf(2.0, 2.0))

        assertEquals(
            CircleError.ParentNotRegistered(
                parentIndex = 1,
                id = "unregistered",
            ),
            assertIs<GMResult.Err<CircleError.ParentNotRegistered>>(
                Circle.create(board, center, unregistered),
            ).error,
        )
        assertEquals(
            CircleError.ParentBoardMismatch(parentIndex = 0),
            assertIs<GMResult.Err<CircleError.ParentBoardMismatch>>(
                Circle.create(board, foreign, point2),
            ).error,
        )

        val first = circle(
            Circle.create(
                board = board,
                center = center,
                point2 = point2,
                id = "fixed",
                name = "Defined",
            ),
        )
        val duplicate = assertIs<GMResult.Err<CircleError.Registration>>(
            Circle.create(
                board = board,
                center = center,
                point2 = point2,
                id = "fixed",
                name = "Orphan",
            ),
        )

        assertEquals(
            BoardError.DuplicateElementId("fixed"),
            duplicate.error.error,
        )
        assertSame(first, board.elementById("fixed"))
        assertSame(first, board.elementByName("Defined"))
        assertNull(board.elementByName("Orphan"))
        assertEquals(setOf(first.id), center.childElements.keys)
        assertEquals(setOf(first.id), point2.childElements.keys)
        assertEquals(3, board.numObjects)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = "board",
    )

    private fun point(
        board: Board,
        coordinates: DoubleArray,
        name: String? = null,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = coordinates,
            name = name,
        ),
    ).value

    private fun circle(result: GMResult<Circle, CircleError>): Circle =
        assertIs<GMResult.Ok<Circle>>(result).value

    private fun line(result: GMResult<Line, LineError>): Line =
        assertIs<GMResult.Ok<Line>>(result).value

    private fun assertMatrixMatches(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertArrayMatches(expected[index], actual[index])
        }
    }

    private fun assertArrayMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            when {
                expected[index].isNaN() ->
                    assertTrue(actual[index].isNaN(), "Expected NaN at index $index")

                expected[index].isInfinite() ->
                    assertEquals(expected[index], actual[index], "Mismatch at index $index")

                else -> assertEquals(
                    expected = expected[index],
                    actual = actual[index],
                    absoluteTolerance = TOLERANCE,
                    message = "Mismatch at index $index",
                )
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
