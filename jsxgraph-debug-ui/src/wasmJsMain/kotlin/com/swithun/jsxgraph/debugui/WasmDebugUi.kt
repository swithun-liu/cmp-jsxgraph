/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
@file:OptIn(ExperimentalWasmJsInterop::class)

package com.swithun.jsxgraph.debugui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.WebElementView
import com.swithun.jsxgraph.core.GMResult
import kotlinx.browser.document
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop
import org.w3c.dom.HTMLIFrameElement
import org.w3c.dom.MessageEvent
import org.w3c.dom.events.Event

private const val OFFICIAL_MESSAGE_PREFIX = "cmp-jsxgraph:"
private const val OFFICIAL_READY_PREFIX = "${OFFICIAL_MESSAGE_PREFIX}ready:"
private const val OFFICIAL_ERROR_PREFIX = "${OFFICIAL_MESSAGE_PREFIX}error:"

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun OfficialJsxGraphDiagram(
    source: String,
    modifier: Modifier,
    onRenderResult: (OfficialRenderResult) -> Unit,
) {
    val currentOnRenderResult by rememberUpdatedState(onRenderResult)
    val frame = remember {
        (document.createElement("iframe") as HTMLIFrameElement).apply {
            title = "Official JSXGraph 1.13.3 rendering"
            setAttribute("sandbox", "allow-scripts allow-same-origin")
            style.border = "0"
            style.width = "100%"
            style.height = "100%"
            style.backgroundColor = "#fcfdfe"
        }
    }
    var frameGeneration by remember { mutableIntStateOf(0) }
    DisposableEffect(Unit) {
        frame.onload = {
            if (hasOfficialRenderer(frame)) {
                frameGeneration += 1
            } else {
                currentOnRenderResult(
                    OfficialRenderResult.Error(
                        "Official JSXGraph renderer did not initialize",
                    ),
                )
            }
        }
        val listener: (Event) -> Unit = { event ->
            val messageEvent = event as? MessageEvent
            if (
                messageEvent != null &&
                isFromOfficialFrame(messageEvent, frame) &&
                messageEvent.origin == window.location.origin
            ) {
                stringMessageData(messageEvent)?.let { message ->
                    when (val result = parseOfficialRenderMessage(message)) {
                        is GMResult.Ok -> {
                            result.value?.let(currentOnRenderResult)
                        }
                        is GMResult.Err -> currentOnRenderResult(
                            OfficialRenderResult.Error(result.error),
                        )
                    }
                }
            }
        }
        window.addEventListener("message", listener)
        onDispose {
            frame.onload = null
            window.removeEventListener("message", listener)
            frame.remove()
        }
    }
    LaunchedEffect(Unit) {
        frame.src = "official-jsxgraph.html"
    }
    LaunchedEffect(source) {
        currentOnRenderResult(OfficialRenderResult.Loading)
    }
    LaunchedEffect(source, frameGeneration) {
        if (
            frameGeneration > 0 &&
            !renderOfficialDiagram(frame, source)
        ) {
            currentOnRenderResult(
                OfficialRenderResult.Error(
                    "Official JSXGraph renderer became unavailable",
                ),
            )
        }
    }
    WebElementView(
        factory = { frame },
        modifier = modifier.fillMaxSize(),
        update = {},
    )
}

@JsFun("(frame) => typeof frame.contentWindow?.renderDiagram === 'function'")
private external fun hasOfficialRenderer(frame: HTMLIFrameElement): Boolean

@JsFun(
    "(frame, source) => {" +
        "const render = frame.contentWindow?.renderDiagram;" +
        "if (typeof render !== 'function') return false;" +
        "render(source);" +
        "return true;" +
        "}",
)
private external fun renderOfficialDiagram(
    frame: HTMLIFrameElement,
    source: String,
): Boolean

@JsFun("(event, frame) => event.source === frame.contentWindow")
private external fun isFromOfficialFrame(
    event: MessageEvent,
    frame: HTMLIFrameElement,
): Boolean

@JsFun("(event) => typeof event.data === 'string' ? event.data : null")
private external fun stringMessageData(event: MessageEvent): String?

private fun parseOfficialRenderMessage(
    message: String,
): GMResult<OfficialRenderResult?, String> {
    if (message.startsWith(OFFICIAL_ERROR_PREFIX)) {
        val error = message.removePrefix(OFFICIAL_ERROR_PREFIX)
            .ifBlank { "Official JSXGraph rendering failed" }
        return GMResult.Ok(OfficialRenderResult.Error(error))
    }
    if (!message.startsWith(OFFICIAL_READY_PREFIX)) {
        return GMResult.Ok(null)
    }

    val dimensions = message.removePrefix(OFFICIAL_READY_PREFIX)
        .split(':', limit = 2)
    if (dimensions.size != 2) {
        return GMResult.Err("Official JSXGraph returned an invalid size")
    }
    val width = dimensions[0].toFloatOrNull()
    val height = dimensions[1].toFloatOrNull()
    if (
        width == null ||
        !width.isFinite() ||
        width <= 0f ||
        height == null ||
        !height.isFinite() ||
        height <= 0f
    ) {
        return GMResult.Err("Official JSXGraph returned an invalid size")
    }
    return GMResult.Ok(OfficialRenderResult.Ready(width, height))
}
