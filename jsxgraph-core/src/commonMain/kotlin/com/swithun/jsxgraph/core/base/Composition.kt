/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/composition.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.DumpUndefined
import com.swithun.jsxgraph.core.utils.EventHandler
import com.swithun.jsxgraph.core.utils.Type
import com.swithun.jsxgraph.core.utils.TypeElementPropertyAccessor
import com.swithun.jsxgraph.core.utils.TypeError
import com.swithun.jsxgraph.core.utils.TypeFilterCriterion
import com.swithun.jsxgraph.core.utils.TypeFilterPredicate

/**
 * Non-rendered container for a set of board elements.
 *
 * Like JXG.Composition, this object is not registered on a Board. Its members
 * remain ordinary, independently registered geometry elements.
 */
internal open class Composition(
    members: Map<String, GeometryElement> = emptyMap(),
    attributes: Map<String, Any?> = emptyMap(),
) {
    internal val elements = linkedMapOf<String, GeometryElement>()
    internal val objects: Map<String, GeometryElement>
        get() = elements
    internal val objectsList = mutableListOf<GeometryElement>()
    internal val elementsByName = linkedMapOf<String, GeometryElement>()
    internal val groups = linkedMapOf<String, Any>()
    internal var dump: Boolean = true
    internal var elType: String = ""
    internal val subs = linkedMapOf<String, GeometryElement>()
    internal val attributes =
        attributes.mapKeysTo(linkedMapOf()) { (key, _) ->
            key.lowercase()
        }

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
        elementsByName[element.name] = element
        element.on(
            event = "attribute:name",
            handler = EventHandler { _, arguments ->
                val oldName = arguments.getOrNull(0) as? String ?: return@EventHandler
                val newName = arguments.getOrNull(1) as? String ?: return@EventHandler
                val renamed = arguments.getOrNull(2) as? GeometryElement
                    ?: return@EventHandler
                nameListener(oldName, newName, renamed)
            },
            context = this,
        )
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
        elementsByName.remove(element.name)
        objectsList.remove(stored)
        membersByRole.remove(role)
        return true
    }

    // JSXGraph: src/base/composition.js -> select
    internal fun select(reference: String): Any =
        elements[reference] ?: elementsByName[reference] ?: reference

    // JSXGraph 1.13.3: src/base/board.js -> select object branch.
    internal fun select(element: GeometryElement?): GeometryElement? {
        if (element == null || elements[element.id] !== element) {
            return null
        }
        return element
    }

    // JSXGraph 1.13.3: src/base/board.js -> select function filter branch.
    internal fun select(
        predicate: TypeFilterPredicate<GeometryElement>,
    ): GMResult<Composition, TypeError> =
        when (val filtered = Type.filterElements(objectsList, predicate)) {
            is GMResult.Ok -> GMResult.Ok(filteredComposition(filtered.value))
            is GMResult.Err -> filtered
        }

    // JSXGraph 1.13.3: src/base/board.js -> select object filter branch.
    internal fun select(
        filter: Map<String, TypeFilterCriterion>,
    ): GMResult<Composition, TypeError> =
        when (
            val filtered = Type.filterElements(
                values = objectsList,
                filter = filter,
                accessor = CompositionElementPropertyAccessor,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(filteredComposition(filtered.value))
            is GMResult.Err -> filtered
        }

    internal fun member(role: String): GeometryElement? = membersByRole[role]

    internal fun memberRole(element: GeometryElement): String? =
        membersByRole.entries.firstOrNull { it.value === element }?.key

    // JSXGraph 1.13.3: src/base/composition.js -> generateMethod.
    // Kotlin exposes the generated forwarding closures as typed methods.
    // JSXGraph: src/base/composition.js -> generated setAttribute.
    internal open fun setAttribute(
        values: Map<String, Any?>,
    ): Composition {
        for (element in objectsList) {
            element.setAttribute(values)
        }
        for ((key, value) in values) {
            attributes[key.lowercase()] = value
        }
        return this
    }

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

    // JSXGraph: src/base/composition.js -> generated highlight
    internal fun highlight(force: Boolean = false): Composition {
        for (element in objectsList) {
            element.highlight(force)
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> generated noHighlight
    internal fun noHighlight(): Composition {
        for (element in objectsList) {
            element.noHighlight()
        }
        return this
    }

    // JSXGraph: src/base/composition.js -> getParents
    internal open fun getParents(): List<String>? = null

    // JSXGraph: src/base/composition.js -> getType
    internal fun getType(): String = elType

    // JSXGraph: src/base/composition.js -> nameListener.
    internal fun nameListener(
        oldName: String,
        newName: String,
        element: GeometryElement,
    ) {
        elementsByName.remove(oldName)
        elementsByName[newName] = element
    }

    // JSXGraph: src/base/composition.js -> getAttributes.
    internal fun getAttributes(): Map<String, Any?> =
        attributes.toMap()

    private fun filteredComposition(
        filtered: List<GeometryElement>,
    ): Composition =
        Composition(
            members = filtered.associateByTo(linkedMapOf()) { it.id },
        )

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
            "setAttribute",
            "setParents",
            "prepareUpdate",
            "update",
            "fullUpdate",
            "updateRenderer",
            "highlight",
            "noHighlight",
            "getParents",
            "getType",
            "getAttributes",
            "nameListener",
        )

        val CompositionElementPropertyAccessor =
            object : TypeElementPropertyAccessor<GeometryElement> {
                override fun readProperty(
                    element: GeometryElement,
                    property: String,
                ): GMResult<Any?, TypeError> =
                    GMResult.Ok(
                        when (property) {
                            "id" -> element.id
                            "name" -> element.name
                            "elType" -> element.elType
                            "type" -> element.type
                            "elementClass" -> element.elementClass
                            "X" -> (element as? CoordsElement)?.X()
                                ?: DumpUndefined
                            "Y" -> (element as? CoordsElement)?.Y()
                                ?: DumpUndefined
                            "Z" -> (element as? CoordsElement)?.Z()
                                ?: DumpUndefined
                            else -> DumpUndefined
                        },
                    )

                override fun readVisualProperty(
                    element: GeometryElement,
                    property: String,
                ): GMResult<Any?, TypeError> =
                    GMResult.Ok(
                        if (property in element.visualProperties) {
                            element.evalVisProp(property)
                        } else {
                            DumpUndefined
                        },
                    )
            }
    }
}
