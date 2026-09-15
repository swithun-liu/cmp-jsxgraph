/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

/**
 * Initial coordinate-access slice of JXG.CoordsElement.
 *
 * Constraint, glider, transformation, animation, and renderer behavior remain
 * in the untranslated element model. This class stays internal until those
 * lifecycle contracts are available.
 */
internal open class CoordsElement(
    board: Board,
    coordinates: DoubleArray = doubleArrayOf(1.0, 0.0, 0.0),
) {
    // JSXGraph: src/base/coordselement.js -> CoordsElement constructor.
    internal val coords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val initialCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val actualCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val isReal: Boolean
        get() = coords.isReal()

    // JSXGraph: src/base/coordselement.js -> X.
    internal fun X(): Double = coords.usrCoords[1]

    // JSXGraph: src/base/coordselement.js -> Y.
    internal fun Y(): Double = coords.usrCoords[2]

    // JSXGraph: src/base/coordselement.js -> Z.
    internal fun Z(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Coords.
    internal fun Coords(withZ: Boolean = false): DoubleArray =
        if (withZ) {
            coords.usrCoords.copyOf()
        } else {
            coords.usrCoords.copyOfRange(1, coords.usrCoords.size)
        }

    // JSXGraph: src/base/coordselement.js -> XEval, YEval, ZEval.
    internal fun XEval(): Double = coords.usrCoords[1]

    internal fun YEval(): Double = coords.usrCoords[2]

    internal fun ZEval(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Dist.
    internal fun Dist(other: CoordsElement): Double =
        if (isReal && other.isReal) {
            coords.distance(Const.COORDS_BY_USER, other.coords)
        } else {
            Double.NaN
        }
}
