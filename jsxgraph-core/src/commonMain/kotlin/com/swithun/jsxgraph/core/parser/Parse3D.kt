/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/parser/3dmodels.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat

data class Parse3DLimits(
    val maxSourceLength: Int = 1_000_000,
    val maxPolyhedra: Int = 10_000,
    val maxVerticesPerPolyhedron: Int = 10_000,
    val maxFacesPerPolyhedron: Int = 10_000,
    val maxVerticesPerFace: Int = 10_000,
)

data class ParsedPolyhedron3D(
    val vertices: List<List<Double>>,
    val faces: List<List<Int>>,
)

sealed interface Parse3DError {
    val message: String

    data class InvalidLimits(
        override val message: String,
    ) : Parse3DError

    data class SourceLengthExceeded(
        val limit: Int,
        val actual: Int,
    ) : Parse3DError {
        override val message = "STL source length $actual exceeds limit $limit"
    }

    data class PolyhedronLimitExceeded(
        val limit: Int,
    ) : Parse3DError {
        override val message = "STL polyhedron count exceeds limit $limit"
    }

    data class VertexLimitExceeded(
        val polyhedronIndex: Int,
        val limit: Int,
    ) : Parse3DError {
        override val message =
            "STL polyhedron $polyhedronIndex vertex count exceeds limit $limit"
    }

    data class FaceLimitExceeded(
        val polyhedronIndex: Int,
        val limit: Int,
    ) : Parse3DError {
        override val message =
            "STL polyhedron $polyhedronIndex face count exceeds limit $limit"
    }

    data class FaceVertexLimitExceeded(
        val polyhedronIndex: Int,
        val faceIndex: Int,
        val limit: Int,
    ) : Parse3DError {
        override val message =
            "STL polyhedron $polyhedronIndex face $faceIndex vertex count " +
                "exceeds limit $limit"
    }

    data class VertexOutsideFacet(
        val line: Int,
    ) : Parse3DError {
        override val message =
            "STL vertex at line $line appears before a facet"
    }
}

object Parse3D {
    // JSXGraph 1.13.3: src/parser/3dmodels.js -> JXG.Parse3D.STL.
    @Suppress("FunctionName")
    fun STL(
        source: String,
        limits: Parse3DLimits = Parse3DLimits(),
    ): GMResult<List<ParsedPolyhedron3D>, Parse3DError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        if (source.length > limits.maxSourceLength) {
            return GMResult.Err(
                Parse3DError.SourceLengthExceeded(
                    limit = limits.maxSourceLength,
                    actual = source.length,
                ),
            )
        }

        val polyhedra = mutableListOf<ParsedPolyhedron3D>()
        var vertices = mutableListOf<DoubleArray>()
        var faces = mutableListOf<MutableList<Int>>()
        var faceIndex: Int? = null

        for ((lineIndex, rawLine) in source.split('\n').withIndex()) {
            val line = rawLine.trim()
            when {
                line.startsWith("solid") -> {
                    faceIndex = -1
                    vertices = mutableListOf()
                    faces = mutableListOf()
                }

                line.startsWith("endsolid") -> {
                    if (polyhedra.size >= limits.maxPolyhedra) {
                        return GMResult.Err(
                            Parse3DError.PolyhedronLimitExceeded(
                                limits.maxPolyhedra,
                            ),
                        )
                    }
                    polyhedra += ParsedPolyhedron3D(
                        vertices = vertices.map(DoubleArray::toList),
                        faces = faces.map { face -> face.toList() },
                    )
                }

                line.startsWith("facet") -> {
                    if (faces.size >= limits.maxFacesPerPolyhedron) {
                        return GMResult.Err(
                            Parse3DError.FaceLimitExceeded(
                                polyhedronIndex = polyhedra.size,
                                limit = limits.maxFacesPerPolyhedron,
                            ),
                        )
                    }
                    faceIndex = faceIndex?.plus(1)
                    faces.add(mutableListOf())
                }

                line.startsWith("outer loop") ||
                    line.startsWith("endloop") -> Unit

                line.startsWith("vertex") -> {
                    val currentFaceIndex = faceIndex
                    if (
                        currentFaceIndex == null ||
                        currentFaceIndex !in faces.indices
                    ) {
                        return GMResult.Err(
                            Parse3DError.VertexOutsideFacet(lineIndex + 1),
                        )
                    }
                    val face = faces[currentFaceIndex]
                    if (face.size >= limits.maxVerticesPerFace) {
                        return GMResult.Err(
                            Parse3DError.FaceVertexLimitExceeded(
                                polyhedronIndex = polyhedra.size,
                                faceIndex = currentFaceIndex,
                                limit = limits.maxVerticesPerFace,
                            ),
                        )
                    }

                    val coordinates = line
                        .split(' ')
                        .drop(1)
                        .map(::jsParseFloat)
                        .toDoubleArray()
                    var position = vertices.indexOfFirst { existing ->
                        Geometry.distance(
                            existing,
                            coordinates,
                            requestedLength = 3,
                        ) < Mat.eps
                    }
                    if (position < 0) {
                        if (
                            vertices.size >=
                            limits.maxVerticesPerPolyhedron
                        ) {
                            return GMResult.Err(
                                Parse3DError.VertexLimitExceeded(
                                    polyhedronIndex = polyhedra.size,
                                    limit =
                                        limits.maxVerticesPerPolyhedron,
                                ),
                            )
                        }
                        position = vertices.size
                        vertices += coordinates
                    }
                    face += position
                }
            }
        }

        return GMResult.Ok(polyhedra)
    }

    private fun validateLimits(limits: Parse3DLimits): Parse3DError? {
        val invalid = when {
            limits.maxSourceLength < 1 -> "maxSourceLength must be positive"
            limits.maxPolyhedra < 1 -> "maxPolyhedra must be positive"
            limits.maxVerticesPerPolyhedron < 1 ->
                "maxVerticesPerPolyhedron must be positive"
            limits.maxFacesPerPolyhedron < 1 ->
                "maxFacesPerPolyhedron must be positive"
            limits.maxVerticesPerFace < 1 ->
                "maxVerticesPerFace must be positive"
            else -> null
        }
        return invalid?.let { message ->
            Parse3DError.InvalidLimits(message)
        }
    }

    private fun jsParseFloat(value: String): Double {
        val match = NUMBER_PREFIX.find(value.trimStart())
        return match?.value?.toDoubleOrNull() ?: Double.NaN
    }

    private val NUMBER_PREFIX = Regex(
        """^[+-]?(?:Infinity|(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)""",
    )
}
