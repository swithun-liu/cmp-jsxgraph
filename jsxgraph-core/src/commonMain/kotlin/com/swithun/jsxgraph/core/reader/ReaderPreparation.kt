/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/reader/{geonext,intergeo,geogebra,cinderella}.js ->
 * prepareString and related decoding helpers.
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.Base64
import com.swithun.jsxgraph.core.utils.Base64Error
import com.swithun.jsxgraph.core.utils.UTF8
import com.swithun.jsxgraph.core.utils.Unzip
import com.swithun.jsxgraph.core.utils.UnzipError
import com.swithun.jsxgraph.core.utils.UnzipLimits

internal enum class ReaderFormat {
    GEONEXT,
    INTERGEO,
    GEOGEBRA,
    CINDERELLA,
}

internal data class ReaderPreparationLimits(
    val maxSourceCharacters: Int = 16 * 1024 * 1024,
    val maxPreparedCharacters: Int = 64 * 1024 * 1024,
    val unzip: UnzipLimits = UnzipLimits(),
)

internal sealed interface ReaderPreparationError {
    data class InvalidLimits(
        val name: String,
        val value: Int,
    ) : ReaderPreparationError

    data class SourceLimitExceeded(
        val format: ReaderFormat,
        val limit: Int,
        val actual: Int,
    ) : ReaderPreparationError

    data class PreparedLimitExceeded(
        val format: ReaderFormat,
        val limit: Int,
        val actual: Int,
    ) : ReaderPreparationError

    data class Base64DecodingFailed(
        val format: ReaderFormat,
        val cause: Base64Error,
    ) : ReaderPreparationError

    data class ArchiveDecodingFailed(
        val format: ReaderFormat,
        val cause: UnzipError,
    ) : ReaderPreparationError

    data class EmptyArchive(
        val format: ReaderFormat,
    ) : ReaderPreparationError
}

internal object ReaderPreparation {
    /*
     * JSXGraph 1.13.3: src/reader/geonext.js ->
     * GeonextReader.decodeString / prepareString.
     */
    internal fun prepareGeonext(
        source: String,
        limits: ReaderPreparationLimits = ReaderPreparationLimits(),
    ): GMResult<String, ReaderPreparationError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        sourceLimit(
            format = ReaderFormat.GEONEXT,
            source = source,
            limits = limits,
        )?.let { return GMResult.Err(it) }

        val decoded =
            if ("GEONEXT" !in source) {
                val bytes = when (val result = Base64.decodeAsArray(source)) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        return GMResult.Err(
                            ReaderPreparationError.Base64DecodingFailed(
                                format = ReaderFormat.GEONEXT,
                                cause = result.error,
                            ),
                        )
                    }
                }
                val files = when (
                    val result = Unzip(bytes, limits.unzip).unzip()
                ) {
                    is GMResult.Ok -> result.value
                    is GMResult.Err -> {
                        return GMResult.Err(
                            ReaderPreparationError.ArchiveDecodingFailed(
                                format = ReaderFormat.GEONEXT,
                                cause = result.error,
                            ),
                        )
                    }
                }
                files.firstOrNull()?.content
                    ?: return GMResult.Err(
                        ReaderPreparationError.EmptyArchive(
                            ReaderFormat.GEONEXT,
                        ),
                    )
            } else {
                source
            }
        val prepared = fixGeonextXml(decoded)
        return preparedResult(
            format = ReaderFormat.GEONEXT,
            prepared = prepared,
            limits = limits,
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/intergeo.js ->
     * IntergeoReader.prepareString.
     */
    internal fun prepareIntergeo(
        source: String,
        limits: ReaderPreparationLimits = ReaderPreparationLimits(),
    ): GMResult<String, ReaderPreparationError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        sourceLimit(
            format = ReaderFormat.INTERGEO,
            source = source,
            limits = limits,
        )?.let { return GMResult.Err(it) }

        var prepared = source
        val isZip =
            prepared.startsWith(ZIP_SIGNATURE) ||
                UTF8.asciiCharCodeAt(prepared.take(1), 0) ==
                GZIP_FIRST_BYTE.toDouble()
        if (!isZip && !prepared.startsWith("<")) {
            prepared = when (val result = Base64.decode(prepared)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.Base64DecodingFailed(
                            format = ReaderFormat.INTERGEO,
                            cause = result.error,
                        ),
                    )
                }
            }
        }
        if (!prepared.startsWith("<")) {
            prepared = when (
                val result = Unzip(
                    binaryStringToBytes(prepared),
                    limits.unzip,
                ).unzipFile(INTERGEO_ENTRY)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.ArchiveDecodingFailed(
                            format = ReaderFormat.INTERGEO,
                            cause = result.error,
                        ),
                    )
                }
            }
        }
        return preparedResult(
            format = ReaderFormat.INTERGEO,
            prepared = prepared,
            limits = limits,
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/geogebra.js ->
     * GeogebraReader.prepareString.
     */
    internal fun prepareGeogebra(
        source: String,
        limits: ReaderPreparationLimits = ReaderPreparationLimits(),
    ): GMResult<String, ReaderPreparationError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        sourceLimit(
            format = ReaderFormat.GEOGEBRA,
            source = source,
            limits = limits,
        )?.let { return GMResult.Err(it) }

        var prepared = source
        var requiresUtf8Decoding = false
        val isString = !prepared.startsWith(ZIP_SIGNATURE)
        if (isString && !prepared.startsWith("<")) {
            val binaryDecoded = when (
                val result = Base64.decode(prepared)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.Base64DecodingFailed(
                            format = ReaderFormat.GEOGEBRA,
                            cause = result.error,
                        ),
                    )
                }
            }
            prepared =
                if (binaryDecoded.startsWith(ZIP_SIGNATURE)) {
                    binaryDecoded
                } else {
                    when (
                        val result = Base64.decode(
                            prepared,
                            utf8 = true,
                        )
                    ) {
                        is GMResult.Ok -> result.value
                        is GMResult.Err -> {
                            return GMResult.Err(
                                ReaderPreparationError.Base64DecodingFailed(
                                    format = ReaderFormat.GEOGEBRA,
                                    cause = result.error,
                                ),
                            )
                        }
                    }
                }
        }
        if (!prepared.startsWith("<")) {
            requiresUtf8Decoding = true
            prepared = when (
                val result = Unzip(
                    binaryStringToBytes(prepared),
                    limits.unzip,
                ).unzipFile(GEOGEBRA_ENTRY)
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.ArchiveDecodingFailed(
                            format = ReaderFormat.GEOGEBRA,
                            cause = result.error,
                        ),
                    )
                }
            }
        }
        prepared = geogebraUtf8Replace(
            if (requiresUtf8Decoding) UTF8.decode(prepared) else prepared,
        )
        return preparedResult(
            format = ReaderFormat.GEOGEBRA,
            prepared = prepared,
            limits = limits,
        )
    }

    /*
     * JSXGraph 1.13.3: src/reader/cinderella.js ->
     * CinderellaReader.prepareString.
     */
    internal fun prepareCinderella(
        source: String,
        isString: Boolean = false,
        limits: ReaderPreparationLimits = ReaderPreparationLimits(),
    ): GMResult<String, ReaderPreparationError> {
        validateLimits(limits)?.let { return GMResult.Err(it) }
        sourceLimit(
            format = ReaderFormat.CINDERELLA,
            source = source,
            limits = limits,
        )?.let { return GMResult.Err(it) }

        var prepared = source
        if (isString) {
            prepared = when (val result = Base64.decode(prepared)) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.Base64DecodingFailed(
                            format = ReaderFormat.CINDERELLA,
                            cause = result.error,
                        ),
                    )
                }
            }
        }
        if (!prepared.startsWith("<")) {
            val files = when (
                val result = Unzip(
                    binaryStringToBytes(prepared),
                    limits.unzip,
                ).unzip()
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> {
                    return GMResult.Err(
                        ReaderPreparationError.ArchiveDecodingFailed(
                            format = ReaderFormat.CINDERELLA,
                            cause = result.error,
                        ),
                    )
                }
            }
            prepared = files.firstOrNull()?.content
                ?: return GMResult.Err(
                    ReaderPreparationError.EmptyArchive(
                        ReaderFormat.CINDERELLA,
                    ),
                )
        }
        return preparedResult(
            format = ReaderFormat.CINDERELLA,
            prepared = prepared,
            limits = limits,
        )
    }

    // JSXGraph 1.13.3: src/reader/geogebra.js -> utf8replace.
    internal fun geogebraUtf8Replace(expression: String): String =
        expression
            .replace("\u03C0", "PI")
            .replace("\u00B2", "^2")
            .replace("\u00B3", "^3")
            .replace("\u225F", "==")
            .replace("\u2260", "!=")
            .replace("\u2264", "<=")
            .replace("\u2265", ">=")
            .replace("\u2227", "&&")
            .replace("\u2228", "//")

    // JSXGraph 1.13.3: src/reader/geonext.js -> fixXML.
    internal fun fixGeonextXml(source: String): String {
        val escaped = source
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
        val restoredTags = escaped.replace(
            GEONEXT_TAG_PATTERN,
            "<\$1>",
        )
        return restoredTags
            .replace(
                CONTENT_ARC_PATTERN,
                "\$1&lt;arc&gt;\$2",
            )
            .replace(
                MP_ARC_PATTERN,
                "\$1&lt;arc&gt;\$2",
            )
            .replace(
                MPX_ARC_PATTERN,
                "\$1&lt;arc&gt;\$2",
            )
    }

    private fun preparedResult(
        format: ReaderFormat,
        prepared: String,
        limits: ReaderPreparationLimits,
    ): GMResult<String, ReaderPreparationError> =
        if (prepared.length > limits.maxPreparedCharacters) {
            GMResult.Err(
                ReaderPreparationError.PreparedLimitExceeded(
                    format = format,
                    limit = limits.maxPreparedCharacters,
                    actual = prepared.length,
                ),
            )
        } else {
            GMResult.Ok(prepared)
        }

    private fun sourceLimit(
        format: ReaderFormat,
        source: String,
        limits: ReaderPreparationLimits,
    ): ReaderPreparationError.SourceLimitExceeded? =
        if (source.length > limits.maxSourceCharacters) {
            ReaderPreparationError.SourceLimitExceeded(
                format = format,
                limit = limits.maxSourceCharacters,
                actual = source.length,
            )
        } else {
            null
        }

    private fun validateLimits(
        limits: ReaderPreparationLimits,
    ): ReaderPreparationError.InvalidLimits? {
        val invalid = when {
            limits.maxSourceCharacters < 0 ->
                "maxSourceCharacters" to limits.maxSourceCharacters
            limits.maxPreparedCharacters < 0 ->
                "maxPreparedCharacters" to limits.maxPreparedCharacters
            else -> null
        }
        return invalid?.let { (name, value) ->
            ReaderPreparationError.InvalidLimits(name, value)
        }
    }

    private fun binaryStringToBytes(source: String): List<Int> =
        source.indices.map { index ->
            UTF8.asciiCharCodeAt(source, index).toInt()
        }

    private const val ZIP_SIGNATURE = "PK"
    private const val GZIP_FIRST_BYTE = 31
    private const val INTERGEO_ENTRY = "construction/intergeo.xml"
    private const val GEOGEBRA_ENTRY = "geogebra.xml"

    private val GEONEXT_TAGS = listOf(
        "active",
        "angle",
        "animate",
        "animated",
        "arc",
        "area",
        "arrow",
        "author",
        "autodigits",
        "axis",
        "back",
        "background",
        "board",
        "border",
        "bottom",
        "buttonsize",
        "cas",
        "circle",
        "color",
        "comment",
        "composition",
        "condition",
        "conditions",
        "content",
        "continuous",
        "control",
        "coord",
        "coordinates",
        "cross",
        "cs",
        "dash",
        "data",
        "description",
        "digits",
        "direction",
        "draft",
        "editable",
        "elements",
        "event",
        "file",
        "fill",
        "first",
        "firstarrow",
        "fix",
        "fontsize",
        "free",
        "full",
        "function",
        "functionx",
        "functiony",
        "GEONEXT",
        "graph",
        "grid",
        "group",
        "height",
        "id",
        "image",
        "info",
        "information",
        "input",
        "intersection",
        "item",
        "jsf",
        "label",
        "last",
        "lastarrow",
        "left",
        "lefttoolbar",
        "lighting",
        "line",
        "loop",
        "max",
        "maximized",
        "member",
        "middle",
        "midpoint",
        "min",
        "modifier",
        "modus",
        "mp",
        "mpx",
        "multi",
        "name",
        "onpolygon",
        "order",
        "origin",
        "output",
        "overline",
        "parametercurve",
        "parent",
        "point",
        "pointsnap",
        "polygon",
        "position",
        "radius",
        "radiusnum",
        "radiusvalue",
        "right",
        "section",
        "selectedlefttoolbar",
        "showconstruction",
        "showcoord",
        "showinfo",
        "showunit",
        "showx",
        "showy",
        "size",
        "slider",
        "snap",
        "speed",
        "src",
        "start",
        "stop",
        "straight",
        "stroke",
        "strokewidth",
        "style",
        "term",
        "text",
        "top",
        "trace",
        "tracecurve",
        "tracepoint",
        "traceslider",
        "type",
        "unit",
        "value",
        "VERSION",
        "vertex",
        "viewport",
        "visible",
        "width",
        "wot",
        "x",
        "xooy",
        "xval",
        "y",
        "yval",
        "zoom",
    )
    private val GEONEXT_TAG_PATTERN = Regex(
        "&lt;(/?(${GEONEXT_TAGS.joinToString("|")}))&gt;",
    )
    private val CONTENT_ARC_PATTERN =
        Regex("""(<content>.*)<arc>(.*</content>)""")
    private val MP_ARC_PATTERN =
        Regex("""(<mp>.*)<arc>(.*</mpx>)""")
    private val MPX_ARC_PATTERN =
        Regex("""(<mpx>.*)<arc>(.*</mpx>)""")
}
