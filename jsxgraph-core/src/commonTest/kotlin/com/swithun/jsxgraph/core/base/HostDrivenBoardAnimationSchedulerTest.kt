package com.swithun.jsxgraph.core.base

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostDrivenBoardAnimationSchedulerTest {
    @Test
    fun advancesDueAnimationsAtMostOncePerHostFrame() {
        val scheduler = HostDrivenBoardAnimationScheduler()
        var calls = 0
        scheduler.schedule(intervalMilliseconds = 25.0) {
            calls += 1
            calls < 3
        }

        assertTrue(scheduler.hasScheduledAnimations)
        assertTrue(scheduler.advance(10.0))
        assertEquals(0, calls)
        assertTrue(scheduler.advance(15.0))
        assertEquals(1, calls)

        assertTrue(scheduler.advance(100.0))
        assertEquals(2, calls)
        assertFalse(scheduler.advance(25.0))
        assertEquals(3, calls)
        assertFalse(scheduler.hasScheduledAnimations)
    }

    @Test
    fun defersTasksScheduledByAnAnimationUntilTheNextHostFrame() {
        val scheduler = HostDrivenBoardAnimationScheduler()
        val calls = mutableListOf<String>()
        scheduler.schedule(intervalMilliseconds = 20.0) {
            calls += "outer"
            scheduler.schedule(intervalMilliseconds = 20.0) {
                calls += "inner"
                false
            }
            false
        }

        assertTrue(scheduler.advance(20.0))
        assertEquals(listOf("outer"), calls)
        assertFalse(scheduler.advance(20.0))
        assertEquals(listOf("outer", "inner"), calls)
    }
}
