package com.swithun.jsxgraph.core.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class EventEmitterTest {
    @Test
    fun triggerUsesRegisteredContextAndArguments() {
        val owner = Any()
        val context = Any()
        val emitter = EventEmitter(owner)
        var receivedContext: Any? = null
        var receivedArguments: List<Any?> = emptyList()

        emitter.on(
            event = "update",
            handler = EventHandler { actualContext, arguments ->
                receivedContext = actualContext
                receivedArguments = arguments
            },
            context = context,
        )
        emitter.trigger(
            events = listOf("update"),
            arguments = listOf(1, "two"),
        )

        assertSame(context, receivedContext)
        assertEquals(listOf(1, "two"), receivedArguments)
    }

    @Test
    fun offRemovesOnlyTheRequestedHandler() {
        val emitter = EventEmitter(Any())
        var firstCount = 0
        var secondCount = 0
        val first = EventHandler { _, _ -> firstCount += 1 }
        val second = EventHandler { _, _ -> secondCount += 1 }
        emitter.on("update", first)
        emitter.on("update", second)

        emitter.off("update", first)
        emitter.trigger(listOf("update"), emptyList())

        assertEquals(0, firstCount)
        assertEquals(1, secondCount)
    }

    @Test
    fun recursiveTriggerOfSameEventIsSuspended() {
        val emitter = EventEmitter(Any())
        var invocationCount = 0
        emitter.on(
            event = "update",
            handler = EventHandler { _, _ ->
                invocationCount += 1
                emitter.trigger(listOf("update"), emptyList())
            },
        )

        emitter.trigger(listOf("update"), emptyList())

        assertEquals(1, invocationCount)
    }
}
