/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsxGraphTicksTest {
    @Test
    fun fixedSegmentTicksPreserveSourceLabelIndexes() {
        val definition = ticksDefinition(
            parent = JsxGraphTicksParent2D.Line(
                point1 = JsxGraphPoint2D(-2.0, 0.0),
                point2 = JsxGraphPoint2D(2.0, 0.0),
                straightFirst = false,
                straightLast = false,
                axis = false,
            ),
            fixedTicks = listOf(-10.0, -1.0, 0.0, 1.0, 10.0),
            fixedLabels = listOf(
                "out-left",
                "minus",
                "zero",
                "plus",
                "out-right",
            ),
            drawLabels = true,
        )

        val resolved = resolve(definition)

        assertEquals(1, resolved.paths.size)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-1.0, 0.1),
                JsxGraphPoint2D(-1.0, 0.0),
                JsxGraphPoint2D(-1.0, -0.1),
            ),
            resolved.paths.single().points,
        )
        assertEquals(
            listOf(
                JsxGraphTickLabel(
                    position = JsxGraphPoint2D(-0.8, 0.0),
                    content = "plus",
                ),
            ),
            resolved.labels,
        )
    }

    @Test
    fun equidistantSegmentUsesTicksDistanceAndExcludesBoundaries() {
        val definition = ticksDefinition(
            parent = JsxGraphTicksParent2D.Line(
                point1 = JsxGraphPoint2D(-2.0, 0.0),
                point2 = JsxGraphPoint2D(2.0, 0.0),
                straightFirst = false,
                straightLast = false,
                axis = false,
            ),
            fixedTicks = null,
            drawZero = true,
            minorTicks = 0,
            ticksDistance = 1.0,
            drawLabels = true,
        )

        val resolved = resolve(definition)

        assertEquals(
            listOf(-1.0, 0.0, 1.0),
            resolved.paths.map { it.points[1].x },
        )
        assertTrue(resolved.paths.all(JsxGraphTickPath::major))
        assertEquals(
            listOf("1", "2", "3"),
            resolved.labels.map(JsxGraphTickLabel::content),
        )
        assertEquals(
            listOf(-0.8, 0.2, 1.2),
            resolved.labels.map { it.position.x },
        )
    }

    @Test
    fun curveTickPathsMatchOfficialEndpointAndDerivativeNormals() {
        val definition = ticksDefinition(
            parent = JsxGraphTicksParent2D.Curve(
                locations = listOf(
                    JsxGraphCurveTickLocation(
                        base = JsxGraphPoint2D(-2.0, 4.0),
                        normal = JsxGraphPoint2D(1.75, 0.5),
                        major = true,
                        label = "left",
                    ),
                    JsxGraphCurveTickLocation(
                        base = JsxGraphPoint2D(-1.0, 1.0),
                        normal = JsxGraphPoint2D(2.0, 1.0),
                        major = true,
                        label = "inside",
                    ),
                    JsxGraphCurveTickLocation(
                        base = JsxGraphPoint2D(2.0, 4.0),
                        normal = JsxGraphPoint2D(-1.25, 0.5),
                        major = true,
                        label = "right",
                    ),
                ),
            ),
            fixedTicks = listOf(0.0, 1.0, 4.0),
            fixedLabels = listOf("left", "inside", "right"),
            drawLabels = true,
        )

        val resolved = resolve(definition)

        assertPoint(
            expectedX = -1.9038476052359176,
            expectedY = 4.027472112789738,
            actual = resolved.paths[0].points[0],
        )
        assertPoint(
            expectedX = -2.0961523947640824,
            expectedY = 3.972527887210262,
            actual = resolved.paths[0].points[2],
        )
        assertPoint(
            expectedX = -0.9105572809000084,
            expectedY = 1.044721359549996,
            actual = resolved.paths[1].points[0],
        )
        assertPoint(
            expectedX = 1.907152330911474,
            expectedY = 4.03713906763541,
            actual = resolved.paths[2].points[0],
        )
        assertEquals(
            listOf("left", "inside", "right"),
            resolved.labels.map(JsxGraphTickLabel::content),
        )
    }

    @Test
    fun finiteFacesAndEndingsMatchOfficialPaths() {
        val parent = JsxGraphTicksParent2D.Line(
            point1 = JsxGraphPoint2D(-4.0, 0.0),
            point2 = JsxGraphPoint2D(4.0, 0.0),
            straightFirst = false,
            straightLast = false,
            axis = false,
        )
        val base = ticksDefinition(
            parent = parent,
            fixedTicks = listOf(4.0),
        ).copy(majorHeight = 20.0)

        val greater = resolve(base.copy(face = ">")).paths.single()
        assertPoint(
            expectedX = -0.1414213562373095,
            expectedY = 0.1414213562373095,
            actual = greater.points[0],
        )
        assertPoint(
            expectedX = -0.1414213562373095,
            expectedY = -0.1414213562373095,
            actual = greater.points[2],
        )

        val less = resolve(base.copy(face = "<")).paths.single()
        assertPoint(
            expectedX = 0.1414213562373095,
            expectedY = 0.1414213562373095,
            actual = less.points[0],
        )
        assertPoint(
            expectedX = 0.1414213562373095,
            expectedY = -0.1414213562373095,
            actual = less.points[2],
        )

        val endings = resolve(
            base.copy(majorTickEndings = listOf(2.0, -1.0)),
        ).paths.single()
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 0.2),
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(0.0, 0.0),
            ),
            endings.points,
        )
    }

    @Test
    fun infiniteTickEndingsMatchOfficialSides() {
        val base = ticksDefinition(
            parent = JsxGraphTicksParent2D.Line(
                point1 = JsxGraphPoint2D(-4.0, 0.0),
                point2 = JsxGraphPoint2D(4.0, 0.0),
                straightFirst = false,
                straightLast = false,
                axis = false,
            ),
            fixedTicks = listOf(4.0),
        ).copy(
            majorHeight = -1.0,
            majorTickEndings = listOf(1.0, 0.0),
        )

        val oneSide = resolve(
            base.copy(ignoreInfiniteTickEndings = false),
        ).paths.single()
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 5.0),
                JsxGraphPoint2D(0.0, 0.0),
            ),
            oneSide.points,
        )

        val bothSides = resolve(
            base.copy(ignoreInfiniteTickEndings = true),
        ).paths.single()
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 5.0),
                JsxGraphPoint2D(0.0, -5.0),
            ),
            bothSides.points,
        )
    }

    @Test
    fun polarTicksUseViewportRadiusBeyondFiniteParent() {
        val definition = ticksDefinition(
            parent = JsxGraphTicksParent2D.Line(
                point1 = JsxGraphPoint2D(0.0, 0.0),
                point2 = JsxGraphPoint2D(1.0, 0.0),
                straightFirst = false,
                straightLast = false,
                axis = false,
            ),
            fixedTicks = listOf(1.0, 2.0, 8.0),
        ).copy(type = "polar")

        val resolved = resolve(definition)

        assertEquals(2, resolved.paths.size)
        assertTrue(resolved.paths.all { it.points.size == 181 })
        assertPoint(1.0, 0.0, resolved.paths[0].points[0])
        assertPoint(0.0, 1.0, resolved.paths[0].points[45])
        assertPoint(-1.0, 0.0, resolved.paths[0].points[90])
        assertPoint(2.0, 0.0, resolved.paths[1].points[0])
        assertPoint(0.0, 2.0, resolved.paths[1].points[45])
        assertPoint(-2.0, 0.0, resolved.paths[1].points[90])
    }

    @Test
    fun invalidViewportOrScaleProducesNoGeometry() {
        val definition = ticksDefinition(
            parent = JsxGraphTicksParent2D.Line(
                point1 = JsxGraphPoint2D(-2.0, 0.0),
                point2 = JsxGraphPoint2D(2.0, 0.0),
                straightFirst = false,
                straightLast = false,
                axis = false,
            ),
            fixedTicks = listOf(1.0),
        )

        assertEquals(
            JsxGraphResolvedTicks(emptyList(), emptyList()),
            definition.resolve(
                visibleLeft = -5.0,
                visibleTop = 5.0,
                visibleRight = 5.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 0.0,
                cssPixelsPerUnitY = 50.0,
            ),
        )
        assertEquals(
            JsxGraphResolvedTicks(emptyList(), emptyList()),
            definition.resolve(
                visibleLeft = 5.0,
                visibleTop = 5.0,
                visibleRight = -5.0,
                visibleBottom = -5.0,
                cssPixelsPerUnitX = 50.0,
                cssPixelsPerUnitY = 50.0,
            ),
        )
    }

    private fun resolve(
        definition: JsxGraphTicks2D,
    ): JsxGraphResolvedTicks =
        definition.resolve(
            visibleLeft = -5.0,
            visibleTop = 5.0,
            visibleRight = 5.0,
            visibleBottom = -5.0,
            cssPixelsPerUnitX = 50.0,
            cssPixelsPerUnitY = 50.0,
        )

    private fun ticksDefinition(
        parent: JsxGraphTicksParent2D,
        fixedTicks: List<Double>?,
        fixedLabels: List<String?> = emptyList(),
        drawZero: Boolean = false,
        minorTicks: Int = 4,
        ticksDistance: Double = 1.0,
        drawLabels: Boolean = false,
    ): JsxGraphTicks2D =
        JsxGraphTicks2D(
            parent = parent,
            fixedTicks = fixedTicks,
            fixedLabels = fixedLabels,
            anchor = "left",
            drawZero = drawZero,
            insertTicks = false,
            minTicksDistance = 10.0,
            minorHeight = 4.0,
            majorHeight = 10.0,
            tickEndings = listOf(1.0, 1.0),
            majorTickEndings = listOf(1.0, 1.0),
            ignoreInfiniteTickEndings = true,
            minorTicks = minorTicks,
            ticksPerLabel = null,
            scale = 1.0,
            scaleSymbol = "",
            maxLabelLength = 5,
            precision = 3,
            digits = 3,
            beautifulScientificTickLabels = false,
            useUnicodeMinus = true,
            face = "|",
            includeBoundaries = false,
            type = "linear",
            ticksDistance = ticksDistance,
            drawLabels = drawLabels,
            clip = true,
            labelStyle = JsxGraphTicksLabelStyle(
                visible = true,
                color = JsxGraphColor(0, 0, 0),
                opacity = 1.0,
                fontSize = 12.0,
                anchorX = "left",
                anchorY = "middle",
                offsetX = 10.0,
                offsetY = 0.0,
            ),
        )

    private fun assertPoint(
        expectedX: Double,
        expectedY: Double,
        actual: JsxGraphPoint2D,
    ) {
        assertEquals(
            expectedX,
            actual.x,
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expectedY,
            actual.y,
            absoluteTolerance = TOLERANCE,
        )
    }

    private companion object {
        const val TOLERANCE = 1.0e-12
    }
}
