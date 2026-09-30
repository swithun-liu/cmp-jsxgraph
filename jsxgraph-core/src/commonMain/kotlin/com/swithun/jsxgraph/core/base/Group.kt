/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/group.js -> Group / createGroup
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.abs

internal sealed interface GroupError {
    data class ParentBoardMismatch(
        val id: String,
    ) : GroupError

    data class CenterEvaluation(
        val reason: String,
    ) : GroupError

    data class Transformation(
        val error: TransformationError,
    ) : GroupError
}

internal sealed interface GroupCenter {
    data object Centroid : GroupCenter

    data class Element(
        val element: CoordsElement,
    ) : GroupCenter

    class Coordinates(
        coordinates: DoubleArray,
    ) : GroupCenter {
        val coordinates: DoubleArray = coordinates.copyOf()
    }

    class Dynamic(
        val evaluate: () -> GMResult<DoubleArray, GroupError>,
    ) : GroupCenter
}

internal enum class GroupAction {
    NOTHING,
    TRANSLATION,
    ROTATION,
    SCALING,
}

internal data class GroupDrag(
    val action: GroupAction,
    val id: String,
    val changed: List<String>,
)

/**
 * Board-level movement constraint for coordinate elements.
 *
 * JSXGraph Group is not a GeometryElement and does not render. It extends
 * [Composition] only as an internal JessieCode reference carrier; all group
 * membership, caching, and update behavior is kept in this class.
 */
internal class Group private constructor(
    internal val board: Board,
    requestedId: String,
    requestedName: String?,
    internal var needsRegularUpdate: Boolean,
) : Composition() {
    internal var id: String = ""
        private set
    internal var name: String = ""
        private set
    internal val groupObjects = linkedMapOf<String, CoordsElement>()
    internal val coords = linkedMapOf<String, DoubleArray>()
    internal var needsUpdate: Boolean = true
    internal var rotationCenter: GroupCenter = GroupCenter.Centroid
        private set
    internal var scaleCenter: GroupCenter? = null
        private set
    internal val rotationPoints = mutableListOf<CoordsElement>()
    internal val translationPoints = mutableListOf<CoordsElement>()
    internal val scalePoints = mutableListOf<CoordsElement>()
    internal val scaleDirections = linkedMapOf<String, String>()
    internal val parentIds = mutableListOf<String>()
    internal var updateError: GroupError? = null
        private set

    init {
        id = board.registerGroup(this, requestedId)
        name =
            if (requestedName.isNullOrEmpty()) {
                board.generateGroupName()
            } else {
                requestedName
            }
        elType = GROUP_ELEMENT_TYPE
    }

    // JSXGraph 1.13.3: src/base/group.js -> ungroup.
    internal fun ungroup(): Group {
        for (point in groupObjects.values) {
            point.groups.remove(id)
        }
        groupObjects.clear()
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> addParents.
    internal fun addParentIds(values: Iterable<String>): Group {
        for (value in values) {
            if (
                (
                    board.elementById(value) != null ||
                        board.groupById(value) != null
                    ) &&
                value !in parentIds
            ) {
                parentIds += value
            }
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> setParents.
    internal fun setParentIds(values: Iterable<String>): Group {
        parentIds.clear()
        return addParentIds(values)
    }

    // JSXGraph 1.13.3: src/base/group.js -> getParents.
    override fun getParents(): List<String> = parentIds.toList()

    // JSXGraph 1.13.3: src/base/group.js -> _updateCoordsCache.
    internal fun updateCoordsCache(elementId: String) {
        val point = groupObjects[elementId] ?: return
        coords[point.id] = point.coords.usrCoords.copyOf()
    }

    // JSXGraph 1.13.3: src/base/group.js -> update.
    internal fun update(
        draggedElement: GeometryElement? = null,
    ): Group {
        when (val result = updateResult(draggedElement)) {
            is GMResult.Ok -> updateError = null
            is GMResult.Err -> updateError = result.error
        }
        return this
    }

    internal fun updateResult(
        draggedElement: GeometryElement? = null,
    ): GMResult<Group, GroupError> {
        if (!needsUpdate) {
            return GMResult.Ok(this)
        }

        val drag = findDragType()
        if (drag.action == GroupAction.NOTHING) {
            updateCoordsCache(drag.id)
            return GMResult.Ok(this)
        }
        val dragged = groupObjects[drag.id]
            ?: return GMResult.Ok(this)

        val transformation = when (drag.action) {
            GroupAction.TRANSLATION -> {
                val cached = coords[drag.id] ?: dragged.coords.usrCoords
                var translation = doubleArrayOf(
                    dragged.coords.usrCoords[1] - cached[1],
                    dragged.coords.usrCoords[2] - cached[2],
                )
                if (dragged.elementClass != Const.OBJECT_CLASS_POINT) {
                    var homogeneous = doubleArrayOf(
                        0.0,
                        translation[0],
                        translation[1],
                    )
                    for (item in dragged.transformations) {
                        homogeneous = Mat.matVecMult(
                            item.matrix,
                            homogeneous,
                        )
                    }
                    translation = homogeneous.copyOfRange(1, 3)
                }
                when (
                    val result = Transformation.create(
                        type = "translate",
                        parameters = translation,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        GroupError.Transformation(result.error),
                    )
                }
            }

            GroupAction.ROTATION,
            GroupAction.SCALING,
            -> {
                val center = when (
                    val result = resolveCenter(
                        if (drag.action == GroupAction.ROTATION) {
                            rotationCenter
                        } else {
                            scaleCenter
                        },
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                if (drag.action == GroupAction.ROTATION) {
                    val cached = coords[drag.id]
                        ?.copyOfRange(1, 3)
                        ?: dragged.Coords()
                    val angle = Geometry.rad(
                        first = cached,
                        vertex = center,
                        third = dragged.Coords(),
                    )
                    when (
                        val result = Transformation.create(
                            type = "rotate",
                            parameters = doubleArrayOf(
                                angle,
                                center[0],
                                center[1],
                            ),
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            GroupError.Transformation(result.error),
                        )
                    }
                } else {
                    val cached = coords[drag.id]
                        ?.copyOfRange(1, 3)
                        ?: dragged.Coords()
                    val previousDistance =
                        Geometry.distance(cached, center)
                    if (abs(previousDistance) < Mat.eps) {
                        return GMResult.Ok(this)
                    }
                    val scale =
                        Geometry.distance(dragged.Coords(), center) /
                            previousDistance
                    val direction =
                        scaleDirections[drag.id] ?: "xy"
                    val scaleX = if ('x' in direction) scale else 1.0
                    val scaleY = if ('y' in direction) scale else 1.0
                    when (
                        val result = Transformation.create(
                            type = "generic",
                            parameters = doubleArrayOf(
                                1.0,
                                0.0,
                                0.0,
                                center[0] * (1.0 - scaleX),
                                scaleX,
                                0.0,
                                center[1] * (1.0 - scaleY),
                                0.0,
                                scaleY,
                            ),
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return GMResult.Err(
                            GroupError.Transformation(result.error),
                        )
                    }
                }
            }

            GroupAction.NOTHING -> return GMResult.Ok(this)
        }

        for (element in groupObjects.values) {
            if (element.elementClass != Const.OBJECT_CLASS_POINT) {
                if (
                    drag.action != GroupAction.TRANSLATION ||
                    element.id != drag.id
                ) {
                    when (val result = transformation.meltTo(element)) {
                        is GMResult.Ok -> Unit
                        is GMResult.Err -> return GMResult.Err(
                            GroupError.Transformation(result.error),
                        )
                    }
                }
            }
        }

        when (val result = applyTransformation(drag, transformation)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        needsUpdate = false

        for (element in groupObjects.values) {
            for (descendant in element.descendants.values) {
                descendant.needsUpdate =
                    descendant.needsRegularUpdate || board.needsFullUpdate
            }
        }
        board.updateElements(draggedElement)

        for (elementId in groupObjects.keys) {
            updateCoordsCache(elementId)
        }
        return GMResult.Ok(this)
    }

    // JSXGraph 1.13.3: src/base/group.js -> _update_find_drag_type.
    internal fun findDragType(): GroupDrag {
        val changed = groupObjects.values.mapNotNull { element ->
            val cached = coords[element.id] ?: return@mapNotNull null
            val weightDifference =
                element.coords.usrCoords[0] - cached[0]
            val distance =
                if (
                    weightDifference * weightDifference >
                    Mat.eps * Mat.eps
                ) {
                    Double.POSITIVE_INFINITY
                } else {
                    Mat.hypot(
                        element.coords.usrCoords[1] - cached[1],
                        element.coords.usrCoords[2] - cached[2],
                    )
                }
            element.id.takeIf { distance > Mat.eps }
        }
        if (changed.isEmpty()) {
            return GroupDrag(
                action = GroupAction.NOTHING,
                id = "",
                changed = changed,
            )
        }

        val id = changed.first()
        val point = groupObjects.getValue(id)
        val action =
            if (changed.size > 1) {
                GroupAction.TRANSLATION
            } else {
                when {
                    point in rotationPoints ->
                        GroupAction.ROTATION
                    point in scalePoints && scaleCenter != null ->
                        GroupAction.SCALING
                    point in translationPoints ->
                        GroupAction.TRANSLATION
                    else -> GroupAction.NOTHING
                }
            }
        return GroupDrag(action = action, id = id, changed = changed)
    }

    // JSXGraph 1.13.3: src/base/group.js -> _update_centroid_center.
    internal fun centroidCenter(): DoubleArray {
        val center = doubleArrayOf(0.0, 0.0)
        for (cached in coords.values) {
            center[0] += cached[1]
            center[1] += cached[2]
        }
        if (coords.isNotEmpty()) {
            center[0] /= coords.size
            center[1] /= coords.size
        }
        return center
    }

    private fun resolveCenter(
        center: GroupCenter?,
    ): GMResult<DoubleArray, GroupError> =
        when (center) {
            GroupCenter.Centroid -> GMResult.Ok(centroidCenter())
            is GroupCenter.Element -> GMResult.Ok(center.element.Coords())
            is GroupCenter.Coordinates ->
                GMResult.Ok(center.coordinates.copyOf())
            is GroupCenter.Dynamic -> center.evaluate()
            null -> GMResult.Err(
                GroupError.CenterEvaluation(
                    reason = "No center is configured",
                ),
            )
        }

    // JSXGraph 1.13.3: src/base/group.js ->
    // _update_apply_transformation.
    private fun applyTransformation(
        drag: GroupDrag,
        transformation: Transformation,
    ): GMResult<Unit, GroupError> {
        val iterator = groupObjects.iterator()
        while (iterator.hasNext()) {
            val (elementId, element) = iterator.next()
            if (board.elementById(elementId) !== element) {
                iterator.remove()
                continue
            }
            val cached = coords[elementId] ?: continue
            if (element.id != drag.id) {
                if (
                    drag.action == GroupAction.TRANSLATION &&
                    element.id !in drag.changed &&
                    element.elementClass == Const.OBJECT_CLASS_POINT
                ) {
                    element.coords.setCoordinates(
                        coordType = Const.COORDS_BY_USER,
                        coordinates = doubleArrayOf(
                            cached[1] + transformation.matrix[1][0],
                            cached[2] + transformation.matrix[2][0],
                        ),
                    )
                } else if (
                    drag.action == GroupAction.ROTATION ||
                    drag.action == GroupAction.SCALING
                ) {
                    if (element.elementClass == Const.OBJECT_CLASS_POINT) {
                        when (val result = transformation.applyOnce(element)) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return GMResult.Err(
                                GroupError.Transformation(result.error),
                            )
                        }
                    }
                }
            } else if (
                (
                    drag.action == GroupAction.ROTATION ||
                        drag.action == GroupAction.SCALING
                    ) &&
                element.elementClass == Const.OBJECT_CLASS_POINT
            ) {
                element.coords.setCoordinates(
                    coordType = Const.COORDS_BY_USER,
                    coordinates = Mat.matVecMult(
                        transformation.matrix,
                        cached,
                    ),
                )
            }
        }
        return GMResult.Ok(Unit)
    }

    // JSXGraph 1.13.3: src/base/group.js -> addPoint.
    internal fun addPoint(point: CoordsElement): Group {
        groupObjects[point.id] = point
        updateCoordsCache(point.id)
        translationPoints += point
        if (id !in point.groups) {
            point.groups += id
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> addPoints.
    internal fun addPoints(points: Iterable<CoordsElement>): Group {
        for (point in points) {
            addPoint(point)
        }
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> addGroup.
    internal fun addGroup(group: Group): Group =
        addPoints(group.groupObjects.values)

    // JSXGraph 1.13.3: src/base/group.js -> removePoint.
    internal fun removePoint(point: CoordsElement): Group {
        groupObjects.remove(point.id)
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> setRotationCenter.
    internal fun setRotationCenter(center: GroupCenter): Group {
        rotationCenter = center
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> setRotationPoints.
    internal fun setRotationPoints(
        points: Iterable<CoordsElement>,
    ): Group = setActionPoints(GroupAction.ROTATION, points)

    // JSXGraph 1.13.3: src/base/group.js -> addRotationPoint.
    internal fun addRotationPoint(point: CoordsElement): Group =
        addActionPoint(GroupAction.ROTATION, point)

    // JSXGraph 1.13.3: src/base/group.js -> removeRotationPoint.
    internal fun removeRotationPoint(point: CoordsElement): Group =
        removeActionPoint(GroupAction.ROTATION, point)

    // JSXGraph 1.13.3: src/base/group.js -> setTranslationPoints.
    internal fun setTranslationPoints(
        points: Iterable<CoordsElement>,
    ): Group = setActionPoints(GroupAction.TRANSLATION, points)

    // JSXGraph 1.13.3: src/base/group.js -> addTranslationPoint.
    internal fun addTranslationPoint(point: CoordsElement): Group =
        addActionPoint(GroupAction.TRANSLATION, point)

    // JSXGraph 1.13.3: src/base/group.js -> removeTranslationPoint.
    internal fun removeTranslationPoint(point: CoordsElement): Group =
        removeActionPoint(GroupAction.TRANSLATION, point)

    // JSXGraph 1.13.3: src/base/group.js -> setScaleCenter.
    internal fun setScaleCenter(center: GroupCenter): Group {
        scaleCenter = center
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> setScalePoints.
    internal fun setScalePoints(
        points: Iterable<CoordsElement>,
        direction: String = "xy",
    ): Group {
        for (point in points) {
            scaleDirections[point.id] = direction
        }
        return setActionPoints(GroupAction.SCALING, points)
    }

    // JSXGraph 1.13.3: src/base/group.js -> addScalePoint.
    internal fun addScalePoint(
        point: CoordsElement,
        direction: String = "xy",
    ): Group {
        addActionPoint(GroupAction.SCALING, point)
        scaleDirections[point.id] = direction
        return this
    }

    // JSXGraph 1.13.3: src/base/group.js -> removeScalePoint.
    internal fun removeScalePoint(point: CoordsElement): Group =
        removeActionPoint(GroupAction.SCALING, point)

    private fun setActionPoints(
        action: GroupAction,
        points: Iterable<CoordsElement>,
    ): Group {
        actionPoints(action).clear()
        for (point in points) {
            addActionPoint(action, point)
        }
        return this
    }

    private fun addActionPoint(
        action: GroupAction,
        point: CoordsElement,
    ): Group {
        actionPoints(action) += point
        return this
    }

    private fun removeActionPoint(
        action: GroupAction,
        point: CoordsElement,
    ): Group {
        actionPoints(action).remove(point)
        return this
    }

    private fun actionPoints(
        action: GroupAction,
    ): MutableList<CoordsElement> =
        when (action) {
            GroupAction.ROTATION -> rotationPoints
            GroupAction.TRANSLATION -> translationPoints
            GroupAction.SCALING -> scalePoints
            GroupAction.NOTHING -> mutableListOf()
        }

    internal companion object {
        private const val GROUP_ELEMENT_TYPE = "group"

        internal fun create(
            board: Board,
            parents: List<GeometryElement>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            recordParents: Boolean = true,
        ): GMResult<Group, GroupError> {
            parents.firstOrNull { it.board !== board }?.let { parent ->
                return GMResult.Err(
                    GroupError.ParentBoardMismatch(parent.id),
                )
            }
            val group = Group(
                board = board,
                requestedId = id,
                requestedName = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            for (parent in parents) {
                val coordinates = parent as? CoordsElement ?: continue
                if (!(coordinates is Point && coordinates.isFixed)) {
                    group.addPoint(coordinates)
                }
            }
            if (recordParents) {
                group.setParentIds(parents.map(GeometryElement::id))
            }
            return GMResult.Ok(group)
        }
    }
}
