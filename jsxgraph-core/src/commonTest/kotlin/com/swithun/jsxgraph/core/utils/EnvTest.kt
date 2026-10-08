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
import kotlin.test.assertSame
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
    fun runtimeCapabilitiesUseTheInjectedPlatformContract() {
        val defaults = EnvRuntimeCapabilities()
        assertTrue(Env.supportsES6(defaults))
        assertFalse(Env.supportsVML(defaults))
        assertFalse(Env.supportsSVG(defaults))
        assertTrue(Env.supportsCanvas(defaults))
        assertFalse(Env.isNode(defaults))
        assertFalse(Env.isWebWorker(defaults))
        assertTrue(Env.supportsPointerEvents(defaults))
        assertFalse(Env.isTouchDevice(defaults))
        assertFalse(Env.isWebkitApple(defaults))
        assertFalse(Env.isMetroApp(defaults))

        val browserHost = EnvRuntimeCapabilities(
            supportsSvg = true,
            supportsCanvas = false,
            isWebWorker = true,
            isTouchDevice = true,
            isWebkitApple = true,
        )
        assertTrue(Env.supportsSVG(browserHost))
        assertFalse(Env.supportsCanvas(browserHost))
        assertTrue(Env.isWebWorker(browserHost))
        assertTrue(Env.isTouchDevice(browserHost))
        assertTrue(Env.isWebkitApple(browserHost))
    }

    @Test
    fun eventRegistryRetainsOriginAndRemovesInReverseOrder() {
        val target = RecordingEventTarget()
        val owner = EnvEventOwner()
        val observed = mutableListOf<String>()
        val first = EnvEventHandler {
            observed += "first:$it"
            Unit
        }
        val second = EnvEventHandler {
            observed += "second:$it"
            Unit
        }

        val firstRegistration = assertIs<GMResult.Ok<EnvEventRegistration>>(
            Env.addEvent(
                target = target,
                type = "pointerdown",
                handler = first,
                owner = owner,
                options = EnvEventOptions(passive = false),
            ),
        ).value
        assertIs<GMResult.Ok<EnvEventRegistration>>(
            Env.addEvent(target, "pointerdown", second, owner),
        )
        assertSame(first, firstRegistration.origin)
        assertEquals(2, target.listeners.size)

        target.listeners.first().second.invoke("event")
        assertEquals(listOf("first:event"), observed)
        assertEquals(
            true,
            assertIs<GMResult.Ok<Boolean>>(
                Env.removeEvent(target, "pointerdown", first, owner),
            ).value,
        )
        assertEquals(1, target.listeners.size)
        assertEquals(
            1,
            assertIs<GMResult.Ok<Int>>(
                Env.removeAllEvents(target, "pointerdown", owner),
            ).value,
        )
        assertTrue(target.listeners.isEmpty())
        assertTrue(owner.registrations.isEmpty())
        assertEquals(
            false,
            assertIs<GMResult.Ok<Boolean>>(
                Env.removeEvent(target, "pointerdown", first, owner),
            ).value,
        )
    }

    @Test
    fun offsetWalkMatchesOfficialParentAndOffsetParentTraversal() {
        val root = EnvOffsetNode(
            offsetLeft = 100.0,
            offsetTop = 50.0,
            styles = mapOf("transform" to "translate(2,3)"),
        )
        val middle = EnvOffsetNode(
            clientLeft = 4.0,
            clientTop = 5.0,
            scrollLeft = 1.0,
            scrollTop = 2.0,
            parentNode = root,
            styles = mapOf("zoom" to "2"),
        )
        val node = EnvOffsetNode(
            offsetLeft = 10.0,
            offsetTop = 20.0,
            scrollLeft = 1.0,
            scrollTop = 2.0,
            offsetParent = root,
            parentNode = middle,
            styles = mapOf("transform" to "translateX(3)"),
        )

        assertContentEquals(
            doubleArrayOf(234.0, 148.0),
            position(Env.getOffset(node)),
        )
        assertIs<GMResult.Err<EnvError.InvalidOffsetHierarchy>>(
            Env.getOffset(
                node.copy(
                    offsetParent = EnvOffsetNode(offsetLeft = 1.0),
                    parentNode = null,
                ),
            ),
        )
    }

    @Test
    fun timedChunkPreservesOrderAndReportsProcessingFailure() {
        val scheduler = QueueScheduler()
        var now = 0L
        val processed = mutableListOf<Int>()
        var completion: GMResult<List<Int>, EnvError>? = null

        Env.timedChunk(
            items = listOf(1, 2, 3),
            process = {
                processed += it
                now += 200L
            },
            scheduler = scheduler,
            clock = EnvClock { now },
            callback = { completion = it },
        )
        assertEquals(1, scheduler.pendingCount)
        scheduler.runAll()
        assertEquals(listOf(1, 2, 3), processed)
        assertEquals(
            listOf(1, 2, 3),
            assertIs<GMResult.Ok<List<Int>>>(completion).value,
        )
        assertEquals(2, scheduler.executionCount)

        val failedScheduler = QueueScheduler()
        var failed: GMResult<List<Int>, EnvError>? = null
        Env.timedChunk(
            items = listOf(1, 2),
            process = { error("failure-$it") },
            scheduler = failedScheduler,
            clock = EnvClock { 0L },
            callback = { failed = it },
        )
        failedScheduler.runAll()
        val failure =
            assertIs<GMResult.Err<EnvError.TimedChunkFailure>>(failed)
        assertEquals(0, failure.error.processedCount)
        assertEquals("failure-1", failure.error.message)
    }

    @Test
    fun fullscreenLayoutMatchesOfficialScalingGeometry() {
        val layout = assertIs<GMResult.Ok<EnvFullscreenLayout>>(
            Env.scaleJSXGraphDiv(
                wrap = EnvDimensions(width = 1_200.0, height = 800.0),
                inner = EnvDimensions(width = 600.0, height = 400.0),
                scale = 0.9,
                originalTransform = "rotate(10deg)",
            ),
        ).value
        assertEquals(600.0, layout.width)
        assertEquals(400.0, layout.height)
        assertEquals(1.8, layout.scale)
        assertEquals(200.0, layout.verticalShift)
        assertEquals(
            "rotate(10deg) matrix(1.8,0,0,1.8,0,200.0)",
            layout.transform,
        )

        val constrained = assertIs<GMResult.Ok<EnvFullscreenLayout>>(
            Env.scaleJSXGraphDiv(
                wrap = EnvDimensions(width = 500.0, height = 300.0),
                inner = EnvDimensions(width = 1_000.0, height = 1_000.0),
                scale = 0.8,
            ),
        ).value
        assertEquals(240.0, constrained.width)
        assertEquals(240.0, constrained.height)
        assertEquals(1.0, constrained.scale)
        assertEquals(30.0, constrained.verticalShift)
        assertIs<GMResult.Err<EnvError.InvalidFullscreenDimensions>>(
            Env.scaleJSXGraphDiv(
                wrap = EnvDimensions(width = 0.0, height = 300.0),
                inner = EnvDimensions(width = 100.0, height = 100.0),
                scale = 1.0,
            ),
        )
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

    private class RecordingEventTarget : EnvEventTarget {
        val listeners =
            mutableListOf<Pair<String, EnvEventHandler>>()

        override fun addEventListener(
            type: String,
            listener: EnvEventHandler,
            options: EnvEventOptions,
        ): GMResult<Unit, EnvEventTargetError> {
            listeners += type to listener
            return GMResult.Ok(Unit)
        }

        override fun removeEventListener(
            type: String,
            listener: EnvEventHandler,
            options: EnvEventOptions,
        ): GMResult<Unit, EnvEventTargetError> {
            listeners.removeAll { entry ->
                entry.first == type && entry.second === listener
            }
            return GMResult.Ok(Unit)
        }
    }

    private class QueueScheduler : EnvTimeoutScheduler {
        private val tasks = ArrayDeque<() -> Unit>()
        var executionCount: Int = 0
            private set

        val pendingCount: Int
            get() = tasks.size

        override fun schedule(
            delayMilliseconds: Long,
            task: () -> Unit,
        ) {
            tasks.addLast(task)
        }

        fun runAll() {
            while (tasks.isNotEmpty()) {
                executionCount += 1
                tasks.removeFirst().invoke()
            }
        }
    }
}
