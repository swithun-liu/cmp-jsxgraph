/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/base/chart.js -> createChart, Chart, Legend
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Chart
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Legend
import com.swithun.jsxgraph.core.math.Numerics
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sin

internal object NativeChartCreator {
    private val defaultColors = listOf(
        "#B02B2C",
        "#3F4C6B",
        "#C79810",
        "#D15600",
        "#FFFF88",
        "#c3d9ff",
        "#4096EE",
        "#008C00",
    )
    private val commonSceneAttributes = setOf(
        "id",
        "name",
        "needsregularupdate",
        "visible",
        "strokecolor",
        "fillcolor",
        "strokewidth",
        "strokeopacity",
        "fillopacity",
        "layer",
        "fixed",
        "highlight",
        "withlabel",
        "dash",
        "dashscale",
    )
    private val sceneAttributesByCreator = mapOf(
        "point" to setOf(
            "size",
            "face",
            "alwaysintersect",
            "precision",
        ),
        "line" to setOf(
            "straightfirst",
            "straightlast",
            "firstarrow",
            "lastarrow",
            "nonnegativeonly",
        ),
        "circle" to setOf("nonnegativeonly"),
        "curve" to setOf(
            "doadvancedplot",
            "numberpointshigh",
            "plotversion",
            "recursiondepthhigh",
            "rdpsmoothing",
            "rdpthreshold",
            "firstarrow",
            "lastarrow",
            "linecap",
            "createpoints",
            "isarrayofcoordinates",
            "points",
        ),
        "functiongraph" to setOf(
            "doadvancedplot",
            "numberpointshigh",
            "plotversion",
            "recursiondepthhigh",
            "rdpsmoothing",
            "rdpthreshold",
            "firstarrow",
            "lastarrow",
            "linecap",
            "createpoints",
            "isarrayofcoordinates",
            "points",
        ),
        "spline" to setOf(
            "doadvancedplot",
            "numberpointshigh",
            "plotversion",
            "recursiondepthhigh",
            "rdpsmoothing",
            "rdpthreshold",
            "firstarrow",
            "lastarrow",
            "linecap",
            "createpoints",
            "isarrayofcoordinates",
            "points",
        ),
        "polygon" to setOf("withlines"),
        "sector" to setOf(
            "selection",
            "orientation",
            "usedirection",
            "firstarrow",
            "lastarrow",
            "linecap",
        ),
        "text" to setOf(
            "fontsize",
            "fontunit",
            "formatnumber",
            "digits",
            "parse",
            "display",
            "anchorx",
            "anchory",
            "rotate",
            "usemathjax",
            "usekatex",
            "useasciimathml",
            "tofraction",
        ),
    )

    internal fun sceneElementCount(
        creatorName: String,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int? =
        when (creatorName) {
            "chart" -> chartSceneElementCount(
                parents = parents,
                attributes = effectiveChartAttributes(attributes),
            )
            "legend" -> legendSceneElementCount(attributes)
            else -> null
        }

    internal fun createChart(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): NativeChartCreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName = "chart",
                reason = "Board is unavailable.",
                location = location,
            )
        val initialObjects = resolvedBoard.objectsList.toSet()
        val result = createChartInternal(
            board = resolvedBoard,
            parents = parents,
            attributes = attributes,
            location = location,
        )
        if (result is GMResult.Err) {
            removeNewObjects(resolvedBoard, initialObjects)
        }
        return result
    }

    internal fun createLegend(
        board: Board?,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): NativeChartCreatorResult {
        val resolvedBoard = board
            ?: return failure(
                creatorName = "legend",
                reason = "Board is unavailable.",
                location = location,
            )
        val initialObjects = resolvedBoard.objectsList.toSet()
        val result = createLegendInternal(
            board = resolvedBoard,
            parents = parents,
            attributes = attributes,
            location = location,
        )
        if (result is GMResult.Err) {
            removeNewObjects(resolvedBoard, initialObjects)
        }
        return result
    }

    private fun createChartInternal(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): NativeChartCreatorResult {
        if (parents.isEmpty()) {
            return failure(
                creatorName = "chart",
                reason = "Can't create a chart without data.",
                location = location,
            )
        }
        val chartAttributes = effectiveChartAttributes(attributes)
        val style = when (
            val result = stringAttribute(
                creatorName = "chart",
                attributes = chartAttributes,
                name = "chartstyle",
                default = "line",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val styleParts = style.replace(" ", "").split(",")
        val data = when (
            val result = chartData(parents, location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> {
                val usesOnlyParentRows = styleParts.all { part ->
                    part == "radar" ||
                        part !in setOf(
                            "bar",
                            "line",
                            "fit",
                            "spline",
                            "pie",
                            "point",
                        )
                }
                if (
                    !usesOnlyParentRows ||
                    parents.size <= 2 ||
                    parents.any {
                        it !is JessieCodeRuntimeValue.ArrayValue
                    }
                ) {
                    return result
                }
                ChartData(
                    x = emptyList(),
                    y = listOf(number(0.0)),
                )
            }
        }
        if (data.y.isEmpty()) {
            return failure(
                creatorName = "chart",
                reason = "Can't create charts without data.",
                location = location,
            )
        }
        val context = ChartContext(board, location)
        val output = mutableListOf<JessieCodeRuntimeValue>()
        for (part in styleParts) {
            val result = when (part) {
                "bar" -> drawBar(context, data, chartAttributes)
                "line" -> drawLine(context, data, chartAttributes)
                "fit" -> drawFit(context, data, chartAttributes)
                "spline" -> drawSpline(context, data, chartAttributes)
                "pie" -> drawPie(context, data.y, chartAttributes)
                "point" -> drawPoints(context, data, chartAttributes)
                "radar" -> drawRadar(
                    context = context,
                    parents = parents,
                    attributes = chartAttributes,
                )
                else -> GMResult.Ok(JessieCodeRuntimeValue.UndefinedValue)
            }
            when (result) {
                is GMResult.Ok -> output += result.value
                is GMResult.Err -> return result
            }
        }
        val identity = when (
            val result = identity(
                creatorName = "chart",
                attributes = chartAttributes,
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        when (
            val result = Chart.create(
                board = board,
                elements = context.sceneElements.map(
                    JessieCodeCreatedSceneElement::element,
                ),
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return failure(
                creatorName = "chart",
                reason = result.error.toString(),
                location = location,
            )
        }
        return GMResult.Ok(
            JessieCodeRuntimeValue.ArrayValue(
                values = output,
                createdSceneElements = context.sceneElements.toList(),
                isSceneComposite = true,
            ),
        )
    }

    private fun drawLine(
        context: ChartContext,
        data: ChartData,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val childAttributes = copyAttributes(
            attributes,
            overrides = mapOf(
                "fillcolor" to string("none"),
                "highlightfillcolor" to string("none"),
            ),
            removed = setOf("id"),
        )
        return context.element(
            creatorName = "curve",
            parents = listOf(array(data.x), array(data.y)),
            attributes = childAttributes,
        ).mapValue(::elementValue)
    }

    private fun drawSpline(
        context: ChartContext,
        data: ChartData,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val childAttributes = copyAttributes(
            attributes,
            overrides = mapOf(
                "fillcolor" to string("none"),
                "highlightfillcolor" to string("none"),
            ),
            removed = setOf("id"),
        )
        return context.element(
            creatorName = "spline",
            parents = listOf(array(data.x), array(data.y)),
            attributes = childAttributes,
        ).mapValue(::elementValue)
    }

    private fun drawFit(
        context: ChartContext,
        data: ChartData,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val x = when (val result = evaluateNumbers(data.x, context.location)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val y = when (val result = evaluateNumbers(data.y, context.location)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val rawDegree = numberLike(attributes.properties["degree"]) ?: 1.0
        val degree = max(
            floor(rawDegree).takeIf(Double::isFinite)?.toInt() ?: 1,
            1,
        )
        val polynomial = when (
            val result = Numerics.regressionPolynomial(
                degree = degree.toDouble(),
                dataX = x,
                dataY = y,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return failure(
                creatorName = "chart",
                reason = "Regression polynomial failed: ${result.error}.",
                location = context.location,
            )
        }
        val function = JessieCodeRuntimeValue.FunctionValue(
            name = "chart.fit",
            callable = JessieCodeCallable { arguments, callLocation ->
                val input = arguments.firstOrNull() as?
                    JessieCodeRuntimeValue.NumberValue
                    ?: return@JessieCodeCallable GMResult.Err(
                        JessieCodeRuntimeError.InvalidArgumentType(
                            functionName = "chart.fit",
                            argumentIndex = 0,
                            expected = "number",
                            actual =
                                arguments.firstOrNull()?.let(::typeName)
                                    ?: "undefined",
                            location = callLocation,
                        ),
                    )
                when (val result = polynomial(input.value)) {
                    is GMResult.Ok -> GMResult.Ok(number(result.value))
                    is GMResult.Err -> GMResult.Err(
                        JessieCodeRuntimeError.BuiltInInvocationFailure(
                            functionName = "chart.fit",
                            reason = result.error.toString(),
                            location = callLocation,
                        ),
                    )
                }
            },
        )
        val childAttributes = copyAttributes(
            attributes,
            overrides = mapOf(
                "fillcolor" to string("none"),
                "highlightfillcolor" to string("none"),
            ),
            removed = setOf("id"),
        )
        return context.element(
            creatorName = "functiongraph",
            parents = listOf(
                function,
                number(context.board.defaultCurveMinimum),
                number(context.board.defaultCurveMaximum),
            ),
            attributes = childAttributes,
        ).mapValue(::elementValue)
    }

    private fun drawPoints(
        context: ChartContext,
        data: ChartData,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val points = mutableListOf<JessieCodeRuntimeValue>()
        val infobox = attributeArray(attributes, "infoboxarray")
        for (index in data.x.indices) {
            val overrides = linkedMapOf<String, JessieCodeRuntimeValue>(
                "fixed" to boolean(true),
                "name" to string(""),
            )
            if (infobox != null && infobox.isNotEmpty()) {
                overrides["infoboxtext"] = infobox[index % infobox.size]
            }
            val child = when (
                val result = context.element(
                    creatorName = "point",
                    parents = listOf(data.x[index], data.y[index]),
                    attributes = copyAttributes(
                        attributes,
                        overrides = overrides,
                        removed = setOf("id"),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            points += elementValue(child)
        }
        return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(points))
    }

    private fun drawBar(
        context: ChartContext,
        data: ChartData,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val xValues = when (
            val result = evaluateNumbers(data.x, context.location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val width = numberLike(attributes.properties["width"]) ?: run {
            if (xValues.size <= 1) {
                1.0
            } else {
                var minimum = xValues[1] - xValues[0]
                for (index in 1 until xValues.lastIndex) {
                    minimum = minOf(
                        minimum,
                        xValues[index + 1] - xValues[index],
                    )
                }
                minimum * 0.8
            }
        }
        if (!width.isFinite()) {
            return failure(
                creatorName = "chart",
                reason = "Bar width must be finite.",
                location = context.location,
            )
        }
        val horizontal = (
            attributes.properties["dir"] as?
                JessieCodeRuntimeValue.StringValue
            )?.value == "horizontal"
        val colors = colorValues(attributes, "colors")
        val labels = attributeArray(attributes, "labels")
        val polygons = mutableListOf<JessieCodeRuntimeValue>()
        for (index in data.x.indices) {
            val x0 = offset(data.x[index], -width * 0.5, context.location)
            val x1 = data.x[index]
            val x2 = offset(data.x[index], width * 0.5, context.location)
            val y = data.y[index]
            val pointValues =
                if (horizontal) {
                    listOf(
                        coordinate(number(0.0), x0),
                        coordinate(y, x0),
                        coordinate(y, x2),
                        coordinate(number(0.0), x2),
                    )
                } else {
                    listOf(
                        coordinate(x0, number(0.0)),
                        coordinate(x0, y),
                        coordinate(x2, y),
                        coordinate(x2, number(0.0)),
                    )
                }
            val overrides = linkedMapOf<String, JessieCodeRuntimeValue>()
            colors.getOrNull(index % colors.size.coerceAtLeast(1))?.let {
                overrides["fillcolor"] = string(it)
            }
            val polygon = when (
                val result = context.element(
                    creatorName = "polygon",
                    parents = pointValues,
                    attributes = copyAttributes(
                        attributes,
                        overrides = overrides,
                        removed = setOf("id"),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            polygons += elementValue(polygon)

            val label = labels?.getOrNull(index)
            if (label != null && isTextValue(label)) {
                val initialY = when (
                    val result = evaluateNumber(y, context.location)
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                val labelOverrides =
                    (
                        attributes.properties["label"] as?
                            JessieCodeRuntimeValue.ObjectValue
                        )?.properties?.toMutableMap() ?: linkedMapOf()
                labelOverrides["anchorx"] =
                    string(if (horizontal) {
                        if (initialY >= 0.0) "left" else "right"
                    } else {
                        "middle"
                    })
                labelOverrides["anchory"] =
                    string(if (horizontal) {
                        "middle"
                    } else if (initialY >= 0.0) {
                        "bottom"
                    } else {
                        "top"
                    })
                when (
                    val result = context.element(
                        creatorName = "text",
                        parents =
                            if (horizontal) {
                                listOf(y, x1, label)
                            } else {
                                listOf(x1, y, label)
                            },
                        attributes = JessieCodeRuntimeValue.ObjectValue(
                            labelOverrides,
                        ),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(JessieCodeRuntimeValue.ArrayValue(polygons))
    }

    private fun drawPie(
        context: ChartContext,
        y: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val centerValues = attributeArray(attributes, "center")
            ?: listOf(number(0.0), number(0.0))
        if (centerValues.size != 2) {
            return failure(
                creatorName = "chart",
                reason = "Pie center must contain two coordinates.",
                location = context.location,
            )
        }
        val radius = attributes.properties["radius"] ?: number(4.0)
        val labels = attributeArray(attributes, "labels").orEmpty()
        val colors = colorValues(attributes, "colors")
        val highlightColors = colorValues(
            attributes = attributes,
            name = "highlightcolors",
            fallback = emptyList(),
        )
        val hidden = objectValue(
            "fixed" to boolean(true),
            "withlabel" to boolean(false),
            "visible" to boolean(false),
            "name" to string(""),
        )
        val center = when (
            val result = context.element(
                creatorName = "point",
                parents = centerValues,
                attributes = hidden,
                trackScene = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val first = when (
            val result = context.element(
                creatorName = "point",
                parents = listOf(
                    add(radius, centerValues[0], context.location),
                    centerValues[1],
                ),
                attributes = hidden,
                trackScene = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val radialPoints = mutableListOf(first)
        val sectors = mutableListOf<JessieCodeRuntimeValue>()
        for (index in y.indices) {
            val angle = computedNumber(
                name = "chart.pie.angle",
                location = context.location,
            ) {
                var total = 0.0
                var partial = 0.0
                for ((valueIndex, value) in y.withIndex()) {
                    val evaluated = when (
                        val result = evaluateNumber(value, context.location)
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> return@computedNumber result
                    }
                    total += evaluated
                    if (valueIndex <= index) {
                        partial += evaluated
                    }
                }
                GMResult.Ok(if (total == 0.0) 0.0 else 2.0 * PI * partial / total)
            }
            val point = when (
                val result = context.element(
                    creatorName = "point",
                    parents = listOf(
                        polarCoordinate(
                            radius = radius,
                            angle = angle,
                            center = centerValues[0],
                            cosine = true,
                            location = context.location,
                        ),
                        polarCoordinate(
                            radius = radius,
                            angle = angle,
                            center = centerValues[1],
                            cosine = false,
                            location = context.location,
                        ),
                    ),
                    attributes = hidden,
                    trackScene = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            radialPoints += point
            val label = labels.getOrNull(index)
            val name = when (label) {
                is JessieCodeRuntimeValue.StringValue -> label.value
                is JessieCodeRuntimeValue.NumberValue ->
                    JsNumberFormat.compact(label.value)
                else -> ""
            }
            val overrides = linkedMapOf<String, JessieCodeRuntimeValue>(
                "name" to string(name),
                "withlabel" to boolean(name.isNotEmpty()),
                "straightfirst" to boolean(false),
                "straightlast" to boolean(false),
                "highlightonsector" to (
                    attributes.properties["highlightonsector"]
                        ?: boolean(false)
                    ),
            )
            colors.getOrNull(index % colors.size.coerceAtLeast(1))?.let {
                overrides["fillcolor"] = string(it)
                overrides["labelcolor"] = string(it)
            }
            highlightColors.getOrNull(
                index % highlightColors.size.coerceAtLeast(1),
            )?.let {
                overrides["highlightfillcolor"] = string(it)
            }
            val sector = when (
                val result = context.element(
                    creatorName = "sector",
                    parents = listOf(
                        elementValue(center),
                        elementValue(radialPoints[index]),
                        elementValue(point),
                    ),
                    attributes = copyAttributes(
                        attributes,
                        overrides = overrides,
                        removed = setOf("id"),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            sectors += elementValue(sector)
            if (name.isNotEmpty()) {
                val middleAngle = computedNumber(
                    name = "chart.pie.labelAngle",
                    location = context.location,
                ) {
                    var total = 0.0
                    var before = 0.0
                    var current = 0.0
                    for ((valueIndex, value) in y.withIndex()) {
                        val evaluated = when (
                            val result = evaluateNumber(
                                value,
                                context.location,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err ->
                                return@computedNumber result
                        }
                        total += evaluated
                        if (valueIndex < index) {
                            before += evaluated
                        } else if (valueIndex == index) {
                            current = evaluated
                        }
                    }
                    GMResult.Ok(
                        if (total == 0.0) {
                            0.0
                        } else {
                            2.0 * PI * (before + current * 0.5) / total
                        },
                    )
                }
                val labelX = polarCoordinate(
                    radius = scale(
                        radius,
                        0.5,
                        context.location,
                    ),
                    angle = middleAngle,
                    center = centerValues[0],
                    cosine = true,
                    location = context.location,
                )
                val labelY = polarCoordinate(
                    radius = scale(
                        radius,
                        0.5,
                        context.location,
                    ),
                    angle = middleAngle,
                    center = centerValues[1],
                    cosine = false,
                    location = context.location,
                )
                when (
                    val result = context.element(
                        creatorName = "text",
                        parents = listOf(
                            labelX,
                            labelY,
                            string(name),
                        ),
                        attributes = objectValue(
                            "name" to string(""),
                            "anchorx" to string("middle"),
                            "anchory" to string("middle"),
                        ),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
            }
        }
        return GMResult.Ok(
            objectValue(
                "sectors" to array(sectors),
                "points" to array(radialPoints.map(::elementValue)),
                "midpoint" to elementValue(center),
            ),
        )
    }

    private fun drawRadar(
        context: ChartContext,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): NativeChartCreatorResult {
        val rows = parents.map { parent ->
            (parent as? JessieCodeRuntimeValue.ArrayValue)?.values
                ?: return failure(
                    creatorName = "chart",
                    reason = "Radar data rows must be arrays.",
                    location = context.location,
                )
        }
        if (rows.isEmpty()) {
            return failure(
                creatorName = "chart",
                reason = "Radar chart has no data.",
                location = context.location,
            )
        }
        val parameters = textArrayAttribute(attributes, "paramarray")
            ?: return failure(
                creatorName = "chart",
                reason = "Radar chart needs paramArray.",
                location = context.location,
            )
        if (parameters.size <= 1) {
            return failure(
                creatorName = "chart",
                reason = "Radar chart needs more than one parameter.",
                location = context.location,
            )
        }
        if (rows.any { it.size != parameters.size }) {
            return failure(
                creatorName = "chart",
                reason = "Radar data length must equal paramArray length.",
                location = context.location,
            )
        }
        val evaluatedRows = mutableListOf<DoubleArray>()
        for (row in rows) {
            when (val result = evaluateNumbers(row, context.location)) {
                is GMResult.Ok -> evaluatedRows += result.value
                is GMResult.Err -> return result
            }
        }
        val minima = DoubleArray(parameters.size) { index ->
            evaluatedRows.minOf { it[index] }
        }
        val maxima = DoubleArray(parameters.size) { index ->
            evaluatedRows.maxOf { it[index] }
        }
        val startShiftRatio =
            numberLike(attributes.properties["startshiftratio"]) ?: 0.0
        val endShiftRatio =
            numberLike(attributes.properties["endshiftratio"]) ?: 0.0
        val startShifts = numberArrayAttribute(
            attributes = attributes,
            name = "startshiftarray",
        ) ?: DoubleArray(parameters.size) { index ->
            (maxima[index] - minima[index]) * startShiftRatio
        }
        val endShifts = numberArrayAttribute(
            attributes = attributes,
            name = "endshiftarray",
        ) ?: DoubleArray(parameters.size) { index ->
            (maxima[index] - minima[index]) * endShiftRatio
        }
        val starts = numberArrayAttribute(
            attributes = attributes,
            name = "startarray",
        ) ?: minima.copyOf()
        val ends = numberArrayAttribute(
            attributes = attributes,
            name = "endarray",
        ) ?: maxima.copyOf()
        numberLike(attributes.properties["start"])?.let { start ->
            starts.fill(start)
        }
        numberLike(attributes.properties["end"])?.let { end ->
            ends.fill(end)
        }
        val arrays = listOf(
            "startShiftArray" to startShifts,
            "endShiftArray" to endShifts,
            "startArray" to starts,
            "endArray" to ends,
        )
        for ((name, values) in arrays) {
            if (values.size != parameters.size) {
                return failure(
                    creatorName = "chart",
                    reason = "$name length must equal paramArray length.",
                    location = context.location,
                )
            }
        }
        val radius = numberLike(attributes.properties["radius"]) ?: 10.0
        val strokeWidth =
            numberLike(attributes.properties["strokewidth"]) ?: 1.0
        val polyStrokeWidth =
            numberLike(attributes.properties["polystrokewidth"])
                ?: strokeWidth
        val center = attributeArray(attributes, "center")
            ?: listOf(number(0.0), number(0.0))
        if (center.size != 2) {
            return failure(
                creatorName = "chart",
                reason = "Radar center must contain two coordinates.",
                location = context.location,
            )
        }
        val centerNumbers = when (
            val result = evaluateNumbers(center, context.location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val startAngle =
            numberLike(attributes.properties["startangle"]) ?: 0.0
        val hidden = objectValue(
            "name" to string(""),
            "fixed" to boolean(true),
            "withlabel" to boolean(false),
            "visible" to boolean(false),
        )
        val midpoint = when (
            val result = context.element(
                creatorName = "point",
                parents = center,
                attributes = hidden,
                trackScene = false,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val axisLines = mutableListOf<JessieCodeRuntimeValue>()
        val dataPoints = MutableList(rows.size) {
            mutableListOf<JessieCodeRuntimeValue>()
        }
        for (parameterIndex in parameters.indices) {
            val angle =
                startAngle +
                    2.0 * PI * (parameterIndex + 1) / parameters.size
            val axisPoint = when (
                val result = context.element(
                    creatorName = "point",
                    parents = listOf(
                        number(radius * cos(angle) + centerNumbers[0]),
                        number(radius * sin(angle) + centerNumbers[1]),
                    ),
                    attributes = hidden,
                    trackScene = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val lineAttributes = copyAttributes(
                attributes,
                overrides = mapOf(
                    "name" to string(parameters[parameterIndex]),
                    "strokewidth" to number(strokeWidth),
                    "straightfirst" to boolean(false),
                    "straightlast" to boolean(false),
                    "withlabel" to boolean(true),
                ),
                removed = setOf("id"),
            )
            val line = when (
                val result = context.element(
                    creatorName = "line",
                    parents = listOf(
                        elementValue(midpoint),
                        elementValue(axisPoint),
                    ),
                    attributes = lineAttributes,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            axisLines += elementValue(line)
            when (
                val result = context.element(
                    creatorName = "text",
                    parents = listOf(
                        number(radius * cos(angle) + centerNumbers[0]),
                        number(radius * sin(angle) + centerNumbers[1]),
                        string(parameters[parameterIndex]),
                    ),
                    attributes = objectValue(
                        "name" to string(""),
                        "anchorx" to string("middle"),
                        "anchory" to string("middle"),
                    ),
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val denominator =
                ends[parameterIndex] +
                    endShifts[parameterIndex] -
                    (starts[parameterIndex] - startShifts[parameterIndex])
            if (!denominator.isFinite() || denominator == 0.0) {
                return failure(
                    creatorName = "chart",
                    reason = "Radar axis range must be finite and non-zero.",
                    location = context.location,
                )
            }
            for (rowIndex in rows.indices) {
                val radial = computedNumber(
                    name = "chart.radar.radius",
                    location = context.location,
                ) {
                    when (
                        val result = evaluateNumber(
                            rows[rowIndex][parameterIndex],
                            context.location,
                        )
                    ) {
                        is GMResult.Ok -> GMResult.Ok(
                            (
                                result.value -
                                    (
                                        starts[parameterIndex] -
                                            startShifts[parameterIndex]
                                        )
                                ) * radius / denominator,
                        )
                        is GMResult.Err -> result
                    }
                }
                val point = when (
                    val result = context.element(
                        creatorName = "point",
                        parents = listOf(
                            polarCoordinate(
                                radius = radial,
                                angle = number(angle),
                                center = center[0],
                                cosine = true,
                                location = context.location,
                            ),
                            polarCoordinate(
                                radius = radial,
                                angle = number(angle),
                                center = center[1],
                                cosine = false,
                                location = context.location,
                            ),
                        ),
                        attributes = hidden,
                        trackScene = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                dataPoints[rowIndex] += elementValue(point)
            }
        }
        val colors = colorValues(attributes, "colors")
        val polygons = mutableListOf<JessieCodeRuntimeValue>()
        for (rowIndex in dataPoints.indices) {
            val color = colors.getOrNull(
                rowIndex % colors.size.coerceAtLeast(1),
            )
            val overrides = linkedMapOf<String, JessieCodeRuntimeValue>(
                "withlines" to boolean(true),
                "withlabel" to boolean(false),
                "fillopacity" to (
                    attributes.properties["fillopacity"] ?: number(0.4)
                    ),
                "polystrokewidth" to number(polyStrokeWidth),
            )
            color?.let {
                overrides["fillcolor"] = string(it)
                overrides["strokecolor"] = string(it)
                overrides["labelcolor"] = string(it)
            }
            val polygon = when (
                val result = context.element(
                    creatorName = "polygon",
                    parents = dataPoints[rowIndex],
                    attributes = copyAttributes(
                        attributes,
                        overrides = overrides,
                        removed = setOf("id"),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            polygons += elementValue(polygon)
        }
        val circles = mutableListOf<JessieCodeRuntimeValue>()
        if (booleanLike(attributes.properties["showcircles"]) == true) {
            val labels = attributeArray(attributes, "circlelabelarray")
                ?: listOf(0, 20, 40, 60, 80, 100).map {
                    number(it.toDouble())
                }
            if (labels.size < 2) {
                return failure(
                    creatorName = "chart",
                    reason = "Radar circleLabelArray needs at least two values.",
                    location = context.location,
                )
            }
            val circleAngle = startAngle + PI / parameters.size
            val delta = (ends[0] - starts[0]) / (labels.size - 1)
            val denominator =
                ends[0] + endShifts[0] - (starts[0] - startShifts[0])
            for (index in labels.indices) {
                val rawRadius = starts[0] + index * delta
                val scaledRadius =
                    (
                        rawRadius -
                            (starts[0] - startShifts[0])
                        ) * radius / denominator
                val label = textValue(labels[index]) ?: ""
                val circlePoint = when (
                    val result = context.element(
                        creatorName = "point",
                        parents = listOf(
                            number(
                                scaledRadius * cos(circleAngle) +
                                    centerNumbers[0],
                            ),
                            number(
                                scaledRadius * sin(circleAngle) +
                                    centerNumbers[1],
                            ),
                        ),
                        attributes = objectValue(
                            "name" to string(""),
                            "size" to number(0.0),
                            "fixed" to boolean(true),
                            "withlabel" to boolean(false),
                            "visible" to boolean(false),
                        ),
                        trackScene = false,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                when (
                    val result = context.element(
                        creatorName = "text",
                        parents = listOf(
                            number(
                                scaledRadius * cos(circleAngle) +
                                    centerNumbers[0],
                            ),
                            number(
                                scaledRadius * sin(circleAngle) +
                                    centerNumbers[1],
                            ),
                            string(label),
                        ),
                        attributes = objectValue(
                            "name" to string(""),
                            "anchorx" to string("middle"),
                            "anchory" to string("middle"),
                        ),
                    )
                ) {
                    is GMResult.Ok -> Unit
                    is GMResult.Err -> return result
                }
                val circle = when (
                    val result = context.element(
                        creatorName = "circle",
                        parents = listOf(
                            elementValue(midpoint),
                            elementValue(circlePoint),
                        ),
                        attributes = copyAttributes(
                            attributes,
                            overrides = mapOf(
                                "fillcolor" to string("none"),
                                "highlightfillcolor" to string("none"),
                                "strokewidth" to number(
                                    numberLike(
                                        attributes.properties[
                                            "circlestrokewidth"
                                        ],
                                    ) ?: 0.5,
                                ),
                            ),
                            removed = setOf("id"),
                        ),
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return result
                }
                circles += elementValue(circle)
            }
        }
        // createChart's option filter drops the undocumented radar
        // legendPosition fields in JSXGraph 1.13.3. Direct Legend creation
        // remains supported below.
        return GMResult.Ok(
            objectValue(
                "circles" to array(circles),
                "lines" to array(axisLines),
                "points" to array(
                    dataPoints.map { row -> array(row) },
                ),
                "midpoint" to elementValue(midpoint),
                "polygons" to array(polygons),
            ),
        )
    }

    private fun createLegendInternal(
        board: Board,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): NativeChartCreatorResult {
        val context = ChartContext(board, location)
        return when (
            val result = createLegendInContext(
                context = context,
                parents = parents,
                attributes = attributes,
            )
        ) {
            is GMResult.Err -> result
            is GMResult.Ok -> GMResult.Ok(
                JessieCodeRuntimeValue.ElementReference(
                    element = result.value,
                    createdSceneElements =
                        context.sceneElements.toList(),
                    isSceneComposite = true,
                ),
            )
        }
    }

    private fun createLegendInContext(
        context: ChartContext,
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): GMResult<Legend, JessieCodeRuntimeError> {
        if (parents.size != 2) {
            return failure(
                creatorName = "legend",
                reason = "Legend needs two coordinates.",
                location = context.location,
            )
        }
        val coordinates = when (
            val result = evaluateNumbers(parents, context.location)
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val style = (
            attributes.properties["legendstyle"]
                ?: attributes.properties["style"]
            )?.let {
                (it as? JessieCodeRuntimeValue.StringValue)?.value
            } ?: "vertical"
        if (style != "vertical") {
            return failure(
                creatorName = "legend",
                reason = "Unknown legend style: $style.",
                location = context.location,
            )
        }
        val labels = attributeArray(attributes, "labelarray")
            ?: attributeArray(attributes, "labels")
            ?: (1..8).map { string(it.toString()) }
        val colors = colorValues(
            attributes = attributes,
            name = if ("colorarray" in attributes.properties) {
                "colorarray"
            } else {
                "colors"
            },
        )
        val opacities = numberArrayAttribute(
            attributes = attributes,
            name = "strokeopacity",
        ) ?: doubleArrayOf(1.0)
        if (colors.isEmpty() || opacities.isEmpty()) {
            return failure(
                creatorName = "legend",
                reason = "Legend colors and strokeOpacity must not be empty.",
                location = context.location,
            )
        }
        val lineLength =
            numberLike(attributes.properties["linelength"]) ?: 1.0
        val rowHeight =
            numberLike(attributes.properties["rowheight"]) ?: 20.0
        val strokeWidth =
            numberLike(attributes.properties["strokewidth"]) ?: 5.0
        val lines = mutableListOf<GeometryElement>()
        for (index in labels.indices) {
            val label = textValue(labels[index]) ?: ""
            val color = colors[index % colors.size]
            val opacity = opacities[index % opacities.size]
            val y = coordinates[1] - index * rowHeight / context.board.unitY
            val line = when (
                val result = context.element(
                    creatorName = "line",
                    parents = listOf(
                        coordinate(number(coordinates[0]), number(y)),
                        coordinate(
                            number(coordinates[0] + lineLength),
                            number(y),
                        ),
                    ),
                    attributes = objectValue(
                        "name" to string(label),
                        "strokecolor" to string(color),
                        "highlightstrokecolor" to string(color),
                        "strokeopacity" to number(opacity),
                        "highlightstrokeopacity" to number(opacity),
                        "strokewidth" to number(strokeWidth),
                        "straightfirst" to boolean(false),
                        "straightlast" to boolean(false),
                        "withlabel" to boolean(true),
                        "fixed" to boolean(true),
                        "frozen" to (
                            attributes.properties["frozen"]
                                ?: boolean(false)
                            ),
                        "label" to objectValue(
                            "offset" to array(
                                listOf(number(10.0), number(0.0)),
                            ),
                            "strokecolor" to string(color),
                            "strokewidth" to number(strokeWidth),
                        ),
                    ),
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            lines += line.element
            when (
                val result = context.element(
                    creatorName = "text",
                    parents = listOf(
                        number(
                            coordinates[0] +
                                lineLength +
                                10.0 / context.board.unitX,
                        ),
                        number(y),
                        string(label),
                    ),
                    attributes = objectValue(
                        "name" to string(""),
                        "strokecolor" to string(color),
                        "anchorx" to string("left"),
                        "anchory" to string("middle"),
                    ),
                )
            ) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
        }
        val identity = when (
            val result = identity(
                creatorName = "legend",
                attributes = attributes,
                location = context.location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        return when (
            val result = Legend.create(
                board = context.board,
                lines = lines.filterIsInstance<com.swithun.jsxgraph.core.base.Line>(),
                id = identity.id,
                name = identity.name,
                needsRegularUpdate = identity.needsRegularUpdate,
            )
        ) {
            is GMResult.Ok -> result
            is GMResult.Err -> failure(
                creatorName = "legend",
                reason = result.error.toString(),
                location = context.location,
            )
        }
    }

    private fun chartData(
        parents: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<ChartData, JessieCodeRuntimeError> {
        val first = parents.firstOrNull()
        return when {
            first is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(
                ChartData(
                    x = parents.indices.map { number((it + 1).toDouble()) },
                    y = parents,
                ),
            )
            parents.size == 1 &&
                first is JessieCodeRuntimeValue.ArrayValue -> GMResult.Ok(
                ChartData(
                    x = first.values.indices.map {
                        number((it + 1).toDouble())
                    },
                    y = first.values.toList(),
                ),
            )
            parents.size == 2 &&
                parents[0] is JessieCodeRuntimeValue.ArrayValue &&
                parents[1] is JessieCodeRuntimeValue.ArrayValue -> {
                val x = (
                    parents[0] as JessieCodeRuntimeValue.ArrayValue
                    ).values
                val y = (
                    parents[1] as JessieCodeRuntimeValue.ArrayValue
                    ).values
                val size = minOf(x.size, y.size)
                GMResult.Ok(
                    ChartData(
                        x = x.take(size),
                        y = y.take(size),
                    ),
                )
            }
            else -> failure(
                creatorName = "chart",
                reason = "Unsupported chart parent structure.",
                location = location,
            )
        }
    }

    private fun effectiveChartAttributes(
        source: JessieCodeRuntimeValue.ObjectValue,
    ): JessieCodeRuntimeValue.ObjectValue {
        val values = linkedMapOf<String, JessieCodeRuntimeValue>(
            "chartstyle" to string("line"),
            "colors" to array(defaultColors.map(::string)),
            "highlightcolors" to JessieCodeRuntimeValue.NullValue,
            "fillopacity" to number(0.6),
            "withlines" to boolean(false),
            "label" to objectValue(),
        )
        values.putAll(source.properties)
        return JessieCodeRuntimeValue.ObjectValue(values)
    }

    private fun chartSceneElementCount(
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int? {
        val style = when (
            val value = attributes.properties["chartstyle"]
        ) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> "line"
            is JessieCodeRuntimeValue.StringValue -> value.value
            else -> return null
        }
        val dataSize = chartDataSize(parents)
        var count = 0L
        for (part in style.replace(" ", "").split(",")) {
            val partCount = when (part) {
                "line", "fit", "spline" -> {
                    if (dataSize == null) return null
                    1L
                }
                "point" -> dataSize?.toLong() ?: return null
                "bar", "pie" -> {
                    val size = dataSize ?: return null
                    size.toLong() +
                        visibleLabelCount(attributes, size).toLong()
                }
                "radar" ->
                    radarSceneElementCount(parents, attributes)
                        ?.toLong() ?: return null
                else -> 0L
            }
            count = (count + partCount)
                .coerceAtMost(Int.MAX_VALUE.toLong())
        }
        return count.toInt()
    }

    private fun chartDataSize(
        parents: List<JessieCodeRuntimeValue>,
    ): Int? {
        val first = parents.firstOrNull()
        return when {
            first is JessieCodeRuntimeValue.NumberValue -> parents.size
            parents.size == 1 &&
                first is JessieCodeRuntimeValue.ArrayValue ->
                first.values.size
            parents.size == 2 &&
                parents[0] is JessieCodeRuntimeValue.ArrayValue &&
                parents[1] is JessieCodeRuntimeValue.ArrayValue ->
                minOf(
                    (
                        parents[0] as JessieCodeRuntimeValue.ArrayValue
                        ).values.size,
                    (
                        parents[1] as JessieCodeRuntimeValue.ArrayValue
                        ).values.size,
                )
            else -> null
        }
    }

    private fun visibleLabelCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        dataSize: Int,
    ): Int =
        attributeArray(attributes, "labels")
            ?.take(dataSize)
            ?.count(::isTextValue) ?: 0

    private fun radarSceneElementCount(
        parents: List<JessieCodeRuntimeValue>,
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int? {
        if (
            parents.isEmpty() ||
            parents.any { it !is JessieCodeRuntimeValue.ArrayValue }
        ) {
            return null
        }
        val parameterCount =
            textArrayAttribute(attributes, "paramarray")?.size
                ?: return null
        val circleCount =
            if (booleanLike(attributes.properties["showcircles"]) == true) {
                attributeArray(attributes, "circlelabelarray")?.size ?: 6
            } else {
                0
            }
        return (
            2L * parameterCount +
                parents.size +
                2L * circleCount
            ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private fun legendSceneElementCount(
        attributes: JessieCodeRuntimeValue.ObjectValue,
    ): Int {
        val labelCount =
            (
                attributeArray(attributes, "labelarray")
                    ?: attributeArray(attributes, "labels")
                )?.size ?: 8
        return (2L * labelCount)
            .coerceAtMost(Int.MAX_VALUE.toLong())
            .toInt()
    }

    private fun colorValues(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        fallback: List<String> = defaultColors,
    ): List<String> {
        val values = attributeArray(attributes, name) ?: return fallback
        return values.mapNotNull { value ->
            (value as? JessieCodeRuntimeValue.StringValue)?.value
        }.ifEmpty { fallback }
    }

    private fun textArrayAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
    ): List<String>? =
        attributeArray(attributes, name)?.map { value ->
            textValue(value) ?: return null
        }

    private fun numberArrayAttribute(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
    ): DoubleArray? {
        val values = attributeArray(attributes, name) ?: return null
        val numbers = DoubleArray(values.size)
        for ((index, value) in values.withIndex()) {
            numbers[index] = numberLike(value) ?: return null
        }
        return numbers
    }

    private fun attributeArray(
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
    ): List<JessieCodeRuntimeValue>? =
        (
            attributes.properties[name] as?
                JessieCodeRuntimeValue.ArrayValue
            )?.values

    private fun identity(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        location: JessieCodeAstLocation,
    ): GMResult<ChartIdentity, JessieCodeRuntimeError> {
        val id = when (
            val result = stringAttribute(
                creatorName = creatorName,
                attributes = attributes,
                name = "id",
                default = "",
                location = location,
            )
        ) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        val nameValue = attributes.properties["name"]
        val name = when (nameValue) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> null
            is JessieCodeRuntimeValue.StringValue -> nameValue.value
            else -> return failure(
                creatorName = creatorName,
                reason = "Attribute 'name' must be a string.",
                location = location,
            )
        }
        val needsRegularUpdate = when (
            val value = attributes.properties["needsregularupdate"]
        ) {
            null,
            JessieCodeRuntimeValue.UndefinedValue,
            -> true
            is JessieCodeRuntimeValue.BooleanValue -> value.value
            else -> return failure(
                creatorName = creatorName,
                reason = "Attribute 'needsRegularUpdate' must be boolean.",
                location = location,
            )
        }
        return GMResult.Ok(
            ChartIdentity(
                id = id,
                name = name,
                needsRegularUpdate = needsRegularUpdate,
            ),
        )
    }

    private fun stringAttribute(
        creatorName: String,
        attributes: JessieCodeRuntimeValue.ObjectValue,
        name: String,
        default: String,
        location: JessieCodeAstLocation,
    ): GMResult<String, JessieCodeRuntimeError> {
        val value = attributes.properties[name]
            ?: return GMResult.Ok(default)
        if (value === JessieCodeRuntimeValue.UndefinedValue) {
            return GMResult.Ok(default)
        }
        return if (value is JessieCodeRuntimeValue.StringValue) {
            GMResult.Ok(value.value)
        } else {
            failure(
                creatorName = creatorName,
                reason = "Attribute '$name' must be a string.",
                location = location,
            )
        }
    }

    private fun evaluateNumbers(
        values: List<JessieCodeRuntimeValue>,
        location: JessieCodeAstLocation,
    ): GMResult<DoubleArray, JessieCodeRuntimeError> {
        val output = DoubleArray(values.size)
        for ((index, value) in values.withIndex()) {
            when (val result = evaluateNumber(value, location)) {
                is GMResult.Ok -> output[index] = result.value
                is GMResult.Err -> return result
            }
        }
        return GMResult.Ok(output)
    }

    private fun evaluateNumber(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> GMResult.Ok(value.value)
            is JessieCodeRuntimeValue.StringValue ->
                value.value.toDoubleOrNull()?.let { GMResult.Ok(it) }
                    ?: numericFailure(value, location)
            is JessieCodeRuntimeValue.FunctionValue ->
                when (
                    val result = value.externalCallable.call(
                        arguments = emptyList(),
                        location = location,
                    )
                ) {
                    is GMResult.Ok -> evaluateNumber(result.value, location)
                    is GMResult.Err -> result
                }
            else -> numericFailure(value, location)
        }

    private fun numericFailure(
        value: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): GMResult<Double, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.BuiltInInvocationFailure(
                functionName = "chart",
                reason = "Expected numeric chart value, got ${typeName(value)}.",
                location = location,
            ),
        )

    private fun offset(
        source: JessieCodeRuntimeValue,
        delta: Double,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeValue =
        if (source is JessieCodeRuntimeValue.NumberValue) {
            number(source.value + delta)
        } else {
            computedNumber("chart.offset", location) {
                when (val result = evaluateNumber(source, location)) {
                    is GMResult.Ok -> GMResult.Ok(result.value + delta)
                    is GMResult.Err -> result
                }
            }
        }

    private fun add(
        first: JessieCodeRuntimeValue,
        second: JessieCodeRuntimeValue,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeValue =
        if (
            first is JessieCodeRuntimeValue.NumberValue &&
            second is JessieCodeRuntimeValue.NumberValue
        ) {
            number(first.value + second.value)
        } else {
            computedNumber("chart.add", location) {
                val left = when (val result = evaluateNumber(first, location)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> return@computedNumber result
                }
                when (val result = evaluateNumber(second, location)) {
                    is GMResult.Ok -> GMResult.Ok(left + result.value)
                    is GMResult.Err -> result
                }
            }
        }

    private fun scale(
        source: JessieCodeRuntimeValue,
        factor: Double,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeValue =
        if (source is JessieCodeRuntimeValue.NumberValue) {
            number(source.value * factor)
        } else {
            computedNumber("chart.scale", location) {
                when (val result = evaluateNumber(source, location)) {
                    is GMResult.Ok -> GMResult.Ok(result.value * factor)
                    is GMResult.Err -> result
                }
            }
        }

    private fun polarCoordinate(
        radius: JessieCodeRuntimeValue,
        angle: JessieCodeRuntimeValue,
        center: JessieCodeRuntimeValue,
        cosine: Boolean,
        location: JessieCodeAstLocation,
    ): JessieCodeRuntimeValue =
        computedNumber("chart.polarCoordinate", location) {
            val radiusValue = when (
                val result = evaluateNumber(radius, location)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@computedNumber result
            }
            val angleValue = when (
                val result = evaluateNumber(angle, location)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@computedNumber result
            }
            val centerValue = when (
                val result = evaluateNumber(center, location)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return@computedNumber result
            }
            GMResult.Ok(
                radiusValue *
                    (
                        if (cosine) {
                            cos(angleValue)
                        } else {
                            sin(angleValue)
                        }
                        ) +
                    centerValue,
            )
        }

    private fun computedNumber(
        name: String,
        location: JessieCodeAstLocation,
        evaluate: () -> GMResult<Double, JessieCodeRuntimeError>,
    ): JessieCodeRuntimeValue.FunctionValue =
        JessieCodeRuntimeValue.FunctionValue(
            name = name,
            callable = JessieCodeCallable { _, _ ->
                when (val result = evaluate()) {
                    is GMResult.Ok ->
                        GMResult.Ok(number(result.value))
                    is GMResult.Err -> result
                }
            },
        )

    private fun copyAttributes(
        source: JessieCodeRuntimeValue.ObjectValue,
        overrides: Map<String, JessieCodeRuntimeValue> = emptyMap(),
        removed: Set<String> = emptySet(),
    ): JessieCodeRuntimeValue.ObjectValue {
        val values = source.properties
            .filterKeys { it !in removed }
            .toMutableMap()
        values.putAll(overrides)
        return JessieCodeRuntimeValue.ObjectValue(values)
    }

    private fun sceneAttributes(
        creatorName: String,
        source: JessieCodeRuntimeValue.ObjectValue,
        element: GeometryElement,
    ): JessieCodeRuntimeValue.ObjectValue {
        val supported =
            commonSceneAttributes +
                sceneAttributesByCreator[creatorName].orEmpty()
        val values = source.properties
            .filterKeys(supported::contains)
            .toMutableMap()
        if (
            element.name.isNotEmpty() &&
            creatorName in setOf("point", "line", "sector")
        ) {
            values["withlabel"] = boolean(false)
        }
        return JessieCodeRuntimeValue.ObjectValue(
            values,
        )
    }

    private fun removeNewObjects(
        board: Board,
        initialObjects: Set<GeometryElement>,
    ) {
        board.removeObjects(
            board.objectsList
                .filter { it !in initialObjects }
                .asReversed(),
        )
    }

    private fun <T> failure(
        creatorName: String,
        reason: String,
        location: JessieCodeAstLocation,
    ): GMResult<T, JessieCodeRuntimeError> =
        GMResult.Err(
            JessieCodeRuntimeError.CreatorFailure(
                creatorName = creatorName,
                error =
                    if (creatorName == "legend") {
                        JessieCodeCreatorError.LegendFactory(reason)
                    } else {
                        JessieCodeCreatorError.ChartFactory(reason)
                    },
                location = location,
            ),
        )

    private fun elementValue(
        value: JessieCodeRuntimeValue.ElementReference,
    ): JessieCodeRuntimeValue = value

    private fun number(value: Double): JessieCodeRuntimeValue.NumberValue =
        JessieCodeRuntimeValue.NumberValue(value)

    private fun boolean(value: Boolean): JessieCodeRuntimeValue.BooleanValue =
        JessieCodeRuntimeValue.BooleanValue(value)

    private fun string(value: String): JessieCodeRuntimeValue.StringValue =
        JessieCodeRuntimeValue.StringValue(value)

    private fun array(
        values: List<JessieCodeRuntimeValue>,
    ): JessieCodeRuntimeValue.ArrayValue =
        JessieCodeRuntimeValue.ArrayValue(values)

    private fun coordinate(
        x: JessieCodeRuntimeValue,
        y: JessieCodeRuntimeValue,
    ): JessieCodeRuntimeValue.ArrayValue = array(listOf(x, y))

    private fun objectValue(
        vararg entries: Pair<String, JessieCodeRuntimeValue>,
    ): JessieCodeRuntimeValue.ObjectValue =
        JessieCodeRuntimeValue.ObjectValue(linkedMapOf(*entries))

    private fun isTextValue(value: JessieCodeRuntimeValue): Boolean =
        value is JessieCodeRuntimeValue.StringValue ||
            value is JessieCodeRuntimeValue.NumberValue

    private fun textValue(value: JessieCodeRuntimeValue): String? =
        when (value) {
            is JessieCodeRuntimeValue.StringValue -> value.value
            is JessieCodeRuntimeValue.NumberValue ->
                JsNumberFormat.compact(value.value)
            else -> null
        }

    private fun numberLike(value: JessieCodeRuntimeValue?): Double? =
        when (value) {
            is JessieCodeRuntimeValue.NumberValue -> value.value
            is JessieCodeRuntimeValue.StringValue ->
                value.value.toDoubleOrNull()
            else -> null
        }

    private fun booleanLike(value: JessieCodeRuntimeValue?): Boolean? =
        (value as? JessieCodeRuntimeValue.BooleanValue)?.value

    private fun typeName(value: JessieCodeRuntimeValue): String =
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
            is JessieCodeRuntimeValue.CompositionReference -> "composition"
            is JessieCodeRuntimeValue.ElementReference -> "element"
        }

    private data class ChartIdentity(
        val id: String,
        val name: String?,
        val needsRegularUpdate: Boolean,
    )

    private data class ChartData(
        val x: List<JessieCodeRuntimeValue>,
        val y: List<JessieCodeRuntimeValue>,
    )

    private class ChartContext(
        val board: Board,
        val location: JessieCodeAstLocation,
    ) {
        val sceneElements =
            mutableListOf<JessieCodeCreatedSceneElement>()

        fun element(
            creatorName: String,
            parents: List<JessieCodeRuntimeValue>,
            attributes: JessieCodeRuntimeValue.ObjectValue,
            trackScene: Boolean = true,
        ): GMResult<
            JessieCodeRuntimeValue.ElementReference,
            JessieCodeRuntimeError,
            > {
            val creator = NativeJessieCodeCreators.creator(creatorName)
                ?: return GMResult.Err(
                    JessieCodeRuntimeError.InvalidAst(
                        reason =
                            "Native chart dependency '$creatorName' " +
                                "is unavailable.",
                        location = location,
                    ),
                )
            return when (
                val result = creator.create(
                    board = board,
                    parents = parents,
                    attributes = attributes,
                    location = location,
                )
            ) {
                is GMResult.Err -> result
                is GMResult.Ok -> {
                    val reference = result.value as?
                        JessieCodeRuntimeValue.ElementReference
                        ?: return GMResult.Err(
                            JessieCodeRuntimeError.InvalidAst(
                                reason =
                                    "Native chart dependency '$creatorName' " +
                                        "did not return an element.",
                                location = location,
                            ),
                        )
                    if (trackScene) {
                        sceneElements += JessieCodeCreatedSceneElement(
                            element = reference.element,
                            creatorName = creatorName,
                            attributes = sceneAttributes(
                                creatorName = creatorName,
                                source = attributes,
                                element = reference.element,
                            ),
                        )
                    }
                    GMResult.Ok(reference)
                }
            }
        }
    }
}

private typealias NativeChartCreatorResult =
    GMResult<JessieCodeRuntimeValue, JessieCodeRuntimeError>

private fun <
    T : JessieCodeRuntimeValue,
    R : JessieCodeRuntimeValue,
    > GMResult<T, JessieCodeRuntimeError>.mapValue(
    transform: (T) -> R,
): GMResult<R, JessieCodeRuntimeError> =
    when (this) {
        is GMResult.Ok -> GMResult.Ok(transform(value))
        is GMResult.Err -> this
    }
