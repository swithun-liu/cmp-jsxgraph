package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Plane3DTest {
    @Test
    fun finiteAndThreePointFormsMatchOfficialWireframeFixture() {
        val board = createBoard()
        val view = createView(board)
        val numeric = point(view, doubleArrayOf(1.0, 2.0, 2.0), "numeric")
        val functionPoint = point(
            view,
            doubleArrayOf(2.0, -1.0, 3.0),
            "functionPoint",
        )
        val mixedPoint = point(
            view,
            doubleArrayOf(-2.0, 3.0, 1.0),
            "mixedPoint",
        )
        val finite = plane(
            Plane3D.create(
                view = view,
                point = numeric,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 1.0),
                rangeUSource = range(-2.0, 3.0),
                rangeVSource = range(-1.0, 2.0),
                planeType = "wireframe",
                id = "finitePlane",
                name = "",
            ),
        )
        val threePoint = plane(
            Plane3D.create(
                view = view,
                point1 = numeric,
                point2 = functionPoint,
                point3 = mixedPoint,
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-3.0, 1.0),
                planeType = "wireframe",
                id = "threePointPlane",
                name = "",
            ),
        )

        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(0.0, 1.0, 0.0, 0.0),
            finite.vec1,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 1.0, 1.0),
            finite.vec2,
        )
        assertVectorMatches(
            doubleArrayOf(
                0.0,
                0.0,
                -0.7071067811865475,
                0.7071067811865475,
            ),
            finite.normal,
        )
        assertEquals(0.0, finite.d, TOLERANCE)
        assertVectorMatches(
            doubleArrayOf(
                1.0,
                1.0,
                2.5,
                1.823223304703363,
            ),
            finite.F(0.5, -0.25),
        )
        assertVectorMatches(
            doubleArrayOf(
                -0.36429513147350645,
                -2.5471164471807914,
                -1.10481259616156,
                1.078008719545725,
                -0.36429513147350645,
            ),
            finite.dataX,
        )
        assertVectorMatches(
            doubleArrayOf(
                -0.8348490315008023,
                -1.713903417932177,
                -0.5205842898903073,
                0.35847009654106743,
                -0.8348490315008023,
            ),
            finite.dataY,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 1.0, -3.0, 1.0),
            threePoint.vec1,
        )
        assertContentEquals(
            doubleArrayOf(0.0, -3.0, 1.0, -1.0),
            threePoint.vec2,
        )
        assertEquals(emptyList(), finite.parents)
        assertTrue(finite.id in numeric.descendants)
        assertFalse(threePoint.id in functionPoint.descendants)
        assertFalse(threePoint.id in mixedPoint.descendants)
        assertSame(finite.outline2D, finite.element2D)
        assertFalse(finite.outline2D.dump)
        assertTrue(requireNotNull(finite.mesh3D).isMesh3D)
        assertEquals(58, requireNotNull(finite.mesh3D).numberPoints)
        assertTrue(requireNotNull(threePoint.mesh3D).isMesh3D)
    }

    @Test
    fun dynamicThreePointAndTransformedPlaneFollowBoardUpdates() {
        val board = createBoard()
        val view = createView(board)
        val first = point(view, doubleArrayOf(1.0, 2.0, 2.0), "first")
        val second = point(view, doubleArrayOf(2.0, -1.0, 3.0), "second")
        val third = point(view, doubleArrayOf(-2.0, 3.0, 1.0), "third")
        val source = plane(
            Plane3D.create(
                view = view,
                point1 = first,
                point2 = second,
                point3 = third,
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-3.0, 1.0),
                planeType = "wireframe",
                id = "source",
                name = "",
            ),
        )
        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create3D(
                "translate",
                doubleArrayOf(2.0, -3.0, 4.0),
            ),
        ).value
        val transformed = plane(
            Plane3D.create(
                view = view,
                basePlane = source,
                transformations = listOf(translation),
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-3.0, 1.0),
                planeType = "wireframe",
                id = "transformed",
                name = "",
            ),
        )

        board.fullUpdate()
        assertVectorMatches(
            doubleArrayOf(1.0, 3.0, -1.0, 6.0),
            transformed.point.coords,
        )
        assertContentEquals(source.vec1, transformed.vec1)
        assertContentEquals(source.vec2, transformed.vec2)
        assertEquals("source", transformed.parents.single())

        assertIs<GMResult.Ok<Point3D>>(
            first.setPosition(doubleArrayOf(2.0, -2.0, 1.0)),
        )
        assertIs<GMResult.Ok<Point3D>>(
            second.setPosition(doubleArrayOf(-3.0, 4.0, 1.0)),
        )
        assertIs<GMResult.Ok<Point3D>>(
            third.setPosition(doubleArrayOf(5.0, 3.0, 0.0)),
        )
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(0.0, -5.0, 6.0, 0.0),
            source.vec1,
        )
        assertContentEquals(
            doubleArrayOf(0.0, 3.0, 5.0, -1.0),
            source.vec2,
        )
        assertVectorMatches(
            doubleArrayOf(1.0, 4.0, -5.0, 5.0),
            transformed.point.coords,
        )
        assertContentEquals(source.vec1, transformed.vec1)
        assertContentEquals(source.vec2, transformed.vec2)
    }

    @Test
    fun fullyInfinitePlaneClipsToViewBoundingBox() {
        val board = createBoard()
        val view = createView(board)
        val origin = point(view, doubleArrayOf(0.0, 0.0, 0.0), "origin")
        val plane = plane(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                planeType = "wireframe",
                id = "plane",
                name = "",
            ),
        )

        board.fullUpdate()

        assertEquals(5, plane.dataX.size)
        assertEquals(5, plane.dataY.size)
        assertTrue(plane.dataX.all(Double::isFinite))
        assertTrue(plane.dataY.all(Double::isFinite))
        assertEquals(plane.dataX.first(), plane.dataX.last(), TOLERANCE)
        assertEquals(plane.dataY.first(), plane.dataY.last(), TOLERANCE)
    }

    @Test
    fun removalCleansProxyAndViewRegistry() {
        val board = createBoard()
        val view = createView(board)
        val origin = point(view, doubleArrayOf(0.0, 0.0, 0.0), "origin")
        val plane = plane(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                planeType = "wireframe",
                id = "plane",
                name = "",
            ),
        )
        val proxyId = plane.outline2D.id
        val meshId = requireNotNull(plane.mesh3D).id

        board.removeObject(plane)

        assertNull(board.elementById(plane.id))
        assertNull(view.select(plane.id))
        assertNull(board.elementById(proxyId))
        assertNull(board.elementById(meshId))
        assertSame(origin, board.elementById(origin.id))
    }

    @Test
    fun finiteSurfaceUsesOfficialRectangleAndTriangleTiling() {
        val board = createBoard()
        val view = createView(board)
        val origin = point(view, doubleArrayOf(0.0, 0.0, 0.0), "origin")
        val rectangle = plane(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-1.0, 1.0),
                planeType = "colorarray",
                surfaceAttributes = Plane3DSurfaceAttributes(
                    stepsU = 2,
                    stepsV = 1,
                    fillColorArray = listOf("#ff0000", "#0000ff"),
                ),
                id = "rectangle",
                name = "",
            ),
        )
        val triangle = plane(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-2.0, 2.0),
                rangeVSource = range(-1.0, 1.0),
                planeType = "shader",
                surfaceAttributes = Plane3DSurfaceAttributes(
                    tiling = "triangle",
                    stepsU = 2,
                    stepsV = 2,
                ),
                id = "triangle",
                name = "",
            ),
        )

        board.fullUpdate()

        val rectangleSurface = requireNotNull(rectangle.surface3D)
        assertEquals(6, rectangleSurface.definition.vertices.size)
        assertEquals(2, rectangleSurface.numberFaces)
        assertEquals(
            listOf("3", "4", "1", "0"),
            rectangleSurface.definition.faceKeys[0],
        )
        assertEquals(
            listOf("#ff0000", "#0000ff"),
            rectangleSurface.faces.map(Face3D::resolvedFillColor),
        )
        assertEquals(0, rectangle.outline2D.points.size)

        val triangleSurface = requireNotNull(triangle.surface3D)
        assertEquals(10, triangleSurface.definition.vertices.size)
        assertEquals(11, triangleSurface.numberFaces)
        assertEquals(
            listOf("3", "4", "0"),
            triangleSurface.definition.faceKeys[0],
        )
        assertEquals(
            triangleSurface.definition.faceKeys[4],
            triangleSurface.definition.faceKeys[5],
        )
        assertTrue(
            triangleSurface.faces.all {
                it.faceAttributes.shader.enabled
            },
        )
    }

    @Test
    fun colormapAndDynamicVerticesFollowPlaneUpdates() {
        val board = createBoard()
        val view = createView(board)
        val first = point(view, doubleArrayOf(0.0, 0.0, 0.0), "first")
        val second = point(view, doubleArrayOf(2.0, 0.0, 0.0), "second")
        val third = point(view, doubleArrayOf(0.0, 2.0, 0.0), "third")
        val plane = plane(
            Plane3D.create(
                view = view,
                point1 = first,
                point2 = second,
                point3 = third,
                rangeUSource = range(0.0, 1.0),
                rangeVSource = range(0.0, 1.0),
                planeType = "colormap",
                surfaceAttributes = Plane3DSurfaceAttributes(
                    stepsU = 1,
                    stepsV = 1,
                    colormap = Plane3DColormapAttributes(
                        minimumHeight = -1.0,
                        minimumHue = 240.0,
                        maximumHeight = 1.0,
                        maximumHue = 0.0,
                        saturation = 1.0,
                        value = 1.0,
                    ),
                ),
                id = "colormap",
                name = "",
            ),
        )

        board.fullUpdate()

        val surface = requireNotNull(plane.surface3D)
        assertEquals("#00ff00", surface.faces.single().resolvedFillColor())
        assertContentEquals(
            doubleArrayOf(1.0, 2.0, 2.0, 0.0),
            surface.definition.coords.getValue("3"),
        )

        assertIs<GMResult.Ok<Point3D>>(
            second.setPosition(doubleArrayOf(3.0, 0.0, 1.0)),
        )
        assertIs<GMResult.Ok<Point3D>>(
            third.setPosition(doubleArrayOf(0.0, 4.0, 1.0)),
        )
        board.fullUpdate()

        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 4.0, 2.0),
            surface.definition.coords.getValue("3"),
        )
        assertEquals("#ff0000", surface.faces.single().resolvedFillColor())
    }

    @Test
    fun surfaceRemovalAndLimitsAreSafe() {
        val board = createBoard()
        val view = createView(board)
        val origin = point(view, doubleArrayOf(0.0, 0.0, 0.0), "origin")
        val plane = plane(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                surfaceAttributes = Plane3DSurfaceAttributes(
                    stepsU = 2,
                    stepsV = 2,
                ),
                id = "surface",
                name = "",
            ),
        )
        val surface = requireNotNull(plane.surface3D)
        val surfaceId = surface.id
        val faceIds = surface.faces.map(Face3D::id)
        val curveIds = surface.faces.map { it.curve2D.id }

        board.removeObject(plane)

        assertNull(board.elementById(surfaceId))
        for (id in faceIds + curveIds) {
            assertNull(board.elementById(id))
        }
        assertIs<GMResult.Err<Plane3DError.InvalidSurfaceSteps>>(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                surfaceAttributes = Plane3DSurfaceAttributes(stepsU = 0),
            ),
        )
        assertIs<GMResult.Err<Plane3DError.SurfaceVertexLimitExceeded>>(
            Plane3D.create(
                view = view,
                point = origin,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
                surfaceAttributes = Plane3DSurfaceAttributes(
                    stepsU = Int.MAX_VALUE,
                ),
            ),
        )
    }

    @Test
    fun malformedInputsReturnStructuredErrors() {
        val view = createView(createBoard())
        val point = point(view, doubleArrayOf(0.0, 0.0, 0.0), "point")

        assertIs<GMResult.Err<Plane3DError.InvalidDirectionCount>>(
            Plane3D.create(
                view = view,
                point = point,
                direction1Source = Plane3DDirectionSource.Values(
                    listOf(Line3DCoordinateValue.Numeric(1.0)),
                ),
                direction2Source = values(0.0, 1.0, 0.0),
            ),
        )
        assertIs<GMResult.Err<Plane3DError.InvalidRangeCount>>(
            Plane3D.create(
                view = view,
                point = point,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource =
                    listOf(Line3DCoordinateValue.Numeric(0.0)),
            ),
        )
        val valid = plane(
            Plane3D.create(
                view = view,
                point = point,
                direction1Source = values(1.0, 0.0, 0.0),
                direction2Source = values(0.0, 1.0, 0.0),
                rangeUSource = range(-1.0, 1.0),
                rangeVSource = range(-1.0, 1.0),
            ),
        )
        assertIs<GMResult.Err<Plane3DError.InvalidTransformationCount>>(
            Plane3D.create(
                view = view,
                basePlane = valid,
                transformations = emptyList(),
            ),
        )
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

    private fun plane(
        result: GMResult<Plane3D, Plane3DError>,
    ): Plane3D = assertIs<GMResult.Ok<Plane3D>>(result).value

    private fun values(
        vararg values: Double,
    ): Plane3DDirectionSource =
        Plane3DDirectionSource.Values(
            values.map(Line3DCoordinateValue::Numeric),
        )

    private fun range(
        start: Double,
        end: Double,
    ): List<Line3DCoordinateValue> =
        listOf(
            Line3DCoordinateValue.Numeric(start),
            Line3DCoordinateValue.Numeric(end),
        )

    private fun assertVectorMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected = expected[index],
                actual = actual[index],
                absoluteTolerance = TOLERANCE,
            )
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
