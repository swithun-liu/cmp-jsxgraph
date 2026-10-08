/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/board.js -> createRoulette
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.C
import com.swithun.jsxgraph.core.math.Complex
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import kotlin.math.PI
import kotlin.math.atan2

internal sealed interface BoardRouletteError {
    data object SynchronousUnboundedAnimation : BoardRouletteError

    data class Transformation(
        val error: TransformationError,
    ) : BoardRouletteError
}

internal class BoardRoulette(
    private val board: Board,
    private val firstCurve: Curve,
    private val secondCurve: Curve,
    startFirstCurve: Double,
    private val stepSize: Double,
    private val direction: Double,
    private val delayMilliseconds: Double,
    private val points: List<CoordsElement>,
) {
    internal var running: Boolean = false
        private set
    internal var error: BoardRouletteError? = null
        private set

    private var firstParameter = startFirstCurve
    private var secondParameter = Numerics.root(
        function = { parameter ->
            val deltaX =
                firstCurve.X(firstParameter) -
                    secondCurve.X(parameter)
            val deltaY =
                firstCurve.Y(firstParameter) -
                    secondCurve.Y(parameter)
            deltaX * deltaX + deltaY * deltaY
        },
        interval = doubleArrayOf(0.0, 2.0 * PI),
    ).valueOrNaN()

    // JSXGraph 1.13.3: src/base/board.js ->
    // createRoulette.Roulette.rolling.
    internal fun rolling(): GMResult<BoardRoulette, BoardRouletteError> {
        val nextFirstParameter =
            firstParameter + direction * stepSize
        val firstDistance = arcLength(
            firstCurve,
            firstParameter,
            nextFirstParameter,
        )
        val nextSecondParameter = Numerics.root(
            function = { parameter ->
                firstDistance -
                    arcLength(
                        secondCurve,
                        secondParameter,
                        parameter,
                    )
            },
            initialValue = secondParameter,
        )
        val firstPosition = Complex(
            firstCurve.X(nextFirstParameter),
            firstCurve.Y(nextFirstParameter),
        )
        val secondPosition = Complex(
            secondCurve.X(nextSecondParameter),
            secondCurve.Y(nextSecondParameter),
        )
        val firstTangent = Complex(
            Numerics.D(firstCurve::X)(nextFirstParameter),
            Numerics.D(firstCurve::Y)(nextFirstParameter),
        )
        val secondTangent = Complex(
            Numerics.D(secondCurve::X)(nextSecondParameter),
            Numerics.D(secondCurve::Y)(nextSecondParameter),
        )
        val tangentRatio = C.div(firstTangent, secondTangent)
        var angle = atan2(tangentRatio.imaginary, tangentRatio.real)
        tangentRatio.div(C.abs(tangentRatio))
        tangentRatio.mult(secondPosition)
        val translationX = firstPosition.real - tangentRatio.real
        val translationY = firstPosition.imaginary - tangentRatio.imaginary

        val cornerThreshold = PI / 18.0
        val cornerMaximum = cornerThreshold * 9.0
        val transformations =
            if (
                angle < -cornerThreshold &&
                angle > -cornerMaximum
            ) {
                angle = -cornerThreshold
                listOf(
                    createTransformation(
                        "rotate",
                        doubleArrayOf(
                            angle,
                            firstCurve.X(firstParameter),
                            firstCurve.Y(firstParameter),
                        ),
                    ),
                )
            } else if (
                angle > cornerThreshold &&
                angle < cornerMaximum
            ) {
                angle = cornerThreshold
                listOf(
                    createTransformation(
                        "rotate",
                        doubleArrayOf(
                            angle,
                            firstCurve.X(firstParameter),
                            firstCurve.Y(firstParameter),
                        ),
                    ),
                )
            } else {
                listOf(
                    createTransformation(
                        "rotate",
                        doubleArrayOf(angle),
                    ),
                    createTransformation(
                        "translate",
                        doubleArrayOf(translationX, translationY),
                    ),
                )
            }
        for (result in transformations) {
            val transformation = when (result) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return GMResult.Err(result.error)
            }
            when (val applied = transformation.applyOnce(points)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    BoardRouletteError.Transformation(applied.error),
                )
            }
        }
        if (transformations.size == 2) {
            firstParameter = nextFirstParameter
            secondParameter = nextSecondParameter
        }
        board.update()
        error = null
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // createRoulette.Roulette.start.
    internal fun start(): GMResult<BoardRoulette, BoardRouletteError> {
        if (delayMilliseconds <= 0.0) {
            return GMResult.Ok(this)
        }
        if (board.animationScheduler.runsSynchronously) {
            val failure =
                BoardRouletteError.SynchronousUnboundedAnimation
            error = failure
            return GMResult.Err(failure)
        }
        running = true
        board.animationScheduler.schedule(delayMilliseconds) {
            if (!running) {
                false
            } else {
                when (val result = rolling()) {
                    is GMResult.Ok -> true
                    is GMResult.Err -> {
                        error = result.error
                        running = false
                        false
                    }
                }
            }
        }
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/board.js ->
    // createRoulette.Roulette.stop.
    internal fun stop(): BoardRoulette {
        running = false
        return this
    }

    private fun createTransformation(
        type: String,
        parameters: DoubleArray,
    ): GMResult<Transformation, BoardRouletteError> =
        when (val result = Transformation.create(type, parameters)) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                BoardRouletteError.Transformation(result.error),
            )
        }

    private fun arcLength(
        curve: Curve,
        start: Double,
        end: Double,
    ): Double {
        val derivativeX = Numerics.D(curve::X)
        val derivativeY = Numerics.D(curve::Y)
        val middle = 0.5 * (start + end)
        val startLength = Mat.hypot(
            derivativeX(start),
            derivativeY(start),
        )
        val endLength = Mat.hypot(
            derivativeX(end),
            derivativeY(end),
        )
        val middleLength = Mat.hypot(
            derivativeX(middle),
            derivativeY(middle),
        )
        return (
            startLength +
                4.0 * middleLength +
                endLength
            ) * (end - start) / 6.0
    }

    private fun <T, E> GMResult<T, E>.valueOrNaN(): Double =
        when (this) {
            is GMResult.Ok -> (value as? Number)?.toDouble() ?: Double.NaN
            is GMResult.Err -> Double.NaN
        }
}
