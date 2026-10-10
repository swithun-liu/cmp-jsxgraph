/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/jxg.js, src/jsxgraph.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Andreas Walter, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core

import com.swithun.jsxgraph.core.base.Axis
import com.swithun.jsxgraph.core.base.AxisError
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardDefaultAxes
import com.swithun.jsxgraph.core.base.BoardDefaultAxesError
import com.swithun.jsxgraph.core.base.BoardHostAdapter
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Grid
import com.swithun.jsxgraph.core.base.GridError
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.createDefaultAxes
import com.swithun.jsxgraph.core.parser.JessieCodeCreator
import com.swithun.jsxgraph.core.parser.NativeJessieCodeCreators
import com.swithun.jsxgraph.core.reader.FileContentLoader
import com.swithun.jsxgraph.core.reader.FileContentSource
import com.swithun.jsxgraph.core.reader.FileReader
import com.swithun.jsxgraph.core.reader.ReaderError
import com.swithun.jsxgraph.core.reader.ReaderRegistry
import com.swithun.jsxgraph.core.reader.registerCinderellaReader
import com.swithun.jsxgraph.core.reader.registerGeogebraReader
import com.swithun.jsxgraph.core.reader.registerGeonextReader
import com.swithun.jsxgraph.core.reader.registerGraphReader
import com.swithun.jsxgraph.core.reader.registerIntergeoReader
import com.swithun.jsxgraph.core.reader.registerTracenpocheReader
import com.swithun.jsxgraph.core.renderer.CanvasRenderer
import com.swithun.jsxgraph.core.renderer.NoRenderer
import com.swithun.jsxgraph.core.renderer.SvgRenderer
import com.swithun.jsxgraph.core.utils.Color
import com.swithun.jsxgraph.core.utils.ColorError
import com.swithun.jsxgraph.core.utils.Env
import com.swithun.jsxgraph.core.utils.EnvDimensions
import com.swithun.jsxgraph.core.utils.EnvRuntimeCapabilities
import com.swithun.jsxgraph.core.utils.Type
import com.swithun.jsxgraph.core.utils.TypeError
import kotlin.math.max

internal sealed interface JxgApiError {
    data class ConstantRedefinition(
        val name: String,
    ) : JxgApiError

    data class UnknownMethod(
        val name: String,
    ) : JxgApiError

    data class MethodFailure(
        val name: String,
        val message: String?,
    ) : JxgApiError

    data class AttributeFailure(
        val cause: TypeError,
    ) : JxgApiError

    data class InvalidAttribute(
        val name: String,
        val value: Any?,
    ) : JxgApiError

    data class UnsupportedRenderer(
        val renderer: String,
    ) : JxgApiError

    data class RendererFailure(
        val renderer: String,
        val message: String?,
    ) : JxgApiError

    data class ReaderFailure(
        val cause: ReaderError,
    ) : JxgApiError

    data class UnknownBoard(
        val id: String,
    ) : JxgApiError

    data class BoardFailure(
        val operation: String,
        val message: String?,
    ) : JxgApiError

    data class AppBoxFailure(
        val message: String?,
    ) : JxgApiError

    data class ColorFailure(
        val option: String,
        val cause: ColorError,
    ) : JxgApiError

    data class GridFailure(
        val cause: GridError,
    ) : JxgApiError

    data class DefaultAxisPointFailure(
        val axis: String,
        val pointIndex: Int,
        val cause: PointError,
    ) : JxgApiError

    data class DefaultAxisFailure(
        val axis: String,
        val cause: AxisError,
    ) : JxgApiError
}

internal data class JxgColorOptions(
    var fillColor: String? = null,
    var highlightFillColor: String? = null,
    var strokeColor: String? = null,
    var highlightStrokeColor: String? = null,
    var fillOpacity: Double? = null,
    var highlightFillOpacity: Double? = null,
)

internal data class JxgLineTicksOptions(
    var minTicksDistance: Double = 50.0,
    var minorHeight: Double = 4.0,
    var majorHeight: Double = -1.0,
)

internal data class JxgGridOptions(
    var hasGrid: Boolean = false,
    var gridX: Double? = null,
    var gridY: Double? = null,
    var gridColor: String = "#c0c0c0",
    var gridOpacity: Double = 0.5,
    var gridDash: Int = 0,
    var snapToGrid: Boolean = false,
    var snapSizeX: Double = 1.0,
    var snapSizeY: Double = 1.0,
)

internal data class JxgStandardOptions(
    var point: JxgColorOptions = JxgColorOptions(
        fillColor = "#D55E00",
        highlightFillColor = "#c3d9ff",
        strokeColor = "#D55E00",
        highlightStrokeColor = "#c3d9ff",
    ),
    var line: JxgColorOptions = JxgColorOptions(
        fillColor = "none",
        highlightFillColor = "none",
        strokeColor = "#0072B2",
        highlightStrokeColor = "#c3d9ff",
    ),
    var lineTicks: JxgLineTicksOptions = JxgLineTicksOptions(),
    var circle: JxgColorOptions = JxgColorOptions(
        fillColor = "none",
        highlightFillColor = "none",
        strokeColor = "#0072B2",
        highlightStrokeColor = "#c3d9ff",
    ),
    var angle: JxgColorOptions = JxgColorOptions(
        fillColor = "#E69F00",
        highlightFillColor = "#E69F00",
        strokeColor = "#E69F00",
    ),
    var arc: JxgColorOptions = JxgColorOptions(
        fillColor = "none",
        highlightFillColor = "none",
        strokeColor = "#0072B2",
        highlightStrokeColor = "#c3d9ff",
    ),
    var polygon: JxgColorOptions = JxgColorOptions(
        fillColor = "#F0E442",
        highlightFillColor = "#F0E442",
        fillOpacity = 0.3,
        highlightFillOpacity = 0.2,
    ),
    var conic: JxgColorOptions = JxgColorOptions(
        fillColor = "none",
        highlightFillColor = "none",
        strokeColor = "#0072B2",
        highlightStrokeColor = "#c3d9ff",
    ),
    var curve: JxgColorOptions = JxgColorOptions(
        fillColor = "none",
        strokeColor = "#0072B2",
    ),
    var sector: JxgColorOptions = JxgColorOptions(
        fillColor = "#F0E442",
        highlightFillColor = "#F0E442",
        fillOpacity = 0.3,
        highlightFillOpacity = 0.3,
    ),
    var grid: JxgGridOptions = JxgGridOptions(),
    var takeSizeFromFile: Boolean = false,
)

internal class JxgPropertyBag(
    initialValues: Map<String, Any?> = emptyMap(),
) {
    private val values = initialValues.toMutableMap()
    private val constants = mutableSetOf<String>()

    operator fun get(name: String): Any? = values[name]

    fun set(
        name: String,
        value: Any?,
    ): GMResult<Unit, JxgApiError> {
        if (name in constants) {
            return GMResult.Err(JxgApiError.ConstantRedefinition(name))
        }
        values[name] = value
        return GMResult.Ok(Unit)
    }

    internal fun defineConstant(
        name: String,
        value: Any?,
        ignoreRedefine: Boolean,
    ): GMResult<Unit, JxgApiError> {
        if (name in values && ignoreRedefine) {
            return GMResult.Ok(Unit)
        }
        if (name in constants) {
            return GMResult.Err(JxgApiError.ConstantRedefinition(name))
        }
        values[name] = value
        constants += name
        return GMResult.Ok(Unit)
    }

    fun snapshot(): Map<String, Any?> = values.toMap()
}

internal fun interface JxgCallable {
    fun invoke(
        receiver: Any?,
        arguments: List<Any?>,
    ): GMResult<Any?, JxgApiError>
}

internal fun interface JxgMethodTarget {
    fun call(
        name: String,
        receiver: Any?,
        arguments: List<Any?>,
    ): GMResult<Any?, JxgApiError>
}

internal fun interface JxgLogger {
    fun log(message: String)
}

internal data class JxgBoardRegistration(
    val board: Board,
    val containerId: String,
    val renderer: JsxGraphRendererAdapter,
)

internal enum class JsxGraphRendererType(
    val value: String,
) {
    AUTO("auto"),
    SVG("svg"),
    CANVAS("canvas"),
    NO("no"),
    VML("vml"),
}

internal interface JsxGraphRendererAdapter {
    val type: String

    fun clear() = Unit

    fun drawNavigationBar(
        board: Board,
        attributes: Map<String, Any?>,
    ) = Unit

    fun free() = Unit
}

internal fun interface JsxGraphRendererFactory {
    fun create(
        renderer: JsxGraphRendererType,
        containerId: String,
        dimensions: EnvDimensions,
    ): GMResult<JsxGraphRendererAdapter, JxgApiError>
}

internal interface JsxGraphAriaAdapter {
    fun setARIA(
        containerId: String,
        title: String,
    ): GMResult<Unit, JxgApiError>

    fun removeARIANodes(
        containerId: String,
    ): GMResult<Unit, JxgApiError>
}

internal object NoJsxGraphAriaAdapter : JsxGraphAriaAdapter {
    override fun setARIA(
        containerId: String,
        title: String,
    ): GMResult<Unit, JxgApiError> = GMResult.Ok(Unit)

    override fun removeARIANodes(
        containerId: String,
    ): GMResult<Unit, JxgApiError> = GMResult.Ok(Unit)
}

internal data class JxgAppBoxRequest(
    val outerContainerId: String,
    val innerContainerId: String,
    val cssClass: String,
    val cssStyle: String,
    val leftPercent: Double?,
    val topPercent: Double?,
    val widthPercent: Double?,
    val heightPercent: Double?,
)

internal fun interface JxgAppBoxHost {
    fun createInnerContainer(
        request: JxgAppBoxRequest,
    ): GMResult<BoardHostAdapter, JxgApiError>
}

internal object JXG {
    private val registrations =
        linkedMapOf<String, JxgBoardRegistration>()
    private val readerRegistry = ReaderRegistry<Board>().apply {
        registerGeonextReader()
        registerGeogebraReader()
        registerIntergeoReader()
        registerCinderellaReader()
        registerTracenpocheReader()
        registerGraphReader()
    }

    internal var logger: JxgLogger = JxgLogger { }
    internal var options: JxgStandardOptions = JxgStandardOptions()

    internal val boards: Map<String, Board>
        get() = registrations.mapValues { (_, value) -> value.board }

    // JSXGraph 1.13.3: src/jxg.js -> extend.
    internal fun extend(
        target: JxgPropertyBag,
        extension: Map<String, Any?>,
        onlyOwn: Boolean = false,
        toLower: Boolean = false,
    ): GMResult<JxgPropertyBag, JxgApiError> {
        @Suppress("UNUSED_VARIABLE")
        val kotlinMapsOnlyExposeOwnEntries = onlyOwn
        for ((sourceName, value) in extension) {
            val name = if (toLower) sourceName.lowercase() else sourceName
            when (val result = target.set(name, value)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(target)
    }

    // JSXGraph 1.13.3: src/jxg.js -> defineConstant.
    internal fun defineConstant(
        target: JxgPropertyBag,
        name: String,
        value: Any?,
        ignoreRedefine: Boolean = false,
    ): GMResult<Unit, JxgApiError> =
        target.defineConstant(name, value, ignoreRedefine)

    // JSXGraph 1.13.3: src/jxg.js -> extendConstants.
    internal fun extendConstants(
        target: JxgPropertyBag,
        constants: Map<String, Any?>,
        onlyOwn: Boolean = false,
        toUpper: Boolean = false,
    ): GMResult<JxgPropertyBag, JxgApiError> {
        @Suppress("UNUSED_VARIABLE")
        val kotlinMapsOnlyExposeOwnEntries = onlyOwn
        for ((sourceName, value) in constants) {
            val name = if (toUpper) sourceName.uppercase() else sourceName
            when (val result = defineConstant(target, name, value)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(target)
    }

    // JSXGraph 1.13.3: src/jxg.js -> shortcut.
    internal fun shortcut(
        target: JxgMethodTarget,
        method: String,
    ): JxgCallable =
        JxgCallable { receiver, arguments ->
            try {
                target.call(method, receiver, arguments)
            } catch (cause: Exception) {
                GMResult.Err(
                    JxgApiError.MethodFailure(method, cause.message),
                )
            }
        }

    // JSXGraph 1.13.3: src/jxg.js -> getRef.
    internal fun getRef(
        board: Board,
        reference: String,
    ): GeometryElement? {
        deprecated("JXG.getRef()", "Board.select()")
        return board.select(reference)
    }

    // JSXGraph 1.13.3: src/jxg.js -> getReference.
    internal fun getReference(
        board: Board,
        reference: String,
    ): GeometryElement? {
        deprecated("JXG.getReference()", "Board.select()")
        return board.select(reference)
    }

    // JSXGraph 1.13.3: src/jxg.js -> getBoardByContainerId.
    internal fun getBoardByContainerId(containerId: String): Board? =
        registrations.values.firstOrNull { registration ->
            registration.containerId == containerId
        }?.board

    // JSXGraph 1.13.3: src/jxg.js -> deprecated.
    internal fun deprecated(
        what: String,
        replacement: String? = null,
    ) {
        val suffix =
            if (replacement == null) {
                ""
            } else {
                " Please use $replacement instead."
            }
        warn("$what is deprecated.$suffix")
    }

    // JSXGraph 1.13.3: src/jxg.js -> warn.
    internal fun warn(warning: String) {
        logger.log("WARNING: $warning")
    }

    // JSXGraph 1.13.3: src/jxg.js -> debugInt.
    internal fun debugInt(vararg values: Any?) {
        for (value in values) {
            logger.log(value.toString())
        }
    }

    // JSXGraph 1.13.3: src/jxg.js -> debugWST.
    internal fun debugWST(vararg values: Any?) {
        debugInt(*values)
        logger.log("stacktrace unavailable in commonMain")
    }

    // JSXGraph 1.13.3: src/jxg.js -> debugLine.
    internal fun debugLine(vararg values: Any?) {
        debugInt(*values)
        logger.log("Called from commonMain")
    }

    // JSXGraph 1.13.3: src/jxg.js -> debug.
    internal fun debug(vararg values: Any?) {
        debugInt(*values)
    }

    // JSXGraph 1.13.3: src/options.js ->
    // useStandardOptions closure copyProps.
    internal fun useStandardOptions(
        board: Board,
    ): GMResult<Board, JxgApiError> {
        val boardHadGrid = board.grids.isNotEmpty()
        val grid = options.grid
        board._set(
            "grid",
            linkedMapOf(
                "hasgrid" to grid.hasGrid,
                "gridx" to grid.gridX,
                "gridy" to grid.gridY,
                "gridcolor" to grid.gridColor,
                "gridopacity" to grid.gridOpacity,
                "griddash" to grid.gridDash,
                "snaptogrid" to grid.snapToGrid,
                "snapsizex" to grid.snapSizeX,
                "snapsizey" to grid.snapSizeY,
            ),
        )
        board._set("takesizefromfile", options.takeSizeFromFile)

        for (element in board.objects.values.toList()) {
            val style = when {
                element.elementClass == Const.OBJECT_CLASS_POINT ->
                    options.point
                element.elementClass == Const.OBJECT_CLASS_LINE ->
                    options.line
                element.elementClass == Const.OBJECT_CLASS_CIRCLE ->
                    options.circle
                element.type == Const.OBJECT_TYPE_ANGLE ->
                    options.angle
                element.type == Const.OBJECT_TYPE_ARC ->
                    options.arc
                element.type == Const.OBJECT_TYPE_POLYGON ->
                    options.polygon
                element.type == Const.OBJECT_TYPE_CONIC ->
                    options.conic
                element.type == Const.OBJECT_TYPE_CURVE ->
                    options.curve
                element.type == Const.OBJECT_TYPE_SECTOR ->
                    options.sector
                else -> null
            }
            style?.let { element.setAttribute(it.attributeMap()) }
            if (element.elementClass == Const.OBJECT_CLASS_LINE) {
                for (ticks in element.ticks) {
                    ticks.applyStandardOptions(
                        minTicksDistance =
                            options.lineTicks.minTicksDistance,
                        minorHeight = options.lineTicks.minorHeight,
                        majorHeight = options.lineTicks.majorHeight,
                    )
                }
            }
        }

        board.fullUpdate()
        if (boardHadGrid && !grid.hasGrid) {
            board.removeGrids()
        } else if (!boardHadGrid && grid.hasGrid) {
            when (val result = createStandardGrid(board, grid)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(board)
    }

    // JSXGraph 1.13.3: src/options.js -> useBlackWhiteOptions.
    internal fun useBlackWhiteOptions(
        board: Board,
    ): GMResult<Board, JxgApiError> {
        val point = when (
            val result = options.point.toBlackWhite(
                prefix = "point",
                properties = ALL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val line = when (
            val result = options.line.toBlackWhite(
                prefix = "line",
                properties = ALL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val circle = when (
            val result = options.circle.toBlackWhite(
                prefix = "circle",
                properties = ALL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val arc = when (
            val result = options.arc.toBlackWhite(
                prefix = "arc",
                properties = ALL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val polygon = when (
            val result = options.polygon.toBlackWhite(
                prefix = "polygon",
                properties = FILL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val sector = when (
            val result = options.sector.toBlackWhite(
                prefix = "sector",
                properties = FILL_COLOR_PROPERTIES,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val curve = when (
            val result = options.curve.toBlackWhite(
                prefix = "curve",
                properties = setOf(STROKE_COLOR),
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val gridColor = when (val result = Color.rgb2bw(options.grid.gridColor)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                return GMResult.Err(
                    JxgApiError.ColorFailure("grid.gridColor", result.error),
                )
            }
        }

        options = options.copy(
            point = point,
            line = line,
            circle = circle,
            arc = arc,
            polygon = polygon,
            sector = sector,
            curve = curve,
            grid = options.grid.copy(gridColor = gridColor),
        )
        return useStandardOptions(board)
    }

    // JSXGraph 1.13.3: src/jxg.js -> board.
    internal fun board(
        containerId: String,
        attributes: Map<String, Any?> = emptyMap(),
        options: Map<String, Any?> = JSXGraph.defaultOptions,
        rendererFactory: JsxGraphRendererFactory =
            JSXGraph.defaultRendererFactory,
        ariaAdapter: JsxGraphAriaAdapter =
            NoJsxGraphAriaAdapter,
        hostAdapter: BoardHostAdapter? = null,
    ): GMResult<Board, JxgApiError> =
        JSXGraph.initBoard(
            containerId = containerId,
            attributes = attributes,
            options = options,
            rendererFactory = rendererFactory,
            ariaAdapter = ariaAdapter,
            hostAdapter = hostAdapter,
        )

    // JSXGraph 1.13.3: src/jxg.js -> appBox.
    internal fun appBox(
        outerContainerId: String,
        attributes: Map<String, Any?>,
        host: JxgAppBoxHost,
        options: Map<String, Any?> = JSXGraph.defaultOptions,
        rendererFactory: JsxGraphRendererFactory =
            JSXGraph.defaultRendererFactory,
        ariaAdapter: JsxGraphAriaAdapter =
            NoJsxGraphAriaAdapter,
    ): GMResult<Board, JxgApiError> {
        val merged = when (
            val result = JSXGraph._setAttributes(attributes, options)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val box = mapValue(merged["jxgbox"])
        val innerId =
            "${outerContainerId}_${box["id"]?.toString() ?: "jxgbox"}"
        val percentages = appBoxPercentages(
            boundingBox = numberList(merged["boundingbox"]),
            outerBox = numberList(box["outerbox"]),
        )
        val request = JxgAppBoxRequest(
            outerContainerId = outerContainerId,
            innerContainerId = innerId,
            cssClass = box["cssclass"]?.toString().orEmpty(),
            cssStyle = box["cssstyle"]?.toString().orEmpty(),
            leftPercent = percentages?.get(0),
            topPercent = percentages?.get(1),
            widthPercent = percentages?.get(2),
            heightPercent = percentages?.get(3),
        )
        val hostAdapter = when (val result = host.createInnerContainer(request)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return board(
            containerId = innerId,
            attributes = attributes,
            options = options,
            rendererFactory = rendererFactory,
            ariaAdapter = ariaAdapter,
            hostAdapter = hostAdapter,
        )
    }

    internal fun registerBoard(registration: JxgBoardRegistration) {
        registrations[registration.board.id] = registration
    }

    internal fun registration(board: Board): JxgBoardRegistration? =
        registrations[board.id]?.takeIf { it.board === board }

    internal fun unregisterBoard(board: Board) {
        val current = registrations[board.id]
        if (current?.board === board) {
            registrations.remove(board.id)
        }
    }

    internal fun readers(): ReaderRegistry<Board> = readerRegistry

    internal fun registerElement(
        name: String,
        creator: JessieCodeCreator,
    ) {
        NativeJessieCodeCreators.registerElement(name, creator)
    }

    internal fun unregisterElement(name: String): JessieCodeCreator? =
        NativeJessieCodeCreators.unregisterElement(name)

    private fun appBoxPercentages(
        boundingBox: List<Double>?,
        outerBox: List<Double>?,
    ): DoubleArray? {
        if (
            boundingBox == null ||
            boundingBox.size < 4 ||
            outerBox == null ||
            outerBox.size < 4
        ) {
            return null
        }
        val width = outerBox[2] - outerBox[0]
        val height = outerBox[1] - outerBox[3]
        if (width == 0.0 || height == 0.0) {
            return null
        }
        return doubleArrayOf(
            100.0 * (boundingBox[0] - outerBox[0]) / width,
            100.0 * (outerBox[1] - boundingBox[1]) / height,
            100.0 * (boundingBox[2] - boundingBox[0]) / width,
            100.0 * (boundingBox[1] - boundingBox[3]) / height,
        )
    }

    private fun JxgColorOptions.attributeMap(): Map<String, Any?> =
        buildMap {
            fillColor?.let { put("fillcolor", it) }
            highlightFillColor?.let { put("highlightfillcolor", it) }
            strokeColor?.let { put("strokecolor", it) }
            highlightStrokeColor?.let {
                put("highlightstrokecolor", it)
            }
            fillOpacity?.let { put("fillopacity", it) }
            highlightFillOpacity?.let {
                put("highlightfillopacity", it)
            }
        }

    private fun JxgColorOptions.toBlackWhite(
        prefix: String,
        properties: Set<String>,
    ): GMResult<JxgColorOptions, JxgApiError> {
        fun convert(
            property: String,
            value: String?,
        ): GMResult<String?, JxgApiError> {
            if (property !in properties || value == null) {
                return GMResult.Ok(value)
            }
            return when (val result = Color.rgb2bw(value)) {
                is GMResult.Ok -> GMResult.Ok(result.value)
                is GMResult.Err -> GMResult.Err(
                    JxgApiError.ColorFailure(
                        option = "$prefix.$property",
                        cause = result.error,
                    ),
                )
            }
        }

        val fill = when (val result = convert(FILL_COLOR, fillColor)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val highlightFill = when (
            val result = convert(
                HIGHLIGHT_FILL_COLOR,
                highlightFillColor,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val stroke = when (val result = convert(STROKE_COLOR, strokeColor)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val highlightStroke = when (
            val result = convert(
                HIGHLIGHT_STROKE_COLOR,
                highlightStrokeColor,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return GMResult.Ok(
            copy(
                fillColor = fill,
                highlightFillColor = highlightFill,
                strokeColor = stroke,
                highlightStrokeColor = highlightStroke,
            ),
        )
    }

    private fun createStandardGrid(
        board: Board,
        options: JxgGridOptions,
    ): GMResult<Unit, JxgApiError> {
        fun gridLength(value: Double?): JsxGraphGridLength =
            value?.let(JsxGraphGridLength::User)
                ?: JsxGraphGridLength.Auto

        val faceDrawZero = JsxGraphGridDrawZero.All
        val definition = JsxGraphGrid2D(
            role = JsxGraphGridRole.Major,
            majorStep = JsxGraphGridPair(
                gridLength(options.gridX),
                gridLength(options.gridY),
            ),
            minorElements = JsxGraphGridPair(
                JsxGraphGridMinorElements.Fixed(0.0),
                JsxGraphGridMinorElements.Fixed(0.0),
            ),
            forceSquare = JsxGraphGridForceSquare.None,
            includeBoundaries = false,
            major = JsxGraphGridFace(
                face = "line",
                size = JsxGraphGridPair(
                    JsxGraphGridLength.Pixels(5.0),
                    JsxGraphGridLength.Pixels(5.0),
                ),
                margin = 0.0,
                drawZero = faceDrawZero,
                polygonVertices = 6,
            ),
            minor = JsxGraphGridFace(
                face = "point",
                size = JsxGraphGridPair(
                    JsxGraphGridLength.Pixels(3.0),
                    JsxGraphGridLength.Pixels(3.0),
                ),
                margin = 0.0,
                drawZero = faceDrawZero,
                polygonVertices = 6,
            ),
            maximumPointCount = board.maxCurvePoints,
        )
        return when (
            val result = Grid.create(
                board = board,
                parentAxes = emptyList(),
                definition = definition,
            )
        ) {
            is GMResult.Err ->
                GMResult.Err(JxgApiError.GridFailure(result.error))
            is GMResult.Ok -> {
                val attributes = mapOf(
                    "strokecolor" to options.gridColor,
                    "strokeopacity" to options.gridOpacity,
                    "dash" to options.gridDash,
                )
                result.value.setAttribute(attributes)
                result.value.minorGrid?.setAttribute(attributes)
                GMResult.Ok(Unit)
            }
        }
    }

    private const val FILL_COLOR = "fillColor"
    private const val HIGHLIGHT_FILL_COLOR = "highlightFillColor"
    private const val STROKE_COLOR = "strokeColor"
    private const val HIGHLIGHT_STROKE_COLOR = "highlightStrokeColor"
    private val FILL_COLOR_PROPERTIES =
        setOf(FILL_COLOR, HIGHLIGHT_FILL_COLOR)
    private val ALL_COLOR_PROPERTIES =
        FILL_COLOR_PROPERTIES +
            setOf(STROKE_COLOR, HIGHLIGHT_STROKE_COLOR)
}

internal object JSXGraph {
    internal val defaultOptions: Map<String, Any?> = mapOf(
        "elements" to emptyMap<String, Any?>(),
        "board" to mapOf(
            "id" to "",
            "renderer" to "auto",
            "boundingbox" to listOf(-5.0, 5.0, 5.0, -5.0),
            "maxboundingbox" to listOf(
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            ),
            "keepaspectratio" to false,
            "zoomx" to 1.0,
            "zoomy" to 1.0,
            "infobox" to emptyMap<String, Any?>(),
            "selection" to emptyMap<String, Any?>(),
            "navbar" to emptyMap<String, Any?>(),
            "jxgbox" to mapOf(
                "id" to "jxgbox",
                "cssclass" to "",
                "cssstyle" to "",
                "outerbox" to null,
            ),
        ),
        "navbar" to emptyMap<String, Any?>(),
    )

    internal val defaultRendererFactory =
        JsxGraphRendererFactory { renderer, containerId, dimensions ->
            when (renderer) {
                JsxGraphRendererType.VML ->
                    GMResult.Err(
                        JxgApiError.UnsupportedRenderer(renderer.value),
                    )
                JsxGraphRendererType.NO ->
                    GMResult.Ok(NoRenderer())
                JsxGraphRendererType.SVG ->
                    GMResult.Ok(SvgRenderer(containerId, dimensions))
                JsxGraphRendererType.CANVAS,
                JsxGraphRendererType.AUTO,
                -> GMResult.Ok(
                    CanvasRenderer(containerId, dimensions),
                )
            }
        }

    // JSXGraph 1.13.3: src/jsxgraph.js -> initRenderer.
    internal fun initRenderer(
        containerId: String,
        dimensions: EnvDimensions,
        requestedRenderer: String?,
        capabilities: EnvRuntimeCapabilities = EnvRuntimeCapabilities(),
        rendererFactory: JsxGraphRendererFactory =
            defaultRendererFactory,
    ): GMResult<JsxGraphRendererAdapter, JxgApiError> {
        val renderer = when (requestedRenderer?.lowercase() ?: "auto") {
            "auto" -> rendererType(capabilities)
            "svg" -> JsxGraphRendererType.SVG
            "canvas" -> JsxGraphRendererType.CANVAS
            "no" -> JsxGraphRendererType.NO
            "vml" -> JsxGraphRendererType.VML
            else -> {
                return GMResult.Err(
                    JxgApiError.UnsupportedRenderer(
                        requestedRenderer.orEmpty(),
                    ),
                )
            }
        }
        if (
            renderer == JsxGraphRendererType.SVG &&
            !Env.supportsSVG(capabilities)
        ) {
            return GMResult.Err(
                JxgApiError.UnsupportedRenderer(renderer.value),
            )
        }
        if (
            renderer == JsxGraphRendererType.CANVAS &&
            !Env.supportsCanvas(capabilities)
        ) {
            return GMResult.Err(
                JxgApiError.UnsupportedRenderer(renderer.value),
            )
        }
        if (renderer == JsxGraphRendererType.VML) {
            return GMResult.Err(
                JxgApiError.UnsupportedRenderer(renderer.value),
            )
        }
        return try {
            rendererFactory.create(renderer, containerId, dimensions)
        } catch (cause: Exception) {
            GMResult.Err(
                JxgApiError.RendererFailure(
                    renderer = renderer.value,
                    message = cause.message,
                ),
            )
        }
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> _setAttributes.
    internal fun _setAttributes(
        attributes: Map<String, Any?>,
        options: Map<String, Any?> = defaultOptions,
    ): GMResult<Map<String, Any?>, JxgApiError> {
        val boardAttributes = when (
            val result = Type.copyAttributes(
                attributes = attributes,
                options = options,
                path = listOf("board"),
            )
        ) {
            is GMResult.Ok -> result.value.toMutableMap()
            is GMResult.Err -> {
                return GMResult.Err(
                    JxgApiError.AttributeFailure(result.error),
                )
            }
        }
        for (key in BOARD_ATTRIBUTE_BRANCHES) {
            val copied = Type.copyAttributes(
                attributes = boardAttributes,
                options = options,
                path = listOf("board", key),
            )
            when (copied) {
                is GMResult.Ok -> boardAttributes[key] = copied.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        JxgApiError.AttributeFailure(copied.error),
                    )
                }
            }
        }
        val navbar = Type.copyAttributes(
            attributes = mapValue(boardAttributes["navbar"]),
            options = options,
            path = listOf("navbar"),
        )
        when (navbar) {
            is GMResult.Ok -> boardAttributes["navbar"] = navbar.value
            is GMResult.Err -> {
                return GMResult.Err(
                    JxgApiError.AttributeFailure(navbar.error),
                )
            }
        }
        boardAttributes["movetarget"] =
            attributes["moveTarget"]
                ?: attributes["movetarget"]
                ?: mapValue(options["board"])["moveTarget"]
        return GMResult.Ok(boardAttributes)
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> _fillBoard.
    internal fun _fillBoard(
        board: Board,
        containerId: String,
        attributes: Map<String, Any?>,
        dimensions: EnvDimensions,
        renderer: JsxGraphRendererAdapter,
    ): Board {
        board.initInfobox(mapValue(attributes["infobox"]))
        numberList(attributes["maxboundingbox"])
            ?.takeIf { it.size >= 4 }
            ?.let { board.maxBoundingBox = it.take(4).toDoubleArray() }
        board.resizeContainer(
            width = dimensions.width,
            height = dimensions.height,
            dontSetHostSize = true,
            dontSetBoundingBox = true,
        )
        board._createSelectionPolygon(
            mapValue(attributes["selection"]),
        )
        renderer.drawNavigationBar(
            board = board,
            attributes = mapValue(attributes["navbar"]),
        )
        JXG.registerBoard(
            JxgBoardRegistration(
                board = board,
                containerId = containerId,
                renderer = renderer,
            ),
        )
        return board
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> _setARIA.
    internal fun _setARIA(
        containerId: String,
        attributes: Map<String, Any?>,
        adapter: JsxGraphAriaAdapter,
    ): GMResult<Unit, JxgApiError> =
        adapter.setARIA(
            containerId = containerId,
            title = attributes["title"]?.toString().orEmpty(),
        )

    // JSXGraph 1.13.3: src/jsxgraph.js -> _removeARIANodes.
    internal fun _removeARIANodes(
        board: Board,
        adapter: JsxGraphAriaAdapter,
    ): GMResult<Unit, JxgApiError> {
        val registration = JXG.registration(board)
            ?: return GMResult.Err(JxgApiError.UnknownBoard(board.id))
        return adapter.removeARIANodes(registration.containerId)
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> JSXGraph.initBoard.
    internal fun initBoard(
        containerId: String,
        attributes: Map<String, Any?> = emptyMap(),
        options: Map<String, Any?> = defaultOptions,
        rendererFactory: JsxGraphRendererFactory =
            defaultRendererFactory,
        ariaAdapter: JsxGraphAriaAdapter =
            NoJsxGraphAriaAdapter,
        hostAdapter: BoardHostAdapter? = null,
    ): GMResult<Board, JxgApiError> {
        val attr = when (val result = _setAttributes(attributes, options)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val dimensions = hostAdapter?.size()?.let { size ->
            EnvDimensions(
                width = size.getOrElse(0) { 500.0 },
                height = size.getOrElse(1) { 500.0 },
            )
        } ?: Env.defaultDimensions()
        if (
            !dimensions.width.isFinite() ||
            !dimensions.height.isFinite() ||
            dimensions.width <= 0.0 ||
            dimensions.height <= 0.0
        ) {
            return GMResult.Err(
                JxgApiError.InvalidAttribute("dimensions", dimensions),
            )
        }
        val boundingBox = numberList(attr["boundingbox"])
            ?.takeIf { it.size >= 4 }
            ?.take(4)
            ?.toDoubleArray()
            ?: return GMResult.Err(
                JxgApiError.InvalidAttribute(
                    "boundingbox",
                    attr["boundingbox"],
                ),
            )
        val maxBoundingBox = numberList(attr["maxboundingbox"])
            ?.takeIf { it.size >= 4 }
            ?.take(4)
            ?: listOf(
                Double.NEGATIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
            )
        boundingBox[0] = max(boundingBox[0], maxBoundingBox[0])
        boundingBox[1] = kotlin.math.min(
            boundingBox[1],
            maxBoundingBox[1],
        )
        boundingBox[2] = kotlin.math.min(
            boundingBox[2],
            maxBoundingBox[2],
        )
        boundingBox[3] = max(boundingBox[3], maxBoundingBox[3])
        if (
            boundingBox.any { !it.isFinite() } ||
            boundingBox[0] == boundingBox[2] ||
            boundingBox[1] == boundingBox[3]
        ) {
            return GMResult.Err(
                JxgApiError.InvalidAttribute("boundingbox", boundingBox),
            )
        }
        val renderer = when (
            val result = initRenderer(
                containerId = containerId,
                dimensions = dimensions,
                requestedRenderer = attr["renderer"]?.toString(),
                rendererFactory = rendererFactory,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (val result = _setARIA(containerId, attr, ariaAdapter)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        val board = Board.fromBoundingBox(
            boundingBox = boundingBox,
            keepAspectRatio = attr["keepaspectratio"] as? Boolean ?: false,
            id = attr["id"]?.toString()?.takeIf(String::isNotEmpty)
                ?: containerId,
            canvasWidth = dimensions.width,
            canvasHeight = dimensions.height,
            defaultCurveMinimum =
                boundingBox[0] -
                    (boundingBox[2] - boundingBox[0]) *
                    Board.CURVE_DOMAIN_PADDING,
            defaultCurveMaximum =
                boundingBox[2] +
                    (boundingBox[2] - boundingBox[0]) *
                    Board.CURVE_DOMAIN_PADDING,
        )
        val initialized = _fillBoard(
            board = board,
            containerId = containerId,
            attributes = attr,
            dimensions = dimensions,
            renderer = renderer,
        )
        val hasDefaultAxes = when (val axis = attr["axis"]) {
            true -> true
            is Map<*, *> -> true
            else -> false
        }
        if (hasDefaultAxes) {
            when (
                val result = createDefaultAxes(
                    board = initialized,
                    axisAttributes = mapValue(attr["axis"]),
                    defaultAxesAttributes =
                        mapValue(attr["defaultaxes"]),
                )
            ) {
                is GMResult.Ok -> initialized.defaultAxes = result.value
                is GMResult.Err -> {
                    freeBoard(initialized, ariaAdapter)
                    return GMResult.Err(
                        when (val error = result.error) {
                            is BoardDefaultAxesError.PointFactory ->
                                JxgApiError.DefaultAxisPointFailure(
                                    axis = error.axis,
                                    pointIndex = error.pointIndex,
                                    cause = error.cause,
                                )
                            is BoardDefaultAxesError.AxisFactory ->
                                JxgApiError.DefaultAxisFailure(
                                    axis = error.axis,
                                    cause = error.cause,
                                )
                        },
                    )
                }
            }
        }
        return GMResult.Ok(initialized)
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> loadBoardFromFile.
    internal fun loadBoardFromFile(
        containerId: String,
        source: FileContentSource,
        format: String,
        attributes: Map<String, Any?> = emptyMap(),
        loader: FileContentLoader,
        callback: ((Board) -> Unit)? = null,
    ): GMResult<Board, JxgApiError> {
        val board = when (val result = initBoard(containerId, attributes)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val encoding =
            attributes["encoding"]?.toString() ?: "iso-8859-1"
        val parsed = FileReader.parseFileContent(
            source = source,
            board = board,
            format = format,
            encoding = encoding,
            registry = JXG.readers(),
            remoteLoader = loader,
            localLoader = loader,
            callback = callback,
        )
        return when (parsed) {
            is GMResult.Ok -> GMResult.Ok(board)
            is GMResult.Err -> {
                freeBoard(board)
                GMResult.Err(JxgApiError.ReaderFailure(parsed.error))
            }
        }
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> loadBoardFromString.
    internal fun loadBoardFromString(
        containerId: String,
        source: String,
        format: String,
        attributes: Map<String, Any?> = emptyMap(),
        callback: ((Board) -> Unit)? = null,
    ): GMResult<Board, JxgApiError> {
        val board = when (val result = initBoard(containerId, attributes)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val parsed = FileReader.parseString(
            source = source,
            board = board,
            format = format,
            registry = JXG.readers(),
            callback = callback,
        )
        return when (parsed) {
            is GMResult.Ok -> GMResult.Ok(board)
            is GMResult.Err -> {
                freeBoard(board)
                GMResult.Err(JxgApiError.ReaderFailure(parsed.error))
            }
        }
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> freeBoard.
    internal fun freeBoard(
        board: Board,
        ariaAdapter: JsxGraphAriaAdapter =
            NoJsxGraphAriaAdapter,
    ): GMResult<Unit, JxgApiError> {
        val registration = JXG.registration(board)
            ?: return GMResult.Err(JxgApiError.UnknownBoard(board.id))
        when (val result = _removeARIANodes(board, ariaAdapter)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return result
        }
        return try {
            board.removeEventHandlers()
            board.suspendUpdate()
            board.removeObjects(board.objects.values.toList())
            registration.renderer.clear()
            registration.renderer.free()
            JXG.unregisterBoard(board)
            GMResult.Ok(Unit)
        } catch (cause: Exception) {
            GMResult.Err(
                JxgApiError.BoardFailure(
                    operation = "freeBoard",
                    message = cause.message,
                ),
            )
        }
    }

    internal fun freeBoard(
        boardId: String,
        ariaAdapter: JsxGraphAriaAdapter =
            NoJsxGraphAriaAdapter,
    ): GMResult<Unit, JxgApiError> {
        val board = JXG.boards[boardId]
            ?: return GMResult.Err(JxgApiError.UnknownBoard(boardId))
        return freeBoard(board, ariaAdapter)
    }

    // JSXGraph 1.13.3: src/jsxgraph.js -> registerElement.
    internal fun registerElement(
        name: String,
        creator: JessieCodeCreator,
    ) {
        JXG.deprecated(
            "JXG.JSXGraph.registerElement()",
            "JXG.registerElement()",
        )
        JXG.registerElement(name, creator)
    }

    private fun rendererType(
        capabilities: EnvRuntimeCapabilities,
    ): JsxGraphRendererType =
        when {
            Env.supportsSVG(capabilities) -> JsxGraphRendererType.SVG
            Env.supportsCanvas(capabilities) ->
                JsxGraphRendererType.CANVAS
            else -> JsxGraphRendererType.NO
        }

    private val BOARD_ATTRIBUTE_BRANCHES = listOf(
        "drag",
        "fullscreen",
        "intl",
        "keyboard",
        "logging",
        "pan",
        "resize",
        "screenshot",
        "selection",
        "zoom",
    )
}

private fun mapValue(value: Any?): Map<String, Any?> =
    (value as? Map<*, *>)
        ?.entries
        ?.mapNotNull { entry ->
            val key = entry.key as? String ?: return@mapNotNull null
            key to entry.value
        }
        ?.toMap()
        .orEmpty()

private fun numberList(value: Any?): List<Double>? =
    when (value) {
        is DoubleArray -> value.toList()
        is FloatArray -> value.map(Float::toDouble)
        is IntArray -> value.map(Int::toDouble)
        is List<*> -> value.map { item ->
            (item as? Number)?.toDouble() ?: return null
        }
        else -> null
    }
