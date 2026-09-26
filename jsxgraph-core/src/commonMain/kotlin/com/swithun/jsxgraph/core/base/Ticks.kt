/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/ticks.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.math.Mat
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.round

internal sealed interface TicksError {
    data class UnsupportedParent(val elementType: String) : TicksError

    data object FunctionArgumentsNoLongerSupported : TicksError

    data class InvalidFixedTick(
        val index: Int,
        val value: Double,
    ) : TicksError

    data class InvalidAttribute(
        val attribute: String,
        val value: String,
    ) : TicksError

    data class TickCountLimitExceeded(
        val limit: Int,
        val requestedSize: Long,
    ) : TicksError

    data class Registration(val error: BoardError) : TicksError
}

internal sealed interface TicksSource {
    data object Equidistant : TicksSource

    data class Fixed(
        val values: DoubleArray,
    ) : TicksSource
}

internal sealed interface TicksAnchor {
    data object Left : TicksAnchor

    data object Right : TicksAnchor

    data object Middle : TicksAnchor

    data class Fraction(
        val value: Double,
    ) : TicksAnchor
}

internal data class TicksAttributes(
    val anchor: TicksAnchor = TicksAnchor.Left,
    val drawZero: Boolean = false,
    val insertTicks: Boolean = false,
    val minTicksDistance: Double = 10.0,
    val minorHeight: Double = 4.0,
    val majorHeight: Double = 10.0,
    val tickEndings: DoubleArray = doubleArrayOf(1.0, 1.0),
    val majorTickEndings: DoubleArray = doubleArrayOf(1.0, 1.0),
    val ignoreInfiniteTickEndings: Boolean = true,
    val minorTicks: Int = 4,
    val ticksPerLabel: Int? = null,
    val scale: Double = 1.0,
    val scaleSymbol: String = "",
    val labels: List<String?> = emptyList(),
    val maxLabelLength: Int = 5,
    val precision: Int = 3,
    val digits: Int = 3,
    val beautifulScientificTickLabels: Boolean = false,
    val useUnicodeMinus: Boolean = true,
    val face: String = "|",
    val includeBoundaries: Boolean = false,
    val ticksType: String = "linear",
    val ticksDistance: Double = 1.0,
    val drawLabels: Boolean = false,
    val clip: Boolean = true,
    val labelOffset: DoubleArray = doubleArrayOf(10.0, 0.0),
    val labelFontSize: Double = 12.0,
    val labelAnchorX: String = "left",
    val labelAnchorY: String = "middle",
)

internal data class TicksCurveLocation(
    val baseX: Double,
    val baseY: Double,
    val normalX: Double,
    val normalY: Double,
    val major: Boolean,
    val label: String?,
)

/**
 * JXG.Ticks state and lifecycle. Pixel-sized paths are resolved by the scene
 * model because Compose determines the final viewport size.
 */
internal class Ticks private constructor(
    board: Board,
    internal val parent: GeometryElement,
    internal val source: TicksSource,
    internal val attributes: TicksAttributes,
    id: String,
    name: String?,
    needsRegularUpdate: Boolean,
    private val maximumTickCount: Int,
) : GeometryElement(
    board = board,
    id = id,
    name = name,
    type = Const.OBJECT_TYPE_TICKS,
    elementClass = Const.OBJECT_CLASS_OTHER,
    needsRegularUpdate = needsRegularUpdate,
) {
    internal var curveLocations: List<TicksCurveLocation> = emptyList()
        private set
    internal var evaluationError: TicksError? = null
        private set

    init {
        elType = TICKS_ELEMENT_TYPE
        isDraggable = true
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> update /
    // calculateTicksCoordinates.
    override fun update(fromParent: Boolean): Ticks {
        if (!needsUpdate) {
            return this
        }
        val curve = parent as? Curve
        if (curve == null) {
            curveLocations = emptyList()
            evaluationError = null
            return this
        }
        when (val result = generateCurveLocations(curve)) {
            is GMResult.Ok -> {
                curveLocations = result.value
                evaluationError = null
            }
            is GMResult.Err -> {
                curveLocations = emptyList()
                evaluationError = result.error
            }
        }
        return this
    }

    override fun remove(): Ticks {
        parent.ticks.remove(this)
        return this
    }

    // JSXGraph 1.13.3: src/base/ticks.js ->
    // generateEquidistantTicks / generateFixedTicks / processTickPosition.
    private fun generateCurveLocations(
        curve: Curve,
    ): GMResult<List<TicksCurveLocation>, TicksError> {
        if (attributes.insertTicks) {
            return GMResult.Err(
                TicksError.InvalidAttribute(
                    attribute = "insertTicks",
                    value = "true for curve parent",
                ),
            )
        }
        val minimum = curve.minX()
        val maximum = curve.maxX()
        val anchor = curveAnchor(minimum, maximum)
        val positions = when (val source = source) {
            TicksSource.Equidistant -> {
                val minorDistance =
                    attributes.ticksDistance * attributes.scale /
                        (attributes.minorTicks + 1)
                equidistantPositions(
                    lower = minimum,
                    upper = maximum,
                    minorDistance = minorDistance,
                    drawZero = attributes.drawZero,
                    maximumTickCount = maximumTickCount,
                )
            }
            is TicksSource.Fixed -> {
                val requested = source.values.size.toLong()
                if (requested > maximumTickCount) {
                    GMResult.Err(
                        TicksError.TickCountLimitExceeded(
                            limit = maximumTickCount,
                            requestedSize = requested,
                        ),
                    )
                } else {
                    GMResult.Ok(source.values.toList())
                }
            }
        }
        val relativePositions = when (positions) {
            is GMResult.Ok ->
                positions.value.mapIndexed { index, position ->
                    IndexedTickPosition(index, position)
                }
            is GMResult.Err -> return positions
        }
        val derivativeX = Numerics.D(curve::X)
        val derivativeY = Numerics.D(curve::Y)
        val locations = mutableListOf<TicksCurveLocation>()
        for ((sourceIndex, relativePosition) in relativePositions) {
            val parameter = anchor + relativePosition
            if (
                source is TicksSource.Fixed &&
                (
                    parameter < minimum - Mat.eps ||
                        parameter > maximum + Mat.eps
                    )
            ) {
                continue
            }
            val x = curve.X(parameter)
            val y = curve.Y(parameter)
            val normal = curveNormal(
                curve = curve,
                parameter = parameter,
                minimum = minimum,
                maximum = maximum,
                derivativeX = derivativeX,
                derivativeY = derivativeY,
            )
            if (
                !x.isFinite() ||
                !y.isFinite() ||
                !normal.first.isFinite() ||
                !normal.second.isFinite()
            ) {
                continue
            }
            val minorDistance =
                attributes.ticksDistance * attributes.scale /
                    (attributes.minorTicks.toDouble() + 1.0)
            val major =
                source is TicksSource.Fixed ||
                    isMajorTick(
                        tickPosition = relativePosition,
                        minorDistance = minorDistance,
                        minorTicks = attributes.minorTicks,
                    )
            val label = when {
                !attributes.drawLabels -> null
                source is TicksSource.Fixed ->
                    attributes.labels.getOrNull(sourceIndex)
                        ?: formatLabel(parameter)
                shouldLabel(
                    relativePosition,
                    minorDistance,
                ) -> formatLabel(parameter)
                else -> null
            }
            locations += TicksCurveLocation(
                baseX = x,
                baseY = y,
                normalX = normal.first,
                normalY = normal.second,
                major = major,
                label = label,
            )
        }
        return GMResult.Ok(locations)
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> setTicksSizeVariables.
    private fun curveNormal(
        curve: Curve,
        parameter: Double,
        minimum: Double,
        maximum: Double,
        derivativeX: (Double) -> Double,
        derivativeY: (Double) -> Double,
    ): Pair<Double, Double> {
        val points = curve.points
        return when {
            points.size < 2 -> 0.0 to 0.0
            Mat.relDif(parameter, minimum) < Mat.eps ->
                (
                    points[0].usrCoords[2] -
                        points[1].usrCoords[2]
                    ) to
                    (
                        points[1].usrCoords[1] -
                            points[0].usrCoords[1]
                        )
            Mat.relDif(parameter, maximum) < Mat.eps -> {
                val lastIndex = points.lastIndex
                (
                    points[lastIndex - 1].usrCoords[2] -
                        points[lastIndex].usrCoords[2]
                    ) to
                    (
                        points[lastIndex].usrCoords[1] -
                            points[lastIndex - 1].usrCoords[1]
                        )
            }
            else -> -derivativeY(parameter) to derivativeX(parameter)
        }
    }

    private fun curveAnchor(
        minimum: Double,
        maximum: Double,
    ): Double =
        when (val anchor = attributes.anchor) {
            TicksAnchor.Left -> minimum
            TicksAnchor.Right -> maximum
            TicksAnchor.Middle -> (minimum + maximum) * 0.5
            is TicksAnchor.Fraction ->
                minimum * (1.0 - anchor.value) + maximum * anchor.value
        }

    internal fun formatLabel(value: Double): String =
        formatTicksLabel(
            value = value,
            maxLabelLength = attributes.maxLabelLength,
            precision = attributes.precision,
            digits = attributes.digits,
            scaleSymbol = attributes.scaleSymbol,
            beautifulScientificTickLabels =
                attributes.beautifulScientificTickLabels,
            useUnicodeMinus = attributes.useUnicodeMinus,
        )

    internal fun shouldLabel(
        tickPosition: Double,
        minorDistance: Double,
    ): Boolean {
        val ticksPerLabel =
            attributes.ticksPerLabel?.toLong()
                ?: (attributes.minorTicks.toLong() + 1L)
        return round(tickPosition / minorDistance).toLong() %
            ticksPerLabel == 0L
    }

    internal companion object {
        private const val TICKS_ELEMENT_TYPE = "ticks"
        private const val TICKS_ID_PREFIX = "Ti"
        internal const val DEFAULT_MAXIMUM_TICK_COUNT = 2048

        // JSXGraph 1.13.3: src/base/ticks.js -> createTicks / Ticks.
        fun create(
            board: Board,
            parent: GeometryElement,
            source: TicksSource = TicksSource.Equidistant,
            attributes: TicksAttributes = TicksAttributes(),
            id: String = "",
            name: String? = null,
            needsRegularUpdate: Boolean = true,
            maximumTickCount: Int = DEFAULT_MAXIMUM_TICK_COUNT,
        ): GMResult<Ticks, TicksError> {
            if (maximumTickCount < 1) {
                return GMResult.Err(
                    TicksError.InvalidAttribute(
                        attribute = "maximumTickCount",
                        value = maximumTickCount.toString(),
                    ),
                )
            }
            if (parent !is Line && parent !is Curve) {
                return GMResult.Err(
                    TicksError.UnsupportedParent(parent.elType),
                )
            }
            if (parent.board !== board || board.elementById(parent.id) !== parent) {
                return GMResult.Err(
                    TicksError.UnsupportedParent(parent.elType),
                )
            }
            if (source is TicksSource.Fixed) {
                if (source.values.size > maximumTickCount) {
                    return GMResult.Err(
                        TicksError.TickCountLimitExceeded(
                            limit = maximumTickCount,
                            requestedSize = source.values.size.toLong(),
                        ),
                    )
                }
                for ((index, value) in source.values.withIndex()) {
                    if (!value.isFinite()) {
                        return GMResult.Err(
                            TicksError.InvalidFixedTick(index, value),
                        )
                    }
                }
            }
            validateAttributes(attributes)?.let {
                return GMResult.Err(it)
            }
            val requestedId =
                id.ifEmpty { "${parent.id}_ticks_${parent.ticks.size + 1}" }
            val ticks = Ticks(
                board = board,
                parent = parent,
                source = source,
                attributes = attributes,
                id = requestedId,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
                maximumTickCount = maximumTickCount,
            )
            ticks.update()
            ticks.evaluationError?.let { return GMResult.Err(it) }
            return when (val result = board.setId(ticks, TICKS_ID_PREFIX)) {
                is GMResult.Ok -> {
                    parent.ticks += ticks
                    parent.addChild(ticks)
                    ticks.setParents(listOf(parent))
                    ticks.needsUpdate = false
                    GMResult.Ok(ticks)
                }
                is GMResult.Err ->
                    GMResult.Err(TicksError.Registration(result.error))
            }
        }

        private fun validateAttributes(
            attributes: TicksAttributes,
        ): TicksError.InvalidAttribute? {
            val invalid = when {
                !attributes.minTicksDistance.isFinite() ||
                    attributes.minTicksDistance < 0.0 ->
                    "minTicksDistance" to attributes.minTicksDistance
                attributes.minorTicks < 0 ->
                    "minorTicks" to attributes.minorTicks
                attributes.ticksPerLabel != null &&
                    attributes.ticksPerLabel <= 0 ->
                    "ticksPerLabel" to attributes.ticksPerLabel
                !attributes.scale.isFinite() || attributes.scale <= 0.0 ->
                    "scale" to attributes.scale
                attributes.anchor is TicksAnchor.Fraction &&
                    !attributes.anchor.value.isFinite() ->
                    "anchor" to attributes.anchor.value
                !attributes.ticksDistance.isFinite() ->
                    "ticksDistance" to attributes.ticksDistance
                !attributes.minorHeight.isFinite() ->
                    "minorHeight" to attributes.minorHeight
                !attributes.majorHeight.isFinite() ->
                    "majorHeight" to attributes.majorHeight
                !attributes.labelFontSize.isFinite() ||
                    attributes.labelFontSize < 0.0 ->
                    "label.fontSize" to attributes.labelFontSize
                attributes.tickEndings.size != 2 ->
                    "tickEndings" to attributes.tickEndings.size
                attributes.tickEndings.any { !it.isFinite() } ->
                    "tickEndings" to attributes.tickEndings.toList()
                attributes.majorTickEndings.size != 2 ->
                    "majorTickEndings" to attributes.majorTickEndings.size
                attributes.majorTickEndings.any { !it.isFinite() } ->
                    "majorTickEndings" to
                        attributes.majorTickEndings.toList()
                attributes.labelOffset.size != 2 ->
                    "label.offset" to attributes.labelOffset.size
                attributes.labelOffset.any { !it.isFinite() } ->
                    "label.offset" to attributes.labelOffset.toList()
                attributes.face !in setOf("|", "<", ">") ->
                    "face" to attributes.face
                attributes.ticksType !in setOf("linear", "polar") ->
                    "type" to attributes.ticksType
                attributes.labelAnchorX !in setOf("left", "middle", "right") ->
                    "label.anchorX" to attributes.labelAnchorX
                attributes.labelAnchorY !in setOf("top", "middle", "bottom") ->
                    "label.anchorY" to attributes.labelAnchorY
                else -> null
            }
            return invalid?.let {
                TicksError.InvalidAttribute(it.first, it.second.toString())
            }
        }
    }
}

private data class IndexedTickPosition(
    val sourceIndex: Int,
    val position: Double,
)

internal fun equidistantPositions(
    lower: Double,
    upper: Double,
    minorDistance: Double,
    drawZero: Boolean,
    maximumTickCount: Int,
): GMResult<List<Double>, TicksError> {
    if (!minorDistance.isFinite() || minorDistance < Mat.eps) {
        return GMResult.Ok(emptyList())
    }
    val requested =
        ceil((upper - lower).coerceAtLeast(0.0) / minorDistance)
            .toLong() + 2L
    if (requested > maximumTickCount) {
        return GMResult.Err(
            TicksError.TickCountLimitExceeded(
                limit = maximumTickCount,
                requestedSize = requested,
            ),
        )
    }
    val positions = mutableListOf<Double>()
    var tickPosition = if (drawZero) 0.0 else minorDistance
    if (tickPosition < lower) {
        tickPosition = floor((lower - Mat.eps) / minorDistance) *
            minorDistance
    }
    while (tickPosition <= upper + Mat.eps) {
        if (tickPosition >= lower - Mat.eps) {
            positions += tickPosition
        }
        tickPosition += minorDistance
    }
    tickPosition = -minorDistance
    if (tickPosition > upper) {
        tickPosition = ceil((upper + Mat.eps) / -minorDistance) *
            -minorDistance
    }
    while (tickPosition >= lower - Mat.eps) {
        if (tickPosition <= upper + Mat.eps) {
            positions += tickPosition
        }
        tickPosition -= minorDistance
    }
    if (positions.size > maximumTickCount) {
        return GMResult.Err(
            TicksError.TickCountLimitExceeded(
                limit = maximumTickCount,
                requestedSize = positions.size.toLong(),
            ),
        )
    }
    return GMResult.Ok(positions)
}

internal fun isMajorTick(
    tickPosition: Double,
    minorDistance: Double,
    minorTicks: Int,
): Boolean =
    round(tickPosition / minorDistance).toLong() %
        (minorTicks.toLong() + 1L) == 0L

// JSXGraph 1.13.3: src/base/ticks.js -> formatLabelText /
// beautifyScientificNotationLabel.
internal fun formatTicksLabel(
    value: Double,
    maxLabelLength: Int,
    precision: Int,
    digits: Int,
    scaleSymbol: String,
    beautifulScientificTickLabels: Boolean,
    useUnicodeMinus: Boolean,
): String {
    val rounded = round(value * 1.0e11) / 1.0e11
    var text = JsNumberFormat.compact(rounded)
    if (text.length > maxLabelLength || 'e' in text.lowercase()) {
        val effectiveDigits =
            if (precision != 3 && digits == 3) precision else digits
        text = JsNumberFormat.exponential(value, effectiveDigits)
    }
    if (beautifulScientificTickLabels && 'e' in text) {
        val parts = text.split('e', limit = 2)
        val mantissa = parts[0].trimEnd('0').trimEnd('.')
        val exponent = parts[1]
            .removePrefix("+")
            .map { character ->
                when (character) {
                    '-' -> '\u207B'
                    '0' -> '\u2070'
                    '1' -> '\u00B9'
                    '2' -> '\u00B2'
                    '3' -> '\u00B3'
                    '4' -> '\u2074'
                    '5' -> '\u2075'
                    '6' -> '\u2076'
                    '7' -> '\u2077'
                    '8' -> '\u2078'
                    '9' -> '\u2079'
                    else -> character
                }
            }.joinToString("")
        text = "$mantissa\u202210$exponent"
    }
    if ('.' in text && 'e' !in text.lowercase()) {
        text = text.trimEnd('0').trimEnd('.')
    }
    if (scaleSymbol.isNotEmpty()) {
        text = when (text) {
            "1" -> scaleSymbol
            "-1" -> "-$scaleSymbol"
            "0" -> text
            else -> text + scaleSymbol
        }
    }
    return if (useUnicodeMinus) {
        text.replace("-", "\u2212")
    } else {
        text
    }
}
