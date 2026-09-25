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
    internal val maxNameLength: Int = 1,
    boundingBox: DoubleArray = doubleArrayOf(
        -originX / (unitX * zoomX),
        originY / (unitY * zoomY),
        (DEFAULT_CANVAS_WIDTH - originX) / (unitX * zoomX),
        (originY - DEFAULT_CANVAS_HEIGHT) / (unitY * zoomY),
    ),
    internal val defaultCurveMinimum: Double =
        (-DEFAULT_CANVAS_WIDTH * CURVE_DOMAIN_PADDING - originX) /
            (unitX * zoomX),
    internal val defaultCurveMaximum: Double =
        (
            DEFAULT_CANVAS_WIDTH * (1.0 + CURVE_DOMAIN_PADDING) -
                originX
            ) / (unitX * zoomX),
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

    private val boundingBox = boundingBox.copyOf()

    internal val objects = linkedMapOf<String, GeometryElement>()
    internal val objectsList = mutableListOf<GeometryElement>()
    internal val elementsByName = linkedMapOf<String, GeometryElement>()
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
    ): GMResult<String, BoardError> = setId(element, type.toString())

    // JSXGraph: src/base/board.js -> setId
    internal fun setId(
        element: GeometryElement,
        type: String,
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
        element.finalizeName()
        return GMResult.Ok(elementId)
    }

    internal fun elementById(id: String): GeometryElement? = objects[id]

    internal fun elementByName(name: String): GeometryElement? =
        elementsByName[name]

    // JSXGraph: src/base/board.js -> getBoundingBox
    internal fun getBoundingBox(): DoubleArray = boundingBox.copyOf()

    // JSXGraph: src/base/board.js -> generateName
    internal fun generateName(element: GeometryElement): String {
        if (element.type == Const.OBJECT_TYPE_TICKS || maxNameLength <= 0) {
            return ""
        }

        val possibleNames = when {
            element.elementClass == Const.OBJECT_CLASS_POINT ||
                element.type == Const.OBJECT_TYPE_POINT3D -> CAPITAL_NAMES
            element.type == Const.OBJECT_TYPE_ANGLE -> ANGLE_NAMES
            else -> LOWERCASE_NAMES
        }
        val (prefix, suffix) = when {
            element.elementClass == Const.OBJECT_CLASS_POINT ||
                element.type == Const.OBJECT_TYPE_POINT3D ||
                element.elementClass == Const.OBJECT_CLASS_LINE ||
                element.type == Const.OBJECT_TYPE_ANGLE -> "" to ""
            element.type == Const.OBJECT_TYPE_POLYGON -> "P_{" to "}"
            element.elementClass == Const.OBJECT_CLASS_CIRCLE -> "k_{" to "}"
            element.elementClass == Const.OBJECT_CLASS_TEXT -> "t_{" to "}"
            else -> "s_{" to "}"
        }
        val indices = IntArray(maxNameLength)

        while (indices[maxNameLength - 1] < possibleNames.size) {
            for (firstIndex in 1 until possibleNames.size) {
                indices[0] = firstIndex
                val candidate = buildString {
                    append(prefix)
                    for (index in maxNameLength downTo 1) {
                        append(possibleNames[indices[index - 1]])
                    }
                    append(suffix)
                }
                if (candidate !in elementsByName) {
                    return candidate
                }
            }

            indices[0] = possibleNames.size
            for (index in 1 until maxNameLength) {
                if (indices[index - 1] == possibleNames.size) {
                    indices[index - 1] = 1
                    indices[index] += 1
                }
            }
        }

        return ""
    }

    // JSXGraph: src/base/board.js -> select
    internal fun select(reference: String): GeometryElement? {
        if (reference.isEmpty()) {
            return null
        }
        return objects[reference] ?: elementsByName[reference]
    }

    // JSXGraph: src/base/board.js -> select
    internal fun select(element: GeometryElement?): GeometryElement? {
        if (element == null || element.id !in objects) {
            return null
        }
        return element
    }

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
        elementsByName.remove(element.name)
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
        elementReference: String,
        saveMethod: Boolean = false,
    ): Board = removeObject(select(elementReference), saveMethod)

    // JSXGraph: src/base/board.js -> _removeObj object.objects branch
    internal fun removeObject(
        composition: Composition,
        saveMethod: Boolean = false,
    ): Board {
        for (element in composition.objects.values.toList()) {
            removeElement(element, saveMethod)
        }
        update()
        return this
    }

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

    // JSXGraph: src/base/board.js -> removeAncestors
    internal fun removeAncestors(element: GeometryElement): Board {
        for (ancestor in element.ancestors.values.toList()) {
            removeAncestors(ancestor)
        }
        removeObject(element)
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
        var index = 0
        while (index < objectsList.size) {
            val element = objectsList[index]
            element
                .update(
                    fromParent =
                        draggedElement == null ||
                            element.id != draggedElement.id,
                )
                .updateVisibility()
            index += 1
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

    private companion object {
        const val DEFAULT_CANVAS_WIDTH = 500.0
        const val DEFAULT_CANVAS_HEIGHT = 500.0
        const val CURVE_DOMAIN_PADDING = 0.1

        val CAPITAL_NAMES = listOf(
            "",
            "A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M",
            "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z",
        )
        val LOWERCASE_NAMES = listOf(
            "",
            "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m",
            "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z",
        )
        val ANGLE_NAMES = listOf(
            "",
            "&alpha;", "&beta;", "&gamma;", "&delta;", "&epsilon;", "&zeta;",
            "&eta;", "&theta;", "&iota;", "&kappa;", "&lambda;", "&mu;", "&nu;",
            "&xi;", "&omicron;", "&pi;", "&rho;", "&sigma;", "&tau;", "&upsilon;",
            "&phi;", "&chi;", "&psi;", "&omega;",
        )
    }
}
