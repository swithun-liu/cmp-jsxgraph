/*
 * Copyright 2026 swithun.
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OptionsValidatorTest {
    @Test
    fun mirrorsUpstreamPrimitiveValidators() {
        assertTrue(OptionsValidator.validatePixel("12px"))
        assertFalse(OptionsValidator.validatePixel("-1px"))
        assertTrue(OptionsValidator.validateDisplay("internal"))
        assertFalse(OptionsValidator.validateDisplay("block"))
        assertTrue(OptionsValidator.validateColor("transparent"))
        assertFalse(OptionsValidator.validateColor(0))
        assertFalse(OptionsValidator.validatePointFace("triangleUp"))
        assertTrue(OptionsValidator.validatePointFace("triangleup"))
        assertTrue(OptionsValidator.validatePointFace("A"))
        assertTrue(OptionsValidator.validatePointFace("[]"))
        assertFalse(OptionsValidator.validatePointFace("hexagon"))
        assertTrue(OptionsValidator.validateNumber(3.0))
        assertTrue(OptionsValidator.validateNumber("3.25"))
        assertFalse(OptionsValidator.validateNumber("3.0"))
        assertFalse(OptionsValidator.validateNumber(Double.NaN))
        assertTrue(OptionsValidator.validateInteger(3.0000001))
        assertFalse(OptionsValidator.validateInteger(3.01))
        assertTrue(OptionsValidator.validateInteger(null))
        assertTrue(OptionsValidator.validateInteger(false))
        assertTrue(OptionsValidator.validateInteger(" 1 "))
        assertTrue(OptionsValidator.validateNotNegativeInteger("1e2"))
        assertFalse(OptionsValidator.validateNotNegativeInteger(-1))
        assertTrue(OptionsValidator.validatePositiveInteger(2))
        assertTrue(OptionsValidator.validatePositiveInteger(true))
        assertFalse(OptionsValidator.validatePositiveInteger(0))
        assertTrue(OptionsValidator.validateRenderer("vml"))
        assertFalse(OptionsValidator.validateRenderer("compose"))
        assertTrue(OptionsValidator.validatePositive(0.1))
        assertTrue(OptionsValidator.validatePositive("01"))
        assertFalse(OptionsValidator.validatePositive(0.0))
        assertTrue(OptionsValidator.validateNotNegative(0.0))
        assertFalse(OptionsValidator.validateNotNegative(-0.1))
    }

    @Test
    fun exposesTheCompleteLowercaseValidatorRegistry() {
        assertEquals(
            listOf(
                "attractordistance",
                "color",
                "display",
                "doadvancedplot",
                "draft",
                "drawlabels",
                "drawzero",
                "face",
                "factor",
                "fillcolor",
                "fillopacity",
                "firstarrow",
                "fontsize",
                "dash",
                "gridx",
                "gridy",
                "hasgrid",
                "highlightfillcolor",
                "highlightfillopacity",
                "highlightstrokecolor",
                "highlightstrokeopacity",
                "insertticks",
                "lastarrow",
                "layer",
                "majorheight",
                "minorheight",
                "minorticks",
                "minticksdistance",
                "numberpointshigh",
                "numberpointslow",
                "opacity",
                "radius",
                "rdpsmoothing",
                "renderer",
                "right",
                "showcopyright",
                "showinfobox",
                "shownavigation",
                "size",
                "snapsizex",
                "snapsizey",
                "snapwidth",
                "snaptogrid",
                "snatchdistance",
                "straightfirst",
                "straightlast",
                "stretch",
                "strokecolor",
                "strokeopacity",
                "strokewidth",
                "takefirst",
                "takesizefromfile",
                "to10",
                "toorigin",
                "translateto10",
                "translatetoorigin",
                "useasciimathml",
                "usedirection",
                "usemathjax",
                "withlabel",
                "withticks",
                "zoom",
            ),
            OptionsValidator.entries.keys.toList(),
        )
        assertTrue(
            OptionsValidator.entries["draft"] is
                OptionsValidatorEntry.Unvalidated,
        )
        assertTrue(OptionsValidator.validateProperty("draft", Any()) == true)
        assertTrue(OptionsValidator.validateProperty("layer", "01") == true)
        assertTrue(OptionsValidator.validateProperty("right", "12px") == true)
        assertTrue(OptionsValidator.validateProperty("missing", 1) == null)
    }
}
