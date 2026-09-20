/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/line.js -> createTangentTo
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface TangentToError {
    data class UnsupportedParents(
        val parentTypes: List<String>,
    ) : TangentToError

    data class UnsupportedConic(
        val parentType: String,
    ) : TangentToError

    data class ParentBoardMismatch(
        val parentIndex: Int,
    ) : TangentToError

    data class ParentNotRegistered(
        val parentIndex: Int,
        val id: String,
    ) : TangentToError

    data class DuplicateElementId(
        val id: String,
    ) : TangentToError

    data class PolarCreation(
        val error: TangentError,
    ) : TangentToError

    data class IntersectionCreation(
        val error: IntersectionError,
    ) : TangentToError

    data class TangentCreation(
        val error: TangentError,
    ) : TangentToError
}

internal data class TangentToIdentity(
    val id: String = "",
    val name: String? = null,
    val needsRegularUpdate: Boolean = true,
)

internal data class TangentToLineAttributes(
    val identity: TangentToIdentity = TangentToIdentity(),
    val straightFirst: Boolean = true,
    val straightLast: Boolean = true,
    val point1: TangentToIdentity = TangentToIdentity(),
    val point2: TangentToIdentity = TangentToIdentity(),
)

internal data class TangentToPointAttributes(
    val identity: TangentToIdentity = TangentToIdentity(),
    val fixed: Boolean = false,
)

internal object TangentTo {
    private const val ELEMENT_TYPE = "tangentto"

    // JSXGraph 1.13.3: src/base/line.js -> createTangentTo.
    internal fun create(
        board: Board,
        conic: GeometryElement,
        pointFrom: Point,
        number: Double = 0.0,
        tangentAttributes: TangentToLineAttributes =
            TangentToLineAttributes(),
        polarAttributes: TangentToLineAttributes =
            TangentToLineAttributes(),
        pointAttributes: TangentToPointAttributes =
            TangentToPointAttributes(),
    ): GMResult<Line, TangentToError> {
        validateParent(board, conic, parentIndex = 0)?.let {
            return GMResult.Err(it)
        }
        validateParent(board, pointFrom, parentIndex = 1)?.let {
            return GMResult.Err(it)
        }
        if (conic.type == Const.OBJECT_TYPE_CONIC) {
            return GMResult.Err(
                TangentToError.UnsupportedConic(conic.elType),
            )
        }
        val circle = conic as? Circle
            ?: return GMResult.Err(
                TangentToError.UnsupportedParents(
                    parentTypes = listOf(conic.elType, pointFrom.elType),
                ),
            )
        duplicateRequestedId(
            board = board,
            identities = listOf(
                polarAttributes.point1,
                polarAttributes.point2,
                polarAttributes.identity,
                pointAttributes.identity,
                tangentAttributes.point1,
                tangentAttributes.point2,
                tangentAttributes.identity,
            ),
        )?.let { id ->
            return GMResult.Err(TangentToError.DuplicateElementId(id))
        }

        val polar = when (
            val result = Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = pointFrom,
                id = polarAttributes.identity.id,
                name = polarAttributes.identity.name,
                needsRegularUpdate =
                    polarAttributes.identity.needsRegularUpdate,
                straightFirst = polarAttributes.straightFirst,
                straightLast = polarAttributes.straightLast,
                point1Id = polarAttributes.point1.id,
                point1Name = polarAttributes.point1.name,
                point1NeedsRegularUpdate =
                    polarAttributes.point1.needsRegularUpdate,
                point2Id = polarAttributes.point2.id,
                point2Name = polarAttributes.point2.name,
                point2NeedsRegularUpdate =
                    polarAttributes.point2.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                TangentToError.PolarCreation(result.error),
            )
        }
        val intersection = when (
            val result = IntersectionPoint.create(
                board = board,
                first = polar,
                second = circle,
                firstIndex = IntersectionIndexSource.Number(number),
                id = pointAttributes.identity.id,
                name = pointAttributes.identity.name,
                needsRegularUpdate =
                    pointAttributes.identity.needsRegularUpdate,
                fixed = pointAttributes.fixed,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                removePolar(board, polar)
                return GMResult.Err(
                    TangentToError.IntersectionCreation(result.error),
                )
            }
        }
        val tangent = when (
            val result = Tangent.create(
                board = board,
                firstParent = circle,
                secondParent = intersection,
                id = tangentAttributes.identity.id,
                name = tangentAttributes.identity.name,
                needsRegularUpdate =
                    tangentAttributes.identity.needsRegularUpdate,
                straightFirst = tangentAttributes.straightFirst,
                straightLast = tangentAttributes.straightLast,
                point1Id = tangentAttributes.point1.id,
                point1Name = tangentAttributes.point1.name,
                point1NeedsRegularUpdate =
                    tangentAttributes.point1.needsRegularUpdate,
                point2Id = tangentAttributes.point2.id,
                point2Name = tangentAttributes.point2.name,
                point2NeedsRegularUpdate =
                    tangentAttributes.point2.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                board.removeObject(intersection)
                removePolar(board, polar)
                return GMResult.Err(
                    TangentToError.TangentCreation(result.error),
                )
            }
        }
        tangent.elType = ELEMENT_TYPE
        tangent.tangentToPoint = intersection
        tangent.tangentToPolar = polar
        return GMResult.Ok(tangent)
    }

    private fun validateParent(
        board: Board,
        element: GeometryElement,
        parentIndex: Int,
    ): TangentToError? =
        when {
            element.board !== board ->
                TangentToError.ParentBoardMismatch(parentIndex)
            board.elementById(element.id) !== element ->
                TangentToError.ParentNotRegistered(
                    parentIndex = parentIndex,
                    id = element.id,
                )
            else -> null
        }

    private fun duplicateRequestedId(
        board: Board,
        identities: List<TangentToIdentity>,
    ): String? {
        val requested = mutableSetOf<String>()
        for (identity in identities) {
            if (identity.id.isEmpty()) {
                continue
            }
            if (!requested.add(identity.id) || board.elementById(identity.id) != null) {
                return identity.id
            }
        }
        return null
    }

    private fun removePolar(
        board: Board,
        polar: Line,
    ) {
        board.removeObjects(
            listOf(
                polar,
                polar.point1,
                polar.point2,
            ),
        )
    }
}
