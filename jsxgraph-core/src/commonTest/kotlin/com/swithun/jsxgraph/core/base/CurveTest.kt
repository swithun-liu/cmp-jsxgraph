/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CurveTest {
    @Test
    fun dataCurveUsesTheUpstreamXArrayLengthAndMissingYBecomesNaN() {
        val board = board()

        val curve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-2.0, 0.0, 3.0),
                dataY = doubleArrayOf(4.0, 1.0),
            ),
        )

        assertEquals("boardG0", curve.id)
        assertEquals("s_{a}", curve.name)
        assertEquals("curve", curve.elType)
        assertEquals("plot", curve.curveType)
        assertEquals(Const.OBJECT_TYPE_CURVE, curve.type)
        assertEquals(Const.OBJECT_CLASS_CURVE, curve.elementClass)
        assertEquals(1, curve.bezierDegree)
        assertEquals(3, curve.numberPoints)
        assertEquals(3, curve.points.size)
        assertContentEquals(
            doubleArrayOf(1.0, -2.0, 4.0),
            curve.points[0].usrCoords,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 1.0),
            curve.points[1].usrCoords,
        )
        assertTrue(curve.points[2].usrCoords[2].isNaN())
        assertSame(curve, board.select(curve.id))
        assertSame(curve, board.select(curve.name))
    }

    @Test
    fun stepfunctionExpandsAndRetainsItsSourceTerms() {
        val board = board()
        val xValues = mutableListOf(0.0, 1.0, 3.0, 4.0)
        val yValues = mutableListOf(2.0, -1.0, 3.0, 1.0)
        val curve = curve(
            Curve.createStepfunction(
                board = board,
                xTerm = MutableStepTerm(xValues),
                yTerm = MutableStepTerm(yValues),
                name = "",
            ),
        )

        assertEquals("curve", curve.elType)
        assertEquals("plot", curve.curveType)
        assertTrue(curve.isStepFunction)
        assertContentEquals(
            doubleArrayOf(0.0, 1.0, 1.0, 3.0, 3.0, 4.0, 4.0),
            curve.dataX,
        )
        assertContentEquals(
            doubleArrayOf(2.0, 2.0, -1.0, -1.0, 3.0, 3.0, 1.0),
            curve.dataY,
        )

        xValues.clear()
        xValues.addAll(listOf(-2.0, 0.0, 3.0))
        yValues.clear()
        yValues.addAll(listOf(1.0, 4.0, -2.0))
        board.update()

        assertContentEquals(
            doubleArrayOf(-2.0, 0.0, 0.0, 3.0, 3.0),
            curve.dataX,
        )
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 4.0, 4.0, -2.0),
            curve.dataY,
        )
        assertEquals(5, curve.numberPoints)
    }

    @Test
    fun stepfunctionMatchesMismatchedEmptyAndFunctionParentBehavior() {
        val mismatched = curve(
            Curve.createStepfunction(
                board = board(),
                xTerm = MutableStepTerm(
                    mutableListOf(0.0, 2.0, 5.0),
                ),
                yTerm = MutableStepTerm(mutableListOf(4.0)),
                name = "",
            ),
        )
        assertContentEquals(
            doubleArrayOf(0.0, 2.0, 2.0, 5.0, 5.0),
            mismatched.dataX,
        )
        assertEquals(4.0, mismatched.dataY?.get(0))
        assertEquals(4.0, mismatched.dataY?.get(1))
        assertTrue(mismatched.dataY?.drop(2)?.all(Double::isNaN) == true)

        val empty = curve(
            Curve.createStepfunction(
                board = board(),
                xTerm = MutableStepTerm(mutableListOf()),
                yTerm = MutableStepTerm(mutableListOf()),
                name = "",
            ),
        )
        assertEquals(0, empty.numberPoints)
        assertTrue(empty.dataX?.isEmpty() == true)
        assertTrue(empty.dataY?.isEmpty() == true)

        val functionParent = curve(
            Curve.createStepfunction(
                board = board(),
                xTerm = FunctionStepTerm(length = 0),
                yTerm = FunctionStepTerm(length = 0),
                name = "",
            ),
        )
        assertEquals("parameter", functionParent.curveType)
        assertEquals(0, functionParent.numberPoints)
        assertTrue(functionParent.parents.isEmpty())
    }

    @Test
    fun stepfunctionRejectsOversizedExpandedDataAtomically() {
        val board = board()
        val sourceCount = Curve.MAX_SAMPLE_COUNT / 2 + 1

        val error = assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
            Curve.createStepfunction(
                board = board,
                xTerm = MutableStepTerm(
                    MutableList(sourceCount) { it.toDouble() },
                ),
                yTerm = MutableStepTerm(
                    MutableList(sourceCount) { it.toDouble() },
                ),
            ),
        ).error

        assertEquals(sourceCount * 2 - 1, error.count)
        assertEquals(Curve.MAX_SAMPLE_COUNT, error.maximum)
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun naiveFunctionGraphSamplingMatchesOfficialRightOpenDomain() {
        val curve = curve(
            Curve.createFunctionGraph(
                board = board(),
                ySource = "x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 4,
                name = "",
            ),
        )

        assertEquals("functiongraph", curve.curveType)
        assertEquals(4, curve.numberPoints)
        assertEquals(-2.0, curve.minX())
        assertEquals(2.0, curve.maxX())
        assertEquals(-1.5, curve.X(-1.5))
        assertEquals(2.25, curve.Y(-1.5))
        assertContentEquals(
            doubleArrayOf(-2.0, -1.0, 0.0, 1.0),
            curve.points.map { it.usrCoords[1] }.toDoubleArray(),
        )
        assertContentEquals(
            doubleArrayOf(4.0, 1.0, 0.0, 1.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun parametricCurveExpressionsTrackBoardDependencies() {
        val board = board()
        val driver = point(
            board = board,
            coordinates = doubleArrayOf(2.0, 1.0),
            name = "A",
        )
        val curve = curve(
            Curve.createParametric(
                board = board,
                xSource = "x",
                ySource = "A.X() * x",
                minimumSource = "0",
                maximumSource = "2",
                sampleCount = 2,
                name = "",
            ),
        )

        assertSame(curve, driver.childElements[curve.id])
        assertContentEquals(
            doubleArrayOf(0.0, 2.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, 1.0),
        )
        board.update()

        assertContentEquals(
            doubleArrayOf(0.0, 3.0),
            curve.points.map { it.usrCoords[2] }.toDoubleArray(),
        )
    }

    @Test
    fun dataCurveInterpolationAndDefaultDomainMatchOfficialBehavior() {
        val curve = curve(
            Curve.createData(
                board = Board(
                    originX = 0.0,
                    originY = 0.0,
                    unitX = 1.0,
                    unitY = 1.0,
                    defaultCurveMinimum = -9.6,
                    defaultCurveMaximum = 9.6,
                ),
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 5.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 3.0),
                name = "",
            ),
        )

        assertEquals(-9.6, curve.minX())
        assertEquals(9.6, curve.maxX())
        assertEquals(-4.0, curve.X(-1.0))
        assertEquals(-4.0, curve.X(0.0))
        assertEquals(-1.0, curve.X(1.0))
        assertEquals(0.5, curve.X(1.5))
        assertEquals(8.0, curve.X(4.0))
        assertEquals(-2.0, curve.Y(-1.0))
        assertEquals(0.5, curve.Y(1.5))
    }

    @Test
    fun derivativeTracksFunctionGraphAndKeepsMetadataOnlyParent() {
        val board = board()
        val coefficient = point(
            board = board,
            coordinates = doubleArrayOf(2.0, 0.0),
            name = "A",
        )
        val source = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "A.X() * x * x + 3 * x - 1",
                minimumSource = "A.Y() - 3",
                maximumSource = "4",
                sampleCount = 8,
                id = "source",
                name = "",
            ),
        )
        val derivative = curve(
            Curve.createDerivative(
                board = board,
                source = source,
                sampleCount = 8,
                id = "derivative",
                name = "",
            ),
        )

        assertTrue(derivative.isDerivative)
        assertEquals("curve", derivative.elType)
        assertEquals("parameter", derivative.curveType)
        assertEquals(listOf("source"), derivative.parents)
        assertFalse(source.childElements.containsKey(derivative.id))
        assertEquals(-3.0, derivative.minX())
        assertEquals(4.0, derivative.maxX())
        assertEquals(-2.0, derivative.X(-2.0))
        assertEquals(
            -5.0,
            derivative.Y(-2.0),
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            3.0,
            derivative.Y(0.0),
            absoluteTolerance = 1.0e-8,
        )
        assertContentEquals(
            doubleArrayOf(
                -3.0,
                -2.125,
                -1.25,
                -0.375,
                0.5,
                1.375,
                2.25,
                3.125,
            ),
            derivative.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        coefficient.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(4.0, 1.0),
        )
        board.update()

        assertEquals(-2.0, source.minX())
        assertEquals(-3.0, derivative.minX())
        assertEquals(
            -13.0,
            derivative.Y(-2.0),
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            19.0,
            derivative.Y(2.0),
            absoluteTolerance = 1.0e-8,
        )

        board.removeObject(source)
        assertSame(derivative, board.select("derivative"))
        assertEquals(listOf("source"), derivative.parents)
    }

    @Test
    fun derivativeSupportsParametricAndDataCurves() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            defaultCurveMinimum = -9.6,
            defaultCurveMaximum = 9.6,
        )
        val parametric = curve(
            Curve.createParametric(
                board = board,
                xSource = "2 * cos(x)",
                ySource = "3 * sin(x)",
                minimumSource = "0",
                maximumSource = "2 * PI",
                sampleCount = 8,
                name = "",
            ),
        )
        val parametricDerivative = curve(
            Curve.createDerivative(
                board = board,
                source = parametric,
                sampleCount = 8,
                name = "",
            ),
        )
        assertTrue(parametricDerivative.Y(0.0).isInfinite())
        assertEquals(
            -1.5,
            parametricDerivative.Y(kotlin.math.PI / 4.0),
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            0.0,
            parametricDerivative.Y(kotlin.math.PI / 2.0),
            absoluteTolerance = 1.0e-8,
        )

        val data = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 5.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 3.0),
                name = "",
            ),
        )
        val dataDerivative = curve(
            Curve.createDerivative(
                board = board,
                source = data,
                sampleCount = 8,
                name = "",
            ),
        )
        assertEquals(-9.6, dataDerivative.minX())
        assertEquals(9.6, dataDerivative.maxX())
        assertTrue(dataDerivative.Y(-1.0).isNaN())
        assertEquals(
            4.0 / 3.0,
            dataDerivative.Y(0.0),
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            1.0 / 6.0,
            dataDerivative.Y(1.0),
            absoluteTolerance = 1.0e-8,
        )
    }

    @Test
    fun splineSortsPointKnotsAndTracksMovedPoints() {
        val board = board()
        val a = point(board, doubleArrayOf(2.0, 0.0), "A")
        val b = point(board, doubleArrayOf(-2.0, 2.0), "B")
        val c = point(board, doubleArrayOf(0.0, -1.0), "C")
        val d = point(board, doubleArrayOf(4.0, 1.0), "D")
        val spline = curve(
            Curve.createSpline(
                board = board,
                points = listOf(a, b, c, d).map(
                    ::CurveElementSplinePoint,
                ),
                sampleCount = 8,
                id = "spline",
                name = "",
            ),
        )

        assertTrue(spline.isSpline)
        assertEquals("spline", spline.elType)
        assertEquals("functiongraph", spline.curveType)
        assertEquals(listOf(a.id, b.id, c.id, d.id), spline.parents)
        assertTrue(
            listOf(a, b, c, d).all {
                spline.id !in it.childElements
            },
        )
        assertEquals(-2.0, spline.minX())
        assertEquals(4.0, spline.maxX())
        assertEquals(0.1, spline.Y(-1.0), absoluteTolerance = 1.0e-14)
        assertEquals(-0.8, spline.Y(1.0), absoluteTolerance = 1.0e-14)
        assertContentEquals(
            doubleArrayOf(
                -2.0,
                -1.25,
                -0.5,
                0.25,
                1.0,
                1.75,
                2.5,
                3.25,
            ),
            spline.points.map { it.usrCoords[1] }.toDoubleArray(),
        )

        a.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, -2.0),
        )
        board.update()

        assertEquals(
            0.5140845070422535,
            spline.Y(-1.0),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            -2.4225352112676055,
            spline.Y(1.0),
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun cardinalSplineTracksPointsAndDynamicTension() {
        val board = board()
        val p0 = point(board, doubleArrayOf(-4.0, 0.0), "P0")
        val p1 = point(board, doubleArrayOf(-2.0, 3.0), "P1")
        val p2 = point(board, doubleArrayOf(1.0, -2.0), "P2")
        val p3 = point(board, doubleArrayOf(4.0, 2.0), "P3")
        val tension = MutableNumericCoordinateFunction(0.35)
        val spline = curve(
            Curve.createCardinalSpline(
                board = board,
                points = listOf(p0, p1, p2, p3),
                tensionTerm = tension,
                type = "uniform",
                sampleCount = 8,
                id = "cardinal",
                name = "",
            ),
        )

        assertTrue(spline.isCardinalSpline)
        assertEquals("cardinalspline", spline.elType)
        assertEquals("parameter", spline.curveType)
        assertEquals(listOf(p0.id, p1.id, p2.id, p3.id), spline.parents)
        assertTrue(
            listOf(p0, p1, p2, p3).all {
                it.childElements[spline.id] === spline
            },
        )
        assertEquals(-3.04375, spline.X(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(1.85, spline.Y(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(-0.54375, spline.X(1.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            0.45625000000000027,
            spline.Y(1.5),
            absoluteTolerance = 1.0e-14,
        )

        tension.value = 0.8
        p2.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(2.0, -3.0),
        )
        board.update()

        assertEquals(-3.2, spline.X(0.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            2.4000000000000004,
            spline.Y(0.5),
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(0.0, spline.X(1.5), absoluteTolerance = 1.0e-14)
        assertEquals(
            -0.20000000000000018,
            spline.Y(1.5),
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun riemannSumTracksDynamicTermsAndExposesItsValue() {
        val board = board()
        val rectangleCount = MutableNumericCoordinateFunction(3.8)
        val approximationType = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.StringValue("left")
        }
        val minimum = MutableNumericCoordinateFunction(-1.0)
        val maximum = MutableNumericCoordinateFunction(2.0)
        val upperFunction = MutableRuntimeCoordinateFunction { arguments ->
            val x = (
                arguments.single() as
                    JessieCodeRuntimeValue.NumberValue
                ).value
            JessieCodeRuntimeValue.NumberValue(x * x + 1.0)
        }
        val riemann = curve(
            Curve.createRiemannSum(
                board = board,
                upperFunction = upperFunction,
                lowerFunction = null,
                rectangleCount = rectangleCount,
                approximationType = approximationType,
                minimum = minimum,
                maximum = maximum,
                id = "riemann",
                name = "",
            ),
        )

        assertTrue(riemann.isRiemannSum)
        assertEquals("curve", riemann.elType)
        assertEquals("plot", riemann.curveType)
        assertEquals(15, riemann.numberPoints)
        assertEquals(5.0, riemann.Value())
        assertEquals(-1.0, riemann.minX())
        assertEquals(2.0, riemann.maxX())
        assertTrue(riemann.parents.isEmpty())

        rectangleCount.value = 5.2
        approximationType.value =
            JessieCodeRuntimeValue.StringValue("right")
        minimum.value = 0.0
        maximum.value = 3.0
        board.fullUpdate()

        assertEquals(25, riemann.numberPoints)
        assertEquals(14.879999999999999, riemann.Value())
        assertEquals(0.0, riemann.minX())
        assertEquals(3.0, riemann.maxX())
    }

    @Test
    fun riemannSumBoundsSimpsonGeometryBeforeRegistration() {
        val board = board()
        val upper = MutableRuntimeCoordinateFunction { arguments ->
            val x = (
                arguments.single() as
                    JessieCodeRuntimeValue.NumberValue
                ).value
            JessieCodeRuntimeValue.NumberValue(x * x + 1.0)
        }
        val lower = MutableRuntimeCoordinateFunction { arguments ->
            val x = (
                arguments.single() as
                    JessieCodeRuntimeValue.NumberValue
                ).value
            JessieCodeRuntimeValue.NumberValue(x * 0.5)
        }

        val error = assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
            Curve.createRiemannSum(
                board = board,
                upperFunction = upper,
                lowerFunction = lower,
                rectangleCount = MutableNumericCoordinateFunction(159.0),
                approximationType = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.StringValue("simpson")
                },
                minimum = MutableNumericCoordinateFunction(-1.0),
                maximum = MutableNumericCoordinateFunction(2.0),
            ),
        ).error

        assertEquals(10_017, error.count)
        assertEquals(Curve.MAX_SAMPLE_COUNT, error.maximum)
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun boxPlotMatchesOfficialGeometryAndUpdatesDynamicTerms() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 80.0,
            unitY = 60.0,
            id = "boxplot",
        )
        val quantiles = listOf(
            MutableNumericCoordinateFunction(-2.0),
            MutableNumericCoordinateFunction(-1.0),
            MutableNumericCoordinateFunction(0.0),
            MutableNumericCoordinateFunction(1.0),
            MutableNumericCoordinateFunction(2.0),
        )
        val outliers = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.ArrayValue(
                listOf(JessieCodeRuntimeValue.NumberValue(3.0)),
            )
        }
        val axis = MutableNumericCoordinateFunction(0.0)
        val width = MutableNumericCoordinateFunction(2.0)
        val boxPlot = curve(
            Curve.createBoxPlot(
                board = board,
                quantileTerms = quantiles + outliers,
                axisTerm = axis,
                widthTerm = width,
                outlierFace = "square",
                outlierSize = 6.0,
                id = "box",
                name = "",
            ),
        )

        assertTrue(boxPlot.isBoxPlot)
        assertEquals("curve", boxPlot.elType)
        assertEquals("plot", boxPlot.curveType)
        assertEquals(26, boxPlot.numberPoints)
        assertContentEquals(
            doubleArrayOf(
                0.0,
                -0.5,
                0.5,
                0.0,
                0.0,
                -1.0,
                -1.0,
                1.0,
                1.0,
                0.0,
                Double.NaN,
                -1.0,
                1.0,
                Double.NaN,
                0.0,
                0.0,
                -0.5,
                0.5,
                0.0,
                Double.NaN,
                -0.075,
                0.075,
                0.075,
                -0.075,
                -0.075,
                Double.NaN,
            ),
            boxPlot.dataX,
        )
        assertEquals(3.1, boxPlot.dataY?.get(20))
        assertEquals(2.9, boxPlot.dataY?.get(22))

        quantiles[0].value = -4.0
        quantiles[4].value = 5.0
        axis.value = 2.0
        width.value = 4.0
        board.fullUpdate()

        val snapshot = assertIs<CurveBoxPlotSnapshot>(
            boxPlot.boxPlotSnapshot(),
        )
        assertEquals(-4.0, snapshot.quantiles[0])
        assertEquals(5.0, snapshot.quantiles[4])
        assertEquals(2.0, snapshot.axis)
        assertEquals(4.0, snapshot.width)
        assertEquals(-4.0, boxPlot.dataY?.first())
        assertEquals(0.0, boxPlot.dataX?.get(5))
        assertEquals(4.0, boxPlot.dataX?.get(7))
    }

    @Test
    fun boxPlotRejectsInvalidOrExcessiveOutliersAtomically() {
        val invalidBoard = board()
        val invalidOutliers = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.ArrayValue(
                listOf(JessieCodeRuntimeValue.StringValue("bad")),
            )
        }
        val invalid = Curve.createBoxPlot(
            board = invalidBoard,
            quantileTerms =
                listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
                    .map(::MutableNumericCoordinateFunction) +
                    invalidOutliers,
            axisTerm = MutableNumericCoordinateFunction(0.0),
            widthTerm = MutableNumericCoordinateFunction(2.0),
        )
        val nonNumeric = assertIs<
            GMResult.Err<CurveError.NonNumericExpression>,
            >(invalid).error
        assertEquals("boxplot.Q[5][0]", nonNumeric.term)
        assertTrue(invalidBoard.objects.isEmpty())

        val excessiveBoard = board()
        val excessiveOutliers = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.ArrayValue(
                List(526) {
                    JessieCodeRuntimeValue.NumberValue(it.toDouble())
                },
            )
        }
        val excessive = Curve.createBoxPlot(
            board = excessiveBoard,
            quantileTerms =
                listOf(-2.0, -1.0, 0.0, 1.0, 2.0)
                    .map(::MutableNumericCoordinateFunction) +
                    excessiveOutliers,
            axisTerm = MutableNumericCoordinateFunction(0.0),
            widthTerm = MutableNumericCoordinateFunction(2.0),
        )
        val limit = assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
            excessive,
        ).error
        assertEquals(10_014, limit.count)
        assertEquals(Curve.MAX_SAMPLE_COUNT, limit.maximum)
        assertTrue(excessiveBoard.objects.isEmpty())
    }

    @Test
    fun combGeometryAndDynamicTermsMatchOfficialBehavior() {
        val board = board()
        val first = point(board, doubleArrayOf(-3.0, -1.0), "A")
        val second = point(board, doubleArrayOf(3.0, -1.0), "B")
        val frequency = MutableNumericCoordinateFunction(2.0)
        val width = MutableNumericCoordinateFunction(1.5)
        val angle = MutableNumericCoordinateFunction(kotlin.math.PI / 2.0)
        val reverse = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(false)
        }
        val comb = curve(
            Curve.createComb(
                board = board,
                point1 = first,
                point2 = second,
                frequencyTerm = frequency,
                widthTerm = width,
                angleTerm = angle,
                reverseTerm = reverse,
                id = "comb",
                name = "",
            ),
        )

        assertTrue(comb.isComb)
        assertEquals("curve", comb.elType)
        assertEquals("plot", comb.curveType)
        assertEquals(9, comb.numberPoints)
        assertTrue(comb.parents.isEmpty())
        assertTrue(comb.childElements.isEmpty())
        assertTrue(first.childElements.isEmpty())
        assertTrue(second.childElements.isEmpty())
        assertCombCoordinates(
            curve = comb,
            expected = listOf(
                -3.0 to -1.0,
                -3.0 to 0.5,
                null,
                -1.0 to -1.0,
                -0.9999999999999999 to 0.5,
                null,
                1.0 to -1.0,
                1.0 to 0.5,
                null,
            ),
        )

        second.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, 3.0),
        )
        frequency.value = 1.5
        width.value = 0.75
        angle.value = kotlin.math.PI / 3.0
        reverse.value = JessieCodeRuntimeValue.BooleanValue(true)
        board.update()

        assertEquals(12, comb.numberPoints)
        assertCombCoordinates(
            curve = comb,
            expected = listOf(
                1.0 to 3.0,
                0.16348369626219206 to 3.224143868042013,
                null,
                -0.06066017177982119 to 1.9393398282201788,
                -0.8971764755176291 to 2.163483696262192,
                null,
                -1.1213203435596424 to 0.8786796564403576,
                -1.9578366472974502 to 1.1028235244823708,
                null,
                -2.181980515339464 to -0.1819805153394638,
                -3.0184968190772716 to 0.04216335270254945,
                null,
            ),
        )
    }

    @Test
    fun combHandlesZeroLengthAndRejectsUnsafeFrequencyAtomically() {
        val zeroBoard = board()
        val point = point(zeroBoard, doubleArrayOf(1.0, 2.0), "A")
        val zero = curve(
            Curve.createComb(
                board = zeroBoard,
                point1 = point,
                point2 = point,
                frequencyTerm = MutableNumericCoordinateFunction(
                    Curve.COMB_DEFAULT_FREQUENCY,
                ),
                widthTerm = MutableNumericCoordinateFunction(
                    Curve.COMB_DEFAULT_WIDTH,
                ),
                angleTerm = MutableNumericCoordinateFunction(
                    Curve.COMB_DEFAULT_ANGLE,
                ),
                reverseTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(false)
                },
                id = "zero",
                name = "",
            ),
        )
        assertEquals(0, zero.numberPoints)
        assertTrue(zero.dataX?.isEmpty() == true)
        assertTrue(zero.dataY?.isEmpty() == true)

        val invalidBoard = board()
        val invalidFirst = point(
            invalidBoard,
            doubleArrayOf(0.0, 0.0),
            "A",
        )
        val invalidSecond = point(
            invalidBoard,
            doubleArrayOf(2.0, 0.0),
            "B",
        )
        val invalidFrequency = assertIs<
            GMResult.Err<CurveError.InvalidCombFrequency>,
            >(
            Curve.createComb(
                board = invalidBoard,
                point1 = invalidFirst,
                point2 = invalidSecond,
                frequencyTerm = MutableNumericCoordinateFunction(0.0),
                widthTerm = MutableNumericCoordinateFunction(1.0),
                angleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI / 2.0,
                ),
                reverseTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(false)
                },
            ),
        ).error
        assertEquals(0.0, invalidFrequency.frequency)
        assertEquals(
            setOf(invalidFirst.id, invalidSecond.id),
            invalidBoard.objects.keys,
        )

        val excessiveBoard = board()
        val excessiveFirst = point(
            excessiveBoard,
            doubleArrayOf(0.0, 0.0),
            "A",
        )
        val excessiveSecond = point(
            excessiveBoard,
            doubleArrayOf(3334.0, 0.0),
            "B",
        )
        val excessive = assertIs<
            GMResult.Err<CurveError.InvalidSampleCount>,
            >(
            Curve.createComb(
                board = excessiveBoard,
                point1 = excessiveFirst,
                point2 = excessiveSecond,
                frequencyTerm = MutableNumericCoordinateFunction(1.0),
                widthTerm = MutableNumericCoordinateFunction(1.0),
                angleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI / 2.0,
                ),
                reverseTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(false)
                },
            ),
        ).error
        assertEquals(10_002, excessive.count)
        assertEquals(Curve.MAX_SAMPLE_COUNT, excessive.maximum)
        assertEquals(
            setOf(excessiveFirst.id, excessiveSecond.id),
            excessiveBoard.objects.keys,
        )
    }

    @Test
    fun splineCreatorsRejectTooFewPointsAtomically() {
        val board = board()
        val point = point(board, doubleArrayOf(0.0, 0.0), "A")

        assertIs<CurveError.InvalidInterpolationPointCount>(
            assertIs<GMResult.Err<CurveError>>(
                Curve.createSpline(
                    board = board,
                    points = listOf(CurveElementSplinePoint(point)),
                    sampleCount = 8,
                ),
            ).error,
        )
        assertIs<CurveError.InvalidInterpolationPointCount>(
            assertIs<GMResult.Err<CurveError>>(
                Curve.createCardinalSpline(
                    board = board,
                    points = listOf(point),
                    tensionTerm = MutableNumericCoordinateFunction(0.5),
                    sampleCount = 8,
                ),
            ).error,
        )
        assertEquals(setOf(point.id), board.objects.keys)
    }

    @Test
    fun curveCreationFailuresRemainExplicitAndAtomic() {
        val board = board()

        assertEquals(
            CurveError.InvalidSampleCount(
                count = 0,
                maximum = Curve.MAX_SAMPLE_COUNT,
            ),
            assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "x",
                    minimumSource = "0",
                    maximumSource = "1",
                    sampleCount = 0,
                ),
            ).error,
        )
        val compileError = assertIs<
            GMResult.Err<CurveError.ExpressionCompile>
            >(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x +",
                minimumSource = "0",
                maximumSource = "1",
                sampleCount = 4,
            ),
        ).error
        assertEquals("yterm", compileError.term)
        assertIs<JessieCodeExpressionCompileError.Parser>(compileError.error)
        assertIs<CurveError.NonNumericExpression>(
            assertIs<GMResult.Err<CurveError.NonNumericExpression>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "\"not a number\"",
                    minimumSource = "0",
                    maximumSource = "1",
                    sampleCount = 4,
                ),
            ).error,
        )
        assertIs<CurveError.InvalidDomain>(
            assertIs<GMResult.Err<CurveError.InvalidDomain>>(
                Curve.createFunctionGraph(
                    board = board,
                    ySource = "x",
                    minimumSource = "2",
                    maximumSource = "-2",
                    sampleCount = 4,
                ),
            ).error,
        )
        assertEquals(0, board.numObjects)
        assertTrue(board.objects.isEmpty())

        val source = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x",
                minimumSource = "0",
                maximumSource = "1",
                sampleCount = 4,
                id = "source",
            ),
        )
        assertEquals(
            CurveError.InvalidSampleCount(
                count = 0,
                maximum = Curve.MAX_SAMPLE_COUNT,
            ),
            assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
                Curve.createDerivative(
                    board = board,
                    source = source,
                    sampleCount = 0,
                ),
            ).error,
        )

        assertEquals(1, board.numObjects)
        assertEquals(setOf("source"), board.objects.keys)
    }

    @Test
    fun lineInequalityMatchesOfficialGeometryAndDynamicInverse() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "inequality",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val first = point(board, doubleArrayOf(-3.0, -1.0), "")
        val second = point(board, doubleArrayOf(3.0, 2.0), "")
        val source = assertIs<GMResult.Ok<Line>>(
            Line.create(
                board = board,
                point1 = first,
                point2 = second,
                id = "sourceLine",
                name = "",
            ),
        ).value
        val inverse = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(false)
        }
        val inequality = curve(
            Curve.createInequality(
                board = board,
                source = source,
                inverseTerm = inverse,
                id = "lineInequality",
                name = "",
            ),
        )

        assertTrue(inequality.isInequality)
        assertEquals("curve", inequality.elType)
        assertEquals("plot", inequality.curveType)
        assertEquals(listOf("sourceLine"), inequality.parents)
        assertFalse(source.childElements.containsKey(inequality.id))
        assertEquals(5L, inequality.requestedPointCount())
        assertInequalityCoordinates(
            curve = inequality,
            expectedX = doubleArrayOf(
                21.266252583997982,
                5.166563145999497,
                -37.765942021996466,
                -21.66625258399798,
                21.266252583997982,
            ),
            expectedY = doubleArrayOf(
                11.133126291998991,
                43.33250516799596,
                21.86625258399798,
                -10.33312629199899,
                11.133126291998991,
            ),
        )

        second.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(1.0, 4.0),
        )
        inverse.value = JessieCodeRuntimeValue.BooleanValue(true)
        board.update()

        assertInequalityCoordinates(
            curve = inequality,
            expectedX = doubleArrayOf(
                13.651217726672035,
                41.76249486662113,
                11.777132584008768,
                -16.334144555940327,
                13.651217726672035,
            ),
            expectedY = doubleArrayOf(
                19.814022158340048,
                -2.6749995536192266,
                -40.15670240688469,
                -17.667680694925412,
                19.814022158340048,
            ),
        )
    }

    @Test
    fun functionGraphInequalityMatchesOfficialSegmentClosure() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "inequality",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        point(board, doubleArrayOf(1.0, 0.0), "A")
        val source = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource =
                    "x == 0 ? 0 / 0 : A.X() * x * x - 2",
                minimumSource = "-3",
                maximumSource = "3",
                sampleCount = 8,
                id = "sourceFunction",
                name = "",
            ),
        )
        val inverse = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(false)
        }
        val inequality = curve(
            Curve.createInequality(
                board = board,
                source = source,
                inverseTerm = inverse,
                id = "functionInequality",
                name = "",
            ),
        )

        assertEquals(18L, inequality.requestedPointCount())
        assertInequalityCoordinates(
            curve = inequality,
            expectedX = doubleArrayOf(
                -3.0, -3.0, -3.0, -2.25, -1.5, -0.75,
                -0.75, -0.75, -3.0, Double.NaN,
                0.75, 0.75, 0.75, 1.5, 2.25, 3.0, 3.0, 0.75,
            ),
            expectedY = doubleArrayOf(
                -13.4375, 7.0, 7.0, 3.0625, 0.25, -1.4375,
                -1.4375, -18.0, -13.4375, Double.NaN,
                -13.4375, -1.4375, -1.4375, 0.25, 3.0625,
                3.0625, -18.0, -13.4375,
            ),
        )

        val driver = assertIs<Point>(board.select("A"))
        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(2.0, 0.0),
        )
        inverse.value = JessieCodeRuntimeValue.BooleanValue(true)
        board.update()

        assertInequalityCoordinates(
            curve = inequality,
            expectedX = doubleArrayOf(
                -3.0, -3.0, -3.0, -2.25, -1.5, -0.75,
                -0.75, -0.75, -3.0, Double.NaN,
                0.75, 0.75, 0.75, 1.5, 2.25, 3.0, 3.0, 0.75,
            ),
            expectedY = doubleArrayOf(
                28.0, 16.0, 16.0, 8.125, 2.5, -0.875,
                -0.875, 18.0, 28.0, Double.NaN,
                28.0, -0.875, -0.875, 2.5, 8.125,
                8.125, 18.0, 28.0,
            ),
        )
    }

    @Test
    fun inequalityHandlesEmptyInvalidAndRemovalBehavior() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "inequality",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val inverse = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(false)
        }
        val emptySource = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "0 / 0",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 4,
                id = "emptySource",
                name = "",
            ),
        )
        val empty = curve(
            Curve.createInequality(
                board = board,
                source = emptySource,
                inverseTerm = inverse,
                id = "emptyInequality",
                name = "",
            ),
        )
        assertEquals(0, empty.numberPoints)
        assertEquals(0L, empty.requestedPointCount())

        val invalidSource = point(
            board = board,
            coordinates = doubleArrayOf(0.0, 0.0),
            name = "",
        )
        val objectCount = board.numObjects
        assertEquals(
            CurveError.InvalidInequalitySource(
                elementType = "point",
                curveType = null,
            ),
            assertIs<GMResult.Err<CurveError.InvalidInequalitySource>>(
                Curve.createInequality(
                    board = board,
                    source = invalidSource,
                    inverseTerm = inverse,
                ),
            ).error,
        )
        assertEquals(objectCount, board.numObjects)

        board.removeObject(emptySource)
        assertSame(empty, board.select("emptyInequality"))
        assertEquals(listOf("emptySource"), empty.parents)
    }

    @Test
    fun vectorFieldMatchesOfficialPathAndDynamicMeshBehavior() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            id = "vectorfield",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val xSteps = MutableNumericCoordinateFunction(2.0)
        val ySteps = MutableNumericCoordinateFunction(1.0)
        val scale = MutableNumericCoordinateFunction(1.0)
        val arrowEnabled = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(true)
        }
        val arrowSize = MutableNumericCoordinateFunction(5.0)
        val arrowAngle = MutableNumericCoordinateFunction(
            kotlin.math.PI * 0.125,
        )
        val field = curve(
            Curve.createVectorField(
                board = board,
                field = CurveVectorFieldComponentFunction(
                    xTerm = MutableRuntimeCoordinateFunction { arguments ->
                        arguments[1]
                    },
                    yTerm = MutableRuntimeCoordinateFunction { arguments ->
                        val x = assertIs<
                            JessieCodeRuntimeValue.NumberValue
                            >(arguments[0]).value
                        JessieCodeRuntimeValue.NumberValue(-x)
                    },
                ),
                xData = listOf(
                    MutableNumericCoordinateFunction(-2.0),
                    xSteps,
                    MutableNumericCoordinateFunction(2.0),
                ),
                yData = listOf(
                    MutableNumericCoordinateFunction(-1.0),
                    ySteps,
                    MutableNumericCoordinateFunction(1.0),
                ),
                scaleTerm = scale,
                arrowEnabledTerm = arrowEnabled,
                arrowSizeTerm = arrowSize,
                arrowAngleTerm = arrowAngle,
                id = "field",
                name = "",
            ),
        )

        assertTrue(field.isVectorField)
        assertEquals("vectorfield", field.elType)
        assertEquals("plot", field.curveType)
        assertEquals(42, field.numberPoints)
        assertEquals(42L, field.requestedPointCount())
        assertTrue(field.parents.isEmpty())
        val initialSnapshot = assertIs<CurveVectorFieldSnapshot>(
            field.vectorFieldSnapshot(),
        )
        assertEquals(6, initialSnapshot.vectors.size)
        assertEquals(
            CurveVectorFieldVectorSnapshot(
                startX = -2.0,
                startY = -1.0,
                endX = -3.0,
                endY = 1.0,
            ),
            initialSnapshot.vectors.first(),
        )
        assertTrue(initialSnapshot.arrowEnabled)
        assertEquals(5.0, initialSnapshot.arrowSize)
        assertEquals(kotlin.math.PI * 0.125, initialSnapshot.arrowAngle)
        assertContentEquals(
            doubleArrayOf(
                -2.0,
                -3.0,
                Double.NaN,
                -2.905568255625537,
                -3.0,
                -2.991138872488665,
                Double.NaN,
            ),
            field.dataX?.copyOfRange(0, 7),
        )
        assertContentEquals(
            doubleArrayOf(
                -1.0,
                1.0,
                Double.NaN,
                0.9180997823299839,
                1.0,
                0.8753144738984197,
                Double.NaN,
            ),
            field.dataY?.copyOfRange(0, 7),
        )

        xSteps.value = 2.5
        ySteps.value = 2.0
        scale.value = 0.5
        arrowEnabled.value =
            JessieCodeRuntimeValue.BooleanValue(false)
        board.update()

        assertEquals(27, field.numberPoints)
        assertEquals(27L, field.requestedPointCount())
        val movedSnapshot = assertIs<CurveVectorFieldSnapshot>(
            field.vectorFieldSnapshot(),
        )
        assertEquals(9, movedSnapshot.vectors.size)
        assertFalse(movedSnapshot.arrowEnabled)
        assertContentEquals(
            doubleArrayOf(-2.0, -2.5, Double.NaN),
            field.dataX?.copyOfRange(0, 3),
        )
        assertContentEquals(
            doubleArrayOf(-1.0, 0.0, Double.NaN),
            field.dataY?.copyOfRange(0, 3),
        )
    }

    @Test
    fun vectorFieldPreservesStepEdgesAndRejectsUnboundedOutput() {
        assertEquals(
            3L,
            Curve.vectorFieldPointCount(
                xSteps = 0.0,
                ySteps = 0.0,
                arrowEnabled = false,
            ),
        )
        assertEquals(
            18L,
            Curve.vectorFieldPointCount(
                xSteps = 1.5,
                ySteps = 2.5,
                arrowEnabled = false,
            ),
        )
        assertEquals(
            0L,
            Curve.vectorFieldPointCount(
                xSteps = -1.0,
                ySteps = 2.0,
                arrowEnabled = true,
            ),
        )
        assertEquals(
            Long.MAX_VALUE,
            Curve.vectorFieldPointCount(
                xSteps = Double.POSITIVE_INFINITY,
                ySteps = 1.0,
                arrowEnabled = true,
            ),
        )

        val zero = vectorField(
            board = board(),
            xSteps = 0.0,
            ySteps = 0.0,
            arrowEnabled = false,
        )
        assertEquals(3, zero.numberPoints)
        assertContentEquals(
            doubleArrayOf(-1.0, -2.0, Double.NaN),
            zero.dataX,
        )
        assertContentEquals(
            doubleArrayOf(-1.0, -2.0, Double.NaN),
            zero.dataY,
        )

        val negative = vectorField(
            board = board(),
            xSteps = -1.0,
            ySteps = 2.0,
            arrowEnabled = false,
        )
        assertEquals(0, negative.numberPoints)

        val oversizedBoard = board()
        val error = assertIs<GMResult.Err<CurveError.InvalidSampleCount>>(
            createVectorField(
                board = oversizedBoard,
                xSteps = 100.0,
                ySteps = 100.0,
                arrowEnabled = true,
            ),
        ).error
        assertEquals(71_407, error.count)
        assertEquals(Curve.MAX_SAMPLE_COUNT, error.maximum)
        assertTrue(oversizedBoard.objects.isEmpty())
    }

    @Test
    fun vectorFieldArrayFunctionFailureIsStructuredAndAtomic() {
        val board = board()
        val result = Curve.createVectorField(
            board = board,
            field = CurveVectorFieldArrayFunction(
                MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.ArrayValue(
                        listOf(JessieCodeRuntimeValue.NumberValue(1.0)),
                    )
                },
            ),
            xData = mesh(1.0),
            yData = mesh(1.0),
            scaleTerm = MutableNumericCoordinateFunction(1.0),
            arrowEnabledTerm = MutableRuntimeCoordinateFunction {
                JessieCodeRuntimeValue.BooleanValue(false)
            },
            arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
            arrowAngleTerm = MutableNumericCoordinateFunction(
                kotlin.math.PI * 0.125,
            ),
        )

        val error = assertIs<
            GMResult.Err<CurveError.NonNumericExpression>
            >(result).error
        assertEquals("vectorfield.F", error.term)
        assertEquals("array", error.actualType)
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun slopeFieldNormalizesScalarSlopeAndPreservesNonFiniteArithmetic() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            id = "slopefield",
            boundingBox = doubleArrayOf(-8.0, 6.0, 8.0, -6.0),
        )
        val slope = MutableRuntimeCoordinateFunction { arguments ->
            val x = assertIs<
                JessieCodeRuntimeValue.NumberValue
                >(arguments[0]).value
            val y = assertIs<
                JessieCodeRuntimeValue.NumberValue
                >(arguments[1]).value
            JessieCodeRuntimeValue.NumberValue(x - y)
        }
        val field = curve(
            Curve.createSlopeField(
                board = board,
                field = CurveSlopeFieldFunction(slope),
                xData = listOf(
                    MutableNumericCoordinateFunction(-2.0),
                    MutableNumericCoordinateFunction(2.0),
                    MutableNumericCoordinateFunction(2.0),
                ),
                yData = listOf(
                    MutableNumericCoordinateFunction(-1.0),
                    MutableNumericCoordinateFunction(1.0),
                    MutableNumericCoordinateFunction(1.0),
                ),
                scaleTerm = MutableNumericCoordinateFunction(1.0),
                arrowEnabledTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(false)
                },
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI * 0.125,
                ),
                id = "field",
                name = "",
            ),
        )

        assertTrue(field.isVectorField)
        assertEquals("slopefield", field.elType)
        assertEquals(18, field.numberPoints)
        assertContentEquals(
            doubleArrayOf(
                -2.0,
                -1.2928932188134525,
                Double.NaN,
            ),
            field.dataX?.copyOfRange(0, 3),
        )
        assertContentEquals(
            doubleArrayOf(
                -1.0,
                -1.7071067811865475,
                Double.NaN,
            ),
            field.dataY?.copyOfRange(0, 3),
        )

        val infinite = curve(
            Curve.createSlopeField(
                board = board,
                field = CurveSlopeFieldFunction(
                    MutableNumericCoordinateFunction(
                        Double.POSITIVE_INFINITY,
                    ),
                ),
                xData = listOf(
                    MutableNumericCoordinateFunction(0.0),
                    MutableNumericCoordinateFunction(0.0),
                    MutableNumericCoordinateFunction(0.0),
                ),
                yData = listOf(
                    MutableNumericCoordinateFunction(0.0),
                    MutableNumericCoordinateFunction(0.0),
                    MutableNumericCoordinateFunction(0.0),
                ),
                scaleTerm = MutableNumericCoordinateFunction(1.0),
                arrowEnabledTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(false)
                },
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI * 0.125,
                ),
                id = "infinite",
                name = "",
            ),
        )
        assertEquals(0.0, infinite.dataX?.get(1))
        assertTrue(infinite.dataY?.get(1)?.isNaN() == true)
    }

    private fun board(): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "board",
        )

    private fun point(
        board: Board,
        coordinates: DoubleArray,
        name: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = coordinates,
                name = name,
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun vectorField(
        board: Board,
        xSteps: Double,
        ySteps: Double,
        arrowEnabled: Boolean,
    ): Curve = curve(
        createVectorField(
            board = board,
            xSteps = xSteps,
            ySteps = ySteps,
            arrowEnabled = arrowEnabled,
        ),
    )

    private fun createVectorField(
        board: Board,
        xSteps: Double,
        ySteps: Double,
        arrowEnabled: Boolean,
    ): GMResult<Curve, CurveError> =
        Curve.createVectorField(
            board = board,
            field = CurveVectorFieldComponentFunction(
                xTerm = MutableRuntimeCoordinateFunction { arguments ->
                    arguments[0]
                },
                yTerm = MutableRuntimeCoordinateFunction { arguments ->
                    arguments[1]
                },
            ),
            xData = mesh(xSteps),
            yData = mesh(ySteps),
            scaleTerm = MutableNumericCoordinateFunction(1.0),
            arrowEnabledTerm = MutableRuntimeCoordinateFunction {
                JessieCodeRuntimeValue.BooleanValue(arrowEnabled)
            },
            arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
            arrowAngleTerm = MutableNumericCoordinateFunction(
                kotlin.math.PI * 0.125,
            ),
        )

    private fun mesh(
        steps: Double,
    ): List<JessieCodeCoordinateFunction> =
        listOf(
            MutableNumericCoordinateFunction(-1.0),
            MutableNumericCoordinateFunction(steps),
            MutableNumericCoordinateFunction(1.0),
        )

    private fun assertCombCoordinates(
        curve: Curve,
        expected: List<Pair<Double, Double>?>,
    ) {
        assertEquals(expected.size, curve.numberPoints)
        for ((index, expectedPoint) in expected.withIndex()) {
            val actualX = curve.dataX?.get(index) ?: Double.NaN
            val actualY = curve.dataY?.get(index) ?: Double.NaN
            if (expectedPoint == null) {
                assertTrue(actualX.isNaN())
                assertTrue(actualY.isNaN())
            } else {
                assertEquals(
                    expectedPoint.first,
                    actualX,
                    absoluteTolerance = 1.0e-12,
                )
                assertEquals(
                    expectedPoint.second,
                    actualY,
                    absoluteTolerance = 1.0e-12,
                )
            }
        }
    }

    private fun assertInequalityCoordinates(
        curve: Curve,
        expectedX: DoubleArray,
        expectedY: DoubleArray,
    ) {
        assertEquals(expectedX.size, curve.numberPoints)
        for (index in expectedX.indices) {
            val actualX = curve.dataX?.get(index) ?: Double.NaN
            val actualY = curve.dataY?.get(index) ?: Double.NaN
            if (expectedX[index].isNaN()) {
                assertTrue(actualX.isNaN(), "Expected NaN x at $index")
            } else {
                assertEquals(
                    expectedX[index],
                    actualX,
                    absoluteTolerance = 1.0e-12,
                    message = "x[$index]",
                )
            }
            if (expectedY[index].isNaN()) {
                assertTrue(actualY.isNaN(), "Expected NaN y at $index")
            } else {
                assertEquals(
                    expectedY[index],
                    actualY,
                    absoluteTolerance = 1.0e-12,
                    message = "y[$index]",
                )
            }
        }
    }

    private class MutableStepTerm(
        private val values: MutableList<Double>,
    ) : CurveStepTerm {
        override val length: Int
            get() = values.size
        override val isFunction: Boolean = false

        override fun valueAt(index: Int): Double =
            values.getOrNull(index) ?: Double.NaN
    }

    private class FunctionStepTerm(
        override val length: Int,
    ) : CurveStepTerm {
        override val isFunction: Boolean = true

        override fun valueAt(index: Int): Double = Double.NaN
    }

    private class MutableNumericCoordinateFunction(
        var value: Double,
    ) : JessieCodeCoordinateFunction {
        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value))
    }

    private class MutableRuntimeCoordinateFunction(
        private val evaluateValue:
            (List<JessieCodeRuntimeValue>) -> JessieCodeRuntimeValue,
    ) : JessieCodeCoordinateFunction {
        var value: JessieCodeRuntimeValue? = null

        override val origin: String? = null
        override val dependencies: Map<String, GeometryElement> = emptyMap()

        override fun evaluate(
            arguments: List<JessieCodeRuntimeValue>,
        ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
            GMResult.Ok(value ?: evaluateValue(arguments))
    }
}
