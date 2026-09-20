/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/view3d.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

internal sealed interface View3DError {
    data class InvalidLowerLeftCorner(
        val actualCount: Int,
    ) : View3DError

    data class InvalidSize(
        val actualCount: Int,
    ) : View3DError

    data class InvalidBoundingBox(
        val actualDimensions: Int,
        val dimensionSizes: List<Int>,
    ) : View3DError

    data class Registration(
        val error: BoardError,
    ) : View3DError
}

/**
 * Source-mapped camera and projection kernel of JXG.View3D.
 *
 * Axes, sliders, keyboard/pointer camera navigation, shaders, and depth
 * ordering are separate upstream phases and intentionally remain outside this
 * first lifecycle slice.
 */
internal class View3D private constructor(
    board: Board,
    internal val llftCorner: DoubleArray,
    internal val size: DoubleArray,
    internal val bbox3D: Array<DoubleArray>,
    projection: String,
    azimuth: Double,
    elevation: Double,
    bank: Double,
    private val cameraDistance: Double?,
    private val fieldOfView: Double,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_VIEW3D,
    elementClass = Const.OBJECT_CLASS_3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val objects = linkedMapOf<String, GeometryElement3D>()
    internal val elementsByName = linkedMapOf<String, GeometryElement3D>()

    internal val angles = Angles(
        az = azimuth,
        el = elevation,
        bank = bank,
    )

    internal var matrix3DRot: Array<DoubleArray> = Mat.identity(4)
        private set
    internal var matrix3DRotShift: Array<DoubleArray> = Mat.identity(4)
        private set
    internal var matrix3D: Array<DoubleArray> = Mat.identity(3, 4)
        private set
    internal var viewPortTransform: Array<DoubleArray>? = null
        private set
    internal var boxToCam: Array<DoubleArray> = emptyArray()
        private set
    internal var shift: Array<DoubleArray> = Mat.identity(4)
        private set
    internal var focalDist: Double = -1.0
        private set
    internal var projectionType: String = projection.lowercase()
        private set

    init {
        elType = VIEW_3D_ELEMENT_TYPE
    }

    internal data class Angles(
        var az: Double,
        var el: Double,
        var bank: Double,
    )

    // JSXGraph: src/3d/view3d.js -> select, ID/name branch.
    internal fun select(reference: String): GeometryElement3D? =
        objects[reference] ?: elementsByName[reference]

    // JSXGraph: src/3d/view3d.js -> getRotationFromAngles.
    internal fun getRotationFromAngles(): Array<DoubleArray> {
        val azimuth = angles.az
        val elevation = angles.el
        val bank = angles.bank
        val elevationFactor = -sin(elevation)
        val matrix = Mat.identity(4)

        matrix[1][1] = -cos(azimuth)
        matrix[1][2] = sin(azimuth)
        matrix[1][3] = 0.0
        matrix[2][1] = elevationFactor * sin(azimuth)
        matrix[2][2] = elevationFactor * cos(azimuth)
        matrix[2][3] = cos(elevation)
        matrix[3][1] = cos(elevation) * sin(azimuth)
        matrix[3][2] = cos(elevation) * cos(azimuth)
        matrix[3][3] = sin(elevation)

        val cosBank = cos(bank)
        val sinBank = sin(bank)
        return Mat.matMatMult(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, cosBank, sinBank, 0.0),
                doubleArrayOf(0.0, -sinBank, cosBank, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            ),
            matrix,
        )
    }

    // JSXGraph: src/3d/view3d.js -> getCameraDistance.
    internal fun getCameraDistance(): Double {
        val automatic =
            cameraDistance == null || cameraDistance == 0.0
        var distance = when {
            automatic -> AUTO_CAMERA_DISTANCE
            projectionType == CENTRAL_PROJECTION -> cameraDistance
            else -> 1.0 / cameraDistance
        }
        if (projectionType == CENTRAL_PROJECTION) {
            val diameter = Mat.hypot(
                bbox3D[0][0] - bbox3D[0][1],
                bbox3D[1][0] - bbox3D[1][1],
                bbox3D[2][0] - bbox3D[2][1],
            )
            distance *= diameter
        }
        return distance
    }

    // JSXGraph: src/3d/view3d.js -> _updateCentralProjection.
    private fun updateCentralProjection(): Array<DoubleArray> {
        val farClipPlane = 20.0
        val nearClipPlane = 8.0
        val distance = getCameraDistance()

        boxToCam = matrix3DRot.deepCopy()
        boxToCam[3][0] = -distance
        focalDist = 1.0 / tan(0.5 * fieldOfView)
        val clip = arrayOf(
            doubleArrayOf(0.0, 0.0, 0.0, -1.0),
            doubleArrayOf(0.0, focalDist, 0.0, 0.0),
            doubleArrayOf(0.0, 0.0, focalDist, 0.0),
            doubleArrayOf(
                2.0 * farClipPlane * nearClipPlane /
                    (nearClipPlane - farClipPlane),
                0.0,
                0.0,
                (farClipPlane + nearClipPlane) /
                    (nearClipPlane - farClipPlane),
            ),
        )
        return Mat.matMatMult(clip, boxToCam)
    }

    // JSXGraph: src/3d/view3d.js -> update.
    override fun update(fromParent: Boolean): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        val mat2D = Mat.identity(3)
        matrix3DRot = getRotationFromAngles()
        shift = arrayOf(
            doubleArrayOf(1.0, 0.0, 0.0, 0.0),
            doubleArrayOf(
                -0.5 * (bbox3D[0][0] + bbox3D[0][1]),
                1.0,
                0.0,
                0.0,
            ),
            doubleArrayOf(
                -0.5 * (bbox3D[1][0] + bbox3D[1][1]),
                0.0,
                1.0,
                0.0,
            ),
            doubleArrayOf(
                -0.5 * (bbox3D[2][0] + bbox3D[2][1]),
                0.0,
                0.0,
                1.0,
            ),
        )

        if (projectionType == CENTRAL_PROJECTION) {
            val viewportSize = 2.0 * 0.4
            mat2D[1][1] = size[0] / viewportSize
            mat2D[2][2] = size[1] / viewportSize
            mat2D[1][0] =
                llftCorner[0] + mat2D[1][1] * 0.5 * viewportSize
            mat2D[2][0] =
                llftCorner[1] + mat2D[2][2] * 0.5 * viewportSize
            viewPortTransform = mat2D
            matrix3D = Mat.matMatMult(
                updateCentralProjection(),
                shift,
            )
        } else {
            val distance = getCameraDistance()
            val stretch = arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, distance, 0.0, 0.0),
                doubleArrayOf(0.0, 0.0, distance, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, distance),
            )
            val width = bbox3D[0][1] - bbox3D[0][0]
            val height = bbox3D[1][1] - bbox3D[1][0]
            mat2D[1][1] = size[0] / width
            mat2D[2][2] = size[1] / height
            mat2D[1][0] =
                llftCorner[0] + mat2D[1][1] * 0.5 * width
            mat2D[2][0] =
                llftCorner[1] + mat2D[2][2] * 0.5 * height
            matrix3D = Mat.matMatMult(
                mat2D,
                Mat.matMatMult(
                    Mat.matMatMult(matrix3DRot, stretch),
                    shift,
                ).copyOfRange(0, 3),
            )
            viewPortTransform = null
            boxToCam = emptyArray()
            focalDist = -1.0
        }

        matrix3DRotShift = Mat.matMatMult(matrix3DRot, shift)
        return this
    }

    override fun updateRenderer(): GeometryElement {
        needsUpdate = false
        return this
    }

    // JSXGraph: src/3d/view3d.js -> worldToFocal.
    internal fun worldToFocal(
        world: DoubleArray,
        homogeneous: Boolean = true,
    ): DoubleArray {
        val focal = Mat.matVecMult(
            boxToCam,
            Mat.matVecMult(shift, world),
        )
        focal[3] -= focal[0] * focalDist
        if (homogeneous) {
            return focal
        }
        for (index in 1 until 4) {
            focal[index] /= focal[0]
        }
        return focal.copyOfRange(1, 4)
    }

    // JSXGraph: src/3d/view3d.js -> project3DTo2D.
    internal fun project3DTo2D(coordinates: DoubleArray): DoubleArray {
        val vector =
            if (coordinates.size == 3) {
                doubleArrayOf(
                    1.0,
                    coordinates[0],
                    coordinates[1],
                    coordinates[2],
                )
            } else {
                coordinates
            }
        val projected = Mat.matVecMult(matrix3D, vector)
        if (projectionType != CENTRAL_PROJECTION) {
            return projected
        }
        projected[1] /= projected[0]
        projected[2] /= projected[0]
        projected[3] /= projected[0]
        projected[0] /= projected[0]
        return Mat.matVecMult(
            viewPortTransform ?: Mat.identity(3),
            projected.copyOfRange(0, 3),
        )
    }

    internal fun project3DTo2D(
        x: Double,
        y: Double,
        z: Double,
    ): DoubleArray = project3DTo2D(doubleArrayOf(1.0, x, y, z))

    // JSXGraph: src/3d/view3d.js -> _getW0.
    private fun getW0(
        matrix: Array<DoubleArray>,
        coordinates2D: DoubleArray,
        distance: Double,
    ): DoubleArray {
        val inverse = Mat.inverse(matrix)
        if (inverse.isEmpty()) {
            return doubleArrayOf(Double.NaN, Double.NaN)
        }
        val first =
            inverse[0][0] +
                coordinates2D[1] * inverse[0][1] +
                coordinates2D[2] * inverse[0][2]
        val second =
            inverse[3][0] +
                coordinates2D[1] * inverse[3][1] +
                coordinates2D[2] * inverse[3][2]
        val determinant =
            distance * inverse[0][3] - inverse[3][3]
        val weight =
            (second * inverse[0][3] - first * inverse[3][3]) /
                determinant
        val height = (second - first * distance) / determinant
        return doubleArrayOf(1.0 / weight, height)
    }

    // JSXGraph: src/3d/view3d.js -> project2DTo3DPlane.
    internal fun project2DTo3DPlane(
        coordinates2D: DoubleArray,
        normal: DoubleArray,
        foot: DoubleArray,
    ): DoubleArray {
        val foot3D = foot.copyOfRange(1, foot.size)
        val normal3D = normal.copyOfRange(1, normal.size)
        val normalLength = Mat.norm(normal3D, 3)
        val distance =
            Mat.innerProduct(foot3D, normal3D, 3) / normalLength
        return if (projectionType != CENTRAL_PROJECTION) {
            val matrix = matrix3D.map(DoubleArray::copyOf).toMutableList()
            matrix += doubleArrayOf(
                0.0,
                normal3D[0],
                normal3D[1],
                normal3D[2],
            )
            val rightHandSide = coordinates2D.copyOf(4)
            rightHandSide[3] = distance
            if (matrix[2][3] == 1.0) {
                matrix[2][1] = Mat.eps * 0.001
                matrix[2][2] = Mat.eps * 0.001
            }
            solveOrNonFinite(matrix.toTypedArray(), rightHandSide)
        } else {
            val viewport = viewPortTransform
                ?: return nonFiniteHomogeneousCoordinates()
            val viewportCoordinates = when (
                val result = Numerics.Gauss(viewport, coordinates2D)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return nonFiniteHomogeneousCoordinates()
            }
            val weightAndHeight =
                getW0(matrix3D, viewportCoordinates, distance)
            val weight = weightAndHeight[0]
            val rightHandSide = doubleArrayOf(
                viewportCoordinates[0] * weight,
                viewportCoordinates[1] * weight,
                viewportCoordinates[2] * weight,
                weightAndHeight[1] * weight,
            )
            val matrix = matrix3D.map(DoubleArray::copyOf).toTypedArray()
            if (matrix[2][3] == 1.0) {
                matrix[2][1] = Mat.eps * 0.001
                matrix[2][2] = Mat.eps * 0.001
            }
            val solution = solveOrNonFinite(matrix, rightHandSide)
            if (solution[0] != 0.0) {
                solution[1] /= solution[0]
                solution[2] /= solution[0]
                solution[3] /= solution[0]
                solution[0] /= solution[0]
            }
            solution
        }
    }

    // JSXGraph: src/3d/view3d.js -> projectScreenToSegment.
    internal fun projectScreenToSegment(
        screen: DoubleArray,
        first: DoubleArray,
        second: DoubleArray,
    ): DoubleArray {
        val first2D = project3DTo2D(first).copyOfRange(1, 3)
        val second2D = project3DTo2D(second).copyOfRange(1, 3)
        val direction2D = doubleArrayOf(
            second2D[0] - first2D[0],
            second2D[1] - first2D[1],
        )
        val normSquared = Mat.innerProduct(direction2D, direction2D)
        val difference = doubleArrayOf(
            screen[0] - first2D[0],
            screen[1] - first2D[1],
        )
        val screenParameter =
            Mat.innerProduct(difference, direction2D) / normSquared
        val viewParameter =
            if (projectionType == CENTRAL_PROJECTION) {
                val midpoint = doubleArrayOf(
                    1.0,
                    0.5 * (first[1] + second[1]),
                    0.5 * (first[2] + second[2]),
                    0.5 * (first[3] + second[3]),
                )
                val midpoint2D =
                    project3DTo2D(midpoint).copyOfRange(1, 3)
                val midpointDifference = doubleArrayOf(
                    midpoint2D[0] - first2D[0],
                    midpoint2D[1] - first2D[1],
                )
                val midpointParameter =
                    Mat.innerProduct(midpointDifference, direction2D) /
                        normSquared
                (1.0 - midpointParameter) * screenParameter /
                    (
                        (1.0 - 2.0 * midpointParameter) *
                            screenParameter +
                            midpointParameter
                        )
            } else {
                screenParameter
            }
        val clamped = min(max(viewParameter, 0.0), 1.0)
        val complement = 1.0 - clamped
        return doubleArrayOf(
            1.0,
            complement * first[1] + clamped * second[1],
            complement * first[2] + clamped * second[2],
            complement * first[3] + clamped * second[3],
        )
    }

    // JSXGraph: src/3d/view3d.js -> project2DTo3DVertical.
    internal fun project2DTo3DVertical(
        coordinates2D: DoubleArray,
        baseCoordinates3D: DoubleArray,
    ): DoubleArray =
        projectScreenToSegment(
            screen = coordinates2D.copyOfRange(1, 3),
            first = doubleArrayOf(
                1.0,
                baseCoordinates3D[1],
                baseCoordinates3D[2],
                bbox3D[2][0],
            ),
            second = doubleArrayOf(
                1.0,
                baseCoordinates3D[1],
                baseCoordinates3D[2],
                bbox3D[2][1],
            ),
        )

    // JSXGraph: src/3d/view3d.js -> project3DToCube.
    internal fun project3DToCube(
        coordinates: DoubleArray,
    ): Pair<DoubleArray, Boolean> {
        var corrected = false
        for (dimension in 0 until 3) {
            val coordinateIndex = dimension + 1
            val lower = bbox3D[dimension][0]
            val upper = bbox3D[dimension][1]
            if (coordinates[coordinateIndex] < lower) {
                coordinates[coordinateIndex] = lower
                corrected = true
            }
            if (coordinates[coordinateIndex] > upper) {
                coordinates[coordinateIndex] = upper
                corrected = true
            }
        }
        if (coordinates[3] <= bbox3D[2][0]) {
            coordinates[3] = bbox3D[2][0]
            corrected = true
        }
        if (coordinates[3] >= bbox3D[2][1]) {
            coordinates[3] = bbox3D[2][1]
            corrected = true
        }
        return coordinates to corrected
    }

    // JSXGraph: src/3d/view3d.js -> intersectionLineCube.
    internal fun intersectionLineCube(
        point: DoubleArray,
        direction: DoubleArray,
        ratio: Double,
    ): Double {
        val affineDirection =
            if (direction.size == 3) {
                direction
            } else {
                direction.copyOfRange(1, direction.size)
            }
        var result = ratio
        for (dimension in 0 until 3) {
            if (affineDirection[dimension] != 0.0) {
                val first =
                    (bbox3D[dimension][0] - point[dimension + 1]) /
                        affineDirection[dimension]
                val second =
                    (bbox3D[dimension][1] - point[dimension + 1]) /
                        affineDirection[dimension]
                result =
                    if (ratio < 0.0) {
                        max(result, min(first, second))
                    } else {
                        min(result, max(first, second))
                    }
            }
        }
        return result
    }

    // JSXGraph: src/3d/view3d.js -> isInCube.
    internal fun isInCube(point: DoubleArray): Boolean {
        if (point.size != 4 || point[0] == 0.0) {
            return false
        }
        return point[1] > bbox3D[0][0] - Mat.eps &&
            point[1] < bbox3D[0][1] + Mat.eps &&
            point[2] > bbox3D[1][0] - Mat.eps &&
            point[2] < bbox3D[1][1] + Mat.eps &&
            point[3] > bbox3D[2][0] - Mat.eps &&
            point[3] < bbox3D[2][1] + Mat.eps
    }

    internal fun setAngles(
        azimuth: Double,
        elevation: Double,
        bank: Double,
    ): View3D {
        angles.az = azimuth
        angles.el = elevation
        angles.bank = bank
        prepareUpdate()
        return this
    }

    private fun solveOrNonFinite(
        matrix: Array<DoubleArray>,
        rightHandSide: DoubleArray,
    ): DoubleArray =
        when (val result = Numerics.Gauss(matrix, rightHandSide)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> nonFiniteHomogeneousCoordinates()
        }

    internal companion object {
        private const val VIEW_3D_ID_PREFIX = "V"
        private const val VIEW_3D_ELEMENT_TYPE = "view3d"
        private const val CENTRAL_PROJECTION = "central"
        private const val AUTO_CAMERA_DISTANCE = 1.01

        // JSXGraph: src/options3d.js -> Options3D.view3d.
        internal const val DEFAULT_AZIMUTH: Double = 1.0
        internal const val DEFAULT_ELEVATION: Double = 0.3
        internal const val DEFAULT_BANK: Double = 0.0
        internal const val DEFAULT_FIELD_OF_VIEW: Double = 2.0 * PI / 5.0

        // JSXGraph: src/3d/view3d.js -> createView3D / View3D constructor.
        internal fun create(
            board: Board,
            lowerLeftCorner: DoubleArray,
            size: DoubleArray,
            boundingBox: Array<DoubleArray>,
            projection: String = "parallel",
            azimuth: Double = DEFAULT_AZIMUTH,
            elevation: Double = DEFAULT_ELEVATION,
            bank: Double = DEFAULT_BANK,
            cameraDistance: Double? = null,
            fieldOfView: Double = DEFAULT_FIELD_OF_VIEW,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<View3D, View3DError> {
            if (lowerLeftCorner.size != 2) {
                return GMResult.Err(
                    View3DError.InvalidLowerLeftCorner(
                        lowerLeftCorner.size,
                    ),
                )
            }
            if (size.size != 2) {
                return GMResult.Err(
                    View3DError.InvalidSize(size.size),
                )
            }
            if (
                boundingBox.size != 3 ||
                boundingBox.any { it.size != 2 }
            ) {
                return GMResult.Err(
                    View3DError.InvalidBoundingBox(
                        actualDimensions = boundingBox.size,
                        dimensionSizes =
                            boundingBox.map(DoubleArray::size),
                    ),
                )
            }
            val view = View3D(
                board = board,
                llftCorner = lowerLeftCorner.copyOf(),
                size = size.copyOf(),
                bbox3D =
                    boundingBox.map(DoubleArray::copyOf).toTypedArray(),
                projection = projection,
                azimuth = azimuth,
                elevation = elevation,
                bank = bank,
                cameraDistance = cameraDistance,
                fieldOfView = fieldOfView,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            return when (
                val registration =
                    board.setId(view, VIEW_3D_ID_PREFIX)
            ) {
                is GMResult.Ok -> {
                    view.update()
                    GMResult.Ok(view)
                }
                is GMResult.Err -> GMResult.Err(
                    View3DError.Registration(registration.error),
                )
            }
        }

        private fun nonFiniteHomogeneousCoordinates(): DoubleArray =
            doubleArrayOf(
                0.0,
                Double.NaN,
                Double.NaN,
                Double.NaN,
            )
    }
}

private fun Array<DoubleArray>.deepCopy(): Array<DoubleArray> =
    map(DoubleArray::copyOf).toTypedArray()
