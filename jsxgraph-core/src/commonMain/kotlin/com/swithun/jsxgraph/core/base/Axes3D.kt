/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/box3d.js -> createAxes3D / createAxis3D
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult

internal sealed interface Axis3DError {
    data class StartPoint(
        val error: Point3DError,
    ) : Axis3DError

    data class EndPoint(
        val error: Point3DError,
    ) : Axis3DError

    data class Line(
        val error: Line3DError,
    ) : Axis3DError
}

internal object Axis3D {
    // JSXGraph: src/3d/box3d.js -> createAxis3D.
    internal fun create(
        view: View3D,
        start: DoubleArray,
        end: DoubleArray,
        id: String = "",
        name: String? = null,
        needsRegularUpdate: Boolean = true,
    ): GMResult<Line3D, Axis3DError> {
        val startPoint = when (
            val result = Point3D.create(
                view = view,
                coordinates = start,
                name = "",
                needsRegularUpdate = needsRegularUpdate,
                fixed = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return GMResult.Err(
                Axis3DError.StartPoint(result.error),
            )
        }
        val endPoint = when (
            val result = Point3D.create(
                view = view,
                coordinates = end,
                name = "",
                needsRegularUpdate = needsRegularUpdate,
                fixed = true,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                view.board.removeObject(startPoint)
                return GMResult.Err(
                    Axis3DError.EndPoint(result.error),
                )
            }
        }
        return when (
            val result = Line3D.create(
                view = view,
                point1 = startPoint,
                point2 = endPoint,
                ownsPoint1 = true,
                ownsPoint2 = true,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                fixed = true,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> GMResult.Err(
                Axis3DError.Line(result.error),
            )
        }
    }
}

internal sealed interface Axes3DError {
    data class UnsupportedAxesPosition(
        val position: String,
    ) : Axes3DError

    data class AxisCreation(
        val role: String,
        val error: Axis3DError,
    ) : Axes3DError

    data class OriginCreation(
        val error: IntersectionError,
    ) : Axes3DError

    data class PlaneCreation(
        val role: String,
        val error: Plane3DError,
    ) : Axes3DError

    data class TicksCreation(
        val role: String,
        val error: Ticks3DError,
    ) : Axes3DError
}

internal data class Axes3DTicksAttributes(
    val ticksDistance: Double = 1.0,
    val tickEndings: DoubleArray = doubleArrayOf(0.0, 1.0),
    val majorHeight: Double = 10.0,
    val drawLabels: Boolean = true,
)

/**
 * Container produced by JSXGraph createAxes3D.
 *
 * The translated slice creates every Line3D, center-origin Intersection,
 * Plane3D, and border Ticks3D member whose dependencies are available.
 */
internal class Axes3D private constructor(
    members: Map<String, GeometryElement>,
    internal val axesPosition: String,
    internal val unsupportedFeatures: Set<String>,
) : Composition(members) {
    init {
        elType = AXES_3D_ELEMENT_TYPE
    }

    internal companion object {
        private const val AXES_3D_ELEMENT_TYPE = "axes3d"
        internal const val PLANE_SURFACE_GAP = "mesh3d/polyhedron3d"

        // JSXGraph: src/3d/box3d.js -> createAxes3D.
        internal fun create(
            view: View3D,
            axesPosition: String = "center",
            planeTypes: Map<String, String> = emptyMap(),
            ticksAttributes: Map<String, Axes3DTicksAttributes> =
                emptyMap(),
            needsRegularUpdate: Boolean = true,
        ): GMResult<Axes3D, Axes3DError> {
            val normalizedPosition = axesPosition.lowercase()
            if (normalizedPosition !in setOf("center", "border", "none")) {
                return GMResult.Err(
                    Axes3DError.UnsupportedAxesPosition(axesPosition),
                )
            }
            val rear = DoubleArray(3) { view.bbox3D[it][0] }
            val front = DoubleArray(3) { view.bbox3D[it][1] }
            val members = linkedMapOf<String, GeometryElement>()
            val created = mutableListOf<GeometryElement>()
            val centerAxes = mutableListOf<Line3D>()

            fun cleanup() {
                view.board.removeObjects(created.asReversed())
            }

            fun createAxis(
                role: String,
                start: DoubleArray,
                end: DoubleArray,
                createdAxes: MutableList<Line3D>? = null,
            ): Axes3DError? =
                when (
                    val result = Axis3D.create(
                        view = view,
                        start = start,
                        end = end,
                        name = defaultAxisName(role),
                        needsRegularUpdate = needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> {
                        members[role] = result.value
                        created += result.value
                        createdAxes?.add(result.value)
                        null
                    }
                    is GMResult.Err ->
                        Axes3DError.AxisCreation(role, result.error)
                }

            for (dimension in 0 until 3) {
                val direction = DIRECTIONS[dimension]
                when (normalizedPosition) {
                    "center" -> {
                        val start = DoubleArray(3)
                        val end = DoubleArray(3)
                        end[dimension] = front[dimension]
                        val role = "${direction}Axis"
                        createAxis(
                            role = role,
                            start = start,
                            end = end,
                            createdAxes = centerAxes,
                        )?.let { error ->
                            cleanup()
                            return GMResult.Err(error)
                        }
                    }
                    "border" -> {
                        val start: DoubleArray
                        val end: DoubleArray
                        when (direction) {
                            "x" -> {
                                start = doubleArrayOf(
                                    rear[0],
                                    front[1],
                                    rear[2],
                                )
                                end = doubleArrayOf(
                                    front[0],
                                    front[1],
                                    rear[2],
                                )
                            }
                            "y" -> {
                                start = doubleArrayOf(
                                    front[0],
                                    rear[1],
                                    rear[2],
                                )
                                end = doubleArrayOf(
                                    front[0],
                                    front[1],
                                    rear[2],
                                )
                            }
                            else -> {
                                start = rear.copyOf()
                                end = front.copyOf()
                                start[1] = front[1]
                                end[0] = rear[0]
                                end[2] = front[2]
                            }
                        }
                        createAxis(
                            role = "${direction}AxisBorder",
                            start = start,
                            end = end,
                        )?.let { error ->
                            cleanup()
                            return GMResult.Err(error)
                        }
                        val ticksRole = "${direction}AxisBorderTicks"
                        val ticksDirection = DoubleArray(3)
                        ticksDirection[dimension] = 1.0
                        val crossDirection = when (direction) {
                            "y" -> doubleArrayOf(1.0, 0.0, 0.0)
                            else -> doubleArrayOf(0.0, 1.0, 0.0)
                        }
                        val tickAttributes =
                            ticksAttributes[ticksRole]
                                ?: Axes3DTicksAttributes()
                        val ticks = when (
                            val result = Ticks3D.create(
                                view = view,
                                pointSource =
                                    Ticks3DPointSource.Values(
                                        start.map(
                                            Line3DCoordinateValue::Numeric,
                                        ),
                                    ),
                                direction1 =
                                    ticksDirection.map(
                                        Line3DCoordinateValue::Numeric,
                                    ),
                                length = front[dimension] - rear[dimension],
                                direction2 =
                                    crossDirection.map(
                                        Line3DCoordinateValue::Numeric,
                                    ),
                                ticksDistance =
                                    tickAttributes.ticksDistance,
                                tickEndings =
                                    tickAttributes.tickEndings,
                                majorHeight =
                                    tickAttributes.majorHeight,
                                drawLabels =
                                    tickAttributes.drawLabels,
                                name = "",
                                needsRegularUpdate = needsRegularUpdate,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> {
                                cleanup()
                                return GMResult.Err(
                                    Axes3DError.TicksCreation(
                                        role = ticksRole,
                                        error = result.error,
                                    ),
                                )
                            }
                        }
                        members[ticksRole] = ticks
                        created += ticks
                    }
                }
            }

            if (normalizedPosition == "center") {
                // JSXGraph: src/3d/box3d.js -> createAxes3D origin.
                val origin = when (
                    val result = IntersectionPoint.createAxes3DOrigin(
                        board = view.board,
                        first = centerAxes[0],
                        second = centerAxes[1],
                        name = "",
                        needsRegularUpdate = needsRegularUpdate,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        cleanup()
                        return GMResult.Err(
                            Axes3DError.OriginCreation(result.error),
                        )
                    }
                }
                members["O"] = origin
                created += origin
            }

            for (dimension in 0 until 3) {
                val firstDirection = (dimension + 1) % 3
                val secondDirection = (dimension + 2) % 3
                val direction = DIRECTIONS[dimension]
                for (sideIndex in SIDES.indices) {
                    val role = "${direction}Plane${SIDES[sideIndex]}"
                    val origin = DoubleArray(3)
                    origin[dimension] =
                        if (sideIndex == 0) {
                            rear[dimension]
                        } else {
                            front[dimension]
                        }
                    val firstVector = DoubleArray(3)
                    val secondVector = DoubleArray(3)
                    firstVector[firstDirection] = 1.0
                    secondVector[secondDirection] = 1.0
                    val originPoint = when (
                        val result = Point3D.create(
                            view = view,
                            coordinates = origin,
                            name = "",
                            needsRegularUpdate = needsRegularUpdate,
                            fixed = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            cleanup()
                            return GMResult.Err(
                                Axes3DError.PlaneCreation(
                                    role = role,
                                    error =
                                        Plane3DError.PointFactory(result.error),
                                ),
                            )
                        }
                    }
                    val plane = when (
                        val result = Plane3D.create(
                            view = view,
                            point = originPoint,
                            direction1Source =
                                numericDirection(firstVector),
                            direction2Source =
                                numericDirection(secondVector),
                            rangeUSource = numericRange(
                                rear[firstDirection],
                                front[firstDirection],
                            ),
                            rangeVSource = numericRange(
                                rear[secondDirection],
                                front[secondDirection],
                            ),
                            ownsPoint = true,
                            planeType = planeTypes[role] ?: "shader",
                            name = "",
                            needsRegularUpdate = needsRegularUpdate,
                            fixed = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            cleanup()
                            return GMResult.Err(
                                Axes3DError.PlaneCreation(
                                    role = role,
                                    error = result.error,
                                ),
                            )
                        }
                    }
                    plane.elType = "axisplane3d"
                    members[role] = plane
                    created += plane
                }
            }

            for (dimension in 0 until 3) {
                val direction = DIRECTIONS[dimension]
                for (sideIndex in SIDES.indices) {
                    for (offset in 1..2) {
                        val axisDimension = (dimension + offset) % 3
                        val axisDirection = DIRECTIONS[axisDimension]
                        val role =
                            "${direction}Plane${SIDES[sideIndex]}" +
                                "${axisDirection.uppercase()}Axis"
                        val start = DoubleArray(3)
                        val end = DoubleArray(3)
                        start[dimension] =
                            if (sideIndex == 0) {
                                rear[dimension]
                            } else {
                                front[dimension]
                            }
                        end[dimension] = start[dimension]
                        start[axisDimension] = rear[axisDimension]
                        end[axisDimension] = front[axisDimension]
                        createAxis(
                            role = role,
                            start = start,
                            end = end,
                        )?.let { error ->
                            cleanup()
                            return GMResult.Err(error)
                        }
                        val planeRole =
                            "${direction}Plane${SIDES[sideIndex]}"
                        members[planeRole]?.addChild(
                            members.getValue(role),
                        )
                    }
                }
            }

            return GMResult.Ok(
                Axes3D(
                    members = members,
                    axesPosition = normalizedPosition,
                    unsupportedFeatures = setOf(PLANE_SURFACE_GAP),
                ),
            )
        }

        private fun numericDirection(
            values: DoubleArray,
        ): Plane3DDirectionSource =
            Plane3DDirectionSource.Values(
                values.map(Line3DCoordinateValue::Numeric),
            )

        private fun numericRange(
            start: Double,
            end: Double,
        ): List<Line3DCoordinateValue> =
            listOf(
                Line3DCoordinateValue.Numeric(start),
                Line3DCoordinateValue.Numeric(end),
            )

        private fun defaultAxisName(role: String): String =
            when (role) {
                "xAxis" -> "X"
                "yAxis" -> "Y"
                "zAxis" -> "Z"
                "xAxisBorder" -> "x"
                "yAxisBorder" -> "y"
                "zAxisBorder" -> "z"
                else -> ""
            }

        private val DIRECTIONS = listOf("x", "y", "z")
        private val SIDES = listOf("Rear", "Front")
    }
}
