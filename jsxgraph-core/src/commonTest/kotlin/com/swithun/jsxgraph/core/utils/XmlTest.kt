/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class XmlTest {
    @Test
    fun structuredTreeMatchesOfficialFixture() {
        val document = parse(FIXTURE_SOURCE)
        assertEquals(
            listOf(XmlNodeType.PROCESSING_INSTRUCTION_NODE, XmlNodeType.ELEMENT_NODE),
            document.childNodes.map(XmlNode::nodeType),
        )
        val instruction = assertIs<XmlProcessingInstruction>(
            document.childNodes[0],
        )
        assertEquals("probe", instruction.nodeName)
        assertEquals("fixture", instruction.nodeValue)
        assertEquals("fixture", instruction.data)

        val root = assertIs<XmlElement>(document.documentElement)
        assertEquals("root", root.nodeName)
        assertEquals("urn:probe", root.getAttribute("xmlns:p"))
        assertEquals("A & B", root.getAttribute("plain"))
        assertEquals("😀", root.getAttribute("numeric"))
        assertNull(root.getAttribute("missing"))
        assertEquals(
            listOf("xmlns:p", "plain", "numeric"),
            root.attributes.map(XmlAttribute::nodeName),
        )
        assertEquals(
            listOf(
                "p:item",
                "#text",
                "#comment",
                "#text",
                "#cdata-section",
                "#text",
                "group",
                "#text",
                "item",
                "#text",
            ),
            root.childNodes.map(XmlNode::nodeName),
        )
        assertEquals("p:item", root.firstChild?.nodeName)
        assertEquals("#text", root.firstChild?.nextSibling?.nodeName)
        assertEquals(
            "alpha < beta",
            root.getElementsByTagName("p:item").single().firstChild?.data,
        )
        assertEquals(
            "raw <xml> & value",
            assertIs<XmlCDataSection>(root.childNodes[4]).data,
        )
        assertEquals(
            "keep-comment",
            assertIs<XmlComment>(root.childNodes[2]).data,
        )
    }

    @Test
    fun descendantQueriesAndNamedNodeMapMatchOfficialFixture() {
        val root = assertIs<XmlElement>(
            parse(FIXTURE_SOURCE).documentElement,
        )
        assertEquals(
            listOf("one", "two", "three"),
            root.getElementsByTagName("item").map { element ->
                element.getAttribute("id")
            },
        )
        assertEquals(
            listOf("p:item", "group", "item", "nested", "item", "item"),
            root.getElementsByTagName("*").map(XmlNode::nodeName),
        )

        val prefixed = root.getElementsByTagName("p:item").single()
        assertEquals(1, prefixed.attributes.length)
        assertEquals("p:key", prefixed.attributes[0]?.nodeName)
        assertEquals("first", prefixed.attributes[0]?.value)
        assertEquals("first", prefixed.attributes["p:key"]?.value)
        assertNull(prefixed.attributes[1])
    }

    @Test
    fun doctypeAndNestedEntitiesMatchOfficialFixture() {
        val document = parse(
            "<!DOCTYPE root [" +
                "<!ENTITY custom \"custom-value\">" +
                "<!ENTITY nested \"&custom;:nested\">" +
                "]><root>&nested;</root>",
        )
        assertEquals(
            listOf(
                XmlNodeType.DOCUMENT_TYPE_NODE,
                XmlNodeType.ELEMENT_NODE,
            ),
            document.childNodes.map(XmlNode::nodeType),
        )
        assertEquals("root", document.childNodes[0].nodeName)
        assertEquals(
            "custom-value:nested",
            document.documentElement?.firstChild?.data,
        )
    }

    @Test
    fun malformedXmlFailuresAreStructured() {
        assertIs<GMResult.Err<XmlError.MismatchedTag>>(
            XML.parse("<root><open></root>"),
        )
        assertIs<GMResult.Err<XmlError.DuplicateAttribute>>(
            XML.parse("<root id=\"one\" id=\"two\"/>"),
        )
        assertIs<GMResult.Err<XmlError.InvalidEntity>>(
            XML.parse("<root>&missing;</root>"),
        )
        assertIs<GMResult.Err<XmlError.MultipleRootElements>>(
            XML.parse("<one/><two/>"),
        )
        assertIs<GMResult.Err<XmlError.UnexpectedToken>>(
            XML.parse("text<root/>"),
        )
        assertIs<GMResult.Err<XmlError.UnexpectedEnd>>(
            XML.parse("<root>"),
        )
        assertIs<GMResult.Err<XmlError.UnsupportedExternalEntity>>(
            XML.parse(
                "<!DOCTYPE root [" +
                    "<!ENTITY ext SYSTEM \"file:///not-read\">" +
                    "]><root>&ext;</root>",
            ),
        )
        assertIs<GMResult.Err<XmlError.UnsupportedParameterEntity>>(
            XML.parse(
                "<!DOCTYPE root [" +
                    "<!ENTITY % parameter \"value\">" +
                    "]><root/>",
            ),
        )
    }

    @Test
    fun parserResourceLimitsAreStructured() {
        assertIs<GMResult.Err<XmlError.InputLimitExceeded>>(
            XML.parse(
                input = "<root/>",
                limits = XmlLimits(maxInputCharacters = 6),
            ),
        )
        assertIs<GMResult.Err<XmlError.DepthLimitExceeded>>(
            XML.parse(
                input = "<root><child/></root>",
                limits = XmlLimits(maxDepth = 1),
            ),
        )
        assertIs<GMResult.Err<XmlError.NodeLimitExceeded>>(
            XML.parse(
                input = "<root/>",
                limits = XmlLimits(maxNodes = 1),
            ),
        )
        assertIs<GMResult.Err<XmlError.AttributeLimitExceeded>>(
            XML.parse(
                input = "<root id=\"one\"/>",
                limits = XmlLimits(maxAttributes = 0),
            ),
        )
        assertIs<GMResult.Err<XmlError.TextLimitExceeded>>(
            XML.parse(
                input = "<root>abc</root>",
                limits = XmlLimits(maxTextCharacters = 2),
            ),
        )
        assertIs<GMResult.Err<XmlError.EntityExpansionLimitExceeded>>(
            XML.parse(
                input = "<root>&amp;</root>",
                limits = XmlLimits(maxEntityExpansions = 0),
            ),
        )
    }

    @Test
    fun cleanWhitespacePreservesTheOfficialSiblingMutationBehavior() {
        val root = assertIs<XmlElement>(
            parse(
                "<root>\n  <first/>\n  <second/>\n</root>",
            ).documentElement,
        )
        assertEquals(
            listOf("first", "#text", "second", "#text"),
            root.childNodes.map(XmlNode::nodeName),
        )
    }

    private fun parse(source: String): XmlDocument =
        assertIs<GMResult.Ok<XmlDocument>>(XML.parse(source)).value

    companion object {
        private val FIXTURE_SOURCE = listOf(
            "<?probe fixture?>",
            "<root xmlns:p=\"urn:probe\" plain=\"A &amp; B\" " +
                "numeric=\"&#x1F600;\">",
            "  <p:item p:key=\"first\">alpha &lt; beta</p:item>",
            "  <!--keep-comment-->",
            "  <![CDATA[raw <xml> & value]]>",
            "  <group>",
            "    <item id=\"one\"/>",
            "    <nested><item id=\"two\"> value </item></nested>",
            "  </group>",
            "  <item id=\"three\"/>",
            "</root>",
        ).joinToString("\n")
    }
}
