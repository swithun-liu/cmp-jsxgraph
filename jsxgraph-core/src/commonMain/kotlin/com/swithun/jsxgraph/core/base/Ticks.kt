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
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
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
    internal var attributes: TicksAttributes,
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

    // JSXGraph 1.13.3: src/options.js -> useStandardOptions line ticks.
    internal fun applyStandardOptions(
        minTicksDistance: Double,
        minorHeight: Double,
        majorHeight: Double,
    ): Ticks {
        attributes = attributes.copy(
            minTicksDistance = minTicksDistance,
            minorHeight = minorHeight,
            majorHeight = majorHeight,
        )
        setAttribute(
            mapOf(
                "minticksdistance" to minTicksDistance,
                "minorheight" to minorHeight,
                "majorheight" to majorHeight,
            ),
        )
        return this
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

    // JSXGraph 1.13.3: src/base/ticks.js -> updateRenderer.
    override fun updateRenderer(): Ticks {
        needsUpdate = false
        return this
    }

    override fun remove(): Ticks {
        parent.ticks.remove(this)
        return this
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> hasPoint.
    internal fun hasPoint(
        screenX: Double,
        screenY: Double,
        precision: Double = DEFAULT_HIT_PRECISION,
    ): Boolean {
        val line = parent as? Line ?: return false
        val scalable =
            (line.getAttribute("scalable") as? Boolean) ?: true
        if (!scalable) {
            return false
        }
        val horizontal = line.stdform[1] == 0.0
        val vertical = line.stdform[2] == 0.0
        if (!horizontal && !vertical) {
            return false
        }
        val strokeWidth =
            (getAttribute("strokewidth") as? Number)?.toDouble() ?: 1.0
        val tolerance = precision + strokeWidth * 0.5
        val tickSpan =
            if (attributes.majorHeight < 0.0) {
                Double.POSITIVE_INFINITY
            } else {
                abs(attributes.majorHeight) * 0.5 *
                    attributes.majorTickEndings.count { it > 0.0 }
            }
        if (tickSpan < 1.0) {
            return false
        }
        for (position in majorLineTickPositions(line)) {
            val base = lineTickBase(line, position) ?: continue
            val screen = Coords(
                method = Const.COORDS_BY_USER,
                coordinates = base,
                board = board,
                emitter = false,
            ).scrCoords
            if (
                horizontal &&
                abs(
                    screen[1] - line.point1.coords.scrCoords[1],
                ) >= Mat.eps &&
                abs(
                    screenY - line.point1.coords.scrCoords[2],
                ) < 2.0 * tolerance &&
                screenX > screen[1] - tolerance &&
                screenX < screen[1] + tolerance
            ) {
                return true
            }
            if (
                vertical &&
                abs(
                    screen[2] - line.point1.coords.scrCoords[2],
                ) >= Mat.eps &&
                abs(
                    screenX - line.point1.coords.scrCoords[1],
                ) < 2.0 * tolerance &&
                screenY > screen[2] - tolerance &&
                screenY < screen[2] + tolerance
            ) {
                return true
            }
        }
        return false
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> setPositionDirectly.
    internal fun setPositionDirectly(
        method: Int,
        coordinates: DoubleArray,
        oldCoordinates: DoubleArray,
    ): GMResult<Ticks, BoardError> {
        val line = parent as? Line ?: return GMResult.Ok(this)
        val scalable =
            (line.getAttribute("scalable") as? Boolean) ?: true
        if (line.type != Const.OBJECT_TYPE_AXIS || !scalable) {
            return GMResult.Ok(this)
        }
        val current = Coords(
            method = method,
            coordinates = coordinates,
            board = board,
            emitter = false,
        )
        val previous = Coords(
            method = method,
            coordinates = oldCoordinates,
            board = board,
            emitter = false,
        )
        val boundingBox = board.getBoundingBox()
        if (
            abs(line.stdform[1]) < Mat.eps &&
            abs(
                current.usrCoords[1] *
                    previous.usrCoords[1],
            ) > Mat.eps
        ) {
            val scale =
                previous.usrCoords[1] / current.usrCoords[1]
            boundingBox[0] *= scale
            boundingBox[2] *= scale
        } else if (
            abs(line.stdform[2]) < Mat.eps &&
            abs(
                current.usrCoords[2] *
                    previous.usrCoords[2],
            ) > Mat.eps
        ) {
            val scale =
                previous.usrCoords[2] / current.usrCoords[2]
            boundingBox[1] *= scale
            boundingBox[3] *= scale
        } else {
            return GMResult.Ok(this)
        }
        return when (
            val result = board.setBoundingBox(
                bbox = boundingBox,
                keepAspectRatio = board.keepAspectRatio,
                setZoom = BoardZoomMode.UPDATE,
            )
        ) {
            is GMResult.Ok -> GMResult.Ok(this)
            is GMResult.Err -> result
        }
    }

    private fun majorLineTickPositions(
        line: Line,
    ): List<Double> {
        val point1 = line.point1.coords.usrCoords
        val point2 = line.point2.coords.usrCoords
        val deltaX = point2[1] - point1[1]
        val deltaY = point2[2] - point1[2]
        val length = hypot(deltaX, deltaY)
        if (!length.isFinite() || length <= Mat.eps) {
            return emptyList()
        }
        val directionX = deltaX / length
        val directionY = deltaY / length
        val anchor = attributes.anchor
        val anchorParameter = when {
            line.type == Const.OBJECT_TYPE_AXIS ->
                (
                    -point1[1] * deltaX -
                        point1[2] * deltaY
                    ) / (length * length)
            anchor == TicksAnchor.Right -> 1.0
            anchor == TicksAnchor.Middle -> 0.5
            anchor is TicksAnchor.Fraction -> anchor.value
            else -> 0.0
        }
        val zeroX = point1[1] + deltaX * anchorParameter
        val zeroY = point1[2] + deltaY * anchorParameter
        val boundingBox = board.getBoundingBox()
        val cornerDistances = listOf(
            (boundingBox[0] - zeroX) * directionX +
                (boundingBox[1] - zeroY) * directionY,
            (boundingBox[2] - zeroX) * directionX +
                (boundingBox[1] - zeroY) * directionY,
            (boundingBox[0] - zeroX) * directionX +
                (boundingBox[3] - zeroY) * directionY,
            (boundingBox[2] - zeroX) * directionX +
                (boundingBox[3] - zeroY) * directionY,
        )
        var lower = cornerDistances.minOrNull() ?: return emptyList()
        var upper = cornerDistances.maxOrNull() ?: return emptyList()
        if (!line.straightFirst) {
            lower = max(
                lower,
                (point1[1] - zeroX) * directionX +
                    (point1[2] - zeroY) * directionY,
            )
        }
        if (!line.straightLast) {
            upper = min(
                upper,
                (point2[1] - zeroX) * directionX +
                    (point2[2] - zeroY) * directionY,
            )
        }
        if (lower > upper) {
            return emptyList()
        }
        val fixed = source as? TicksSource.Fixed
        if (fixed != null) {
            return fixed.values.filter {
                it >= lower - Mat.eps && it <= upper + Mat.eps
            }
        }
        val distance =
            if (attributes.insertTicks) {
                val screenUnits = hypot(
                    directionX * board.unitX,
                    directionY * board.unitY,
                )
                val minimumDistance =
                    attributes.minTicksDistance /
                        attributes.scale /
                        screenUnits *
                        (attributes.minorTicks + 1)
                val maximumDistance =
                    (upper - lower) / 6.0 / attributes.scale
                max(
                    nextNiceMinimum(minimumDistance),
                    previousNiceMaximum(maximumDistance),
                ) * attributes.scale
            } else {
                attributes.ticksDistance * attributes.scale
            }
        if (!distance.isFinite() || distance <= Mat.eps) {
            return emptyList()
        }
        val firstIndex = ceil((lower - Mat.eps) / distance).toLong()
        val lastIndex = floor((upper + Mat.eps) / distance).toLong()
        val requested = lastIndex - firstIndex + 1L
        if (
            requested <= 0L ||
            requested > maximumTickCount
        ) {
            return emptyList()
        }
        return List(requested.toInt()) { index ->
            (firstIndex + index) * distance
        }
    }

    private fun lineTickBase(
        line: Line,
        position: Double,
    ): DoubleArray? {
        val point1 = line.point1.coords.usrCoords
        val point2 = line.point2.coords.usrCoords
        val deltaX = point2[1] - point1[1]
        val deltaY = point2[2] - point1[2]
        val length = hypot(deltaX, deltaY)
        if (!length.isFinite() || length <= Mat.eps) {
            return null
        }
        val anchor = attributes.anchor
        val anchorParameter = when {
            line.type == Const.OBJECT_TYPE_AXIS ->
                (
                    -point1[1] * deltaX -
                        point1[2] * deltaY
                    ) / (length * length)
            anchor == TicksAnchor.Right -> 1.0
            anchor == TicksAnchor.Middle -> 0.5
            anchor is TicksAnchor.Fraction -> anchor.value
            else -> 0.0
        }
        return doubleArrayOf(
            1.0,
            point1[1] + deltaX * anchorParameter +
                position * deltaX / length,
            point1[2] + deltaY * anchorParameter +
                position * deltaY / length,
        )
    }

    private fun nextNiceMinimum(
        value: Double,
    ): Double {
        if (!value.isFinite() || value <= 0.0) {
            return 0.0
        }
        var delta = 10.0.pow(floor(log10(value)))
        if (2.0 * delta >= value) {
            delta *= 2.0
        } else if (5.0 * delta >= value) {
            delta *= 5.0
        }
        return delta
    }

    private fun previousNiceMaximum(
        value: Double,
    ): Double {
        if (!value.isFinite() || value <= 0.0) {
            return 0.0
        }
        var delta = 10.0.pow(floor(log10(value)))
        if (5.0 * delta < value) {
            delta *= 5.0
        } else if (2.0 * delta < value) {
            delta *= 2.0
        }
        return delta
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> hideElement.
    internal override fun hideElement(): Ticks {
        setAttribute(mapOf("visible" to false))
        return this
    }

    // JSXGraph 1.13.3: src/base/ticks.js -> showElement.
    internal override fun showElement(): Ticks {
        setAttribute(mapOf("visible" to true))
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
        private const val DEFAULT_HIT_PRECISION = 4.0
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
