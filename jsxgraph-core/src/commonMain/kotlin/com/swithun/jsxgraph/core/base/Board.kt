/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/board.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface BoardError {
    data class DuplicateElementId(val id: String) : BoardError
}

/**
 * Initial translated slice of JXG.Board.
 *
 * The coordinate-system state, object registry, and basic update loop are
 * present. The class remains internal until the public initBoard, renderer,
 * event, and complete element-factory contracts are translated.
 */
internal class Board(
    originX: Double,
    originY: Double,
    unitX: Double,
    unitY: Double,
    internal var zoomX: Double = 1.0,
    internal var zoomY: Double = 1.0,
    internal val id: String = "jxgBoard1",
) {
    internal class Origin(
        val usrCoords: DoubleArray,
        val scrCoords: DoubleArray,
    )

    // JSXGraph: src/base/board.js -> Board constructor origin initialization
    internal val origin = Origin(
        usrCoords = doubleArrayOf(1.0, 0.0, 0.0),
        scrCoords = doubleArrayOf(1.0, originX, originY),
    )

    // JSXGraph: src/base/board.js -> Board constructor unitX / unitY
    internal var unitX: Double = unitX * zoomX
    internal var unitY: Double = unitY * zoomY

    internal val objects = linkedMapOf<String, GeometryElement>()
    internal val objectsList = mutableListOf<GeometryElement>()
    internal var numObjects: Int = 0
        private set

    internal var needsFullUpdate: Boolean = false
        private set
    internal var inUpdate: Boolean = false
        private set
    internal var isSuspendedUpdate: Boolean = false
        private set

    // JSXGraph: src/base/board.js -> setId
    internal fun setId(
        element: GeometryElement,
        type: Int,
    ): GMResult<String, BoardError> {
        val creationIndex = numObjects
        val requestedId = element.id
        if (requestedId.isNotEmpty() && requestedId in objects) {
            return GMResult.Err(BoardError.DuplicateElementId(requestedId))
        }

        var elementId = requestedId
        if (elementId.isEmpty()) {
            val baseId = "$id$type$creationIndex"
            elementId = baseId
            var collisionIndex = 1
            while (elementId in objects) {
                elementId = "$baseId-$collisionIndex"
                collisionIndex += 1
            }
        }

        numObjects += 1
        element.id = elementId
        objects[elementId] = element
        element.positionInBoard = objectsList.size
        objectsList += element
        return GMResult.Ok(elementId)
    }

    internal fun elementById(id: String): GeometryElement? = objects[id]

    // JSXGraph: src/base/board.js -> _removeObj
    private fun removeElement(
        element: GeometryElement?,
        saveMethod: Boolean,
    ) {
        if (element == null || objects[element.id] !== element) {
            return
        }

        // The upstream recursive call intentionally does not forward
        // saveMethod, so descendants always use the ancestor-based path.
        for (child in element.childElements.values.toList()) {
            child.board.removeElement(child, saveMethod = false)
        }

        if (saveMethod) {
            for (candidate in objects.values) {
                candidate.childElements.remove(element.id)
                candidate.descendants.remove(element.id)
            }
        } else {
            for (ancestor in element.ancestors.values) {
                ancestor.childElements.remove(element.id)
                ancestor.descendants.remove(element.id)
            }
        }

        val position = element.positionInBoard
        if (
            position >= 0 &&
            position < objectsList.size &&
            objectsList[position] === element
        ) {
            objectsList.removeAt(position)
            for (index in position until objectsList.size) {
                objectsList[index].positionInBoard = index
            }
        }

        objects.remove(element.id)
        element.positionInBoard = -1
        element.remove()
    }

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObject(
        element: GeometryElement?,
        saveMethod: Boolean = false,
    ): Board {
        removeElement(element, saveMethod)
        update()
        return this
    }

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObject(
        elementId: String,
        saveMethod: Boolean = false,
    ): Board = removeObject(objects[elementId], saveMethod)

    // JSXGraph: src/base/board.js -> removeObject
    internal fun removeObjects(
        elements: Iterable<GeometryElement>,
        saveMethod: Boolean = false,
    ): Board {
        for (element in elements.toList()) {
            removeElement(element, saveMethod)
        }
        update()
        return this
    }

    // JSXGraph: src/base/board.js -> prepareUpdate
    internal fun prepareUpdate(): Board {
        for (element in objectsList) {
            element.needsUpdate =
                element.needsRegularUpdate || needsFullUpdate
        }
        return this
    }

    // JSXGraph: src/base/board.js -> updateElements
    internal fun updateElements(
        draggedElement: GeometryElement? = null,
    ): Board {
        for (element in objectsList) {
            element
                .update(
                    fromParent =
                        draggedElement == null ||
                            element.id != draggedElement.id,
                )
                .updateVisibility()
        }
        return this
    }

    // JSXGraph: src/base/board.js -> updateRenderer
    internal fun updateRenderer(): Board {
        for (element in objectsList) {
            element.updateRenderer()
        }
        return this
    }

    // JSXGraph: src/base/board.js -> update
    internal fun update(
        draggedElement: GeometryElement? = null,
    ): Board {
        if (inUpdate || isSuspendedUpdate) {
            return this
        }
        inUpdate = true
        try {
            prepareUpdate()
                .updateElements(draggedElement)
                .updateRenderer()
        } finally {
            inUpdate = false
        }
        return this
    }

    // JSXGraph: src/base/board.js -> fullUpdate
    internal fun fullUpdate(): Board {
        needsFullUpdate = true
        try {
            update()
        } finally {
            needsFullUpdate = false
        }
        return this
    }

    internal fun suspendUpdate(): Board {
        if (!inUpdate) {
            isSuspendedUpdate = true
        }
        return this
    }

    internal fun unsuspendUpdate(): Board {
        if (isSuspendedUpdate) {
            isSuspendedUpdate = false
            fullUpdate()
        }
        return this
    }
}
