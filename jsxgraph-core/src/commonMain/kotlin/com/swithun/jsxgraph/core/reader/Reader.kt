/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/jxg.js -> registerReader,
 * src/reader/file.js -> JXG.FileReader.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult

internal interface ReaderDomainError

internal sealed interface ReaderError {
    data class UnknownFormat(
        val format: String,
    ) : ReaderError

    data class ConstructionFailed(
        val format: String,
        val message: String?,
    ) : ReaderError

    data class ReadFailed(
        val format: String,
        val message: String?,
    ) : ReaderError

    data class CallbackFailed(
        val format: String,
        val message: String?,
    ) : ReaderError

    data class DomainFailure(
        val cause: ReaderDomainError,
    ) : ReaderError

    data class UnsupportedSource(
        val source: FileContentSource,
    ) : ReaderError
}

internal sealed interface FileContentSource {
    data class RemoteUrl(
        val url: String,
    ) : FileContentSource

    data class LocalBlob(
        val identifier: String? = null,
    ) : FileContentSource
}

internal fun interface JsxGraphReader {
    fun read(): GMResult<Unit, ReaderError>
}

internal fun interface JsxGraphReaderFactory<B> {
    fun create(
        board: B,
        source: String,
    ): GMResult<JsxGraphReader, ReaderError>
}

internal class ReaderRegistry<B> {
    private val readers =
        linkedMapOf<String, JsxGraphReaderFactory<B>>()

    // JSXGraph 1.13.3: src/jxg.js -> JXG.registerReader.
    internal fun registerReader(
        reader: JsxGraphReaderFactory<B>,
        extensions: Iterable<String>,
    ) {
        for (extension in extensions) {
            val normalized = extension.lowercase()
            if (normalized !in readers) {
                readers[normalized] = reader
            }
        }
    }

    internal fun readerFor(
        format: String,
    ): JsxGraphReaderFactory<B>? = readers[format.lowercase()]
}

internal object FileReader {
    // JSXGraph 1.13.3: src/reader/file.js -> JXG.FileReader.parseString.
    internal fun <B> parseString(
        source: String,
        board: B,
        format: String,
        registry: ReaderRegistry<B>,
        callback: ((B) -> Unit)? = null,
    ): GMResult<Unit, ReaderError> {
        val normalizedFormat = format.lowercase()
        val factory = registry.readerFor(normalizedFormat)

        if (factory != null) {
            val created = try {
                factory.create(board, source)
            } catch (cause: Exception) {
                return GMResult.Err(
                    ReaderError.ConstructionFailed(
                        format = normalizedFormat,
                        message = cause.message,
                    ),
                )
            }
            val reader = when (created) {
                is GMResult.Ok -> created.value
                is GMResult.Err -> return created
            }
            val read = try {
                reader.read()
            } catch (cause: Exception) {
                return GMResult.Err(
                    ReaderError.ReadFailed(
                        format = normalizedFormat,
                        message = cause.message,
                    ),
                )
            }
            if (read is GMResult.Err) {
                return read
            }
        } else if (normalizedFormat != JESSIE_CODE_FORMAT) {
            return GMResult.Err(
                ReaderError.UnknownFormat(normalizedFormat),
            )
        }

        if (callback != null) {
            try {
                callback(board)
            } catch (cause: Exception) {
                return GMResult.Err(
                    ReaderError.CallbackFailed(
                        format = normalizedFormat,
                        message = cause.message,
                    ),
                )
            }
        }
        return GMResult.Ok(Unit)
    }

    /*
     * JSXGraph's remote XMLHttpRequest and browser Blob/FileReader paths
     * have no portable commonMain equivalent. Platform loaders can obtain
     * text and then call parseString.
     */
    internal fun parseFileContent(
        source: FileContentSource,
    ): GMResult<Nothing, ReaderError> =
        GMResult.Err(ReaderError.UnsupportedSource(source))

    private const val JESSIE_CODE_FORMAT = "jessiecode"
}
