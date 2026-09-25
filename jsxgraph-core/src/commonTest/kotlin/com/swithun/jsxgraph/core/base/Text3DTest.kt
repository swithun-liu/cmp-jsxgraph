package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame

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
        assertCoordinates(
            expectedX = 0.22641937518774236,
            expectedY = -2.233304675143941,
            actual = text.text2D.coords.usrCoords,
        )
        assertSame(text, view.select("label"))
        assertEquals(listOf("label"), text.text2D.parents)

        x = 0.0
        board.fullUpdate()

        assertCoordinates(
            expectedX = -0.6467091510951717,
            expectedY = -2.584926429716491,
            actual = text.text2D.coords.usrCoords,
        )
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

    private fun assertCoordinates(
        expectedX: Double,
        expectedY: Double,
        actual: DoubleArray,
    ) {
        assertEquals(expectedX, actual[1], TOLERANCE)
        assertEquals(expectedY, actual[2], TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
