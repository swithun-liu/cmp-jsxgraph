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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swithun.jsxgraph.compose.JsxGraphScenePreview
import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphEngine
import com.swithun.jsxgraph.core.JsxGraphScene

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
    val boardOnly: Boolean = false,
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

    if (options.boardOnly) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .semantics {
                    contentDescription =
                        "$renderStatus jsxgraph-case:$caseMarker " +
                            "jsxgraph-parity-board"
                },
        ) {
            ParityPreview(
                parityCase = parityCase,
                parsedScene = parsedScene,
                preview = preview,
                onOfficialResult = { officialResult = it },
            )
        }
        return
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
    parsedScene: GMResult<JsxGraphScene, String>,
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
                ParityPreview(
                    parityCase = parityCase,
                    parsedScene = parsedScene,
                    preview = preview,
                    onOfficialResult = onOfficialResult,
                )
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

@Composable
private fun ParityPreview(
    parityCase: GMResult<JsxGraphParityCase, String>,
    parsedScene: GMResult<JsxGraphScene, String>,
    preview: JsxGraphDebugPreview,
    onOfficialResult: (OfficialRenderResult) -> Unit,
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
                is GMResult.Ok -> JsxGraphScenePreview(
                    scene = parsedScene.value,
                    modifier = Modifier.fillMaxSize(),
                )
                is GMResult.Err -> ErrorPreview(parsedScene.error)
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
): GMResult<JsxGraphScene, String> =
    when (val result = JsxGraphEngine.parse(source)) {
        is GMResult.Ok -> result
        is GMResult.Err -> GMResult.Err(result.error.message)
    }
