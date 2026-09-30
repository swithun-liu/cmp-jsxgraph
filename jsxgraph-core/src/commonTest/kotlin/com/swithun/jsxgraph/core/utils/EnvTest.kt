/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class EnvTest {
    @Test
    fun eventClassificationAndPositionsMatchOfficialFixture() {
        val touch = EnvEvent(
            touches = listOf(
                EnvPointer(3.0, 4.0),
                EnvPointer(5.0, 6.0),
            ),
        )
        assertTrue(Env.isTouchEvent(touch))
        assertFalse(Env.isPointerEvent(touch))
        assertFalse(Env.isMouseEvent(touch))
        assertEquals(2, Env.getNumberOfTouchPoints(touch))
        assertFalse(Env.isFirstTouch(touch))
        assertContentEquals(
            doubleArrayOf(3.0, 4.0),
            position(Env.getPosition(touch, 0)),
        )
        assertContentEquals(
            doubleArrayOf(3.0, 4.0),
            position(Env.getPosition(touch, -1)),
        )

        val touchEnd = EnvEvent(
            touches = emptyList(),
            changedTouches = listOf(EnvPointer(7.0, 8.0)),
        )
        assertTrue(Env.isTouchEvent(touchEnd))
        assertEquals(0, Env.getNumberOfTouchPoints(touchEnd))
        assertContentEquals(
            doubleArrayOf(7.0, 8.0),
            position(Env.getPosition(touchEnd, 0)),
        )

        val pointer = EnvEvent(
            pointerId = 0,
            isPrimary = true,
            clientX = 9.0,
            clientY = 10.0,
        )
        assertFalse(Env.isTouchEvent(pointer))
        assertTrue(Env.isPointerEvent(pointer))
        assertFalse(Env.isMouseEvent(pointer))
        assertEquals(-1, Env.getNumberOfTouchPoints(pointer))
        assertTrue(Env.isFirstTouch(pointer))
        assertContentEquals(
            doubleArrayOf(9.0, 10.0),
            position(Env.getPosition(pointer)),
        )

        val mouse = EnvEvent(clientX = 11.0, clientY = 12.0)
        assertTrue(Env.isMouseEvent(mouse))
        assertContentEquals(
            doubleArrayOf(11.0, 12.0),
            position(Env.getPosition(mouse)),
        )
        assertContentEquals(
            doubleArrayOf(0.0, 0.0),
            position(
                Env.getPosition(
                    EnvEvent(clientX = 0.0, clientY = 12.0),
                ),
            ),
        )
        assertIs<GMResult.Err<EnvError.InvalidTouchIndex>>(
            Env.getPosition(touch, 3),
        )
    }

    @Test
    fun userAgentPredicatesPreserveOfficialDeprecatedRules() {
        val android =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36"
        assertTrue(Env.isAndroid(android))
        assertTrue(Env.isWebkitAndroid(android))
        assertFalse(Env.isApple(android))

        assertTrue(Env.isApple("Mozilla/5.0 (iPhone) AppleWebKit"))
        assertTrue(Env.isApple("Mozilla/5.0 (iPad) AppleWebKit"))
        assertTrue(Env.isMozilla("Mozilla Mobile"))
        assertFalse(Env.isMozilla("Mozilla AppleWebKit"))
        assertTrue(Env.isFirefoxOS("Mozilla Mobile"))
        assertFalse(Env.isFirefoxOS("Mozilla Android Mobile"))
        assertFalse(Env.isAndroid(null))
        assertTrue(Env.isDesktop())
        assertTrue(Env.isMobile())
    }

    @Test
    fun cssTranslationAndScaleMatricesMatchOfficialFixture() {
        assertContentEquals(
            doubleArrayOf(8.0, 12.0),
            position(
                Env.getCssTransform(
                    position = doubleArrayOf(1.0, 2.0),
                    styles = mapOf(
                        "transform" to "translate(3px,4px)",
                        "zoom" to "2",
                    ),
                ),
            ),
        )
        assertContentEquals(
            doubleArrayOf(-1.5, 2.0),
            position(
                Env.getCssTransform(
                    position = doubleArrayOf(1.0, 2.0),
                    styles = mapOf(
                        "webkitTransform" to "translateX(-2.5px)",
                    ),
                ),
            ),
        )
        assertContentEquals(
            doubleArrayOf(1.0, 7.0),
            position(
                Env.getCssTransform(
                    position = doubleArrayOf(1.0, 2.0),
                    styles = mapOf(
                        "MozTransform" to "translateY(5px)",
                    ),
                ),
            ),
        )

        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 3.0, 3.0),
                doubleArrayOf(0.0, 4.0, 7.5),
            ),
            actual = Env.getCssTransformMatrix(
                mapOf(
                    "transform" to "matrix(2,3,4,5,6,7)",
                    "zoom" to "1.5",
                ),
            ),
        )
        assertMatrixEquals(
            expected = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0),
                doubleArrayOf(0.0, 0.0, 1.5),
            ),
            actual = Env.getCssTransformMatrix(
                mapOf(
                    "transform" to "scale(2,3)",
                    "zoom" to "0.5",
                ),
            ),
        )
        val singleScale = Env.getCssTransformMatrix(
            mapOf("transform" to "scale(2)"),
        )
        assertEquals(2.0, singleScale[1][1])
        assertTrue(singleScale[2][2].isNaN())
        assertEquals(
            4.0,
            Env.getCssTransformMatrix(
                mapOf("msTransform" to "scaleX(4)"),
            )[1][1],
        )
        assertEquals(
            5.0,
            Env.getCssTransformMatrix(
                mapOf("oTransform" to "scaleY(5)"),
            )[2][2],
        )
    }

    @Test
    fun inlineStyleAndFallbackDimensionsMatchOfficialFixture() {
        val styles = mapOf(
            "borderLeftWidth" to "12.5px",
            "invalidValue" to "invalid",
        )
        assertEquals("12.5px", Env.getStyle(styles, "border-left-width"))
        assertEquals(null, Env.getStyle(styles, "missing-value"))
        assertEquals(12, Env.getProp(styles, "border-left-width"))
        assertEquals(0, Env.getProp(styles, "invalid-value"))
        assertEquals(
            EnvDimensions(width = 500.0, height = 500.0),
            Env.defaultDimensions(),
        )
        assertEquals(16_777_215, Env.MAX_SCREEN_COORD)
        assertIs<GMResult.Err<EnvError.InvalidPositionCoordinateCount>>(
            Env.getCssTransform(
                position = doubleArrayOf(1.0),
                styles = emptyMap(),
            ),
        )
    }

    private fun position(
        result: GMResult<DoubleArray, EnvError>,
    ): DoubleArray =
        assertIs<GMResult.Ok<DoubleArray>>(result).value

    private fun assertMatrixEquals(
        expected: Array<DoubleArray>,
        actual: Array<DoubleArray>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertContentEquals(expected[index], actual[index])
        }
    }
}
