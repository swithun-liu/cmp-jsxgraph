/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.CircumcenterPoint
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.CurveError
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.Group
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.OrthogonalPoint
import com.swithun.jsxgraph.core.base.ParallelPoint
import com.swithun.jsxgraph.core.base.PerpendicularSegmentLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import com.swithun.jsxgraph.core.base.Sector
import com.swithun.jsxgraph.core.base.Text
import com.swithun.jsxgraph.core.base.TextError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeonextReaderTest {
    @Test
    fun parseMatchesOfficialPointLineCircleAndArrowBranches() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(SUPPORTED_SOURCE).parse(),
        ).value

        assertEquals(6, parsed.definitions.size)
        assertTrue(parsed.diagnostics.isEmpty())

        val point = assertIs<GeonextPointDefinition>(
            parsed.definitions[0],
        )
        assertEquals("P1", point.id)
        assertEquals("A", point.name)
        assertEquals(1.0, point.x)
        assertEquals(2.0, point.y)
        assertTrue(point.fixed)

        val line = assertIs<GeonextLineDefinition>(
            parsed.definitions[2],
        )
        assertEquals("P1", line.firstId)
        assertEquals("P2", line.lastId)
        assertFalse(line.straightFirst)
        assertTrue(line.straightLast)
        assertFalse(line.arrow)

        val pointCircle = assertIs<GeonextCircleDefinition>(
            parsed.definitions[3],
        )
        assertEquals("P1", pointCircle.centerId)
        assertEquals(
            GeonextCircleRadius.PointReference("P2"),
            pointCircle.radius,
        )
        assertEquals(
            GeonextPropertyValue.Text("P1"),
            pointCircle.properties.values["center"],
        )
        assertEquals(
            GeonextPropertyValue.Text("P2"),
            pointCircle.properties.values["radius"],
        )

        val radiusCircle = assertIs<GeonextCircleDefinition>(
            parsed.definitions[4],
        )
        assertEquals(
            GeonextCircleRadius.ValueExpression("2.5"),
            radiusCircle.radius,
        )

        val arrow = assertIs<GeonextLineDefinition>(
            parsed.definitions[5],
        )
        assertTrue(arrow.arrow)
    }

    @Test
    fun readCreatesOfficialPointLineCircleAndArrowGeometry() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(SUPPORTED_SOURCE).read(board),
        ).value

        assertEquals(
            listOf("P1", "P2", "L1", "C1", "C2", "V1"),
            drawn.objects.keys.toList(),
        )
        val first = assertIs<Point>(drawn.objects.getValue("P1"))
        val second = assertIs<Point>(drawn.objects.getValue("P2"))
        assertEquals(1.0, first.X())
        assertEquals(2.0, first.Y())
        assertTrue(first.isFixed)
        assertEquals(4.0, second.X())
        assertEquals(2.0, second.Y())
        assertFalse(second.isFixed)

        val line = assertIs<Line>(drawn.objects.getValue("L1"))
        assertEquals(first, line.point1)
        assertEquals(second, line.point2)
        assertFalse(line.straightFirst)
        assertTrue(line.straightLast)
        assertFalse(line.firstArrowEnabled)
        assertFalse(line.lastArrowEnabled)

        val pointCircle = assertIs<Circle>(
            drawn.objects.getValue("C1"),
        )
        assertEquals(first, pointCircle.center)
        assertEquals(second, pointCircle.point2)
        assertEquals(3.0, pointCircle.Radius())

        val radiusCircle = assertIs<Circle>(
            drawn.objects.getValue("C2"),
        )
        assertEquals(first, radiusCircle.center)
        assertEquals(null, radiusCircle.point2)
        assertEquals(2.5, radiusCircle.Radius())

        val arrow = assertIs<Line>(drawn.objects.getValue("V1"))
        assertEquals(second, arrow.point1)
        assertEquals(first, arrow.point2)
        assertEquals("arrow", arrow.elType)
        assertFalse(arrow.straightFirst)
        assertFalse(arrow.straightLast)
        assertFalse(arrow.firstArrowEnabled)
        assertTrue(arrow.lastArrowEnabled)
    }

    @Test
    fun parseMatchesOfficialIntersectionArcAndAngleBranches() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(DEPENDENT_SOURCE).parse(),
        ).value

        assertEquals(12, parsed.definitions.size)
        assertTrue(parsed.diagnostics.isEmpty())

        val lineIntersection = assertIs<GeonextIntersectionDefinition>(
            parsed.definitions[8],
        )
        assertEquals("I_LINES", lineIntersection.id)
        assertEquals("", lineIntersection.name)
        assertEquals("L1", lineIntersection.firstId)
        assertEquals("L2", lineIntersection.lastId)
        assertEquals("I0", lineIntersection.firstOutput.id)
        assertEquals("I", lineIntersection.firstOutput.name)
        assertFalse(lineIntersection.firstOutput.fixed)
        assertEquals(null, lineIntersection.lastOutput)

        val circleIntersection = assertIs<GeonextIntersectionDefinition>(
            parsed.definitions[9],
        )
        assertEquals("I1", circleIntersection.firstOutput.id)
        assertEquals("I2", circleIntersection.lastOutput?.id)
        assertEquals("K", circleIntersection.lastOutput?.name)
        assertTrue(circleIntersection.lastOutput?.fixed == true)

        val arc = assertIs<GeonextArcDefinition>(
            parsed.definitions[10],
        )
        assertEquals("P1", arc.centerId)
        assertEquals("P2", arc.radiusId)
        assertEquals("P4", arc.angleId)
        assertTrue(arc.firstArrow)
        assertFalse(arc.lastArrow)

        val angle = assertIs<GeonextAngleDefinition>(
            parsed.definitions[11],
        )
        assertEquals("P2", angle.firstId)
        assertEquals("P1", angle.middleId)
        assertEquals("P4", angle.lastId)
        assertEquals(1.25, angle.radius)
    }

    @Test
    fun readCreatesOfficialIntersectionArcAndAngleGeometry() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(DEPENDENT_SOURCE).read(board),
        ).value

        assertEquals(
            listOf(
                "P1",
                "P2",
                "P3",
                "P4",
                "L1",
                "L2",
                "C1",
                "C3",
                "I0",
                "I1",
                "I2",
                "A1",
                "ANG1",
            ),
            drawn.objects.keys.toList(),
        )
        assertEquals(null, board.select("I_LINES"))
        assertEquals(null, board.select("I_CIRCLES"))

        val lineIntersection = assertIs<IntersectionPoint>(
            drawn.objects.getValue("I0"),
        )
        assertEquals(2.5, lineIntersection.X(), 1e-12)
        assertEquals(2.0, lineIntersection.Y(), 1e-12)
        assertFalse(lineIntersection.isFixed)
        assertEquals(listOf("L1", "L2"), lineIntersection.parents)

        val firstCircleIntersection = assertIs<IntersectionPoint>(
            drawn.objects.getValue("I1"),
        )
        assertEquals(2.5, firstCircleIntersection.X(), 1e-12)
        assertEquals(
            -0.5980762113533159,
            firstCircleIntersection.Y(),
            1e-12,
        )
        val secondCircleIntersection = assertIs<IntersectionPoint>(
            drawn.objects.getValue("I2"),
        )
        assertEquals(2.5, secondCircleIntersection.X(), 1e-12)
        assertEquals(
            4.598076211353316,
            secondCircleIntersection.Y(),
            1e-12,
        )
        assertTrue(secondCircleIntersection.isFixed)

        val arc = assertIs<Arc>(drawn.objects.getValue("A1"))
        assertEquals(drawn.objects.getValue("P1"), arc.center)
        assertEquals(drawn.objects.getValue("P2"), arc.radiuspoint)
        assertEquals(drawn.objects.getValue("P4"), arc.anglepoint)
        assertEquals(3.0, arc.Radius(), 1e-12)
        assertTrue(arc.firstArrowEnabled)
        assertFalse(arc.lastArrowEnabled)

        val angle = assertIs<Sector>(drawn.objects.getValue("ANG1"))
        assertTrue(angle.isAngle)
        assertEquals(
            listOf("P2", "P1", "P4"),
            angle.parents,
        )
        assertEquals(1.25, angle.Radius(), 1e-12)
        assertEquals(1.1071487177940904, angle.Value("radians"), 1e-12)
    }

    @Test
    fun parseMatchesOfficialPolygonGraphAndParameterCurveBranches() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(POLYGON_CURVE_SOURCE).parse(),
        ).value

        assertEquals(6, parsed.definitions.size)
        assertTrue(parsed.diagnostics.isEmpty())

        val polygon = assertIs<GeonextPolygonDefinition>(
            parsed.definitions[3],
        )
        assertEquals("POLY1", polygon.id)
        assertEquals("triangle", polygon.name)
        assertEquals(listOf("P1", "P2", "P3"), polygon.vertexIds)
        assertEquals(listOf("B1", "B2", "B3"), polygon.borders.map { it.id })
        assertEquals(
            listOf("b1", "b2", "b3"),
            polygon.borders.map { it.name },
        )
        assertEquals(
            listOf(true, false, true),
            polygon.borders.map { it.straightFirst },
        )
        assertEquals(
            listOf(false, true, true),
            polygon.borders.map { it.straightLast },
        )

        val graph = assertIs<GeonextGraphDefinition>(
            parsed.definitions[4],
        )
        assertEquals("G1", graph.id)
        assertEquals("f", graph.name)
        assertEquals("x*x-1", graph.function)

        val parameterCurve = assertIs<GeonextParameterCurveDefinition>(
            parsed.definitions[5],
        )
        assertEquals("PC1", parameterCurve.id)
        assertEquals("q", parameterCurve.name)
        assertEquals("2*t", parameterCurve.functionX)
        assertEquals("t*t", parameterCurve.functionY)
        assertEquals("-1", parameterCurve.minimum)
        assertEquals("2", parameterCurve.maximum)
    }

    @Test
    fun readCreatesOfficialPolygonGraphAndParameterCurveGeometry() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(POLYGON_CURVE_SOURCE).read(board),
        ).value

        assertEquals(
            listOf("P1", "P2", "P3", "POLY1", "G1", "PC1"),
            drawn.objects.keys.toList(),
        )
        val polygon = assertIs<Polygon>(
            drawn.objects.getValue("POLY1"),
        )
        assertEquals(
            listOf("P1", "P2", "P3", "P1"),
            polygon.vertices.map { it.id },
        )
        assertEquals(
            listOf("B1", "B2", "B3"),
            polygon.borders.map { it.id },
        )
        assertEquals(
            listOf("b1", "b2", "b3"),
            polygon.borders.map { it.name },
        )
        assertEquals(
            listOf(true, false, true),
            polygon.borders.map { it.straightFirst },
        )
        assertEquals(
            listOf(false, true, true),
            polygon.borders.map { it.straightLast },
        )

        val graph = assertIs<Curve>(drawn.objects.getValue("G1"))
        assertEquals("functiongraph", graph.curveType)
        assertEquals(-6.0, graph.minX())
        assertEquals(6.0, graph.maxX())
        assertEquals(-2.0, graph.X(-2.0))
        assertEquals(3.0, graph.Y(-2.0))
        assertEquals(-1.0, graph.Y(0.0))
        assertEquals(8.0, graph.Y(3.0))

        val parameterCurve = assertIs<Curve>(
            drawn.objects.getValue("PC1"),
        )
        assertEquals("parameter", parameterCurve.curveType)
        assertEquals(-1.0, parameterCurve.minX())
        assertEquals(2.0, parameterCurve.maxX())
        assertEquals(-2.0, parameterCurve.X(-1.0))
        assertEquals(1.0, parameterCurve.Y(-1.0))
        assertEquals(1.0, parameterCurve.X(0.5))
        assertEquals(0.25, parameterCurve.Y(0.5))
        assertEquals(4.0, parameterCurve.X(2.0))
        assertEquals(4.0, parameterCurve.Y(2.0))
    }

    @Test
    fun parseMatchesOfficialSliderTraceCurveAndGroupBranches() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(SLIDER_TRACE_GROUP_SOURCE).parse(),
        ).value

        assertEquals(6, parsed.definitions.size)
        assertTrue(parsed.diagnostics.isEmpty())

        val slider = assertIs<GeonextSliderDefinition>(
            parsed.definitions[3],
        )
        assertEquals("S1", slider.id)
        assertEquals("S", slider.name)
        assertEquals(2.5, slider.x)
        assertEquals(3.0, slider.y)
        assertEquals("L1", slider.parentId)
        assertEquals(0.25, slider.position)
        assertFalse(slider.fixed)
        assertFalse(slider.onPolygon)
        assertEquals(
            GeonextPropertyValue.Text("false"),
            slider.properties.values["animateAnimated"],
        )
        assertEquals(
            GeonextPropertyValue.Text("20"),
            slider.properties.values["animateSpeed"],
        )

        val trace = assertIs<GeonextTraceCurveDefinition>(
            parsed.definitions[4],
        )
        assertEquals("S1", trace.tracePointId)
        assertEquals("S1", trace.traceSliderId)

        val group = assertIs<GeonextGroupDefinition>(
            parsed.definitions[5],
        )
        assertEquals("GR1", group.id)
        assertEquals("pair", group.name)
        assertEquals(listOf("P1", "P2", "S1"), group.memberIds)
    }

    @Test
    fun readCreatesOfficialSliderAndTraceWithSafeGroupAdaptation() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(SLIDER_TRACE_GROUP_SOURCE).read(board),
        ).value

        assertEquals(
            listOf("P1", "P2", "L1", "S1", "TC1"),
            drawn.objects.keys.toList(),
        )
        val slider = assertIs<Glider>(drawn.objects.getValue("S1"))
        assertEquals(2.5, slider.X())
        assertEquals(2.0, slider.Y())
        assertEquals(0.5, slider.position)
        assertEquals(drawn.objects.getValue("L1"), slider.slideObject)
        assertFalse(slider.onPolygon)
        assertFalse(slider.isFixed)

        val trace = assertIs<Curve>(drawn.objects.getValue("TC1"))
        assertTrue(trace.isTraceCurve)
        assertEquals("plot", trace.curveType)
        assertEquals(101, trace.numberPoints)
        assertEquals(1.0, trace.points.first().usrCoords[1])
        assertEquals(2.0, trace.points.first().usrCoords[2])
        assertEquals(4.0, trace.points.last().usrCoords[1])
        assertEquals(2.0, trace.points.last().usrCoords[2])

        val group = assertIs<Group>(drawn.groups["GR1"])
        assertEquals(group, board.groupById("GR1"))
        assertEquals("pair", group.name)
        assertEquals(listOf("P2", "S1"), group.groupObjects.keys.toList())
        assertTrue(group.getParents().isEmpty())
        assertTrue("GR1" in slider.groups)
        assertTrue(
            "GR1" !in assertIs<Point>(
                drawn.objects.getValue("P1"),
            ).groups,
        )
    }

    @Test
    fun parseAndReadMatchOfficialTextBranch() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(TEXT_SOURCE).parse(),
        ).value

        assertEquals(5, parsed.definitions.size)
        assertTrue(parsed.diagnostics.isEmpty())
        val freeDefinition = assertIs<GeonextTextDefinition>(
            parsed.definitions[3],
        )
        assertEquals("T1", freeDefinition.id)
        assertEquals("", freeDefinition.name)
        assertEquals(1.25, freeDefinition.x)
        assertEquals(-0.5, freeDefinition.y)
        assertEquals(
            "Hello &lt;b&gt;world&lt;/b&gt;",
            freeDefinition.content,
        )
        assertEquals(null, freeDefinition.parentId)
        assertEquals(4, freeDefinition.digits)
        assertTrue(freeDefinition.fixed)
        assertTrue(freeDefinition.visible)
        assertEquals("#405060", freeDefinition.strokeColor)

        val anchoredDefinition = assertIs<GeonextTextDefinition>(
            parsed.definitions[4],
        )
        assertEquals("T2", anchoredDefinition.id)
        assertEquals("L1", anchoredDefinition.parentId)
        assertEquals(2, anchoredDefinition.digits)
        assertFalse(anchoredDefinition.fixed)
        assertFalse(anchoredDefinition.visible)

        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(TEXT_SOURCE).read(board),
        ).value
        assertEquals(
            listOf("P1", "P2", "L1", "T1", "T2"),
            drawn.objects.keys.toList(),
        )
        val free = assertIs<Text>(drawn.objects.getValue("T1"))
        assertEquals(1.25, free.X())
        assertEquals(-0.5, free.Y())
        assertEquals("Hello &lt;b&gt;world&lt;/b&gt;", free.orgText)
        assertEquals("Hello &lt;b&gt;world&lt;/b&gt;", free.plaintext)
        assertEquals(null, free.anchor)
        assertEquals(null, free.relativeCoordinates)
        assertEquals(4, free.digits)
        assertTrue(free.isFixed)
        assertTrue(free.isVisible)
        assertEquals("#405060", free.strokeColor)

        val line = assertIs<Line>(drawn.objects.getValue("L1"))
        val anchored = assertIs<Text>(drawn.objects.getValue("T2"))
        assertEquals(3.0, anchored.X())
        assertEquals(3.0, anchored.Y())
        assertEquals("anchored", anchored.plaintext)
        assertEquals(line, anchored.anchor)
        assertTrue(line.childElements["T2"] === anchored)
        assertEquals(
            listOf(0.5, 1.0),
            anchored.relativeCoordinates?.toList(),
        )
        assertEquals(2, anchored.digits)
        assertFalse(anchored.isFixed)
        assertFalse(anchored.isVisible)
        assertEquals(null, board.select("oldVersionT3"))
    }

    @Test
    fun readResolvesSpecialOriginIdsThroughBoardPrefix() {
        val board = board()
        val origin = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                id = board.id + "gOOe0",
                name = "",
            ),
        ).value
        val source = geonext(
            point("P1", "A", 2.0, 3.0),
            line(
                id = "L1",
                name = "l",
                first = "gOOe0",
                last = "P1",
            ),
        )

        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(source).read(board),
        ).value
        val line = assertIs<Line>(drawn.objects.getValue("L1"))

        assertEquals(origin, line.point1)
        assertEquals(drawn.objects.getValue("P1"), line.point2)
    }

    @Test
    fun unsupportedElementsRemainExplicitAndStrictModeRejectsThem() {
        val source = geonext(
            point("P1", "A", 1.0, 2.0),
            "<image><id>IMG1</id></image>",
        )
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(source).parse(),
        ).value

        assertEquals(1, parsed.definitions.size)
        assertEquals(
            GeonextReaderDiagnostic.UnsupportedElement(
                elementIndex = 1,
                elementType = "image",
                elementId = "IMG1",
            ),
            parsed.diagnostics.single(),
        )
        assertIs<GMResult.Err<GeonextReaderError.UnsupportedElements>>(
            GeonextReader(source).read(
                board = board(),
                failOnUnsupportedElements = true,
            ),
        )
    }

    @Test
    fun missingReferenceRollsBackTheWholeRead() {
        val board = board()
        val existing = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(-1.0, -1.0),
                id = "existing",
                name = "Existing",
            ),
        ).value
        val source = geonext(
            point("P1", "A", 1.0, 2.0),
            line(
                id = "L1",
                name = "l",
                first = "P1",
                last = "missing",
            ),
        )

        val error = assertIs<
            GMResult.Err<GeonextReaderError.MissingReference>,
            >(
            GeonextReader(source).read(board),
        ).error

        assertEquals("L1", error.definitionId)
        assertEquals("missing", error.referenceId)
        assertEquals(listOf("existing"), board.objects.keys.toList())
        assertEquals(existing, board.elementByName("Existing"))
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun pointReferenceTypeMismatchRollsBackTheWholeRead() {
        val board = board()
        val source = geonext(
            point("P1", "A", 1.0, 2.0),
            point("P2", "B", 4.0, 2.0),
            line("L1", "l", "P1", "P2"),
            arc(
                id = "A1",
                name = "a",
                center = "L1",
                radius = "P2",
                angle = "P1",
            ),
        )

        val error = assertIs<
            GMResult.Err<GeonextReaderError.ReferenceTypeMismatch>,
            >(
            GeonextReader(source).read(board),
        ).error

        assertEquals("A1", error.definitionId)
        assertEquals("L1", error.referenceId)
        assertEquals("point", error.expectedType)
        assertEquals("line", error.actualType)
        assertTrue(board.objects.isEmpty())
        assertTrue(board.elementsByName.isEmpty())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun polygonAndCurveFailuresAreStructuredAndRollBackTheWholeRead() {
        val missingVertexBoard = board()
        val missingVertexError = assertIs<
            GMResult.Err<GeonextReaderError.MissingReference>,
            >(
            GeonextReader(
                geonext(
                    point("P1", "A", 1.0, 2.0),
                    polygon(
                        id = "POLY1",
                        name = "triangle",
                        vertices = listOf("P1", "missing", "P1"),
                    ),
                ),
            ).read(missingVertexBoard),
        ).error
        assertEquals("POLY1", missingVertexError.definitionId)
        assertEquals("missing", missingVertexError.referenceId)
        assertTrue(missingVertexBoard.objects.isEmpty())
        assertFalse(missingVertexBoard.isSuspendedUpdate)

        val invalidGraphBoard = board()
        val invalidGraphError = assertIs<
            GMResult.Err<GeonextReaderError.BoardCreationFailed>,
            >(
            GeonextReader(
                geonext(
                    point("P1", "A", 1.0, 2.0),
                    graph("G1", "f", "x +"),
                ),
            ).read(invalidGraphBoard),
        ).error
        val curveFailure = assertIs<GeonextCreationError.Curve>(
            invalidGraphError.cause,
        )
        assertIs<CurveError.ExpressionCompile>(curveFailure.cause)
        assertTrue(invalidGraphBoard.objects.isEmpty())
        assertFalse(invalidGraphBoard.isSuspendedUpdate)
    }

    @Test
    fun textReferenceAndCreationFailuresRollBackTheWholeRead() {
        val missingAnchorBoard = board()
        val missingAnchorError = assertIs<
            GMResult.Err<GeonextReaderError.MissingReference>,
            >(
            GeonextReader(
                geonext(
                    point("P1", "A", 1.0, 2.0),
                    textElement(
                        id = "T1",
                        x = 0.0,
                        y = 0.0,
                        content = "missing",
                        parent = "missing",
                    ),
                ),
            ).read(missingAnchorBoard),
        ).error
        assertEquals("T1", missingAnchorError.definitionId)
        assertEquals("missing", missingAnchorError.referenceId)
        assertEquals("text anchor", missingAnchorError.expectedType)
        assertTrue(missingAnchorBoard.objects.isEmpty())
        assertFalse(missingAnchorBoard.isSuspendedUpdate)

        val wrongAnchorBoard = board()
        val wrongAnchorError = assertIs<
            GMResult.Err<GeonextReaderError.ReferenceTypeMismatch>,
            >(
            GeonextReader(
                geonext(
                    point("P1", "A", 1.0, 2.0),
                    textElement(
                        id = "T1",
                        x = 0.0,
                        y = 0.0,
                        content = "wrong",
                        parent = "P1",
                    ),
                ),
            ).read(wrongAnchorBoard),
        ).error
        assertEquals("line text anchor", wrongAnchorError.expectedType)
        assertEquals("point", wrongAnchorError.actualType)
        assertTrue(wrongAnchorBoard.objects.isEmpty())

        val invalidContentBoard = board()
        val invalidContentError = assertIs<
            GMResult.Err<GeonextReaderError.BoardCreationFailed>,
            >(
            GeonextReader(
                geonext(
                    textElement(
                        id = "T1",
                        x = 0.0,
                        y = 0.0,
                        content = "<value>x +</value>",
                    ),
                ),
            ).read(invalidContentBoard),
        ).error
        val textFailure = assertIs<GeonextCreationError.Text>(
            invalidContentError.cause,
        )
        assertIs<TextError.ContentExpressionCompile>(textFailure.cause)
        assertTrue(invalidContentBoard.objects.isEmpty())
        assertFalse(invalidContentBoard.isSuspendedUpdate)
    }

    @Test
    fun compositionDefinitionsPreserveTypeInputsAndOutputs() {
        val parsed = assertIs<GMResult.Ok<ParsedGeonext>>(
            GeonextReader(COMPOSITION_SOURCE).parse(),
        ).value
        val compositions = parsed.definitions
            .filterIsInstance<GeonextCompositionDefinition>()

        assertEquals(13, compositions.size)
        val perpendicular = compositions.first {
            it.type == "210160"
        }
        assertEquals("COMP_PERPENDICULAR", perpendicular.id)
        assertEquals(listOf("P4", "L1"), perpendicular.inputIds)
        assertEquals(
            listOf("PERP_POINT", "PERP_LINE"),
            perpendicular.outputs.map { it.id },
        )
        assertEquals(
            GeonextPropertyValue.Text("false"),
            perpendicular.outputs[0].properties.values["fixed"],
        )
        assertFalse(
            assertIs<GeonextPropertyValue.Flag>(
                perpendicular.outputs[1]
                    .properties.values["straightFirst"],
            ).value,
        )
    }

    @Test
    fun readCreatesOfficialGeonextCompositionGeometry() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnGeonext>>(
            GeonextReader(COMPOSITION_SOURCE).read(board),
        ).value

        val midpoint = assertIs<MidpointPoint>(
            drawn.objects.getValue("MIDPOINT"),
        )
        assertEquals(1.0, midpoint.X())
        assertEquals(0.0, midpoint.Y())

        val circumcenter = assertIs<CircumcenterPoint>(
            drawn.objects.getValue("CIRCUMCENTER"),
        )
        assertEquals(1.0, circumcenter.X())
        assertEquals(1.0, circumcenter.Y())

        val reflection = assertIs<Point>(
            drawn.objects.getValue("REFLECTION"),
        )
        assertEquals(0.0, reflection.X())
        assertEquals(-2.0, reflection.Y())
        assertTrue(reflection.isFixed)

        val mirrorPoint = assertIs<Point>(
            drawn.objects.getValue("MIRROR_POINT"),
        )
        assertEquals(2.0, mirrorPoint.X(), 1.0e-12)
        assertEquals(0.0, mirrorPoint.Y(), 1.0e-12)
        assertTrue(mirrorPoint.isFixed)

        val parallelogram = assertIs<ParallelPoint>(
            drawn.objects.getValue("PARALLELOGRAM"),
        )
        assertEquals(2.0, parallelogram.X())
        assertEquals(2.0, parallelogram.Y())

        val perpendicularPoint = assertIs<OrthogonalPoint>(
            drawn.objects.getValue("PERPENDICULAR_POINT"),
        )
        assertEquals(1.0, perpendicularPoint.X())
        assertEquals(0.0, perpendicularPoint.Y(), 1.0e-12)

        val normal = assertIs<PerpendicularSegmentLine>(
            drawn.objects.getValue("NORMAL"),
        )
        assertTrue(normal.straightFirst)
        assertTrue(normal.straightLast)

        val perpendicular = assertIs<PerpendicularSegmentLine>(
            drawn.objects.getValue("PERP_LINE"),
        )
        assertEquals("PERP_POINT", perpendicular.point.id)
        assertEquals(
            perpendicular.point,
            drawn.objects.getValue("PERP_POINT"),
        )
        assertFalse(perpendicular.straightFirst)
        assertFalse(perpendicular.straightLast)

        val arrowParallel = assertIs<Line>(
            drawn.objects.getValue("ARROW_PARALLEL"),
        )
        assertEquals(
            "ARROW_PARALLEL_POINT",
            arrowParallel.parallelPoint?.id,
        )
        assertFalse(arrowParallel.firstArrowEnabled)
        assertTrue(arrowParallel.lastArrowEnabled)
        assertFalse(arrowParallel.straightFirst)
        assertFalse(arrowParallel.straightLast)

        val parallel = assertIs<Line>(
            drawn.objects.getValue("PARALLEL"),
        )
        assertEquals("parallelpoint", parallel.parallelPoint?.elType)
        assertTrue(parallel.straightFirst)
        assertTrue(parallel.straightLast)

        val bisector = assertIs<BisectorLine>(
            drawn.objects.getValue("BISECTOR"),
        )
        assertFalse(bisector.straightFirst)
        assertTrue(bisector.straightLast)

        assertEquals(null, board.select("CIRCUMCIRCLE_CENTER"))
        val circumcircle = assertIs<Circle>(
            drawn.objects.getValue("CIRCUMCIRCLE"),
        )
        assertEquals("circumcircle", circumcircle.elType)
        assertEquals(1.0, circumcircle.center.X())
        assertEquals(1.0, circumcircle.center.Y())

        val sector = assertIs<Sector>(
            drawn.objects.getValue("SECTOR"),
        )
        assertEquals(2.0, sector.Radius())
        val sectorPoint = assertIs<Point>(
            drawn.objects.getValue("SECTOR_POINT"),
        )
        assertTrue(sectorPoint.X().isNaN())
        assertTrue(sectorPoint.Y().isNaN())
        assertEquals(
            listOf("SECTOR", "SECTOR_POINT", "SECTOR_FIRST", "SECTOR_LAST"),
            listOf(
                sector.id,
                sectorPoint.id,
                drawn.objects.getValue("SECTOR_FIRST").id,
                drawn.objects.getValue("SECTOR_LAST").id,
            ),
        )
    }

    @Test
    fun officialMirrorLineParentOrderFailureIsStructuredAndRollsBack() {
        val board = board()
        val source = geonext(
            point("P1", "A", 0.0, 0.0),
            point("P2", "B", 2.0, 0.0),
            point("P3", "C", 0.0, 2.0),
            line("L1", "l", "P1", "P2"),
            composition(
                id = "BROKEN_REFLECTION",
                type = "210120",
                inputs = listOf("P3", "L1"),
                outputs = listOf(compositionOutput("R", "R")),
            ),
        )

        val error = assertIs<
            GMResult.Err<GeonextReaderError.ReferenceTypeMismatch>,
            >(
            GeonextReader(source).read(board),
        ).error

        assertEquals("BROKEN_REFLECTION", error.definitionId)
        assertEquals("L1", error.referenceId)
        assertEquals("point", error.expectedType)
        assertTrue(board.objects.isEmpty())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun rotationUnknownTypeAndMissingOutputAreStructured() {
        for (type in listOf("210180", "999999")) {
            val error = assertIs<
                GMResult.Err<
                    GeonextReaderError.UnsupportedCompositionType,
                    >,
                >(
                GeonextReader(
                    geonext(
                        composition(
                            id = "UNSUPPORTED",
                            type = type,
                            inputs = emptyList(),
                            outputs = emptyList(),
                        ),
                    ),
                ).parse(),
            ).error
            assertEquals(type, error.type)
        }

        val missingOutput = assertIs<
            GMResult.Err<GeonextReaderError.MissingCompositionOutput>,
            >(
            GeonextReader(
                geonext(
                    point("P1", "A", 0.0, 0.0),
                    point("P2", "B", 2.0, 0.0),
                    composition(
                        id = "MID",
                        type = "210110",
                        inputs = listOf("P1", "P2"),
                        outputs = emptyList(),
                    ),
                ),
            ).read(board()),
        ).error
        assertEquals(0, missingOutput.outputIndex)
    }

    @Test
    fun failureAfterGroupCreationRollsBackGroupMembership() {
        val board = board()
        val source = geonext(
            point("P1", "A", 1.0, 2.0),
            point("P2", "B", 4.0, 2.0),
            group("GR1", "pair", listOf("P1", "P2")),
            line("L1", "l", "P1", "missing"),
        )

        assertIs<GMResult.Err<GeonextReaderError.MissingReference>>(
            GeonextReader(source).read(board),
        )
        assertTrue(board.objects.isEmpty())
        assertTrue(board.groups.isEmpty())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun malformedPropertiesAndLimitsAreStructured() {
        val malformedCircle = geonext(
            listOf(
                "<circle><name>c</name><id>C1</id>",
                "<data><midpoint>P1</midpoint></data>",
                "<visible>true</visible><trace>false</trace>",
                COLOR,
                "</circle>",
            ).joinToString(""),
        )

        assertIs<GMResult.Err<GeonextReaderError.PropertyParsingFailed>>(
            GeonextReader(malformedCircle).parse(),
        )
        assertIs<GMResult.Err<GeonextReaderError.InvalidLimits>>(
            GeonextReader(SUPPORTED_SOURCE).parse(
                GeonextReaderLimits(maxElements = -1),
            ),
        )
        assertIs<GMResult.Err<GeonextReaderError.ElementLimitExceeded>>(
            GeonextReader(SUPPORTED_SOURCE).parse(
                GeonextReaderLimits(maxElements = 1),
            ),
        )
    }

    @Test
    fun fileReaderRegistryUsesStrictGeonextReader() {
        val registry = ReaderRegistry<Board>().apply {
            registerGeonextReader()
        }

        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source = SUPPORTED_SOURCE,
                board = board(),
                format = "gxt",
                registry = registry,
            ),
        )
        assertIs<GMResult.Err<ReaderError.DomainFailure>>(
            FileReader.parseString(
                source = geonext("<text><id>T1</id></text>"),
                board = board(),
                format = "geonext",
                registry = registry,
            ),
        )
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
    )

    private companion object {
        val COLOR = listOf(
            "<color>",
            "<stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill>",
            "<label>#405060FF</label>",
            "<draft>#506070FF</draft>",
            "</color>",
        ).joinToString("")

        fun point(
            id: String,
            name: String,
            x: Double,
            y: Double,
            fixed: Boolean = false,
        ): String = listOf(
            "<point><name>$name</name><id>$id</id>",
            "<data><x>$x</x><y>$y</y></data>",
            "<fix>$fixed</fix>",
            "<visible>true</visible><trace>false</trace>",
            "<style>3</style>",
            COLOR,
            "</point>",
        ).joinToString("")

        fun line(
            id: String,
            name: String,
            first: String,
            last: String,
            type: String = "line",
            straightFirst: Boolean = false,
            straightLast: Boolean = true,
        ): String = listOf(
            "<$type><name>$name</name><id>$id</id>",
            "<data><first>$first</first><last>$last</last></data>",
            "<straight><first>$straightFirst</first>",
            "<last>$straightLast</last></straight>",
            "<visible>true</visible><trace>false</trace>",
            COLOR,
            "</$type>",
        ).joinToString("")

        fun circle(
            id: String,
            name: String,
            center: String,
            radiusTag: String,
            radius: String,
        ): String = listOf(
            "<circle><name>$name</name><id>$id</id>",
            "<data><midpoint>$center</midpoint>",
            "<$radiusTag>$radius</$radiusTag></data>",
            "<visible>true</visible><trace>false</trace>",
            COLOR,
            "</circle>",
        ).joinToString("")

        fun intersectionOutput(
            tag: String,
            id: String,
            name: String,
            fixed: Boolean = false,
        ): String = listOf(
            "<$tag><name>$name</name><id>$id</id>",
            "<visible>true</visible><trace>false</trace>",
            "<strokewidth>3</strokewidth><fix>$fixed</fix>",
            "<style>3</style>",
            COLOR,
            "</$tag>",
        ).joinToString("")

        fun intersection(
            id: String,
            first: String,
            last: String,
            firstOutput: String,
            lastOutput: String? = null,
        ): String = listOfNotNull(
            "<intersection><name></name><id>$id</id>",
            "<data><first>$first</first><last>$last</last></data>",
            firstOutput,
            lastOutput,
            "</intersection>",
        ).joinToString("")

        fun arc(
            id: String,
            name: String,
            center: String,
            radius: String,
            angle: String,
            firstArrow: Boolean = true,
            lastArrow: Boolean = false,
        ): String = listOf(
            "<arc><name>$name</name><id>$id</id>",
            "<data><midpoint>$center</midpoint>",
            "<radius>$radius</radius><angle>$angle</angle></data>",
            "<visible>true</visible><trace>false</trace>",
            "<firstarrow>$firstArrow</firstarrow>",
            "<lastarrow>$lastArrow</lastarrow>",
            COLOR,
            "</arc>",
        ).joinToString("")

        fun angle(
            id: String,
            name: String,
            first: String,
            middle: String,
            last: String,
            radius: Double,
        ): String = listOf(
            "<angle><name>$name</name><id>$id</id>",
            "<data><first>$first</first><middle>$middle</middle>",
            "<last>$last</last><radius>$radius</radius></data>",
            "<visible>true</visible><trace>false</trace>",
            COLOR,
            "</angle>",
        ).joinToString("")

        fun border(
            id: String,
            name: String,
            straightFirst: Boolean,
            straightLast: Boolean,
            strokeWidth: Int,
        ): String = listOf(
            "<border><name>$name</name><id>$id</id>",
            "<straight><first>$straightFirst</first>",
            "<last>$straightLast</last></straight>",
            "<strokewidth>$strokeWidth</strokewidth>",
            "<visible>true</visible><draft>false</draft>",
            "<trace>false</trace>",
            COLOR,
            "</border>",
        ).joinToString("")

        fun polygon(
            id: String,
            name: String,
            vertices: List<String>,
            borders: List<String> = emptyList(),
        ): String = listOf(
            "<polygon><name>$name</name><id>$id</id>",
            "<data>",
            vertices.joinToString("") { vertex ->
                "<vertex>$vertex</vertex>"
            },
            "</data><visible>true</visible>",
            COLOR,
            borders.joinToString(""),
            "</polygon>",
        ).joinToString("")

        fun graph(
            id: String,
            name: String,
            function: String,
        ): String = listOf(
            "<graph><name>$name</name><id>$id</id>",
            "<data><function>$function</function></data>",
            "<visible>true</visible><strokewidth>2</strokewidth>",
            COLOR,
            "</graph>",
        ).joinToString("")

        fun parameterCurve(
            id: String,
            name: String,
            functionX: String,
            functionY: String,
            minimum: String,
            maximum: String,
        ): String = listOf(
            "<parametercurve><name>$name</name><id>$id</id>",
            "<functionx>$functionX</functionx>",
            "<functiony>$functionY</functiony>",
            "<min>$minimum</min><max>$maximum</max>",
            "<visible>true</visible><trace>false</trace>",
            COLOR,
            "</parametercurve>",
        ).joinToString("")

        fun slider(
            id: String,
            name: String,
            x: Double,
            y: Double,
            parent: String,
            position: Double,
        ): String = listOf(
            "<slider><name>$name</name><id>$id</id>",
            "<data><x>$x</x><y>$y</y><parent>$parent</parent>",
            "<position>$position</position></data>",
            "<visible>true</visible><trace>false</trace>",
            "<fix>false</fix><style>3</style>",
            COLOR,
            "<animate><animated>false</animated><back>true</back>",
            "<speed>20</speed><start>0</start><stop>1</stop>",
            "<loop>2</loop><direction>false</direction></animate>",
            "<onpolygon>false</onpolygon>",
            "</slider>",
        ).joinToString("")

        fun traceCurve(
            id: String,
            name: String,
            tracePoint: String,
            traceSlider: String,
        ): String = listOf(
            "<tracecurve><name>$name</name><id>$id</id>",
            "<tracepoint>$tracePoint</tracepoint>",
            "<traceslider>$traceSlider</traceslider>",
            "</tracecurve>",
        ).joinToString("")

        fun group(
            id: String,
            name: String,
            members: List<String>,
        ): String = listOf(
            "<group><name>$name</name><id>$id</id><data>",
            members.joinToString("") { member ->
                "<member>$member</member>"
            },
            "</data>",
            COLOR,
            "</group>",
        ).joinToString("")

        fun textElement(
            id: String,
            x: Double,
            y: Double,
            content: String,
            parent: String? = null,
            fixed: Boolean? = null,
            digits: Int? = null,
            visible: Boolean = true,
        ): String = listOf(
            "<text><id>$id</id><data><x>$x</x><y>$y</y>",
            "<mp>$content</mp><content>fallback</content>",
            "<parent>${parent.orEmpty()}</parent></data>",
            "<condition></condition>",
            fixed?.let { value -> "<fix>$value</fix>" }.orEmpty(),
            digits?.let { value -> "<digits>$value</digits>" }.orEmpty(),
            "<visible>$visible</visible><trace>false</trace>",
            COLOR,
            "</text>",
        ).joinToString("")

        fun compositionOutput(
            id: String,
            name: String,
            fixed: Boolean? = null,
            straightFirst: Boolean? = null,
            straightLast: Boolean? = null,
        ): String = listOf(
            "<output><name>$name</name><id>$id</id>",
            "<visible>true</visible><trace>false</trace>",
            fixed?.let { "<fix>$it</fix>" }.orEmpty(),
            if (straightFirst != null && straightLast != null) {
                "<straight><first>$straightFirst</first>" +
                    "<last>$straightLast</last></straight>"
            } else {
                ""
            },
            "<style>1</style>",
            COLOR,
            "</output>",
        ).joinToString("")

        fun composition(
            id: String,
            type: String,
            inputs: List<String>,
            outputs: List<String>,
        ): String = listOf(
            "<composition><name></name><id>$id</id>",
            "<data><type>$type</type>",
            inputs.joinToString("") { input ->
                "<input>$input</input>"
            },
            "</data><active>true</active><visible>true</visible>",
            outputs.joinToString(""),
            "</composition>",
        ).joinToString("")

        fun geonext(vararg elements: String): String =
            "<GEONEXT><elements>" +
                elements.joinToString("") +
                "</elements></GEONEXT>"

        val SUPPORTED_SOURCE = geonext(
            point("P1", "A", 1.0, 2.0, fixed = true),
            point("P2", "B", 4.0, 2.0),
            line("L1", "l", "P1", "P2"),
            circle("C1", "c", "P1", "radius", "P2"),
            circle("C2", "d", "P1", "radiusvalue", "2.5"),
            line(
                id = "V1",
                name = "v",
                first = "P2",
                last = "P1",
                type = "arrow",
                straightFirst = true,
                straightLast = true,
            ),
        )

        val DEPENDENT_SOURCE = geonext(
            point("P1", "A", 1.0, 2.0, fixed = true),
            point("P2", "B", 4.0, 2.0),
            point("P3", "C", 2.5, -1.0),
            point("P4", "D", 2.5, 5.0),
            line("L1", "l", "P1", "P2"),
            line(
                id = "L2",
                name = "m",
                first = "P3",
                last = "P4",
                straightFirst = true,
                straightLast = true,
            ),
            circle("C1", "c", "P1", "radius", "P2"),
            circle("C3", "e", "P2", "radiusvalue", "3"),
            intersection(
                id = "I_LINES",
                first = "L1",
                last = "L2",
                firstOutput = intersectionOutput("first", "I0", "I"),
            ),
            intersection(
                id = "I_CIRCLES",
                first = "C1",
                last = "C3",
                firstOutput = intersectionOutput("first", "I1", "J"),
                lastOutput = intersectionOutput(
                    tag = "last",
                    id = "I2",
                    name = "K",
                    fixed = true,
                ),
            ),
            arc("A1", "a", "P1", "P2", "P4"),
            angle("ANG1", "w", "P2", "P1", "P4", 1.25),
        )

        val POLYGON_CURVE_SOURCE = geonext(
            point("P1", "A", 1.0, 2.0, fixed = true),
            point("P2", "B", 4.0, 2.0),
            point("P3", "C", 2.5, -1.0),
            polygon(
                id = "POLY1",
                name = "triangle",
                vertices = listOf("P1", "P2", "P3", "P1"),
                borders = listOf(
                    border("B1", "b1", true, false, 2),
                    border("B2", "b2", false, true, 3),
                    border("B3", "b3", true, true, 4),
                ),
            ),
            graph("G1", "f", "x*x-1"),
            parameterCurve(
                id = "PC1",
                name = "q",
                functionX = "2*t",
                functionY = "t*t",
                minimum = "-1",
                maximum = "2",
            ),
        )

        val SLIDER_TRACE_GROUP_SOURCE = geonext(
            point("P1", "A", 1.0, 2.0, fixed = true),
            point("P2", "B", 4.0, 2.0),
            line("L1", "l", "P1", "P2"),
            slider(
                id = "S1",
                name = "S",
                x = 2.5,
                y = 3.0,
                parent = "L1",
                position = 0.25,
            ),
            traceCurve("TC1", "trace", "S1", "S1"),
            group("GR1", "pair", listOf("P1", "P2", "S1")),
        )

        val TEXT_SOURCE = geonext(
            point("P1", "A", 1.0, 2.0),
            point("P2", "B", 4.0, 2.0),
            line("L1", "l", "P1", "P2"),
            textElement(
                id = "T1",
                x = 1.25,
                y = -0.5,
                content = "Hello <b>world</b>",
                fixed = true,
                digits = 4,
            ),
            textElement(
                id = "T2",
                x = 0.5,
                y = 1.0,
                content = "anchored",
                parent = "L1",
                visible = false,
            ),
            "<text><id>oldVersionT3</id></text>",
        )

        val COMPOSITION_SOURCE = geonext(
            point("P1", "A", 0.0, 0.0),
            point("P2", "B", 2.0, 0.0),
            point("P3", "C", 0.0, 2.0),
            point("P4", "D", 1.0, 1.0),
            line(
                id = "L1",
                name = "l",
                first = "P1",
                last = "P2",
                straightFirst = true,
                straightLast = true,
            ),
            composition(
                id = "COMP_ARROW_PARALLEL",
                type = "210070",
                inputs = listOf("P3", "L1"),
                outputs = listOf(
                    compositionOutput(
                        "ARROW_PARALLEL",
                        "a",
                        straightFirst = false,
                        straightLast = false,
                    ),
                    compositionOutput("ARROW_PARALLEL_POINT", "E"),
                ),
            ),
            composition(
                id = "COMP_BISECTOR",
                type = "210080",
                inputs = listOf("P1", "P2", "P3"),
                outputs = listOf(
                    compositionOutput(
                        "BISECTOR",
                        "b",
                        straightFirst = false,
                        straightLast = true,
                    ),
                ),
            ),
            composition(
                id = "COMP_CIRCUMCIRCLE",
                type = "210090",
                inputs = listOf("P1", "P2", "P3"),
                outputs = listOf(
                    compositionOutput("CIRCUMCIRCLE_CENTER", "O"),
                    compositionOutput("CIRCUMCIRCLE", "c"),
                ),
            ),
            composition(
                id = "COMP_CIRCUMCENTER",
                type = "210100",
                inputs = listOf("P1", "P2", "P3"),
                outputs = listOf(
                    compositionOutput("CIRCUMCENTER", "O2"),
                ),
            ),
            composition(
                id = "COMP_MIDPOINT",
                type = "210110",
                inputs = listOf("P1", "P2"),
                outputs = listOf(
                    compositionOutput("MIDPOINT", "M"),
                ),
            ),
            composition(
                id = "COMP_REFLECTION",
                type = "210120",
                inputs = listOf("L1", "P3"),
                outputs = listOf(
                    compositionOutput("REFLECTION", "R"),
                ),
            ),
            composition(
                id = "COMP_MIRROR_POINT",
                type = "210125",
                inputs = listOf("P3", "P4"),
                outputs = listOf(
                    compositionOutput("MIRROR_POINT", "R2"),
                ),
            ),
            composition(
                id = "COMP_NORMAL",
                type = "210130",
                inputs = listOf("P3", "L1"),
                outputs = listOf(
                    compositionOutput(
                        "NORMAL",
                        "n",
                        straightFirst = true,
                        straightLast = true,
                    ),
                ),
            ),
            composition(
                id = "COMP_PARALLEL",
                type = "210140",
                inputs = listOf("P3", "L1"),
                outputs = listOf(
                    compositionOutput(
                        "PARALLEL",
                        "p",
                        straightFirst = true,
                        straightLast = true,
                    ),
                ),
            ),
            composition(
                id = "COMP_PARALLELOGRAM",
                type = "210150",
                inputs = listOf("P1", "P2", "P3"),
                outputs = listOf(
                    compositionOutput("PARALLELOGRAM", "P"),
                ),
            ),
            composition(
                id = "COMP_PERPENDICULAR",
                type = "210160",
                inputs = listOf("P4", "L1"),
                outputs = listOf(
                    compositionOutput(
                        "PERP_POINT",
                        "Q",
                        fixed = false,
                    ),
                    compositionOutput(
                        "PERP_LINE",
                        "q",
                        straightFirst = false,
                        straightLast = false,
                    ),
                ),
            ),
            composition(
                id = "COMP_PERPENDICULAR_POINT",
                type = "210170",
                inputs = listOf("P4", "L1"),
                outputs = listOf(
                    compositionOutput("PERPENDICULAR_POINT", "Q2"),
                ),
            ),
            composition(
                id = "COMP_SECTOR",
                type = "210190",
                inputs = listOf("P1", "P2", "P3"),
                outputs = listOf(
                    compositionOutput("SECTOR", "s"),
                    compositionOutput(
                        "SECTOR_POINT",
                        "S",
                        fixed = false,
                    ),
                    compositionOutput(
                        "SECTOR_FIRST",
                        "s1",
                        straightFirst = false,
                        straightLast = false,
                    ),
                    compositionOutput(
                        "SECTOR_LAST",
                        "s2",
                        straightFirst = false,
                        straightLast = false,
                    ),
                ),
            ),
        )
    }
}
