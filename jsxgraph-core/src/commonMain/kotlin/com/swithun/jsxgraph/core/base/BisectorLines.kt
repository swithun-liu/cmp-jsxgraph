/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js ->
 * createAngularBisectorsOfTwoLines
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue

internal sealed interface BisectorLinesError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : BisectorLinesError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : BisectorLinesError

    data class HelperPoint(
        val outputIndex: Int,
        val pointIndex: Int,
        val error: PointError,
    ) : BisectorLinesError

    data class OutputLine(
        val outputIndex: Int,
        val error: LineError,
    ) : BisectorLinesError
}

internal data class BisectorLineAttributes(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
)

/**
 * The two angular bisectors returned by JSXGraph's `bisectorlines` factory.
 */
internal class BisectorLines private constructor(
    line1: Line,
    line2: Line,
) : Composition(
    linkedMapOf(
        LINE1_MEMBER to line1,
        LINE2_MEMBER to line2,
    ),
) {
    internal val line1: Line?
        get() = member(LINE1_MEMBER) as? Line
    internal val line2: Line?
        get() = member(LINE2_MEMBER) as? Line

    init {
        elType = BISECTOR_LINES_ELEMENT_TYPE
        subs[LINE1_MEMBER] = line1
        subs[LINE2_MEMBER] = line2
    }

    internal companion object {
        private const val BISECTOR_LINES_ELEMENT_TYPE = "bisectorlines"
        private const val LINE1_MEMBER = "line1"
        private const val LINE2_MEMBER = "line2"

        // JSXGraph: src/element/composition.js ->
        // createAngularBisectorsOfTwoLines
        fun create(
            board: Board,
            first: Line,
            second: Line,
            line1Attributes: BisectorLineAttributes =
                BisectorLineAttributes(),
            line2Attributes: BisectorLineAttributes =
                BisectorLineAttributes(),
        ): GMResult<BisectorLines, BisectorLinesError> {
            validateParent(board, first, parentIndex = 0)?.let {
                return GMResult.Err(it)
            }
            validateParent(board, second, parentIndex = 1)?.let {
                return GMResult.Err(it)
            }

            val firstOutput = when (
                val result = createOutputLine(
                    board = board,
                    first = first,
                    second = second,
                    secondCoefficientFactor = -1.0,
                    outputIndex = 0,
                    attributes = line1Attributes,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val secondOutput = when (
                val result = createOutputLine(
                    board = board,
                    first = first,
                    second = second,
                    secondCoefficientFactor = 1.0,
                    outputIndex = 1,
                    attributes = line2Attributes,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    cleanupOutput(board, firstOutput)
                    return result
                }
            }

            val composition = BisectorLines(
                line1 = firstOutput.line,
                line2 = secondOutput.line,
            )
            composition.setParents(listOf(first, second))
            return GMResult.Ok(composition)
        }

        private fun createOutputLine(
            board: Board,
            first: Line,
            second: Line,
            secondCoefficientFactor: Double,
            outputIndex: Int,
            attributes: BisectorLineAttributes,
        ): GMResult<BisectorLineOutput, BisectorLinesError> {
            val point1 = when (
                val result = createHelperPoint(
                    board = board,
                    first = first,
                    second = second,
                    secondCoefficientFactor = secondCoefficientFactor,
                    endpoint = BisectorEndpoint.FIRST,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(
                    BisectorLinesError.HelperPoint(
                        outputIndex = outputIndex,
                        pointIndex = 0,
                        error = result.error,
                    ),
                )
            }
            val point2 = when (
                val result = createHelperPoint(
                    board = board,
                    first = first,
                    second = second,
                    secondCoefficientFactor = secondCoefficientFactor,
                    endpoint = BisectorEndpoint.SECOND,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    board.removeObject(point1)
                    return GMResult.Err(
                        BisectorLinesError.HelperPoint(
                            outputIndex = outputIndex,
                            pointIndex = 1,
                            error = result.error,
                        ),
                    )
                }
            }
            return when (
                val result = Line.create(
                    board = board,
                    point1 = point1,
                    point2 = point2,
                    id = attributes.id,
                    name = attributes.name,
                    needsRegularUpdate = attributes.needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> {
                    val line = result.value
                    line.dump = false
                    line.isDraggable = false
                    GMResult.Ok(
                        BisectorLineOutput(
                            line = line,
                            helperPoints = listOf(point1, point2),
                        ),
                    )
                }
                is GMResult.Err -> {
                    board.removeObjects(listOf(point1, point2))
                    GMResult.Err(
                        BisectorLinesError.OutputLine(
                            outputIndex = outputIndex,
                            error = result.error,
                        ),
                    )
                }
            }
        }

        private fun createHelperPoint(
            board: Board,
            first: Line,
            second: Line,
            secondCoefficientFactor: Double,
            endpoint: BisectorEndpoint,
        ): GMResult<Point, PointError> =
            Point.createConstrained(
                board = board,
                coordinateFunctions = List(3) { coordinateIndex ->
                    BisectorCoordinateFunction {
                        val coefficients = normalizedCoefficients(
                            first = first,
                            second = second,
                            secondCoefficientFactor =
                                secondCoefficientFactor,
                        )
                        endpoint.coordinates(coefficients)[coordinateIndex]
                    }
                },
                name = "",
            )

        private fun normalizedCoefficients(
            first: Line,
            second: Line,
            secondCoefficientFactor: Double,
        ): DoubleArray {
            val firstLength = Mat.hypot(
                first.stdform[1],
                first.stdform[2],
            )
            val secondLength = Mat.hypot(
                second.stdform[1],
                second.stdform[2],
            )
            return DoubleArray(3) { index ->
                first.stdform[index] / firstLength +
                    secondCoefficientFactor *
                    second.stdform[index] / secondLength
            }
        }

        private fun validateParent(
            board: Board,
            line: Line,
            parentIndex: Int,
        ): BisectorLinesError? {
            if (line.board !== board) {
                return BisectorLinesError.ParentBoardMismatch(parentIndex)
            }
            if (board.elementById(line.id) !== line) {
                return BisectorLinesError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = line.id,
                )
            }
            return null
        }

        private fun cleanupOutput(
            board: Board,
            output: BisectorLineOutput,
        ) {
            board.removeObjects(listOf(output.line) + output.helperPoints)
        }
    }
}

private data class BisectorLineOutput(
    val line: Line,
    val helperPoints: List<Point>,
)

private enum class BisectorEndpoint {
    FIRST,
    SECOND,
    ;

    fun coordinates(coefficients: DoubleArray): DoubleArray {
        val a = coefficients[0]
        val b = coefficients[1]
        val c = coefficients[2]
        val homogeneous = c * c + b * b
        return when (this) {
            FIRST -> doubleArrayOf(
                homogeneous,
                c - b * a + c,
                -b - c * a - b,
            )
            SECOND -> doubleArrayOf(
                homogeneous,
                -b * a + c,
                -c * a - b,
            )
        }
    }
}

private class BisectorCoordinateFunction(
    private val value: () -> Double,
) : JessieCodeCoordinateFunction {
    override val origin: String? = null
    override val dependencies: Map<String, GeometryElement> = emptyMap()

    override fun evaluate(
        arguments: List<JessieCodeRuntimeValue>,
    ): GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError> =
        GMResult.Ok(JessieCodeRuntimeValue.NumberValue(value()))
}
