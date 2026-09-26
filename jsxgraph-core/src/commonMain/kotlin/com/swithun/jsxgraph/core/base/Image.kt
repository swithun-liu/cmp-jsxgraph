/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/image.js -> Image, update, updateSize, updateSpan,
 * addTransform, setSize, createImage;
 * src/base/element.js -> addRotation;
 * src/base/coordselement.js -> CoordsElement.create
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.parser.JessieCodeCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeExpressionCompileError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeError
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.math.PI
import kotlin.math.abs

internal sealed interface ImageError {
    data class InvalidCoordinateCount(
        val count: Int,
    ) : ImageError

    data class InvalidSizeCount(
        val count: Int,
    ) : ImageError

    data class CoordinateEvaluation(
        val error: CoordinateConstraintError,
    ) : ImageError

    data class CoordinateExpressionCompile(
        val coordinateIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : ImageError

    data class UrlEvaluation(
        val error: JessieCodeRuntimeError,
    ) : ImageError

    data class UrlResult(
        val actualType: String,
    ) : ImageError

    data class SizeEvaluation(
        val sizeIndex: Int,
        val error: JessieCodeRuntimeError,
    ) : ImageError

    data class SizeExpressionCompile(
        val sizeIndex: Int,
        val error: JessieCodeExpressionCompileError,
    ) : ImageError

    data class SizeResult(
        val sizeIndex: Int,
        val actualType: String,
    ) : ImageError

    data class Rotation(
        val error: TransformationError,
    ) : ImageError

    data class Registration(
        val error: BoardError,
    ) : ImageError
}

/**
 * Translated JXG.Image state without platform image loading.
 *
 * The source string and user-space geometry live in core. Platform renderers
 * resolve the source into their native image representation.
 */
internal class Image private constructor(
    board: Board,
    coordinates: DoubleArray,
    private val urlTerm: JessieCodeCoordinateFunction,
    initialUrl: String,
    sizeTerms: List<JessieCodeCoordinateFunction>,
    initialSize: DoubleArray,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    coordinateFunctions: List<JessieCodeCoordinateFunction>,
) : CoordsElement(
    board = board,
    coordinates = coordinates,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_IMAGE,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
    coordinateFunctions = coordinateFunctions,
) {
    internal var url: String = initialUrl
        private set
    private var sizeTerms: List<JessieCodeCoordinateFunction> = sizeTerms

    internal var usrSize: DoubleArray = initialSize.copyOf()
        private set
    internal var size: DoubleArray = pixelSize(initialSize)
        private set
    internal var span: Array<DoubleArray> =
        untransformedSpan(initialSize[0], initialSize[1])
        private set
    internal var renderSpan: Array<DoubleArray> =
        untransformedSpan(abs(initialSize[0]), abs(initialSize[1]))
        private set

    internal var urlEvaluationError: ImageError? = null
        private set
    internal var sizeEvaluationError: ImageError? = null
        private set

    init {
        elType = IMAGE_ELEMENT_TYPE
    }

    internal fun W(): Double = usrSize[0]

    internal fun H(): Double = usrSize[1]

    // JSXGraph: src/base/image.js -> setSize.
    internal fun setSize(
        newSizeTerms: List<JessieCodeCoordinateFunction>,
    ): GMResult<Image, ImageError> {
        if (newSizeTerms.size != 2) {
            return GMResult.Err(
                ImageError.InvalidSizeCount(newSizeTerms.size),
            )
        }
        sizeTerms = newSizeTerms
        addParentsFromJCFunctions(newSizeTerms)
        prepareUpdate()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/base/element.js -> addRotation.
    internal fun addRotation(
        degrees: Double,
    ): GMResult<Image, ImageError> {
        if (degrees == 0.0) {
            return GMResult.Ok(this)
        }
        val dynamic = { value: () -> Double ->
            TransformationParameter.Dynamic(
                TransformationDynamicParameter {
                    GMResult.Ok(value())
                },
            )
        }
        val definitions = listOf(
            "translate" to listOf(dynamic { -X() }, dynamic { -Y() }),
            "scale" to listOf(
                dynamic { board.unitX / board.unitY },
                TransformationParameter.Numeric(1.0),
            ),
            "rotate" to listOf(
                TransformationParameter.Numeric(degrees * PI / 180.0),
            ),
            "scale" to listOf(
                dynamic { board.unitY / board.unitX },
                TransformationParameter.Numeric(1.0),
            ),
            "translate" to listOf(dynamic(::X), dynamic(::Y)),
        )
        val rotationTransforms = mutableListOf<Transformation>()
        for ((type, parameters) in definitions) {
            when (
                val result = Transformation.create(
                    board = board,
                    type = type,
                    parameters = parameters,
                )
            ) {
                is GMResult.Ok -> rotationTransforms += result.value
                is GMResult.Err -> return GMResult.Err(
                    ImageError.Rotation(result.error),
                )
            }
        }
        addTransform(this, rotationTransforms)
        prepareUpdate()
        return GMResult.Ok(this)
    }

    // JSXGraph: src/base/image.js -> update.
    override fun update(fromParent: Boolean): Image {
        if (!needsUpdate) {
            return this
        }
        updateCoords(fromParent)
        updateUrl()
        updateSize()
        updateSpan()
        return this
    }

    // JSXGraph: src/base/image.js -> updateRenderer.
    override fun updateRenderer(): Image {
        needsUpdate = false
        return this
    }

    private fun updateUrl() {
        when (val result = evaluateUrl(urlTerm)) {
            is GMResult.Ok -> {
                url = result.value
                urlEvaluationError = null
            }
            is GMResult.Err -> urlEvaluationError = result.error
        }
    }

    // JSXGraph: src/base/image.js -> updateSize.
    private fun updateSize() {
        when (val result = evaluateSize(sizeTerms)) {
            is GMResult.Ok -> {
                usrSize = result.value
                size = pixelSize(result.value)
                sizeEvaluationError = null
            }
            is GMResult.Err -> {
                usrSize = doubleArrayOf(Double.NaN, Double.NaN)
                size = doubleArrayOf(Double.NaN, Double.NaN)
                sizeEvaluationError = result.error
            }
        }
    }

    // JSXGraph: src/base/image.js -> updateSpan.
    private fun updateSpan() {
        if (transformations.isEmpty()) {
            span = untransformedSpan(W(), H())
            renderSpan = untransformedSpan(abs(W()), abs(H()))
            return
        }
        if (transformationEvaluationError != null) {
            span = invalidSpan()
            renderSpan = invalidSpan()
            return
        }
        span = transformedSpan(W(), H())
        renderSpan = transformedSpan(abs(W()), abs(H()))
    }

    private fun transformedSpan(
        width: Double,
        height: Double,
    ): Array<DoubleArray> {
        val vectors = arrayOf(
            doubleArrayOf(Z(), X(), Y()),
            doubleArrayOf(Z(), X() + width, Y()),
            doubleArrayOf(Z(), X(), Y() + height),
        )
        for (transformation in transformations) {
            for (index in vectors.indices) {
                vectors[index] = Mat.matVecMult(
                    transformation.matrix,
                    vectors[index],
                )
            }
        }
        for (vector in vectors) {
            vector[1] /= vector[0]
            vector[2] /= vector[0]
            vector[0] /= vector[0]
        }
        for (index in 1..2) {
            for (component in 0..2) {
                vectors[index][component] -= vectors[0][component]
            }
        }
        return vectors
    }

    private fun untransformedSpan(
        width: Double,
        height: Double,
    ): Array<DoubleArray> =
        arrayOf(
            doubleArrayOf(Z(), X(), Y()),
            doubleArrayOf(Z(), width, 0.0),
            doubleArrayOf(Z(), 0.0, height),
        )

    private fun invalidSpan(): Array<DoubleArray> =
        Array(3) { DoubleArray(3) { Double.NaN } }

    private fun pixelSize(userSize: DoubleArray): DoubleArray =
        doubleArrayOf(
            abs(userSize[0] * board.unitX),
            abs(userSize[1] * board.unitY),
        )

    internal companion object {
        private const val IMAGE_ID_PREFIX = "Im"
        private const val IMAGE_ELEMENT_TYPE = "image"

        internal fun create(
            board: Board,
            urlTerm: JessieCodeCoordinateFunction,
            coordinates: DoubleArray,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            rotationDegrees: Double = 0.0,
        ): GMResult<Image, ImageError> =
            createResolved(
                board = board,
                urlTerm = urlTerm,
                coordinates = coordinates,
                coordinateFunctions = emptyList(),
                sizeTerms = sizeTerms,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                rotationDegrees = rotationDegrees,
            )

        internal fun createConstrained(
            board: Board,
            urlTerm: JessieCodeCoordinateFunction,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            rotationDegrees: Double = 0.0,
        ): GMResult<Image, ImageError> {
            if (
                coordinateFunctions.size !in 1..3 ||
                (
                    coordinateFunctions.size == 1 &&
                        !coordinateFunctions[0].returnsCoordinateArray
                    )
            ) {
                return GMResult.Err(
                    ImageError.InvalidCoordinateCount(
                        coordinateFunctions.size,
                    ),
                )
            }
            val placeholder =
                if (coordinateFunctions.size <= 2) {
                    doubleArrayOf(0.0, 0.0)
                } else {
                    doubleArrayOf(1.0, 0.0, 0.0)
                }
            return createResolved(
                board = board,
                urlTerm = urlTerm,
                coordinates = placeholder,
                coordinateFunctions = coordinateFunctions,
                sizeTerms = sizeTerms,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                rotationDegrees = rotationDegrees,
            )
        }

        private fun createResolved(
            board: Board,
            urlTerm: JessieCodeCoordinateFunction,
            coordinates: DoubleArray,
            coordinateFunctions: List<JessieCodeCoordinateFunction>,
            sizeTerms: List<JessieCodeCoordinateFunction>,
            id: String,
            name: String?,
            needsRegularUpdate: Boolean,
            rotationDegrees: Double,
        ): GMResult<Image, ImageError> {
            if (coordinates.size !in 2..3) {
                return GMResult.Err(
                    ImageError.InvalidCoordinateCount(coordinates.size),
                )
            }
            if (sizeTerms.size != 2) {
                return GMResult.Err(
                    ImageError.InvalidSizeCount(sizeTerms.size),
                )
            }
            val initialUrl = when (val result = evaluateUrl(urlTerm)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val initialSize = when (val result = evaluateSize(sizeTerms)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val image = Image(
                board = board,
                coordinates = coordinates,
                urlTerm = urlTerm,
                initialUrl = initialUrl,
                sizeTerms = sizeTerms,
                initialSize = initialSize,
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                coordinateFunctions = coordinateFunctions,
            )
            if (coordinateFunctions.isEmpty()) {
                image.baseElement = image
            } else {
                val initialCoordinates = when (
                    val result = image.coordinateConstraintResult()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return GMResult.Err(
                        ImageError.CoordinateEvaluation(result.error),
                    )
                }
                if (initialCoordinates.size !in 2..3) {
                    return GMResult.Err(
                        ImageError.InvalidCoordinateCount(
                            initialCoordinates.size,
                        ),
                    )
                }
                image.applyCoordinateConstraint(initialCoordinates)
            }
            when (val registration = board.setId(image, IMAGE_ID_PREFIX)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return GMResult.Err(
                    ImageError.Registration(registration.error),
                )
            }
            image.addParentsFromJCFunctions(
                coordinateFunctions + sizeTerms + urlTerm,
            )
            when (val rotation = image.addRotation(rotationDegrees)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> {
                    board.removeObject(image)
                    return rotation
                }
            }
            image.update()
            return GMResult.Ok(image)
        }

        private fun evaluateUrl(
            term: JessieCodeCoordinateFunction,
        ): GMResult<String, ImageError> =
            when (val result = term.evaluate()) {
                is GMResult.Err -> GMResult.Err(
                    ImageError.UrlEvaluation(result.error),
                )
                is GMResult.Ok -> {
                    val value = result.value
                    if (value is JessieCodeRuntimeValue.StringValue) {
                        GMResult.Ok(value.value)
                    } else {
                        GMResult.Err(
                            ImageError.UrlResult(runtimeType(value)),
                        )
                    }
                }
            }

        private fun evaluateSize(
            terms: List<JessieCodeCoordinateFunction>,
        ): GMResult<DoubleArray, ImageError> {
            if (terms.size != 2) {
                return GMResult.Err(
                    ImageError.InvalidSizeCount(terms.size),
                )
            }
            val values = DoubleArray(2)
            for ((index, term) in terms.withIndex()) {
                when (val result = term.evaluate()) {
                    is GMResult.Err -> return GMResult.Err(
                        ImageError.SizeEvaluation(index, result.error),
                    )
                    is GMResult.Ok -> {
                        val value = result.value
                        if (value !is JessieCodeRuntimeValue.NumberValue) {
                            return GMResult.Err(
                                ImageError.SizeResult(
                                    sizeIndex = index,
                                    actualType = runtimeType(value),
                                ),
                            )
                        }
                        values[index] = value.value
                    }
                }
            }
            return GMResult.Ok(values)
        }

        private fun runtimeType(
            value: JessieCodeRuntimeValue,
        ): String =
            when (value) {
                JessieCodeRuntimeValue.UndefinedValue -> "undefined"
                JessieCodeRuntimeValue.NullValue -> "null"
                is JessieCodeRuntimeValue.NumberValue -> "number"
                is JessieCodeRuntimeValue.BooleanValue -> "boolean"
                is JessieCodeRuntimeValue.StringValue -> "string"
                is JessieCodeRuntimeValue.ArrayValue -> "array"
                is JessieCodeRuntimeValue.ObjectValue -> "object"
                is JessieCodeRuntimeValue.FunctionValue -> "function"
                is JessieCodeRuntimeValue.BoardReference -> "board"
                is JessieCodeRuntimeValue.TransformationReference ->
                    "transformation"
                is JessieCodeRuntimeValue.CompositionReference ->
                    "composition"
                is JessieCodeRuntimeValue.ElementReference -> "element"
            }
    }
}
