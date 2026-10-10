package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class Text3DTest {
    @Test
    fun staticAndDynamicCoordinatesProjectThroughTextProxy() {
        val board = createBoard()
        val view = createView(board)
        var x = -2.0
        val text = text3D(
            Text3D.create(
                view = view,
                coordinateSource = Point3DCoordinateSource.Values(
                    listOf(
                        Point3DCoordinateValue.Dynamic(
                            Point3DScalarEvaluator {
                                GMResult.Ok(x)
                            },
                        ),
                        Point3DCoordinateValue.Numeric(
                            1.519615242270663,
                        ),
                        Point3DCoordinateValue.Numeric(-1.0),
                    ),
                ),
                content = "-2",
                id = "label",
                name = "",
            ),
        )

        assertEquals("text3d", text.elType)
        assertEquals("-2", text.text2D.plaintext)
        assertContentEquals(
            doubleArrayOf(1.0, -2.0, 1.519615242270663, -1.0),
            text.coords,
        )
        assertEquals(-2.0, text.X())
        assertEquals(1.519615242270663, text.Y())
        assertEquals(-1.0, text.Z())
        assertCoordinates(
            expectedX = 0.22641937518774236,
            expectedY = -2.233304675143941,
            actual = text.text2D.coords.usrCoords,
        )
        assertSame(text, view.select("label"))
        assertEquals(listOf("label"), text.text2D.parents)

        x = 0.0
        board.fullUpdate()

        assertEquals(0.0, text.X())
        assertEquals(1.519615242270663, text.Y())
        assertEquals(-1.0, text.Z())
        assertCoordinates(
            expectedX = -0.6467091510951717,
            expectedY = -2.584926429716491,
            actual = text.text2D.coords.usrCoords,
        )
    }

    @Test
    fun setPositionNormalizesHomogeneousCoordinatesAndPersists() {
        val text = text3D(
            Text3D.create(
                view = createView(createBoard()),
                coordinateSource = numericCoordinates(1.0, 2.0, 3.0),
                content = "position",
                id = "text",
                name = "",
            ),
        )

        assertIs<GMResult.Ok<Text3D>>(
            text.setPosition(
                doubleArrayOf(2.0, 8.0, -4.0, 6.0),
            ),
        )
        text.prepareUpdate().update()

        assertContentEquals(
            doubleArrayOf(1.0, 4.0, -2.0, 3.0),
            text.coords,
        )
        assertTrue(text.testIfFinite())
        assertIs<GMResult.Err<Text3DError.InvalidCoordinateCount>>(
            text.setPosition(doubleArrayOf(1.0, 2.0)),
        )
    }

    @Test
    fun proxyDragUpdatesThreeDimensionalCoordinates() {
        val board = createBoard()
        val text = text3D(
            Text3D.create(
                view = createView(board),
                coordinateSource = numericCoordinates(1.0, 2.0, 3.0),
                content = "drag",
                id = "text",
                name = "",
            ),
        )
        val original = text.coords.copyOf()
        val projection = text.text2D.coords.usrCoords.copyOf()
        text.text2D.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(
                projection[1] + 0.5,
                projection[2] - 0.25,
            ),
        )

        board.fullUpdate()

        assertEquals(original[3], text.coords[3], TOLERANCE)
        assertTrue(
            original[1] != text.coords[1] ||
                original[2] != text.coords[2],
        )
    }

    @Test
    fun slideProjectsTextOntoLineAndRegistersDependency() {
        val board = createBoard()
        val view = createView(board)
        val first = point(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(-2.0, 0.0, 0.0),
                id = "A",
                name = "",
            ),
        )
        val second = point(
            Point3D.create(
                view = view,
                coordinates = doubleArrayOf(2.0, 0.0, 0.0),
                id = "B",
                name = "",
            ),
        )
        val line = assertIs<GMResult.Ok<Line3D>>(
            Line3D.create(
                view = view,
                point1 = first,
                point2 = second,
                id = "line",
                name = "",
            ),
        ).value
        val text = text3D(
            Text3D.create(
                view = view,
                coordinateSource = numericCoordinates(0.5, 3.0, -2.0),
                content = "slide",
                slide = line,
                id = "text",
                name = "",
            ),
        )

        assertVectorMatches(
            doubleArrayOf(1.0, 2.0, 0.0, 0.0),
            text.coords,
        )
        assertEquals(listOf(line.id), text.parents)
        assertSame(text, line.childElements[text.id])
    }

    @Test
    fun geometryElement3DAttributesDelegateToTheTwoDimensionalProxy() {
        val text = text3D(
            Text3D.create(
                view = createView(createBoard()),
                coordinateSource = numericCoordinates(1.0, 2.0, 3.0),
                content = "attributes",
                id = "text",
                name = "label",
            ),
        )

        text.setAttribute(
            mapOf(
                "fillColor" to "#123456",
                "stepsU" to 12,
                "strokeWidth" to 3.0,
            ),
        )

        assertEquals("#123456", text.getAttribute("fillColor"))
        assertEquals(12, text.getAttribute("stepsU"))
        assertEquals(3.0, text.getAttribute("strokeWidth"))
        assertNull(text.text2D.getAttribute("fillColor"))
        assertEquals(3.0, text.text2D.getAttribute("strokeWidth"))
        val attributes = text.getAttributes()
        assertContentEquals(
            doubleArrayOf(0.0, 0.0),
            assertIs<DoubleArray>(attributes["offset"]),
        )
        assertEquals(
            mapOf(
                "formatnumber" to false,
                "digits" to 2,
                "islabel" to false,
                "intl" to mapOf(
                    "enabled" to "inherit",
                    "options" to mapOf(
                        "minimumfractiondigits" to 0,
                        "maximumfractiondigits" to 2,
                    ),
                ),
                "withlabel" to false,
                "element3d" to text,
                "strokewidth" to 3.0,
                "highlightstrokewidth" to 3.0,
                "fillcolor" to "#123456",
                "stepsu" to 12,
            ),
            attributes - "offset",
        )
        assertEquals(
            listOf<GeometryElement>(text.text2D),
            text.inherits,
        )
        assertNull(text.elementLabel)

        val proxyAttributes = linkedMapOf<String, Any?>("visible" to true)
        assertSame(proxyAttributes, text.setAttr2D(proxyAttributes))
        assertEquals("label", proxyAttributes["name"])
        assertSame(text, proxyAttributes["element3d"])
        assertTrue("id" in proxyAttributes)
        assertNull(proxyAttributes["id"])
    }

    @Test
    fun removalCleansProxyAndViewRegistry() {
        val board = createBoard()
        val view = createView(board)
        val text = text3D(
            Text3D.create(
                view = view,
                coordinateSource = numericCoordinates(1.0, 2.0, 3.0),
                content = "hello",
                id = "text",
                name = "",
            ),
        )
        val proxyId = text.text2D.id

        board.removeObject(text)

        assertNull(board.elementById("text"))
        assertNull(board.elementById(proxyId))
        assertNull(view.select("text"))
    }

    @Test
    fun malformedCoordinatesReturnStructuredErrors() {
        assertIs<GMResult.Err<Text3DError.InvalidCoordinateCount>>(
            Text3D.create(
                view = createView(createBoard()),
                coordinateSource =
                    Point3DCoordinateSource.Values(
                        listOf(
                            Point3DCoordinateValue.Numeric(1.0),
                            Point3DCoordinateValue.Numeric(2.0),
                        ),
                    ),
                content = "invalid",
            ),
        )
    }

    private fun numericCoordinates(
        x: Double,
        y: Double,
        z: Double,
    ): Point3DCoordinateSource =
        Point3DCoordinateSource.Values(
            listOf(x, y, z).map(Point3DCoordinateValue::Numeric),
        )

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

    private fun text3D(
        result: GMResult<Text3D, Text3DError>,
    ): Text3D = assertIs<GMResult.Ok<Text3D>>(result).value

    private fun point(
        result: GMResult<Point3D, Point3DError>,
    ): Point3D = assertIs<GMResult.Ok<Point3D>>(result).value

    private fun assertCoordinates(
        expectedX: Double,
        expectedY: Double,
        actual: DoubleArray,
    ) {
        assertEquals(expectedX, actual[1], TOLERANCE)
        assertEquals(expectedY, actual[2], TOLERANCE)
    }

    private fun assertVectorMatches(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(expected[index], actual[index], TOLERANCE)
        }
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
