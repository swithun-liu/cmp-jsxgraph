/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swithun.jsxgraph.compose.JsxGraphBoard
import com.swithun.jsxgraph.compose.JsxGraphScenePreview
import com.swithun.jsxgraph.core.GMResult

private const val DEFAULT_PARITY_BOARD_ASPECT_RATIO = 1.2f

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
    StableLoad,
}

data class JsxGraphDebugOptions(
    val initialDestination: JsxGraphDebugDestination = JsxGraphDebugDestination.Roadmap,
    val initialPreview: JsxGraphDebugPreview = JsxGraphDebugPreview.Native,
    val parityCaseId: String = JsxGraphParityCorpus.DEFAULT_CASE_ID,
    val sourceOverride: String? = null,
    val boardOnly: Boolean = false,
    val autoRunLoadTest: Boolean = false,
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
    onParityCaseChange: (String) -> Unit = {},
) {
    var destination by rememberSaveable(options.initialDestination) {
        mutableStateOf(options.initialDestination)
    }
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF176B5B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8EBE5),
            onPrimaryContainer = Color(0xFF0E372F),
            secondary = Color(0xFFA25C32),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF2DED2),
            onSecondaryContainer = Color(0xFF4A2715),
            background = Color(0xFFF4F6F5),
            onBackground = Color(0xFF17211C),
            surface = Color.White,
            onSurface = Color(0xFF17211C),
            surfaceVariant = Color(0xFFE7ECE9),
            onSurfaceVariant = Color(0xFF5D6963),
            outline = Color(0xFF85918B),
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
                onBackToRoadmap = {
                    destination = JsxGraphDebugDestination.Roadmap
                },
                onParityCaseChange = onParityCaseChange,
            )
            JsxGraphDebugDestination.StableLoad -> StableLoadScreen(
                autoRun = options.autoRunLoadTest,
                onBackToRoadmap = {
                    destination = JsxGraphDebugDestination.Roadmap
                },
            )
        }
    }
}

@Composable
private fun ParityWorkspace(
    options: JsxGraphDebugOptions,
    onBackToRoadmap: () -> Unit,
    onParityCaseChange: (String) -> Unit,
) {
    var preview by rememberSaveable { mutableStateOf(options.initialPreview) }
    var selectedCaseId by rememberSaveable(options.parityCaseId) {
        mutableStateOf(options.parityCaseId)
    }
    var sourceOverride by rememberSaveable(options.sourceOverride) {
        mutableStateOf(options.sourceOverride)
    }
    val parityCase = remember(selectedCaseId, sourceOverride) {
        resolveParityCase(
            options.copy(
                parityCaseId = selectedCaseId,
                sourceOverride = sourceOverride,
            ),
        )
    }
    val session = remember(parityCase) {
        when (parityCase) {
            is GMResult.Ok -> createParitySession(parityCase.value.source)
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
        JsxGraphDebugPreview.Native -> when (session) {
            is GMResult.Ok -> "jsxgraph-audit:ready"
            is GMResult.Err -> "jsxgraph-audit:error:${session.error}"
        }
        JsxGraphDebugPreview.Official -> when (val result = officialResult) {
            OfficialRenderResult.Loading -> "jsxgraph-audit:loading"
            is OfficialRenderResult.Ready -> "jsxgraph-audit:ready"
            is OfficialRenderResult.Error -> "jsxgraph-audit:error:${result.message}"
        }
    }
    val caseMarker = when (parityCase) {
        is GMResult.Ok -> parityCase.value.id
        is GMResult.Err -> selectedCaseId
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
                session = session,
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
                    "$renderStatus jsxgraph-case:$caseMarker " +
                        "jsxgraph-catalog:${JsxGraphParityCorpus.cases.size}"
            },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        DebugContent(
            contentPadding = contentPadding,
            parityCase = parityCase,
            session = session,
            preview = preview,
            onPreviewChange = { preview = it },
            onOfficialResult = { officialResult = it },
            onBackToRoadmap = onBackToRoadmap,
            onCaseSelect = { caseId ->
                selectedCaseId = caseId
                sourceOverride = null
                onParityCaseChange(caseId)
            },
        )
    }
}

@Composable
private fun DebugContent(
    contentPadding: PaddingValues,
    parityCase: GMResult<JsxGraphParityCase, String>,
    session: GMResult<JsxGraphParitySession, String>,
    preview: JsxGraphDebugPreview,
    onPreviewChange: (JsxGraphDebugPreview) -> Unit,
    onOfficialResult: (OfficialRenderResult) -> Unit,
    onBackToRoadmap: () -> Unit,
    onCaseSelect: (String) -> Unit,
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
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        val compact = maxWidth < 720.dp
        val shortViewport = maxHeight < 620.dp
        val previousCase = caseIndex
            ?.takeIf { index -> index > 0 }
            ?.let { index -> JsxGraphParityCorpus.cases[index - 1] }
        val nextCase = caseIndex
            ?.takeIf { index -> index < JsxGraphParityCorpus.cases.lastIndex }
            ?.let { index -> JsxGraphParityCorpus.cases[index + 1] }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            WorkbenchHeader(
                caseIndex = caseIndex,
                onBackToRoadmap = onBackToRoadmap,
            )

            if (compact) {
                CaseNavigator(
                    selectedCase = selectedCase,
                    selectedIndex = caseIndex,
                    previousCase = previousCase,
                    nextCase = nextCase,
                    onCaseSelect = onCaseSelect,
                )
                PreviewModeSelector(
                    preview = preview,
                    onPreviewChange = onPreviewChange,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CaseNavigator(
                        selectedCase = selectedCase,
                        selectedIndex = caseIndex,
                        previousCase = previousCase,
                        nextCase = nextCase,
                        onCaseSelect = onCaseSelect,
                        modifier = Modifier.weight(1f),
                    )
                    PreviewModeSelector(
                        preview = preview,
                        onPreviewChange = onPreviewChange,
                        modifier = Modifier.width(360.dp),
                    )
                }
            }

            selectedCase?.let { currentCase ->
                CaseSummary(
                    parityCase = currentCase,
                    showFeatures = !shortViewport,
                )
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                val boardWidth = minOf(
                    maxWidth,
                    (
                        maxHeight - 28.dp
                    ).coerceAtLeast(1.dp) * DEFAULT_PARITY_BOARD_ASPECT_RATIO,
                )
                Column(
                    modifier = Modifier
                        .align(
                            if (compact) {
                                Alignment.TopCenter
                            } else {
                                Alignment.Center
                            },
                        )
                        .width(boardWidth),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(DEFAULT_PARITY_BOARD_ASPECT_RATIO)
                            .semantics {
                                contentDescription = "jsxgraph-parity-board"
                            },
                        shape = RoundedCornerShape(6.dp),
                        color = Color.White,
                        tonalElevation = 0.dp,
                        shadowElevation = 0.dp,
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                        ),
                    ) {
                        ParityPreview(
                            parityCase = parityCase,
                            session = session,
                            preview = preview,
                            onOfficialResult = onOfficialResult,
                        )
                    }
                    Text(
                        text = "${selectedCase?.title ?: "Invalid parity case"} - " +
                            when (preview) {
                                JsxGraphDebugPreview.Source -> "Parity source"
                                JsxGraphDebugPreview.Official -> "Official JSXGraph 1.13.3"
                                JsxGraphDebugPreview.Native -> "Compose Canvas"
                            },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkbenchHeader(
    caseIndex: Int?,
    onBackToRoadmap: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBackToRoadmap) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Open roadmap",
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Parity workbench",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = "JSXGraph 1.13.3 reference",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = caseIndex?.let { index ->
                "${index + 1} of ${JsxGraphParityCorpus.cases.size}"
            } ?: "Custom",
            modifier = Modifier.semantics {
                contentDescription =
                    "Case ${caseIndex?.plus(1) ?: 0} of " +
                        JsxGraphParityCorpus.cases.size
            },
            color = MaterialTheme.colorScheme.secondary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun CaseNavigator(
    selectedCase: JsxGraphParityCase?,
    selectedIndex: Int?,
    previousCase: JsxGraphParityCase?,
    nextCase: JsxGraphParityCase?,
    onCaseSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = previousCase != null,
            onClick = {
                previousCase?.let { parityCase ->
                    onCaseSelect(parityCase.id)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronLeft,
                contentDescription = "Previous case",
            )
        }
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { menuExpanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics {
                        contentDescription =
                            "Select case: ${selectedCase?.title ?: "Custom source"}"
                    },
                shape = RoundedCornerShape(6.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                ) {
                    Text(
                        text = selectedCase?.title ?: "Select a parity case",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    )
                    Text(
                        text = selectedCase?.let { parityCase ->
                            "${selectedIndex?.plus(1) ?: 0} / " +
                                "${JsxGraphParityCorpus.cases.size} · " +
                                parityCase.suite.label
                        } ?: "Custom source",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Outlined.ExpandMore,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                JsxGraphParityCorpus.cases.forEachIndexed { index, parityCase ->
                    if (index == JsxGraphParityCorpus.productionCaseCount) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                    }
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = parityCase.title,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    fontWeight = if (index == selectedIndex) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    },
                                    letterSpacing = 0.sp,
                                )
                                Text(
                                    text = "${index + 1} · ${parityCase.suite.label}",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.labelSmall,
                                    letterSpacing = 0.sp,
                                )
                            }
                        },
                        leadingIcon = {
                            if (index == selectedIndex) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                )
                            }
                        },
                        modifier = Modifier.semantics {
                            contentDescription = "Open case:${parityCase.id}"
                        },
                        onClick = {
                            menuExpanded = false
                            onCaseSelect(parityCase.id)
                        },
                    )
                }
            }
        }
        IconButton(
            enabled = nextCase != null,
            onClick = {
                nextCase?.let { parityCase ->
                    onCaseSelect(parityCase.id)
                }
            },
        ) {
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Next case",
            )
        }
    }
}

@Composable
private fun PreviewModeSelector(
    preview: JsxGraphDebugPreview,
    onPreviewChange: (JsxGraphDebugPreview) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier = modifier.semantics {
            contentDescription = "Preview modes: Source, Official, Native"
        },
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
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
}

@Composable
private fun CaseSummary(
    parityCase: JsxGraphParityCase,
    showFeatures: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription =
                    "jsxgraph-case-title:${parityCase.id}:${parityCase.title}"
            },
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = parityCase.scenario,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            letterSpacing = 0.sp,
        )
        if (showFeatures && parityCase.features.isNotEmpty()) {
            Text(
                text = parityCase.features.sorted().joinToString(" · "),
                color = MaterialTheme.colorScheme.secondary,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                letterSpacing = 0.sp,
            )
        }
    }
}

@Composable
private fun ParityPreview(
    parityCase: GMResult<JsxGraphParityCase, String>,
    session: GMResult<JsxGraphParitySession, String>,
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
            JsxGraphDebugPreview.Native -> when (session) {
                is GMResult.Ok -> when (val value = session.value) {
                    is JsxGraphParitySession.ConstructionDocument ->
                        JsxGraphBoard(
                            session = value.session,
                            modifier = Modifier.fillMaxSize(),
                        )
                    is JsxGraphParitySession.JessieCode ->
                        JsxGraphBoard(
                            session = value.session,
                            modifier = Modifier.fillMaxSize(),
                        )
                }
                is GMResult.Err -> ErrorPreview(session.error)
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
                corpusCase.value.copy(
                    title = "Custom source",
                    scenario =
                        "An isolated source override based on ${corpusCase.value.title}.",
                    source = source,
                    features = emptySet(),
                    suite = JsxGraphParitySuite.Custom,
                )
            } ?: corpusCase.value,
        )
        is GMResult.Err -> options.sourceOverride?.let { source ->
            GMResult.Ok(
                JsxGraphParityCase(
                    id = options.parityCaseId,
                    title = "Custom source",
                    scenario = "An isolated source override outside the case catalog.",
                    source = source,
                    features = emptySet(),
                    suite = JsxGraphParitySuite.Custom,
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
