/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.utils.XML
import com.swithun.jsxgraph.core.utils.XmlElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GeonextPropertiesTest {
    @Test
    fun pointPropertiesMatchOfficialFixture() {
        val properties = parseElement(index = 0).values

        assertEquals(text("3"), properties["strokewidth"])
        assertEquals(text("A"), properties["name"])
        assertEquals(text("P1"), properties["id"])
        assertEquals(text("#FF0000"), properties["strokeColor"])
        assertEquals(
            number(0.5019607843137255),
            properties["strokeOpacity"],
        )
        assertEquals(
            text("#00FF00"),
            properties["highlightStrokeColor"],
        )
        assertEquals(
            number(0.25098039215686274),
            properties["highlightStrokeOpacity"],
        )
        assertEquals(text("#FF0000"), properties["fillColor"])
        assertEquals(
            number(0.5019607843137255),
            properties["fillOpacity"],
        )
        assertEquals(
            text("#00FF00"),
            properties["highlightFillColor"],
        )
        assertEquals(
            number(0.25098039215686274),
            properties["highlightFillOpacity"],
        )
        assertEquals(text("#445566"), properties["labelColor"])
        assertEquals(flag(false), properties["withLabel"])
        assertEquals(number(0.0), properties["labelOpacity"])
        assertEquals(text("#778899"), properties["colorDraft"])
        assertEquals(text("#FF0000"), properties["colorStroke"])
        assertEquals(text("#112233"), properties["colorFill"])
        assertEquals(text("#445566"), properties["colorLabel"])
        assertEquals(flag(true), properties["visible"])
        assertEquals(flag(false), properties["trace"])
        assertEquals(flag(false), properties["draft"])
        assertEquals(text("1.5"), properties["x"])
        assertEquals(text("-2"), properties["y"])
        assertEquals(flag(false), properties["fixed"])
        assertEquals(text("3"), properties["strokeWidth"])
        assertEquals(text("circle"), properties["face"])
        assertEquals(number(4.0), properties["size"])
        assertEquals(flag(true), properties["straightFirst"])
        assertEquals(flag(true), properties["straightLast"])
        assertEquals(
            GeonextPropertyValue.Label(opacity = 0.0),
            properties["label"],
        )
        assertEquals(30, properties.size)
    }

    @Test
    fun linePropertiesMatchOfficialFixture() {
        val properties = parseElement(index = 1).values

        assertEquals(text("a"), properties["name"])
        assertEquals(text("L1"), properties["id"])
        assertEquals(text("#010203"), properties["strokeColor"])
        assertEquals(number(1.0), properties["strokeOpacity"])
        assertEquals(text("#040506"), properties["highlightStrokeColor"])
        assertEquals(
            number(0.5019607843137255),
            properties["highlightStrokeOpacity"],
        )
        assertEquals(text("#070809"), properties["fillColor"])
        assertEquals(
            number(0.25098039215686274),
            properties["fillOpacity"],
        )
        assertEquals(text("#0A0B0C"), properties["labelColor"])
        assertEquals(flag(true), properties["withLabel"])
        assertEquals(flag(false), properties["visible"])
        assertEquals(flag(true), properties["trace"])
        assertEquals(flag(true), properties["draft"])
        assertEquals(text("2"), properties["strokewidth"])
        assertEquals(text("P1"), properties["first"])
        assertEquals(text("gXOe0"), properties["last"])
        assertEquals(flag(false), properties["straightFirst"])
        assertEquals(flag(true), properties["straightLast"])
        assertEquals(text("2"), properties["strokeWidth"])
        assertEquals(text("plus"), properties["face"])
        assertEquals(number(3.0), properties["size"])
        assertEquals(
            GeonextPropertyValue.Label(opacity = 1.0),
            properties["label"],
        )
        assertEquals(29, properties.size)
    }

    @Test
    fun polygonGraphAndParameterCurvePropertiesFollowTheirBranches() {
        val polygon = parseExtendedElement(index = 0)
        assertEquals(
            listOf("P1", "P2", "P3", "P1"),
            polygon.lists[GeonextProperties.VERTICES_KEY],
        )
        assertEquals(3, polygon.borders.size)
        val firstBorder = polygon.borders.first().values
        assertEquals(text("B1"), firstBorder["id"])
        assertEquals(text("b1"), firstBorder["name"])
        assertEquals(flag(true), firstBorder["straightFirst"])
        assertEquals(flag(false), firstBorder["straightLast"])
        assertEquals(text("2"), firstBorder["strokeWidth"])
        assertEquals(text("#102030"), firstBorder["strokeColor"])
        assertEquals(number(1.0), firstBorder["strokeOpacity"])
        assertEquals(text("#304050"), firstBorder["fillColor"])
        assertEquals(
            number(0.25098039215686274),
            firstBorder["fillOpacity"],
        )
        assertEquals(text("#405060FF"), firstBorder["labelColor"])
        assertEquals(text("#506070FF"), firstBorder["colorDraft"])

        val graph = parseExtendedElement(index = 1).values
        assertEquals(text("x*x-1"), graph["function"])
        assertEquals(text("true"), graph["visible"])
        assertEquals(text("2"), graph["strokewidth"])
        assertEquals(null, graph["strokeWidth"])

        val parameterCurve = parseExtendedElement(index = 2).values
        assertEquals(text("2*t"), parameterCurve["functionx"])
        assertEquals(text("t*t"), parameterCurve["functiony"])
        assertEquals(text("-1"), parameterCurve["min"])
        assertEquals(text("2"), parameterCurve["max"])
        assertEquals(flag(true), parameterCurve["visible"])

        assertIs<GMResult.Err<GeonextPropertyError.PropertyLimitExceeded>>(
            GeonextProperties.parseElementProperties(
                source = EXTENDED_SOURCE,
                elementIndex = 0,
                limits = GeonextPropertyLimits(maxProperties = 10),
            ),
        )
    }

    @Test
    fun sliderTraceCurveAndGroupPropertiesFollowTheirBranches() {
        val slider = parseExtendedElement(index = 3).values
        assertEquals(text("2.5"), slider["x"])
        assertEquals(text("3"), slider["y"])
        assertEquals(text("L1"), slider["parent"])
        assertEquals(text("0.25"), slider["position"])
        assertEquals(flag(false), slider["fixed"])
        assertEquals(text("false"), slider["animateAnimated"])
        assertEquals(text("true"), slider["animateBack"])
        assertEquals(text("20"), slider["animateSpeed"])
        assertEquals(text("false"), slider["onpolygon"])

        val traceCurve = parseExtendedElement(index = 4).values
        assertEquals(text("S1"), traceCurve["tracepoint"])
        assertEquals(text("S1"), traceCurve["traceslider"])
        assertEquals(5, traceCurve.size)

        val group = parseExtendedElement(index = 5)
        assertEquals(
            listOf("P1", "P2", "S1"),
            group.lists[GeonextProperties.MEMBERS_KEY],
        )
    }

    @Test
    fun textPropertiesMatchOfficialMpFallbackAndDefaults() {
        val mpText = parseTextElement(index = 0).values
        assertEquals(text(""), mpText["name"])
        assertEquals(text("T1"), mpText["id"])
        assertEquals(text("1.25"), mpText["x"])
        assertEquals(text("-0.5"), mpText["y"])
        assertEquals(
            text(
                "Hello &lt;b class=\"strong\"&gt;" +
                    "world &amp;amp; all&lt;/b&gt;",
            ),
            mpText["mpStr"],
        )
        assertEquals(text("fallback"), mpText["content"])
        assertEquals(text("shown"), mpText["condition"])
        assertEquals(text("true"), mpText["fixed"])
        assertEquals(text("4"), mpText["autodigits"])
        assertEquals(text("true"), mpText["visible"])
        assertEquals(text("#405060"), mpText["colorLabel"])

        val fallbackText = parseTextElement(index = 1).values
        assertEquals(
            text("anchored &amp;amp; plain"),
            fallbackText["mpStr"],
        )
        assertEquals(text("L1"), fallbackText["parent"])
        assertEquals(text(""), fallbackText["condition"])
        assertEquals(flag(false), fallbackText["fixed"])
        assertEquals(number(2.0), fallbackText["autodigits"])
        assertEquals(text("false"), fallbackText["visible"])
    }

    @Test
    fun compositionPropertiesPreserveInputsAndOrderedOutputs() {
        val properties =
            assertIs<GMResult.Ok<GeonextElementProperties>>(
                GeonextProperties.parseElementProperties(
                    COMPOSITION_SOURCE,
                ),
            ).value

        assertEquals(text("COMP1"), properties.values["id"])
        assertEquals(text(""), properties.values["name"])
        assertEquals(text("210160"), properties.values["type"])
        assertEquals(
            listOf("P1", "L1"),
            properties.lists[GeonextProperties.INPUTS_KEY],
        )

        val point = properties.outputs.getValue(
            GeonextProperties.compositionOutputKey(0),
        ).values
        assertEquals(text("Q"), point["name"])
        assertEquals(text("Q1"), point["id"])
        assertEquals(text("false"), point["fixed"])

        val line = properties.outputs.getValue(
            GeonextProperties.compositionOutputKey(1),
        ).values
        assertEquals(text("n"), line["name"])
        assertEquals(text("N1"), line["id"])
        assertEquals(flag(false), line["straightFirst"])
        assertEquals(flag(true), line["straightLast"])
    }

    @Test
    fun viewportAndOriginIdsMatchOfficialFixture() {
        val document = assertIs<GMResult.Ok<*>>(XML.parse(FIXTURE_SOURCE))
            .value
        val root = assertIs<com.swithun.jsxgraph.core.utils.XmlDocument>(
            document,
        )
        val coordinates = assertIs<XmlElement>(
            root.getElementsByTagName("coordinates").first(),
        )

        assertEquals(
            listOf(-4.0, 5.0, 6.0, -7.0),
            GeonextProperties.readViewPort(coordinates),
        )
        assertEquals(
            listOf("board-gOOe0", "board-gXOe0", "other"),
            listOf("gOOe0", "gXOe0", "other").map { id ->
                GeonextProperties.changeOriginId("board-", id)
            },
        )
    }

    @Test
    fun preparationXmlLookupAndLimitsFailuresAreStructured() {
        assertIs<GMResult.Err<GeonextPropertyError.PreparationFailed>>(
            GeonextProperties.parseElementProperties("abc"),
        )
        assertIs<GMResult.Err<GeonextPropertyError.XmlParsingFailed>>(
            GeonextProperties.parseElementProperties("<GEONEXT>"),
        )
        assertIs<GMResult.Err<GeonextPropertyError.MissingSection>>(
            GeonextProperties.parseElementProperties("<GEONEXT/>"),
        )
        assertIs<GMResult.Err<GeonextPropertyError.MissingElement>>(
            GeonextProperties.parseElementProperties(
                "<GEONEXT><elements/></GEONEXT>",
            ),
        )
        assertIs<GMResult.Err<GeonextPropertyError.UnsupportedElementType>>(
            GeonextProperties.parseElementProperties(
                "<GEONEXT><elements><circle/></elements></GEONEXT>",
            ),
        )
        assertIs<GMResult.Err<GeonextPropertyError.ElementLimitExceeded>>(
            GeonextProperties.parseElementProperties(
                source = FIXTURE_SOURCE,
                limits = GeonextPropertyLimits(maxElements = 1),
            ),
        )
        assertIs<GMResult.Err<GeonextPropertyError.InvalidLimits>>(
            GeonextProperties.parseElementProperties(
                source = FIXTURE_SOURCE,
                limits = GeonextPropertyLimits(maxProperties = -1),
            ),
        )
    }

    private fun parseElement(index: Int): GeonextElementProperties =
        assertIs<GMResult.Ok<GeonextElementProperties>>(
            GeonextProperties.parseElementProperties(
                source = FIXTURE_SOURCE,
                elementIndex = index,
            ),
        ).value

    private fun parseExtendedElement(index: Int): GeonextElementProperties =
        assertIs<GMResult.Ok<GeonextElementProperties>>(
            GeonextProperties.parseElementProperties(
                source = EXTENDED_SOURCE,
                elementIndex = index,
            ),
        ).value

    private fun parseTextElement(index: Int): GeonextElementProperties =
        assertIs<GMResult.Ok<GeonextElementProperties>>(
            GeonextProperties.parseElementProperties(
                source = TEXT_SOURCE,
                elementIndex = index,
            ),
        ).value

    private fun text(value: String): GeonextPropertyValue.Text =
        GeonextPropertyValue.Text(value)

    private fun number(value: Double): GeonextPropertyValue.Number =
        GeonextPropertyValue.Number(value)

    private fun flag(value: Boolean): GeonextPropertyValue.Flag =
        GeonextPropertyValue.Flag(value)

    private companion object {
        val FIXTURE_SOURCE = listOf(
            "<GEONEXT><elements>",
            "<point><name>A</name><id>P1</id>",
            "<data><x>1.5</x><y>-2</y></data>",
            "<active>true</active><area>9</area><dash>0</dash>",
            "<draft>false</draft><visible>true</visible><trace>false</trace>",
            "<width>3</width><color>",
            "<stroke>#FF000080</stroke><lighting>#00FF0040</lighting>",
            "<fill>#11223320</fill><label>#44556600</label>",
            "<draft>#778899FF</draft></color>",
            "<showinfo>false</showinfo><showcoord>false</showcoord>",
            "<fix>false</fix><style>6</style>",
            "</point>",
            "<line><name>a</name><id>L1</id>",
            "<data><first>P1</first><last>gXOe0</last></data>",
            "<straight><first>false</first><last>true</last></straight>",
            "<draft>true</draft><visible>false</visible><trace>true</trace>",
            "<strokewidth>2</strokewidth><color>",
            "<stroke>#010203FF</stroke><lighting>#04050680</lighting>",
            "<fill>#07080940</fill><label>#0A0B0CFF</label>",
            "<draft>#0D0E0FFF</draft></color><style>11</style>",
            "</line>",
            "</elements><coordinates><viewport>",
            "<left>-4junk</left><top>5</top><right>6</right>",
            "<bottom>-7</bottom>",
            "</viewport></coordinates></GEONEXT>",
        ).joinToString("")

        val EXTENDED_SOURCE = listOf(
            "<GEONEXT><elements>",
            "<polygon><name>triangle</name><id>POLY1</id>",
            "<data><vertex>P1</vertex><vertex>P2</vertex>",
            "<vertex>P3</vertex><vertex>P1</vertex></data>",
            "<visible>true</visible>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color>",
            "<border><name>b1</name><id>B1</id>",
            "<straight><first>true</first><last>false</last></straight>",
            "<strokewidth>2</strokewidth><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></border>",
            "<border><name>b2</name><id>B2</id>",
            "<straight><first>false</first><last>true</last></straight>",
            "<width>3</width><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></border>",
            "<border><name>b3</name><id>B3</id>",
            "<straight><first>true</first><last>true</last></straight>",
            "<strokewidth>4</strokewidth><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></border>",
            "</polygon>",
            "<graph><name>f</name><id>G1</id>",
            "<data><function>x*x-1</function></data>",
            "<visible>true</visible><strokewidth>2</strokewidth>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></graph>",
            "<parametercurve><name>q</name><id>PC1</id>",
            "<functionx>2*t</functionx><functiony>t*t</functiony>",
            "<min>-1</min><max>2</max>",
            "<visible>true</visible><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></parametercurve>",
            "<slider><name>S</name><id>S1</id>",
            "<data><x>2.5</x><y>3</y><parent>L1</parent>",
            "<position>0.25</position></data>",
            "<visible>true</visible><trace>false</trace>",
            "<fix>false</fix><style>3</style>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color>",
            "<animate><animated>false</animated><back>true</back>",
            "<speed>20</speed><start>0</start><stop>1</stop>",
            "<loop>2</loop><direction>false</direction></animate>",
            "<onpolygon>false</onpolygon></slider>",
            "<tracecurve><name>trace</name><id>TC1</id>",
            "<tracepoint>S1</tracepoint><traceslider>S1</traceslider>",
            "</tracecurve>",
            "<group><name>pair</name><id>GR1</id><data>",
            "<member>P1</member><member>P2</member><member>S1</member>",
            "</data><color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></group>",
            "</elements></GEONEXT>",
        ).joinToString("")

        val TEXT_SOURCE = listOf(
            "<GEONEXT><elements>",
            "<text><id>T1</id><data><x>1.25</x><y>-0.5</y>",
            "<mp>Hello <b class=\"strong\">world &amp; all</b></mp>",
            "<content>fallback</content><parent></parent></data>",
            "<condition>shown</condition><fix>true</fix><digits>4</digits>",
            "<visible>true</visible><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></text>",
            "<text><id>T2</id><data><x>0.5</x><y>1</y>",
            "<content>anchored &amp; plain</content>",
            "<parent>L1</parent></data><condition></condition>",
            "<visible>false</visible><trace>false</trace>",
            "<color><stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill><label>#405060FF</label>",
            "<draft>#506070FF</draft></color></text>",
            "</elements></GEONEXT>",
        ).joinToString("")

        val COMPOSITION_SOURCE = listOf(
            "<GEONEXT><elements><composition>",
            "<name></name><id>COMP1</id>",
            "<data><type>210160</type><input>P1</input>",
            "<input>L1</input></data>",
            "<active>true</active><visible>true</visible>",
            "<output><name>Q</name><id>Q1</id>",
            "<visible>true</visible><trace>false</trace>",
            "<fix>false</fix><style>1</style>",
            "<color><stroke>#808080FF</stroke>",
            "<lighting>#00FF00FF</lighting>",
            "<fill>#00FF0032</fill><label>#000000FF</label>",
            "<draft>#C8C8C864</draft></color></output>",
            "<output><name>n</name><id>N1</id>",
            "<visible>true</visible><trace>false</trace>",
            "<straight><first>false</first><last>true</last></straight>",
            "<color><stroke>#0000FFFF</stroke>",
            "<lighting>#00FFFFFF</lighting>",
            "<fill>#00FF0032</fill><label>#00000000</label>",
            "<draft>#C8C8C864</draft></color></output>",
            "</composition></elements></GEONEXT>",
        ).joinToString("")
    }
}
