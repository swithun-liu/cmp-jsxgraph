/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.debugui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.swithun.jsxgraph.compose.JsxGraphScenePreview
import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.debugui.generated.ProductionCorpusCase
import com.swithun.jsxgraph.debugui.generated.productionCorpusCases
import kotlinx.coroutines.delay

@Composable
internal fun StableLoadScreen(
    autoRun: Boolean,
    onBackToRoadmap: (() -> Unit)?,
) {
    val renderedCases = remember {
        productionCorpusCases.map { case ->
            case to parseParitySource(case.source)
        }
    }
    val listState = rememberLazyListState()
    LaunchedEffect(autoRun, renderedCases.size) {
        if (autoRun) {
            for (index in renderedCases.indices) {
                listState.animateScrollToItem(index)
                delay(AUTO_SCROLL_DELAY_MILLIS)
            }
        }
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                contentDescription =
                    "stable-load:ready:${renderedCases.size}"
            },
        containerColor = Color(0xFFF4F6F5),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            StableLoadTopBar(
                caseCount = renderedCases.size,
                onBackToRoadmap = onBackToRoadmap,
            )
        },
    ) { contentPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            itemsIndexed(
                items = renderedCases,
                key = { _, item -> item.first.id },
            ) { index, (case, scene) ->
                StableLoadCase(
                    index = index,
                    case = case,
                    scene = scene,
                )
            }
        }
    }
}

private const val AUTO_SCROLL_DELAY_MILLIS = 80L

@Composable
private fun StableLoadTopBar(
    caseCount: Int,
    onBackToRoadmap: (() -> Unit)?,
) {
    Surface(
        color = Color.White,
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (onBackToRoadmap != null) {
                IconButton(onClick = onBackToRoadmap) {
                    Icon(
                        imageVector =
                            Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = "Back to roadmap",
                    )
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = "Stable corpus",
                    color = Color(0xFF17211C),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.sp,
                )
                Text(
                    text = "$caseCount independent production scenarios",
                    color = Color(0xFF66716B),
                    style = MaterialTheme.typography.bodySmall,
                    letterSpacing = 0.sp,
                )
            }
        }
    }
}

@Composable
private fun StableLoadCase(
    index: Int,
    case: ProductionCorpusCase,
    scene: GMResult<JsxGraphScene, String>,
) {
    val background =
        if (index % 2 == 0) Color.White else Color(0xFFF8FAF9)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .semantics {
                contentDescription = "stable-load-case:${case.id}"
            }
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = case.title,
            color = Color(0xFF17211C),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        )
        Text(
            text = case.scenario,
            color = Color(0xFF66716B),
            style = MaterialTheme.typography.bodySmall,
            letterSpacing = 0.sp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
        ) {
            when (scene) {
                is GMResult.Ok -> JsxGraphScenePreview(
                    scene = scene.value,
                    modifier = Modifier.fillMaxSize(),
                )
                is GMResult.Err -> Text(
                    text = scene.error,
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error,
                    letterSpacing = 0.sp,
                )
            }
        }
        HorizontalDivider(color = Color(0xFFDCE2DE))
    }
}
