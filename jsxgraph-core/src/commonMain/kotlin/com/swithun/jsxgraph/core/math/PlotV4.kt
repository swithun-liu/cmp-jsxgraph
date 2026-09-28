/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/math/plot.js -> Plot algorithm v4.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.math

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal data class PlotInterval(
    val lo: Double,
    val hi: Double,
)

internal fun interface PlotIntervalFunction<E> {
    fun evaluate(
        minimum: Double,
        maximum: Double,
        suspendedUpdate: Boolean,
    ): GMResult<PlotInterval?, E>
}

internal fun <E> updateParametricCurveV4Impl(
    board: Board,
    minimum: Double,
    maximum: Double,
    identityXTerm: Boolean,
    maximumPointCount: Int,
    x: PlotFunction<E>,
    y: PlotFunction<E>,
    intervalY: PlotIntervalFunction<E>? = null,
): GMResult<PlotResult, PlotError<E>> =
    PlotV4State(
        board = board,
        minimum = minimum,
        maximum = maximum,
        identityXTerm = identityXTerm,
        maximumPointCount = maximumPointCount,
        x = x,
        y = y,
        intervalY = intervalY,
    ).updateParametricCurveV4()

private class PlotV4State<E>(
    private val board: Board,
    private val minimum: Double,
    private val maximum: Double,
    private val identityXTerm: Boolean,
    private val maximumPointCount: Int,
    private val x: PlotFunction<E>,
    private val y: PlotFunction<E>,
    private val intervalY: PlotIntervalFunction<E>?,
) {
    private val points = mutableListOf<Coords>()
    private val projection = PlotV4Projection(board.getBoundingBox())

    // JSXGraph 1.13.3: src/math/plot.js -> updateParametricCurve_v4.
    fun updateParametricCurveV4(): GMResult<PlotResult, PlotError<E>> {
        val boundingBox = board.getBoundingBox()
        val horizontalMargin =
            (boundingBox[2] - boundingBox[0]) * DOMAIN_MARGIN_RATIO
        val startParameter = if (identityXTerm) {
            max(minimum, boundingBox[0] - horizontalMargin)
        } else {
            minimum
        }
        val endParameter = if (identityXTerm) {
            min(maximum, boundingBox[2] + horizontalMargin)
        } else {
            maximum
        }

        when (
            val result = plotV4(
                startParameter = startParameter,
                endParameter = endParameter,
                steps = SAMPLE_STEPS,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            PlotResult(
                points = points.toList(),
                visibleArea = doubleArrayOf(
                    startParameter,
                    endParameter,
                ),
            ),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _criticalInterval.
    private fun criticalInterval(
        values: DoubleArray,
        length: Int,
        level: Int,
    ): PlotV4CriticalInterval {
        val absoluteValues = Statistics.abs(values)
        var median = Statistics.median(absoluteValues)
        var verySmall = false
        if (median < MINIMUM_CRITICAL_MEDIAN) {
            median = MINIMUM_CRITICAL_MEDIAN
            verySmall = true
        } else {
            median *= CRITICAL_THRESHOLD
        }

        var inGroup = false
        var last = Int.MIN_VALUE
        var groupIndex = 0
        var positions = mutableListOf<PlotV4CriticalPosition>()
        val groups = mutableListOf<List<PlotV4CriticalPosition>>()
        for (index in 0 until length) {
            if (absoluteValues[index] > median) {
                positions += PlotV4CriticalPosition(
                    index = index,
                    value = values[index],
                    group = groupIndex,
                )
                last = index
                if (!inGroup) {
                    inGroup = true
                }
            } else if (inGroup && index > last + GROUP_GAP) {
                if (positions.isNotEmpty()) {
                    groups += positions.toList()
                }
                positions = mutableListOf()
                inGroup = false
                groupIndex += 1
            }
        }
        if (inGroup && positions.size > 1) {
            groups += positions.toList()
        }

        val types = MutableList(groups.size) { PlotV4CriticalType.POINT }
        for (group in groups.indices) {
            val positionsInGroup = groups[group]
            if (positionsInGroup.size < INTERVAL_GROUP_SIZE) {
                continue
            }
            var signChanges = 0
            var sign = sign(positionsInGroup[0].value)
            for (index in 1 until positionsInGroup.size) {
                val nextSign = sign(positionsInGroup[index].value)
                if (nextSign != sign) {
                    signChanges += 1
                    sign = nextSign
                }
            }
            if (signChanges * 6 > positionsInGroup.size) {
                types[group] = PlotV4CriticalType.INTERVAL
            }
        }
        return PlotV4CriticalInterval(
            smooth = verySmall && groups.isEmpty(),
            groups = groups,
            types = types,
            level = level,
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> findComponents.
    private fun findComponents(
        minimum: Double,
        maximum: Double,
        steps: Int,
    ): GMResult<List<PlotV4Component>, PlotError<E>> {
        val step = (maximum - minimum) / steps
        val components = mutableListOf(PlotV4Component())
        var component = components[0]
        var componentCount = 0
        var nanCount = 0
        var componentStarted = false
        var suspendedUpdate = false
        var parameter = minimum

        for (index in 0..steps) {
            val xValue = when (
                val result = evaluateCoordinate(
                    coordinate = PlotCoordinate.X,
                    parameter = parameter,
                    suspendedUpdate = suspendedUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val yValue = when (
                val result = evaluateCoordinate(
                    coordinate = PlotCoordinate.Y,
                    parameter = parameter,
                    suspendedUpdate = suspendedUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }

            if (xValue.isNaN() || yValue.isNaN()) {
                nanCount += 1
                if (nanCount > 1 && componentStarted) {
                    component.rightIsNaN = true
                    component.rightParameter = parameter - step
                    component.length = componentCount

                    componentStarted = false
                    components += PlotV4Component()
                    component = components.last()
                    nanCount = 0
                }
            } else {
                if (!componentStarted) {
                    componentStarted = true
                    componentCount = 0
                    if (nanCount > 0) {
                        component.leftParameter = parameter - step
                        component.leftIsNaN = true
                    }
                }
                nanCount = 0
                component.parameterValues += parameter
                component.xValues += xValue
                component.yValues += yValue
                componentCount += 1
            }
            if (index == 0) {
                suspendedUpdate = true
            }
            parameter += step
        }
        if (componentStarted) {
            component.length = componentCount
        } else {
            components.removeAt(components.lastIndex)
        }
        return GMResult.Ok(components)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> getPointType.
    private fun getPointType(
        position: Int,
        approximateParameter: Double,
        parameterValues: List<Double>,
        xTable: List<DoubleArray>,
        yTable: List<DoubleArray>,
        length: Int,
    ): PlotV4CriticalPoint {
        val fullLength = parameterValues.size
        val result = PlotV4CriticalPoint(
            index = position,
            parameter = approximateParameter,
            x = xTable[0][position],
            y = yTable[0][position],
            type = PlotV4PointType.OTHER,
        )
        if (position < BORDER_POINT_COUNT) {
            return result.copy(
                index = 0,
                parameter = parameterValues[0],
                x = xTable[0][0],
                y = yTable[0][0],
                type = PlotV4PointType.BORDER_LEFT,
            )
        }
        if (position > length - BORDER_POINT_COUNT - 1) {
            return result.copy(
                index = fullLength - 1,
                parameter = parameterValues[fullLength - 1],
                x = xTable[0][fullLength - 1],
                y = yTable[0][fullLength - 1],
                type = PlotV4PointType.BORDER_RIGHT,
            )
        }
        return result
    }

    // JSXGraph 1.13.3: src/math/plot.js -> newtonApprox.
    @Suppress("unused")
    private fun newtonApprox(
        index: Int,
        parameter: Double,
        step: Double,
        level: Int,
        table: List<DoubleArray>,
    ): Double {
        var value = 0.0
        for (currentLevel in level downTo 1) {
            value =
                (
                    (value + table[currentLevel][index]) *
                        (parameter - (currentLevel - 1) * step)
                    ) /
                    currentLevel
        }
        return value + table[0][index]
    }

    // JSXGraph 1.13.3: src/math/plot.js -> thiele.
    @Suppress("unused")
    private fun thiele(
        parameter: Double,
        reciprocalDifferences: List<DoubleArray>,
        parameterValues: List<Double>,
        index: Int,
        degree: Int,
    ): Double {
        var value = 0.0
        for (currentDegree in degree downTo 2) {
            value =
                (
                    parameter -
                        parameterValues[index + currentDegree]
                    ) /
                    (
                        reciprocalDifferences[currentDegree][index + 1] -
                            reciprocalDifferences[currentDegree - 2][index + 1] +
                            value
                        )
        }
        return reciprocalDifferences[0][index + 1] +
            (
                parameter -
                    parameterValues[index + 1]
                ) /
            (reciprocalDifferences[1][index + 1] + value)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> getCenterOfCriticalInterval.
    private fun getCenterOfCriticalInterval(
        group: List<PlotV4CriticalPosition>,
        degree: Int,
        parameterValues: List<Double>,
    ): PlotV4CriticalCenter {
        var maximum = Double.NEGATIVE_INFINITY
        var position = 0
        var positionMean = 0.0
        val equalMaximumPositions = mutableListOf<Int>()
        for (index in group.indices) {
            val value = abs(group[index].value)
            if (value > maximum) {
                equalMaximumPositions.clear()
                equalMaximumPositions += index
                maximum = value
                position = index
            } else if (maximum == value) {
                equalMaximumPositions += index
            }
        }
        if (equalMaximumPositions.isNotEmpty()) {
            positionMean =
                equalMaximumPositions.sum().toDouble() /
                equalMaximumPositions.size
            position = floor(positionMean).toInt()
            positionMean += group[0].index
        }

        if (maximum < Double.POSITIVE_INFINITY) {
            var numerator = 0.0
            var denominator = 0.0
            for (entry in group) {
                numerator += abs(entry.value) * entry.index
                denominator += abs(entry.value)
            }
            positionMean = numerator / denominator
        }
        positionMean += degree * 0.5
        val lowerIndex = floor(positionMean).toInt()
        val step = parameterValues[1] - parameterValues[0]
        return PlotV4CriticalCenter(
            index = group[position].index + degree * 0.5,
            position = positionMean,
            parameter =
                parameterValues[lowerIndex] +
                    step * (positionMean - lowerIndex),
        )
    }

    // JSXGraph 1.13.3: src/math/plot.js -> differenceMethod.
    private fun differenceMethod(
        component: PlotV4Component,
    ): PlotV4DifferenceResult {
        val parameterValues = component.parameterValues
        val xTable = mutableListOf(component.xValues.toDoubleArray())
        val yTable = mutableListOf(component.yValues.toDoubleArray())
        var length = component.yValues.size - 1
        val upperLevel = min(MAX_DIFFERENCE_LEVEL, length)
        var foundCriticalPoint = 0
        var degreeX = -1
        var degreeY = -1
        var groups: List<List<PlotV4CriticalPosition>> = emptyList()
        var types: List<PlotV4CriticalType> = emptyList()
        var yCritical = PlotV4CriticalInterval(
            smooth = false,
            groups = emptyList(),
            types = emptyList(),
            level = -1,
        )
        var level = 0

        while (level < upperLevel) {
            xTable += forwardDifferences(xTable[level])
            yTable += forwardDifferences(yTable[level])

            yCritical = criticalInterval(
                values = yTable[level + 1],
                length = length,
                level = level,
            )
            if (yCritical.smooth) {
                degreeY = level
                groups = emptyList()
            }
            val xCritical = criticalInterval(
                values = xTable[level + 1],
                length = length,
                level = level,
            )
            if (degreeX == -1 && xCritical.smooth) {
                degreeX = level
            }
            if (degreeY >= 0) {
                break
            }
            if (yCritical.groups.isNotEmpty()) {
                foundCriticalPoint += 1
                if (
                    foundCriticalPoint > REQUIRED_CRITICAL_LEVELS &&
                    (level + 1) % 2 == 0
                ) {
                    groups = yCritical.groups
                    types = yCritical.types
                    break
                }
            }
            length -= 1
            level += 1
        }

        val criticalPoints = mutableListOf<PlotV4CriticalPoint>()
        for (index in groups.indices) {
            if (types[index] == PlotV4CriticalType.INTERVAL) {
                continue
            }
            val center = getCenterOfCriticalInterval(
                group = groups[index],
                degree = level + 1,
                parameterValues = parameterValues,
            )
            criticalPoints += getPointType(
                position = floor(center.position).toInt(),
                approximateParameter = center.parameter,
                parameterValues = parameterValues,
                xTable = xTable,
                yTable = yTable,
                length = length + 1,
            )
        }
        return PlotV4DifferenceResult(
            criticalPoints = criticalPoints,
            xTable = xTable,
            yTable = yTable,
            degreeX = degreeX,
            degreeY = degreeY,
        )
    }

    private fun forwardDifferences(values: DoubleArray): DoubleArray =
        DoubleArray(values.size) { index ->
            if (index + 1 < values.size) {
                values[index + 1] - values[index]
            } else {
                Double.NaN
            }
        }

    // JSXGraph 1.13.3: src/math/plot.js -> _insertPoint_v4.
    private fun insertPoint(
        xValue: Double,
        yValue: Double,
        parameter: Double,
    ): GMResult<Unit, PlotError<E>> {
        if (points.isNotEmpty()) {
            val previous = points.last().usrCoords
            val previousScreen = projection.toScreen(
                previous[1],
                previous[2],
            )
            val screen = projection.toScreen(xValue, yValue)
            val deltaX = screen[1] - previousScreen[1]
            val deltaY = screen[2] - previousScreen[2]
            if (
                deltaX * deltaX + deltaY * deltaY <
                MINIMUM_SCREEN_DISTANCE * MINIMUM_SCREEN_DISTANCE
            ) {
                return GMResult.Ok(Unit)
            }
        }
        if (points.size >= maximumPointCount) {
            return GMResult.Err(
                PlotError.PointLimitExceeded(
                    attemptedCount = points.size + 1,
                    maximum = maximumPointCount,
                ),
            )
        }
        val point = Coords(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(1.0, xValue, yValue),
            board = board,
            emitter = false,
        )
        point.curveParameter = parameter
        points += point
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> getInterval.
    private fun getInterval(
        minimum: Double,
        maximum: Double,
    ): GMResult<PlotInterval?, PlotError<E>> {
        val function = intervalY ?: return GMResult.Ok(null)
        val result = try {
            function.evaluate(
                minimum = minimum,
                maximum = maximum,
                suspendedUpdate = true,
            )
        } catch (exception: Exception) {
            return GMResult.Err(
                PlotError.EvaluationException(
                    coordinate = PlotCoordinate.Y,
                    parameter = (minimum + maximum) * 0.5,
                    message = exception.message
                        ?: exception::class.simpleName
                        ?: "Plot interval evaluation failed",
                ),
            )
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PlotError.Evaluation(
                    coordinate = PlotCoordinate.Y,
                    parameter = (minimum + maximum) * 0.5,
                    error = result.error,
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/math/plot.js -> sign.
    private fun sign(value: Double): Int =
        when {
            value < 0.0 -> -1
            value > 0.0 -> 1
            else -> 0
        }

    // JSXGraph 1.13.3: src/math/plot.js -> handleBorder.
    private fun handleBorder(
        component: PlotV4Component,
        group: PlotV4CriticalPoint,
        yTable: List<DoubleArray>,
    ): GMResult<Unit, PlotError<E>> {
        val index = group.index
        val componentStep =
            component.parameterValues[1] - component.parameterValues[0]
        var parameter: Double
        var startParameter: Double
        var endParameter: Double
        if (group.type == PlotV4PointType.BORDER_LEFT) {
            parameter = if (component.leftIsNaN) {
                component.leftParameter ?: group.parameter - componentStep
            } else {
                group.parameter - componentStep
            }
            startParameter = parameter
            endParameter = startParameter + componentStep
        } else {
            parameter = if (component.rightIsNaN) {
                component.rightParameter ?: group.parameter + componentStep
            } else {
                group.parameter + componentStep
            }
            endParameter = parameter
            startParameter = endParameter - componentStep
        }

        val refinedComponents = when (
            val result = findComponents(
                minimum = startParameter,
                maximum = endParameter,
                steps = BORDER_STEPS,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (refinedComponents.isEmpty()) {
            return GMResult.Ok(Unit)
        }
        val refined = refinedComponents[0]

        if (group.type == PlotV4PointType.BORDER_LEFT) {
            endParameter = refined.parameterValues[0]
            val refinedStep =
                refined.parameterValues.getOrNull(1)
                    ?.minus(refined.parameterValues[0])
                    ?: Double.NaN
            startParameter =
                refined.leftParameter ?: (endParameter - refinedStep)
            parameter = startParameter
            val interval = when (
                val result = getInterval(startParameter, endParameter)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (interval != null) {
                val xValue = when (
                    val result = evaluateCoordinate(
                        coordinate = PlotCoordinate.X,
                        parameter = parameter,
                        suspendedUpdate = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val yValue =
                    if (yTable[1][index] < 0.0) interval.hi else interval.lo
                when (val result = insertPoint(xValue, yValue, parameter)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }

        for (refinedIndex in refined.parameterValues.indices) {
            when (
                val result = insertPoint(
                    xValue = refined.xValues[refinedIndex],
                    yValue = refined.yValues[refinedIndex],
                    parameter = refined.parameterValues[refinedIndex],
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        if (group.type == PlotV4PointType.BORDER_RIGHT) {
            val lastIndex = refined.parameterValues.lastIndex
            startParameter = refined.parameterValues[lastIndex]
            val refinedStep =
                refined.parameterValues.getOrNull(1)
                    ?.minus(refined.parameterValues[0])
                    ?: Double.NaN
            endParameter =
                refined.rightParameter ?: (startParameter + refinedStep)
            parameter = endParameter
            val interval = when (
                val result = getInterval(startParameter, endParameter)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (interval != null) {
                val xValue = when (
                    val result = evaluateCoordinate(
                        coordinate = PlotCoordinate.X,
                        parameter = parameter,
                        suspendedUpdate = true,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val yValue =
                    if (yTable[1][index] > 0.0) interval.hi else interval.lo
                when (val result = insertPoint(xValue, yValue, parameter)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _seconditeration_v4.
    private fun secondIteration(
        component: PlotV4Component,
        group: PlotV4CriticalPoint,
    ): GMResult<Unit, PlotError<E>> {
        val startParameter = component.parameterValues[group.index - 2]
        val endParameter = component.parameterValues[group.index + 2]
        val refinedComponents = when (
            val result = findComponents(
                minimum = startParameter,
                maximum = endParameter,
                steps = SECOND_ITERATION_STEPS,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for (componentIndex in refinedComponents.indices) {
            val refined = refinedComponents[componentIndex]
            val differences = differenceMethod(refined)
            var start = 0
            for (
                groupIndex in
                0..differences.criticalPoints.size
            ) {
                val end = if (
                    groupIndex == differences.criticalPoints.size
                ) {
                    refined.length
                } else {
                    differences.criticalPoints[groupIndex].index
                }
                for (index in start until end) {
                    val xValue = refined.xValues[index]
                    val yValue = refined.yValues[index]
                    if (!xValue.isNaN() && !yValue.isNaN()) {
                        when (
                            val result = insertPoint(
                                xValue = xValue,
                                yValue = yValue,
                                parameter = refined.parameterValues[index],
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }
                }
                if (
                    groupIndex <
                    differences.criticalPoints.size
                ) {
                    val criticalPoint =
                        differences.criticalPoints[groupIndex]
                    when (
                        val result = handleSingularity(
                            component = refined,
                            group = criticalPoint,
                            xTable = differences.xTable,
                            yTable = differences.yTable,
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                    start = criticalPoint.index + 1
                }
            }
            if (componentIndex < refinedComponents.lastIndex) {
                when (
                    val result = insertPoint(
                        xValue = Double.NaN,
                        yValue = Double.NaN,
                        parameter = refined.rightParameter ?: Double.NaN,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> _recurse_v4.
    private fun recurse(
        startParameter: Double,
        endParameter: Double,
        startX: Double,
        startY: Double,
        endX: Double,
        endY: Double,
        level: Int,
    ): GMResult<Unit, PlotError<E>> {
        val parameter = (startParameter + endParameter) * 0.5
        val xValue = when (
            val result = evaluateCoordinate(
                coordinate = PlotCoordinate.X,
                parameter = parameter,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val yValue = when (
            val result = evaluateCoordinate(
                coordinate = PlotCoordinate.Y,
                parameter = parameter,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (level == 0) {
            return insertPoint(
                xValue = Double.NaN,
                yValue = Double.NaN,
                parameter = parameter,
            )
        }

        if (
            projection.distance(
                firstX = xValue,
                firstY = yValue,
                secondX = startX,
                secondY = startY,
            ) > RECURSION_SCREEN_TOLERANCE
        ) {
            when (
                val result = recurse(
                    startParameter = startParameter,
                    endParameter = parameter,
                    startX = startX,
                    startY = startY,
                    endX = xValue,
                    endY = yValue,
                    level = level - 1,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        } else {
            when (val result = insertPoint(xValue, yValue, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }

        if (
            projection.distance(
                firstX = xValue,
                firstY = yValue,
                secondX = endX,
                secondY = endY,
            ) > RECURSION_SCREEN_TOLERANCE
        ) {
            return recurse(
                startParameter = parameter,
                endParameter = endParameter,
                startX = xValue,
                startY = yValue,
                endX = endX,
                endY = endY,
                level = level - 1,
            )
        }
        return insertPoint(xValue, yValue, parameter)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> handleSingularity.
    private fun handleSingularity(
        component: PlotV4Component,
        group: PlotV4CriticalPoint,
        xTable: List<DoubleArray>,
        yTable: List<DoubleArray>,
    ): GMResult<Unit, PlotError<E>> {
        val index = group.index
        val parameter = group.parameter
        val intervalStart =
            component.parameterValues[index - SINGULARITY_INTERVAL_OFFSET]
        val intervalEnd =
            component.parameterValues[index + SINGULARITY_INTERVAL_OFFSET]
        val interval = when (
            val result = getInterval(intervalStart, intervalEnd)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val low: Double
        val high: Double
        if (interval != null) {
            low = interval.lo
            high = interval.hi
        } else if (yTable[0][index - 1] < yTable[0][index + 1]) {
            low = yTable[0][index - 1]
            high = yTable[0][index + 1]
        } else {
            low = yTable[0][index + 1]
            high = yTable[0][index - 1]
        }
        val xValue = when (
            val result = evaluateCoordinate(
                coordinate = PlotCoordinate.X,
                parameter = parameter,
                suspendedUpdate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val leftDerivative =
            (
                yTable[0][index - DERIVATIVE_NEAR_OFFSET] -
                    yTable[0][index - SINGULARITY_INTERVAL_OFFSET]
                ) /
                (
                    component.parameterValues[
                        index - DERIVATIVE_NEAR_OFFSET
                    ] -
                        component.parameterValues[
                            index - SINGULARITY_INTERVAL_OFFSET
                        ]
                    )
        val rightDerivative =
            (
                yTable[0][index + DERIVATIVE_NEAR_OFFSET] -
                    yTable[0][index + SINGULARITY_INTERVAL_OFFSET]
                ) /
                (
                    component.parameterValues[
                        index + DERIVATIVE_NEAR_OFFSET
                    ] -
                        component.parameterValues[
                            index + SINGULARITY_INTERVAL_OFFSET
                        ]
                    )

        if (leftDerivative < -DERIVATIVE_THRESHOLD) {
            when (val result = insertPoint(xValue, low, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            if (rightDerivative <= DERIVATIVE_THRESHOLD) {
                when (
                    val result = insertPoint(
                        Double.NaN,
                        Double.NaN,
                        parameter,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        } else if (leftDerivative > DERIVATIVE_THRESHOLD) {
            when (val result = insertPoint(xValue, high, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            if (rightDerivative >= -DERIVATIVE_THRESHOLD) {
                when (
                    val result = insertPoint(
                        Double.NaN,
                        Double.NaN,
                        parameter,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        } else {
            if (low == Double.NEGATIVE_INFINITY) {
                when (val result = insertPoint(xValue, low, parameter)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (
                    val result = insertPoint(
                        Double.NaN,
                        Double.NaN,
                        parameter,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
            if (high == Double.POSITIVE_INFINITY) {
                when (
                    val result = insertPoint(
                        Double.NaN,
                        Double.NaN,
                        parameter,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                when (val result = insertPoint(xValue, high, parameter)) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }

            val firstIndex: Int
            val secondIndex: Int
            if (group.parameter < component.parameterValues[index]) {
                firstIndex = index - 1
                secondIndex = index
            } else {
                firstIndex = index
                secondIndex = index + 1
            }
            when (
                val result = recurse(
                    startParameter =
                        component.parameterValues[firstIndex],
                    endParameter =
                        component.parameterValues[secondIndex],
                    startX = xTable[0][firstIndex],
                    startY = yTable[0][firstIndex],
                    endX = xTable[0][secondIndex],
                    endY = yTable[0][secondIndex],
                    level = SINGULARITY_RECURSION_DEPTH,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        if (rightDerivative < -DERIVATIVE_THRESHOLD) {
            when (val result = insertPoint(xValue, high, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        } else if (rightDerivative > DERIVATIVE_THRESHOLD) {
            when (val result = insertPoint(xValue, low, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/math/plot.js -> plot_v4.
    private fun plotV4(
        startParameter: Double,
        endParameter: Double,
        steps: Int,
    ): GMResult<Unit, PlotError<E>> {
        val step = (endParameter - startParameter) / steps
        val components = when (
            val result = findComponents(
                minimum = startParameter,
                maximum = endParameter,
                steps = steps,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        for (componentIndex in components.indices) {
            val component = components[componentIndex]
            val differences = differenceMethod(component)
            val groups = differences.criticalPoints.toMutableList()
            if (
                groups.isEmpty() ||
                groups[0].type != PlotV4PointType.BORDER_LEFT
            ) {
                groups.add(
                    0,
                    PlotV4CriticalPoint(
                        index = 0,
                        parameter = component.parameterValues[0],
                        x = component.xValues[0],
                        y = component.yValues[0],
                        type = PlotV4PointType.BORDER_LEFT,
                    ),
                )
            }
            if (
                groups.last().type != PlotV4PointType.BORDER_RIGHT
            ) {
                val lastIndex = component.parameterValues.lastIndex
                groups += PlotV4CriticalPoint(
                    index = lastIndex,
                    parameter = component.parameterValues[lastIndex],
                    x = component.xValues[lastIndex],
                    y = component.yValues[lastIndex],
                    type = PlotV4PointType.BORDER_RIGHT,
                )
            }

            var start = 0
            for (groupIndex in 0..groups.size) {
                val end = if (groupIndex == groups.size) {
                    component.length
                } else {
                    groups[groupIndex].index - 1
                }
                for (index in start until end - 2) {
                    when (
                        val result = insertPoint(
                            xValue = component.xValues[index],
                            yValue = component.yValues[index],
                            parameter = component.parameterValues[index],
                        )
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                    if (
                        index >= start + EXTREMA_EDGE_DISTANCE &&
                        index < end - EXTREMA_EDGE_DISTANCE &&
                        differences.yTable.size > 3 &&
                        abs(differences.yTable[2][index]) >
                        EXTREMA_RATIO *
                        abs(differences.yTable[0][index])
                    ) {
                        val parameter = component.parameterValues[index]
                        when (
                            val result = insertExtrema(
                                startParameter = parameter,
                                endParameter = parameter + step,
                                step = step,
                                secondDifference =
                                    differences.yTable[2][index],
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }
                }

                if (groupIndex < groups.size) {
                    val group = groups[groupIndex]
                    when (
                        val result = if (
                            group.type == PlotV4PointType.BORDER_LEFT ||
                            group.type == PlotV4PointType.BORDER_RIGHT
                        ) {
                            handleBorder(
                                component = component,
                                group = group,
                                yTable = differences.yTable,
                            )
                        } else {
                            secondIteration(
                                component = component,
                                group = group,
                            )
                        }
                    ) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return result
                    }
                    start = group.index + 2
                }
            }
            if (componentIndex < components.lastIndex) {
                when (
                    val result = insertPoint(
                        xValue = Double.NaN,
                        yValue = Double.NaN,
                        parameter = component.rightParameter ?: Double.NaN,
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun insertExtrema(
        startParameter: Double,
        endParameter: Double,
        step: Double,
        secondDifference: Double,
    ): GMResult<Unit, PlotError<E>> {
        val interval = when (
            val result = getInterval(startParameter, endParameter)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (interval != null) {
            val quarterStep = step * 0.25
            val parameter: Double
            val yValue: Double
            if (secondDifference > 0.0) {
                parameter = startParameter + quarterStep
                yValue = interval.lo
            } else {
                parameter = endParameter - quarterStep
                yValue = interval.hi
            }
            return insertPoint(
                xValue = parameter,
                yValue = yValue,
                parameter = parameter,
            )
        }

        val minimum = when (
            val result = minimizeY(
                startParameter = startParameter,
                endParameter = endParameter,
                negate = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val maximum = when (
            val result = minimizeY(
                startParameter = startParameter,
                endParameter = endParameter,
                negate = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val ordered = if (minimum < maximum) {
            doubleArrayOf(minimum, maximum)
        } else {
            doubleArrayOf(maximum, minimum)
        }
        for (parameter in ordered) {
            val xValue = when (
                val result = evaluateCoordinate(
                    coordinate = PlotCoordinate.X,
                    parameter = parameter,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val yValue = when (
                val result = evaluateCoordinate(
                    coordinate = PlotCoordinate.Y,
                    parameter = parameter,
                    suspendedUpdate = true,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = insertPoint(xValue, yValue, parameter)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(Unit)
    }

    private fun minimizeY(
        startParameter: Double,
        endParameter: Double,
        negate: Boolean,
    ): GMResult<Double, PlotError<E>> {
        var evaluationError: PlotError<E>? = null
        val minimum = Numerics.fminbr(
            function = { parameter ->
                if (evaluationError != null) {
                    Double.NaN
                } else {
                    when (
                        val result = evaluateCoordinate(
                            coordinate = PlotCoordinate.Y,
                            parameter = parameter,
                            suspendedUpdate = true,
                        )
                    ) {
                        is GMResult.Ok ->
                            if (negate) -result.value else result.value
                        is GMResult.Err -> {
                            evaluationError = result.error
                            Double.NaN
                        }
                    }
                }
            },
            interval = doubleArrayOf(
                startParameter,
                endParameter,
            ),
        )
        evaluationError?.let { return GMResult.Err(it) }
        return when (minimum) {
            is GMResult.Ok -> minimum
            is GMResult.Err -> GMResult.Err(
                PlotError.Numerics(
                    operation = "fminbr",
                    error = minimum.error,
                ),
            )
        }
    }

    private fun evaluateCoordinate(
        coordinate: PlotCoordinate,
        parameter: Double,
        suspendedUpdate: Boolean,
    ): GMResult<Double, PlotError<E>> {
        val function = when (coordinate) {
            PlotCoordinate.X -> x
            PlotCoordinate.Y -> y
        }
        val result = try {
            function.evaluate(parameter, suspendedUpdate)
        } catch (exception: Exception) {
            return GMResult.Err(
                PlotError.EvaluationException(
                    coordinate = coordinate,
                    parameter = parameter,
                    message = exception.message
                        ?: exception::class.simpleName
                        ?: "Plot evaluation failed",
                ),
            )
        }
        return when (result) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                PlotError.Evaluation(
                    coordinate = coordinate,
                    parameter = parameter,
                    error = result.error,
                ),
            )
        }
    }

    private data class PlotV4Component(
        var leftIsNaN: Boolean = false,
        var rightIsNaN: Boolean = false,
        var leftParameter: Double? = null,
        var rightParameter: Double? = null,
        val parameterValues: MutableList<Double> = mutableListOf(),
        val xValues: MutableList<Double> = mutableListOf(),
        val yValues: MutableList<Double> = mutableListOf(),
        var length: Int = 0,
    )

    private data class PlotV4CriticalPosition(
        val index: Int,
        val value: Double,
        val group: Int,
    )

    private data class PlotV4CriticalInterval(
        val smooth: Boolean,
        val groups: List<List<PlotV4CriticalPosition>>,
        val types: List<PlotV4CriticalType>,
        val level: Int,
    )

    private enum class PlotV4CriticalType {
        POINT,
        INTERVAL,
    }

    private data class PlotV4CriticalPoint(
        val index: Int,
        val parameter: Double,
        val x: Double,
        val y: Double,
        val type: PlotV4PointType,
    )

    private enum class PlotV4PointType(
        val upstreamName: String,
    ) {
        BORDER_LEFT("borderleft"),
        BORDER_RIGHT("borderright"),
        OTHER("other"),
    }

    private data class PlotV4CriticalCenter(
        val index: Double,
        val position: Double,
        val parameter: Double,
    )

    private data class PlotV4DifferenceResult(
        val criticalPoints: List<PlotV4CriticalPoint>,
        val xTable: List<DoubleArray>,
        val yTable: List<DoubleArray>,
        val degreeX: Int,
        val degreeY: Int,
    )

    private class PlotV4Projection(
        boundingBox: DoubleArray,
    ) {
        private val unitX =
            CANONICAL_BOARD_WIDTH / (boundingBox[2] - boundingBox[0])
        private val unitY =
            CANONICAL_BOARD_HEIGHT / (boundingBox[1] - boundingBox[3])
        private val originX = -boundingBox[0] * unitX
        private val originY = boundingBox[1] * unitY

        fun toScreen(
            x: Double,
            y: Double,
        ): DoubleArray =
            doubleArrayOf(
                1.0,
                originX + x * unitX,
                originY - y * unitY,
            )

        fun distance(
            firstX: Double,
            firstY: Double,
            secondX: Double,
            secondY: Double,
        ): Double {
            val deltaX = (firstX - secondX) * unitX
            val deltaY = (firstY - secondY) * unitY
            return Mat.hypot(deltaX, deltaY)
        }
    }

    private companion object {
        private const val SAMPLE_STEPS = 1021
        private const val CRITICAL_THRESHOLD = 1000.0
        private const val MINIMUM_CRITICAL_MEDIAN = 1.0e-7
        private const val GROUP_GAP = 4
        private const val INTERVAL_GROUP_SIZE = 64
        private const val MAX_DIFFERENCE_LEVEL = 12
        private const val REQUIRED_CRITICAL_LEVELS = 2
        private const val BORDER_POINT_COUNT = 5
        private const val BORDER_STEPS = 32
        private const val SECOND_ITERATION_STEPS = 64
        private const val MINIMUM_SCREEN_DISTANCE = 0.8
        private const val RECURSION_SCREEN_TOLERANCE = 2.0
        private const val SINGULARITY_INTERVAL_OFFSET = 5
        private const val DERIVATIVE_NEAR_OFFSET = 3
        private const val DERIVATIVE_THRESHOLD = 100.0
        private const val SINGULARITY_RECURSION_DEPTH = 10
        private const val EXTREMA_EDGE_DISTANCE = 3
        private const val EXTREMA_RATIO = 0.2
        private const val DOMAIN_MARGIN_RATIO = 0.3
        private const val CANONICAL_BOARD_WIDTH = 500.0
        private const val CANONICAL_BOARD_HEIGHT = 500.0
    }
}
