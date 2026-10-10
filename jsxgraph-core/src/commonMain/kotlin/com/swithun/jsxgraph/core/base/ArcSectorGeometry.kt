/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/curve.js -> interpolationFunctionFromArray /
 * getLabelPosition, src/element/arc.js -> getLabelAnchor,
 * src/element/sector.js -> getLabelAnchor
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.Type
import kotlin.math.cos
import kotlin.math.sin

internal fun arcSectorLabelAnchor(
    board: Board,
    center: Point,
    radiusPoint: Point,
    anglePoint: Point,
    selection: String,
    orientation: String,
    position: String,
    distance: Double,
    labelSize: DoubleArray,
    points: List<Coords>,
    bezierDegree: Int,
    offsetPixels: Double,
): Coords {
    if ("right" in position || "left" in position) {
        return sampledCurveLabelPosition(
            board = board,
            points = points,
            bezierDegree = bezierDegree,
            position = position,
            distance = distance,
            labelSize = labelSize,
        )
    }

    var angle = Geometry.rad(
        radiusPoint.Coords(),
        center.Coords(),
        anglePoint.Coords(),
    )
    if (
        (selection == Arc.SELECTION_MINOR && angle > kotlin.math.PI) ||
        (selection == Arc.SELECTION_MAJOR && angle < kotlin.math.PI) ||
        (
            selection == Arc.SELECTION_AUTO &&
                orientation == Arc.ORIENTATION_CLOCKWISE
            )
    ) {
        angle = -(2.0 * kotlin.math.PI - angle)
    }

    val centerCoordinates = center.coords.usrCoords
    val radialX = radiusPoint.X() - centerCoordinates[1]
    val radialY = radiusPoint.Y() - centerCoordinates[2]
    val halfAngle = angle * 0.5
    var vectorX =
        cos(halfAngle) * radialX - sin(halfAngle) * radialY
    var vectorY =
        sin(halfAngle) * radialX + cos(halfAngle) * radialY
    val length = Mat.hypot(vectorX, vectorY)
    vectorX *= (length + offsetPixels / board.unitX) / length
    vectorY *= (length + offsetPixels / board.unitY) / length
    val coordinates = doubleArrayOf(
        centerCoordinates[1] + vectorX,
        centerCoordinates[2] + vectorY,
    )
    return Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
        emitter = false,
    )
}

/**
 * Portable form of Curve#getLabelPosition for Arc/Sector, whose upstream
 * instances are Curve objects backed by cubic data arrays.
 */
private fun sampledCurveLabelPosition(
    board: Board,
    points: List<Coords>,
    bezierDegree: Int,
    position: String,
    distance: Double,
    labelSize: DoubleArray,
): Coords {
    if (points.isEmpty()) {
        return Coords(
            method = Const.COORDS_BY_SCREEN,
            coordinates = doubleArrayOf(Double.NaN, Double.NaN),
            board = board,
            emitter = false,
        )
    }
    val x = { parameter: Double ->
        sampledCoordinate(points, bezierDegree, parameter, 1)
    }
    val y = { parameter: Double ->
        sampledCoordinate(points, bezierDegree, parameter, 2)
    }
    val minimum = 0.0
    val maximum = (points.size - 1).toDouble() / bezierDegree

    val parsed = Type.parsePosition(position)
    var lambda = Type.parseNumber(parsed.pos, maximum - minimum)
    if ("fr" !in parsed.pos && "%" !in parsed.pos) {
        lambda = 0.0
    }
    var parameter = minimum + lambda
    var curvePoint = doubleArrayOf(1.0, x(parameter), y(parameter))
    var userX = curvePoint[1]
    var userY = curvePoint[2]
    if ((userX + userY).isNaN()) {
        lambda /= maximum - minimum
        parameter = minimum + lambda
        val first = doubleArrayOf(1.0, x(minimum), y(minimum))
        val last = doubleArrayOf(1.0, x(maximum), y(maximum))
        userX = first[1] + lambda * (last[1] - first[1])
        userY = first[2] + lambda * (last[2] - first[2])
        curvePoint = doubleArrayOf(1.0, userX, userY)
    }
    val screen = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = doubleArrayOf(userX, userY),
        board = board,
        emitter = false,
    ).scrCoords
    val epsilon = Mat.eps
    val nearby = when {
        parameter < minimum + epsilon ->
            doubleArrayOf(
                1.0,
                x(parameter + epsilon),
                y(parameter + epsilon),
            )
        parameter > maximum - epsilon ->
            doubleArrayOf(
                1.0,
                x(parameter - epsilon),
                y(parameter - epsilon),
            )
        else ->
            doubleArrayOf(
                1.0,
                x(parameter + epsilon),
                y(parameter + epsilon),
            )
    }
    var deltaX: Double
    var deltaY: Double
    if (parameter > maximum - epsilon) {
        deltaX = (curvePoint[1] - nearby[1]) / epsilon
        deltaY = (curvePoint[2] - nearby[2]) / epsilon
    } else if (parameter < minimum + epsilon) {
        deltaX = (nearby[1] - curvePoint[1]) / epsilon
        deltaY = (nearby[2] - curvePoint[2]) / epsilon
    } else {
        val before = doubleArrayOf(
            1.0,
            x(parameter - epsilon),
            y(parameter - epsilon),
        )
        deltaX = 0.5 * (nearby[1] - before[1]) / epsilon
        deltaY = 0.5 * (nearby[2] - before[2]) / epsilon
    }
    if (deltaX.isNaN()) {
        deltaX = 1.0
    }
    if (deltaY.isNaN()) {
        deltaY = 1.0
    }
    val norm = Mat.hypot(deltaX, deltaY)
    if (parsed.side == "left") {
        deltaY *= -1.0
    } else {
        deltaX *= -1.0
    }
    val factor = 0.5 * distance / norm
    return Coords(
        method = Const.COORDS_BY_SCREEN,
        coordinates = doubleArrayOf(
            screen[1] +
                deltaY * labelSize.getOrElse(0) { 0.0 } * factor,
            screen[2] -
                deltaX * labelSize.getOrElse(1) { 0.0 } * factor,
        ),
        board = board,
        emitter = false,
    )
}

internal fun sampledCoordinate(
    points: List<Coords>,
    bezierDegree: Int,
    parameter: Double,
    coordinateIndex: Int,
): Double {
    if (parameter.isNaN()) {
        return Double.NaN
    }
    if (parameter < 0.0) {
        return points.first().usrCoords[coordinateIndex]
    }
    if (bezierDegree == 3) {
        val last = (points.size - 1) / 3.0
        if (parameter >= last) {
            return points.last().usrCoords[coordinateIndex]
        }
        val segment = parameter.toInt() * 3
        val local = parameter % 1.0
        val inverse = 1.0 - local
        val values = DoubleArray(4) { offset ->
            points[segment + offset].usrCoords[coordinateIndex]
        }
        return inverse * inverse *
            (inverse * values[0] + 3.0 * local * values[1]) +
            (3.0 * inverse * values[2] + local * values[3]) *
            local * local
    }
    val index = if (parameter > points.size - 2) {
        points.size - 2
    } else {
        parameter.toInt()
    }
    if (index < 0) {
        return points.first().usrCoords[coordinateIndex]
    }
    if (index.toDouble() == parameter) {
        return points[index].usrCoords[coordinateIndex]
    }
    val first = points[index].usrCoords[coordinateIndex]
    val second = points[index + 1].usrCoords[coordinateIndex]
    return first + (second - first) * (parameter - index)
}
