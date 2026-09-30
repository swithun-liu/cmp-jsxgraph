/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Arc
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.OtherIntersectionPoint
import com.swithun.jsxgraph.core.base.ParallelLine
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Polygon
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CinderellaReaderTest {
    @Test
    fun coreDefinitionsMatchOfficialReaderBranches() {
        val parsed = parse(SOURCE)

        assertEquals(5, parsed.definitions.size)
        assertEquals(
            CinderellaFreePointDefinition(
                name = "A",
                coordinates = listOf(1.0, 3.0),
                properties = CinderellaPointProperties(
                    appearance = CinderellaAppearance(
                        listOf(
                            "red",
                            "5",
                            "1",
                            "0",
                            "0",
                            "9",
                            "true",
                            "false",
                        ),
                    ),
                    nextIndex = 4,
                    border = "black",
                    labelColor = "black",
                ),
                sourceLine = 1,
            ),
            parsed.definitions[0],
        )
        assertEquals(
            CinderellaLineDefinition(
                name = "l",
                pointNames = listOf("A", "B"),
                segment = false,
                properties = CinderellaLineProperties(
                    appearance = CinderellaAppearance(
                        listOf(
                            "#199e4e",
                            "5",
                            "5",
                            "0",
                            "0",
                            "9",
                            "true",
                            "false",
                        ),
                    ),
                    dashing = 3,
                    nextIndex = 7,
                ),
                sourceLine = 5,
            ),
            parsed.definitions[1],
        )
        assertEquals(
            CinderellaLineDefinition::class,
            parsed.definitions[2]::class,
        )
        assertTrue(
            (parsed.definitions[2] as CinderellaLineDefinition).segment,
        )
        val pointCircle = assertIs<CinderellaCircleByPointsDefinition>(
            parsed.definitions[3],
        )
        assertEquals("C0", pointCircle.name)
        assertEquals("A", pointCircle.centerName)
        assertEquals("B", pointCircle.pointName)
        assertEquals(0.4, pointCircle.properties.fillOpacity)

        val radiusCircle = assertIs<CinderellaCircleByRadiusDefinition>(
            parsed.definitions[4],
        )
        assertEquals("C1", radiusCircle.name)
        assertEquals("A", radiusCircle.centerName)
        assertEquals(3.0, radiusCircle.radius)
        assertTrue(radiusCircle.fixedRadius)
        assertEquals("12.5", parsed.originX)
        assertEquals("20", parsed.originY)
        assertEquals(2.0, parsed.scale)
        assertEquals(
            listOf(
                CinderellaReaderDiagnostic.UnsupportedDefinition(
                    lineIndex = 19,
                    line = "(\"Z\"):=Unknown(\"A\");",
                ),
            ),
            parsed.diagnostics,
        )
    }

    @Test
    fun constrainedDefinitionsMatchOfficialReaderBranches() {
        val parsed = parse(CONSTRAINED_SOURCE)
        val definitions = parsed.definitions.associateBy { it.name }

        assertEquals(11, parsed.definitions.size)
        val circleGlider = assertIs<CinderellaPointOnCircleDefinition>(
            definitions["G"],
        )
        assertEquals("G", circleGlider.name)
        assertEquals("C0", circleGlider.circleName)
        assertEquals(listOf(1.0, -2.0), circleGlider.centerOffset)

        val reversedCircleGlider =
            assertIs<CinderellaPointOnCircleDefinition>(
                definitions["H"],
            )
        assertEquals("H", reversedCircleGlider.name)
        assertEquals(listOf(-1.0, 2.0), reversedCircleGlider.centerOffset)

        val lineGlider = assertIs<CinderellaPointOnLineDefinition>(
            definitions["I"],
        )
        assertEquals("I", lineGlider.name)
        assertEquals("l", lineGlider.lineName)
        assertEquals(listOf(1.0, 3.0), lineGlider.coordinates)

        val midpoint = assertIs<CinderellaMidpointDefinition>(
            definitions["J"],
        )
        assertEquals("J", midpoint.name)
        assertEquals(listOf("A", "B"), midpoint.pointNames)

        val circumcircle = assertIs<CinderellaCircumcircleDefinition>(
            definitions["C2"],
        )
        assertEquals("C2", circumcircle.name)
        assertEquals(listOf("A", "B", "D"), circumcircle.pointNames)
        assertEquals("#7700b7", circumcircle.properties.appearance.values[0])
        assertEquals(0.5, circumcircle.properties.fillOpacity)
        assertTrue(parsed.diagnostics.isEmpty())
    }

    @Test
    fun readCreatesOfficialGeometryAndViewportOnBoard() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnCinderella>>(
            CinderellaReader(CONSTRAINED_SOURCE).read(board),
        ).value

        assertEquals(11, drawn.objects.size)
        assertEquals(12, board.objects.size)
        val pointA = assertIs<Point>(drawn.objects["A"])
        val pointB = assertIs<Point>(drawn.objects["B"])
        val line = assertIs<Line>(drawn.objects["l"])
        val segment = assertIs<Line>(drawn.objects["s"])
        val circle = assertIs<Circle>(drawn.objects["C0"])
        val circleGlider = assertIs<Glider>(drawn.objects["G"])
        val reversedGlider = assertIs<Glider>(drawn.objects["H"])
        val lineGlider = assertIs<Glider>(drawn.objects["I"])
        val midpoint = assertIs<MidpointPoint>(drawn.objects["J"])
        val circumcircle = assertIs<Circle>(drawn.objects["C2"])

        assertEquals(1.0, pointA.X())
        assertEquals(3.0, pointA.Y())
        assertEquals(2.0, pointB.X())
        assertEquals(1.0, pointB.Y())
        assertTrue(line.straightFirst)
        assertTrue(line.straightLast)
        assertFalse(segment.straightFirst)
        assertFalse(segment.straightLast)
        assertEquals(
            kotlin.math.sqrt(5.0),
            circle.Radius(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            2.0,
            circleGlider.X(),
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(1.0, circleGlider.Y(), absoluteTolerance = 1.0e-12)
        assertSame(circle, circleGlider.slideElement)
        assertEquals(0.0, reversedGlider.X(), absoluteTolerance = 1.0e-12)
        assertEquals(5.0, reversedGlider.Y(), absoluteTolerance = 1.0e-12)
        assertEquals(1.0, lineGlider.X(), absoluteTolerance = 1.0e-12)
        assertEquals(3.0, lineGlider.Y(), absoluteTolerance = 1.0e-12)
        assertSame(line, lineGlider.slideElement)
        assertEquals(1.5, midpoint.X())
        assertEquals(2.0, midpoint.Y())
        assertEquals(2.0, circumcircle.center.X())
        assertEquals(2.25, circumcircle.center.Y())
        assertEquals(1.25, circumcircle.Radius())
        assertEquals("", circumcircle.center.name)
        assertEquals(5.0 / 6.0, board.zoomX)
        assertEquals(5.0 / 6.0, board.zoomY)
        assertEquals(162.5, board.origin.scrCoords[1])
        assertEquals(362.5, board.origin.scrCoords[2])
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun advancedLineAndConicDefinitionsMatchOfficialReaderBranches() {
        val parsed = parse(ADVANCED_SOURCE)
        val definitions = parsed.definitions.associateBy { it.name }

        assertEquals(15, parsed.definitions.size)
        val parallel = assertIs<CinderellaParallelDefinition>(
            definitions["b"],
        )
        assertEquals("a", parallel.lineName)
        assertEquals("C", parallel.pointName)
        assertEquals(0, parallel.properties.dashing)

        val orthogonal = assertIs<CinderellaOrthogonalDefinition>(
            definitions["d"],
        )
        assertEquals("b", orthogonal.lineName)
        assertEquals("E", orthogonal.pointName)

        val conic = assertIs<CinderellaConicByFivePointsDefinition>(
            definitions["Q"],
        )
        assertEquals(listOf("A", "B", "C", "D", "E"), conic.pointNames)
        assertEquals(0.5, conic.properties.fillOpacity)

        val ellipse = assertIs<CinderellaConicFociDefinition>(
            definitions["El"],
        )
        assertEquals(listOf("A", "B", "C"), ellipse.pointNames)
        assertFalse(ellipse.hyperbola)

        val hyperbola = assertIs<CinderellaConicFociDefinition>(
            definitions["Hy"],
        )
        assertEquals(listOf("A", "B", "E"), hyperbola.pointNames)
        assertTrue(hyperbola.hyperbola)

        val parabola = assertIs<CinderellaParabolaDefinition>(
            definitions["Pa"],
        )
        assertEquals("C", parabola.focusName)
        assertEquals("a", parabola.directrixName)

        val polygon = assertIs<CinderellaPolygonDefinition>(
            definitions["Poly0"],
        )
        assertEquals(listOf("A", "B", "E", "D"), polygon.pointNames)

        val arc = assertIs<CinderellaArcDefinition>(definitions["Ar"])
        assertEquals(listOf("A", "C", "B"), arc.pointNames)

        val through = assertIs<CinderellaThroughDefinition>(
            definitions["t"],
        )
        assertEquals("D", through.pointName)
        assertEquals(listOf(1.0, -2.0), through.offset)
        assertTrue(parsed.diagnostics.isEmpty())
    }

    @Test
    fun readCreatesOfficialParallelNormalAndConicGeometry() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnCinderella>>(
            CinderellaReader(ADVANCED_SOURCE).read(board),
        ).value

        assertEquals(15, drawn.objects.size)
        val parallel = assertIs<ParallelLine>(drawn.objects["b"])
        val orthogonal = assertIs<Line>(drawn.objects["d"])
        val conic = assertIs<Curve>(drawn.objects["Q"])
        val ellipse = assertIs<Curve>(drawn.objects["El"])
        val hyperbola = assertIs<Curve>(drawn.objects["Hy"])
        val parabola = assertIs<Curve>(drawn.objects["Pa"])
        val polygon = assertIs<Polygon>(drawn.objects["Poly0"])
        val arc = assertIs<Arc>(drawn.objects["Ar"])
        val through = assertIs<Line>(drawn.objects["t"])

        assertCoordinates(
            expected = listOf(3.0, 0.0, -1.0),
            actual = parallel.stdform.take(3),
        )
        assertCoordinates(
            expected = listOf(3.0, -1.0, 0.0),
            actual = orthogonal.stdform.take(3),
        )
        assertEquals(
            listOf(
                assertIs<Line>(drawn.objects["a"]).id,
                assertIs<Point>(drawn.objects["C"]).id,
            ),
            parallel.parents,
        )
        assertEquals(
            listOf(
                parallel.id,
                assertIs<Point>(drawn.objects["E"]).id,
            ),
            orthogonal.parents,
        )

        assertTrue(conic.isGenericConic)
        assertTrue(ellipse.isEllipse)
        assertTrue(hyperbola.isHyperbola)
        assertCurveSample(
            curve = conic,
            parameter = 0.0,
            expectedX = 2.841392385967725,
            expectedY = 0.49190972512645614,
        )
        assertCurveSample(
            curve = conic,
            parameter = kotlin.math.PI / 2.0,
            expectedX = 0.0,
            expectedY = 3.0,
        )
        assertCurveSample(
            curve = ellipse,
            parameter = 0.0,
            expectedX = 3.6055512754639887,
            expectedY = 0.0,
        )
        assertCurveSample(
            curve = hyperbola,
            parameter = kotlin.math.PI / 2.0,
            expectedX = -2.0,
            expectedY = 0.9658625133940745,
        )

        val conicCenter = assertIs<Point>(conic.center)
        assertEquals(0.0, conicCenter.X(), absoluteTolerance = TOLERANCE)
        assertEquals(
            1.326086956521739,
            conicCenter.Y(),
            absoluteTolerance = TOLERANCE,
        )
        val ellipseCenter = assertIs<Point>(ellipse.center)
        assertEquals(0.0, ellipseCenter.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, ellipseCenter.Y(), absoluteTolerance = TOLERANCE)
        val hyperbolaCenter = assertIs<Point>(hyperbola.center)
        assertEquals(0.0, hyperbolaCenter.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, hyperbolaCenter.Y(), absoluteTolerance = TOLERANCE)

        assertTrue(parabola.isParabola)
        assertCurveSample(
            curve = parabola,
            parameter = 0.0,
            expectedX = 3.0,
            expectedY = 3.0,
        )
        assertCurveSample(
            curve = parabola,
            parameter = kotlin.math.PI,
            expectedX = -3.0,
            expectedY = 3.0,
        )
        val parabolaCenter = assertIs<Point>(parabola.center)
        assertEquals(0.0, parabolaCenter.X(), absoluteTolerance = TOLERANCE)
        assertEquals(0.0, parabolaCenter.Y(), absoluteTolerance = TOLERANCE)

        assertEquals(10.0, polygon.Area(), absoluteTolerance = TOLERANCE)
        assertEquals(
            14.47213595499958,
            polygon.Perimeter(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(5, polygon.vertices.size)
        assertSame(polygon.vertices.first(), polygon.vertices.last())

        assertEquals(0.0, arc.center.X(), absoluteTolerance = TOLERANCE)
        assertEquals(
            0.8333333333333334,
            arc.center.Y(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            2.1666666666666665,
            arc.Radius(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            3.9311748929893158,
            arc.Value("radians"),
            absoluteTolerance = TOLERANCE,
        )
        assertCoordinates(
            expected = listOf(
                1.7888543819998317,
                0.8944271909999159,
                0.4472135954999579,
            ),
            actual = through.stdform.take(3),
        )
        assertEquals(-3.0, through.point1.X())
        assertEquals(2.0, through.point1.Y())
        assertEquals(-2.0, through.point2.X())
        assertEquals(0.0, through.point2.Y())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun remainingDefinitionsPreservePairedNamesAndPropertyOrder() {
        val parsed = parse(REMAINING_SOURCE)
        val angular = parsed.definitions
            .filterIsInstance<CinderellaAngularBisectorDefinition>()
        val intersections = parsed.definitions
            .filterIsInstance<CinderellaPairIntersectionDefinition>()

        assertEquals(20, parsed.definitions.size)
        assertEquals(3, angular.size)
        assertEquals("BFirst", angular[0].firstOutputName)
        assertEquals("BSecond", angular[0].secondOutputName)
        assertEquals(listOf("LX", "LY"), angular[0].lineNames)
        assertEquals(
            "#ffc800",
            angular[0].firstProperties.appearance.values.first(),
        )
        assertEquals(0, angular[0].firstProperties.dashing)
        assertEquals(
            "#199e4e",
            angular[0].secondProperties?.appearance?.values?.first(),
        )
        assertEquals(3, angular[0].secondProperties?.dashing)
        assertEquals("BOnlyFirst", angular[1].firstOutputName)
        assertEquals(null, angular[1].secondOutputName)
        assertEquals(null, angular[2].firstOutputName)
        assertEquals("BOnlySecond", angular[2].secondOutputName)

        val meet = assertIs<CinderellaMeetDefinition>(
            parsed.definitions.first { it.name == "M" },
        )
        assertEquals(listOf("LX", "LY"), meet.lineNames)

        assertEquals(6, intersections.size)
        assertEquals(
            CinderellaIntersectionKind.CONIC_LINE,
            intersections[0].kind,
        )
        assertEquals("CLFirst", intersections[0].firstOutputName)
        assertEquals("CLSecond", intersections[0].secondOutputName)
        assertEquals(listOf("C0", "LC"), intersections[0].parentNames)
        assertEquals(
            "red",
            intersections[0].firstProperties.appearance.values.first(),
        )
        assertEquals(
            "blue",
            intersections[0].secondProperties
                ?.appearance
                ?.values
                ?.first(),
        )
        assertEquals("CLOnlyFirst", intersections[1].firstOutputName)
        assertEquals(null, intersections[1].secondOutputName)
        assertEquals(null, intersections[2].firstOutputName)
        assertEquals("CLOnlySecond", intersections[2].secondOutputName)
        assertEquals(
            CinderellaIntersectionKind.CIRCLE_CIRCLE,
            intersections[3].kind,
        )
        assertEquals(listOf("C0", "C1"), intersections[3].parentNames)
        assertTrue(parsed.diagnostics.isEmpty())
    }

    @Test
    fun readCreatesOfficialBisectorsAndIntersectionOrdering() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnCinderella>>(
            CinderellaReader(REMAINING_SOURCE).read(board),
        ).value

        assertEquals(23, drawn.objects.size)
        val firstBisector = assertIs<Line>(drawn.objects["BFirst"])
        val secondBisector = assertIs<Line>(drawn.objects["BSecond"])
        val onlyFirstBisector =
            assertIs<Line>(drawn.objects["BOnlyFirst"])
        val onlySecondBisector =
            assertIs<Line>(drawn.objects["BOnlySecond"])
        val inverseSqrtTwo = 1.0 / kotlin.math.sqrt(2.0)
        assertCoordinates(
            listOf(0.0, -inverseSqrtTwo, -inverseSqrtTwo),
            firstBisector.stdform.take(3),
        )
        assertCoordinates(
            listOf(0.0, inverseSqrtTwo, -inverseSqrtTwo),
            secondBisector.stdform.take(3),
        )
        assertCoordinates(
            firstBisector.stdform.take(3),
            onlyFirstBisector.stdform.take(3),
        )
        assertCoordinates(
            secondBisector.stdform.take(3),
            onlySecondBisector.stdform.take(3),
        )

        val meet = assertIs<IntersectionPoint>(drawn.objects["M"])
        assertPoint(meet, expectedX = 0.0, expectedY = 0.0)

        val circleLineFirst =
            assertIs<OtherIntersectionPoint>(drawn.objects["CLFirst"])
        val circleLineSecond =
            assertIs<IntersectionPoint>(drawn.objects["CLSecond"])
        val circleLineOnlyFirst =
            assertIs<IntersectionPoint>(drawn.objects["CLOnlyFirst"])
        val circleLineOnlySecond =
            assertIs<IntersectionPoint>(drawn.objects["CLOnlySecond"])
        assertPoint(
            circleLineFirst,
            expectedX = kotlin.math.sqrt(3.0),
            expectedY = 1.0,
        )
        assertPoint(
            circleLineSecond,
            expectedX = -kotlin.math.sqrt(3.0),
            expectedY = 1.0,
        )
        assertPoint(
            circleLineOnlyFirst,
            expectedX = circleLineFirst.X(),
            expectedY = circleLineFirst.Y(),
        )
        assertPoint(
            circleLineOnlySecond,
            expectedX = circleLineSecond.X(),
            expectedY = circleLineSecond.Y(),
        )

        val circleCircleFirst =
            assertIs<OtherIntersectionPoint>(drawn.objects["CCFirst"])
        val circleCircleSecond =
            assertIs<IntersectionPoint>(drawn.objects["CCSecond"])
        val circleCircleOnlyFirst =
            assertIs<IntersectionPoint>(drawn.objects["CCOnlyFirst"])
        val circleCircleOnlySecond =
            assertIs<IntersectionPoint>(drawn.objects["CCOnlySecond"])
        assertPoint(
            circleCircleFirst,
            expectedX = 1.0,
            expectedY = kotlin.math.sqrt(3.0),
        )
        assertPoint(
            circleCircleSecond,
            expectedX = 1.0,
            expectedY = -kotlin.math.sqrt(3.0),
        )
        assertPoint(
            circleCircleOnlyFirst,
            expectedX = circleCircleFirst.X(),
            expectedY = circleCircleFirst.Y(),
        )
        assertPoint(
            circleCircleOnlySecond,
            expectedX = circleCircleSecond.X(),
            expectedY = circleCircleSecond.Y(),
        )
        assertSame(circleLineFirst, board.select("CLFirst"))
        assertSame(circleCircleSecond, board.select("CCSecond"))
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun pairedDefinitionFailureRollsBackAllCreatedOutputs() {
        val source = REMAINING_SOURCE.replace(
            "IntersectionConicLine(\"C0\",\"LC\")",
            "IntersectionConicLine(\"missing\",\"LC\")",
        )
        val board = board()

        val failure = assertIs<
            GMResult.Err<CinderellaReaderError.MissingReference>
        >(
            CinderellaReader(source).read(board),
        ).error

        assertEquals("CLFirst", failure.definitionName)
        assertEquals("missing", failure.referenceName)
        assertTrue(board.objects.isEmpty())
        assertTrue(board.elementsByName.isEmpty())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun compassPreservesOfficialFailureAsStructuredResult() {
        val parsed = parse(COMPASS_SOURCE)
        val compass = assertIs<CinderellaCompassDefinition>(
            parsed.definitions.last(),
        )

        assertEquals("C0", compass.name)
        assertEquals(listOf("A", "B", "C"), compass.pointNames)

        val board = board()
        val failure = assertIs<
            GMResult.Err<CinderellaReaderError.BoardCreationFailed>
        >(
            CinderellaReader(COMPASS_SOURCE).read(board),
        ).error

        assertEquals("C0", failure.definitionName)
        assertIs<CinderellaCreationError.UpstreamCompassRadiusFunction>(
            failure.cause,
        )
        assertTrue(board.objects.isEmpty())
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun readFailuresRollBackOnlyNewObjects() {
        val board = board()
        val existing = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(9.0, 9.0),
                name = "existing",
            ),
        ).value
        val source = listOf(
            "<cindyscript>",
            "(\"A\"):=FreePoint([2+i*0,-6+i*0,2+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([4+i*0,-2+i*0,2+i*0]);",
            "\"B\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"a\"):=Join(\"A\",\"B\");",
            "\"a\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"a\".setAttribute(\"linedashing\",\"false\");",
            "(\"b\"):=Parallel(\"a\",\"A\");",
            "\"b\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"b\".setAttribute(\"linedashing\",\"false\");",
            "(\"M\"):=Mid(\"A\",\"missing\");",
            "\"M\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"M\".setAttribute(\"pointborder\",\"true\");",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")

        val failure = assertIs<
            GMResult.Err<CinderellaReaderError.MissingReference>
        >(
            CinderellaReader(source).read(board),
        ).error

        assertEquals("M", failure.definitionName)
        assertEquals("missing", failure.referenceName)
        assertEquals(1, board.objects.size)
        assertSame(existing, board.select("existing"))
        assertFalse(board.isSuspendedUpdate)
    }

    @Test
    fun strictRegistryRejectsUnsupportedDefinitionsBeforeBoardMutation() {
        val board = board()
        val registry = ReaderRegistry<Board>().also {
            it.registerCinderellaReader()
        }

        val failure = assertIs<
            GMResult.Err<ReaderError.DomainFailure>
        >(
            FileReader.parseString(
                source = SOURCE,
                board = board,
                format = "cdy",
                registry = registry,
            ),
        ).error

        assertIs<CinderellaReaderError.UnsupportedDefinitions>(
            failure.cause,
        )
        assertTrue(board.objects.isEmpty())
    }

    @Test
    fun registeredCinderellaFactoryCreatesSupportedSource() {
        val board = board()
        val registry = ReaderRegistry<Board>().also {
            it.registerCinderellaReader()
        }
        var callbackBoard: Board? = null

        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source = CONSTRAINED_SOURCE,
                board = board,
                format = "CINDY",
                registry = registry,
                callback = { callbackBoard = it },
            ),
        )

        assertSame(board, callbackBoard)
        assertEquals(12, board.objects.size)
    }

    @Test
    fun malformedDefinitionsAndLimitsAreStructured() {
        assertIs<GMResult.Err<CinderellaReaderError.MalformedDefinition>>(
            CinderellaReader(
                "<cindyscript>\n(\"A\"):=FreePoint([1,2]);",
            ).parse(),
        )
        assertIs<
            GMResult.Err<CinderellaReaderError.PropertyParsingFailed>
        >(
            CinderellaReader(
                "<cindyscript>\n(\"A\"):=FreePoint([1,2,1]);",
            ).parse(),
        )
        assertIs<GMResult.Err<CinderellaReaderError.InvalidLimits>>(
            CinderellaReader("<cindyscript>").parse(
                limits = CinderellaReaderLimits(maxElements = -1),
            ),
        )
        assertIs<GMResult.Err<CinderellaReaderError.LineLimitExceeded>>(
            CinderellaReader("<cindyscript>\nline").parse(
                limits = CinderellaReaderLimits(maxLines = 1),
            ),
        )
        assertIs<GMResult.Err<CinderellaReaderError.ElementLimitExceeded>>(
            CinderellaReader(SOURCE).parse(
                limits = CinderellaReaderLimits(maxElements = 1),
            ),
        )
    }

    private fun parse(source: String): ParsedCinderella =
        assertIs<GMResult.Ok<ParsedCinderella>>(
            CinderellaReader(source).parse(),
        ).value

    private fun assertCurveSample(
        curve: Curve,
        parameter: Double,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            curve.X(parameter),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expectedY,
            curve.Y(parameter),
            absoluteTolerance = TOLERANCE,
        )
    }

    private fun assertCoordinates(
        expected: List<Double>,
        actual: List<Double>,
    ) {
        assertEquals(expected.size, actual.size)
        for (index in expected.indices) {
            assertEquals(
                expected[index],
                actual[index],
                absoluteTolerance = TOLERANCE,
            )
        }
    }

    private fun assertPoint(
        point: Point,
        expectedX: Double,
        expectedY: Double,
    ) {
        assertEquals(
            expectedX,
            point.X(),
            absoluteTolerance = TOLERANCE,
        )
        assertEquals(
            expectedY,
            point.Y(),
            absoluteTolerance = TOLERANCE,
        )
    }

    private companion object {
        const val TOLERANCE = 1.0e-12

        val SOURCE = listOf(
            "<cindyscript>",
            "(\"A\"):=FreePoint([2+i*0,-6+i*0,2+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"color\",\"2\");",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"l\"):=Join(\"A\",\"B\");",
            "\"l\".setAppearance(9,5,5,0,0,9,true,false);",
            "\"l\".setAttribute(\"linedashing\",\"true\");",
            "(\"s\"):=Segment(\"A\",\"B\");",
            "\"s\".setAppearance(11,5,2,0,0,9,true,false);",
            "\"s\".setAttribute(\"linedashing\",\"false\");",
            "(\"C0\"):=CircleMP(\"A\",\"B\");",
            "\"C0\".setAppearance(14,5,2,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"5\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"4\");",
            "(\"C1\"):=CircleByFixedRadius(\"A\",9+i*0);",
            "\"C1\".setAppearance(11,5,4,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"4\");",
            "\"C1\".setAttribute(\"fillalpha\",\"0.25\");",
            "(\"Z\"):=Unknown(\"A\");",
            "setOriginX(12.5);",
            "setOriginY(20);",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")

        val CONSTRAINED_SOURCE = listOf(
            "<cindyscript>",
            "(\"A\"):=FreePoint([2+i*0,-6+i*0,2+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([4+i*0,-2+i*0,2+i*0]);",
            "\"B\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"D\"):=FreePoint([3+i*0,-3+i*0,1+i*0]);",
            "\"D\".setAppearance(4,5,1,0,0,9,true,false);",
            "\"D\".setAttribute(\"pointborder\",\"true\");",
            "(\"l\"):=Join(\"A\",\"B\");",
            "\"l\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"l\".setAttribute(\"linedashing\",\"false\");",
            "(\"s\"):=Segment(\"A\",\"B\");",
            "\"s\".setAppearance(11,5,2,0,0,9,true,false);",
            "\"s\".setAttribute(\"linedashing\",\"false\");",
            "(\"C0\"):=CircleMP(\"A\",\"B\");",
            "\"C0\".setAppearance(14,5,2,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"5\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"4\");",
            "{\"G\",null,[1+i*0,0+i*0,1+i*0]}:=" +
                "PointOnCircle(\"C0\",[1+i*0,2+i*0,0+i*0]);",
            "\"G\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"G\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"H\",[1+i*0,0+i*0,1+i*0]}:=" +
                "PointOnCircle(\"C0\",[1+i*0,2+i*0,0+i*0]);",
            "\"H\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"H\".setAttribute(\"pointborder\",\"true\");",
            "(\"I\"):=PointOnLine(\"l\",[2+i*0,-6+i*0,2+i*0]);",
            "\"I\".setAppearance(4,5,1,0,0,9,true,false);",
            "\"I\".setAttribute(\"pointborder\",\"true\");",
            "(\"J\"):=Mid(\"A\",\"B\");",
            "\"J\".setAppearance(5,5,1,0,0,9,true,false);",
            "\"J\".setAttribute(\"pointborder\",\"true\");",
            "(\"C2\"):=CircleBy3(\"A\",\"B\",\"D\");",
            "\"C2\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C2\".setAttribute(\"colorfill\",\"7\");",
            "\"C2\".setAttribute(\"visibilityfill\",\"5\");",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")

        val ADVANCED_SOURCE = listOf(
            "<cindyscript>",
            "(\"A\"):=FreePoint([-2+i*0,0+i*0,1+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"B\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"C\"):=FreePoint([0+i*0,-3+i*0,1+i*0]);",
            "\"C\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"C\".setAttribute(\"pointborder\",\"true\");",
            "(\"D\"):=FreePoint([-3+i*0,-2+i*0,1+i*0]);",
            "\"D\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"D\".setAttribute(\"pointborder\",\"true\");",
            "(\"E\"):=FreePoint([3+i*0,-2+i*0,1+i*0]);",
            "\"E\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"E\".setAttribute(\"pointborder\",\"true\");",
            "(\"a\"):=Join(\"A\",\"B\");",
            "\"a\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"a\".setAttribute(\"linedashing\",\"false\");",
            "(\"b\"):=Parallel(\"a\",\"C\");",
            "\"b\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"b\".setAttribute(\"linedashing\",\"false\");",
            "(\"d\"):=Orthogonal(\"b\",\"E\");",
            "\"d\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"d\".setAttribute(\"linedashing\",\"false\");",
            "(\"Q\"):=ConicBy5(\"A\",\"B\",\"C\",\"D\",\"E\");",
            "\"Q\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Q\".setAttribute(\"colorfill\",\"7\");",
            "\"Q\".setAttribute(\"visibilityfill\",\"5\");",
            "{null,\"El\",[1+i*0,0+i*0,0+i*0]}:=" +
                "ConicFoci(\"A\",\"B\",\"C\");",
            "\"El\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"El\".setAttribute(\"colorfill\",\"7\");",
            "\"El\".setAttribute(\"visibilityfill\",\"5\");",
            "{\"Hy\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "ConicFociH(\"A\",\"B\",\"E\");",
            "\"Hy\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Hy\".setAttribute(\"colorfill\",\"7\");",
            "\"Hy\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Pa\"):=ConicParabolaPL(\"C\",\"a\");",
            "\"Pa\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Pa\".setAttribute(\"colorfill\",\"7\");",
            "\"Pa\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Poly0\"):=Poly(\"A\",\"B\",\"E\",\"D\");",
            "\"Poly0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Poly0\".setAttribute(\"colorfill\",\"7\");",
            "\"Poly0\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Ar\"):=Arc(\"A\",\"C\",\"B\");",
            "\"Ar\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Ar\".setAttribute(\"colorfill\",\"7\");",
            "\"Ar\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"t\"):=Through(\"D\",[1+i*0,-2+i*0,0+i*0]);",
            "\"t\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"t\".setAttribute(\"linedashing\",\"false\");",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")

        val COMPASS_SOURCE = listOf(
            "<cindyscript>",
            "(\"A\"):=FreePoint([-2+i*0,0+i*0,1+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"B\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"C\"):=FreePoint([0+i*0,-3+i*0,1+i*0]);",
            "\"C\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"C\".setAttribute(\"pointborder\",\"true\");",
            "(\"C0\"):=Compass(\"A\",\"B\",\"C\");",
            "\"C0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"7\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"5\");",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")

        val REMAINING_SOURCE = listOf(
            "<cindyscript>",
            "(\"O\"):=FreePoint([0+i*0,0+i*0,1+i*0]);",
            "\"O\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"O\".setAttribute(\"pointborder\",\"true\");",
            "(\"X\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"X\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"X\".setAttribute(\"pointborder\",\"true\");",
            "(\"Y\"):=FreePoint([0+i*0,-2+i*0,1+i*0]);",
            "\"Y\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"Y\".setAttribute(\"pointborder\",\"true\");",
            "(\"P\"):=FreePoint([-3+i*0,-1+i*0,1+i*0]);",
            "\"P\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"P\".setAttribute(\"pointborder\",\"true\");",
            "(\"Q\"):=FreePoint([3+i*0,-1+i*0,1+i*0]);",
            "\"Q\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"Q\".setAttribute(\"pointborder\",\"true\");",
            "(\"LX\"):=Join(\"O\",\"X\");",
            "\"LX\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LX\".setAttribute(\"linedashing\",\"false\");",
            "(\"LY\"):=Join(\"Y\",\"O\");",
            "\"LY\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LY\".setAttribute(\"linedashing\",\"false\");",
            "(\"LC\"):=Join(\"P\",\"Q\");",
            "\"LC\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LC\".setAttribute(\"linedashing\",\"false\");",
            "{\"BFirst\",\"BSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BSecond\".setAppearance(8,5,1,0,0,9,true,false);",
            "\"BSecond\".setAttribute(\"linedashing\",\"false\");",
            "\"BFirst\".setAppearance(9,5,3,0,0,9,true,false);",
            "\"BFirst\".setAttribute(\"linedashing\",\"true\");",
            "{\"BOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BOnlyFirst\".setAppearance(9,5,3,0,0,9,true,false);",
            "\"BOnlyFirst\".setAttribute(\"linedashing\",\"true\");",
            "{null,\"BOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BOnlySecond\".setAppearance(8,5,1,0,0,9,true,false);",
            "\"BOnlySecond\".setAttribute(\"linedashing\",\"false\");",
            "(\"M\"):=Meet(\"LX\",\"LY\");",
            "\"M\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"M\".setAttribute(\"pointborder\",\"true\");",
            "(\"C0\"):=CircleMP(\"O\",\"X\");",
            "\"C0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"7\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"0\");",
            "(\"C1\"):=CircleMP(\"X\",\"O\");",
            "\"C1\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"7\");",
            "\"C1\".setAttribute(\"visibilityfill\",\"0\");",
            "{\"CLFirst\",\"CLSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLSecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLSecond\".setAttribute(\"pointborder\",\"true\");",
            "\"CLFirst\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"CLFirst\".setAttribute(\"pointborder\",\"true\");",
            "{\"CLOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLOnlyFirst\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLOnlyFirst\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"CLOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLOnlySecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLOnlySecond\".setAttribute(\"pointborder\",\"true\");",
            "{\"CCFirst\",\"CCSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCSecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCSecond\".setAttribute(\"pointborder\",\"true\");",
            "\"CCFirst\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"CCFirst\".setAttribute(\"pointborder\",\"true\");",
            "{\"CCOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCOnlyFirst\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCOnlyFirst\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"CCOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCOnlySecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCOnlySecond\".setAttribute(\"pointborder\",\"true\");",
            "setScale(50);",
            "</cindyscript>",
        ).joinToString("\n")
    }

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
        canvasWidth = 500.0,
        canvasHeight = 500.0,
    )
}
