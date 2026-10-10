/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/view3d.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Geometry
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.random.Random

// JSXGraph: src/base/element.js -> evalVisProp('projection')
// and src/3d/view3d.js -> update.
internal sealed interface View3DProjectionDynamicError {
    data class Rejected(
        val reason: String,
    ) : View3DProjectionDynamicError
}

internal fun interface View3DProjectionEvaluator {
    fun evaluate(): GMResult<String, View3DProjectionDynamicError>
}

internal sealed interface View3DProjectionSource {
    data class Value(
        val value: String,
    ) : View3DProjectionSource

    data class Dynamic(
        val evaluator: View3DProjectionEvaluator,
    ) : View3DProjectionSource
}

internal data class View3DAxisNavigationAttributes(
    val pointerEnabled: Boolean = true,
    val pointerSpeed: Double = 1.0,
    val pointerOutside: Boolean = true,
    val pointerButton: Int = -1,
    val pointerKey: String = "none",
    val keyboardEnabled: Boolean = true,
    val keyboardStepDegrees: Double = 10.0,
    val keyboardKey: String = "ctrl",
    val continuous: Boolean = true,
    val minimum: Double,
    val maximum: Double,
)

internal data class View3DTrackballAttributes(
    val enabled: Boolean = false,
    val pointerOutside: Boolean = true,
    val pointerButton: Int = -1,
    val pointerKey: String = "none",
)

internal data class View3DNavigationEvent(
    val screenX: Double = 0.0,
    val screenY: Double = 0.0,
    val boardX: Double = screenX,
    val boardY: Double = screenY,
    val deltaY: Double = 0.0,
    val button: Int = -1,
    val key: String? = null,
    val shiftKey: Boolean = false,
    val controlKey: Boolean = false,
    val insideBoard: Boolean = true,
)

internal fun interface View3DAzimuthAnimationScheduler {
    fun schedule(
        delayMilliseconds: Long,
        action: () -> Unit,
    )
}

internal data class View3DIntersectionData(
    val x: DoubleArray,
    val y: DoubleArray,
    val z: DoubleArray,
)

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

    data class ProjectionEvaluation(
        val error: View3DProjectionDynamicError,
    ) : View3DError

    data class Registration(
        val error: BoardError,
    ) : View3DError
}

/**
 * Source-mapped camera and projection kernel of JXG.View3D.
 *
 * Browser event registration and slider widgets stay in host adapters. The
 * camera, navigation, shader, and depth-order kernels remain portable here.
 */
internal class View3D private constructor(
    board: Board,
    internal val llftCorner: DoubleArray,
    internal val size: DoubleArray,
    internal val bbox3D: Array<DoubleArray>,
    projection: String,
    private var projectionSource: View3DProjectionSource,
    azimuth: Double,
    elevation: Double,
    bank: Double,
    private val cameraDistanceSource: Double?,
    private val fieldOfView: Double,
    private val verticalDragEnabled: Boolean,
    private val verticalDragKey: String,
    private val azimuthNavigation: View3DAxisNavigationAttributes,
    private val elevationNavigation: View3DAxisNavigationAttributes,
    private val bankNavigation: View3DAxisNavigationAttributes,
    private val trackballAttributes: View3DTrackballAttributes,
    private val storedViews: List<DoubleArray>,
    internal val depthOrderEnabled: Boolean,
    internal val depthOrderLayers: IntArray,
    private val azimuthAnimationScheduler:
        View3DAzimuthAnimationScheduler?,
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
    internal val depthOrdered =
        linkedMapOf<Int, List<GeometryElement3D>>()
    internal val resolvedShaderColors = linkedMapOf<String, String>()

    // JSXGraph: src/3d/view3d.js -> View3D constructor / createView3D.
    internal var defaultAxes: Axes3D? = null
        private set

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
    internal var trackballEnabled: Boolean = trackballAttributes.enabled
        private set
    internal var zIndexMin: Double = Double.POSITIVE_INFINITY
        private set
    internal var zIndexMax: Double = Double.NEGATIVE_INFINITY
        private set
    internal var currentView: Int = 0
        private set
    private var pendingProjection: String? = projection
    private var cameraDistanceOverride: Double? = null
    private var trackball: TrackballState? = null
    private var lastPointerX: Double = 0.0
    private var lastPointerY: Double = 0.0
    private var hasMoveAzimuthElevation: Boolean = false
    private var hasMoveAzimuth: Boolean = false
    private var hasMoveElevation: Boolean = false
    private var hasMoveBank: Boolean = false
    private var hasMoveTrackball: Boolean = false
    private var navigationActive: Boolean = false
    private var azimuthAnimationActive: Boolean = false
    private val azimuthSlider = AngleSliderState(
        minimum = azimuthNavigation.minimum,
        maximum = azimuthNavigation.maximum,
        value = azimuth,
    )
    private val elevationSlider = AngleSliderState(
        minimum = elevationNavigation.minimum,
        maximum = elevationNavigation.maximum,
        value = elevation,
    )
    private val bankSlider = AngleSliderState(
        minimum = bankNavigation.minimum,
        maximum = bankNavigation.maximum,
        value = bank,
    )
    internal var evaluationError: View3DError? = null
        private set

    internal val pointerNavigationEnabled: Boolean
        get() =
            trackballAttributes.enabled ||
                azimuthNavigation.pointerEnabled ||
                elevationNavigation.pointerEnabled ||
                bankNavigation.pointerEnabled

    internal val keyboardNavigationEnabled: Boolean
        get() =
            azimuthNavigation.keyboardEnabled ||
                elevationNavigation.keyboardEnabled ||
                bankNavigation.keyboardEnabled ||
                storedViews.isNotEmpty()

    internal val pointerNavigationActive: Boolean
        get() =
            hasMoveAzimuthElevation ||
                hasMoveAzimuth ||
                hasMoveElevation ||
                hasMoveBank ||
                hasMoveTrackball

    init {
        elType = VIEW_3D_ELEMENT_TYPE
        matrix3DRot = getRotationFromAngles()
        updateAngleSliderBounds()
    }

    internal data class Angles(
        var az: Double,
        var el: Double,
        var bank: Double,
    )

    private data class AngleSliderState(
        var minimum: Double,
        var maximum: Double,
        var value: Double,
    ) {
        fun updateValue(next: Double) {
            value = Mat.clamp(next, minimum, maximum)
        }
    }

    private data class TrackballState(
        val deltaX: Double,
        val deltaY: Double,
        val x: Double,
        val y: Double,
    )

    // JSXGraph: src/3d/view3d.js -> select, ID/name branch.
    internal fun select(reference: String): GeometryElement3D? =
        objects[reference] ?: elementsByName[reference]

    // JSXGraph: src/3d/view3d.js -> setAnglesFromRotation.
    internal fun setAnglesFromRotation(): View3D {
        var remaining = matrix3DRot
        val bankRadius = sqrt(
            remaining[1][3] * remaining[1][3] +
                remaining[2][3] * remaining[2][3],
        )
        val cosBank: Double
        val sinBank: Double
        if (bankRadius > Mat.eps) {
            cosBank = remaining[2][3] / bankRadius
            sinBank = remaining[1][3] / bankRadius
        } else {
            cosBank = cos(angles.bank)
            sinBank = sin(angles.bank)
        }
        remaining = Mat.matMatMult(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, cosBank, -sinBank, 0.0),
                doubleArrayOf(0.0, sinBank, cosBank, 0.0),
                doubleArrayOf(0.0, 0.0, 0.0, 1.0),
            ),
            remaining,
        )
        angles.bank = atan2(sinBank, cosBank)

        val cosElevation = remaining[2][3]
        val sinElevation = remaining[3][3]
        remaining = Mat.matMatMult(
            arrayOf(
                doubleArrayOf(1.0, 0.0, 0.0, 0.0),
                doubleArrayOf(0.0, 1.0, 0.0, 0.0),
                doubleArrayOf(
                    0.0,
                    0.0,
                    cosElevation,
                    sinElevation,
                ),
                doubleArrayOf(
                    0.0,
                    0.0,
                    -sinElevation,
                    cosElevation,
                ),
            ),
            remaining,
        )
        angles.el = atan2(sinElevation, cosElevation)

        val cosAzimuth = -remaining[1][1]
        val sinAzimuth = remaining[3][1]
        angles.az = atan2(sinAzimuth, cosAzimuth)
        if (angles.az < 0.0) {
            angles.az += 2.0 * PI
        }
        setSlidersFromAngles()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> anglesHaveMoved.
    internal fun anglesHaveMoved(): Boolean =
        hasMoveAzimuth ||
            hasMoveElevation ||
            abs(angles.az - azimuthSlider.value) > Mat.eps ||
            abs(angles.el - elevationSlider.value) > Mat.eps ||
            abs(angles.bank - bankSlider.value) > Mat.eps

    // JSXGraph: src/3d/view3d.js -> getAnglesFromSliders.
    internal fun getAnglesFromSliders(): View3D {
        angles.az = azimuthSlider.value
        angles.el = elevationSlider.value
        angles.bank = bankSlider.value
        return this
    }

    // JSXGraph: src/3d/view3d.js -> setSlidersFromAngles.
    internal fun setSlidersFromAngles(): View3D {
        azimuthSlider.updateValue(angles.az)
        elevationSlider.updateValue(angles.el)
        bankSlider.updateValue(angles.bank)
        return this
    }

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

    // JSXGraph: src/3d/view3d.js -> _projectToSphere.
    internal fun projectToSphere(
        radius: Double,
        x: Double,
        y: Double,
    ): Double {
        val distance = Mat.hypot(x, y)
        return if (distance < radius * 0.7071067811865475) {
            sqrt(radius * radius - distance * distance)
        } else {
            val tangent = radius / 1.414213562373095
            tangent * tangent / distance
        }
    }

    // JSXGraph: src/3d/view3d.js -> updateProjectionTrackball.
    internal fun updateProjectionTrackball(): Array<DoubleArray> {
        val state = trackball ?: return matrix3DRot
        var rotation = Mat.identity(4)
        val squaredDistance =
            state.deltaX * state.deltaX + state.deltaY * state.deltaY
        if (squaredDistance > Mat.eps) {
            val radius =
                (
                    size[0] * board.unitX +
                        size[1] * board.unitY
                    ) * 0.25
            val second = doubleArrayOf(
                state.x,
                state.y,
                projectToSphere(radius, state.x, state.y),
            )
            val firstX = state.x - state.deltaX
            val firstY = state.y - state.deltaY
            val first = doubleArrayOf(
                firstX,
                firstY,
                projectToSphere(radius, firstX, firstY),
            )
            val axis = Mat.crossProduct(first, second)
            val axisLength = Mat.hypot(axis[0], axis[1], axis[2])
            if (axisLength > Mat.eps) {
                for (index in axis.indices) {
                    axis[index] /= axisLength
                }
                val sineInput = Mat.clamp(
                    Geometry.distance(second, first, 3) /
                        (2.0 * radius),
                    -1.0,
                    1.0,
                )
                val theta = 2.0 * asin(sineInput)
                val cosine = cos(theta)
                val complement = 1.0 - cosine
                val sine = sin(theta)

                rotation[1][1] =
                    cosine + axis[0] * axis[0] * complement
                rotation[2][1] =
                    axis[1] * axis[0] * complement + axis[2] * sine
                rotation[3][1] =
                    axis[2] * axis[0] * complement - axis[1] * sine
                rotation[1][2] =
                    axis[0] * axis[1] * complement - axis[2] * sine
                rotation[2][2] =
                    cosine + axis[1] * axis[1] * complement
                rotation[3][2] =
                    axis[2] * axis[1] * complement + axis[0] * sine
                rotation[1][3] =
                    axis[0] * axis[2] * complement + axis[1] * sine
                rotation[2][3] =
                    axis[1] * axis[2] * complement - axis[0] * sine
                rotation[3][3] =
                    cosine + axis[2] * axis[2] * complement
            }
        }
        rotation = Mat.matMatMult(rotation, matrix3DRot)
        return rotation
    }

    // JSXGraph: src/3d/view3d.js -> updateAngleSliderBounds.
    internal fun updateAngleSliderBounds(): View3D {
        trackballEnabled = trackballAttributes.enabled
        if (trackballEnabled) {
            azimuthSlider.minimum = 0.0
            azimuthSlider.maximum = 2.0 * PI
            elevationSlider.minimum = -0.5 * PI
            elevationSlider.maximum = 0.5 * PI
            bankSlider.minimum = -PI
            bankSlider.maximum = PI
        } else {
            azimuthSlider.minimum = azimuthNavigation.minimum
            azimuthSlider.maximum = azimuthNavigation.maximum
            elevationSlider.minimum = elevationNavigation.minimum
            elevationSlider.maximum = elevationNavigation.maximum
            bankSlider.minimum = bankNavigation.minimum
            bankSlider.maximum = bankNavigation.maximum
        }

        if (trackballEnabled) {
            val elevationCover = Mat.mod(angles.el, 2.0 * PI)
            if (
                elevationCover > 0.5 * PI &&
                elevationCover < 1.5 * PI
            ) {
                angles.el = PI - elevationCover
                angles.az = Mat.wrap(
                    angles.az + PI,
                    azimuthSlider.minimum,
                    azimuthSlider.maximum,
                )
                angles.bank = Mat.wrap(
                    angles.bank + PI,
                    bankSlider.minimum,
                    bankSlider.maximum,
                )
            }
            angles.az = Mat.wrap(
                angles.az,
                azimuthSlider.minimum,
                azimuthSlider.maximum,
            )
            angles.el = Mat.wrap(
                angles.el,
                elevationSlider.minimum,
                elevationSlider.maximum,
            )
            angles.bank = Mat.wrap(
                angles.bank,
                bankSlider.minimum,
                bankSlider.maximum,
            )
        } else {
            val elevationMidpoint =
                0.5 * (
                    elevationSlider.minimum +
                        elevationSlider.maximum
                    )
            val equivalent = Mat.wrap(
                angles.el,
                elevationMidpoint - PI,
                elevationMidpoint + PI,
            )
            val flippedEquivalent = Mat.wrap(
                PI - angles.el,
                elevationMidpoint - PI,
                elevationMidpoint + PI,
            )
            val equivalentLoss = intervalLoss(
                equivalent,
                elevationSlider.minimum,
                elevationSlider.maximum,
            )
            val flippedLoss = intervalLoss(
                flippedEquivalent,
                elevationSlider.minimum,
                elevationSlider.maximum,
            )
            if (equivalentLoss <= flippedLoss) {
                angles.el = Mat.clamp(
                    equivalent,
                    elevationSlider.minimum,
                    elevationSlider.maximum,
                )
            } else {
                angles.el = Mat.clamp(
                    flippedEquivalent,
                    elevationSlider.minimum,
                    elevationSlider.maximum,
                )
                angles.az = Mat.wrap(
                    angles.az + PI,
                    azimuthSlider.minimum,
                    azimuthSlider.maximum,
                )
                angles.bank = Mat.wrap(
                    angles.bank + PI,
                    bankSlider.minimum,
                    bankSlider.maximum,
                )
            }
            angles.az = Mat.wrapAndClamp(
                angles.az,
                azimuthSlider.minimum,
                azimuthSlider.maximum,
                2.0 * PI,
            )
            angles.bank = Mat.wrapAndClamp(
                angles.bank,
                bankSlider.minimum,
                bankSlider.maximum,
                2.0 * PI,
            )
            matrix3DRot = getRotationFromAngles()
        }
        setSlidersFromAngles()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> getCameraDistance.
    internal fun getCameraDistance(): Double {
        val configuredDistance =
            cameraDistanceOverride ?: cameraDistanceSource
        val automatic =
            configuredDistance == null || configuredDistance == 0.0
        var distance = when {
            automatic -> AUTO_CAMERA_DISTANCE
            projectionType == CENTRAL_PROJECTION -> configuredDistance
            else -> 1.0 / configuredDistance
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
        val projectionResult = pendingProjection?.let { initial ->
            pendingProjection = null
            GMResult.Ok(initial)
        } ?: evaluateProjectionSource(projectionSource)
        when (val result = projectionResult) {
            is GMResult.Ok -> {
                projectionType = result.value.lowercase()
                evaluationError = null
            }
            is GMResult.Err -> {
                evaluationError = result.error
                return this
            }
        }
        val mat2D = Mat.identity(3)
        if (trackballEnabled != trackballAttributes.enabled) {
            updateAngleSliderBounds()
        }
        if (hasMoveTrackball) {
            matrix3DRot = updateProjectionTrackball()
            setAnglesFromRotation()
        } else if (anglesHaveMoved()) {
            getAnglesFromSliders()
            matrix3DRot = getRotationFromAngles()
        }
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

    // JSXGraph: src/3d/view3d.js -> compareDepth.
    internal fun compareDepth(
        first: GeometryElement3D,
        second: GeometryElement3D,
    ): Double = first.zIndex - second.zIndex

    // JSXGraph: src/3d/view3d.js -> updateZIndices.
    internal fun updateZIndices(): View3D {
        for (element in objects.values) {
            when (element) {
                is Line3D -> {
                    if (isVisible(element.element2D)) {
                        element.updateZIndex()
                    }
                }
                is Polygon3D -> {
                    if (isVisible(element.element2D)) {
                        element.updateZIndex()
                    }
                }
                else -> Unit
            }
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> updateShaders.
    internal fun updateShaders(): View3D {
        for (element in objects.values) {
            if (element is Face3D && element.faceAttributes.visible) {
                val color = element.shadedFillColor() ?: continue
                resolvedShaderColors[element.id] = color
                if (element.zIndex < zIndexMin) {
                    zIndexMin = element.zIndex
                } else if (element.zIndex > zIndexMax) {
                    zIndexMax = element.zIndex
                }
            }
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> updateDepthOrdering.
    internal fun updateDepthOrdering(): View3D {
        depthOrdered.clear()
        for (layer in depthOrderLayers) {
            depthOrdered[layer] = emptyList()
        }
        for (layer in depthOrderLayers) {
            depthOrdered[layer] = objects.values
                .asSequence()
                .filter {
                    it.type == Const.OBJECT_TYPE_FACE3D ||
                        it.type == Const.OBJECT_TYPE_LINE3D ||
                        it.type == Const.OBJECT_TYPE_POINT3D ||
                        it.type == Const.OBJECT_TYPE_POLYGON3D
                }
                .filter { isVisible(it.element2D) }
                .filter { elementLayer(it) == layer }
                .sortedWith { first, second ->
                    compareDepth(first, second).compareTo(0.0)
                }
                .toList()
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> updateRenderer.
    override fun updateRenderer(): GeometryElement {
        if (!needsUpdate) {
            return this
        }
        depthOrdered.clear()
        if (depthOrderEnabled && shift.isNotEmpty()) {
            updateZIndices()
            updateShaders()
            updateDepthOrdering()
        }
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

    // JSXGraph: src/3d/view3d.js -> View3D.getMesh.
    internal fun getMesh(
        function: (u: Double, v: Double) -> DoubleArray,
        intervalU: DoubleArray,
        intervalV: DoubleArray,
    ): CurveDataUpdate {
        val stepsU = intervalU[2]
        val stepsV = intervalV[2]
        val deltaU = (intervalU[1] - intervalU[0]) / stepsU
        val deltaV = (intervalV[1] - intervalV[0]) / stepsV
        val lastU = floor(stepsU).toInt()
        val lastV = floor(stepsV).toInt()
        val dataX = mutableListOf<Double>()
        val dataY = mutableListOf<Double>()

        for (indexU in 0..lastU) {
            val u = intervalU[0] + deltaU * indexU
            for (indexV in 0..lastV) {
                val v = intervalV[0] + deltaV * indexV
                val projected = project3DTo2D(function(u, v))
                dataX += projected[1]
                dataY += projected[2]
            }
            dataX += Double.NaN
            dataY += Double.NaN
        }

        for (indexV in 0..lastV) {
            val v = intervalV[0] + deltaV * indexV
            for (indexU in 0..lastU) {
                val u = intervalU[0] + deltaU * indexU
                val projected = project3DTo2D(function(u, v))
                dataX += projected[1]
                dataY += projected[2]
            }
            dataX += Double.NaN
            dataY += Double.NaN
        }

        return CurveDataUpdate(
            x = dataX.toDoubleArray(),
            y = dataY.toDoubleArray(),
        )
    }

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

    // JSXGraph: src/3d/view3d.js -> intersectionPlanePlane.
    internal fun intersectionPlanePlane(
        firstNormal: DoubleArray,
        firstDistance: Double,
        firstVector1: DoubleArray,
        firstVector2: DoubleArray,
        secondNormal: DoubleArray,
        secondDistance: Double,
        secondVector1: DoubleArray,
        secondVector2: DoubleArray,
    ): Array<DoubleArray?> {
        val thirdNormal = Mat.crossProduct(
            firstNormal.copyOfRange(1, 4),
            secondNormal.copyOfRange(1, 4),
        ).let { direction ->
            doubleArrayOf(
                0.0,
                direction[0],
                direction[1],
                direction[2],
            )
        }
        val point = Geometry.meet3Planes(
            firstNormal = firstNormal,
            firstDistance = firstDistance,
            secondNormal = secondNormal,
            secondDistance = secondDistance,
            thirdNormal = thirdNormal,
            thirdDistance = 0.0,
        )
        val direction = Geometry.meetPlanePlane(
            firstVector = firstVector1,
            secondVector = firstVector2,
            thirdVector = secondVector1,
            fourthVector = secondVector2,
        )
        val result = arrayOfNulls<DoubleArray>(2)
        val positiveRatio = intersectionLineCube(
            point = point,
            direction = direction,
            ratio = Double.POSITIVE_INFINITY,
        )
        val positivePoint = Mat.axpy(positiveRatio, direction, point)
        if (isInCube(positivePoint)) {
            result[0] = positivePoint
        }
        val negativeRatio = intersectionLineCube(
            point = point,
            direction = direction,
            ratio = Double.NEGATIVE_INFINITY,
        )
        val negativePoint = Mat.axpy(negativeRatio, direction, point)
        if (isInCube(negativePoint)) {
            result[1] = negativePoint
        }
        return result
    }

    // JSXGraph: src/3d/view3d.js -> intersectionPlaneFace.
    internal fun intersectionPlaneFace(
        plane: Plane3D,
        face: Face3D,
    ): List<DoubleArray> {
        val thirdNormal = Mat.crossProduct(
            plane.normal.copyOfRange(1, 4),
            face.normal.copyOfRange(1, 4),
        ).let { direction ->
            doubleArrayOf(
                0.0,
                direction[0],
                direction[1],
                direction[2],
            )
        }
        val point = Geometry.meet3Planes(
            firstNormal = plane.normal,
            firstDistance = plane.d,
            secondNormal = face.normal,
            secondDistance = face.d,
            thirdNormal = thirdNormal,
            thirdDistance = 0.0,
        )
        val direction = Geometry.meetPlanePlane(
            firstVector = plane.vec1,
            secondVector = plane.vec2,
            thirdVector = face.vec1,
            fourthVector = face.vec2,
        )
        val faceKeys = face.polyhedron.faceKeys[face.faceNumber]
        val result = mutableListOf<DoubleArray>()
        for (index in faceKeys.indices) {
            val first =
                face.polyhedron.coords[faceKeys[index]] ?: continue
            val second =
                face.polyhedron.coords[
                    faceKeys[(index + 1) % faceKeys.size]
                ] ?: continue
            val edge = doubleArrayOf(
                0.0,
                second[1] - first[1],
                second[2] - first[2],
                second[3] - first[3],
            )
            val x1 = Random.nextDouble()
            val y1 = Random.nextDouble()
            val x2 = Random.nextDouble()
            val y2 = Random.nextDouble()
            val matrix = arrayOf(
                doubleArrayOf(
                    x1 * direction[1] + y1 * direction[3],
                    x1 * -edge[1] + y1 * -edge[3],
                ),
                doubleArrayOf(
                    x2 * direction[2] + y2 * direction[3],
                    x2 * -edge[2] + y2 * -edge[3],
                ),
            )
            val rightHandSide = doubleArrayOf(
                x1 * (first[1] - point[1]) +
                    y1 * (first[3] - point[3]),
                x2 * (first[2] - point[2]) +
                    y2 * (first[3] - point[3]),
            )
            val solution = when (
                val solved = Numerics.Gauss(matrix, rightHandSide)
            ) {
                is GMResult.Ok -> solved.value
                is GMResult.Err -> continue
            }
            val ratio = solution[1]
            if (ratio > -Mat.eps && ratio < 1.0 + Mat.eps) {
                result += doubleArrayOf(
                    1.0,
                    first[1] + ratio * edge[1],
                    first[2] + ratio * edge[2],
                    first[3] + ratio * edge[3],
                )
            }
        }
        return result
    }

    // JSXGraph: src/3d/view3d.js -> intersectionPlanePolyhedron.
    internal fun intersectionPlanePolyhedron(
        plane: Plane3D,
        polyhedron: Polyhedron3D,
    ): View3DIntersectionData {
        val segments = mutableListOf<Array<DoubleArray>>()
        for (face in polyhedron.faces) {
            if (
                polyhedron.definition.faceKeys[face.faceNumber].size < 3
            ) {
                continue
            }
            val segment = intersectionPlaneFace(plane, face)
            if (segment.size < 2) {
                continue
            }
            val duplicate = segments.any { existing ->
                (
                    Geometry.distance(segment[0], existing[0], 4) <
                        INTERSECTION_EPSILON &&
                        Geometry.distance(segment[1], existing[1], 4) <
                        INTERSECTION_EPSILON
                    ) ||
                    (
                        Geometry.distance(segment[0], existing[1], 4) <
                            INTERSECTION_EPSILON &&
                            Geometry.distance(segment[1], existing[0], 4) <
                            INTERSECTION_EPSILON
                        )
            }
            if (!duplicate) {
                segments += arrayOf(
                    segment[0].copyOf(),
                    segment[1].copyOf(),
                )
            }
        }
        if (segments.isEmpty()) {
            return View3DIntersectionData(
                x = DoubleArray(0),
                y = DoubleArray(0),
                z = DoubleArray(0),
            )
        }

        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        val z = mutableListOf<Double>()
        val firstSegment = 0
        var position = firstSegment
        var endpoint = 0
        var visits = 0
        do {
            val current = segments[position][endpoint]
            x += current[1]
            y += current[2]
            z += current[3]
            endpoint = (endpoint + 1) % 2
            val pending = segments[position][endpoint]
            val previousPosition = position
            for (candidate in segments.indices) {
                if (
                    candidate != position &&
                    Geometry.distance(
                        pending,
                        segments[candidate][0],
                    ) < INTERSECTION_EPSILON
                ) {
                    position = candidate
                    endpoint = 0
                    break
                }
                if (
                    candidate != position &&
                    Geometry.distance(
                        pending,
                        segments[candidate][1],
                    ) < INTERSECTION_EPSILON
                ) {
                    position = candidate
                    endpoint = 1
                    break
                }
            }
            visits += 1
            if (
                position == previousPosition ||
                visits > segments.size * 2
            ) {
                break
            }
        } while (position != firstSegment)
        if (x.isNotEmpty()) {
            x += x[0]
            y += y[0]
            z += z[0]
        }
        return View3DIntersectionData(
            x = x.toDoubleArray(),
            y = y.toDoubleArray(),
            z = z.toDoubleArray(),
        )
    }

    internal fun setAngles(
        azimuth: Double,
        elevation: Double,
        bank: Double,
    ): View3D {
        angles.az = azimuth
        angles.el = elevation
        angles.bank = bank
        matrix3DRot = getRotationFromAngles()
        setSlidersFromAngles()
        prepareUpdate()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> freeR.
    internal fun freeR(): View3D {
        cameraDistanceOverride = null
        prepareUpdate()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> setView.
    internal fun setView(
        azimuth: Double,
        elevation: Double,
        cameraDistance: Double? = null,
    ): View3D {
        if (cameraDistance != null) {
            cameraDistanceOverride = cameraDistance
        }
        azimuthSlider.updateValue(azimuth)
        elevationSlider.updateValue(elevation)
        board.update()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> nextView.
    internal fun nextView(): View3D {
        if (storedViews.isNotEmpty()) {
            setCurrentView((currentView + 1) % storedViews.size)
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> previousView.
    internal fun previousView(): View3D {
        if (storedViews.isNotEmpty()) {
            setCurrentView(
                (currentView + storedViews.size - 1) %
                    storedViews.size,
            )
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> setCurrentView.
    internal fun setCurrentView(index: Int): View3D {
        if (storedViews.isEmpty()) {
            return this
        }
        val normalized =
            ((index % storedViews.size) + storedViews.size) %
                storedViews.size
        val values = storedViews[normalized]
        if (values.size >= 2) {
            setView(
                azimuth = values[0],
                elevation = values[1],
                cameraDistance = values.getOrNull(2),
            )
            currentView = normalized
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> animateAzimuth.
    internal fun animateAzimuth(): View3D {
        azimuthAnimationActive = true
        advanceAzimuthAnimation()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> stopAzimuth.
    internal fun stopAzimuth(): View3D {
        azimuthAnimationActive = false
        return this
    }

    // JSXGraph: src/3d/view3d.js -> _az_elEventHandler.
    internal fun azimuthElevationEventHandler(
        event: View3DNavigationEvent,
    ): View3D {
        val deltaX = event.screenX - lastPointerX
        val deltaY = event.screenY - lastPointerY
        lastPointerX = event.screenX
        lastPointerY = event.screenY
        if (!navigationActive && event.key == null) {
            return this
        }
        var azimuth = azimuthSlider.value
        var elevation = elevationSlider.value
        val speed =
            (azimuthSlider.maximum - azimuthSlider.minimum) /
                board.canvasWidth *
                azimuthNavigation.pointerSpeed
        if (
            azimuthNavigation.pointerEnabled &&
            deltaX != 0.0 &&
            event.key == null
        ) {
            azimuth += deltaX * speed
        }
        if (
            elevationNavigation.pointerEnabled &&
            deltaY != 0.0 &&
            event.key == null
        ) {
            elevation += deltaY * speed
        }
        azimuth = constrainAngle(
            azimuth,
            azimuthSlider,
            azimuthNavigation.continuous,
        )
        elevation = constrainAngle(
            elevation,
            elevationSlider,
            elevationNavigation.continuous && !trackballEnabled,
        )
        return setView(azimuth, elevation)
    }

    // JSXGraph: src/3d/view3d.js -> _azEventHandler.
    internal fun azimuthEventHandler(
        event: View3DNavigationEvent,
    ): View3D {
        val delta = event.screenX - lastPointerX
        lastPointerX = event.screenX
        if (!navigationActive && event.key == null) {
            return this
        }
        var azimuth = azimuthSlider.value
        if (azimuthNavigation.keyboardEnabled) {
            val step = azimuthNavigation.keyboardStepDegrees * PI / 180.0
            when (event.key) {
                "ArrowRight" -> azimuth += step
                "ArrowLeft" -> azimuth -= step
            }
        }
        if (
            azimuthNavigation.pointerEnabled &&
            delta != 0.0 &&
            event.key == null
        ) {
            val speed =
                (azimuthSlider.maximum - azimuthSlider.minimum) /
                    board.canvasWidth *
                    azimuthNavigation.pointerSpeed
            azimuth += delta * speed
        }
        azimuth = constrainAngle(
            azimuth,
            azimuthSlider,
            azimuthNavigation.continuous,
        )
        return setView(azimuth, elevationSlider.value)
    }

    // JSXGraph: src/3d/view3d.js -> _elEventHandler.
    internal fun elevationEventHandler(
        event: View3DNavigationEvent,
    ): View3D {
        val delta = event.screenY - lastPointerY
        lastPointerY = event.screenY
        if (!navigationActive && event.key == null) {
            return this
        }
        var elevation = elevationSlider.value
        if (elevationNavigation.keyboardEnabled) {
            val step =
                elevationNavigation.keyboardStepDegrees * PI / 180.0
            when (event.key) {
                "ArrowUp" -> elevation -= step
                "ArrowDown" -> elevation += step
            }
        }
        if (
            elevationNavigation.pointerEnabled &&
            delta != 0.0 &&
            event.key == null
        ) {
            val speed =
                (elevationSlider.maximum - elevationSlider.minimum) /
                    board.canvasHeight *
                    elevationNavigation.pointerSpeed
            elevation += delta * speed
        }
        elevation = constrainAngle(
            elevation,
            elevationSlider,
            elevationNavigation.continuous && !trackballEnabled,
        )
        return setView(azimuthSlider.value, elevation)
    }

    // JSXGraph: src/3d/view3d.js -> _bankEventHandler.
    internal fun bankEventHandler(
        event: View3DNavigationEvent,
    ): View3D {
        if (!navigationActive && event.key == null) {
            return this
        }
        var bank = bankSlider.value
        if (bankNavigation.keyboardEnabled) {
            val step = bankNavigation.keyboardStepDegrees * PI / 180.0
            when (event.key) {
                ".", "<" -> bank -= step
                ",", ">" -> bank += step
            }
        }
        if (
            bankNavigation.pointerEnabled &&
            event.deltaY != 0.0 &&
            event.key == null
        ) {
            val speed =
                (bankSlider.maximum - bankSlider.minimum) /
                    board.canvasHeight *
                    bankNavigation.pointerSpeed
            bank += event.deltaY * speed
        }
        bank = constrainAngle(
            bank,
            bankSlider,
            bankNavigation.continuous,
        )
        bankSlider.updateValue(bank)
        board.update()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> _trackballHandler.
    internal fun trackballHandler(
        event: View3DNavigationEvent,
    ): View3D {
        val centerUserX = llftCorner[0] + size[0] * 0.5
        val centerUserY = llftCorner[1] + size[1] * 0.5
        val centerScreenX =
            board.origin.scrCoords[1] + centerUserX * board.unitX
        val centerScreenY =
            board.origin.scrCoords[2] - centerUserY * board.unitY
        val deltaX = event.screenX - lastPointerX
        val deltaY = event.screenY - lastPointerY
        lastPointerX = event.screenX
        lastPointerY = event.screenY
        trackball = TrackballState(
            deltaX = deltaX,
            deltaY = -deltaY,
            x = event.boardX - centerScreenX,
            y = -(event.boardY - centerScreenY),
        )
        board.update()
        return this
    }

    // JSXGraph: src/3d/view3d.js -> pointermove handlers registered by
    // pointerDownHandler.
    internal fun pointerMoveHandler(
        event: View3DNavigationEvent,
    ): Boolean {
        if (
            hasMoveTrackball &&
            (trackballAttributes.pointerOutside || event.insideBoard)
        ) {
            trackballHandler(event)
            return true
        }
        if (
            hasMoveAzimuthElevation &&
            (azimuthNavigation.pointerOutside || event.insideBoard)
        ) {
            azimuthElevationEventHandler(event)
            return true
        }
        var handled = false
        if (
            hasMoveAzimuth &&
            (azimuthNavigation.pointerOutside || event.insideBoard)
        ) {
            azimuthEventHandler(event)
            handled = true
        }
        if (
            hasMoveElevation &&
            (elevationNavigation.pointerOutside || event.insideBoard)
        ) {
            elevationEventHandler(event)
            handled = true
        }
        return handled
    }

    // JSXGraph: src/3d/view3d.js -> wheel listener registered by
    // pointerDownHandler.
    internal fun wheelHandler(
        event: View3DNavigationEvent,
    ): Boolean {
        if (
            !hasMoveBank ||
            (!bankNavigation.pointerOutside && !event.insideBoard)
        ) {
            return false
        }
        bankEventHandler(event)
        return true
    }

    // JSXGraph: src/3d/view3d.js -> createView3D keydown listener.
    internal fun keyDownHandler(
        event: View3DNavigationEvent,
    ): Boolean {
        val key = event.key ?: return false
        var handled = false
        if (
            elevationNavigation.keyboardEnabled &&
            key in setOf("ArrowUp", "ArrowDown") &&
            modifierMatches(event, elevationNavigation.keyboardKey)
        ) {
            elevationEventHandler(event)
            handled = true
        }
        if (
            azimuthNavigation.keyboardEnabled &&
            key in setOf("ArrowLeft", "ArrowRight") &&
            modifierMatches(event, azimuthNavigation.keyboardKey)
        ) {
            azimuthEventHandler(event)
            handled = true
        }
        if (
            bankNavigation.keyboardEnabled &&
            key in setOf(",", "<", ".", ">") &&
            modifierMatches(event, bankNavigation.keyboardKey)
        ) {
            bankEventHandler(event)
            handled = true
        }
        when (key) {
            "PageUp" -> {
                nextView()
                handled = true
            }
            "PageDown" -> {
                previousView()
                handled = true
            }
        }
        return handled
    }

    // JSXGraph: src/3d/view3d.js -> pointerDownHandler.
    internal fun pointerDownHandler(
        event: View3DNavigationEvent,
    ): View3D {
        hasMoveAzimuthElevation = false
        hasMoveAzimuth = false
        hasMoveElevation = false
        hasMoveBank = false
        hasMoveTrackball = false
        if (board.mode != Board.BOARD_MODE_NONE) {
            navigationActive = false
            return this
        }
        navigationActive = true
        lastPointerX = event.screenX
        lastPointerY = event.screenY

        if (trackballEnabled) {
            if (
                eventMatches(
                    event,
                    trackballAttributes.pointerButton,
                    trackballAttributes.pointerKey,
                )
            ) {
                hasMoveTrackball = true
            }
            return this
        }
        if (
            azimuthNavigation.pointerEnabled &&
            elevationNavigation.pointerEnabled &&
            azimuthNavigation.pointerButton ==
            elevationNavigation.pointerButton &&
            azimuthNavigation.pointerKey ==
            elevationNavigation.pointerKey &&
            azimuthNavigation.pointerOutside ==
            elevationNavigation.pointerOutside &&
            eventMatches(
                event,
                azimuthNavigation.pointerButton,
                azimuthNavigation.pointerKey,
            )
        ) {
            hasMoveAzimuthElevation = true
        } else {
            hasMoveAzimuth =
                azimuthNavigation.pointerEnabled &&
                    eventMatches(
                        event,
                        azimuthNavigation.pointerButton,
                        azimuthNavigation.pointerKey,
                    )
            hasMoveElevation =
                elevationNavigation.pointerEnabled &&
                    eventMatches(
                        event,
                        elevationNavigation.pointerButton,
                        elevationNavigation.pointerKey,
                    )
        }
        hasMoveBank =
            bankNavigation.pointerEnabled &&
                eventMatches(
                    event,
                    bankNavigation.pointerButton,
                    bankNavigation.pointerKey,
                )
        return this
    }

    // JSXGraph: src/3d/view3d.js -> pointerUpHandler.
    internal fun pointerUpHandler(): View3D {
        hasMoveAzimuthElevation = false
        hasMoveAzimuth = false
        hasMoveElevation = false
        hasMoveBank = false
        hasMoveTrackball = false
        navigationActive = false
        return this
    }

    // JSXGraph: src/3d/view3d.js -> removeObject.
    internal fun removeObject(
        element: GeometryElement3D?,
        saveMethod: Boolean = false,
    ): View3D {
        if (element != null && objects[element.id] === element) {
            board.removeObject(element, saveMethod)
        }
        return this
    }

    internal fun removeObject(
        reference: String,
        saveMethod: Boolean = false,
    ): View3D = removeObject(select(reference), saveMethod)

    // JSXGraph: src/base/element.js -> setAttribute({projection})
    // and src/3d/view3d.js -> update.
    internal fun setProjection(projection: String): View3D {
        projectionSource = View3DProjectionSource.Value(projection)
        if (needsRegularUpdate) {
            board.update(this)
        } else {
            board.fullUpdate()
        }
        return this
    }

    // JSXGraph: src/3d/view3d.js -> isVerticalDrag.
    internal fun isVerticalDrag(): Boolean {
        if (!verticalDragEnabled) {
            return false
        }
        return when (verticalDragKey.lowercase()) {
            "shift" -> board.shiftKeyPressed
            "ctrl", "control" -> board.controlKeyPressed
            else -> false
        }
    }

    // JSXGraph: src/3d/view3d.js -> createView3D.
    internal fun createDefaultAxes(
        axesPosition: String = "center",
        planeTypes: Map<String, String> = emptyMap(),
        planeSurfaceAttributes:
            Map<String, Plane3DSurfaceAttributes> = emptyMap(),
        ticksAttributes: Map<String, Axes3DTicksAttributes> = emptyMap(),
        needsRegularUpdate: Boolean = true,
    ): GMResult<Axes3D, Axes3DError> =
        when (
            val result = Axes3D.create(
                view = this,
                axesPosition = axesPosition,
                planeTypes = planeTypes,
                planeSurfaceAttributes = planeSurfaceAttributes,
                ticksAttributes = ticksAttributes,
                needsRegularUpdate = needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> {
                defaultAxes = result.value
                result
            }
            is GMResult.Err -> result
        }

    override fun remove(): GeometryElement {
        defaultAxes?.let(board::removeObject)
        defaultAxes = null
        return super.remove()
    }

    private fun advanceAzimuthAnimation() {
        if (!azimuthAnimationActive) {
            return
        }
        val difference =
            azimuthSlider.maximum - azimuthSlider.minimum
        var position =
            (
                azimuthSlider.value + AZIMUTH_ANIMATION_STEP -
                    azimuthSlider.minimum
                ) / difference
        if (position > 1.0) {
            position = 0.0
        }
        azimuthSlider.value =
            azimuthSlider.minimum + position * difference
        board.update()
        azimuthAnimationScheduler?.schedule(
            AZIMUTH_ANIMATION_DELAY_MILLISECONDS,
        ) {
            advanceAzimuthAnimation()
        }
    }

    private fun constrainAngle(
        value: Double,
        slider: AngleSliderState,
        continuous: Boolean,
    ): Double =
        if (continuous) {
            Mat.wrap(value, slider.minimum, slider.maximum)
        } else if (value > 0.0) {
            min(slider.maximum, value)
        } else if (value < 0.0) {
            max(slider.minimum, value)
        } else {
            value
        }

    private fun eventMatches(
        event: View3DNavigationEvent,
        neededButton: Int,
        neededKey: String,
    ): Boolean {
        val buttonMatches =
            neededButton == -1 || neededButton == event.button
        val normalizedKey = neededKey.lowercase()
        val keyMatches =
            normalizedKey == "none" ||
                ("shift" in normalizedKey && event.shiftKey) ||
                (
                    ("ctrl" in normalizedKey ||
                        "control" in normalizedKey) &&
                        event.controlKey
                    )
        return buttonMatches && keyMatches
    }

    private fun modifierMatches(
        event: View3DNavigationEvent,
        neededKey: String,
    ): Boolean {
        val normalizedKey = neededKey.lowercase()
        return normalizedKey == "none" ||
            ("shift" in normalizedKey && event.shiftKey) ||
            (
                ("ctrl" in normalizedKey ||
                    "control" in normalizedKey) &&
                    event.controlKey
                )
    }

    private fun isVisible(element: GeometryElement?): Boolean =
        when (val visible = element?.getAttribute("visible")) {
            is Boolean -> visible
            else -> element != null
        }

    private fun elementLayer(element: GeometryElement3D): Int {
        if (element is Face3D) {
            return element.faceAttributes.layer
        }
        return (
            element.element2D?.getAttribute("layer") as? Number
            )?.toInt() ?: DEFAULT_3D_LAYER
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
        private const val DEFAULT_3D_LAYER = 12
        private const val INTERSECTION_EPSILON = 1.0e-12
        private const val AZIMUTH_ANIMATION_STEP = 0.1
        private const val AZIMUTH_ANIMATION_DELAY_MILLISECONDS = 200L

        // JSXGraph: src/options3d.js -> Options3D.view3d.
        internal const val DEFAULT_AZIMUTH: Double = 1.0
        internal const val DEFAULT_ELEVATION: Double = 0.3
        internal const val DEFAULT_BANK: Double = 0.0
        internal const val DEFAULT_FIELD_OF_VIEW: Double = 2.0 * PI / 5.0
        private val DEFAULT_STORED_VIEWS = listOf(
            doubleArrayOf(0.0, 1.57),
            doubleArrayOf(0.78, 0.62),
            doubleArrayOf(0.0, 0.0),
            doubleArrayOf(5.49, 0.62),
            doubleArrayOf(4.71, 0.0),
            doubleArrayOf(3.93, 0.62),
            doubleArrayOf(3.14, 0.0),
            doubleArrayOf(2.36, 0.62),
            doubleArrayOf(1.57, 1.57),
        )

        // JSXGraph: src/3d/view3d.js -> createView3D / View3D constructor.
        internal fun create(
            board: Board,
            lowerLeftCorner: DoubleArray,
            size: DoubleArray,
            boundingBox: Array<DoubleArray>,
            projection: String = "parallel",
            projectionSource: View3DProjectionSource? = null,
            azimuth: Double = DEFAULT_AZIMUTH,
            elevation: Double = DEFAULT_ELEVATION,
            bank: Double = DEFAULT_BANK,
            cameraDistance: Double? = null,
            fieldOfView: Double = DEFAULT_FIELD_OF_VIEW,
            verticalDragEnabled: Boolean = true,
            verticalDragKey: String = "shift",
            azimuthNavigation: View3DAxisNavigationAttributes =
                View3DAxisNavigationAttributes(
                    minimum = 0.0,
                    maximum = 2.0 * PI,
                ),
            elevationNavigation: View3DAxisNavigationAttributes =
                View3DAxisNavigationAttributes(
                    minimum = 0.0,
                    maximum = 2.0 * PI,
                ),
            bankNavigation: View3DAxisNavigationAttributes =
                View3DAxisNavigationAttributes(
                    pointerSpeed = 0.08,
                    minimum = -PI,
                    maximum = PI,
                ),
            trackballAttributes: View3DTrackballAttributes =
                View3DTrackballAttributes(),
            storedViews: List<DoubleArray> = DEFAULT_STORED_VIEWS,
            depthOrderEnabled: Boolean = true,
            depthOrderLayers: IntArray = intArrayOf(12, 13),
            azimuthAnimationScheduler:
                View3DAzimuthAnimationScheduler? = null,
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
            val resolvedProjectionSource =
                projectionSource
                    ?: View3DProjectionSource.Value(projection)
            val evaluatedProjection = when (
                val result =
                    evaluateProjectionSource(resolvedProjectionSource)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val view = View3D(
                board = board,
                llftCorner = lowerLeftCorner.copyOf(),
                size = size.copyOf(),
                bbox3D =
                    boundingBox.map(DoubleArray::copyOf).toTypedArray(),
                projection = evaluatedProjection,
                projectionSource = resolvedProjectionSource,
                azimuth = azimuth,
                elevation = elevation,
                bank = bank,
                cameraDistanceSource = cameraDistance,
                fieldOfView = fieldOfView,
                verticalDragEnabled = verticalDragEnabled,
                verticalDragKey = verticalDragKey,
                azimuthNavigation = azimuthNavigation,
                elevationNavigation = elevationNavigation,
                bankNavigation = bankNavigation,
                trackballAttributes = trackballAttributes,
                storedViews =
                    storedViews.map(DoubleArray::copyOf),
                depthOrderEnabled = depthOrderEnabled,
                depthOrderLayers = depthOrderLayers.copyOf(),
                azimuthAnimationScheduler =
                    azimuthAnimationScheduler,
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

        // JSXGraph 1.13.3: src/3d/view3d.js ->
        // updateAngleSliderBounds closure el_interval_loss.
        private fun intervalLoss(
            value: Double,
            minimum: Double,
            maximum: Double,
        ): Double =
            when {
                value < minimum -> minimum - value
                value > maximum -> value - maximum
                else -> 0.0
            }
    }
}

private fun Array<DoubleArray>.deepCopy(): Array<DoubleArray> =
    map(DoubleArray::copyOf).toTypedArray()

private fun evaluateProjectionSource(
    source: View3DProjectionSource,
): GMResult<String, View3DError> =
    when (source) {
        is View3DProjectionSource.Value -> GMResult.Ok(source.value)
        is View3DProjectionSource.Dynamic ->
            when (val result = source.evaluator.evaluate()) {
                is GMResult.Ok -> result
                is GMResult.Err -> GMResult.Err(
                    View3DError.ProjectionEvaluation(result.error),
                )
            }
    }
