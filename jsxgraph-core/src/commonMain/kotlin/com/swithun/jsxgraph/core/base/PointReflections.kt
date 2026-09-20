/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/element/composition.js ->
 * createReflection / createMirrorElement / createMirrorPoint
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI

internal sealed interface PointReflectionError {
    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : PointReflectionError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : PointReflectionError

    data class TransformationFactory(
        val error: TransformationError,
    ) : PointReflectionError

    data class PointFactory(
        val error: PointError,
    ) : PointReflectionError
}

/**
 * Point branches of JSXGraph's reflection and mirror-element compositions.
 *
 * The original Point intentionally does not own the transformed output.
 * JSXGraph only adds the output as a child of the reflection Line or mirror
 * Point, which determines recursive removal behavior.
 */
internal object PointReflections {
    // JSXGraph: src/element/composition.js -> createReflection (Point branch)
    fun createReflection(
        board: Board,
        source: Point,
        line: Line,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        fixed: Boolean = true,
    ): GMResult<Point, PointReflectionError> {
        validateParent(board, source, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, line, 1)?.let {
            return GMResult.Err(it)
        }

        val output = when (
            val result = Point.create(
                board = board,
                basePoint = source,
                transformations = listOf(
                    Transformation.createReflectionFromLine(line),
                ),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PointReflectionError.PointFactory(result.error),
            )
        }
        line.addChild(output)
        output.elType = REFLECTION_ELEMENT_TYPE
        output.addParents(listOf(line))
        output.prepareUpdate().update()
        return GMResult.Ok(output)
    }

    // JSXGraph: src/element/composition.js -> createMirrorElement
    // (Point branch)
    fun createMirrorElement(
        board: Board,
        source: Point,
        mirror: Point,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        fixed: Boolean = true,
    ): GMResult<Point, PointReflectionError> =
        createMirror(
            board = board,
            source = source,
            mirror = mirror,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            fixed = fixed,
            elementType = MIRROR_ELEMENT_TYPE,
        )

    // JSXGraph: src/element/composition.js -> createMirrorPoint
    fun createMirrorPoint(
        board: Board,
        source: Point,
        mirror: Point,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
        fixed: Boolean = true,
    ): GMResult<Point, PointReflectionError> =
        createMirror(
            board = board,
            source = source,
            mirror = mirror,
            id = id,
            name = name,
            needsRegularUpdate = needsRegularUpdate,
            fixed = fixed,
            elementType = MIRROR_POINT_ELEMENT_TYPE,
        )

    private fun createMirror(
        board: Board,
        source: Point,
        mirror: Point,
        id: String,
        name: String?,
        needsRegularUpdate: Boolean,
        fixed: Boolean,
        elementType: String,
    ): GMResult<Point, PointReflectionError> {
        validateParent(board, source, 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, mirror, 1)?.let {
            return GMResult.Err(it)
        }

        val transformation = when (
            val result = Transformation.createRotation(
                board = board,
                angle = TransformationParameter.Numeric(PI),
                center = mirror,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PointReflectionError.TransformationFactory(result.error),
            )
        }
        val output = when (
            val result = Point.create(
                board = board,
                basePoint = source,
                transformations = listOf(transformation),
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                PointReflectionError.PointFactory(result.error),
            )
        }
        mirror.addChild(output)
        output.elType = elementType
        output.addParents(listOf(mirror))
        output.prepareUpdate().update()
        return GMResult.Ok(output)
    }

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): PointReflectionError? =
        when {
            element.board !== board ->
                PointReflectionError.ParentBoardMismatch(parentIndex)
            board.objects[element.id] !== element ->
                PointReflectionError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private const val REFLECTION_ELEMENT_TYPE = "reflection"
    private const val MIRROR_ELEMENT_TYPE = "mirrorelement"
    private const val MIRROR_POINT_ELEMENT_TYPE = "mirrorpoint"
}
