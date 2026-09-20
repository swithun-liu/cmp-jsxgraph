/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/element3d.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

/**
 * Shared translated state for 3D geometry elements.
 *
 * JSXGraph renders a 3D element through one or more ordinary 2D elements.
 * The Kotlin model preserves that ownership boundary so Compose can keep
 * consuming the existing portable 2D scene.
 */
internal open class GeometryElement3D(
    internal val view: View3D,
    id: String = "",
    name: String? = null,
    type: Int,
    needsRegularUpdate: Boolean = true,
) : GeometryElement(
    board = view.board,
    id = id,
    name = name,
    type = type,
    elementClass = Const.OBJECT_CLASS_3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    // JSXGraph: src/3d/element3d.js -> GeometryElement3D constructor.
    internal var element2D: GeometryElement? = null
    internal val is3D: Boolean = true
    internal var zIndex: Double = 0.0

    internal fun registerInView(): GeometryElement3D {
        view.objects[id] = this
        if (name.isNotEmpty()) {
            view.elementsByName[name] = this
        }
        return this
    }

    // JSXGraph: src/3d/element3d.js -> addTransformGeneric.
    internal fun addTransformGeneric(
        element: GeometryElement,
        newTransformations: Iterable<Transformation>,
    ): GeometryElement3D {
        if (transformations.isEmpty()) {
            baseElement = element
        }
        transformations += newTransformations
        return this
    }

    // JSXGraph: src/3d/element3d.js -> removeTransformGeneric.
    internal fun removeTransformGeneric(
        removedTransformations: Iterable<Transformation>,
    ): GeometryElement3D {
        for (transformation in removedTransformations) {
            transformations.remove(transformation)
        }
        if (transformations.isEmpty()) {
            baseElement = null
        }
        return this
    }

    // JSXGraph: src/3d/element3d.js -> clearTransforms.
    internal fun clearTransformsGeneric(): GeometryElement3D {
        transformations.clear()
        baseElement = null
        return this
    }

    override fun remove(): GeometryElement {
        view.objects.remove(id)
        if (name.isNotEmpty()) {
            view.elementsByName.remove(name)
        }
        return this
    }
}
