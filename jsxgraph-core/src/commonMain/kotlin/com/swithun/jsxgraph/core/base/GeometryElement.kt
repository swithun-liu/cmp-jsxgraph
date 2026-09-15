/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/element.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

/**
 * Initial lifecycle and dependency slice of JXG.GeometryElement.
 *
 * Visual properties, renderer nodes, transformations, labels, traces, and
 * animations remain in the untranslated element model. This class stays
 * internal until those contracts are available.
 */
internal open class GeometryElement(
    internal val board: Board,
    internal var id: String = "",
    name: String? = null,
    internal val type: Int = 0,
    internal val elementClass: Int = Const.OBJECT_CLASS_OTHER,
    internal var needsRegularUpdate: Boolean = true,
) {
    internal var name: String = name ?: ""
        private set
    private var needsGeneratedName: Boolean = name == null

    internal var needsUpdate: Boolean = true
    internal var positionInBoard: Int = -1

    internal val childElements = linkedMapOf<String, GeometryElement>()
    internal val descendants = linkedMapOf<String, GeometryElement>()
    internal val ancestors = linkedMapOf<String, GeometryElement>()
    internal val parents = mutableListOf<String>()

    // JSXGraph: src/base/element.js -> GeometryElement name initialization
    internal fun finalizeName() {
        if (needsGeneratedName) {
            name = board.generateName(this)
            needsGeneratedName = false
        }
        if (name.isNotEmpty()) {
            board.elementsByName[name] = this
        }
    }

    // JSXGraph: src/base/element.js -> _set("name", value)
    internal fun setName(value: String): GeometryElement {
        board.elementsByName.remove(name)
        name = value
        needsGeneratedName = false
        board.elementsByName[name] = this
        return this
    }

    // JSXGraph: src/base/element.js -> addChild
    internal fun addChild(element: GeometryElement): GeometryElement {
        childElements[element.id] = element
        addDescendants(element)
        element.ancestors[id] = this

        for (descendant in descendants.values) {
            descendant.ancestors[id] = this
            for (ancestor in ancestors.values) {
                descendant.ancestors[ancestor.id] = ancestor
            }
        }
        for (ancestor in ancestors.values) {
            for (descendant in descendants.values) {
                ancestor.descendants[descendant.id] = descendant
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> addDescendants
    private fun addDescendants(element: GeometryElement) {
        descendants[element.id] = element
        for (child in element.childElements.values) {
            addDescendants(child)
        }
    }

    // JSXGraph: src/base/element.js -> addParents
    internal fun addParents(elements: Iterable<GeometryElement>): GeometryElement {
        for (element in elements) {
            if (element.id !in parents) {
                parents += element.id
            }
        }
        return this
    }

    // JSXGraph: src/base/element.js -> setParents
    internal fun setParents(elements: Iterable<GeometryElement>): GeometryElement {
        parents.clear()
        return addParents(elements)
    }

    // JSXGraph: src/base/element.js -> removeChild
    internal fun removeChild(element: GeometryElement): GeometryElement {
        childElements.remove(element.id)
        removeDescendants(element)
        element.ancestors.remove(id)
        return this
    }

    // JSXGraph: src/base/element.js -> removeDescendants
    private fun removeDescendants(element: GeometryElement) {
        descendants.remove(element.id)
        for (child in element.childElements.values) {
            removeDescendants(child)
        }
    }

    // JSXGraph: src/base/element.js -> countChildren
    internal fun countChildren(): Int =
        childElements.keys.count { childId -> "Label" !in childId }

    // JSXGraph: src/base/element.js -> prepareUpdate
    internal open fun prepareUpdate(): GeometryElement {
        needsUpdate = true
        return this
    }

    // JSXGraph: src/base/element.js -> update
    internal open fun update(fromParent: Boolean = true): GeometryElement = this

    // JSXGraph: src/base/element.js -> updateVisibility
    internal open fun updateVisibility(): GeometryElement = this

    // JSXGraph: src/base/element.js -> updateRenderer
    internal open fun updateRenderer(): GeometryElement = this

    // JSXGraph: src/base/element.js -> remove
    internal open fun remove(): GeometryElement = this

    // JSXGraph: src/base/element.js -> fullUpdate
    internal fun fullUpdate(): GeometryElement =
        prepareUpdate()
            .update()
            .updateVisibility()
            .updateRenderer()
}
