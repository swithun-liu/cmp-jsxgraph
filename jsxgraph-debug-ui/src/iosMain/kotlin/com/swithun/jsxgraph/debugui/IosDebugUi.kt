package com.swithun.jsxgraph.debugui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun OfficialJsxGraphDiagram(
    source: String,
    modifier: Modifier,
    onRenderResult: (OfficialRenderResult) -> Unit,
) {
    UnavailableOfficialPreview(
        message = "Official JSXGraph comparison is currently available on Android.",
        modifier = modifier,
        onRenderResult = onRenderResult,
    )
}
