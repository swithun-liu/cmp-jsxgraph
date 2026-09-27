/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class IntegralTest {
    @Test
    fun xAxisIntegralCreatesOfficialHelperGraphAndSignedValue() {
        val board = board("basic")
        val source = functionGraph(board, "x * x - 2", "source")
        val integral = integral(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Fixed(-2.0) to
                    IntegralBoundary.Fixed(3.0),
                source = source,
                attributes = attributes("integral"),
            ),
        )
        val definition = integralDefinition(integral)

        assertTrue(integral.isIntegral)
        assertEquals("integral", integral.elType)
        assertEquals("plot", integral.curveType)
        assertFalse(integral.isDraggable)
        assertEquals(listOf(source.id), integral.parents)
        assertEquals(
            listOf(
                "integralCurveLeft",
                "integralBaseLeft",
                "integralCurveRight",
                "integralBaseRight",
                "integral",
                "integralLabel",
            ),
            board.objectsList.drop(1).map(GeometryElement::id),
        )
        assertSame(definition.curveLeft, integral.subs["curveLeft"])
        assertSame(definition.baseLeft, integral.subs["baseLeft"])
        assertSame(definition.curveRight, integral.subs["curveRight"])
        assertSame(definition.baseRight, integral.subs["baseRight"])
        assertSame(definition.label, integral.subs["label"])
        assertEquals(1.6666666666666683, integral.Value(), TOLERANCE)
        assertPoint(-2.0, 2.0, definition.curveLeft)
        assertPoint(-2.0, 0.0, definition.baseLeft)
        assertPoint(3.0, 7.0, definition.curveRight)
        assertPoint(3.0, 0.0, definition.baseRight)
        assertTrue(definition.curveLeft.isDraggable)
        assertTrue(definition.curveRight.isDraggable)
        assertFalse(definition.curveLeft.dump)
        assertFalse(definition.baseLeft.dump)
        assertFalse(definition.curveRight.dump)
        assertFalse(definition.baseRight.dump)

        val x = integral.dataX ?: error("missing x data")
        val y = integral.dataY ?: error("missing y data")
        assertEquals(-2.0, x.first())
        assertEquals(0.0, y.first())
        assertEquals(-2.0, x[1])
        assertEquals(2.0, y[1])
        assertEquals(3.0, x[x.lastIndex - 1])
        assertEquals(0.0, y[y.lastIndex - 1])
        assertEquals(-2.0, x.last())
        assertEquals(0.0, y.last())

        val label = definition.label ?: error("missing label")
        assertEquals("\u222b = 1.6667", label.plaintext)
        assertPoint(3.0, 4.8, label)
        assertContentEquals(doubleArrayOf(12.0, -8.0), label.screenOffset)
        assertFalse(label.dump)
        assertSame(label, definition.curveLeft.childElements[label.id])
        assertSame(label, definition.curveRight.childElements[label.id])
    }

    @Test
    fun dynamicBoundsTrackTermsWhileFixedBoundsRemainDraggable() {
        val board = board("dynamic")
        val source = functionGraph(board, "x * cos(x)", "source")
        val start = MutableBoundary(-2.0)
        val end = MutableBoundary(2.0)
        val integral = integral(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Dynamic(start) to
                    IntegralBoundary.Dynamic(end),
                source = source,
                attributes = attributes("dynamic"),
            ),
        )
        val definition = integralDefinition(integral)

        assertFalse(definition.curveLeft.isDraggable)
        assertFalse(definition.curveRight.isDraggable)
        assertPoint(-2.0, -2.0 * kotlin.math.cos(-2.0), definition.curveLeft)
        assertPoint(2.0, 2.0 * kotlin.math.cos(2.0), definition.curveRight)
        assertEquals(0.0, integral.Value(), TOLERANCE)

        start.value = -1.0
        end.value = 3.0
        board.fullUpdate()

        assertPoint(-1.0, -kotlin.math.cos(-1.0), definition.curveLeft)
        assertPoint(3.0, 3.0 * kotlin.math.cos(3.0), definition.curveRight)
        assertPoint(-1.0, 0.0, definition.baseLeft)
        assertPoint(3.0, 0.0, definition.baseRight)
        assertEquals(-1.9484057630968799, integral.Value(), TOLERANCE)
        assertEquals(
            "\u222b = -1.9484",
            definition.label?.plaintext,
        )

        val fixed = integral(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Fixed(3.0) to
                    IntegralBoundary.Fixed(-2.0),
                source = source,
                attributes = attributes(
                    id = "reverse",
                    withLabel = false,
                ),
            ),
        )
        val fixedDefinition = integralDefinition(fixed)
        assertTrue(fixedDefinition.curveLeft.isDraggable)
        assertTrue(fixedDefinition.curveRight.isDraggable)
        assertEquals(
            -integralBetween(source, -2.0, 3.0),
            fixed.Value(),
            TOLERANCE,
        )
    }

    @Test
    fun yAxisUsesCurveCoordinatesAndSuppressesLabel() {
        val board = board("y-axis")
        val source = curve(
            Curve.createParametric(
                board = board,
                xSource = "x * x - 1",
                ySource = "x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 256,
                id = "source",
                name = "",
            ),
        )
        val integral = integral(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Fixed(-1.5) to
                    IntegralBoundary.Fixed(1.5),
                source = source,
                attributes = attributes(
                    id = "yIntegral",
                    axis = "y",
                    withLabel = true,
                ),
            ),
        )
        val definition = integralDefinition(integral)

        assertEquals("y", definition.axis)
        assertEquals(0.0, definition.baseLeft.X())
        assertEquals(definition.curveLeft.Y(), definition.baseLeft.Y())
        assertEquals(0.0, definition.baseRight.X())
        assertEquals(definition.curveRight.Y(), definition.baseRight.Y())
        assertEquals(0.0, integral.Value())
        assertEquals(null, definition.label)
        assertTrue("label" !in integral.subs)
        assertEquals(0.0, integral.dataX?.first())
        assertEquals(0.0, integral.dataX?.last())
    }

    @Test
    fun creationFailuresAreAtomicAndRemovalLeavesOfficialHelpers() {
        val board = board("lifecycle")
        val source = functionGraph(board, "x * x", "source")
        point(board, 4.0, 4.0, "collision")
        val beforeDuplicate = board.objects.keys.toList()

        assertIs<GMResult.Err<IntegralError.DuplicateElementId>>(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Fixed(-1.0) to
                    IntegralBoundary.Fixed(2.0),
                source = source,
                attributes = attributes("candidate").copy(
                    baseRight = IntegralElementAttributes(
                        id = "collision",
                        name = "",
                    ),
                ),
            ),
        )
        assertEquals(beforeDuplicate, board.objects.keys.toList())

        val integral = integral(
            Integral.create(
                board = board,
                interval = IntegralBoundary.Fixed(-1.0) to
                    IntegralBoundary.Fixed(2.0),
                source = source,
                attributes = attributes("removable"),
            ),
        )
        val helpers = integral.inherits.toList()
        board.removeObject(integral)

        assertTrue(integral.id !in board.objects)
        assertTrue(helpers.all { helper -> board.elementById(helper.id) === helper })
        assertTrue(source.childElements.values.none { it === integral })
    }

    private fun attributes(
        id: String,
        axis: String = "x",
        withLabel: Boolean = true,
    ): IntegralAttributes = IntegralAttributes(
        id = id,
        name = "",
        axis = axis,
        withLabel = withLabel,
        curveLeft = IntegralElementAttributes(
            id = "${id}CurveLeft",
            name = "",
        ),
        baseLeft = IntegralElementAttributes(
            id = "${id}BaseLeft",
            name = "",
        ),
        curveRight = IntegralElementAttributes(
            id = "${id}CurveRight",
            name = "",
        ),
        baseRight = IntegralElementAttributes(
            id = "${id}BaseRight",
            name = "",
        ),
        label = IntegralLabelAttributes(
            id = "${id}Label",
            name = "",
            offset = doubleArrayOf(12.0, -8.0),
        ),
    )

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
        boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
    )

    private fun functionGraph(
        board: Board,
        source: String,
        id: String,
    ): Curve = curve(
        Curve.createFunctionGraph(
            board = board,
            ySource = source,
            minimumSource = "-5",
            maximumSource = "5",
            sampleCount = 256,
            id = id,
            name = "",
        ),
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point = assertIs<GMResult.Ok<Point>>(
        Point.create(
            board = board,
            coordinates = doubleArrayOf(x, y),
            id = id,
            name = "",
        ),
    ).value

    private fun integralBetween(
        curve: Curve,
        start: Double,
        end: Double,
    ): Double = when (
        val result = com.swithun.jsxgraph.core.math.Numerics.I(
            doubleArrayOf(start, end),
            curve::Y,
        )
    ) {
        is GMResult.Ok -> result.value
        is GMResult.Err -> Double.NaN
    }

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun integral(
        result: GMResult<Curve, IntegralError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun integralDefinition(
        curve: Curve,
    ): CurveIntegralDefinition =
        curve.integralDefinition ?: error("missing integral definition")

    private fun assertPoint(
        x: Double,
        y: Double,
        point: CoordsElement,
    ) {
        assertEquals(x, point.X(), TOLERANCE)
        assertEquals(y, point.Y(), TOLERANCE)
    }

    private class MutableBoundary(
        var value: Double,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }

    private companion object {
        const val TOLERANCE = 1.0e-6
    }
}
