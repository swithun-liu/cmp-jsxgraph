/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/composition.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

/**
 * Non-rendered container for a set of board elements.
 *
 * Like JXG.Composition, this object is not registered on a Board. Its members
 * remain ordinary, independently registered geometry elements.
 */
internal open class Composition(
    members: Map<String, GeometryElement> = emptyMap(),
) {
    internal val elements = linkedMapOf<String, GeometryElement>()
    internal val objects: Map<String, GeometryElement>
        get() = elements
    internal val objectsList = mutableListOf<GeometryElement>()
    internal val elementsByName: Map<String, GeometryElement>
        get() = objectsList.associateByTo(linkedMapOf()) { it.name }
    internal val groups = linkedMapOf<String, Any>()
    internal var dump: Boolean = true
    internal var elType: String = ""
    internal val subs = linkedMapOf<String, GeometryElement>()

    private val membersByRole = linkedMapOf<String, GeometryElement>()

    init {
        for ((role, element) in members) {
            add(role, element)
        }
    }

    // JSXGraph: src/base/composition.js -> add
    internal fun add(
        role: String,
        element: GeometryElement,
    ): Boolean {
        if (role in RESERVED_MEMBER_NAMES || role in membersByRole) {
            return false
        }
        elements[element.id] = element
        objectsList += element
        membersByRole[role] = element
        return true
    }

    // JSXGraph: src/base/composition.js -> remove
    internal fun remove(role: String): Boolean {
        val element = membersByRole[role] ?: return false
        val stored = elements.values.firstOrNull { it.id == element.id }
            ?: return false
        elements.remove(stored.id)
        objectsList.removeAll { it === stored }
        membersByRole.remove(role)
        return true
    }

    // JSXGraph: src/base/composition.js -> select
    internal fun select(reference: String): GeometryElement? =
        elements[reference] ?: elementsByName[reference]

    internal fun member(role: String): GeometryElement? = membersByRole[role]

    internal fun memberRole(element: GeometryElement): String? =
        membersByRole.entries.firstOrNull { it.value === element }?.key

    // JSXGraph: src/base/composition.js -> generated setParents
    internal fun setParents(parents: Iterable<GeometryElement>): Composition {
        for (element in objectsList) {
            element.setParents(parents)
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> generated prepareUpdate
    internal fun prepareUpdate(): Composition {
        for (element in objectsList) {
            element.prepareUpdate()
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> generated update
    internal fun update(): Composition {
        for (element in objectsList) {
            element.update()
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> generated fullUpdate
    internal fun fullUpdate(): Composition {
        for (element in objectsList) {
            element.fullUpdate()
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> generated updateRenderer
    internal fun updateRenderer(): Composition {
        for (element in objectsList) {
            element.updateRenderer()
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> getParents
    internal fun getParents(): List<String>? = null

    // JSXGraph: src/base/composition.js -> getType
    internal fun getType(): String = elType

    private companion object {
        val RESERVED_MEMBER_NAMES = setOf(
            "elements",
            "objects",
            "elementsByName",
            "objectsList",
            "groups",
            "subs",
            "dump",
            "elType",
            "add",
            "remove",
            "select",
            "setParents",
            "prepareUpdate",
            "update",
            "fullUpdate",
            "updateRenderer",
            "getParents",
            "getType",
        )
    }
}
