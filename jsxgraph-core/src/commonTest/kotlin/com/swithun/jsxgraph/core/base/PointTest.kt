package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PointTest {
    @Test
    fun freePointFactoryRegistersOfficialIdentityAndCoordinates() {
        val board = board()

        val first = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, -3.0),
            ),
        )
        val second = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 8.0, -4.0),
            ),
        )

        assertEquals("boardP0", first.id)
        assertEquals("A", first.name)
        assertEquals("point", first.elType)
        assertEquals(Const.OBJECT_TYPE_POINT, first.type)
        assertEquals(Const.OBJECT_TYPE_POINT, first.originalType)
        assertEquals(Const.OBJECT_CLASS_POINT, first.elementClass)
        assertSame(first, first.baseElement)
        assertContentEquals(doubleArrayOf(1.0, 2.0, -3.0), first.coords.usrCoords)
        assertContentEquals(doubleArrayOf(1.0, 4.0, -2.0), second.coords.usrCoords)
        assertEquals("boardP1", second.id)
        assertEquals("B", second.name)
        assertSame(first, board.select(first.id))
        assertSame(second, board.select(second.name))
    }

    @Test
    fun explicitIdentityAndDuplicateFailureDoNotLeakRegistryEntries() {
        val board = board()
        val first = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                id = "fixed",
                name = "Named",
            ),
        )

        val duplicate = assertIs<GMResult.Err<PointError.Registration>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                id = "fixed",
                name = "Orphan",
            ),
        )

        assertEquals(
            BoardError.DuplicateElementId("fixed"),
            duplicate.error.error,
        )
        assertSame(first, board.elementById("fixed"))
        assertSame(first, board.elementByName("Named"))
        assertNull(board.elementByName("Orphan"))
        assertEquals(1, board.numObjects)
    }

    @Test
    fun tooFewCoordinatesReportAnExplicitFactoryError() {
        val board = board()

        assertEquals(
            PointError.InvalidCoordinateCount(0),
            assertIs<GMResult.Err<PointError.InvalidCoordinateCount>>(
                Point.create(board, doubleArrayOf()),
            ).error,
        )
        assertEquals(
            PointError.InvalidCoordinateCount(1),
            assertIs<GMResult.Err<PointError.InvalidCoordinateCount>>(
                Point.create(board, doubleArrayOf(1.0)),
            ).error,
        )
        assertTrue(board.objects.isEmpty())
        assertEquals(0, board.numObjects)
    }

    @Test
    fun stringCoordinatesMatchOfficialStableIdAndUpdateBehavior() {
        val board = board()
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                name = "A",
            ),
        )

        val constrained = point(
            Point.create(
                board = board,
                coordinateExpressions = listOf(
                    "A.X() + 1",
                    "A.Y() * 2",
                ),
                name = "Dynamic",
            ),
        )

        assertEquals(Const.OBJECT_TYPE_CAS, constrained.type)
        assertEquals(Const.OBJECT_TYPE_POINT, constrained.originalType)
        assertTrue(constrained.isConstrained)
        assertFalse(constrained.isDraggable)
        assertNull(constrained.baseElement)
        assertEquals("A.X() + 1", constrained.Xjc)
        assertEquals("A.Y() * 2", constrained.Yjc)
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 8.0),
            constrained.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            constrained.initialCoords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            constrained.actualCoords.usrCoords,
        )
        assertTrue(constrained.parents.isEmpty())
        assertEquals(
            listOf(driver.id),
            constrained.coordinateFunctions[0]
                .dependencies.keys.toList(),
        )
        assertEquals(
            listOf(driver.id),
            constrained.coordinateFunctions[1]
                .dependencies.keys.toList(),
        )
        assertSame(
            constrained,
            driver.childElements[constrained.id],
        )
        assertSame(driver, constrained.ancestors[driver.id])
        assertNull(constrained.coordinateEvaluationError)

        driver.setName("Renamed")
        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(5.0, 6.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(1.0, 6.0, 12.0),
            constrained.coords.usrCoords,
        )
        assertNull(constrained.coordinateEvaluationError)
    }

    @Test
    fun homogeneousStringCoordinatesUseTheFirstThreeTerms() {
        val board = board()
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                name = "A",
            ),
        )

        val constrained = point(
            Point.create(
                board = board,
                coordinateExpressions = listOf(
                    "2",
                    "A.X() * 2",
                    "A.Y() * 3",
                    "\"ignored\"",
                ),
            ),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 6.0),
            constrained.coords.usrCoords,
        )
        assertEquals(4, constrained.coordinateFunctions.size)
        assertNull(constrained.Xjc)
        assertNull(constrained.Yjc)
        assertSame(
            constrained,
            driver.childElements[constrained.id],
        )
    }

    @Test
    fun stringCoordinateFailuresDoNotPolluteBoardOrDependencies() {
        val board = board()
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                name = "A",
            ),
        )

        assertEquals(
            PointError.InvalidCoordinateCount(1),
            assertIs<GMResult.Err<PointError.InvalidCoordinateCount>>(
                Point.create(
                    board = board,
                    coordinateExpressions = listOf("1"),
                ),
            ).error,
        )

        val compileError = assertIs<
            GMResult.Err<PointError.CoordinateExpressionCompile>
            >(
            Point.create(
                board = board,
                coordinateExpressions = listOf("0", "1 +"),
            ),
        ).error
        assertEquals(1, compileError.coordinateIndex)
        assertIs<JessieCodeExpressionCompileError.Parser>(
            compileError.error,
        )

        val evaluationError = assertIs<
            GMResult.Err<PointError.CoordinateExpressionEvaluation>
            >(
            Point.create(
                board = board,
                coordinateExpressions = listOf(
                    "A.Unknown()",
                    "0",
                ),
            ),
        ).error.error
        val runtimeError = assertIs<
            CoordinateConstraintError.Evaluation
            >(evaluationError)
        assertEquals(0, runtimeError.coordinateIndex)
        assertIs<JessieCodeRuntimeError.ElementPropertyUnavailable>(
            runtimeError.error,
        )

        assertEquals(
            CoordinateConstraintError.NonNumericResult(
                coordinateIndex = 1,
                actualType = "string",
            ),
            assertIs<
                GMResult.Err<PointError.CoordinateExpressionEvaluation>
                >(
                Point.create(
                    board = board,
                    coordinateExpressions = listOf(
                        "0",
                        "\"y\"",
                    ),
                ),
            ).error.error,
        )

        assertIs<GMResult.Err<PointError.Registration>>(
            Point.create(
                board = board,
                coordinateExpressions = listOf(
                    "A.X()",
                    "A.Y()",
                ),
                id = driver.id,
            ),
        )

        assertEquals(1, board.numObjects)
        assertSame(driver, board.elementById(driver.id))
        assertTrue(driver.childElements.isEmpty())
    }

    @Test
    fun missingCoordinateDependencyReturnsErrorAndUpdateUsesNaN() {
        val board = board()
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 4.0),
                name = "A",
            ),
        )
        val constrained = point(
            Point.create(
                board = board,
                coordinateExpressions = listOf(
                    "A.X() + 1",
                    "A.Y() * 2",
                ),
            ),
        )

        board.removeObject(driver)

        val explicitError = assertIs<
            GMResult.Err<CoordinateConstraintError.Evaluation>
            >(constrained.coordinateConstraintResult()).error
        assertEquals(0, explicitError.coordinateIndex)
        assertIs<JessieCodeRuntimeError.UnknownProperty>(
            explicitError.error,
        )

        constrained.needsUpdate = true
        constrained.update(fromParent = true)

        assertEquals(1.0, constrained.coords.usrCoords[0])
        assertTrue(constrained.coords.usrCoords[1].isNaN())
        assertTrue(constrained.coords.usrCoords[2].isNaN())
        assertIs<CoordinateConstraintError.Evaluation>(
            constrained.coordinateEvaluationError,
        )
    }

    @Test
    fun pointUpdateUsesTheFreeCoordinateLifecycle() {
        val calls = mutableListOf<String>()
        val point = RecordingPoint(
            board = board(),
            coordinates = doubleArrayOf(2.0, -3.0),
            calls = calls,
        )

        point.needsUpdate = false
        assertSame(point, point.update(fromParent = false))
        assertTrue(calls.isEmpty())

        point.needsUpdate = true
        assertSame(point, point.update(fromParent = false))
        assertEquals(
            listOf("constraint", "transform:false"),
            calls,
        )
    }

    @Test
    fun transformedPointFactoryMatchesOfficialDynamicChainLifecycle() {
        val board = board()
        val driver = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(2.0, 3.0),
                id = "driver",
                name = "driver",
            ),
        )
        val center = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, -1.0),
                id = "center",
            ),
        )
        val base = point(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(3.0, 2.0),
                id = "base",
            ),
        )
        val translate = transformation(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Expression("X(driver)"),
                    TransformationParameter.Expression("Y(driver)"),
                ),
            ),
        )
        val rotate = transformation(
            Transformation.createRotation(
                board = board,
                angle = TransformationParameter.Expression("Y(driver)"),
                center = center,
            ),
        )
        val transformed = point(
            Point.create(
                board = board,
                basePoint = base,
                transformations = listOf(translate, rotate),
                id = "transformed",
            ),
        )

        board.update()

        assertSame(base, transformed.baseElement)
        assertEquals(listOf(base.id), transformed.parents)
        assertEquals(listOf(translate, rotate), transformed.transformations)
        assertFalse(transformed.isDraggable)
        assertContentEquals(
            doubleArrayOf(
                1.0,
                -3.806690034760985,
                -6.3754749473632035,
            ),
            transformed.coords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            transformed.initialCoords.usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            transformed.actualCoords.usrCoords,
        )
        assertNull(transformed.transformationEvaluationError)

        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-1.0, 0.5),
        )
        center.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 2.0),
        )
        base.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, 4.0),
        )
        board.update()

        assertEquals(
            -3.5864766559623713,
            transformed.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            1.7968287117049169,
            transformed.Y(),
            absoluteTolerance = 1.0e-12,
        )
        assertNull(transformed.transformationEvaluationError)
    }

    @Test
    fun transformedPointFactoryRejectsAnEmptyTransformationList() {
        val board = board()
        val base = point(Point.create(board, doubleArrayOf(1.0, 2.0)))

        assertEquals(
            PointError.InvalidTransformationCount(0),
            assertIs<GMResult.Err<PointError.InvalidTransformationCount>>(
                Point.create(
                    board = board,
                    basePoint = base,
                    transformations = emptyList(),
                ),
            ).error,
        )
    }

    @Test
    fun pointBoundsRepeatTheAffineCoordinates() {
        val point = Point(
            board = board(),
            coordinates = doubleArrayOf(2.0, -3.0),
        )

        assertContentEquals(
            doubleArrayOf(2.0, -3.0, 2.0, -3.0),
            point.bounds(),
        )
    }

    @Test
    fun pointIncidenceMatchesOfficialToleranceAndIdealPointBehavior() {
        val board = board()
        val origin = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val same = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val halfEpsilon = point(
            Point.create(board, doubleArrayOf(Mat.eps * 0.5, 0.0)),
        )
        val atEpsilon = point(
            Point.create(board, doubleArrayOf(Mat.eps, 0.0)),
        )
        val ideal = point(
            Point.create(board, doubleArrayOf(0.0, 1.0, 0.0)),
        )

        assertTrue(origin.isOn(same))
        assertTrue(origin.isOn(halfEpsilon))
        assertFalse(origin.isOn(atEpsilon))
        assertTrue(origin.isOn(halfEpsilon, tolerance = 0.0))
        assertTrue(origin.isOn(halfEpsilon, tolerance = Double.NaN))
        assertFalse(origin.isOn(same, tolerance = -1.0))
        assertFalse(origin.isOn(ideal))
        assertFalse(origin.isOn(GeometryElement(board)))
    }

    @Test
    fun lineIncidenceMatchesOfficialStrictDistanceComparison() {
        val board = board()
        val line = line(
            Line.create(
                board,
                point(Point.create(board, doubleArrayOf(-2.0, 0.0))),
                point(Point.create(board, doubleArrayOf(3.0, 0.0))),
            ),
        )
        val onLine = point(Point.create(board, doubleArrayOf(1.0, 0.0)))
        val halfEpsilon = point(
            Point.create(board, doubleArrayOf(1.0, Mat.eps * 0.5)),
        )
        val atEpsilon = point(
            Point.create(board, doubleArrayOf(1.0, Mat.eps)),
        )

        assertTrue(onLine.isOn(line))
        assertTrue(halfEpsilon.isOn(line))
        assertFalse(atEpsilon.isOn(line))
        assertTrue(halfEpsilon.isOn(line, tolerance = 0.0))
        assertTrue(halfEpsilon.isOn(line, tolerance = Double.NaN))
        assertFalse(onLine.isOn(line, tolerance = -1.0))
    }

    @Test
    fun circleIncidenceMatchesOfficialBoundaryBehavior() {
        val board = board()
        val center = point(Point.create(board, doubleArrayOf(0.0, 0.0)))
        val circle = circle(Circle.create(board, center, radius = 2.0))
        val boundary = point(Point.create(board, doubleArrayOf(2.0, 0.0)))
        val outerHalfEpsilon = point(
            Point.create(board, doubleArrayOf(2.0 + Mat.eps * 0.5, 0.0)),
        )
        val outerAtEpsilon = point(
            Point.create(board, doubleArrayOf(2.0 + Mat.eps, 0.0)),
        )

        assertTrue(boundary.isOn(circle))
        assertTrue(outerHalfEpsilon.isOn(circle))
        assertFalse(outerAtEpsilon.isOn(circle))
        assertFalse(center.isOn(circle))
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 200.0,
        unitX = 50.0,
        unitY = 40.0,
        id = "board",
    )

    private fun point(
        result: GMResult<Point, PointError>,
    ): Point = assertIs<GMResult.Ok<Point>>(result).value

    private fun line(
        result: GMResult<Line, LineError>,
    ): Line = assertIs<GMResult.Ok<Line>>(result).value

    private fun circle(
        result: GMResult<Circle, CircleError>,
    ): Circle = assertIs<GMResult.Ok<Circle>>(result).value

    private fun transformation(
        result: GMResult<Transformation, TransformationError>,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(result).value

    private class RecordingPoint(
        board: Board,
        coordinates: DoubleArray,
        private val calls: MutableList<String>,
    ) : Point(
        board = board,
        coordinates = coordinates,
    ) {
        override fun updateConstraint(): CoordsElement {
            calls += "constraint"
            return this
        }

        override fun updateTransform(fromParent: Boolean): CoordsElement {
            calls += "transform:$fromParent"
            return this
        }
    }
}
