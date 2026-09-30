/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class GeogebraReaderTest {
    @Test
    fun commandDependenciesExpressionsAndSliderRemainDynamic() {
        val board = board()
        val drawn = read(BASIC_SOURCE, board)

        val first = assertIs<Point>(drawn.objects.getValue("A"))
        val second = assertIs<Point>(drawn.objects.getValue("B"))
        val segment = assertIs<Line>(drawn.objects.getValue("s"))
        assertSame(first, segment.point1)
        assertSame(second, segment.point2)
        assertEquals(false, segment.straightFirst)
        assertEquals(false, segment.straightLast)
        assertIs<Text>(drawn.objects.getValue("distance"))
        assertEquals(3.0, scalar(drawn, "distance"), TOLERANCE)
        assertIs<Text>(drawn.objects.getValue("slope"))
        assertEquals(0.0, scalar(drawn, "slope"), TOLERANCE)

        val translated = assertIs<Point>(
            drawn.objects.getValue("C"),
        )
        assertPoint(3.0, 5.0, translated)
        val slider = assertIs<Slider>(drawn.objects.getValue("n"))
        val dynamic = assertIs<Point>(drawn.objects.getValue("D"))
        assertPoint(4.5, 6.5, dynamic)

        val function = assertIs<Curve>(drawn.objects.getValue("f"))
        assertEquals(7.0, function.Y(3.0), TOLERANCE)
        assertIs<Circle>(drawn.objects.getValue("circle"))
        assertEquals(
            listOf(-1.25, 11.25, 11.25, -1.25),
            board.getBoundingBox().toList(),
        )

        slider.setValue(2.5)
        second.setPositionDirectly(
            method = 1,
            coordinates = doubleArrayOf(4.0, 5.0),
        )
        board.fullUpdate()

        assertPoint(5.5, 7.5, dynamic)
        assertEquals(
            kotlin.math.sqrt(18.0),
            scalar(drawn, "distance"),
            TOLERANCE,
        )
        assertEquals(1.0, scalar(drawn, "slope"), TOLERANCE)
    }

    @Test
    fun unsupportedCommandsRollbackBoardAndLimitsAreStructured() {
        val board = board()
        val seed = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(9.0, 8.0),
                id = "seed",
                name = "seed",
            ),
        ).value
        val originalBoundingBox = board.getBoundingBox()

        assertEquals(
            GeogebraReaderError.UnsupportedCommand(
                "unsupportedthing",
            ),
            assertIs<GMResult.Err<GeogebraReaderError.UnsupportedCommand>>(
                GeogebraReader(UNSUPPORTED_SOURCE).read(board),
            ).error,
        )
        assertEquals(listOf(seed), board.objects.values.toList())
        assertSame(seed, board.elementByName("seed"))
        assertEquals(
            originalBoundingBox.toList(),
            board.getBoundingBox().toList(),
        )

        assertIs<GMResult.Err<GeogebraReaderError.InvalidLimits>>(
            GeogebraReader(BASIC_SOURCE).read(
                board = board,
                limits = GeogebraReaderLimits(maxObjects = -1),
            ),
        )
        assertIs<
            GMResult.Err<GeogebraReaderError.CommandLimitExceeded>
            >(
            GeogebraReader(BASIC_SOURCE).read(
                board = board,
                limits = GeogebraReaderLimits(maxCommands = 0),
            ),
        )
    }

    @Test
    fun registryReadsGeogebraAndContainsDomainFailures() {
        val registry = ReaderRegistry<Board>().apply {
            registerGeogebraReader()
        }
        val board = board()

        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source = BASIC_SOURCE,
                board = board,
                format = "GGB",
                registry = registry,
            ),
        )
        assertIs<Point>(board.elementByName("A"))
        assertIs<GMResult.Err<ReaderError.DomainFailure>>(
            FileReader.parseString(
                source = UNSUPPORTED_SOURCE,
                board = board(),
                format = "geogebra",
                registry = registry,
            ),
        )
    }

    @Test
    fun highOrderCommandsCreateDynamicGeometry() {
        val board = board()
        val drawn = read(HIGH_ORDER_SOURCE, board)

        assertIs<Arc>(drawn.objects.getValue("circleArc"))
        assertIs<Sector>(drawn.objects.getValue("circleSector"))
        assertIs<Arc>(drawn.objects.getValue("circumcircleArc"))
        assertIs<Sector>(
            drawn.objects.getValue("circumcircleSector"),
        )
        assertIs<Arc>(drawn.objects.getValue("semicircle"))
        assertIs<Sector>(drawn.objects.getValue("angle"))
        assertIs<Line>(drawn.objects.getValue("angleBisector"))

        val pointEllipse = assertIs<Curve>(
            drawn.objects.getValue("pointEllipse"),
        )
        val axisEllipse = assertIs<Curve>(
            drawn.objects.getValue("axisEllipse"),
        )
        assertEquals(
            2.0 * kotlin.math.sqrt(13.0),
            pointEllipse.majorAxis(),
            TOLERANCE,
        )
        assertEquals(6.0, axisEllipse.majorAxis(), TOLERANCE)
        assertIs<Curve>(drawn.objects.getValue("fivePointConic"))
        assertIs<Curve>(drawn.objects.getValue("matrixConic"))
        assertIs<Line>(drawn.objects.getValue("polar"))

        val root = assertIs<Point>(drawn.objects.getValue("root"))
        assertEquals(2.0, root.X(), 1.0e-6)
        assertEquals(0.0, root.Y(), TOLERANCE)
        val integral = assertIs<Curve>(
            drawn.objects.getValue("integral"),
        )
        assertEquals(-16.0 / 3.0, integral.Value(), 1.0e-3)

        val slider = assertIs<Slider>(drawn.objects.getValue("k"))
        val text = assertIs<Text>(drawn.objects.getValue("dynamicText"))
        assertEquals("k = 1.5", text.plaintext)
        slider.setValue(2.5)
        board.fullUpdate()
        assertEquals("k = 2.5", text.plaintext)
    }

    private fun read(
        source: String,
        board: Board = board(),
    ): DrawnGeogebra =
        assertIs<GMResult.Ok<DrawnGeogebra>>(
            GeogebraReader(source).read(board),
        ).value

    private fun scalar(
        drawn: DrawnGeogebra,
        name: String,
    ): Double {
        val value = assertIs<GeogebraReaderValue.ElementScalar>(
            drawn.values.getValue(name),
        )
        return assertIs<GMResult.Ok<Double>>(
            value.scalar.evaluate(),
        ).value
    }

    private fun assertPoint(
        expectedX: Double,
        expectedY: Double,
        actual: Point,
    ) {
        assertEquals(expectedX, actual.X(), TOLERANCE)
        assertEquals(expectedY, actual.Y(), TOLERANCE)
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )

    private companion object {
        const val TOLERANCE = 1.0e-8

        val BASIC_SOURCE = """
            <geogebra format="5.0">
              <euclidianView>
                <coordSystem
                    xZero="50"
                    yZero="450"
                    scale="40"
                    yscale="40"
                />
              </euclidianView>
              <kernel><decimals val="4"/></kernel>
              <construction>
                <element type="point" label="A">
                  <coords x="1" y="2" z="1"/>
                </element>
                <element type="point" label="B">
                  <coords x="4" y="2" z="1"/>
                </element>
                <command name="Segment">
                  <input a0="A" a1="B"/>
                  <output a0="s"/>
                </command>
                <element type="segment" label="s"/>
                <command name="Distance">
                  <input a0="A" a1="B"/>
                  <output a0="distance"/>
                </command>
                <element type="numeric" label="distance"/>
                <command name="Slope">
                  <input a0="s"/>
                  <output a0="slope"/>
                </command>
                <element type="numeric" label="slope">
                  <slopeTriangleSize val="1"/>
                </element>
                <expression label="C" exp="A + (2, 3)"/>
                <element type="point" label="C">
                  <coords x="3" y="5" z="1"/>
                </element>
                <element type="numeric" label="n">
                  <value val="1.5"/>
                  <slider
                      min="0"
                      max="5"
                      absoluteScreenLocation="false"
                      width="4"
                      x="0"
                      y="-2"
                      horizontal="true"
                  />
                  <animation step="0.5"/>
                </element>
                <expression label="D" exp="C + (n, n)"/>
                <element type="point" label="D">
                  <coords x="4.5" y="6.5" z="1"/>
                </element>
                <expression label="f" exp="f(x) = 2x + 1"/>
                <element type="function" label="f"/>
                <command name="Circle">
                  <input a0="A" a1="n"/>
                  <output a0="circle"/>
                </command>
                <element type="conic" label="circle"/>
              </construction>
            </geogebra>
        """.trimIndent()

        val UNSUPPORTED_SOURCE = """
            <geogebra format="5.0">
              <euclidianView>
                <coordSystem
                    xZero="100"
                    yZero="300"
                    scale="20"
                    yscale="20"
                />
              </euclidianView>
              <construction>
                <element type="point" label="A">
                  <coords x="1" y="2" z="1"/>
                </element>
                <command name="UnsupportedThing">
                  <input a0="A"/>
                  <output a0="k"/>
                </command>
                <element type="numeric" label="k">
                  <value val="1"/>
                </element>
              </construction>
            </geogebra>
        """.trimIndent()

        val HIGH_ORDER_SOURCE = """
            <geogebra format="5.0">
              <euclidianView>
                <coordSystem
                    xZero="250"
                    yZero="250"
                    scale="50"
                    yscale="50"
                />
              </euclidianView>
              <kernel><decimals val="4"/></kernel>
              <construction>
                <element type="point" label="A">
                  <coords x="-2" y="0" z="1"/>
                </element>
                <element type="point" label="B">
                  <coords x="2" y="0" z="1"/>
                </element>
                <element type="point" label="C">
                  <coords x="0" y="3" z="1"/>
                </element>
                <element type="point" label="D">
                  <coords x="-1" y="2" z="1"/>
                </element>
                <element type="point" label="E">
                  <coords x="1" y="2" z="1"/>
                </element>
                <command name="CircleArc">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="circleArc"/>
                </command>
                <element type="conicpart" label="circleArc"/>
                <command name="CircleSector">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="circleSector"/>
                </command>
                <element type="conicpart" label="circleSector"/>
                <command name="CircumcircleArc">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="circumcircleArc"/>
                </command>
                <element type="conicpart" label="circumcircleArc"/>
                <command name="CircumcircleSector">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="circumcircleSector"/>
                </command>
                <element type="conicpart" label="circumcircleSector"/>
                <command name="Semicircle">
                  <input a0="A" a1="B"/>
                  <output a0="semicircle"/>
                </command>
                <element type="conicpart" label="semicircle"/>
                <command name="Angle">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="angle"/>
                </command>
                <element type="angle" label="angle"/>
                <command name="AngularBisector">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="angleBisector"/>
                </command>
                <element type="line" label="angleBisector"/>
                <command name="Ellipse">
                  <input a0="A" a1="B" a2="C"/>
                  <output a0="pointEllipse"/>
                </command>
                <element type="conic" label="pointEllipse"/>
                <command name="Ellipse">
                  <input a0="A" a1="B" a2="3"/>
                  <output a0="axisEllipse"/>
                </command>
                <element type="conic" label="axisEllipse"/>
                <command name="Conic">
                  <input a0="A" a1="B" a2="C" a3="D" a4="E"/>
                  <output a0="fivePointConic"/>
                </command>
                <element type="conic" label="fivePointConic"/>
                <element type="conic" label="matrixConic">
                  <matrix
                      A0="4"
                      A1="9"
                      A2="-36"
                      A3="0"
                      A4="0"
                      A5="0"
                  />
                </element>
                <command name="Polar">
                  <input a0="pointEllipse" a1="E"/>
                  <output a0="polar"/>
                </command>
                <element type="line" label="polar"/>
                <expression label="f" exp="f(x) = x*x - 4"/>
                <element type="function" label="f"/>
                <command name="Root">
                  <input a0="f"/>
                  <output a0="root"/>
                </command>
                <element type="point" label="root">
                  <coords x="1.8" y="0" z="1"/>
                </element>
                <command name="Integral">
                  <input a0="f" a1="0" a2="2"/>
                  <output a0="integral"/>
                </command>
                <element type="numeric" label="integral"/>
                <element type="numeric" label="k">
                  <value val="1.5"/>
                  <slider
                      min="0"
                      max="5"
                      absoluteScreenLocation="false"
                      width="4"
                      x="-2"
                      y="-3"
                      horizontal="true"
                  />
                  <animation step="0.5"/>
                </element>
                <expression
                    label="dynamicText"
                    exp="&quot;k = &quot; + k"
                />
                <element type="text" label="dynamicText">
                  <coords x="0" y="-3" z="1"/>
                </element>
              </construction>
            </geogebra>
        """.trimIndent()
    }
}
