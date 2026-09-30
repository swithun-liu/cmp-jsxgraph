/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.reader

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Circle
import com.swithun.jsxgraph.core.base.Curve
import com.swithun.jsxgraph.core.base.BisectorLine
import com.swithun.jsxgraph.core.base.Glider
import com.swithun.jsxgraph.core.base.IntersectionPoint
import com.swithun.jsxgraph.core.base.Line
import com.swithun.jsxgraph.core.base.MidpointPoint
import com.swithun.jsxgraph.core.base.Point
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.math.abs
import kotlin.math.sqrt

class IntergeoReaderTest {
    @Test
    fun elementsMatchOfficialParseOnlyFixture() {
        val parsed = parse(FIXTURE_SOURCE)

        assertEquals(
            listOf("H", "CR", "E", "P", "L", "S", "C", "Q"),
            parsed.objects.keys.toList(),
        )
        assertObject<IntergeoPointObject>(
            parsed = parsed,
            id = "H",
            expectedCoords = listOf(4.0, 2.0, 3.0),
        )
        assertObject<IntergeoPointObject>(
            parsed = parsed,
            id = "CR",
            expectedCoords = listOf(4.0, 2.0, 3.0),
        )
        assertObject<IntergeoPointObject>(
            parsed = parsed,
            id = "E",
            expectedCoords = listOf(5.0, 6.0),
        )
        val polar = assertIs<IntergeoPointObject>(parsed.objects["P"])
        assertEquals(
            1.2246467991473532e-16,
            polar.coords[0],
            absoluteTolerance = 1e-30,
        )
        assertEquals(2.0, polar.coords[1])
        assertObject<IntergeoLineObject>(
            parsed = parsed,
            id = "L",
            expectedCoords = listOf(1.0, 2.0, 3.0),
        )
        assertObject<IntergeoLineObject>(
            parsed = parsed,
            id = "S",
            expectedCoords = listOf(4.0, 5.0, 6.0),
        )
        assertObject<IntergeoConicObject>(
            parsed = parsed,
            id = "C",
            expectedCoords = listOf(
                1.0,
                0.0,
                -1.0,
                0.0,
                1.0,
                -3.0,
                -1.0,
                -3.0,
                6.0,
            ),
        )
        assertObject<IntergeoConicObject>(
            parsed = parsed,
            id = "Q",
            expectedCoords = listOf(1.0, 2.0, 3.0),
        )
    }

    @Test
    fun constraintParamsAndDiagnosticsMatchOfficialFixture() {
        val parsed = parse(FIXTURE_SOURCE)

        assertEquals(
            listOf(
                IntergeoConstraint(
                    name = "line_through_two_points",
                    parameters = listOf("L", "H", "E"),
                    supportedByUpstreamReader = true,
                ),
                IntergeoConstraint(
                    name = "free_point",
                    parameters = listOf("H"),
                    supportedByUpstreamReader = false,
                ),
                IntergeoConstraint(
                    name = "translate",
                    parameters = listOf("T", "H"),
                    supportedByUpstreamReader = false,
                ),
            ),
            parsed.constraints,
        )
        assertEquals(
            listOf(
                IntergeoReaderDiagnostic.UnsupportedPointCoordinates(
                    elementId = "BAD",
                    valueCount = 2,
                ),
                IntergeoReaderDiagnostic.UnsupportedCoordinateType(
                    elementType = "point",
                    elementId = "U",
                    coordinateType = "cartesian_coordinates",
                ),
                IntergeoReaderDiagnostic.UnsupportedElement(
                    elementType = "polygon",
                    elementId = "PG",
                ),
                IntergeoReaderDiagnostic.UnsupportedConstraint(
                    constraintType = "free_point",
                    firstParameter = "H",
                ),
                IntergeoReaderDiagnostic.UnsupportedConstraint(
                    constraintType = "translate",
                    firstParameter = "T",
                ),
            ),
            parsed.diagnostics,
        )
    }

    @Test
    fun malformedInputFailuresAreStructured() {
        assertIs<GMResult.Err<IntergeoReaderError.XmlParsingFailed>>(
            IntergeoReader("<construction>").parse(),
        )
        assertIs<GMResult.Err<IntergeoReaderError.MissingSection>>(
            IntergeoReader(
                "<construction><elements/></construction>",
            ).parse(),
        )
        assertIs<GMResult.Err<IntergeoReaderError.MissingElementId>>(
            IntergeoReader(
                "<construction><elements>" +
                    "<point><euclidean_coordinates>" +
                    "<double>1</double><double>2</double>" +
                    "</euclidean_coordinates></point>" +
                    "</elements><constraints/></construction>",
            ).parse(),
        )
        assertIs<GMResult.Err<IntergeoReaderError.MissingTextValue>>(
            IntergeoReader(
                "<construction><elements>" +
                    "<point id=\"P\"><euclidean_coordinates>" +
                    "<double/><double>2</double>" +
                    "</euclidean_coordinates></point>" +
                    "</elements><constraints/></construction>",
            ).parse(),
        )
        assertIs<GMResult.Err<IntergeoReaderError.PreparationFailed>>(
            IntergeoReader("AQIDBA==").parse(),
        )
    }

    @Test
    fun parserResourceLimitsAreStructured() {
        assertIs<GMResult.Err<IntergeoReaderError.InvalidLimits>>(
            IntergeoReader(EMPTY_SOURCE).parse(
                IntergeoReaderLimits(maxElements = -1),
            ),
        )
        assertIs<GMResult.Err<IntergeoReaderError.ElementLimitExceeded>>(
            IntergeoReader(FIXTURE_SOURCE).parse(
                IntergeoReaderLimits(maxElements = 0),
            ),
        )
        assertIs<GMResult.Err<IntergeoReaderError.ConstraintLimitExceeded>>(
            IntergeoReader(FIXTURE_SOURCE).parse(
                IntergeoReaderLimits(maxConstraints = 0),
            ),
        )
        assertIs<GMResult.Err<IntergeoReaderError.ParameterLimitExceeded>>(
            IntergeoReader(FIXTURE_SOURCE).parse(
                IntergeoReaderLimits(maxParameters = 2),
            ),
        )
    }

    @Test
    fun readCreatesSupportedConstraintsAndCleansUpStoredObjects() {
        val board = board()
        val drawn = assertIs<GMResult.Ok<DrawnIntergeo>>(
            IntergeoReader(BOARD_SOURCE).read(board),
        ).value

        val pointA = assertIs<Point>(drawn.objects["A"])
        val pointB = assertIs<Point>(drawn.objects["B"])
        val pointC = assertIs<Point>(drawn.objects["C"])
        val line = assertIs<Line>(drawn.objects["L"])
        val segment = assertIs<Line>(drawn.objects["S"])
        val freeLine = assertIs<Line>(drawn.objects["F"])
        val midpoint = assertIs<MidpointPoint>(drawn.objects["M"])
        val segmentMidpoint = assertIs<MidpointPoint>(drawn.objects["N"])
        val intersection = assertIs<IntersectionPoint>(drawn.objects["X"])
        val circle = assertIs<Circle>(drawn.objects["K"])
        val conic = assertIs<Curve>(drawn.objects["Q"])
        val lineGlider = assertIs<Glider>(drawn.objects["U"])
        val circleGlider = assertIs<Glider>(drawn.objects["G"])
        val vector = assertIs<Line>(drawn.objects["V"])
        val circumcircle = assertIs<Circle>(drawn.objects["R"])
        val bisector = assertIs<BisectorLine>(drawn.objects["D"])

        assertEquals(400.0, board.origin.scrCoords[1])
        assertEquals(300.0, board.origin.scrCoords[2])
        assertEquals(30.0, board.unitX)
        assertEquals(30.0, board.unitY)
        assertSame(pointA, line.point1)
        assertSame(pointB, line.point2)
        assertTrue(line.straightFirst)
        assertTrue(line.straightLast)
        assertSame(pointB, segment.point1)
        assertSame(pointC, segment.point2)
        assertFalse(segment.straightFirst)
        assertFalse(segment.straightLast)
        assertEquals(listOf(1.0, -1.0, 0.0), freeLine.stdform.take(3))
        assertEquals(1.0, midpoint.X())
        assertEquals(0.0, midpoint.Y())
        assertEquals(1.0, segmentMidpoint.X())
        assertEquals(1.0, segmentMidpoint.Y())
        assertEquals(1.0, intersection.X())
        assertEquals(0.0, intersection.Y(), absoluteTolerance = 0.0)
        assertSame(pointA, circle.center)
        assertEquals(2.0, circle.Radius())
        assertEquals("curve", conic.elType)
        assertEquals(0.0, lineGlider.X())
        assertEquals(0.0, lineGlider.Y())
        assertEquals(2.0, circleGlider.X())
        assertEquals(0.0, circleGlider.Y())
        assertEquals("arrow", vector.elType)
        assertFalse(vector.straightFirst)
        assertFalse(vector.straightLast)
        assertFalse(circumcircle.center.id == "Rc")
        assertEquals("", circumcircle.center.name)
        assertEquals(1.0, circumcircle.center.X())
        assertEquals(1.0, circumcircle.center.Y())
        assertFalse(bisector.straightFirst)
        assertTrue(bisector.straightLast)
        assertFalse(board.isSuspendedUpdate)
        assertTrue(drawn.diagnostics.isEmpty())
    }

    @Test
    fun untranslatedBoardConstraintRemainsAnExplicitDiagnostic() {
        val drawn = assertIs<GMResult.Ok<DrawnIntergeo>>(
            IntergeoReader(
                listOf(
                    "<construction><elements>",
                    "<point id=\"P\"><euclidean_coordinates>",
                    "<double>1</double><double>2</double>",
                    "</euclidean_coordinates></point>",
                    "</elements><constraints>",
                    "<locus_defined_by_point><point>P</point>",
                    "</locus_defined_by_point>",
                    "</constraints></construction>",
                ).joinToString(""),
            ).read(board()),
        ).value

        assertEquals(
            listOf(
                IntergeoReaderDiagnostic.UnsupportedBoardConstraint(
                    constraintType = "locus_defined_by_point",
                    firstParameter = "P",
                ),
            ),
            drawn.diagnostics,
        )
        assertIs<Point>(drawn.objects["P"])
    }

    @Test
    fun registeredIntergeoReaderRejectsPartialBoardReadsBeforeMutation() {
        val registry = ReaderRegistry<Board>().also {
            it.registerIntergeoReader()
        }
        val supportedBoard = board()
        assertIs<GMResult.Ok<Unit>>(
            FileReader.parseString(
                source = BOARD_SOURCE,
                board = supportedBoard,
                format = "I2G",
                registry = registry,
            ),
        )
        assertIs<Circle>(supportedBoard.select("K"))

        val unsupportedBoard = board()
        val failure = assertIs<
            GMResult.Err<ReaderError.DomainFailure>
        >(
            FileReader.parseString(
                source = listOf(
                    "<construction><elements>",
                    "<point id=\"P\"><euclidean_coordinates>",
                    "<double>1</double><double>2</double>",
                    "</euclidean_coordinates></point>",
                    "</elements><constraints>",
                    "<locus_defined_by_point><point>P</point>",
                    "</locus_defined_by_point>",
                    "</constraints></construction>",
                ).joinToString(""),
                board = unsupportedBoard,
                format = "intergeo",
                registry = registry,
            ),
        ).error
        val cause = assertIs<
            IntergeoReaderError.UnsupportedBoardConstraints
        >(failure.cause)
        assertEquals(listOf("locus_defined_by_point"), cause.constraintTypes)
        assertTrue(unsupportedBoard.objects.isEmpty())
        assertEquals(250.0, unsupportedBoard.origin.scrCoords[1])
        assertEquals(250.0, unsupportedBoard.origin.scrCoords[2])
    }

    @Test
    fun readCreatesIntersectionAndTwoLineBisectorConstraints() {
        val drawn = assertIs<GMResult.Ok<DrawnIntergeo>>(
            IntergeoReader(INTERSECTION_SOURCE).read(board()),
        ).value

        val circleCircle = listOf(
            assertIs<IntersectionPoint>(drawn.objects["CC1"]),
            assertIs<IntersectionPoint>(drawn.objects["CC2"]),
        )
        val circleCircleY = circleCircle.map(Point::Y).sorted()
        assertEquals(
            -sqrt(3.0),
            circleCircleY[0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            sqrt(3.0),
            circleCircleY[1],
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(circleCircle.all { abs(it.X() - 1.0) < 1.0e-12 })

        val circleLine = listOf(
            assertIs<IntersectionPoint>(drawn.objects["CL1"]),
            assertIs<IntersectionPoint>(drawn.objects["CL2"]),
        )
        val other = assertIs<Point>(drawn.objects["O"])
        val circleLineY = circleLine.map(Point::Y).sorted()
        assertEquals(
            -sqrt(3.0),
            circleLineY[0],
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            sqrt(3.0),
            circleLineY[1],
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(circleLine.all { abs(it.X() - 1.0) < 1.0e-12 })
        assertTrue(
            circleLine.any {
                abs(it.Dist(other)) < 1.0e-12
            },
        )
        assertTrue(
            circleLine.any {
                abs(it.Dist(other)) > 1.0
            },
        )

        val firstBisector = assertIs<Line>(drawn.objects["B1"])
        val secondBisector = assertIs<Line>(drawn.objects["B2"])
        assertTrue(abs(abs(firstBisector.Slope()) - 1.0) < 1.0e-12)
        assertTrue(abs(abs(secondBisector.Slope()) - 1.0) < 1.0e-12)
        assertTrue(
            firstBisector.Slope() * secondBisector.Slope() < 0.0,
        )
    }

    @Test
    fun boardCreationFailureRollsBackOnlyReaderObjects() {
        val board = board()
        val existing = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(9.0, 9.0),
                id = "existing",
            ),
        ).value
        val result = IntergeoReader(
            listOf(
                "<construction><elements>",
                "<point id=\"A\"><euclidean_coordinates>",
                "<double>0</double><double>0</double>",
                "</euclidean_coordinates></point>",
                "<conic id=\"Q\"><matrix>",
                "<double>1</double><double>2</double><double>3</double>",
                "</matrix></conic>",
                "</elements><constraints/></construction>",
            ).joinToString(""),
        ).read(board)

        assertIs<
            GMResult.Err<IntergeoReaderError.InvalidStoredCoordinateCount>
        >(result)
        assertEquals(listOf("existing"), board.objects.keys.toList())
        assertSame(existing, board.elementById("existing"))
        assertFalse(board.isSuspendedUpdate)
    }

    private inline fun <reified T : IntergeoStoredObject> assertObject(
        parsed: ParsedIntergeo,
        id: String,
        expectedCoords: List<Double>,
    ) {
        val objectValue = assertIs<T>(parsed.objects[id])
        assertEquals(expectedCoords, objectValue.coords)
        assertFalse(objectValue.exists)
    }

    private fun parse(source: String): ParsedIntergeo =
        assertIs<GMResult.Ok<ParsedIntergeo>>(
            IntergeoReader(source).parse(),
        ).value

    private fun board(): Board = Board(
        originX = 250.0,
        originY = 250.0,
        unitX = 50.0,
        unitY = 50.0,
    )

    private companion object {
        const val EMPTY_SOURCE =
            "<construction><elements/><constraints/></construction>"

        val FIXTURE_SOURCE = listOf(
            "<construction>",
            "  <elements>",
            "    <point id=\"H\"><homogeneous_coordinates>",
            "      <double>2junk</double><double>3</double><double>4</double>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"CR\"><homogeneous_coordinates>",
            "      <complex><double>2</double><double>0</double></complex>",
            "      <complex><double>3</double><double>0</double></complex>",
            "      <complex><double>4</double><double>0</double></complex>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"E\"><euclidian_coordinates>",
            "      <double>5</double><double>6</double>",
            "    </euclidian_coordinates></point>",
            "    <point id=\"P\"><polar_coordinates>",
            "      <double>2</double><double>1.5707963267948966</double>",
            "    </polar_coordinates></point>",
            "    <point id=\"BAD\"><homogeneous_coordinates>",
            "      <double>1</double><double>2</double>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"U\"><cartesian_coordinates>",
            "      <double>7</double><double>8</double>",
            "    </cartesian_coordinates></point>",
            "    <line id=\"L\"><homogeneous_coordinates>",
            "      <double>1</double><double>2</double><double>3</double>",
            "    </homogeneous_coordinates></line>",
            "    <line_segment id=\"S\"><homogeneous_coordinates>",
            "      <double>4</double><double>5</double><double>6</double>",
            "    </homogeneous_coordinates><homogeneous_coordinates>",
            "      <double>7</double><double>8</double><double>9</double>",
            "    </homogeneous_coordinates></line_segment>",
            "    <circle id=\"C\"><matrix>",
            "      <double>1</double><double>0</double><double>-1</double>",
            "      <double>0</double><double>1</double><double>-3</double>",
            "      <double>-1</double><double>-3</double><double>6</double>",
            "    </matrix></circle>",
            "    <conic id=\"Q\"><matrix>",
            "      <double>1</double><double>2</double><double>3</double>",
            "    </matrix></conic>",
            "    <polygon id=\"PG\"/>",
            "  </elements>",
            "  <constraints>",
            "    <line_through_two_points>",
            "      <line out=\"true\">L</line><point>H</point><point>E</point>",
            "    </line_through_two_points>",
            "    <free_point><point out=\"true\">H</point></free_point>",
            "    <translate><point out=\"true\">T</point><point>H</point>",
            "    </translate>",
            "  </constraints>",
            "</construction>",
        ).joinToString("\n")

        val BOARD_SOURCE = listOf(
            "<construction>",
            "  <elements>",
            "    <point id=\"A\"><euclidean_coordinates>",
            "      <double>0</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"B\"><euclidean_coordinates>",
            "      <double>2</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"C\"><euclidean_coordinates>",
            "      <double>0</double><double>2</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"U\"><euclidean_coordinates>",
            "      <double>1</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"M\"><euclidean_coordinates>",
            "      <double>0</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"N\"><euclidean_coordinates>",
            "      <double>0</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"G\"><homogeneous_coordinates>",
            "      <double>2</double><double>0</double><double>1</double>",
            "    </homogeneous_coordinates></point>",
            "    <line id=\"F\"><homogeneous_coordinates>",
            "      <double>1</double><double>0</double><double>-1</double>",
            "    </homogeneous_coordinates></line>",
            "    <vector id=\"V\"><homogeneous_coordinates>",
            "      <double>0</double><double>1</double><double>0</double>",
            "    </homogeneous_coordinates></vector>",
            "    <circle id=\"K\"><matrix>",
            "      <double>1</double><double>0</double><double>0</double>",
            "      <double>0</double><double>1</double><double>0</double>",
            "      <double>0</double><double>0</double><double>-4</double>",
            "    </matrix></circle>",
            "    <circle id=\"R\"><matrix>",
            "      <double>1</double><double>0</double><double>-1</double>",
            "      <double>0</double><double>1</double><double>-1</double>",
            "      <double>-1</double><double>-1</double><double>1</double>",
            "    </matrix></circle>",
            "    <conic id=\"Q\"><matrix>",
            "      <double>1</double><double>0</double><double>0</double>",
            "      <double>0</double><double>1</double><double>0</double>",
            "      <double>0</double><double>0</double><double>-1</double>",
            "    </matrix></conic>",
            "  </elements>",
            "  <constraints>",
            "    <line_through_two_points>",
            "      <line>L</line><point>A</point><point>B</point>",
            "    </line_through_two_points>",
            "    <line_segment_by_points>",
            "      <line_segment>S</line_segment>",
            "      <point>B</point><point>C</point>",
            "    </line_segment_by_points>",
            "    <free_line><line>F</line></free_line>",
            "    <midpoint_of_two_points>",
            "      <point>M</point><point>A</point><point>B</point>",
            "    </midpoint_of_two_points>",
            "    <midpoint_of_line_segment>",
            "      <point>N</point><line_segment>S</line_segment>",
            "    </midpoint_of_line_segment>",
            "    <point_intersection_of_two_lines>",
            "      <point>X</point><line>L</line><line>F</line>",
            "    </point_intersection_of_two_lines>",
            "    <circle_by_center_and_point>",
            "      <circle>K</circle><point>A</point><point>C</point>",
            "    </circle_by_center_and_point>",
            "    <point_on_line><point>U</point><line>L</line>",
            "    </point_on_line>",
            "    <point_on_circle><point>G</point><circle>K</circle>",
            "    </point_on_circle>",
            "    <vector_from_point_to_point>",
            "      <vector>V</vector><point>A</point><point>C</point>",
            "    </vector_from_point_to_point>",
            "    <circle_by_three_points>",
            "      <circle>R</circle><point>A</point>",
            "      <point>B</point><point>C</point>",
            "    </circle_by_three_points>",
            "    <angular_bisector_of_three_points>",
            "      <line>D</line><point>A</point>",
            "      <point>B</point><point>C</point>",
            "    </angular_bisector_of_three_points>",
            "  </constraints>",
            "</construction>",
        ).joinToString("\n")

        val INTERSECTION_SOURCE = listOf(
            "<construction>",
            "  <elements>",
            "    <point id=\"A\"><euclidean_coordinates>",
            "      <double>0</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"B\"><euclidean_coordinates>",
            "      <double>2</double><double>0</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"C\"><euclidean_coordinates>",
            "      <double>0</double><double>2</double>",
            "    </euclidean_coordinates></point>",
            "    <point id=\"D\"><euclidean_coordinates>",
            "      <double>2</double><double>2</double>",
            "    </euclidean_coordinates></point>",
            "    <line id=\"F\"><homogeneous_coordinates>",
            "      <double>1</double><double>0</double><double>-1</double>",
            "    </homogeneous_coordinates></line>",
            "  </elements>",
            "  <constraints>",
            "    <line_through_two_points>",
            "      <line>L</line><point>A</point><point>B</point>",
            "    </line_through_two_points>",
            "    <circle_by_center_and_point>",
            "      <circle>K</circle><point>A</point><point>C</point>",
            "    </circle_by_center_and_point>",
            "    <circle_by_center_and_point>",
            "      <circle>R</circle><point>B</point><point>D</point>",
            "    </circle_by_center_and_point>",
            "    <free_line><line>F</line></free_line>",
            "    <intersection_points_of_two_circles>",
            "      <point>CC1</point><point>CC2</point>",
            "      <circle>K</circle><circle>R</circle>",
            "    </intersection_points_of_two_circles>",
            "    <intersection_points_of_circle_and_line>",
            "      <point>CL1</point><point>CL2</point>",
            "      <circle>K</circle><line>F</line>",
            "    </intersection_points_of_circle_and_line>",
            "    <other_intersection_point_of_circle_and_line>",
            "      <point>O</point><point>CL1</point>",
            "      <circle>K</circle><line>F</line>",
            "    </other_intersection_point_of_circle_and_line>",
            "    <angular_bisectors_of_two_lines>",
            "      <line>B1</line><line>B2</line>",
            "      <line>L</line><line>F</line>",
            "    </angular_bisectors_of_two_lines>",
            "  </constraints>",
            "</construction>",
        ).joinToString("\n")
    }
}
