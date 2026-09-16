package com.swithun.jsxgraph.debugui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

fun JsxGraphDebugViewController(): UIViewController =
    ComposeUIViewController {
        JsxGraphDebugApp()
    }

fun JsxGraphLoadViewController(): UIViewController =
    ComposeUIViewController {
        JsxGraphDebugApp(
            options = JsxGraphDebugOptions(
                initialDestination =
                    JsxGraphDebugDestination.StableLoad,
                autoRunLoadTest = true,
            ),
        )
    }

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
