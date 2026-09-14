/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coords.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.utils.EventEmitter
import com.swithun.jsxgraph.core.utils.EventHandler
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

internal enum class CoordinateArray {
    USER,
    SCREEN,
}

internal class Coords(
    method: Int,
    coordinates: DoubleArray,
    private val board: Board,
    private val emitter: Boolean = true,
) {
    internal val usrCoords = DoubleArray(3) { Double.NaN }
    internal val scrCoords = DoubleArray(3) { Double.NaN }

    private val events = if (emitter) EventEmitter(this) else null

    init {
        setCoordinates(method, coordinates, doRound = false, noEvent = true)
    }

    // JSXGraph: src/base/coords.js -> normalizeUsrCoords
    private fun normalizeUsrCoords() {
        if (abs(usrCoords[0]) > Mat.eps) {
            usrCoords[1] /= usrCoords[0]
            usrCoords[2] /= usrCoords[0]
            usrCoords[0] = 1.0
        }
    }

    // JSXGraph: src/base/coords.js -> usr2screen
    private fun usr2screen(doRound: Boolean?) {
        val origin = board.origin.scrCoords
        if (doRound == true) {
            scrCoords[0] = jsRound(usrCoords[0])
            scrCoords[1] = jsRound(usrCoords[0] * origin[1] + usrCoords[1] * board.unitX)
            scrCoords[2] = jsRound(usrCoords[0] * origin[2] - usrCoords[2] * board.unitY)
        } else {
            scrCoords[0] = usrCoords[0]
            scrCoords[1] = usrCoords[0] * origin[1] + usrCoords[1] * board.unitX
            scrCoords[2] = usrCoords[0] * origin[2] - usrCoords[2] * board.unitY
        }
    }

    // JSXGraph: src/base/coords.js -> screen2usr
    private fun screen2usr() {
        val origin = board.origin.scrCoords
        usrCoords[0] = 1.0
        usrCoords[1] = (scrCoords[1] - origin[1]) / board.unitX
        usrCoords[2] = (origin[2] - scrCoords[2]) / board.unitY
    }

    // JSXGraph: src/base/coords.js -> distance
    internal fun distance(
        coordType: Int,
        coordinates: Coords,
    ): Double {
        if (coordType == Const.COORDS_BY_USER) {
            val weightDifference = usrCoords[0] - coordinates.usrCoords[0]
            if (weightDifference * weightDifference > Mat.eps * Mat.eps) {
                return Double.POSITIVE_INFINITY
            }
            return Mat.hypot(
                usrCoords[1] - coordinates.usrCoords[1],
                usrCoords[2] - coordinates.usrCoords[2],
            )
        }

        return Mat.hypot(
            scrCoords[1] - coordinates.scrCoords[1],
            scrCoords[2] - coordinates.scrCoords[2],
        )
    }

    // JSXGraph: src/base/coords.js -> setCoordinates
    internal fun setCoordinates(
        coordType: Int,
        coordinates: DoubleArray,
        doRound: Boolean? = null,
        noEvent: Boolean = false,
    ): Coords {
        val oldUserCoordinates = usrCoords.copyOf()
        val oldScreenCoordinates = scrCoords.copyOf()

        if (coordType == Const.COORDS_BY_USER) {
            if (coordinates.size == 2) {
                usrCoords[0] = 1.0
                usrCoords[1] = coordinates.valueOrNaN(0)
                usrCoords[2] = coordinates.valueOrNaN(1)
            } else {
                usrCoords[0] = coordinates.valueOrNaN(0)
                usrCoords[1] = coordinates.valueOrNaN(1)
                usrCoords[2] = coordinates.valueOrNaN(2)
                normalizeUsrCoords()
            }
            usr2screen(doRound)
        } else {
            if (coordinates.size == 2) {
                scrCoords[1] = coordinates.valueOrNaN(0)
                scrCoords[2] = coordinates.valueOrNaN(1)
            } else {
                scrCoords[1] = coordinates.valueOrNaN(1)
                scrCoords[2] = coordinates.valueOrNaN(2)
            }
            screen2usr()
        }

        if (
            emitter &&
            !noEvent &&
            (oldScreenCoordinates[1] != scrCoords[1] || oldScreenCoordinates[2] != scrCoords[2])
        ) {
            events?.trigger(
                events = listOf(UPDATE_EVENT),
                arguments = listOf(oldUserCoordinates, oldScreenCoordinates),
            )
        }
        return this
    }

    // JSXGraph: src/base/coords.js -> copy
    internal fun copy(
        coordinateArray: CoordinateArray,
        offset: Int = 0,
    ): DoubleArray {
        val source = when (coordinateArray) {
            CoordinateArray.USER -> usrCoords
            CoordinateArray.SCREEN -> scrCoords
        }
        val start = if (offset < 0) {
            max(source.size + offset, 0)
        } else {
            min(offset, source.size)
        }
        return source.copyOfRange(start, source.size)
    }

    // JSXGraph: src/base/coords.js -> isReal
    internal fun isReal(): Boolean =
        !(usrCoords[1] + usrCoords[2]).isNaN() &&
            abs(usrCoords[0]) > Mat.eps

    internal fun on(
        event: String,
        handler: EventHandler,
        context: Any = this,
    ): Coords {
        events?.on(event, handler, context)
        return this
    }

    internal fun off(
        event: String,
        handler: EventHandler? = null,
    ): Coords {
        events?.off(event, handler)
        return this
    }

    private fun DoubleArray.valueOrNaN(index: Int): Double =
        if (index in indices) this[index] else Double.NaN

    private fun jsRound(value: Double): Double = when {
        value == 0.0 -> value
        value < 0.0 && value >= -0.5 -> -0.0
        else -> floor(value + 0.5)
    }

    internal companion object {
        const val UPDATE_EVENT = "update"
    }
}
