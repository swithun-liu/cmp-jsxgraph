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

    // JSXGraph: src/3d/element3d.js -> setAttr2D.
    internal fun setAttr2D(
        attributes3D: MutableMap<String, Any?>,
    ): MutableMap<String, Any?> {
        attributes3D["name"] = name
        attributes3D["element3d"] = this
        attributes3D["id"] = null
        return attributes3D
    }

    // JSXGraph: src/3d/element3d.js -> setAttribute.
    override fun setAttribute(
        attributes: Map<String, Any?>,
    ): GeometryElement3D {
        super.setAttribute(attributes)
        val delegated = attributes.filterKeys { rawKey ->
            normalize3DAttributeKey(rawKey) !in LOCAL_3D_ATTRIBUTES
        }
        if (delegated.isNotEmpty()) {
            element2D?.setAttribute(delegated)
        }
        return this
    }

    // JSXGraph: src/3d/element3d.js -> getAttribute.
    override fun getAttribute(key: String): Any? {
        val normalized = normalize3DAttributeKey(key)
        return if (normalized in LOCAL_3D_ATTRIBUTES) {
            super.getAttribute(normalized)
        } else {
            element2D?.getAttribute(normalized)
                ?: super.getAttribute(normalized)
        }
    }

    // JSXGraph: src/3d/element3d.js -> getAttributes.
    override fun getAttributes(): Map<String, Any?> {
        val result = linkedMapOf<String, Any?>()
        result.putAll(element2D?.getAttributes().orEmpty())
        result.remove("id")
        result.remove("name")
        for (key in LOCAL_3D_ATTRIBUTES) {
            if (key in visualProperties) {
                result[key] = visualProperties[key]
            }
        }
        return result
    }

    /*
     * JSXGraph: src/3d/element3d.js -> setPosition2D / projectCoords.
     * These upstream base methods are intentional stubs. Kotlin keeps the
     * executable contracts on concrete 3D element types, where result and
     * error types can remain explicit.
     */

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

    private fun normalize3DAttributeKey(key: String): String =
        key.filterNot(Char::isWhitespace).lowercase()

    private companion object {
        val LOCAL_3D_ATTRIBUTES = setOf(
            "fillcolor",
            "numberpointshigh",
            "stepsu",
            "stepsv",
        )
    }
}
