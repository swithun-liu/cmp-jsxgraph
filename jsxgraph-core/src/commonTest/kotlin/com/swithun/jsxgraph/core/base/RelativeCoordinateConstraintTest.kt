package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeConstantCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeCoordsCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.JessieCodeSliderCoordinateFunction
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RelativeCoordinateConstraintTest {
    @Test
    fun relativeCoordinateElementsMatchOfficialAnchorLifecycle() {
        val board = board()
        val anchor = point(board, 1.0, 2.0, "anchor")
        val relativePoint = point(
            board = board,
            x = 0.5,
            y = -1.0,
            id = "relativePoint",
            anchor = anchor,
        )
        val relativeImage = assertIs<GMResult.Ok<Image>>(
            Image.create(
                board = board,
                urlTerm = string(
                    "data:image/gif;base64," +
                        "R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
                ),
                coordinates = doubleArrayOf(0.25, -0.5),
                sizeTerms = listOf(number(1.0), number(1.0)),
                id = "relativeImage",
                name = "",
                anchor = anchor,
            ),
        ).value
        val relativeForeignObject =
            assertIs<GMResult.Ok<ForeignObject>>(
                ForeignObject.create(
                    board = board,
                    content = "<span>relative</span>",
                    coordinates = doubleArrayOf(-0.75, 0.25),
                    sizeTerms = listOf(number(1.0), number(1.0)),
                    id = "relativeForeignObject",
                    name = "",
                    anchor = anchor,
                ),
            ).value
        val relativeText = assertIs<GMResult.Ok<Text>>(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(1.0, -2.0),
                content = "relative",
                id = "relativeText",
                name = "",
                anchor = anchor,
            ),
        ).value
        val relativeLabel = assertIs<GMResult.Ok<Text>>(
            Text.create(
                board = board,
                coordinates = doubleArrayOf(10.0, 20.0),
                content = "label",
                id = "relativeLabel",
                name = "",
                anchor = anchor,
                isLabel = true,
                offset = doubleArrayOf(5.0, 7.0),
            ),
        ).value
        board.update()

        assertCoordinates(relativePoint, 1.5, 1.0)
        assertCoordinates(relativeImage, 1.25, 1.5)
        assertCoordinates(relativeForeignObject, 0.25, 2.25)
        assertCoordinates(relativeText, 2.0, 0.0)
        assertCoordinates(relativeLabel, 1.375, 1.675)
        assertEquals(
            listOf(
                "relativePoint",
                "relativeImage",
                "relativeForeignObject",
                "relativeText",
                "relativeLabel",
            ),
            anchor.childElements.keys.toList(),
        )
        assertEquals(emptyList(), relativePoint.parents)
        assertEquals(listOf("anchor"), relativeLabel.parents)

        assertSame(
            relativePoint,
            relativePoint.setPositionDirectly(
                Const.COORDS_BY_USER,
                doubleArrayOf(4.0, 3.0),
            ),
        )
        assertCoordinates(relativePoint, 1.5, 1.0)
        assertContentEquals(
            doubleArrayOf(1.0, 3.0, 1.0),
            requireNotNull(relativePoint.relativeCoords).usrCoords,
        )
        board.update()
        assertCoordinates(relativePoint, 4.0, 3.0)

        anchor.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -1.0),
        )
        board.update()

        assertCoordinates(relativePoint, 1.0, 0.0)
        assertCoordinates(relativeImage, -1.75, -1.5)
        // ForeignObject defaults to needsRegularUpdate=false upstream.
        assertCoordinates(relativeForeignObject, 0.25, 2.25)
        assertCoordinates(relativeText, -1.0, -3.0)
        assertCoordinates(relativeLabel, -1.625, -1.325)
    }

    @Test
    fun sliderAndCoordsObjectConstraintResultsMatchOfficialUpdates() {
        val board = board()
        val slider = assertIs<GMResult.Ok<Slider>>(
            Slider.create(
                board = board,
                startCoordinates = doubleArrayOf(-4.0, -4.0),
                endCoordinates = doubleArrayOf(4.0, -4.0),
                range = doubleArrayOf(-2.0, 1.0, 6.0),
                attributes = SliderAttributes(
                    id = "slider",
                    name = "",
                    withLabel = false,
                    withTicks = false,
                ),
            ),
        ).value
        val sliderPoint = assertIs<GMResult.Ok<Point>>(
            Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    JessieCodeSliderCoordinateFunction(slider),
                    SliderOffsetCoordinateFunction(slider, 1.0),
                ),
                id = "sliderPoint",
                name = "",
            ),
        ).value

        var coordsX = 2.0
        var coordsY = -3.0
        val coordsPoint = assertIs<GMResult.Ok<Point>>(
            Point.createConstrained(
                board = board,
                coordinateFunctions = listOf(
                    JessieCodeCoordsCoordinateFunction {
                        GMResult.Ok(
                            Coords(
                                method = Const.COORDS_BY_USER,
                                coordinates =
                                    doubleArrayOf(coordsX, coordsY),
                                board = board,
                            ),
                        )
                    },
                ),
                id = "coordsPoint",
                name = "",
            ),
        ).value
        board.update()

        assertCoordinates(sliderPoint, 1.0, 2.0)
        assertEquals(listOf("slider"), sliderPoint.parents)
        assertCoordinates(coordsPoint, 2.0, -3.0)
        val firstCoords = coordsPoint.coords

        slider.setValue(5.0)
        coordsX = -6.0
        coordsY = 4.0
        board.update()

        assertCoordinates(sliderPoint, 5.0, 6.0)
        assertCoordinates(coordsPoint, -6.0, 4.0)
        assertNotSame(firstCoords, coordsPoint.coords)
        assertTrue(coordsPoint.isConstrained)
    }

    private fun board(): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        canvasWidth = 640.0,
        canvasHeight = 480.0,
        id = "board",
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
        anchor: GeometryElement? = null,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
                anchor = anchor,
            ),
        ).value

    private fun number(value: Double): JessieCodeCoordinateFunction =
        JessieCodeConstantCoordinateFunction(
            JessieCodeRuntimeValue.NumberValue(value),
        )

    private fun string(value: String): JessieCodeCoordinateFunction =
        JessieCodeConstantCoordinateFunction(
            JessieCodeRuntimeValue.StringValue(value),
        )

    private fun assertCoordinates(
        element: CoordsElement,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, element.X(), absoluteTolerance = 1.0e-12)
        assertEquals(y, element.Y(), absoluteTolerance = 1.0e-12)
    }

    private class SliderOffsetCoordinateFunction(
        private val slider: Slider,
        private val offset: Double,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> =
            mapOf(slider.id to slider)

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(
                JessieCodeRuntimeValue.NumberValue(
                    slider.Value() + offset,
                ),
            )
    }
}
