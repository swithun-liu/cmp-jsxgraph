/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swithun.jsxgraph.compose.GeometryPlaygroundScene
import com.swithun.jsxgraph.compose.JsxGraphGeometryPreview
import com.swithun.jsxgraph.core.GMResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

enum class JsxGraphDebugPreview {
    Source,
    Official,
    Native,
    ;

    companion object {
        fun from(value: String?): JsxGraphDebugPreview =
            entries.firstOrNull { preview ->
                preview.name.equals(value, ignoreCase = true)
            } ?: Native
    }
}

enum class JsxGraphDebugDestination {
    Roadmap,
    Parity,
}

data class JsxGraphDebugOptions(
    val initialDestination: JsxGraphDebugDestination = JsxGraphDebugDestination.Roadmap,
    val initialPreview: JsxGraphDebugPreview = JsxGraphDebugPreview.Native,
    val parityCaseId: String = JsxGraphParityCorpus.DEFAULT_CASE_ID,
    val sourceOverride: String? = null,
)

internal sealed interface OfficialRenderResult {
    data object Loading : OfficialRenderResult

    data class Ready(
        val width: Float,
        val height: Float,
    ) : OfficialRenderResult

    data class Error(val message: String) : OfficialRenderResult
}

@Composable
internal expect fun OfficialJsxGraphDiagram(
    source: String,
    modifier: Modifier = Modifier,
    onRenderResult: (OfficialRenderResult) -> Unit = {},
)

@Composable
fun JsxGraphDebugApp(
    options: JsxGraphDebugOptions = JsxGraphDebugOptions(),
) {
    var destination by rememberSaveable(options.initialDestination) {
        mutableStateOf(options.initialDestination)
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF246BCE),
            secondary = Color(0xFF16877A),
            background = Color(0xFFF3F5F7),
            surface = Color(0xFFFCFDFE),
        ),
    ) {
        when (destination) {
            JsxGraphDebugDestination.Roadmap -> RoadmapDashboard(
                onOpenParity = {
                    destination = JsxGraphDebugDestination.Parity
                },
            )
            JsxGraphDebugDestination.Parity -> ParityWorkspace(
                options = options,
                onBackToRoadmap = if (
                    options.initialDestination == JsxGraphDebugDestination.Roadmap
                ) {
                    { destination = JsxGraphDebugDestination.Roadmap }
                } else {
                    null
                },
            )
        }
    }
}

@Composable
private fun ParityWorkspace(
    options: JsxGraphDebugOptions,
    onBackToRoadmap: (() -> Unit)?,
) {
    var preview by rememberSaveable { mutableStateOf(options.initialPreview) }
    val parityCase = remember(options.parityCaseId, options.sourceOverride) {
        resolveParityCase(options)
    }
    val parsedScene = remember(parityCase) {
        when (parityCase) {
            is GMResult.Ok -> parseParitySource(parityCase.value.source)
            is GMResult.Err -> GMResult.Err(parityCase.error)
        }
    }
    var officialResult by remember(parityCase) {
        mutableStateOf<OfficialRenderResult>(OfficialRenderResult.Loading)
    }
    val renderStatus = when (preview) {
        JsxGraphDebugPreview.Source -> when (parityCase) {
            is GMResult.Ok -> "jsxgraph-audit:ready"
            is GMResult.Err -> "jsxgraph-audit:error:${parityCase.error}"
        }
        JsxGraphDebugPreview.Native -> when (parsedScene) {
            is GMResult.Ok -> "jsxgraph-audit:ready"
            is GMResult.Err -> "jsxgraph-audit:error:${parsedScene.error}"
        }
        JsxGraphDebugPreview.Official -> when (val result = officialResult) {
            OfficialRenderResult.Loading -> "jsxgraph-audit:loading"
            is OfficialRenderResult.Ready -> "jsxgraph-audit:ready"
            is OfficialRenderResult.Error -> "jsxgraph-audit:error:${result.message}"
        }
    }
    val caseMarker = when (parityCase) {
        is GMResult.Ok -> parityCase.value.id
        is GMResult.Err -> options.parityCaseId
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription =
                    "$renderStatus jsxgraph-case:$caseMarker"
            },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        DebugContent(
            contentPadding = contentPadding,
            parityCase = parityCase,
            parsedScene = parsedScene,
            preview = preview,
            onPreviewChange = { preview = it },
            onOfficialResult = { officialResult = it },
            onBackToRoadmap = onBackToRoadmap,
        )
    }
}

@Composable
private fun DebugContent(
    contentPadding: PaddingValues,
    parityCase: GMResult<JsxGraphParityCase, String>,
    parsedScene: GMResult<GeometryPlaygroundScene, String>,
    preview: JsxGraphDebugPreview,
    onPreviewChange: (JsxGraphDebugPreview) -> Unit,
    onOfficialResult: (OfficialRenderResult) -> Unit,
    onBackToRoadmap: (() -> Unit)?,
) {
    val selectedCase = (parityCase as? GMResult.Ok)?.value
    val caseIndex = selectedCase?.let { currentCase ->
        JsxGraphParityCorpus.cases.indexOfFirst { candidate ->
            candidate.id == currentCase.id
        }
    }?.takeIf { index -> index >= 0 }
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        val compactHeight = maxHeight < 600.dp
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(
                if (compactHeight) 8.dp else 12.dp,
            ),
        ) {
            if (!compactHeight) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom,
                ) {
                    Column {
                        Text(
                            text = "JSXGraph parity",
                            style = MaterialTheme.typography.titleLarge,
                            letterSpacing = 0.sp,
                        )
                        Text(
                            text = "Reference 1.13.3",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            letterSpacing = 0.sp,
                        )
                    }
                    Text(
                        text = caseIndex?.let { index ->
                            "case ${index + 1} / ${JsxGraphParityCorpus.cases.size}"
                        } ?: "unknown case",
                        color = MaterialTheme.colorScheme.secondary,
                        style = MaterialTheme.typography.labelMedium,
                        letterSpacing = 0.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (onBackToRoadmap != null) {
                    IconButton(onClick = onBackToRoadmap) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back to roadmap",
                        )
                    }
                }
                SingleChoiceSegmentedButtonRow(modifier = Modifier.weight(1f)) {
                    JsxGraphDebugPreview.entries.forEachIndexed { index, item ->
                        SegmentedButton(
                            selected = preview == item,
                            onClick = { onPreviewChange(item) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = JsxGraphDebugPreview.entries.size,
                            ),
                            label = { Text(item.name, letterSpacing = 0.sp) },
                        )
                    }
                }
            }

            val boardModifier = if (compactHeight) {
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
            } else {
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
            }
            Surface(
                modifier = boardModifier.semantics {
                    contentDescription = "jsxgraph-parity-board"
                },
                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                when (parityCase) {
                    is GMResult.Err -> ErrorPreview(parityCase.error)
                    is GMResult.Ok -> when (preview) {
                        JsxGraphDebugPreview.Source -> SourcePreview(
                            parityCase.value.source,
                        )
                        JsxGraphDebugPreview.Official -> OfficialJsxGraphDiagram(
                            source = parityCase.value.source,
                            modifier = Modifier.fillMaxSize(),
                            onRenderResult = onOfficialResult,
                        )
                        JsxGraphDebugPreview.Native -> when (parsedScene) {
                            is GMResult.Ok -> JsxGraphGeometryPreview(
                                scene = parsedScene.value,
                                modifier = Modifier.fillMaxSize(),
                            )
                            is GMResult.Err -> ErrorPreview(parsedScene.error)
                        }
                    }
                }
            }

            if (!compactHeight) {
                Text(
                    text = "${selectedCase?.title ?: "Invalid parity case"} - " + when (preview) {
                        JsxGraphDebugPreview.Source -> "Parity fixture JSON"
                        JsxGraphDebugPreview.Official -> "Official JSXGraph 1.13.3"
                        JsxGraphDebugPreview.Native -> "Compose Canvas"
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

private fun resolveParityCase(
    options: JsxGraphDebugOptions,
): GMResult<JsxGraphParityCase, String> =
    when (val corpusCase = JsxGraphParityCorpus.find(options.parityCaseId)) {
        is GMResult.Ok -> GMResult.Ok(
            options.sourceOverride?.let { source ->
                corpusCase.value.copy(source = source)
            } ?: corpusCase.value,
        )
        is GMResult.Err -> options.sourceOverride?.let { source ->
            GMResult.Ok(
                JsxGraphParityCase(
                    id = options.parityCaseId,
                    title = "Custom source",
                    source = source,
                    features = emptySet(),
                ),
            )
        } ?: corpusCase
    }

@Composable
private fun SourcePreview(source: String) {
    SelectionContainer {
        Text(
            text = source,
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFAFBFC))
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            color = Color(0xFF26323B),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun ErrorPreview(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
internal fun UnavailableOfficialPreview(
    message: String,
    modifier: Modifier,
    onRenderResult: (OfficialRenderResult) -> Unit,
) {
    LaunchedEffect(message) {
        onRenderResult(OfficialRenderResult.Error(message))
    }
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.sp,
        )
    }
}

internal fun parseParitySource(
    source: String,
): GMResult<GeometryPlaygroundScene, String> {
    val root = try {
        Json.parseToJsonElement(source)
    } catch (failure: Exception) {
        return GMResult.Err(failure.message ?: "Invalid parity source")
    }
    val objectRoot = root as? JsonObject
        ?: return GMResult.Err("Parity source must be a JSON object")
    val version = objectRoot.int("schemaVersion")
        ?: return GMResult.Err("Missing integer schemaVersion")
    if (version != 1) {
        return GMResult.Err("Unsupported parity schemaVersion: $version")
    }
    val bounds = objectRoot.numberArray("boundingBox", 4)
        ?: return GMResult.Err("boundingBox must contain four numbers")
    if (bounds != listOf(-6.0, 5.0, 6.0, -5.0)) {
        return GMResult.Err("Only the baseline [-6, 5, 6, -5] viewport is supported")
    }
    val fixedPoint = objectRoot.offset("fixedPoint")
        ?: return GMResult.Err("fixedPoint must contain two finite numbers")
    val controlPoint = objectRoot.offset("controlPoint")
        ?: return GMResult.Err("controlPoint must contain two finite numbers")
    val circle = objectRoot["circle"] as? JsonObject
        ?: return GMResult.Err("circle must be a JSON object")
    val circleCenter = circle.offset("center")
        ?: return GMResult.Err("circle.center must contain two finite numbers")
    val circleRadius = circle.float("radius")
        ?: return GMResult.Err("circle.radius must be finite")
    if (circleRadius <= 0.0f) {
        return GMResult.Err("circle.radius must be positive")
    }
    val sine = objectRoot["sine"] as? JsonObject
        ?: return GMResult.Err("sine must be a JSON object")
    val parabola = objectRoot["parabola"] as? JsonObject
        ?: return GMResult.Err("parabola must be a JSON object")
    return GMResult.Ok(
        GeometryPlaygroundScene(
            fixedPoint = fixedPoint,
            controlPoint = controlPoint,
            circleCenter = circleCenter,
            circleRadius = circleRadius,
            sineAmplitude = sine.float("amplitude")
                ?: return GMResult.Err("sine.amplitude must be finite"),
            sineFrequency = sine.float("frequency")
                ?: return GMResult.Err("sine.frequency must be finite"),
            parabolaQuadratic = parabola.float("quadratic")
                ?: return GMResult.Err("parabola.quadratic must be finite"),
            parabolaConstant = parabola.float("constant")
                ?: return GMResult.Err("parabola.constant must be finite"),
        ),
    )
}

private fun JsonObject.number(name: String): Double? =
    (this[name] as? JsonPrimitive)?.doubleOrNull?.takeIf(Double::isFinite)

private fun JsonObject.float(name: String): Float? =
    number(name)?.toFloat()?.takeIf(Float::isFinite)

private fun JsonObject.int(name: String): Int? =
    (this[name] as? JsonPrimitive)?.intOrNull

private fun JsonObject.numberArray(
    name: String,
    expectedSize: Int,
): List<Double>? {
    val array = this[name] as? JsonArray ?: return null
    if (array.size != expectedSize) {
        return null
    }
    val numbers = array.map { element: JsonElement ->
        (element as? JsonPrimitive)?.doubleOrNull
    }
    if (numbers.any { value -> value == null || !value.isFinite() }) {
        return null
    }
    return numbers.map { value -> value ?: return null }
}

private fun JsonObject.offset(name: String): Offset? {
    val values = numberArray(name, 2) ?: return null
    val x = values[0].toFloat()
    val y = values[1].toFloat()
    if (!x.isFinite() || !y.isFinite()) {
        return null
    }
    return Offset(x, y)
}
