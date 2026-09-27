/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import com.swithun.jsxgraph.core.parser.PrefixParser
import com.swithun.jsxgraph.core.parser.PrefixParserError
import com.swithun.jsxgraph.core.parser.PrefixParserLimits
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MeasurementTest {
    @Test
    fun measuresCircleAndTracksDependencies() {
        val board = board()
        val center = point(board, 1.0, 1.0, "center")
        val radiusPoint = point(board, 1.0, 3.0, "radiusPoint")
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = center,
                point2 = radiusPoint,
                id = "circle",
                name = "",
            ),
        ).value
        val measurement = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-3.0, -2.0),
                term = term("Radius", element(circle)),
                attributes = MeasurementAttributes(
                    id = "radius",
                    name = "",
                    values = mapOf(
                        "prefix" to string("r="),
                        "baseunit" to string("cm"),
                        "digits" to number(3.0),
                    ),
                ),
            ),
        )
        val definition =
            measurement.measurementDefinition ?: error("missing definition")

        assertEquals(Const.OBJECT_TYPE_MEASUREMENT, measurement.type)
        assertEquals("measurement", measurement.elType)
        assertEquals("r=2.000cm", measurement.plaintext)
        assertEquals(2.0, numberValue(definition.Value()))
        assertEquals(1.0, numberValue(definition.Dimension()))
        assertEquals("cm", stringValue(definition.Unit()))
        assertEquals("Radius", method(definition))
        assertEquals(listOf(circle.id), measurement.parents)
        assertSame(measurement, circle.childElements[measurement.id])

        radiusPoint.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 5.0),
        )
        board.fullUpdate()

        assertEquals("r=4.000cm", measurement.plaintext)
        assertEquals(4.0, numberValue(definition.Value()))
    }

    @Test
    fun evaluatesArithmeticUnitsExecCoordinatesAndDirection() {
        val board = board()
        val first = point(board, 0.0, 1.0, "first")
        val second = point(board, 3.0, 1.0, "second")
        val circle = assertIs<GMResult.Ok<Circle>>(
            Circle.create(
                board = board,
                center = first,
                point2 = second,
                id = "circle",
                name = "",
            ),
        ).value
        val line = assertIs<GMResult.Ok<Line>>(
            Line.createSegment(
                board = board,
                point1 = first,
                point2 = second,
                id = "segment",
                name = "",
            ),
        ).value
        val area = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, -2.0),
                term = term("Area", element(circle)),
                attributes = MeasurementAttributes(
                    name = "",
                    values = mapOf(
                        "prefix" to string("A="),
                        "baseunit" to string(" cm"),
                        "digits" to string("none"),
                    ),
                ),
            ),
        )
        val sum = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, -3.0),
                term = term(
                    "+",
                    term("Radius", element(circle)),
                    term("L", element(line)),
                ),
                attributes = MeasurementAttributes(
                    name = "",
                    values = mapOf(
                        "prefix" to string("sum="),
                        "suffix" to string("!"),
                        "baseunit" to string("cm"),
                        "digits" to number(2.0),
                    ),
                ),
            ),
        )
        val sine = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, -4.0),
                term = term("exec", string("sin"), number(PI / 2.0)),
                attributes = MeasurementAttributes(
                    name = "",
                    values = mapOf(
                        "prefix" to string("sin="),
                        "digits" to string("auto"),
                    ),
                ),
            ),
        )
        val coordinates = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, -5.0),
                term = term("Coords", element(second)),
                attributes = MeasurementAttributes(
                    name = "",
                    values = mapOf(
                        "prefix" to string("P="),
                        "dim" to string("coords"),
                        "digits" to number(1.0),
                    ),
                ),
            ),
        )
        val direction = measurement(
            Measurement.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, -6.0),
                term = term("Direction", element(line)),
                attributes = MeasurementAttributes(
                    name = "",
                    values = mapOf(
                        "prefix" to string("d="),
                        "dim" to string("direction"),
                        "digits" to number(1.0),
                    ),
                ),
            ),
        )

        assertEquals("A=${9.0 * PI} cm<sup>2</sup>", area.plaintext)
        assertEquals("sum=6.00cm!", sum.plaintext)
        assertEquals("sin=1.00", sine.plaintext)
        assertEquals("P=(3.0, 1.0)", coordinates.plaintext)
        assertEquals("d=(3.0, 0.0)", direction.plaintext)
        val coordinateDefinition =
            coordinates.measurementDefinition ?: error("missing definition")
        val coordinateTerm = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            coordinateDefinition.getTerm(),
        ).values
        assertEquals(3, coordinateTerm.size)
        assertEquals(
            "true",
            assertIs<JessieCodeRuntimeValue.StringValue>(
                coordinateTerm[2],
            ).value,
        )
        val coordinatePrefix = assertIs<JessieCodeRuntimeValue.ArrayValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
                coordinateDefinition.toPrefix(),
            ).value,
        ).values
        assertEquals("Coords", string(coordinatePrefix[0]))
        assertSame(
            JessieCodeRuntimeValue.UndefinedValue,
            coordinatePrefix[1],
        )
        val coordinateParents = assertIs<
            GMResult.Ok<List<JessieCodeRuntimeValue>>,
            >(coordinateDefinition.getParents()).value
        assertSame(
            second,
            assertIs<JessieCodeRuntimeValue.ElementReference>(
                coordinateParents[0],
            ).element,
        )
        assertEquals("true", string(coordinateParents[1]))
        assertEquals(listOf(second.id), coordinates.parents)
        assertEquals(
            mapOf(
                "dim0" to "",
                "dim1" to "cm",
                "dim2" to "cm^{2}",
            ),
            objectStrings(
                sum.measurementDefinition?.Unit(
                    JessieCodeRuntimeValue.ArrayValue(
                        listOf(number(0.0), number(1.0), number(2.0)),
                    ),
                ) ?: error("missing definition"),
            ),
        )
    }

    @Test
    fun prefixFailuresAndLimitsAreStructured() {
        val board = board()
        val invalid = assertIs<
            GMResult.Err<PrefixParserError.InvalidTerm>,
            >(
            PrefixParser.execute(
                board = board,
                term = JessieCodeRuntimeValue.ArrayValue(
                    listOf(string("+")),
                ),
                location = SOURCE_LOCATION,
            ),
        )
        assertEquals("parse", invalid.error.operation)

        val forbidden = assertIs<
            GMResult.Err<PrefixParserError.FunctionNotAllowed>,
            >(
            PrefixParser.execute(
                board = board,
                term = term("exec", string("alert"), number(1.0)),
                location = SOURCE_LOCATION,
            ),
        )
        assertEquals("alert", forbidden.error.name)

        var deep: JessieCodeRuntimeValue = number(1.0)
        repeat(8) {
            deep = term("+", deep, number(1.0))
        }
        val limited = assertIs<
            GMResult.Err<PrefixParserError.DepthLimitExceeded>,
            >(
            PrefixParser.execute(
                board = board,
                term = deep,
                location = SOURCE_LOCATION,
                limits = PrefixParserLimits(maxDepth = 4),
            ),
        )
        assertEquals(4, limited.error.limit)
        assertTrue(board.objects.isEmpty())
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        id = "board",
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = "",
            ),
        ).value

    private fun measurement(
        result: GMResult<Text, MeasurementError>,
    ): Text = assertIs<GMResult.Ok<Text>>(result, result.toString()).value

    private fun method(definition: MeasurementDefinition): String =
        assertIs<GMResult.Ok<String>>(definition.getMethod()).value

    private fun numberValue(
        result: GMResult<JessieCodeRuntimeValue, MeasurementError>,
    ): Double =
        assertIs<JessieCodeRuntimeValue.NumberValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(result).value,
        ).value

    private fun stringValue(
        result: GMResult<JessieCodeRuntimeValue, MeasurementError>,
    ): String =
        assertIs<JessieCodeRuntimeValue.StringValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(result).value,
        ).value

    private fun objectStrings(
        result: GMResult<JessieCodeRuntimeValue, MeasurementError>,
    ): Map<String, String> =
        assertIs<JessieCodeRuntimeValue.ObjectValue>(
            assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(result).value,
        ).properties.mapValues { (_, value) ->
            assertIs<JessieCodeRuntimeValue.StringValue>(value).value
        }

    private fun term(
        operator: String,
        vararg operands: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue =
        JessieCodeRuntimeValue.ArrayValue(
            listOf(JessieCodeRuntimeValue.StringValue(operator)) +
                operands,
        )

    private fun element(
        value: GeometryElement,
    ): JessieCodeRuntimeValue.ElementReference =
        JessieCodeRuntimeValue.ElementReference(value)

    private fun number(value: Double): JessieCodeRuntimeValue.NumberValue =
        JessieCodeRuntimeValue.NumberValue(value)

    private fun string(value: String): JessieCodeRuntimeValue.StringValue =
        JessieCodeRuntimeValue.StringValue(value)

    private fun string(value: JessieCodeRuntimeValue): String =
        assertIs<JessieCodeRuntimeValue.StringValue>(value).value

    private companion object {
        val SOURCE_LOCATION = com.swithun.jsxgraph.core.parser
            .JessieCodeAstLocation(1, 1, 1, 1)
    }
}
