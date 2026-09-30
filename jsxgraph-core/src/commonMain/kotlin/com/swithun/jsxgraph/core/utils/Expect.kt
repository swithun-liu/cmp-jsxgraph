/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/expect.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.Point

internal object Expect {
    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.each.
    fun <T, R> each(
        values: List<T>,
        copy: Boolean = false,
        format: (T, Boolean) -> R,
    ): List<R> = values.map { value -> format(value, copy) }

    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coords.
    fun coords(
        source: Point,
        copy: Boolean = false,
    ): Coords = coords(source.coords, copy)

    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coords.
    fun coords(
        source: Coords,
        copy: Boolean = false,
    ): Coords =
        if (copy) {
            Coords(
                method = Const.COORDS_BY_USER,
                coordinates = source.usrCoords,
                board = source.board,
            )
        } else {
            source
        }

    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coordsArray.
    fun coordsArray(
        source: Point,
        copy: Boolean = false,
    ): DoubleArray = coordsArray(source.coords, copy)

    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coordsArray.
    fun coordsArray(
        source: Coords,
        copy: Boolean = false,
    ): DoubleArray =
        if (copy) {
            source.usrCoords.copyOfRange(0, 3)
        } else {
            source.usrCoords
        }

    /*
     * Kotlin arrays cannot grow in place. For a two-value array, this returns
     * the same prefixed value as upstream but necessarily allocates it.
     */
    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coordsArray.
    fun coordsArray(
        source: DoubleArray,
        copy: Boolean = false,
    ): DoubleArray {
        val normalized = if (source.size < 3) {
            doubleArrayOf(1.0) + source
        } else {
            source
        }
        return if (copy) {
            DoubleArray(3) { index ->
                normalized.getOrNull(index) ?: Double.NaN
            }
        } else {
            normalized
        }
    }

    // Resizable lists preserve upstream's in-place `unshift(1)` behavior.
    // JSXGraph 1.13.3: src/utils/expect.js -> JXG.Expect.coordsArray.
    fun coordsArray(
        source: MutableList<Double>,
        copy: Boolean = false,
    ): MutableList<Double> {
        if (source.size < 3) {
            source.add(0, 1.0)
        }
        return if (copy) {
            MutableList(3) { index ->
                source.getOrNull(index) ?: Double.NaN
            }
        } else {
            source
        }
    }
}
