/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/3d/face3d.js, src/3d/polyhedron3d.js
 * Copyright 2008-2026 Matthias Ehmann, Carsten Miller, Andreas Walter,
 * and Alfred Wassermann.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

internal sealed interface Polyhedron3DVertexSource {
    data class Point(
        val point: Point3D,
    ) : Polyhedron3DVertexSource

    data class Values(
        val values: List<Line3DCoordinateValue>,
    ) : Polyhedron3DVertexSource

    data class Function(
        val evaluator: Line3DArrayEvaluator,
    ) : Polyhedron3DVertexSource
}

internal data class Face3DLightAttributes(
    val type: Int = 1,
    val azimuth: Double = -45.0,
    val elevation: Double = 20.0,
    val bank: Double = 0.0,
    val direction: Int = -1,
)

internal data class Face3DShaderAttributes(
    val enabled: Boolean = false,
    val fixed: Boolean = true,
    val type: String = "angle",
    val hue: Double = 60.0,
    val saturation: Double = 90.0,
    val minimumLightness: Double = 30.0,
    val maximumLightness: Double = 90.0,
    val light: Face3DLightAttributes = Face3DLightAttributes(),
)

internal fun interface Face3DFillColorEvaluator {
    fun evaluate(face: Face3D): String
}

internal object Face3DColor {
    fun hsvToHex(
        hue: Double,
        saturation: Double,
        value: Double,
    ): String {
        if (
            !hue.isFinite() ||
            !saturation.isFinite() ||
            !value.isFinite()
        ) {
            return "#000000"
        }
        val lightness = value * (1.0 - saturation / 2.0)
        val hslSaturation =
            if (lightness == 0.0 || lightness == 1.0) {
                0.0
            } else {
                (value - lightness) /
                    minOf(lightness, 1.0 - lightness)
            }
        return hslToHex(
            hue = hue,
            saturation = hslSaturation * 100.0,
            lightness = lightness * 100.0,
        )
    }

    fun hslToHex(
        hue: Double,
        saturation: Double,
        lightness: Double,
    ): String {
        if (
            !hue.isFinite() ||
            !saturation.isFinite() ||
            !lightness.isFinite()
        ) {
            return "#000000"
        }
        val normalizedHue = ((hue % 360.0) + 360.0) % 360.0 / 360.0
        val normalizedSaturation =
            (saturation / 100.0).coerceIn(0.0, 1.0)
        val normalizedLightness =
            (lightness / 100.0).coerceIn(0.0, 1.0)
        val chroma =
            (1.0 - abs(2.0 * normalizedLightness - 1.0)) *
                normalizedSaturation
        val hueSection = normalizedHue * 6.0
        val secondary =
            chroma * (1.0 - abs(hueSection % 2.0 - 1.0))
        val (red, green, blue) = when {
            hueSection < 1.0 -> Triple(chroma, secondary, 0.0)
            hueSection < 2.0 -> Triple(secondary, chroma, 0.0)
            hueSection < 3.0 -> Triple(0.0, chroma, secondary)
            hueSection < 4.0 -> Triple(0.0, secondary, chroma)
            hueSection < 5.0 -> Triple(secondary, 0.0, chroma)
            else -> Triple(chroma, 0.0, secondary)
        }
        val match = normalizedLightness - chroma / 2.0
        return "#%02x%02x%02x".formatHex(
            (255.0 * (red + match)).roundToInt().coerceIn(0, 255),
            (255.0 * (green + match)).roundToInt().coerceIn(0, 255),
            (255.0 * (blue + match)).roundToInt().coerceIn(0, 255),
        )
    }

    private fun String.formatHex(
        red: Int,
        green: Int,
        blue: Int,
    ): String =
        replaceFirst("%02x", red.toString(16).padStart(2, '0'))
            .replaceFirst("%02x", green.toString(16).padStart(2, '0'))
            .replaceFirst("%02x", blue.toString(16).padStart(2, '0'))
}

internal data class Face3DAttributes(
    val id: String = "",
    val name: String? = "",
    val needsRegularUpdate: Boolean = true,
    val visible: Boolean = true,
    val strokeColor: String = "#0072b2",
    val fillColor: String = "yellow",
    val strokeWidth: Double = 1.0,
    val strokeOpacity: Double = 1.0,
    val fillOpacity: Double = 0.4,
    val layer: Int = 12,
    val fixed: Boolean = false,
    val highlight: Boolean = false,
    val withLabel: Boolean = false,
    val dash: Int = 0,
    val dashScale: Boolean = false,
    val lineCap: String = "round",
    val shader: Face3DShaderAttributes = Face3DShaderAttributes(),
    val fillColorEvaluator: Face3DFillColorEvaluator? = null,
)

internal data class Polyhedron3DFaceInput(
    val vertexKeys: List<String>,
    val attributes: Face3DAttributes = Face3DAttributes(),
)

internal sealed interface Face3DError {
    data class VertexEvaluation(
        val key: String,
        val coordinateIndex: Int?,
        val error: Line3DDynamicError,
    ) : Face3DError

    data class InvalidCoordinateCount(
        val key: String,
        val count: Int,
    ) : Face3DError

    data class MissingVertex(
        val faceNumber: Int,
        val key: String,
    ) : Face3DError

    data class TransformationEvaluation(
        val transformationIndex: Int,
        val error: TransformationError,
    ) : Face3DError

    data class InvalidBaseElement(
        val id: String?,
    ) : Face3DError

    data class Registration(
        val error: BoardError,
    ) : Face3DError

    data class ProxyCurveFactory(
        val error: CurveError,
    ) : Face3DError
}

internal sealed interface Polyhedron3DError {
    data class VertexLimitExceeded(
        val count: Int,
        val maximum: Int,
    ) : Polyhedron3DError

    data class FaceLimitExceeded(
        val count: Int,
        val maximum: Int,
    ) : Polyhedron3DError

    data class FaceVertexLimitExceeded(
        val faceNumber: Int,
        val count: Int,
        val maximum: Int,
    ) : Polyhedron3DError

    data class ParentViewMismatch(
        val vertexKey: String? = null,
    ) : Polyhedron3DError

    data class ParentNotRegistered(
        val vertexKey: String,
        val id: String,
    ) : Polyhedron3DError

    data class FaceFactory(
        val faceNumber: Int,
        val error: Face3DError,
    ) : Polyhedron3DError

    data class Registration(
        val error: BoardError,
    ) : Polyhedron3DError

    data object EmptyFaces : Polyhedron3DError

    data class InvalidTransformationCount(
        val count: Int,
    ) : Polyhedron3DError
}

/**
 * Shared defining data used by every Face3D of one Polyhedron3D.
 */
internal class Polyhedron3DDefinition(
    internal val view: View3D,
    internal val vertices: LinkedHashMap<String, Polyhedron3DVertexSource>,
    internal val faceKeys: List<List<String>>,
) {
    internal val coords = linkedMapOf<String, DoubleArray>()
    internal val coords2D = linkedMapOf<String, DoubleArray>()
    internal val vertexZIndices = linkedMapOf<String, Double>()
}

/**
 * A JSXGraph Face3D and its ordinary Curve proxy.
 */
internal class Face3D internal constructor(
    view: View3D,
    internal val polyhedron: Polyhedron3DDefinition,
    internal val faceNumber: Int,
    internal val faceAttributes: Face3DAttributes,
) : GeometryElement3D(
    view = view,
    id = faceAttributes.id,
    name = faceAttributes.name,
    type = Const.OBJECT_TYPE_FACE3D,
    needsRegularUpdate = faceAttributes.needsRegularUpdate,
) {
    internal var normal = DoubleArray(4)
        private set
    internal var d: Double = 0.0
        private set
    internal var vec1 = DoubleArray(4)
        private set
    internal var vec2 = DoubleArray(4)
        private set
    internal lateinit var curve2D: Curve
        private set
    internal var evaluationError: Face3DError? = null
        private set

    init {
        elType = FACE_3D_ELEMENT_TYPE
    }

    // JSXGraph: src/3d/face3d.js -> updateCoords.
    internal fun updateCoordsResult(): GMResult<Face3D, Face3DError> {
        for ((key, source) in polyhedron.vertices) {
            val coordinates = when (
                val result = evaluateVertex(key, source)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            polyhedron.coords[key] = coordinates
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/face3d.js -> updateDataArray2D.
    internal fun updateDataArray2D():
        GMResult<CurveDataUpdate, Face3DError> {
        val face = polyhedron.faceKeys[faceNumber]
        if (faceNumber == 0) {
            for (key in polyhedron.vertices.keys) {
                polyhedron.coords2D[key] = DoubleArray(0)
            }
        }

        val x = mutableListOf<Double>()
        val y = mutableListOf<Double>()
        zIndex = 0.0
        for (key in face) {
            val coordinates3D = polyhedron.coords[key]
                ?: return GMResult.Err(
                    Face3DError.MissingVertex(faceNumber, key),
                )
            var coordinates2D = polyhedron.coords2D[key]
            if (coordinates2D == null || coordinates2D.isEmpty()) {
                coordinates2D = view.project3DTo2D(coordinates3D)
                polyhedron.coords2D[key] = coordinates2D
                polyhedron.vertexZIndices[key] = Mat.innerProduct(
                    view.matrix3DRotShift[3],
                    coordinates3D,
                    4,
                )
            }
            x += coordinates2D[1]
            y += coordinates2D[2]
            zIndex += polyhedron.vertexZIndices[key] ?: 0.0
        }
        if (face.isNotEmpty()) {
            zIndex /= face.size
        }
        if (face.size != 2 && face.isNotEmpty()) {
            x += x[0]
            y += y[0]
        }
        return GMResult.Ok(
            CurveDataUpdate(
                x = x.toDoubleArray(),
                y = y.toDoubleArray(),
            ),
        )
    }

    // JSXGraph: src/3d/face3d.js -> addTransform.
    internal fun addTransform(
        element: Face3D,
        newTransformations: Iterable<Transformation>,
    ): Face3D {
        if (faceNumber == 0) {
            addTransformGeneric(element, newTransformations)
        }
        return this
    }

    // JSXGraph: src/3d/face3d.js -> removeTransform.
    internal fun removeTransform(
        removedTransformations: Iterable<Transformation>,
    ): Face3D {
        if (faceNumber == 0) {
            removeTransformGeneric(removedTransformations)
        }
        return this
    }

    // JSXGraph: src/3d/face3d.js -> clearTransforms.
    internal fun clearTransforms(): Face3D {
        if (faceNumber == 0) {
            clearTransformsGeneric()
        }
        return this
    }

    // JSXGraph: src/3d/face3d.js -> updateTransform.
    private fun updateTransformResult(): GMResult<Face3D, Face3DError> {
        if (
            faceNumber != 0 ||
            transformations.isEmpty() ||
            baseElement == null
        ) {
            return GMResult.Ok(this)
        }
        val baseFace = baseElement as? Face3D
            ?: return GMResult.Err(
                Face3DError.InvalidBaseElement(baseElement?.id),
            )
        for ((index, transformation) in transformations.withIndex()) {
            when (val result = transformation.updateResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Face3DError.TransformationEvaluation(
                        transformationIndex = index,
                        error = result.error,
                    ),
                )
            }
        }
        val baseCoordinates =
            if (this === baseFace) polyhedron.coords else baseFace.polyhedron.coords
        for ((key, sourceCoordinates) in baseCoordinates) {
            var coordinates = sourceCoordinates.copyOf()
            for (transformation in transformations) {
                coordinates = Mat.matVecMult(
                    transformation.matrix,
                    coordinates,
                )
            }
            polyhedron.coords[key] = coordinates
        }
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/face3d.js -> update.
    override fun update(fromParent: Boolean): Face3D {
        if (!needsUpdate) {
            return this
        }
        if (faceNumber == 0) {
            when (val result = updateCoordsResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    evaluationError = result.error
                    return this
                }
            }
            when (val result = updateTransformResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    evaluationError = result.error
                    return this
                }
            }
        }
        val face = polyhedron.faceKeys[faceNumber]
        if (face.size < 3) {
            evaluationError = null
            return this
        }
        val point1 = polyhedron.coords[face[0]]
        val point2 = polyhedron.coords[face[1]]
        val point3 = polyhedron.coords[face[2]]
        if (point1 == null || point2 == null || point3 == null) {
            evaluationError = Face3DError.MissingVertex(
                faceNumber = faceNumber,
                key = face.firstOrNull {
                    it !in polyhedron.coords
                } ?: face[0],
            )
            return this
        }
        vec1 = DoubleArray(4) { index -> point2[index] - point1[index] }
        vec2 = DoubleArray(4) { index -> point3[index] - point1[index] }
        val cross = Mat.crossProduct(
            vec1.copyOfRange(1, 4),
            vec2.copyOfRange(1, 4),
        )
        val length = Mat.norm(cross)
        normal = doubleArrayOf(0.0, cross[0], cross[1], cross[2])
        if (abs(length) > NORMAL_EPSILON) {
            for (index in 1 until 4) {
                normal[index] /= length
            }
        }
        d = Mat.innerProduct(point1, normal, 4)
        evaluationError = null
        return this
    }

    override fun updateRenderer(): Face3D {
        needsUpdate = false
        return this
    }

    /**
     * Resolves the color mutation performed by Face3D.shader into a CSS hex
     * color so the portable scene does not depend on a browser CSS parser.
     */
    internal fun resolvedFillColor(): String =
        shadedFillColor()
            ?: faceAttributes.fillColorEvaluator?.evaluate(this)
            ?: faceAttributes.fillColor

    internal fun shadedFillColor(): String? {
        val shader = faceAttributes.shader
        if (!shader.enabled) {
            return null
        }
        val lightness =
            if (shader.type.lowercase() == "angle") {
                val light = shader.light
                val sun = when (light.type) {
                    2 -> rotationFromAngles(
                        azimuth = light.azimuth * PI / 180.0,
                        elevation = light.elevation * PI / 180.0,
                        bank = light.bank * PI / 180.0,
                    )[3]
                    3 -> rotationFromAngles(
                        azimuth =
                            view.angles.az +
                                light.azimuth * PI / 180.0,
                        elevation =
                            view.angles.el +
                                light.elevation * PI / 180.0,
                        bank = view.angles.bank,
                    )[3]
                    else -> view.matrix3DRotShift[3]
                }
                val rawAngle = Mat.innerProduct(sun, normal, 4)
                val directedAngle = when {
                    light.direction == 0 -> abs(rawAngle)
                    light.direction < 0 -> -rawAngle
                    else -> rawAngle
                }
                shader.minimumLightness +
                    (
                        shader.maximumLightness -
                            shader.minimumLightness
                        ) * directedAngle
            } else {
                val visibleFaces = view.objects.values
                    .filterIsInstance<Face3D>()
                    .filter { it.faceAttributes.visible }
                val minimum = visibleFaces.minOfOrNull { it.zIndex } ?: zIndex
                val maximum = visibleFaces.maxOfOrNull { it.zIndex } ?: zIndex
                val ratio =
                    if (maximum == minimum) {
                        0.0
                    } else {
                        (zIndex - minimum) / (maximum - minimum)
                    }
                shader.minimumLightness +
                    (
                        shader.maximumLightness -
                            shader.minimumLightness
                        ) * ratio
            }
        return Face3DColor.hslToHex(
            hue = shader.hue,
            saturation = shader.saturation,
            lightness = lightness,
        )
    }

    private fun evaluateVertex(
        key: String,
        source: Polyhedron3DVertexSource,
    ): GMResult<DoubleArray, Face3DError> {
        val evaluated = when (source) {
            is Polyhedron3DVertexSource.Point ->
                return GMResult.Ok(source.point.coords)
            is Polyhedron3DVertexSource.Function ->
                when (val result = source.evaluator.evaluate()) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        Face3DError.VertexEvaluation(
                            key = key,
                            coordinateIndex = null,
                            error = result.error,
                        ),
                    )
                }
            is Polyhedron3DVertexSource.Values -> {
                val values = DoubleArray(source.values.size)
                for ((index, value) in source.values.withIndex()) {
                    values[index] = when (value) {
                        is Line3DCoordinateValue.Numeric -> value.value
                        is Line3DCoordinateValue.Dynamic ->
                            when (val result = value.evaluator.evaluate()) {
                                is GMResult.Ok -> result.value
                                is GMResult.Err -> return GMResult.Err(
                                    Face3DError.VertexEvaluation(
                                        key = key,
                                        coordinateIndex = index,
                                        error = result.error,
                                    ),
                                )
                            }
                    }
                }
                values
            }
        }
        return when (evaluated.size) {
            3 -> GMResult.Ok(
                doubleArrayOf(
                    1.0,
                    evaluated[0],
                    evaluated[1],
                    evaluated[2],
                ),
            )
            4 -> GMResult.Ok(evaluated.copyOf())
            else -> GMResult.Err(
                Face3DError.InvalidCoordinateCount(
                    key = key,
                    count = evaluated.size,
                ),
            )
        }
    }

    private fun rotationFromAngles(
        azimuth: Double,
        elevation: Double,
        bank: Double,
    ): Array<DoubleArray> {
        val matrix = Mat.identity(4)
        val factor = -sin(elevation)
        matrix[1][1] = -cos(azimuth)
        matrix[1][2] = sin(azimuth)
        matrix[1][3] = 0.0
        matrix[2][1] = factor * sin(azimuth)
        matrix[2][2] = factor * cos(azimuth)
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

    internal companion object {
        private const val FACE_3D_ID_PREFIX = "face3d"
        private const val FACE_3D_ELEMENT_TYPE = "face3d"
        private const val NORMAL_EPSILON = 1.0e-12

        // JSXGraph: src/3d/face3d.js -> createFace3D.
        internal fun create(
            definition: Polyhedron3DDefinition,
            faceNumber: Int,
            attributes: Face3DAttributes,
        ): GMResult<Face3D, Face3DError> {
            val face = Face3D(
                view = definition.view,
                polyhedron = definition,
                faceNumber = faceNumber,
                faceAttributes = attributes,
            )
            face.update()
            face.evaluationError?.let {
                return GMResult.Err(it)
            }
            when (
                val registration = definition.view.board.setId(
                    face,
                    FACE_3D_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Face3DError.Registration(registration.error),
                )
            }
            face.registerInView()
            val data = when (val result = face.updateDataArray2D()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    definition.view.board.removeObject(face)
                    return result
                }
            }
            val curve = when (
                val result = Curve.createData(
                    board = definition.view.board,
                    dataX = data.x,
                    dataY = data.y,
                    name = "",
                    needsRegularUpdate = attributes.needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    definition.view.board.removeObject(face)
                    return GMResult.Err(
                        Face3DError.ProxyCurveFactory(result.error),
                    )
                }
            }
            curve.dump = false
            curve.isDraggable = false
            curve.setParents(listOf(face))
            curve.setDataUpdater(
                CurveDataUpdater {
                    when (val result = face.updateDataArray2D()) {
                        is GMResult.Ok -> result
                        is GMResult.Err -> GMResult.Err(
                            CurveDataUpdateError.Face3D(result.error),
                        )
                    }
                },
            )
            face.curve2D = curve
            face.element2D = curve
            face.addChild(curve)
            return GMResult.Ok(face)
        }
    }
}

/**
 * A JSXGraph Polyhedron3D that forwards lifecycle and transformations to its
 * Face3D elements.
 */
internal class Polyhedron3D private constructor(
    view: View3D,
    internal val definition: Polyhedron3DDefinition,
    internal val faces: MutableList<Face3D>,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
) : GeometryElement3D(
    view = view,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_POLYHEDRON3D,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal val numberFaces: Int
        get() = faces.size
    internal val inherits = mutableListOf<GeometryElement>()

    init {
        elType = POLYHEDRON_3D_ELEMENT_TYPE
    }

    override fun prepareUpdate(): Polyhedron3D {
        for (face in faces) {
            face.prepareUpdate()
        }
        needsUpdate = true
        return this
    }

    override fun update(fromParent: Boolean): Polyhedron3D {
        if (needsUpdate) {
            for (face in faces) {
                face.update(fromParent)
            }
        }
        return this
    }

    override fun updateRenderer(): Polyhedron3D {
        if (needsUpdate) {
            for (face in faces) {
                face.updateRenderer()
            }
            needsUpdate = false
        }
        return this
    }

    // JSXGraph: src/3d/polyhedron3d.js -> addTransform.
    internal fun addTransform(
        element: Polyhedron3D,
        transformations: Iterable<Transformation>,
    ): GMResult<Polyhedron3D, Polyhedron3DError> {
        val targetFace = faces.firstOrNull()
            ?: return GMResult.Err(Polyhedron3DError.EmptyFaces)
        val sourceFace = element.faces.firstOrNull()
            ?: return GMResult.Err(Polyhedron3DError.EmptyFaces)
        targetFace.addTransform(sourceFace, transformations)
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/polyhedron3d.js -> removeTransform.
    internal fun removeTransform(
        transformations: Iterable<Transformation>,
    ): GMResult<Polyhedron3D, Polyhedron3DError> {
        val firstFace = faces.firstOrNull()
            ?: return GMResult.Err(Polyhedron3DError.EmptyFaces)
        firstFace.removeTransform(transformations)
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/polyhedron3d.js -> clearTransforms.
    internal fun clearTransforms():
        GMResult<Polyhedron3D, Polyhedron3DError> {
        val firstFace = faces.firstOrNull()
            ?: return GMResult.Err(Polyhedron3DError.EmptyFaces)
        firstFace.clearTransforms()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/3d/polyhedron3d.js -> toSTL.
    internal fun toSTL(
        solidName: String = name,
    ): GMResult<String, Polyhedron3DError> {
        val firstFace = faces.firstOrNull()
        if (firstFace != null) {
            when (val result = firstFace.updateCoordsResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Polyhedron3DError.FaceFactory(
                        faceNumber = 0,
                        error = result.error,
                    ),
                )
            }
        }
        return GMResult.Ok(
            buildString {
                append("solid ")
                append(solidName)
                append('\n')
                for (face in definition.faceKeys) {
                    append(" facet normal 0 0 0\n  outer loop\n")
                    for (key in face) {
                        val coordinates = definition.coords[key] ?: continue
                        append("   vertex ")
                        append(coordinates[1])
                        append(' ')
                        append(coordinates[2])
                        append(' ')
                        append(coordinates[3])
                        append('\n')
                    }
                    append("  endloop\n endfacet\n")
                }
                append("endsolid ")
                append(solidName)
                append('\n')
            },
        )
    }

    override fun remove(): GeometryElement {
        board.removeObjects(faces.asReversed())
        faces.clear()
        inherits.clear()
        return super.remove()
    }

    internal companion object {
        internal const val MAX_VERTEX_COUNT: Int = 10_000
        internal const val MAX_FACE_COUNT: Int = 10_000
        internal const val MAX_FACE_VERTEX_COUNT: Int = 10_000
        private const val POLYHEDRON_3D_ID_PREFIX = "polyhedron3d"
        private const val POLYHEDRON_3D_ELEMENT_TYPE = "polyhedron3d"

        // JSXGraph: src/3d/polyhedron3d.js -> createPolyhedron3D.
        internal fun create(
            view: View3D,
            vertices: LinkedHashMap<String, Polyhedron3DVertexSource>,
            faceInputs: List<Polyhedron3DFaceInput>,
            dependencies: Iterable<GeometryElement> = emptyList(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Polyhedron3D, Polyhedron3DError> {
            validateDefinition(
                view = view,
                vertices = vertices,
                faceInputs = faceInputs,
            )?.let { return GMResult.Err(it) }
            if (id.isNotEmpty() && view.board.elementById(id) != null) {
                return GMResult.Err(
                    Polyhedron3DError.Registration(
                        BoardError.DuplicateElementId(id),
                    ),
                )
            }
            val definition = Polyhedron3DDefinition(
                view = view,
                vertices = vertices,
                faceKeys = faceInputs.map { it.vertexKeys.toList() },
            )
            val evaluator = Face3D(
                view = view,
                polyhedron = definition,
                faceNumber = 0,
                faceAttributes = Face3DAttributes(),
            )
            when (val result = evaluator.updateCoordsResult()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    Polyhedron3DError.FaceFactory(
                        faceNumber = 0,
                        error = result.error,
                    ),
                )
            }
            val faces = mutableListOf<Face3D>()
            for ((faceNumber, input) in faceInputs.withIndex()) {
                val face = when (
                    val result = Face3D.create(
                        definition = definition,
                        faceNumber = faceNumber,
                        attributes = input.attributes,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        view.board.removeObjects(faces.asReversed())
                        return GMResult.Err(
                            Polyhedron3DError.FaceFactory(
                                faceNumber = faceNumber,
                                error = result.error,
                            ),
                        )
                    }
                }
                faces += face
            }
            val polyhedron = Polyhedron3D(
                view = view,
                definition = definition,
                faces = faces,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            )
            when (
                val registration = view.board.setId(
                    polyhedron,
                    POLYHEDRON_3D_ID_PREFIX,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    view.board.removeObjects(faces.asReversed())
                    return GMResult.Err(
                        Polyhedron3DError.Registration(registration.error),
                    )
                }
            }
            polyhedron.registerInView()
            for (face in faces) {
                face.setParents(listOf(polyhedron))
                polyhedron.inherits += face
            }
            for (dependency in dependencies.distinctBy { it.id }) {
                dependency.addChild(polyhedron)
            }
            polyhedron.prepareUpdate().update()
            return GMResult.Ok(polyhedron)
        }

        // JSXGraph: src/3d/polyhedron3d.js -> createPolyhedron3D,
        // transformed parent form.
        internal fun create(
            view: View3D,
            base: Polyhedron3D,
            transformations: List<Transformation>,
            faceAttributes: List<Face3DAttributes>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
        ): GMResult<Polyhedron3D, Polyhedron3DError> {
            if (base.view !== view) {
                return GMResult.Err(
                    Polyhedron3DError.ParentViewMismatch(),
                )
            }
            if (transformations.isEmpty()) {
                return GMResult.Err(
                    Polyhedron3DError.InvalidTransformationCount(0),
                )
            }
            val inputs = base.definition.faceKeys.mapIndexed {
                    index,
                    keys,
                ->
                Polyhedron3DFaceInput(
                    vertexKeys = keys,
                    attributes =
                        faceAttributes.getOrNull(index)
                            ?: Face3DAttributes(
                                needsRegularUpdate = needsRegularUpdate,
                            ),
                )
            }
            val created = when (
                val result = create(
                    view = view,
                    vertices = base.definition.vertices,
                    faceInputs = inputs,
                    id = id,
                    name = name,
                    needsRegularUpdate = needsRegularUpdate,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (
                val result = created.addTransform(
                    element = base,
                    transformations = transformations,
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    view.board.removeObject(created)
                    return result
                }
            }
            base.addChild(created)
            created.addParents(listOf(base))
            created.prepareUpdate().update()
            created.faces.firstOrNull()?.evaluationError?.let { error ->
                view.board.removeObject(created)
                return GMResult.Err(
                    Polyhedron3DError.FaceFactory(
                        faceNumber = 0,
                        error = error,
                    ),
                )
            }
            return GMResult.Ok(created)
        }

        private fun validateDefinition(
            view: View3D,
            vertices: Map<String, Polyhedron3DVertexSource>,
            faceInputs: List<Polyhedron3DFaceInput>,
        ): Polyhedron3DError? {
            if (vertices.size > MAX_VERTEX_COUNT) {
                return Polyhedron3DError.VertexLimitExceeded(
                    count = vertices.size,
                    maximum = MAX_VERTEX_COUNT,
                )
            }
            if (faceInputs.size > MAX_FACE_COUNT) {
                return Polyhedron3DError.FaceLimitExceeded(
                    count = faceInputs.size,
                    maximum = MAX_FACE_COUNT,
                )
            }
            for ((key, source) in vertices) {
                if (source is Polyhedron3DVertexSource.Point) {
                    if (source.point.view !== view) {
                        return Polyhedron3DError.ParentViewMismatch(key)
                    }
                    if (
                        view.board.elementById(source.point.id) !==
                        source.point
                    ) {
                        return Polyhedron3DError.ParentNotRegistered(
                            vertexKey = key,
                            id = source.point.id,
                        )
                    }
                }
                val staticCount = (
                    source as? Polyhedron3DVertexSource.Values
                    )?.values?.size
                if (staticCount != null && staticCount !in setOf(3, 4)) {
                    return Polyhedron3DError.FaceFactory(
                        faceNumber = 0,
                        error = Face3DError.InvalidCoordinateCount(
                            key = key,
                            count = staticCount,
                        ),
                    )
                }
            }
            for ((faceNumber, face) in faceInputs.withIndex()) {
                if (face.vertexKeys.size > MAX_FACE_VERTEX_COUNT) {
                    return Polyhedron3DError.FaceVertexLimitExceeded(
                        faceNumber = faceNumber,
                        count = face.vertexKeys.size,
                        maximum = MAX_FACE_VERTEX_COUNT,
                    )
                }
                face.vertexKeys.firstOrNull {
                    it !in vertices
                }?.let { key ->
                    return Polyhedron3DError.FaceFactory(
                        faceNumber = faceNumber,
                        error = Face3DError.MissingVertex(
                            faceNumber = faceNumber,
                            key = key,
                        ),
                    )
                }
            }
            return null
        }
    }
}
