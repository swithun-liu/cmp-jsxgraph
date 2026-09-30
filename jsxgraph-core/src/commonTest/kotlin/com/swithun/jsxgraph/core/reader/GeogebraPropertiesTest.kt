/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.utils.XML
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class GeogebraPropertiesTest {
    @Test
    fun richVisualPropertiesMatchOfficialFixture() {
        assertEquals(
            GeogebraElementAttributes(
                fillColor = "#0510ff",
                strokeColor = "#0510ff",
                highlightFillColor = "#0510ff",
                highlightStrokeColor = "#0510ff",
                fillOpacity = 1.0,
                highlightFillOpacity = 0.4,
                labelColor = "#0510ff",
                visible = false,
                withLabel = true,
                size = 7.0,
                styleGGB = 4.0,
                face = "diamond",
                slopeWidth = "3",
                strokeWidth = 3.0,
                highlightStrokeWidth = 4.0,
                dashGGB = "15",
                dash = 3,
                labelX = -2.5,
                labelY = 4.0,
                trace = "true",
                fixed = "false",
            ),
            parseElement(RICH_SOURCE),
        )
    }

    @Test
    fun pointStylesAndMissingColorMatchOfficialFixture() {
        assertEquals(
            GeogebraElementAttributes(
                fillColor = "#ff0010",
                strokeColor = "black",
                highlightFillColor = "#ff0010",
                highlightStrokeColor = "#ff0010",
                fillOpacity = 1.0,
                highlightFillOpacity = 1.0,
                labelColor = "#ff0010",
                styleGGB = 0.0,
                face = "circle",
                strokeWidth = 1.0,
                highlightStrokeWidth = 2.0,
            ),
            parseElement(STYLE_ZERO_SOURCE),
        )
        assertEquals(
            GeogebraElementAttributes(
                fillColor = "none",
                strokeColor = "#000",
                highlightFillColor = "#000",
                highlightStrokeColor = "#000",
                fillOpacity = 0.0,
                highlightFillOpacity = 0.0,
                labelColor = "#000",
                visible = true,
                withLabel = false,
                styleGGB = 2.0,
                face = "circle",
                strokeWidth = 0.0,
                dashGGB = "30",
                dash = 6,
            ),
            parseElement(NO_COLOR_SOURCE),
        )
    }

    @Test
    fun vectorRecognitionMatchesOfficialFixture() {
        assertTrue(GeogebraProperties.isGGBVector(listOf(1.0, 2.0, 3.0)))
        assertFalse(GeogebraProperties.isGGBVector(listOf(0.0, 2.0, 3.0)))
        assertFalse(GeogebraProperties.isGGBVector(listOf(1.0, 2.0)))
        assertFalse(GeogebraProperties.isGGBVector(null))
    }

    @Test
    fun coordinateSourcesMatchOfficialFixtureAndTrackAnchors() {
        val board = board()
        val anchor = point(board, 2.0, 3.0, "A")

        val direct = coordinates(
            "<coords x=\"4.5\" y=\"-2\" z=\"3\"/>" +
                "<labelOffset x=\"50\" y=\"25\"/>",
            board,
        )
        assertEquals(4.5, direct.x())
        assertEquals(-2.0, direct.y())
        assertEquals(3.0, direct.z)

        val startPoint = coordinates(
            "<startPoint x=\"1\" y=\"2\" z=\"3\"/>",
            board,
        )
        assertEquals(1.0, startPoint.x())
        assertEquals(2.0, startPoint.y())
        assertEquals(3.0, startPoint.z)

        val anchored = coordinates(
            "<startPoint exp=\"A\"/>" +
                "<labelOffset x=\"50\" y=\"25\"/>",
            board,
        )
        assertEquals(3.0, anchored.x())
        assertEquals(2.5, anchored.y())
        assertEquals(null, anchored.z)

        anchor.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, -1.0),
        )
        board.update()
        assertEquals(5.0, anchored.x())
        assertEquals(-1.5, anchored.y())

        val absolute = coordinates(
            "<absoluteScreenLocation x=\"300\" y=\"150\"/>" +
                "<labelOffset x=\"50\" y=\"25\"/>",
            board,
        )
        assertEquals(2.0, absolute.x())
        assertEquals(2.5, absolute.y())
        assertEquals(null, absolute.z)
    }

    @Test
    fun coordinateFailuresAreStructured() {
        val board = board()
        assertIs<GMResult.Err<GeogebraPropertyError.MissingCoordinateData>>(
            GeogebraProperties.parseElementCoordinates(
                source = element("<show object=\"true\"/>"),
                board = board,
            ),
        )
        assertEquals(
            GeogebraPropertyError.MissingStartPoint("missing"),
            assertIs<GMResult.Err<GeogebraPropertyError.MissingStartPoint>>(
                GeogebraProperties.parseElementCoordinates(
                    source = element("<startPoint exp=\"missing\"/>"),
                    board = board,
                ),
            ).error,
        )

        val point1 = point(board, 0.0, 0.0, "P")
        val point2 = point(board, 1.0, 1.0, "Q")
        assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = point1,
                point2 = point2,
                name = "line",
            ),
        )
        assertEquals(
            GeogebraPropertyError.StartPointTypeMismatch(
                name = "line",
                actualType = "line",
            ),
            assertIs<
                GMResult.Err<
                    GeogebraPropertyError.StartPointTypeMismatch
                    >
                >(
                GeogebraProperties.parseElementCoordinates(
                    source = element("<startPoint exp=\"line\"/>"),
                    board = board,
                ),
            ).error,
        )
    }

    @Test
    fun elementLookupAndBoardPropertiesMatchOfficialFixture() {
        val tree = assertIs<com.swithun.jsxgraph.core.utils.XmlDocument>(
            assertIs<GMResult.Ok<com.swithun.jsxgraph.core.utils.XmlDocument>>(
                XML.parse(
                    "<geogebra><construction>" +
                        "<element type=\"point\" label=\"A\"/>" +
                        "<element type=\"numeric\" label=\"n\"/>" +
                        "<element type=\"function\" label=\"f\"/>" +
                        "<expression label=\"n\" exp=\"2+3\"/>" +
                        "<expression label=\"f\" exp=\"x+1\"/>" +
                        "</construction></geogebra>",
                ),
            ).value,
        )

        val element = GeogebraProperties.getElement(tree, "A")
        assertEquals("element", element?.nodeName)
        assertEquals("A", element?.getAttribute("label"))
        val expression =
            GeogebraProperties.getElement(tree, "f", expression = true)
        assertEquals("expression", expression?.nodeName)
        assertEquals("x+1", expression?.getAttribute("exp"))
        val expressionValue =
            GeogebraProperties.getElement(tree, "2+3", expression = true)
        assertEquals("element", expressionValue?.nodeName)
        assertEquals("n", expressionValue?.getAttribute("label"))
        assertEquals(
            null,
            GeogebraProperties.getElement(tree, "missing"),
        )

        val attributes = GeogebraElementAttributes(visible = true)
        assertSame(
            attributes,
            GeogebraProperties.boardProperties(attributes),
        )
    }

    @Test
    fun preparationXmlLookupAndLimitsFailuresAreStructured() {
        assertIs<GMResult.Err<GeogebraPropertyError.PreparationFailed>>(
            GeogebraProperties.parseElementProperties("AQIDBA=="),
        )
        assertIs<GMResult.Err<GeogebraPropertyError.XmlParsingFailed>>(
            GeogebraProperties.parseElementProperties("<geogebra>"),
        )
        assertIs<GMResult.Err<GeogebraPropertyError.MissingElement>>(
            GeogebraProperties.parseElementProperties(
                "<geogebra><construction/></geogebra>",
            ),
        )
        assertIs<
            GMResult.Err<GeogebraPropertyError.ElementLimitExceeded>
        >(
            GeogebraProperties.parseElementProperties(
                source = NO_COLOR_SOURCE,
                limits = GeogebraPropertyLimits(maxElements = 0),
            ),
        )
        assertIs<GMResult.Err<GeogebraPropertyError.InvalidLimits>>(
            GeogebraProperties.parseElementProperties(
                source = NO_COLOR_SOURCE,
                limits = GeogebraPropertyLimits(maxElements = -1),
            ),
        )
    }

    private fun parseElement(
        source: String,
    ): GeogebraElementAttributes =
        assertIs<GMResult.Ok<GeogebraElementAttributes>>(
            GeogebraProperties.parseElementProperties(source),
        ).value

    private fun coordinates(
        body: String,
        board: Board,
    ): GeogebraElementCoordinates =
        assertIs<GMResult.Ok<GeogebraElementCoordinates>>(
            GeogebraProperties.parseElementCoordinates(
                source = element(body),
                board = board,
            ),
        ).value

    private fun element(body: String): String =
        "<geogebra><construction><element>$body</element>" +
            "</construction></geogebra>"

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                name = name,
            ),
        ).value

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )

    private companion object {
        const val RICH_SOURCE =
            "<geogebra><construction><element>" +
                "<objColor alpha=\"0.4\" r=\"5\" g=\"16\" b=\"255\"/>" +
                "<show object=\"false\" label=\"true\"/>" +
                "<pointSize val=\"7tail\"/><pointStyle val=\"4\"/>" +
                "<slopeTriangleSize val=\"3\"/>" +
                "<lineStyle thickness=\"5\" type=\"15\"/>" +
                "<labelOffset x=\"-2.5\" y=\"4\"/>" +
                "<trace val=\"true\"/><fix val=\"false\"/>" +
                "</element></construction></geogebra>"
        const val STYLE_ZERO_SOURCE =
            "<geogebra><construction><element>" +
                "<objColor alpha=\"0.25\" r=\"255\" g=\"0\" b=\"16\"/>" +
                "<pointStyle val=\"0\"/>" +
                "</element></construction></geogebra>"
        const val NO_COLOR_SOURCE =
            "<geogebra><construction><element>" +
                "<show object=\"true\" label=\"false\"/>" +
                "<pointStyle val=\"2\"/>" +
                "<lineStyle thickness=\"0\" type=\"30\"/>" +
                "</element></construction></geogebra>"
    }
}
