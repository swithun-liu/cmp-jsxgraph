/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/event.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

fun interface EventHandler {
    fun handle(context: Any, arguments: List<Any?>)
}

class EventEmitter(
    private val owner: Any,
) {
    private data class RegisteredHandler(
        val handler: EventHandler,
        val context: Any,
    )

    private val eventHandlers = mutableMapOf<String, MutableList<RegisteredHandler>>()
    private val suspended = mutableSetOf<String>()

    // JSXGraph: src/utils/event.js -> trigger
    fun trigger(
        events: List<String>,
        arguments: List<Any?>,
    ): EventEmitter {
        for (event in events) {
            if (event in suspended) {
                continue
            }

            suspended += event
            val handlers = eventHandlers[event]
            if (handlers != null) {
                val handlerCount = handlers.size
                var index = 0
                while (index < handlerCount) {
                    val registered = handlers[index]
                    registered.handler.handle(registered.context, arguments)
                    index += 1
                }
            }
            suspended -= event
        }
        return this
    }

    // JSXGraph: src/utils/event.js -> on
    fun on(
        event: String,
        handler: EventHandler,
        context: Any = owner,
    ): EventEmitter {
        eventHandlers.getOrPut(event) { mutableListOf() } +=
            RegisteredHandler(handler = handler, context = context)
        return this
    }

    // JSXGraph: src/utils/event.js -> off
    fun off(
        event: String,
        handler: EventHandler? = null,
    ): EventEmitter {
        val handlers = eventHandlers[event] ?: return this
        if (handler == null) {
            eventHandlers.remove(event)
            return this
        }

        val index = handlers.indexOfFirst { it.handler === handler }
        if (index >= 0) {
            handlers.removeAt(index)
        }
        if (handlers.isEmpty()) {
            eventHandlers.remove(event)
        }
        return this
    }
}
