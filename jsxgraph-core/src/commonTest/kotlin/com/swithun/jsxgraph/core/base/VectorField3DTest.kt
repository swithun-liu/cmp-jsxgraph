package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class VectorField3DTest {
    @Test
    fun componentFieldMatchesOfficialMeshScaleAndDynamicArrowBehavior() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 53.333333333333336,
            unitY = 48.0,
            boundingBox = doubleArrayOf(-6.0, 5.0, 6.0, -5.0),
        )
        val view = createView(board)
        val scale = MutableNumericCoordinateFunction(0.5)
        val arrowEnabled = MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(true)
        }
        val field = curve(
            Curve3D.createVectorField(
                view = view,
                field = Curve3DVectorFieldComponentFunction(
                    xTerm = MutableNumericCoordinateFunction(1.0),
                    yTerm = MutableNumericCoordinateFunction(0.0),
                    zTerm = MutableRuntimeCoordinateFunction { arguments ->
                        arguments[2]
                    },
                ),
                xData = mesh(0.0, 1.0, 1.0),
                yData = mesh(0.0, 0.0, 0.0),
                zData = mesh(0.0, 1.0, 1.0),
                scaleTerm = scale,
                arrowEnabledTerm = arrowEnabled,
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI * 0.125,
                ),
                id = "field",
                name = "",
            ),
        )

        assertTrue(field.isVectorField3D)
        assertEquals("curve3d", field.elType)
        assertEquals(Const.OBJECT_TYPE_CURVE3D, field.type)
        assertEquals(Const.OBJECT_CLASS_3D, field.elementClass)
        assertEquals(28, field.numberPoints)
        assertEquals(28L, field.requestedPointCount())
        assertEquals(28, field.curve2D.numberPoints)
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            field.points[0],
        )
        assertContentEquals(
            doubleArrayOf(1.0, 0.5, 0.0, 0.0),
            field.points[1],
        )
        assertArrayClose(
            doubleArrayOf(
                1.0,
                0.4133862938270669,
                0.0,
                -0.03781722715889626,
            ),
            field.points[3],
        )
        val snapshot = assertIs<Curve3DVectorFieldSnapshot>(
            field.vectorField3DSnapshot(),
        )
        assertEquals(4, snapshot.vectors.size)
        assertContentEquals(
            doubleArrayOf(0.0, 0.0, 0.0),
            snapshot.vectors.first().start,
        )
        assertContentEquals(
            doubleArrayOf(0.5, 0.0, 0.0),
            snapshot.vectors.first().vector,
        )

        scale.value = 1.0
        arrowEnabled.value =
            JessieCodeRuntimeValue.BooleanValue(false)
        board.update()

        assertEquals(12, field.numberPoints)
        assertEquals(12L, field.requestedPointCount())
        assertContentEquals(
            doubleArrayOf(1.0, 1.0, 0.0, 0.0),
            field.points[1],
        )
        assertTrue(
            assertIs<Curve3DVectorFieldSnapshot>(
                field.vectorField3DSnapshot(),
            ).arrowEnabled.not(),
        )
    }

    @Test
    fun arrayFieldSkipsOnlyVectorsBelowJavaScriptNumberEpsilon() {
        val board = createBoard()
        val field = curve(
            Curve3D.createVectorField(
                view = createView(board),
                field = Curve3DVectorFieldArrayFunction(
                    MutableRuntimeCoordinateFunction {
                        JessieCodeRuntimeValue.ArrayValue(
                            listOf(
                                JessieCodeRuntimeValue.NumberValue(0.0),
                                JessieCodeRuntimeValue.NumberValue(0.0),
                                JessieCodeRuntimeValue.NumberValue(0.0),
                            ),
                        )
                    },
                ),
                xData = mesh(0.0, 1.0, 1.0),
                yData = mesh(0.0, 0.0, 0.0),
                zData = mesh(0.0, 1.0, 1.0),
                scaleTerm = MutableNumericCoordinateFunction(1.0),
                arrowEnabledTerm = MutableRuntimeCoordinateFunction {
                    JessieCodeRuntimeValue.BooleanValue(true)
                },
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(
                    kotlin.math.PI * 0.125,
                ),
            ),
        )

        assertEquals(0, field.numberPoints)
        assertEquals(28L, field.requestedPointCount())
        assertTrue(
            assertIs<Curve3DVectorFieldSnapshot>(
                field.vectorField3DSnapshot(),
            ).vectors.isEmpty(),
        )
    }

    @Test
    fun malformedMeshAndExcessivePointCountAreStructured() {
        val board = createBoard()
        val view = createView(board)
        val invalidMesh = assertIs<GMResult.Err<Curve3DError.VectorField>>(
            Curve3D.createVectorField(
                view = view,
                field = constantField(),
                xData = mesh(0.0, 1.0, 1.0),
                yData = mesh(0.0, 1.0, 1.0),
                zData = mesh(0.0, 1.0),
                scaleTerm = MutableNumericCoordinateFunction(1.0),
                arrowEnabledTerm = booleanTerm(true),
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(0.0),
            ),
        ).error
        assertIs<CurveError.NonNumericExpression>(invalidMesh.error)
        assertTrue(board.objects.keys.none { it.startsWith("curve3d") })

        val excessive = assertIs<GMResult.Err<Curve3DError.VectorField>>(
            Curve3D.createVectorField(
                view = view,
                field = constantField(),
                xData = mesh(0.0, 100.0, 1.0),
                yData = mesh(0.0, 100.0, 1.0),
                zData = mesh(0.0, 1.0, 1.0),
                scaleTerm = MutableNumericCoordinateFunction(1.0),
                arrowEnabledTerm = booleanTerm(true),
                arrowSizeTerm = MutableNumericCoordinateFunction(5.0),
                arrowAngleTerm = MutableNumericCoordinateFunction(0.0),
            ),
        ).error
        assertIs<CurveError.InvalidSampleCount>(excessive.error)
        assertTrue(board.objects.keys.none { it.startsWith("curve3d") })
    }

    private fun constantField(): Curve3DVectorFieldFunction =
        Curve3DVectorFieldComponentFunction(
            xTerm = MutableNumericCoordinateFunction(1.0),
            yTerm = MutableNumericCoordinateFunction(0.0),
            zTerm = MutableNumericCoordinateFunction(0.0),
        )

    private fun booleanTerm(value: Boolean): JessieCodeCoordinateFunction =
        MutableRuntimeCoordinateFunction {
            JessieCodeRuntimeValue.BooleanValue(value)
        }

    private fun mesh(
        vararg values: Double,
    ): List<JessieCodeCoordinateFunction> =
        values.map(::MutableNumericCoordinateFunction)

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

    private fun curve(
        result: GMResult<Curve3D, Curve3DError>,
    ): Curve3D = assertIs<GMResult.Ok<Curve3D>>(result).value

    private fun assertArrayClose(
        expected: DoubleArray,
        actual: DoubleArray,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = 1.0e-12,
            )
        }
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
