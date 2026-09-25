package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Surface3DTest {
    @Test
    fun wireframeSamplesBothParametersAndUpdatesProxy() {
        val board = createBoard()
        val view = createView(board)
        var endU = 2.0
        val surface = surface(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = dynamicRange(0.0) { endU },
                rangeVSource = range(-1.0, 1.0),
                attributes = Surface3DAttributes(
                    stepsU = 2,
                    stepsV = 1,
                ),
                id = "surface",
                name = "",
            ),
        )

        assertEquals("parametricsurface3d", surface.elType)
        assertEquals(Const.OBJECT_TYPE_SURFACE3D, surface.type)
        assertEquals(3, surface.points.size)
        assertEquals(2, surface.points[0].size)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, -1.0, -1.0),
            surface.points.first().first(),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 1.0, 3.0),
            surface.points.last().last(),
        )
        assertEquals(17, surface.curve2D.numberPoints)
        assertNull(surface.polyhedron)
        val proxy = surface.curve2D

        endU = 4.0
        val parameters = mutableListOf(1.0, 0.0)
        val projection = surface.projectCoords(
            coordinates = doubleArrayOf(1.0, 3.0, 0.0, 3.0),
            parameters = parameters,
        )

        assertArrayClose(
            doubleArrayOf(1.0, 3.0, 0.0, 3.0),
            assertIs<GMResult.Ok<DoubleArray>>(projection).value,
            absoluteTolerance = 1.0e-5,
        )
        assertArrayClose(
            doubleArrayOf(3.0, 0.0),
            parameters.toDoubleArray(),
            absoluteTolerance = 1.0e-6,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 2.0),
            surface.evaluatedRangeU,
        )

        surface.prepareUpdate().update()

        assertSame(proxy, surface.curve2D)
        assertContentEquals(doubleArrayOf(0.0, 4.0), surface.evaluatedRangeU)
        assertContentEquals(
            doubleArrayOf(1.0, 4.0, 1.0, 5.0),
            surface.points.last().last(),
        )
    }

    @Test
    fun zeroWireframeStepDrawsOnlyTheOtherDirection() {
        val surface = surface(
            Surface3D.create(
                view = createView(createBoard()),
                source = components(),
                rangeUSource = range(0.0, 2.0),
                rangeVSource = range(0.0, 3.0),
                attributes = Surface3DAttributes(
                    stepsU = 0,
                    stepsV = 3,
                ),
                name = "",
            ),
        )

        assertEquals(2, surface.points.size)
        assertEquals(4, surface.points[0].size)
        assertEquals(14, surface.curve2D.numberPoints)
        assertTrue(
            surface.curve2D.points.take(2).all {
                it.usrCoords[1].isNaN() && it.usrCoords[2].isNaN()
            },
        )
    }

    @Test
    fun rectangleAndTriangleTilingsCreateUpstreamFaceTopology() {
        val board = createBoard()
        val view = createView(board)
        val rectangle = surface(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                attributes = Surface3DAttributes(
                    surfaceType = "colorarray",
                    tiling = "rectangle",
                    stepsU = 2,
                    stepsV = 2,
                ),
                id = "rectangle",
                name = "",
            ),
        )
        val triangle = surface(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                attributes = Surface3DAttributes(
                    surfaceType = "shader",
                    tiling = "triangle",
                    stepsU = 2,
                    stepsV = 2,
                ),
                id = "triangle",
                name = "",
            ),
        )

        assertEquals(9, rectangle.polyhedron?.definition?.vertices?.size)
        assertEquals(4, rectangle.polyhedron?.numberFaces)
        assertEquals(10, triangle.polyhedron?.definition?.vertices?.size)
        assertEquals(11, triangle.polyhedron?.numberFaces)
        assertEquals(0, rectangle.curve2D.numberPoints)
        assertTrue(
            triangle.polyhedron?.faces?.all {
                it.faceAttributes.shader.enabled
            } == true,
        )
    }

    @Test
    fun colormapAndFunctionGraphUseDynamicSurfaceCoordinates() {
        val board = createBoard()
        val view = createView(board)
        var height = 1.0
        val surface = surface(
            Surface3D.create(
                view = view,
                source = Surface3DSource.Components(
                    x = scalar { u, _ -> u },
                    y = scalar { _, v -> v },
                    z = scalar { u, v -> height * (u + v) },
                ),
                rangeUSource = range(0.0, 1.0),
                rangeVSource = range(0.0, 1.0),
                attributes = Surface3DAttributes(
                    surfaceType = "colormap",
                    stepsU = 1,
                    stepsV = 1,
                ),
                id = "graph",
                name = "",
                functionGraph = true,
            ),
        )
        val face = surface.polyhedron?.faces?.single()
            ?: error("Expected one face")
        val firstColor = face.resolvedFillColor()
        assertEquals("functiongraph3d", surface.elType)

        height = 2.0
        board.fullUpdate()

        assertEquals(4.0, face.polyhedron.coords.getValue("3")[3])
        assertTrue(firstColor != face.resolvedFillColor())
    }

    @Test
    fun transformedSurfaceAppliesMatrixAndKeepsExternalBase() {
        val board = createBoard()
        val view = createView(board)
        val base = surface(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = range(0.0, 1.0),
                rangeVSource = range(0.0, 1.0),
                attributes = Surface3DAttributes(
                    stepsU = 1,
                    stepsV = 1,
                ),
                id = "base",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                type = "translate",
                parameters = doubleArrayOf(2.0, -1.0, 3.0),
            ),
        ).value
        val transformed = surface(
            Surface3D.create(
                view = view,
                baseSurface = base,
                transformations = listOf(translation),
                id = "moved",
                name = "",
            ),
        )

        assertContentEquals(
            doubleArrayOf(1.0, 2.0, -1.0, 3.0),
            transformed.points.first().first(),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 0.0, 5.0),
            transformed.points.last().last(),
        )
        assertEquals(listOf(base.id), transformed.parents)

        board.removeObject(transformed)

        assertSame(base, board.elementById(base.id))
        assertNull(board.elementById(transformed.id))
    }

    @Test
    fun parametricProjectionMatchesOfficialCobylaResult() {
        val surface = surface(
            Surface3D.create(
                view = createView(createBoard()),
                source = components(),
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-2.0, 2.0),
                attributes = Surface3DAttributes(
                    stepsU = 1,
                    stepsV = 1,
                ),
                name = "",
            ),
        )
        val parameters = mutableListOf(0.1, 0.1)

        val result = surface.projectCoords(
            coordinates = doubleArrayOf(1.0, 0.8, -0.4, 0.2),
            parameters = parameters,
        )

        assertArrayClose(
            doubleArrayOf(
                1.0,
                0.7333332582827766,
                -0.4666667008424886,
                0.266666557440288,
            ),
            assertIs<GMResult.Ok<DoubleArray>>(result).value,
        )
        assertArrayClose(
            doubleArrayOf(
                0.7333332582827766,
                -0.4666667008424886,
            ),
            parameters.toDoubleArray(),
        )
    }

    @Test
    fun malformedInputsReturnStructuredErrors() {
        val view = createView(createBoard())
        assertIs<GMResult.Err<Surface3DError.InvalidRangeCount>>(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = range(0.0),
                rangeVSource = range(0.0, 1.0),
            ),
        )
        assertIs<GMResult.Err<Surface3DError.InvalidSurfaceType>>(
            Surface3D.create(
                view = view,
                source = components(),
                rangeUSource = range(0.0, 1.0),
                rangeVSource = range(0.0, 1.0),
                attributes = Surface3DAttributes(
                    surfaceType = "unknown",
                ),
            ),
        )
        assertIs<GMResult.Err<Surface3DError.CoordinateEvaluation>>(
            Surface3D.create(
                view = view,
                source = Surface3DSource.Function(
                    Surface3DArrayEvaluator { _, _ ->
                        GMResult.Err(
                            Surface3DDynamicError.Rejected("failure"),
                        )
                    },
                ),
                rangeUSource = range(0.0, 1.0),
                rangeVSource = range(0.0, 1.0),
                attributes = Surface3DAttributes(
                    stepsU = 1,
                    stepsV = 1,
                ),
            ),
        )
    }

    private fun components(): Surface3DSource.Components =
        Surface3DSource.Components(
            x = scalar { u, _ -> u },
            y = scalar { _, v -> v },
            z = scalar { u, v -> u + v },
        )

    private fun scalar(
        evaluator: (Double, Double) -> Double,
    ): Surface3DScalarEvaluator =
        Surface3DScalarEvaluator { u, v ->
            GMResult.Ok(evaluator(u, v))
        }

    private fun dynamicRange(
        start: Double,
        end: () -> Double,
    ): List<Line3DCoordinateValue> =
        listOf(
            Line3DCoordinateValue.Numeric(start),
            Line3DCoordinateValue.Dynamic(
                Line3DScalarEvaluator { GMResult.Ok(end()) },
            ),
        )

    private fun range(
        vararg values: Double,
    ): List<Line3DCoordinateValue> =
        values.map(Line3DCoordinateValue::Numeric)

    private fun surface(
        result: GMResult<Surface3D, Surface3DError>,
    ): Surface3D = assertIs<GMResult.Ok<Surface3D>>(result).value

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
}
