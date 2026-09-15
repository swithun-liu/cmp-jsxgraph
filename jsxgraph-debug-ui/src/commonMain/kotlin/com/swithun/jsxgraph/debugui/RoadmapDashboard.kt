/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DashboardBackground = Color(0xFFF4F6F5)
private val DashboardSurface = Color(0xFFFFFFFF)
private val DashboardInk = Color(0xFF17211C)
private val DashboardMuted = Color(0xFF66716B)
private val DashboardBorder = Color(0xFFDCE2DE)
private val FoundationGreen = Color(0xFF1D7A50)
private val NumericsBlue = Color(0xFF27659C)
private val GeometryAmber = Color(0xFFA66024)
private val RenderingCoral = Color(0xFFB84B3A)
private val TrackColor = Color(0xFFE3E8E5)

private enum class RoadmapStatus(
    val label: String,
) {
    ACTIVE("Active"),
    IN_PROGRESS("In progress"),
    PLANNED("Planned"),
}

private data class RoadmapPhase(
    val title: String,
    val subtitle: String,
    val status: RoadmapStatus,
    val progress: Float,
    val accent: Color,
    val completedItems: List<String>,
    val nextTitle: String,
    val nextDetail: String,
    val exitGate: String,
)

private data class UpcomingMilestone(
    val title: String,
    val detail: String,
    val progress: Float?,
    val status: String,
    val accent: Color,
)

private val roadmapPhases = listOf(
    RoadmapPhase(
        title = "Foundation",
        subtitle = "Core primitives & shared model",
        status = RoadmapStatus.ACTIVE,
        progress = 1.0f,
        accent = FoundationGreen,
        completedItems = listOf(
            "Event system",
            "Base object model",
            "Common utilities",
        ),
        nextTitle = "Coordinate systems & transforms",
        nextDetail = "Viewport mapping, matrix transforms and shared state",
        exitGate = "Common behavior tests pass across JVM, iOS Simulator, and Wasm.",
    ),
    RoadmapPhase(
        title = "Numerics",
        subtitle = "Algorithms & computation",
        status = RoadmapStatus.IN_PROGRESS,
        progress = 0.78f,
        accent = NumericsBlue,
        completedItems = listOf(
            "Linear algebra",
            "Integration & ODE",
            "Roots & interpolation",
        ),
        nextTitle = "Optimization kernels",
        nextDetail = "Complete the remaining MIT-compatible numerical surface",
        exitGate = "Official fixtures match for regular, degenerate, and non-finite inputs.",
    ),
    RoadmapPhase(
        title = "Geometry",
        subtitle = "2D & spatial operations",
        status = RoadmapStatus.IN_PROGRESS,
        progress = 0.62f,
        accent = GeometryAmber,
        completedItems = listOf(
            "Points, lines & segments",
            "Circles & intersections",
            "Polygons & Bezier curves",
        ),
        nextTitle = "Constructions",
        nextDetail = "Dynamic elements, transformations and dependency updates",
        exitGate = "Element construction and update behavior matches JSXGraph 1.13.3.",
    ),
    RoadmapPhase(
        title = "Rendering & Interaction",
        subtitle = "Compose Canvas runtime",
        status = RoadmapStatus.IN_PROGRESS,
        progress = 0.32f,
        accent = RenderingCoral,
        completedItems = listOf(
            "Portable render scene",
            "Point, line & circle Canvas",
            "Same-source visual parity",
        ),
        nextTitle = "Curves, text & interaction",
        nextDetail = "Expand element rendering before hit testing and gestures",
        exitGate = "Visual geometry and interaction traces meet the stable parity gates.",
    ),
)

private val upcomingMilestones = listOf(
    UpcomingMilestone(
        title = "Parser",
        detail = "Construction documents & JessieCode",
        progress = 0.42f,
        status = "42%",
        accent = NumericsBlue,
    ),
    UpcomingMilestone(
        title = "Composition",
        detail = "Board order, z-order & clipping",
        progress = 0.15f,
        status = "15%",
        accent = GeometryAmber,
    ),
    UpcomingMilestone(
        title = "Platform",
        detail = "Android, iOS, Desktop & Web",
        progress = null,
        status = "Ready",
        accent = FoundationGreen,
    ),
)

@Composable
internal fun RoadmapDashboard(
    onOpenParity: () -> Unit,
) {
    var selectedPhase by remember { mutableStateOf<RoadmapPhase?>(null) }
    var showHelp by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = "jsxgraph-roadmap-dashboard" },
        containerColor = DashboardBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            DashboardTopBar(
                onOpenParity = onOpenParity,
                onShowHelp = { showHelp = true },
                showMenu = showMenu,
                onShowMenuChange = { showMenu = it },
            )
        },
    ) { contentPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            val expanded = maxWidth >= 760.dp
            val horizontalPadding = when {
                maxWidth >= 1180.dp -> 48.dp
                maxWidth >= 760.dp -> 32.dp
                else -> 16.dp
            }
            val sectionGap = if (expanded) 16.dp else 18.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(
                        start = horizontalPadding,
                        end = horizontalPadding,
                        top = 24.dp,
                        bottom = 18.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(sectionGap),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1280.dp)
                        .align(Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(sectionGap),
                ) {
                    RoadmapSummary(expanded = expanded)
                    PhaseGrid(
                        expanded = expanded,
                        onSelectPhase = { selectedPhase = it },
                    )
                    UpcomingStrip(expanded = expanded)
                    DashboardFooter()
                }
            }
        }
    }

    selectedPhase?.let { phase ->
        PhaseDetailDialog(
            phase = phase,
            onDismiss = { selectedPhase = null },
            onOpenParity = onOpenParity,
        )
    }
    if (showHelp) {
        DashboardHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun DashboardTopBar(
    onOpenParity: () -> Unit,
    onShowHelp: () -> Unit,
    showMenu: Boolean,
    onShowMenuChange: (Boolean) -> Unit,
) {
    Surface(
        color = DashboardSurface,
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, DashboardBorder),
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val compact = maxWidth < 620.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (compact) 66.dp else 76.dp)
                    .padding(horizontal = if (compact) 12.dp else 28.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    color = DashboardInk,
                    shape = RoundedCornerShape(6.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp),
                            tint = Color.White,
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "CMP JSXGraph",
                        color = DashboardInk,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    )
                    if (!compact) {
                        Text(
                            text = "Pure Kotlin Multiplatform implementation",
                            color = DashboardMuted,
                            style = MaterialTheme.typography.bodySmall,
                            letterSpacing = 0.sp,
                        )
                    }
                }
                if (!compact) {
                    Button(
                        onClick = onOpenParity,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DashboardInk,
                            contentColor = Color.White,
                        ),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Science,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Parity lab",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onShowHelp) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                        contentDescription = "Roadmap help",
                        tint = DashboardInk,
                    )
                }
                Box {
                    IconButton(onClick = { onShowMenuChange(true) }) {
                        Icon(
                            imageVector = Icons.Outlined.MoreVert,
                            contentDescription = "More actions",
                            tint = DashboardInk,
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { onShowMenuChange(false) },
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open parity lab", letterSpacing = 0.sp) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Science, contentDescription = null)
                            },
                            onClick = {
                                onShowMenuChange(false)
                                onOpenParity()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("About this roadmap", letterSpacing = 0.sp) },
                            leadingIcon = {
                                Icon(
                                    Icons.AutoMirrored.Outlined.HelpOutline,
                                    contentDescription = null,
                                )
                            },
                            onClick = {
                                onShowMenuChange(false)
                                onShowHelp()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RoadmapSummary(
    expanded: Boolean,
) {
    if (expanded) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(32.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            RoadmapHeading(modifier = Modifier.weight(1f))
            OverallProgress(
                modifier = Modifier.widthIn(min = 280.dp, max = 360.dp),
            )
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            RoadmapHeading()
            OverallProgress(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RoadmapHeading(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(
            text = "PRODUCTION ROADMAP",
            color = DashboardInk,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
        Text(
            text = "From translated primitives to a production-ready Compose runtime.",
            color = DashboardMuted,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun OverallProgress(
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .semantics { contentDescription = "overall-progress:70" },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "OVERALL PROGRESS",
                color = DashboardMuted,
                style = MaterialTheme.typography.labelMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = "70%",
                color = DashboardInk,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
        }
        ProgressTrack(progress = 0.70f, accent = FoundationGreen)
    }
}

@Composable
private fun PhaseGrid(
    expanded: Boolean,
    onSelectPhase: (RoadmapPhase) -> Unit,
) {
    if (expanded) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            roadmapPhases.chunked(2).forEach { rowPhases ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    rowPhases.forEach { phase ->
                        PhaseCard(
                            phase = phase,
                            modifier = Modifier.weight(1f),
                            onClick = { onSelectPhase(phase) },
                        )
                    }
                }
            }
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            roadmapPhases.forEach { phase ->
                PhaseCard(
                    phase = phase,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelectPhase(phase) },
                )
            }
        }
    }
}

@Composable
private fun PhaseCard(
    phase: RoadmapPhase,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Surface(
        modifier = modifier
            .heightIn(min = 244.dp)
            .semantics { contentDescription = "roadmap-phase:${phase.title}" },
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = DashboardSurface,
        border = BorderStroke(1.dp, DashboardBorder),
        shadowElevation = 0.dp,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(phase.accent),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = phase.title,
                            color = DashboardInk,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.sp,
                        )
                        Text(
                            text = phase.subtitle,
                            color = DashboardMuted,
                            style = MaterialTheme.typography.bodyMedium,
                            letterSpacing = 0.sp,
                        )
                    }
                    StatusBadge(status = phase.status, accent = phase.accent)
                }

                ProgressTrack(
                    progress = phase.progress,
                    accent = phase.accent,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    phase.completedItems.forEach { item ->
                        CompletedItem(text = item, accent = phase.accent)
                    }
                }

                HorizontalDivider(color = DashboardBorder)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(phase.accent.copy(alpha = 0.10f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = phase.accent,
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "NEXT UP",
                            color = phase.accent,
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                        )
                        Text(
                            text = phase.nextTitle,
                            color = DashboardInk,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: RoadmapStatus,
    accent: Color,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(accent.copy(alpha = 0.10f))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(accent, CircleShape),
        )
        Text(
            text = status.label,
            color = accent,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun CompletedItem(
    text: String,
    accent: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(17.dp),
            tint = accent,
        )
        Text(
            text = text,
            color = DashboardInk,
            style = MaterialTheme.typography.bodyMedium,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    accent: Color,
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "roadmap-progress",
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(TrackColor),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(animatedProgress)
                .fillMaxHeight()
                .background(accent),
        )
    }
}

@Composable
private fun UpcomingStrip(
    expanded: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFE9EEEB),
        shape = RoundedCornerShape(8.dp),
        shadowElevation = 0.dp,
    ) {
        if (expanded) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "NEXT",
                    color = DashboardMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
                upcomingMilestones.forEachIndexed { index, milestone ->
                    if (index > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(44.dp)
                                .background(DashboardBorder),
                        )
                    }
                    UpcomingMilestoneItem(
                        milestone = milestone,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = "NEXT",
                    color = DashboardMuted,
                    style = MaterialTheme.typography.labelMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
                upcomingMilestones.forEachIndexed { index, milestone ->
                    if (index > 0) {
                        HorizontalDivider(color = DashboardBorder)
                    }
                    UpcomingMilestoneItem(
                        milestone = milestone,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingMilestoneItem(
    milestone: UpcomingMilestone,
    modifier: Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .background(milestone.accent, CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = milestone.title,
                color = DashboardInk,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = milestone.detail,
                color = DashboardMuted,
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 0.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            text = milestone.status,
            color = milestone.accent,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun DashboardFooter() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(FoundationGreen, CircleShape),
            )
            Text(
                text = "Development is active",
                color = DashboardMuted,
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 0.sp,
            )
        }
        Text(
            text = "Updated Sep 15",
            color = DashboardMuted,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.sp,
        )
    }
}

@Composable
private fun PhaseDetailDialog(
    phase: RoadmapPhase,
    onDismiss: () -> Unit,
    onOpenParity: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Outlined.Timeline,
                contentDescription = null,
                tint = phase.accent,
            )
        },
        title = {
            Text(
                text = phase.title,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = phase.nextDetail,
                    color = DashboardInk,
                    style = MaterialTheme.typography.bodyMedium,
                    letterSpacing = 0.sp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(
                        text = "EXIT GATE",
                        color = DashboardMuted,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.sp,
                    )
                    Text(
                        text = phase.exitGate,
                        color = DashboardMuted,
                        style = MaterialTheme.typography.bodyMedium,
                        letterSpacing = 0.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done", letterSpacing = 0.sp)
            }
        },
        dismissButton = if (phase.title == "Rendering & Interaction") {
            {
                TextButton(
                    onClick = {
                        onDismiss()
                        onOpenParity()
                    },
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Science,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Open parity lab", letterSpacing = 0.sp)
                }
            }
        } else {
            null
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = DashboardSurface,
    )
}

@Composable
private fun DashboardHelpDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                contentDescription = null,
                tint = FoundationGreen,
            )
        },
        title = {
            Text(
                text = "Reading the roadmap",
                fontWeight = FontWeight.Black,
                letterSpacing = 0.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                HelpRow(
                    accent = FoundationGreen,
                    title = "Active",
                    detail = "Current work has passing implementation slices.",
                )
                HelpRow(
                    accent = NumericsBlue,
                    title = "In progress",
                    detail = "Coverage exists, but the phase exit gate is not complete.",
                )
                HelpRow(
                    accent = RenderingCoral,
                    title = "Planned",
                    detail = "Prototype work exists; production contracts remain open.",
                )
                HorizontalDivider(color = DashboardBorder)
                Text(
                    text = "Progress reflects translated capability, not production readiness.",
                    color = DashboardMuted,
                    style = MaterialTheme.typography.bodySmall,
                    letterSpacing = 0.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", letterSpacing = 0.sp)
            }
        },
        shape = RoundedCornerShape(8.dp),
        containerColor = DashboardSurface,
    )
}

@Composable
private fun HelpRow(
    accent: Color,
    title: String,
    detail: String,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .background(accent, CircleShape),
        )
        Column {
            Text(
                text = title,
                color = DashboardInk,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            )
            Text(
                text = detail,
                color = DashboardMuted,
                style = MaterialTheme.typography.bodySmall,
                letterSpacing = 0.sp,
            )
        }
    }
}
