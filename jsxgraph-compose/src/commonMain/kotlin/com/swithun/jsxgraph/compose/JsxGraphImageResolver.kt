/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.compose

import androidx.compose.ui.graphics.ImageBitmap
import com.swithun.jsxgraph.core.GMResult
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import org.jetbrains.compose.resources.decodeToImageBitmap

fun interface JsxGraphImageResolver {
    fun resolve(source: String): GMResult<ImageBitmap, JsxGraphImageResolveError>
}

sealed interface JsxGraphImageResolveError {
    val message: String

    data class UnsupportedSource(
        val source: String,
    ) : JsxGraphImageResolveError {
        override val message: String =
            "No image resolver is configured for '$source'"
    }

    data class EncodedDataTooLarge(
        val limit: Int,
        val actual: Int,
    ) : JsxGraphImageResolveError {
        override val message: String =
            "Decoded image data size $actual exceeds limit $limit"
    }

    data class InvalidDataUri(
        override val message: String,
    ) : JsxGraphImageResolveError

    data class DecodeFailure(
        override val message: String,
    ) : JsxGraphImageResolveError

    data class InvalidConfiguration(
        override val message: String,
    ) : JsxGraphImageResolveError
}

data class JsxGraphImageLoadError(
    val elementId: String,
    val source: String,
    val error: JsxGraphImageResolveError,
)

/**
 * Pure KMP default image resolver.
 *
 * External URLs and application resources remain caller-owned so the
 * renderer never acquires implicit network or filesystem access.
 */
class JsxGraphDataUriImageResolver(
    private val maxDecodedBytes: Int = DEFAULT_MAX_DECODED_BYTES,
) : JsxGraphImageResolver {
    @OptIn(ExperimentalEncodingApi::class)
    override fun resolve(
        source: String,
    ): GMResult<ImageBitmap, JsxGraphImageResolveError> {
        if (maxDecodedBytes < 0) {
            return GMResult.Err(
                JsxGraphImageResolveError.InvalidConfiguration(
                    "maxDecodedBytes must not be negative",
                ),
            )
        }
        if (!source.startsWith(DATA_IMAGE_PREFIX, ignoreCase = true)) {
            return GMResult.Err(
                JsxGraphImageResolveError.UnsupportedSource(source),
            )
        }
        val separator = source.indexOf(',')
        if (separator <= DATA_IMAGE_PREFIX.length) {
            return GMResult.Err(
                JsxGraphImageResolveError.InvalidDataUri(
                    "Image data URI has no metadata or payload separator",
                ),
            )
        }
        val metadata = source.substring(0, separator)
        if (!metadata.endsWith(";base64", ignoreCase = true)) {
            return GMResult.Err(
                JsxGraphImageResolveError.InvalidDataUri(
                    "Only base64 image data URIs are supported",
                ),
            )
        }
        val payload = source.substring(separator + 1)
        val maximumEncodedLength =
            ((maxDecodedBytes.toLong() + 2L) / 3L * 4L + 4L)
                .coerceAtMost(Int.MAX_VALUE.toLong())
                .toInt()
        if (payload.length > maximumEncodedLength) {
            return GMResult.Err(
                JsxGraphImageResolveError.EncodedDataTooLarge(
                    limit = maxDecodedBytes,
                    actual = decodedSizeUpperBound(payload.length),
                ),
            )
        }
        val bytes = try {
            Base64.Default.decode(payload)
        } catch (error: Exception) {
            return GMResult.Err(
                JsxGraphImageResolveError.InvalidDataUri(
                    error.message ?: "Invalid base64 image payload",
                ),
            )
        }
        if (bytes.size > maxDecodedBytes) {
            return GMResult.Err(
                JsxGraphImageResolveError.EncodedDataTooLarge(
                    limit = maxDecodedBytes,
                    actual = bytes.size,
                ),
            )
        }
        return try {
            GMResult.Ok(bytes.decodeToImageBitmap())
        } catch (error: Exception) {
            GMResult.Err(
                JsxGraphImageResolveError.DecodeFailure(
                    error.message ?: "Could not decode image data",
                ),
            )
        }
    }

    private fun decodedSizeUpperBound(encodedLength: Int): Int =
        (
            (encodedLength.toLong() + 3L) / 4L * 3L
            ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()

    companion object {
        const val DEFAULT_MAX_DECODED_BYTES: Int = 4 * 1024 * 1024
        private const val DATA_IMAGE_PREFIX = "data:image/"
    }
}

val DefaultJsxGraphImageResolver: JsxGraphImageResolver =
    JsxGraphDataUriImageResolver()
