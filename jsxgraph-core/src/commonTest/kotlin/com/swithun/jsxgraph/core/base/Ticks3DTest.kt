package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Ticks3DTest {
    @Test
    fun projectedTicksAndCreationTimeLabelsMatchOfficialFixture() {
        val board = createBoard()
        val view = createView(board)
        var point = doubleArrayOf(1.0, -2.0, 1.0, -1.0)
        var directionX = 2.0
        val ticks = ticks3D(
            Ticks3D.create(
                view = view,
                pointSource = Ticks3DPointSource.Function(
                    Line3DArrayEvaluator {
                        GMResult.Ok(point.copyOf())
                    },
                ),
                direction1 = listOf(
                    dynamic { directionX },
                    numeric(0.0),
                    numeric(0.0),
                ),
                length = 4.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                ticksDistance = 2.0,
                majorHeight = 12.0,
                tickEndings = doubleArrayOf(0.25, 0.75),
                drawLabels = true,
                id = "ticks",
                name = "",
            ),
        )

        assertEquals("curve", ticks.elType)
        assertEquals("ticks", ticks.id)
        assertTrue(ticks.isTicks3D)
        assertVectorMatches(
            expected = doubleArrayOf(
                -0.18575328186789075,
                0.04977395073532809,
                Double.NaN,
                -1.058881808150805,
                -0.823354575547586,
                Double.NaN,
                -1.932010334433719,
                -1.6964831018305,
                Double.NaN,
            ),
            actual = ticks.dataX ?: DoubleArray(0),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                -2.1648706494826797,
                -2.203975807003401,
                Double.NaN,
                -2.5164924040552297,
                -2.5555975615759507,
                Double.NaN,
                -2.86811415862778,
                -2.9072193161485007,
                Double.NaN,
            ),
            actual = ticks.dataY ?: DoubleArray(0),
        )
        val labels = ticks.ticks3DDefinition?.labels.orEmpty()
        assertEquals(listOf(-2.0, 0.0, 2.0), labels.map { it.value })
        val labelElements =
            ticks.ticks3DDefinition?.labelElements.orEmpty()
        assertEquals(3, labelElements.size)
        assertEquals(listOf("-2", "0", "2"), labelElements.map { it.content })
        assertEquals(
            listOf("-2", "0", "2"),
            labelElements.map { it.text2D.plaintext },
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, 1.519615242270663, -1.0),
            labels.first().coordinates3D,
        )
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                0.22641937518774236,
                -2.233304675143941,
            ),
            labels.first().coordinates2D,
        )

        point = doubleArrayOf(1.0, -1.0, 2.0, 0.5)
        directionX = 4.0
        board.fullUpdate()

        assertVectorMatches(
            expected = doubleArrayOf(
                0.05759101071543271,
                0.29311824331865166,
                Double.NaN,
                -0.8155375155674816,
                -0.5800102829642626,
                Double.NaN,
                -1.6886660418503951,
                -1.4531388092471762,
                Double.NaN,
            ),
            actual = ticks.dataX ?: DoubleArray(0),
        )
        assertVectorMatches(
            expected = doubleArrayOf(
                -1.4404340461577039,
                -1.4795392036784245,
                Double.NaN,
                -1.7920558007302538,
                -1.8311609582509745,
                Double.NaN,
                -2.143677555302804,
                -2.182782712823524,
                Double.NaN,
            ),
            actual = ticks.dataY ?: DoubleArray(0),
        )
        assertVectorMatches(
            doubleArrayOf(1.0, -2.0, 1.519615242270663, -1.0),
            ticks.ticks3DDefinition?.labels?.first()?.coordinates3D
                ?: DoubleArray(0),
        )
    }

    @Test
    fun dynamicDependenciesAndRemovalFollowCurveLifecycle() {
        val board = createBoard()
        val view = createView(board)
        val dependency = point3D(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(0.0, 0.0, 0.0),
                id = "dependency",
                name = "",
            ),
        )
        val ticks = ticks3D(
            Ticks3D.create(
                view = view,
                pointSource = Ticks3DPointSource.Values(
                    listOf(
                        dynamic { dependency.X() },
                        numeric(0.0),
                        numeric(0.0),
                    ),
                ),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 2.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                drawLabels = false,
                dependencies = listOf(dependency),
                id = "ticks",
                name = "",
            ),
        )

        assertEquals(listOf("dependency"), ticks.parents)
        assertSame(ticks, dependency.childElements["ticks"])

        board.removeObject(dependency)

        assertNull(board.elementById("dependency"))
        assertNull(board.elementById("ticks"))
    }

    @Test
    fun zeroDistanceMatchesUpstreamEmptyGeometry() {
        val ticks = ticks3D(
            Ticks3D.create(
                view = createView(createBoard()),
                pointSource = numericPoint(0.0, 0.0, 0.0),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 4.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                ticksDistance = 0.0,
            ),
        )

        assertTrue(ticks.dataX?.isEmpty() == true)
        assertTrue(ticks.dataY?.isEmpty() == true)
        assertTrue(ticks.ticks3DDefinition?.labels?.isEmpty() == true)
    }

    @Test
    fun malformedInputsAndExcessiveCountsReturnStructuredErrors() {
        val view = createView(createBoard())

        assertIs<GMResult.Err<Ticks3DError.InvalidPointCount>>(
            Ticks3D.create(
                view = view,
                pointSource = Ticks3DPointSource.Values(
                    listOf(numeric(0.0), numeric(0.0)),
                ),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 1.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
            ),
        )
        assertIs<GMResult.Err<Ticks3DError.InvalidTickEndingsCount>>(
            Ticks3D.create(
                view = view,
                pointSource = numericPoint(0.0, 0.0, 0.0),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 1.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                tickEndings = doubleArrayOf(1.0),
            ),
        )
        assertIs<GMResult.Err<Ticks3DError.InvalidTicksDistance>>(
            Ticks3D.create(
                view = view,
                pointSource = numericPoint(0.0, 0.0, 0.0),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 1.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                ticksDistance = -1.0,
            ),
        )
        assertIs<GMResult.Err<Ticks3DError.TickCountLimitExceeded>>(
            Ticks3D.create(
                view = view,
                pointSource = numericPoint(0.0, 0.0, 0.0),
                direction1 = numericVector(1.0, 0.0, 0.0),
                length = 10.0,
                direction2 = numericVector(0.0, 1.0, 0.0),
                ticksDistance = 1.0,
                maximumTickCount = 4,
            ),
        )
    }

    private fun createBoard(): Board =
        Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 30.0,
        )

    private fun createView(board: Board): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(8.0, 7.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 6.0),
                    doubleArrayOf(-3.0, 7.0),
                ),
                projection = "parallel",
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
            ),
        ).value

    private fun numericPoint(
        x: Double,
        y: Double,
        z: Double,
    ): Ticks3DPointSource =
        Ticks3DPointSource.Values(numericVector(x, y, z))

    private fun numericVector(
        x: Double,
        y: Double,
        z: Double,
    ): List<Line3DCoordinateValue> =
        listOf(numeric(x), numeric(y), numeric(z))

    private fun numeric(value: Double): Line3DCoordinateValue =
        Line3DCoordinateValue.Numeric(value)

    private fun dynamic(value: () -> Double): Line3DCoordinateValue =
        Line3DCoordinateValue.Dynamic(
            Line3DScalarEvaluator { GMResult.Ok(value()) },
        )

    private fun point3D(
        result: GMResult<Point3D, Point3DError>,
    ): Point3D = assertIs<GMResult.Ok<Point3D>>(result).value

    private fun ticks3D(
        result: GMResult<Curve, Ticks3DError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun assertVectorMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            if (expected[index].isNaN()) {
                assertTrue(actual[index].isNaN())
            } else {
                assertEquals(
                    expected = expected[index],
                    actual = actual[index],
                    absoluteTolerance = TOLERANCE,
                )
            }
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
