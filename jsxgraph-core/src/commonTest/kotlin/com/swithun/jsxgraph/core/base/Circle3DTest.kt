package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Circle3DTest {
    @Test
    fun negativeRadiusBuildsOrthonormalFrameAndCurveProxy() {
        val board = createBoard()
        val view = createView(board)
        val center = point(view, doubleArrayOf(1.0, 2.0, 3.0), "center")
        val circle = circle(
            Circle3D.create(
                view = view,
                center = center,
                normalSource = normal(0.0, 0.0, 1.0),
                radiusSource = Line3DCoordinateValue.Numeric(-2.0),
                sampleCount = 5,
                id = "circle",
                name = "",
            ),
        )

        assertEquals(Const.OBJECT_TYPE_CIRCLE3D, circle.type)
        assertEquals("circle3d", circle.elType)
        assertEquals(2.0, circle.Radius())
        assertArrayClose(
            doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            circle.normal,
        )
        assertEquals(0.0, MatForTest.dot(circle.normal, circle.frame1))
        assertEquals(0.0, MatForTest.dot(circle.normal, circle.frame2))
        assertEquals(1.0, MatForTest.norm(circle.frame1))
        assertEquals(1.0, MatForTest.norm(circle.frame2))
        assertEquals(5, circle.curve.numberPoints)
        assertArrayClose(
            doubleArrayOf(1.0, 1.0, 4.0, 3.0),
            circle.curve.points.first(),
        )
        assertSame(circle.curve.curve2D, circle.element2D)
        assertEquals(listOf(circle.id), circle.curve.parents)
        assertTrue(circle.curve.id in circle.childElements)
        assertTrue(circle.id in center.childElements)
    }

    @Test
    fun dynamicNormalRadiusAndCenterUpdateExistingProxy() {
        val board = createBoard()
        val view = createView(board)
        var centerX = 0.0
        var radius = 1.0
        var normal = doubleArrayOf(0.0, 0.0, 0.0, 1.0)
        val center = assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Function(
                    Point3DArrayEvaluator {
                        GMResult.Ok(doubleArrayOf(centerX, 0.0, 0.0))
                    },
                ),
                id = "center",
                name = "",
            ),
        ).value
        val circle = circle(
            Circle3D.create(
                view = view,
                center = center,
                normalSource = Circle3DNormalSource.Function(
                    Line3DArrayEvaluator {
                        GMResult.Ok(normal.copyOf())
                    },
                ),
                radiusSource = Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator { GMResult.Ok(radius) },
                ),
                sampleCount = 9,
                id = "circle",
                name = "",
            ),
        )
        val proxy = circle.curve.curve2D

        centerX = 2.0
        radius = -3.0
        normal = doubleArrayOf(0.0, 0.0, 1.0, 0.0)
        board.fullUpdate()

        assertSame(proxy, circle.curve.curve2D)
        assertEquals(3.0, circle.Radius())
        assertArrayClose(
            doubleArrayOf(0.0, 0.0, 1.0, 0.0),
            circle.normal,
        )
        for (point in circle.curve.points) {
            assertEquals(0.0, point[2], absoluteTolerance = 1.0e-12)
            val dx = point[1] - center.X()
            val dz = point[3] - center.Z()
            assertEquals(
                3.0,
                kotlin.math.sqrt(dx * dx + dz * dz),
                absoluteTolerance = 1.0e-12,
            )
        }
    }

    @Test
    fun projectionDelegatesToOwnedParametricCurve() {
        val view = createView(createBoard())
        val circle = circle(
            Circle3D.create(
                view = view,
                center = point(
                    view,
                    doubleArrayOf(0.0, 0.0, 0.0),
                    "center",
                ),
                normalSource = normal(0.0, 0.0, 1.0),
                radiusSource = Line3DCoordinateValue.Numeric(2.0),
                sampleCount = 9,
                name = "",
            ),
        )
        val expectedParameter = kotlin.math.PI
        val expectedPoint = circle.curve.F(expectedParameter)
        val parameters = mutableListOf(expectedParameter - 0.2)

        val result = circle.projectCoords(
            coordinates = doubleArrayOf(
                1.0,
                expectedPoint[0],
                expectedPoint[1],
                expectedPoint[2],
            ),
            parameters = parameters,
        )

        assertArrayClose(
            doubleArrayOf(
                1.0,
                expectedPoint[0],
                expectedPoint[1],
                expectedPoint[2],
            ),
            assertIs<GMResult.Ok<DoubleArray>>(result).value,
            absoluteTolerance = 2.0e-7,
        )
        assertEquals(
            expectedParameter,
            parameters.single(),
            absoluteTolerance = 1.0e-6,
        )
    }

    @Test
    fun invalidDynamicValuesReturnStructuredErrors() {
        val board = createBoard()
        val view = createView(board)
        val center = point(view, doubleArrayOf(0.0, 0.0, 0.0), "center")

        assertIs<GMResult.Err<Circle3DError.InvalidNormalCount>>(
            Circle3D.create(
                view = view,
                center = center,
                normalSource = Circle3DNormalSource.Function(
                    Line3DArrayEvaluator {
                        GMResult.Ok(doubleArrayOf(0.0, 1.0, 0.0))
                    },
                ),
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
            ),
        )
        assertIs<GMResult.Err<Circle3DError.RadiusEvaluation>>(
            Circle3D.create(
                view = view,
                center = center,
                normalSource = normal(0.0, 0.0, 1.0),
                radiusSource = Line3DCoordinateValue.Dynamic(
                    Line3DScalarEvaluator {
                        GMResult.Err(
                            Line3DDynamicError.Rejected("radius"),
                        )
                    },
                ),
            ),
        )
    }

    @Test
    fun removalDeletesOwnedCenterAndProxyButPreservesExternalCenter() {
        val board = createBoard()
        val view = createView(board)
        val ownedCenter = point(
            view,
            doubleArrayOf(-1.0, 0.0, 0.0),
            "ownedCenter",
        )
        val externalCenter = point(
            view,
            doubleArrayOf(1.0, 0.0, 0.0),
            "externalCenter",
        )
        val owned = circle(
            Circle3D.create(
                view = view,
                center = ownedCenter,
                normalSource = normal(0.0, 0.0, 1.0),
                radiusSource = Line3DCoordinateValue.Numeric(1.0),
                ownsCenter = true,
                id = "owned",
                name = "",
            ),
        )
        val external = circle(
            Circle3D.create(
                view = view,
                center = externalCenter,
                normalSource = normal(0.0, 1.0, 0.0),
                radiusSource = Line3DCoordinateValue.Numeric(2.0),
                id = "external",
                name = "",
            ),
        )
        val ownedCurveId = owned.curve.id
        val ownedProxyId = owned.curve.curve2D.id
        val externalCurveId = external.curve.id
        val externalProxyId = external.curve.curve2D.id

        board.removeObject(owned)
        board.removeObject(external)

        assertNull(board.elementById(owned.id))
        assertNull(board.elementById(ownedCenter.id))
        assertNull(board.elementById(ownedCurveId))
        assertNull(board.elementById(ownedProxyId))
        assertNull(board.elementById(external.id))
        assertSame(externalCenter, board.elementById(externalCenter.id))
        assertNull(board.elementById(externalCurveId))
        assertNull(board.elementById(externalProxyId))
    }

    private fun createBoard(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )

    private fun createView(board: Board): View3D =
        assertIs<GMResult.Ok<View3D>>(
            View3D.create(
                board = board,
                lowerLeftCorner = doubleArrayOf(-5.0, -4.0),
                size = doubleArrayOf(10.0, 8.0),
                boundingBox = arrayOf(
                    doubleArrayOf(-5.0, 5.0),
                    doubleArrayOf(-4.0, 4.0),
                    doubleArrayOf(-3.0, 5.0),
                ),
                projection = "parallel",
                azimuth = 1.0,
                elevation = 0.3,
                bank = 0.0,
                id = "view",
                name = "",
            ),
        ).value

    private fun point(
        view: View3D,
        coordinates: DoubleArray,
        id: String,
    ): Point3D =
        assertIs<GMResult.Ok<Point3D>>(
            Point3D.create(
                view = view,
                coordinates = coordinates,
                id = id,
                name = "",
            ),
        ).value

    private fun normal(
        x: Double,
        y: Double,
        z: Double,
    ): Circle3DNormalSource =
        Circle3DNormalSource.Values(
            listOf(x, y, z).map(Line3DCoordinateValue::Numeric),
        )

    private fun circle(
        result: GMResult<Circle3D, Circle3DError>,
    ): Circle3D = assertIs<GMResult.Ok<Circle3D>>(result).value

    private fun assertArrayClose(
        expected: DoubleArray,
        actual: DoubleArray,
        absoluteTolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = absoluteTolerance,
            )
        }
    }

    private object MatForTest {
        fun dot(first: DoubleArray, second: DoubleArray): Double =
            first.indices.sumOf { first[it] * second[it] }

        fun norm(vector: DoubleArray): Double =
            kotlin.math.sqrt(dot(vector, vector))
    }
}
