/*
 * Kotlin translation of JSXGraph.
 * Upstream: src/utils/xml.js
 * Copyright 2008-2026 Matthias Ehmann, Michael Gerhaeuser, Carsten Miller,
 * Bianca Valentin, Alfred Wassermann, and Peter Wilfahrt.
 * Used under the MIT License option.
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult

object XmlNodeType {
    const val ELEMENT_NODE = 1
    const val ATTRIBUTE_NODE = 2
    const val TEXT_NODE = 3
    const val CDATA_SECTION_NODE = 4
    const val PROCESSING_INSTRUCTION_NODE = 7
    const val COMMENT_NODE = 8
    const val DOCUMENT_NODE = 9
    const val DOCUMENT_TYPE_NODE = 10
}

data class XmlLimits(
    val maxInputCharacters: Int = 16 * 1024 * 1024,
    val maxDepth: Int = 256,
    val maxNodes: Int = 1_000_000,
    val maxAttributes: Int = 1_000_000,
    val maxTextCharacters: Int = 64 * 1024 * 1024,
    val maxEntityExpansions: Int = 100_000,
    val maxEntityDepth: Int = 32,
)

sealed interface XmlError {
    data class InputLimitExceeded(
        val limit: Int,
        val actual: Int,
    ) : XmlError

    data class DepthLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : XmlError

    data class NodeLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : XmlError

    data class AttributeLimitExceeded(
        val limit: Int,
        val requested: Int,
    ) : XmlError

    data class TextLimitExceeded(
        val limit: Int,
        val requested: Long,
    ) : XmlError

    data class EntityExpansionLimitExceeded(
        val limit: Int,
    ) : XmlError

    data class EntityDepthLimitExceeded(
        val limit: Int,
        val entity: String,
    ) : XmlError

    data class InvalidCharacter(
        val offset: Int,
        val codePoint: Int,
    ) : XmlError

    data class UnexpectedEnd(
        val offset: Int,
        val context: String,
    ) : XmlError

    data class UnexpectedToken(
        val offset: Int,
        val expected: String,
    ) : XmlError

    data class InvalidName(
        val offset: Int,
    ) : XmlError

    data class DuplicateAttribute(
        val offset: Int,
        val name: String,
    ) : XmlError

    data class InvalidEntity(
        val offset: Int,
        val name: String,
    ) : XmlError

    data class RecursiveEntity(
        val name: String,
    ) : XmlError

    data class UnsupportedExternalEntity(
        val name: String,
    ) : XmlError

    data class UnsupportedParameterEntity(
        val offset: Int,
    ) : XmlError

    data class UnsupportedMarkupEntity(
        val name: String,
    ) : XmlError

    data class InvalidComment(
        val offset: Int,
    ) : XmlError

    data class ReservedProcessingInstructionTarget(
        val offset: Int,
    ) : XmlError

    data class MismatchedTag(
        val offset: Int,
        val expected: String,
        val actual: String,
    ) : XmlError

    data class MultipleRootElements(
        val offset: Int,
    ) : XmlError

    data object MissingRootElement : XmlError
}

sealed class XmlNode protected constructor(
    val nodeType: Int,
    val nodeName: String,
    open val nodeValue: String?,
) {
    private val mutableChildNodes = mutableListOf<XmlNode>()

    var parentNode: XmlNode? = null
        internal set

    open val data: String?
        get() = null

    open val attributes: XmlNamedNodeMap?
        get() = null

    val childNodes: List<XmlNode>
        get() = mutableChildNodes

    val firstChild: XmlNode?
        get() = mutableChildNodes.firstOrNull()

    val nextSibling: XmlNode?
        get() {
            val parent = parentNode ?: return null
            val index = parent.mutableChildNodes.indexOf(this)
            return if (index >= 0) {
                parent.mutableChildNodes.getOrNull(index + 1)
            } else {
                null
            }
        }

    open fun getElementsByTagName(name: String): List<XmlElement> {
        val result = mutableListOf<XmlElement>()
        collectElementsByTagName(name, result)
        return result
    }

    internal fun appendChild(child: XmlNode) {
        child.parentNode = this
        mutableChildNodes += child
    }

    internal fun removeChild(child: XmlNode): Boolean {
        val index = mutableChildNodes.indexOf(child)
        if (index < 0) {
            return false
        }
        mutableChildNodes.removeAt(index)
        child.parentNode = null
        return true
    }

    private fun collectElementsByTagName(
        name: String,
        result: MutableList<XmlElement>,
    ) {
        for (child in mutableChildNodes) {
            if (child is XmlElement) {
                if (name == "*" || child.nodeName == name) {
                    result += child
                }
                child.collectElementsByTagName(name, result)
            }
        }
    }
}

class XmlDocument internal constructor() : XmlNode(
    nodeType = XmlNodeType.DOCUMENT_NODE,
    nodeName = "#document",
    nodeValue = null,
) {
    val documentElement: XmlElement?
        get() = childNodes.firstOrNull { node ->
            node.nodeType == XmlNodeType.ELEMENT_NODE
        } as? XmlElement
}

class XmlElement internal constructor(
    name: String,
    attributeList: List<XmlAttribute>,
) : XmlNode(
    nodeType = XmlNodeType.ELEMENT_NODE,
    nodeName = name,
    nodeValue = null,
) {
    override val attributes = XmlNamedNodeMap(attributeList)

    fun getAttribute(name: String): String? =
        attributes.getNamedItem(name)?.value
}

class XmlAttribute internal constructor(
    name: String,
    val value: String,
) : XmlNode(
    nodeType = XmlNodeType.ATTRIBUTE_NODE,
    nodeName = name,
    nodeValue = value,
)

class XmlText internal constructor(
    override val data: String,
) : XmlNode(
    nodeType = XmlNodeType.TEXT_NODE,
    nodeName = "#text",
    nodeValue = data,
)

class XmlCDataSection internal constructor(
    override val data: String,
) : XmlNode(
    nodeType = XmlNodeType.CDATA_SECTION_NODE,
    nodeName = "#cdata-section",
    nodeValue = data,
)

class XmlProcessingInstruction internal constructor(
    target: String,
    override val data: String,
) : XmlNode(
    nodeType = XmlNodeType.PROCESSING_INSTRUCTION_NODE,
    nodeName = target,
    nodeValue = data,
)

class XmlComment internal constructor(
    override val data: String,
) : XmlNode(
    nodeType = XmlNodeType.COMMENT_NODE,
    nodeName = "#comment",
    nodeValue = data,
)

class XmlDocumentType internal constructor(
    name: String,
) : XmlNode(
    nodeType = XmlNodeType.DOCUMENT_TYPE_NODE,
    nodeName = name,
    nodeValue = null,
)

class XmlNamedNodeMap internal constructor(
    attributes: List<XmlAttribute>,
) : Iterable<XmlAttribute> {
    private val values = attributes.toList()

    val length: Int
        get() = values.size

    operator fun get(index: Int): XmlAttribute? = values.getOrNull(index)

    operator fun get(name: String): XmlAttribute? = getNamedItem(name)

    fun getNamedItem(name: String): XmlAttribute? =
        values.firstOrNull { attribute -> attribute.nodeName == name }

    override fun iterator(): Iterator<XmlAttribute> = values.iterator()

    fun toList(): List<XmlAttribute> = values
}

object XML {
    /*
     * JSXGraph 1.13.3: src/utils/xml.js ->
     * JXG.XML.cleanWhitespace.
     *
     * The sibling lookup intentionally happens after removal. Browser DOM
     * clears the removed node's parent, so the upstream loop stops at the
     * first removed whitespace node at each level.
     */
    fun cleanWhitespace(element: XmlNode) {
        var current = element.firstChild
        while (current != null) {
            if (
                current.nodeType == XmlNodeType.TEXT_NODE &&
                current.nodeValue?.contains(NON_WHITESPACE) != true
            ) {
                element.removeChild(current)
            } else if (current.nodeType == XmlNodeType.ELEMENT_NODE) {
                cleanWhitespace(current)
            }
            current = current.nextSibling
        }
    }

    // JSXGraph 1.13.3: src/utils/xml.js -> JXG.XML.parse.
    fun parse(
        input: String,
        limits: XmlLimits = XmlLimits(),
    ): GMResult<XmlDocument, XmlError> {
        if (input.length > limits.maxInputCharacters) {
            return GMResult.Err(
                XmlError.InputLimitExceeded(
                    limit = limits.maxInputCharacters,
                    actual = input.length,
                ),
            )
        }

        val normalized = input
            .replace("\r\n", "\n")
            .replace('\r', '\n')
        when (val validation = validateCharacters(normalized)) {
            is GMResult.Ok -> Unit
            is GMResult.Err -> return validation
        }
        return when (val parsed = Parser(normalized, limits).parse()) {
            is GMResult.Ok -> {
                cleanWhitespace(parsed.value)
                parsed
            }
            is GMResult.Err -> parsed
        }
    }

    private fun validateCharacters(
        input: String,
    ): GMResult<Unit, XmlError> {
        var index = 0
        while (index < input.length) {
            val first = input[index].code
            val codePoint: Int
            val width: Int
            if (first in HIGH_SURROGATE_START..HIGH_SURROGATE_END) {
                val second = input.getOrNull(index + 1)?.code
                if (second == null || second !in LOW_SURROGATE_START..LOW_SURROGATE_END) {
                    return GMResult.Err(
                        XmlError.InvalidCharacter(index, first),
                    )
                }
                codePoint =
                    0x10000 +
                    ((first - HIGH_SURROGATE_START) shl 10) +
                    (second - LOW_SURROGATE_START)
                width = 2
            } else {
                if (first in LOW_SURROGATE_START..LOW_SURROGATE_END) {
                    return GMResult.Err(
                        XmlError.InvalidCharacter(index, first),
                    )
                }
                codePoint = first
                width = 1
            }
            if (!isXmlCharacter(codePoint)) {
                return GMResult.Err(
                    XmlError.InvalidCharacter(index, codePoint),
                )
            }
            index += width
        }
        return GMResult.Ok(Unit)
    }

    private fun isXmlCharacter(codePoint: Int): Boolean =
        codePoint == 0x09 ||
            codePoint == 0x0A ||
            codePoint == 0x0D ||
            codePoint in 0x20..0xD7FF ||
            codePoint in 0xE000..0xFFFD ||
            codePoint in 0x10000..0x10FFFF

    private class Parser(
        private val source: String,
        private val limits: XmlLimits,
    ) {
        private var index = 0
        private var nodeCount = 0
        private var attributeCount = 0
        private var textCharacterCount = 0L
        private var entityExpansionCount = 0
        private val entities = mutableMapOf(
            "amp" to "&",
            "lt" to "<",
            "gt" to ">",
            "apos" to "'",
            "quot" to "\"",
        )
        private val externalEntities = mutableSetOf<String>()

        fun parse(): GMResult<XmlDocument, XmlError> {
            if (source.startsWith(BYTE_ORDER_MARK)) {
                index += 1
            }
            val declarationStart = index
            when (val nodeReservation = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return nodeReservation
            }
            val document = XmlDocument()
            var rootSeen = false
            var doctypeSeen = false

            while (index < source.length) {
                if (source[index] != '<') {
                    val textOffset = index
                    val text = readUntilMarkup()
                    if (text.any { character -> !isMarkupWhitespace(character) }) {
                        return GMResult.Err(
                            XmlError.UnexpectedToken(
                                offset = textOffset,
                                expected = "document markup",
                            ),
                        )
                    }
                    // Browser XML DOM does not expose document-level
                    // whitespace as child text nodes.
                    continue
                }

                when {
                    source.startsWith(XML_DECLARATION_START, index) &&
                        index == declarationStart -> {
                        when (val result = skipXmlDeclaration()) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(COMMENT_START, index) -> {
                        when (val result = parseComment()) {
                            is GMResult.Ok -> document.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(PROCESSING_INSTRUCTION_START, index) -> {
                        when (val result = parseProcessingInstruction()) {
                            is GMResult.Ok -> document.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(DOCTYPE_START, index) -> {
                        if (rootSeen || doctypeSeen) {
                            return GMResult.Err(
                                XmlError.UnexpectedToken(
                                    offset = index,
                                    expected = "one doctype before the root element",
                                ),
                            )
                        }
                        when (val result = parseDocumentType()) {
                            is GMResult.Ok -> {
                                document.appendChild(result.value)
                                doctypeSeen = true
                            }
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(CDATA_START, index) ||
                        source.startsWith(CLOSING_TAG_START, index) ||
                        source.startsWith(DECLARATION_START, index) -> {
                        return GMResult.Err(
                            XmlError.UnexpectedToken(
                                offset = index,
                                expected = "a document element",
                            ),
                        )
                    }

                    else -> {
                        if (rootSeen) {
                            return GMResult.Err(
                                XmlError.MultipleRootElements(index),
                            )
                        }
                        when (val result = parseElement(depth = 1)) {
                            is GMResult.Ok -> {
                                document.appendChild(result.value)
                                rootSeen = true
                            }
                            is GMResult.Err -> return result
                        }
                    }
                }
            }

            return if (rootSeen) {
                GMResult.Ok(document)
            } else {
                GMResult.Err(XmlError.MissingRootElement)
            }
        }

        private fun parseElement(
            depth: Int,
        ): GMResult<XmlElement, XmlError> {
            if (depth > limits.maxDepth) {
                return GMResult.Err(
                    XmlError.DepthLimitExceeded(
                        limit = limits.maxDepth,
                        requested = depth,
                    ),
                )
            }
            val elementStart = index
            index += 1
            val name = when (val result = parseName()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val attributes = mutableListOf<XmlAttribute>()
            val attributeNames = mutableSetOf<String>()

            while (index < source.length) {
                val whitespaceStart = index
                skipWhitespace()
                when {
                    source.startsWith(EMPTY_TAG_END, index) -> {
                        index += EMPTY_TAG_END.length
                        when (val result = reserveNode()) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                        return GMResult.Ok(XmlElement(name, attributes))
                    }

                    source[index] == '>' -> {
                        index += 1
                        break
                    }

                    whitespaceStart == index -> {
                        return GMResult.Err(
                            XmlError.UnexpectedToken(
                                offset = index,
                                expected = "whitespace, '>', or '/>'",
                            ),
                        )
                    }

                    else -> {
                        val attributeOffset = index
                        val attributeName = when (val result = parseName()) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        if (!attributeNames.add(attributeName)) {
                            return GMResult.Err(
                                XmlError.DuplicateAttribute(
                                    offset = attributeOffset,
                                    name = attributeName,
                                ),
                            )
                        }
                        skipWhitespace()
                        if (source.getOrNull(index) != '=') {
                            return GMResult.Err(
                                XmlError.UnexpectedToken(
                                    offset = index,
                                    expected = "'='",
                                ),
                            )
                        }
                        index += 1
                        skipWhitespace()
                        val quote = source.getOrNull(index)
                        if (quote != '"' && quote != '\'') {
                            return GMResult.Err(
                                XmlError.UnexpectedToken(
                                    offset = index,
                                    expected = "a quoted attribute value",
                                ),
                            )
                        }
                        index += 1
                        val valueStart = index
                        while (
                            index < source.length &&
                            source[index] != quote
                        ) {
                            if (source[index] == '<') {
                                return GMResult.Err(
                                    XmlError.UnexpectedToken(
                                        offset = index,
                                        expected = "attribute text",
                                    ),
                                )
                            }
                            index += 1
                        }
                        if (index >= source.length) {
                            return GMResult.Err(
                                XmlError.UnexpectedEnd(
                                    offset = index,
                                    context = "attribute '$attributeName'",
                                ),
                            )
                        }
                        val rawValue = source.substring(valueStart, index)
                        index += 1
                        val value = when (
                            val result = decodeEntities(
                                raw = rawValue,
                                sourceOffset = valueStart,
                                normalizeAttributeWhitespace = true,
                            )
                        ) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        when (val result = reserveAttribute()) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                        when (val result = reserveText(value.length)) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                        attributes += XmlAttribute(attributeName, value)
                    }
                }
            }

            if (index > source.length) {
                return GMResult.Err(
                    XmlError.UnexpectedEnd(
                        offset = source.length,
                        context = "element '$name'",
                    ),
                )
            }
            when (val result = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            val element = XmlElement(name, attributes)

            while (index < source.length) {
                when {
                    source.startsWith(CLOSING_TAG_START, index) -> {
                        val closeOffset = index
                        index += CLOSING_TAG_START.length
                        val closingName = when (val result = parseName()) {
                            is GMResult.Ok -> result.value
                            is GMResult.Err -> return result
                        }
                        skipWhitespace()
                        if (source.getOrNull(index) != '>') {
                            return GMResult.Err(
                                XmlError.UnexpectedToken(
                                    offset = index,
                                    expected = "'>'",
                                ),
                            )
                        }
                        index += 1
                        if (closingName != name) {
                            return GMResult.Err(
                                XmlError.MismatchedTag(
                                    offset = closeOffset,
                                    expected = name,
                                    actual = closingName,
                                ),
                            )
                        }
                        return GMResult.Ok(element)
                    }

                    source.startsWith(COMMENT_START, index) -> {
                        when (val result = parseComment()) {
                            is GMResult.Ok -> element.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(CDATA_START, index) -> {
                        when (val result = parseCData()) {
                            is GMResult.Ok -> element.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(PROCESSING_INSTRUCTION_START, index) -> {
                        when (val result = parseProcessingInstruction()) {
                            is GMResult.Ok -> element.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    source.startsWith(DECLARATION_START, index) -> {
                        return GMResult.Err(
                            XmlError.UnexpectedToken(
                                offset = index,
                                expected = "element content",
                            ),
                        )
                    }

                    source[index] == '<' -> {
                        when (val result = parseElement(depth + 1)) {
                            is GMResult.Ok -> element.appendChild(result.value)
                            is GMResult.Err -> return result
                        }
                    }

                    else -> {
                        val textStart = index
                        val rawText = readUntilMarkup()
                        if (rawText.contains(CDATA_END)) {
                            return GMResult.Err(
                                XmlError.UnexpectedToken(
                                    offset = textStart + rawText.indexOf(CDATA_END),
                                    expected = "character data without ']]>'",
                                ),
                            )
                        }
                        when (
                            val result = appendText(
                                parent = element,
                                rawText = rawText,
                                sourceOffset = textStart,
                            )
                        ) {
                            is GMResult.Ok -> Unit
                            is GMResult.Err -> return result
                        }
                    }
                }
            }

            return GMResult.Err(
                XmlError.UnexpectedEnd(
                    offset = source.length,
                    context = "element '$name' opened at $elementStart",
                ),
            )
        }

        private fun appendText(
            parent: XmlNode,
            rawText: String,
            sourceOffset: Int,
        ): GMResult<Unit, XmlError> {
            if (rawText.isEmpty()) {
                return GMResult.Ok(Unit)
            }
            val text = when (
                val result = decodeEntities(
                    raw = rawText,
                    sourceOffset = sourceOffset,
                    normalizeAttributeWhitespace = false,
                )
            ) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            when (val result = reserveText(text.length)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            parent.appendChild(XmlText(text))
            return GMResult.Ok(Unit)
        }

        private fun parseComment(): GMResult<XmlComment, XmlError> {
            val contentStart = index + COMMENT_START.length
            val end = source.indexOf(COMMENT_END, contentStart)
            if (end < 0) {
                return GMResult.Err(
                    XmlError.UnexpectedEnd(
                        offset = source.length,
                        context = "comment",
                    ),
                )
            }
            val content = source.substring(contentStart, end)
            if (content.contains("--") || content.endsWith("-")) {
                return GMResult.Err(
                    XmlError.InvalidComment(contentStart),
                )
            }
            index = end + COMMENT_END.length
            when (val result = reserveText(content.length)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(XmlComment(content))
        }

        private fun parseCData(): GMResult<XmlCDataSection, XmlError> {
            val contentStart = index + CDATA_START.length
            val end = source.indexOf(CDATA_END, contentStart)
            if (end < 0) {
                return GMResult.Err(
                    XmlError.UnexpectedEnd(
                        offset = source.length,
                        context = "CDATA section",
                    ),
                )
            }
            val content = source.substring(contentStart, end)
            index = end + CDATA_END.length
            when (val result = reserveText(content.length)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(XmlCDataSection(content))
        }

        private fun parseProcessingInstruction():
            GMResult<XmlProcessingInstruction, XmlError> {
            val instructionOffset = index
            index += PROCESSING_INSTRUCTION_START.length
            val target = when (val result = parseName()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            if (target.lowercase() == "xml") {
                return GMResult.Err(
                    XmlError.ReservedProcessingInstructionTarget(
                        instructionOffset,
                    ),
                )
            }
            val data = when {
                source.startsWith(PROCESSING_INSTRUCTION_END, index) -> ""
                source.getOrNull(index)?.let(::isMarkupWhitespace) == true -> {
                    skipWhitespace()
                    val end = source.indexOf(PROCESSING_INSTRUCTION_END, index)
                    if (end < 0) {
                        return GMResult.Err(
                            XmlError.UnexpectedEnd(
                                offset = source.length,
                                context = "processing instruction '$target'",
                            ),
                        )
                    }
                    source.substring(index, end).also {
                        index = end
                    }
                }
                else -> {
                    return GMResult.Err(
                        XmlError.UnexpectedToken(
                            offset = index,
                            expected = "processing instruction data or '?>'",
                        ),
                    )
                }
            }
            index += PROCESSING_INSTRUCTION_END.length
            when (val result = reserveText(data.length)) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            when (val result = reserveNode()) {
                is GMResult.Ok -> Unit
                is GMResult.Err -> return result
            }
            return GMResult.Ok(XmlProcessingInstruction(target, data))
        }

        private fun skipXmlDeclaration(): GMResult<Unit, XmlError> {
            val end = source.indexOf(PROCESSING_INSTRUCTION_END, index)
            if (end < 0) {
                return GMResult.Err(
                    XmlError.UnexpectedEnd(
                        offset = source.length,
                        context = "XML declaration",
                    ),
                )
            }
            index = end + PROCESSING_INSTRUCTION_END.length
            return GMResult.Ok(Unit)
        }

        private fun parseDocumentType():
            GMResult<XmlDocumentType, XmlError> {
            val doctypeOffset = index
            index += DOCTYPE_START.length
            if (source.getOrNull(index)?.let(::isMarkupWhitespace) != true) {
                return GMResult.Err(
                    XmlError.UnexpectedToken(
                        offset = index,
                        expected = "whitespace after DOCTYPE",
                    ),
                )
            }
            skipWhitespace()
            val name = when (val result = parseName()) {
                is GMResult.Ok -> result.value
                is GMResult.Err -> return result
            }
            val declarationBodyStart = index
            var subsetDepth = 0
            var quote: Char? = null
            while (index < source.length) {
                val character = source[index]
                if (quote != null) {
                    if (character == quote) {
                        quote = null
                    }
                } else {
                    when (character) {
                        '\'', '"' -> quote = character
                        '[' -> subsetDepth += 1
                        ']' -> {
                            if (subsetDepth == 0) {
                                return GMResult.Err(
                                    XmlError.UnexpectedToken(
                                        offset = index,
                                        expected = "a balanced DOCTYPE subset",
                                    ),
                                )
                            }
                            subsetDepth -= 1
                        }
                        '>' -> {
                            if (subsetDepth == 0) {
                                val body = source.substring(
                                    declarationBodyStart,
                                    index,
                                )
                                when (
                                    val result = readEntityDeclarations(
                                        body = body,
                                        sourceOffset = declarationBodyStart,
                                    )
                                ) {
                                    is GMResult.Ok -> Unit
                                    is GMResult.Err -> return result
                                }
                                index += 1
                                when (val result = reserveNode()) {
                                    is GMResult.Ok -> Unit
                                    is GMResult.Err -> return result
                                }
                                return GMResult.Ok(XmlDocumentType(name))
                            }
                        }
                    }
                }
                index += 1
            }
            return GMResult.Err(
                XmlError.UnexpectedEnd(
                    offset = source.length,
                    context = "DOCTYPE opened at $doctypeOffset",
                ),
            )
        }

        private fun readEntityDeclarations(
            body: String,
            sourceOffset: Int,
        ): GMResult<Unit, XmlError> {
            val parameterMatch = PARAMETER_ENTITY_PATTERN.find(body)
            if (parameterMatch != null) {
                return GMResult.Err(
                    XmlError.UnsupportedParameterEntity(
                        sourceOffset + parameterMatch.range.first,
                    ),
                )
            }
            for (match in EXTERNAL_ENTITY_PATTERN.findAll(body)) {
                externalEntities += match.groupValues[1]
            }
            for (match in INTERNAL_ENTITY_PATTERN.findAll(body)) {
                val name = match.groupValues[1]
                val doubleQuoted = match.groupValues[2]
                val singleQuoted = match.groupValues[3]
                if (name !in entities && name !in externalEntities) {
                    entities[name] = if (doubleQuoted.isNotEmpty()) {
                        doubleQuoted
                    } else {
                        singleQuoted
                    }
                }
            }
            return GMResult.Ok(Unit)
        }

        private fun decodeEntities(
            raw: String,
            sourceOffset: Int,
            normalizeAttributeWhitespace: Boolean,
        ): GMResult<String, XmlError> {
            val output = StringBuilder(raw.length)
            var rawIndex = 0
            while (rawIndex < raw.length) {
                val character = raw[rawIndex]
                if (character != '&') {
                    output.append(
                        if (
                            normalizeAttributeWhitespace &&
                            isMarkupWhitespace(character)
                        ) {
                            ' '
                        } else {
                            character
                        },
                    )
                    rawIndex += 1
                    continue
                }

                val semicolon = raw.indexOf(';', rawIndex + 1)
                if (semicolon < 0) {
                    return GMResult.Err(
                        XmlError.InvalidEntity(
                            offset = sourceOffset + rawIndex,
                            name = raw.substring(rawIndex + 1),
                        ),
                    )
                }
                val reference = raw.substring(rawIndex + 1, semicolon)
                when (
                    val result = decodeEntityReference(
                        reference = reference,
                        sourceOffset = sourceOffset + rawIndex,
                        stack = emptyList(),
                    )
                ) {
                    is GMResult.Ok -> output.append(result.value)
                    is GMResult.Err -> return result
                }
                rawIndex = semicolon + 1
            }
            return GMResult.Ok(output.toString())
        }

        private fun decodeEntityReference(
            reference: String,
            sourceOffset: Int,
            stack: List<String>,
        ): GMResult<String, XmlError> {
            entityExpansionCount += 1
            if (entityExpansionCount > limits.maxEntityExpansions) {
                return GMResult.Err(
                    XmlError.EntityExpansionLimitExceeded(
                        limits.maxEntityExpansions,
                    ),
                )
            }
            if (reference.startsWith("#")) {
                val hexadecimal = reference.startsWith("#x") ||
                    reference.startsWith("#X")
                val digits = if (hexadecimal) {
                    reference.drop(2)
                } else {
                    reference.drop(1)
                }
                val codePoint = digits.toIntOrNull(if (hexadecimal) 16 else 10)
                if (
                    digits.isEmpty() ||
                    codePoint == null ||
                    !isXmlCharacter(codePoint)
                ) {
                    return GMResult.Err(
                        XmlError.InvalidEntity(sourceOffset, reference),
                    )
                }
                return GMResult.Ok(codePointToString(codePoint))
            }
            if (reference in externalEntities) {
                return GMResult.Err(
                    XmlError.UnsupportedExternalEntity(reference),
                )
            }
            val replacement = entities[reference]
                ?: return GMResult.Err(
                    XmlError.InvalidEntity(sourceOffset, reference),
                )
            if (reference in DEFAULT_ENTITIES) {
                return GMResult.Ok(replacement)
            }
            if (reference in stack) {
                return GMResult.Err(XmlError.RecursiveEntity(reference))
            }
            if (stack.size >= limits.maxEntityDepth) {
                return GMResult.Err(
                    XmlError.EntityDepthLimitExceeded(
                        limit = limits.maxEntityDepth,
                        entity = reference,
                    ),
                )
            }
            if (replacement.contains('<')) {
                return GMResult.Err(
                    XmlError.UnsupportedMarkupEntity(reference),
                )
            }

            val output = StringBuilder(replacement.length)
            var replacementIndex = 0
            while (replacementIndex < replacement.length) {
                if (replacement[replacementIndex] != '&') {
                    output.append(replacement[replacementIndex])
                    replacementIndex += 1
                    continue
                }
                val semicolon = replacement.indexOf(';', replacementIndex + 1)
                if (semicolon < 0) {
                    return GMResult.Err(
                        XmlError.InvalidEntity(sourceOffset, reference),
                    )
                }
                val nestedReference = replacement.substring(
                    replacementIndex + 1,
                    semicolon,
                )
                when (
                    val result = decodeEntityReference(
                        reference = nestedReference,
                        sourceOffset = sourceOffset,
                        stack = stack + reference,
                    )
                ) {
                    is GMResult.Ok -> output.append(result.value)
                    is GMResult.Err -> return result
                }
                replacementIndex = semicolon + 1
            }
            return GMResult.Ok(output.toString())
        }

        private fun parseName(): GMResult<String, XmlError> {
            val start = index
            val first = source.getOrNull(index)
            if (first == null || !isNameStart(first)) {
                return GMResult.Err(XmlError.InvalidName(index))
            }
            index += 1
            while (
                index < source.length &&
                isNameCharacter(source[index])
            ) {
                index += 1
            }
            return GMResult.Ok(source.substring(start, index))
        }

        private fun readUntilMarkup(): String {
            val start = index
            while (index < source.length && source[index] != '<') {
                index += 1
            }
            return source.substring(start, index)
        }

        private fun skipWhitespace() {
            while (
                index < source.length &&
                isMarkupWhitespace(source[index])
            ) {
                index += 1
            }
        }

        private fun reserveNode(): GMResult<Unit, XmlError> {
            val requested = nodeCount + 1
            if (requested > limits.maxNodes) {
                return GMResult.Err(
                    XmlError.NodeLimitExceeded(
                        limit = limits.maxNodes,
                        requested = requested,
                    ),
                )
            }
            nodeCount = requested
            return GMResult.Ok(Unit)
        }

        private fun reserveAttribute(): GMResult<Unit, XmlError> {
            val requested = attributeCount + 1
            if (requested > limits.maxAttributes) {
                return GMResult.Err(
                    XmlError.AttributeLimitExceeded(
                        limit = limits.maxAttributes,
                        requested = requested,
                    ),
                )
            }
            attributeCount = requested
            return GMResult.Ok(Unit)
        }

        private fun reserveText(
            length: Int,
        ): GMResult<Unit, XmlError> {
            val requested = textCharacterCount + length
            if (requested > limits.maxTextCharacters) {
                return GMResult.Err(
                    XmlError.TextLimitExceeded(
                        limit = limits.maxTextCharacters,
                        requested = requested,
                    ),
                )
            }
            textCharacterCount = requested
            return GMResult.Ok(Unit)
        }
    }

    private fun isMarkupWhitespace(character: Char): Boolean =
        character == ' ' ||
            character == '\t' ||
            character == '\n' ||
            character == '\r'

    private fun isNameStart(character: Char): Boolean =
        character == ':' ||
            character == '_' ||
            character in 'A'..'Z' ||
            character in 'a'..'z' ||
            character.code >= 0xC0 &&
            character.code !in HIGH_SURROGATE_START..LOW_SURROGATE_END

    private fun isNameCharacter(character: Char): Boolean =
        isNameStart(character) ||
            character == '-' ||
            character == '.' ||
            character in '0'..'9' ||
            character == '\u00B7'

    private fun codePointToString(codePoint: Int): String {
        if (codePoint <= 0xFFFF) {
            return codePoint.toChar().toString()
        }
        val adjusted = codePoint - 0x10000
        val high = (HIGH_SURROGATE_START + (adjusted shr 10)).toChar()
        val low = (LOW_SURROGATE_START + (adjusted and 0x3FF)).toChar()
        return "$high$low"
    }

    private val NON_WHITESPACE = Regex("\\S")
    private val DEFAULT_ENTITIES = setOf(
        "amp",
        "lt",
        "gt",
        "apos",
        "quot",
    )
    private val PARAMETER_ENTITY_PATTERN =
        Regex("""<!ENTITY\s+%""")
    private val EXTERNAL_ENTITY_PATTERN = Regex(
        """<!ENTITY\s+([A-Za-z_:][A-Za-z0-9_.:-]*)\s+""" +
            """(?:SYSTEM|PUBLIC)\b""",
    )
    private val INTERNAL_ENTITY_PATTERN = Regex(
        """<!ENTITY\s+([A-Za-z_:][A-Za-z0-9_.:-]*)\s+""" +
            """(?:"([^"]*)"|'([^']*)')\s*>""",
    )

    private const val BYTE_ORDER_MARK = "\uFEFF"
    private const val XML_DECLARATION_START = "<?xml"
    private const val PROCESSING_INSTRUCTION_START = "<?"
    private const val PROCESSING_INSTRUCTION_END = "?>"
    private const val COMMENT_START = "<!--"
    private const val COMMENT_END = "-->"
    private const val CDATA_START = "<![CDATA["
    private const val CDATA_END = "]]>"
    private const val DOCTYPE_START = "<!DOCTYPE"
    private const val DECLARATION_START = "<!"
    private const val CLOSING_TAG_START = "</"
    private const val EMPTY_TAG_END = "/>"
    private const val HIGH_SURROGATE_START = 0xD800
    private const val HIGH_SURROGATE_END = 0xDBFF
    private const val LOW_SURROGATE_START = 0xDC00
    private const val LOW_SURROGATE_END = 0xDFFF
}
