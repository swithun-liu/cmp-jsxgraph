/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/polygon3d.js
 * Copyright 2008-2026 Matthias Ehmann, Aaron Fenyes, Carsten Miller,
 * Andreas Walter, and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat

internal data class Polygon3DVertexAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val fixed: Boolean = false,
    val withLabel: Boolean = true,
)

internal sealed interface Polygon3DError {
    data class VertexLimitExceeded(
        val count: Int,
        val maximum: Int,
    ) : Polygon3DError

    data class ParentViewMismatch(
        val parentIndex: Int,
    ) : Polygon3DError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : Polygon3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Polygon3DError

    data class VertexFactory(
        val vertexIndex: Int,
        val error: Point3DError,
    ) : Polygon3DError

    data class Registration(
        val error: BoardError,
    ) : Polygon3DError

    data class ProxyPolygonFactory(
        val error: PolygonError,
    ) : Polygon3DError
}

/**
 * A JSXGraph Polygon3D and its ordinary Polygon proxy.
 */
internal class Polygon3D private constructor(
    view: View3D,
    internal val vertices: MutableList<Point3D>,
    internal val ownedVertices: MutableSet<Point3D>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    internal val withLines: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POLYGON3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal lateinit var polygon2D: Polygon
        private set
    internal val inherits = mutableListOf<GeometryElement>()

    init {
        elType = POLYGON_3D_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph: src/3d/polygon3d.js -> update.
    override fun update(fromParent: Boolean): Polygon3D = this

    // JSXGraph: src/3d/polygon3d.js -> updateRenderer.
    override fun updateRenderer(): Polygon3D {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/polygon3d.js -> updateZIndex.
    internal fun updateZIndex(): Polygon3D {
        if (vertices.isEmpty()) {
            zIndex = Double.NaN
            return this
        }
        val centroid = doubleArrayOf(1.0, 0.0, 0.0, 0.0)
        for (vertex in vertices) {
            centroid[1] += vertex.coords[1]
            centroid[2] += vertex.coords[2]
            centroid[3] += vertex.coords[3]
        }
        for (index in 1..3) {
            centroid[index] /= vertices.size
        }
        zIndex = Mat.innerProduct(
            view.matrix3DRotShift[3],
            centroid,
        )
        return this
    }

    override fun remove(): GeometryElement {
        vertices.clear()
        ownedVertices.clear()
        inherits.clear()
        return super.remove()
    }

    internal companion object {
        internal const val MAX_VERTEX_COUNT: Int = 10_000
        private const val POLYGON_3D_ID_PREFIX = "polygon3d"
        private const val POLYGON_3D_ELEMENT_TYPE = "polygon3d"

        // JSXGraph: src/3d/polygon3d.js -> createPolygon3D.
        internal fun create(
            view: View3D,
            vertices: List<Point3D>,
            ownedVertices: Set<Point3D> = emptySet(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            withLines: Boolean = true,
        ): GMResult<Polygon3D, Polygon3DError> {
            validateVertices(view, vertices)?.let { error ->
                cleanupOwnedVertices(view, ownedVertices)
                return GMResult.Err(error)
            }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                cleanupOwnedVertices(view, ownedVertices)
                return GMResult.Err(
                    Polygon3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val polygon = Polygon3D(
                view = view,
                vertices = vertices.toMutableList(),
                ownedVertices = ownedVertices.toMutableSet(),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                withLines = withLines,
            )
            when (
                val registration = view.board.setId(
                    polygon,
                    POLYGON_3D_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    cleanupOwnedVertices(view, ownedVertices)
                    return GMResult.Err(
                        Polygon3DError.Registration(registration.error),
                    )
                }
            }
            polygon.registerInView()

            val proxy = when (
                val result = Polygon.create(
                    board = view.board,
                    vertices = vertices.map(Point3D::point2D),
                    withLines = withLines,
                    name = "",
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    view.board.removeObject(polygon)
                    cleanupOwnedVertices(view, ownedVertices)
                    return GMResult.Err(
                        Polygon3DError.ProxyPolygonFactory(result.error),
                    )
                }
            }
            proxy.dump = false
            proxy.implicitVertices =
                ownedVertices.map(Point3D::point2D)
            proxy.setParents(listOf(polygon))
            polygon.polygon2D = proxy
            polygon.element2D = proxy
            polygon.addChild(proxy)
            polygon.inherits += proxy

            for (vertex in vertices.distinct()) {
                if (vertex in ownedVertices) {
                    polygon.addChild(vertex)
                } else {
                    vertex.addChild(polygon)
                }
            }
            polygon.updateZIndex()
            return GMResult.Ok(polygon)
        }

        // JSXGraph: src/3d/polygon3d.js -> createPolygon3D,
        // transformed parent form. The upstream 1.13.3 loop intentionally
        // iterates to base.vertices.length - 1.
        internal fun create(
            view: View3D,
            base: Polygon3D,
            transformations: List<Transformation>,
            vertexAttributes: Polygon3DVertexAttributes =
                Polygon3DVertexAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            withLines: Boolean = true,
        ): GMResult<Polygon3D, Polygon3DError> {
            if (base.view !== view) {
                return GMResult.Err(
                    Polygon3DError.ParentViewMismatch(parentIndex = 0),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Polygon3DError.InvalidTransformationCount(0),
                )
            }
            val transformedVertices = mutableListOf<Point3D>()
            for ((index, baseVertex) in
                base.vertices.dropLast(1).withIndex()
            ) {
                val vertexName =
                    if (vertexAttributes.withLabel) {
                        if (baseVertex.name.isEmpty()) {
                            ""
                        } else {
                            "${baseVertex.name}'"
                        }
                    } else {
                        vertexAttributes.name
                    }
                val vertex = when (
                    val result = Point3D.create(
                        view = view,
                        basePoint = baseVertex,
                        transformations = transformations,
                        id = vertexAttributes.id,
                        name = vertexName,
                        needsRegularUpdate =
                            vertexAttributes.needsRegularUpdate,
                        fixed = vertexAttributes.fixed,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        cleanupOwnedVertices(view, transformedVertices)
                        return GMResult.Err(
                            Polygon3DError.VertexFactory(
                                vertexIndex = index,
                                error = result.error,
                            ),
                        )
                    }
                }
                transformedVertices += vertex
            }
            val polygon = when (
                val result = create(
                    view = view,
                    vertices = transformedVertices,
                    ownedVertices = transformedVertices.toSet(),
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                    withLines = withLines,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            base.addChild(polygon)
            polygon.addParents(listOf(base))
            polygon.prepareUpdate().update().updateRenderer()
            for (vertex in transformedVertices) {
                vertex.prepareUpdate().update().updateRenderer()
            }
            polygon.updateZIndex()
            return GMResult.Ok(polygon)
        }

        private fun validateVertices(
            view: View3D,
            vertices: List<Point3D>,
        ): Polygon3DError? {
            if (vertices.size > MAX_VERTEX_COUNT) {
                return Polygon3DError.VertexLimitExceeded(
                    count = vertices.size,
                    maximum = MAX_VERTEX_COUNT,
                )
            }
            for ((index, vertex) in vertices.withIndex()) {
                if (vertex.view !== view) {
                    return Polygon3DError.ParentViewMismatch(index)
                }
                if (view.board.elementById(vertex.id) !== vertex) {
                    return Polygon3DError.ParentNotRegistered(
                        parentIndex = index,
                        id = vertex.id,
                    )
                }
            }
            return null
        }

        private fun cleanupOwnedVertices(
            view: View3D,
            vertices: Iterable<Point3D>,
        ) {
            view.board.removeObjects(vertices.toList().asReversed())
        }
    }
}
