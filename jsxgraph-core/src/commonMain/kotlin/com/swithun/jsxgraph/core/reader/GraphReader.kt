/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/graph.js -> JXG.GraphReader.parseData.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BoardError
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.LineError
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.PointError
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import com.swithun.jsxgraph.core.math.RandomSource
import com.swithun.jsxgraph.core.utils.JsNumberFormat
import kotlin.random.Random

internal data class GraphReaderLimits(
    val maxSourceCharacters: Int = 16 * 1024 * 1024,
    val maxLines: Int = 1_000_000,
    val maxNodes: Int = 10_000,
    val maxEdges: Int = 1_000_000,
    val maxMatrixCells: Long = 4_000_000,
)

internal sealed interface GraphReaderError : ReaderDomainError {
    data class InvalidLimits(
        val name: String,
        val value: Long,
    ) : GraphReaderError

    data class SourceLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : GraphReaderError

    data class LineLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : GraphReaderError

    data class InvalidNodeCount(
        val line: Int,
        val value: String?,
    ) : GraphReaderError

    data class NodeLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GraphReaderError

    data class MatrixLimitExceeded(
        val limit: Long,
        val requested: Long,
    ) : GraphReaderError

    data class EdgeLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : GraphReaderError

    data class MissingNodeLine(
        val nodeIndex: Int,
        val line: Int,
    ) : GraphReaderError

    data class UnknownNode(
        val line: Int,
        val name: String,
    ) : GraphReaderError

    data class InvalidBoundingBox(
        val values: List<Double>,
    ) : GraphReaderError

    data class RandomCoordinateFailed(
        val nodeIndex: Int,
        val axis: Int,
        val message: String?,
    ) : GraphReaderError

    data class BoundingBoxUpdateFailed(
        val cause: BoardError,
    ) : GraphReaderError

    data class PointCreationFailed(
        val nodeIndex: Int,
        val cause: PointError,
    ) : GraphReaderError

    data class SegmentCreationFailed(
        val sourceNodeIndex: Int,
        val targetNodeIndex: Int,
        val cause: LineError,
    ) : GraphReaderError

    data class WeightTextCreationFailed(
        val sourceNodeIndex: Int,
        val targetNodeIndex: Int,
        val cause: TextError,
    ) : GraphReaderError
}

internal data class GraphNode(
    val name: String,
    val coords: List<Double?>,
)

internal data class GraphData(
    val n: Int,
    val nodes: List<GraphNode>,
    val adjMatrix: List<List<Double>>,
    val nodenumbers: Map<String, Int>,
    val weighted: Boolean,
    val directed: Boolean,
)

internal data class ParsedGraph(
    val boundingBox: List<Double>,
    val graph: GraphData,
)

internal data class DrawnGraphNode(
    val source: GraphNode,
    val reference: Point,
)

internal data class DrawnGraphSegment(
    val edge: Line,
    val weight: Double,
    val weightLabel: Text? = null,
)

internal data class DrawnGraph(
    val graph: GraphData,
    val nodes: List<DrawnGraphNode>,
    val segments: List<List<DrawnGraphSegment?>>,
)

/**
 * Translated Graph reader slice.
 *
 * Parsing and Board creation follow the upstream reader, including directed
 * edge arrows and Line-anchored weight labels.
 */
internal class GraphReader(
    private val data: String,
) {
    // JSXGraph 1.13.3: src/reader/graph.js -> GraphReader.parseData.
    @Suppress("UNUSED_PARAMETER")
    internal fun parseData(
        directed: Boolean = false,
        limits: GraphReaderLimits = GraphReaderLimits(),
    ): GMResult<ParsedGraph, GraphReaderError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        if (data.length > limits.maxSourceCharacters) {
            return GMResult.Err(
                GraphReaderError.SourceLimitExceeded(
                    limit = limits.maxSourceCharacters,
                    actual = data.length,
                ),
            )
        }

        val lines = splitPreservingEmpty(data, '\n').map(String::trim)
        if (lines.size > limits.maxLines) {
            return GMResult.Err(
                GraphReaderError.LineLimitExceeded(
                    limit = limits.maxLines,
                    actual = lines.size,
                ),
            )
        }

        val boundingBox = splitPreservingEmpty(
            lines.firstOrNull().orEmpty(),
            ' ',
        ).map(::jsParseInt)
        val directedFromSource = lines.getOrNull(1) == "digraph"
        val nodeCountToken = lines.getOrNull(2)
        val nodeCountValue = jsParseInt(nodeCountToken)
        if (
            !nodeCountValue.isFinite() ||
            nodeCountValue < 0.0 ||
            nodeCountValue > Int.MAX_VALUE.toDouble()
        ) {
            return GMResult.Err(
                GraphReaderError.InvalidNodeCount(
                    line = NODE_COUNT_LINE,
                    value = nodeCountToken,
                ),
            )
        }
        val nodeCount = nodeCountValue.toInt()
        if (nodeCount > limits.maxNodes) {
            return GMResult.Err(
                GraphReaderError.NodeLimitExceeded(
                    limit = limits.maxNodes,
                    requested = nodeCount,
                ),
            )
        }
        val matrixCells = nodeCount.toLong() * nodeCount.toLong()
        if (matrixCells > limits.maxMatrixCells) {
            return GMResult.Err(
                GraphReaderError.MatrixLimitExceeded(
                    limit = limits.maxMatrixCells,
                    requested = matrixCells,
                ),
            )
        }

        val nodes = ArrayList<GraphNode>(nodeCount)
        val nodeNumbers = linkedMapOf<String, Int>()
        for (nodeIndex in 0 until nodeCount) {
            val sourceIndex = FIRST_NODE_INDEX + nodeIndex
            val line = lines.getOrNull(sourceIndex)
                ?: return GMResult.Err(
                    GraphReaderError.MissingNodeLine(
                        nodeIndex = nodeIndex,
                        line = sourceIndex + 1,
                    ),
                )
            val node = if (' ' in line) {
                val parts = splitPreservingEmpty(line, ' ')
                GraphNode(
                    name = parts.firstOrNull().orEmpty(),
                    coords = listOf(
                        jsParseInt(parts.getOrNull(1)),
                        jsParseInt(parts.getOrNull(2)),
                    ),
                )
            } else {
                GraphNode(
                    name = line,
                    coords = listOf(null, null),
                )
            }
            nodes += node
            nodeNumbers[node.name] = nodeIndex
        }

        val firstEdgeIndex = FIRST_NODE_INDEX + nodeCount
        val edgeLines =
            if (firstEdgeIndex < lines.size) {
                lines.subList(firstEdgeIndex, lines.size)
            } else {
                emptyList()
            }
        if (edgeLines.size > limits.maxEdges) {
            return GMResult.Err(
                GraphReaderError.EdgeLimitExceeded(
                    limit = limits.maxEdges,
                    requested = edgeLines.size,
                ),
            )
        }
        val splitEdges = edgeLines.map { line ->
            splitPreservingEmpty(line, ' ')
        }
        val weighted = splitEdges.any { parts -> parts.size > 2 }
        val adjacencyMatrix = MutableList(nodeCount) { row ->
            MutableList(nodeCount) { column ->
                when {
                    !weighted -> 0.0
                    row == column -> 0.0
                    else -> Double.POSITIVE_INFINITY
                }
            }
        }

        for ((edgeIndex, parts) in splitEdges.withIndex()) {
            val sourceName = parts.firstOrNull().orEmpty()
            val targetName = parts.getOrNull(1) ?: UNDEFINED_TOKEN
            val sourceNode = nodeNumbers[sourceName]
                ?: return GMResult.Err(
                    GraphReaderError.UnknownNode(
                        line = firstEdgeIndex + edgeIndex + 1,
                        name = sourceName,
                    ),
                )
            val targetNode = nodeNumbers[targetName]
                ?: return GMResult.Err(
                    GraphReaderError.UnknownNode(
                        line = firstEdgeIndex + edgeIndex + 1,
                        name = targetName,
                    ),
                )
            val weight =
                if (parts.size > 2) {
                    jsParseInt(parts[2])
                } else {
                    1.0
                }
            adjacencyMatrix[sourceNode][targetNode] = weight
            if (!directedFromSource) {
                adjacencyMatrix[targetNode][sourceNode] = weight
            }
        }

        return GMResult.Ok(
            ParsedGraph(
                boundingBox = boundingBox,
                graph = GraphData(
                    n = nodeCount,
                    nodes = nodes.toList(),
                    adjMatrix = adjacencyMatrix.map(MutableList<Double>::toList),
                    nodenumbers = nodeNumbers.toMap(),
                    weighted = weighted,
                    directed = directedFromSource,
                ),
            ),
        )
    }

    // JSXGraph 1.13.3: src/reader/graph.js -> read and drawGraph.
    internal fun read(
        board: Board,
        limits: GraphReaderLimits = GraphReaderLimits(),
        randomSource: RandomSource = DEFAULT_RANDOM_SOURCE,
    ): GMResult<DrawnGraph, GraphReaderError> {
        val parsed = when (val result = parseData(limits = limits)) {
            is GMResult.Ok -> result.value
            is GMResult.Err -> return result
        }
        if (
            parsed.boundingBox.size < BOUNDING_BOX_COORDINATE_COUNT ||
            parsed.boundingBox
                .take(BOUNDING_BOX_COORDINATE_COUNT)
                .any { !it.isFinite() }
        ) {
            return GMResult.Err(
                GraphReaderError.InvalidBoundingBox(
                    parsed.boundingBox,
                ),
            )
        }

        when (
            val result = board.setBoundingBox(
                bbox = parsed.boundingBox.toDoubleArray(),
                keepAspectRatio = true,
            )
        ) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> {
                return GMResult.Err(
                    GraphReaderError.BoundingBoxUpdateFailed(
                        result.error,
                    ),
                )
            }
        }

        val coordinates = mutableListOf<DoubleArray>()
        for ((nodeIndex, node) in parsed.graph.nodes.withIndex()) {
            val x = node.coords[0] ?: when (
                val result = randomCoordinate(
                    board = board,
                    randomSource = randomSource,
                    nodeIndex = nodeIndex,
                    axis = X_AXIS,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val y = node.coords[1] ?: when (
                val result = randomCoordinate(
                    board = board,
                    randomSource = randomSource,
                    nodeIndex = nodeIndex,
                    axis = Y_AXIS,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            coordinates += doubleArrayOf(x, y)
        }

        val createdElements = mutableListOf<GeometryElement>()
        val drawnNodes = mutableListOf<DrawnGraphNode>()
        val segments = MutableList(parsed.graph.n) {
            MutableList<DrawnGraphSegment?>(parsed.graph.n) { null }
        }
        board.suspendUpdate()
        for ((nodeIndex, node) in parsed.graph.nodes.withIndex()) {
            val point = when (
                val result = Point.create(
                    board = board,
                    coordinates = coordinates[nodeIndex],
                    name = node.name,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    rollback(board, createdElements)
                    return GMResult.Err(
                        GraphReaderError.PointCreationFailed(
                            nodeIndex = nodeIndex,
                            cause = result.error,
                        ),
                    )
                }
            }
            createdElements += point
            drawnNodes +=
                DrawnGraphNode(
                    source = node,
                    reference = point,
                )
        }

        for (sourceIndex in 0 until parsed.graph.n) {
            for (targetIndex in 0 until parsed.graph.n) {
                if (sourceIndex == targetIndex) {
                    continue
                }
                if (!parsed.graph.directed && targetIndex < sourceIndex) {
                    segments[sourceIndex][targetIndex] =
                        segments[targetIndex][sourceIndex]
                    continue
                }
                val weight =
                    parsed.graph.adjMatrix[sourceIndex][targetIndex]
                if (!(weight < Double.MAX_VALUE) || weight == 0.0) {
                    continue
                }
                val segment = when (
                    val result = Line.createSegment(
                        board = board,
                        point1 = drawnNodes[sourceIndex].reference,
                        point2 = drawnNodes[targetIndex].reference,
                    )
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        rollback(board, createdElements)
                        return GMResult.Err(
                            GraphReaderError.SegmentCreationFailed(
                                sourceNodeIndex = sourceIndex,
                                targetNodeIndex = targetIndex,
                                cause = result.error,
                            ),
                        )
                    }
                }
                segment.configureVisualArrows(
                    firstArrow = false,
                    lastArrow = parsed.graph.directed,
                )
                createdElements += segment
                val weightLabel =
                    if (parsed.graph.weighted) {
                        when (
                            val result = Text.create(
                                board = board,
                                coordinates = doubleArrayOf(0.0, 0.0),
                                content = JsNumberFormat.compact(weight),
                                anchor = segment,
                            )
                        ) {
                            is GMResult.Ok -> {
                                createdElements += result.value
                                result.value
                            }
                            is GMResult.Err -> {
                                rollback(board, createdElements)
                                return GMResult.Err(
                                    GraphReaderError
                                        .WeightTextCreationFailed(
                                            sourceNodeIndex = sourceIndex,
                                            targetNodeIndex = targetIndex,
                                            cause = result.error,
                                        ),
                                )
                            }
                        }
                    } else {
                        null
                    }
                val drawnSegment = DrawnGraphSegment(
                    edge = segment,
                    weight = weight,
                    weightLabel = weightLabel,
                )
                segments[sourceIndex][targetIndex] = drawnSegment
                if (!parsed.graph.directed) {
                    segments[targetIndex][sourceIndex] = drawnSegment
                }
            }
        }
        board.unsuspendUpdate()
        return GMResult.Ok(
            DrawnGraph(
                graph = parsed.graph,
                nodes = drawnNodes.toList(),
                segments = segments.map(MutableList<DrawnGraphSegment?>::toList),
            ),
        )
    }

    private fun randomCoordinate(
        board: Board,
        randomSource: RandomSource,
        nodeIndex: Int,
        axis: Int,
    ): GMResult<Double, GraphReaderError> {
        val random = try {
            randomSource.nextDouble()
        } catch (cause: Exception) {
            return GMResult.Err(
                GraphReaderError.RandomCoordinateFailed(
                    nodeIndex = nodeIndex,
                    axis = axis,
                    message = cause.message,
                ),
            )
        }
        val value =
            if (axis == X_AXIS) {
                random * board.canvasWidth / (board.unitX * RANDOM_PADDING) -
                    board.origin.scrCoords[1] /
                    (board.unitX * RANDOM_PADDING)
            } else {
                random * board.canvasHeight / (board.unitY * RANDOM_PADDING) -
                    (
                        board.canvasHeight -
                            board.origin.scrCoords[2]
                        ) / (board.unitY * RANDOM_PADDING)
            }
        return GMResult.Ok(value)
    }

    private fun rollback(
        board: Board,
        createdElements: List<GeometryElement>,
    ) {
        board.removeObjects(createdElements.asReversed())
        board.unsuspendUpdate()
    }

    private fun validateLimits(
        limits: GraphReaderLimits,
    ): GraphReaderError.InvalidLimits? {
        val invalid = when {
            limits.maxSourceCharacters < 0 ->
                "maxSourceCharacters" to limits.maxSourceCharacters.toLong()
            limits.maxLines < 0 ->
                "maxLines" to limits.maxLines.toLong()
            limits.maxNodes < 0 ->
                "maxNodes" to limits.maxNodes.toLong()
            limits.maxEdges < 0 ->
                "maxEdges" to limits.maxEdges.toLong()
            limits.maxMatrixCells < 0 ->
                "maxMatrixCells" to limits.maxMatrixCells
            else -> null
        }
        return invalid?.let { (name, value) ->
            GraphReaderError.InvalidLimits(name, value)
        }
    }

    private fun jsParseInt(value: String?): Double {
        val prefix = value
            ?.trimStart()
            ?.let(INTEGER_PREFIX::find)
            ?.value
            ?: return Double.NaN
        return prefix.toDoubleOrNull() ?: Double.NaN
    }

    private fun splitPreservingEmpty(
        source: String,
        separator: Char,
    ): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        for (index in source.indices) {
            if (source[index] == separator) {
                result += source.substring(start, index)
                start = index + 1
            }
        }
        result += source.substring(start)
        return result
    }

    private companion object {
        const val NODE_COUNT_LINE = 3
        const val FIRST_NODE_INDEX = 3
        const val UNDEFINED_TOKEN = "undefined"
        const val BOUNDING_BOX_COORDINATE_COUNT = 4
        const val X_AXIS = 0
        const val Y_AXIS = 1
        const val RANDOM_PADDING = 1.1
        val INTEGER_PREFIX = Regex("""^[+-]?\d+""")
        val DEFAULT_RANDOM_SOURCE =
            RandomSource { Random.nextDouble() }
    }
}

internal class GraphReaderFactory(
    private val randomSource: RandomSource =
        RandomSource { Random.nextDouble() },
) : JsxGraphReaderFactory<Board> {
    override fun create(
        board: Board,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError> =
        GMResult.Ok(
            JsxGraphReader {
                when (
                    val result = GraphReader(source).read(
                        board = board,
                        randomSource = randomSource,
                    )
                ) {
                    is GMResult.Ok -> GMResult.Ok(Unit)
                    is GMResult.Err -> {
                        GMResult.Err(
                            ReaderError.DomainFailure(result.error),
                        )
                    }
                }
            },
        )
}

internal fun ReaderRegistry<Board>.registerGraphReader(
    randomSource: RandomSource =
        RandomSource { Random.nextDouble() },
) {
    registerReader(
        reader = GraphReaderFactory(randomSource),
        extensions = listOf("txt", "graph", "digraph"),
    )
}
