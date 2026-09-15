/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/coordselement.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

/**
 * Initial coordinate-access slice of JXG.CoordsElement.
 *
 * Constraint, glider, transformation, animation, and renderer behavior remain
 * in the untranslated element model. This class stays internal until those
 * lifecycle contracts are available.
 */
internal open class CoordsElement(
    board: Board,
    coordinates: DoubleArray = doubleArrayOf(1.0, 0.0, 0.0),
    id: String = "",
    name: String? = null,
    type: Int = 0,
    elementClass: Int = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = type,
    elementClass = elementClass,
    needsRegularUpdate = needsRegularUpdate,
) {
    // JSXGraph: src/base/coordselement.js -> CoordsElement constructor.
    internal val coords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val initialCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal val actualCoords = Coords(
        method = Const.COORDS_BY_USER,
        coordinates = coordinates,
        board = board,
    )

    internal var position: Double? = null
    internal var isConstrained: Boolean = false
    internal var onPolygon: Boolean = false
    internal var slideObject: GeometryElement? = null
    internal val slideObjects = mutableListOf<GeometryElement>()
    internal var needsUpdateFromParent: Boolean = true

    init {
        isDraggable = true
    }

    internal val isReal: Boolean
        get() = coords.isReal()

    // JSXGraph: src/base/coordselement.js -> X.
    internal fun X(): Double = coords.usrCoords[1]

    // JSXGraph: src/base/coordselement.js -> Y.
    internal fun Y(): Double = coords.usrCoords[2]

    // JSXGraph: src/base/coordselement.js -> Z.
    internal fun Z(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Coords.
    internal fun Coords(withZ: Boolean = false): DoubleArray =
        if (withZ) {
            coords.usrCoords.copyOf()
        } else {
            coords.usrCoords.copyOfRange(1, coords.usrCoords.size)
        }

    // JSXGraph: src/base/coordselement.js -> XEval, YEval, ZEval.
    internal fun XEval(): Double = coords.usrCoords[1]

    internal fun YEval(): Double = coords.usrCoords[2]

    internal fun ZEval(): Double = coords.usrCoords[0]

    // JSXGraph: src/base/coordselement.js -> Dist.
    internal fun Dist(other: CoordsElement): Double =
        if (isReal && other.isReal) {
            coords.distance(Const.COORDS_BY_USER, other.coords)
        } else {
            Double.NaN
        }

    // JSXGraph: src/base/coordselement.js -> updateConstraint
    internal open fun updateConstraint(): CoordsElement = this

    // JSXGraph: src/base/coordselement.js -> updateTransform
    internal open fun updateTransform(fromParent: Boolean): CoordsElement = this

    // JSXGraph: src/base/coordselement.js -> updateCoords
    internal fun updateCoords(fromParent: Boolean = false): CoordsElement {
        if (!needsUpdate) {
            return this
        }

        /*
         * This is the free-element path. Frozen visual properties, glider
         * projection, and transformations are added with their owner models.
         */
        updateConstraint()
        updateTransform(fromParent)
        return this
    }

    /*
     * These hooks preserve setPositionDirectly's upstream call order. Their
     * attribute-driven algorithms are translated with the visual-property and
     * attractor models.
     */
    internal open fun handleSnapToGrid(): CoordsElement = this

    internal open fun handleSnapToPoints(): CoordsElement = this

    internal open fun handleAttractors(): CoordsElement = this

    // JSXGraph: src/base/coordselement.js -> setPositionDirectly
    internal fun setPositionDirectly(
        method: Int,
        coordinates: DoubleArray,
    ): CoordsElement {
        coords.setCoordinates(method, coordinates)
        handleSnapToGrid()
        handleSnapToPoints()
        handleAttractors()

        actualCoords.setCoordinates(
            coordType = Const.COORDS_BY_USER,
            coordinates = coords.usrCoords,
        )

        /*
         * relativeCoords and transformation preimages are intentionally not
         * represented until their owner models are translated.
         */
        prepareUpdate()
        update()
        return this
    }

    // JSXGraph: src/base/coordselement.js -> setPosition
    internal fun setPosition(
        method: Int,
        coordinates: DoubleArray,
    ): CoordsElement = setPositionDirectly(method, coordinates)
}
