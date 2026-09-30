/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CurveGliderTest {
    @Test
    fun functionGraphGliderProjectsDragsAndTracksParentUpdates() {
        val board = board("function-graph")
        val driver = point(board, 0.0, 1.0, "driver")
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "driver.Y() + 0.25 * x * x",
                minimumSource = "-4",
                maximumSource = "4",
                sampleCount = 64,
                id = "functionGraph",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(1.5, 4.0),
                slideObject = curve,
                id = "functionGlider",
                name = "",
            ),
        )

        assertPoint(
            x = 2.8622543136990153,
            y = 3.048124939072155,
            point = glider,
        )
        assertEquals(
            2.8622543136990153,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(curve, glider.slideElement)
        assertSame(curve, glider.slideObject)
        assertSame(curve, glider.slideObjects.single())
        assertSame(glider, curve.childElements[glider.id])
        assertEquals(listOf(curve.id), glider.parents)

        glider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, 0.0),
        )
        board.update(draggedElement = glider)
        assertPoint(
            x = 1.6354632777583658,
            y = 1.6686850332240344,
            point = glider,
        )
        assertEquals(
            1.6354632777583658,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )

        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 2.0),
        )
        board.update()
        assertPoint(
            x = 1.6354632777583658,
            y = 1.6686850332240344,
            point = glider,
        )

        board.update()
        assertPoint(
            x = 1.6354632777583658,
            y = 2.6686850332240344,
            point = glider,
        )
        assertEquals(
            1.6354632777583658,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun parametricAndDataGlidersMatchOfficialProjectionParameters() {
        val parametricBoard = board("parametric")
        val parametric = curve(
            Curve.createParametric(
                board = parametricBoard,
                xSource = "2 * cos(x)",
                ySource = "sin(x)",
                minimumSource = "0",
                maximumSource = (2.0 * PI).toString(),
                sampleCount = 64,
                id = "parametric",
                name = "",
            ),
        )
        val parametricGlider = glider(
            Glider.create(
                board = parametricBoard,
                coordinates = doubleArrayOf(3.0, 0.4),
                slideObject = parametric,
                id = "parametricGlider",
                name = "",
            ),
        )
        assertPoint(
            x = 1.9827506191050663,
            y = 0.13105340747046232,
            point = parametricGlider,
        )
        assertEquals(
            0.13143147710988975,
            assertIs<Double>(parametricGlider.position),
            absoluteTolerance = TOLERANCE,
        )

        parametricGlider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-3.0, 0.2),
        )
        parametricBoard.update(draggedElement = parametricGlider)
        assertPoint(
            x = -1.9955897190913985,
            y = 0.06637332494442595,
            point = parametricGlider,
        )
        assertEquals(
            3.075170498070034,
            assertIs<Double>(parametricGlider.position),
            absoluteTolerance = TOLERANCE,
        )

        parametricGlider.setGliderPosition(PI * 0.5)
        assertPoint(
            x = -1.9955897190913985,
            y = 0.06637332494442595,
            point = parametricGlider,
        )
        assertEquals(
            PI * 0.5,
            assertIs<Double>(parametricGlider.position),
        )

        val plotBoard = board("plot")
        val plot = curve(
            Curve.createData(
                board = plotBoard,
                dataX = doubleArrayOf(-4.0, -1.0, 2.0, 4.0),
                dataY = doubleArrayOf(-2.0, 2.0, -1.0, 2.0),
                id = "plot",
                name = "",
            ),
        )
        val plotGlider = glider(
            Glider.create(
                board = plotBoard,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = plot,
                id = "plotGlider",
                name = "",
            ),
        )
        assertPoint(0.5, 0.5, plotGlider)
        assertEquals(1.5, assertIs<Double>(plotGlider.position))

        plotGlider.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.5, 0.0),
        )
        plotBoard.update(draggedElement = plotGlider)
        assertPoint(
            x = 2.9230769230769234,
            y = 0.3846153846153846,
            point = plotGlider,
        )
        assertEquals(
            2.4615384615384617,
            assertIs<Double>(plotGlider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun transformedCurveGliderMatchesOfficialNestedProjectionAndUpdates() {
        val board = board("transformed")
        val driver = point(board, 1.0, 0.0, "driver")
        val source = curve(
            Curve.createParametric(
                board = board,
                xSource = "x",
                ySource = "x * x",
                minimumSource = "-2",
                maximumSource = "2",
                sampleCount = 5,
                id = "source",
                name = "",
            ),
        )
        val translate = transformation(
            Transformation.create(
                board = board,
                type = "translate",
                parameters = listOf(
                    TransformationParameter.Dynamic(
                        TransformationDynamicParameter {
                            GMResult.Ok(driver.X())
                        },
                    ),
                    TransformationParameter.Numeric(2.0),
                ),
            ),
        )
        val rotate = transformation("rotate", PI * 0.5)
        val first = curve(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(translate),
                id = "first",
                name = "",
            ),
        )
        val nested = curve(
            Curve.createTransformed(
                board = board,
                source = first,
                transformations = listOf(rotate),
                id = "nested",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 4.0),
                slideObject = nested,
                id = "glider",
                name = "",
            ),
        )

        assertPoint(
            x = -2.2892143018611795,
            y = 1.537786483524065,
            point = glider,
        )
        assertEquals(
            0.537786483524065,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )

        glider.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(-3.0, 4.0),
        )
        board.update(draggedElement = glider)
        assertPoint(
            x = -3.663132105131613,
            y = 2.289624792384054,
            point = glider,
        )
        assertEquals(
            1.2896247923840534,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )

        driver.setPositionDirectly(
            method = Const.COORDS_BY_USER,
            coordinates = doubleArrayOf(3.0, 0.0),
        )
        board.update()
        assertPoint(
            x = -3.663132105131613,
            y = 2.289624792384054,
            point = glider,
        )

        board.update()
        assertPoint(
            x = -3.6631324551048388,
            y = 4.289624928072049,
            point = glider,
        )
        assertEquals(
            1.2896247923840534,
            assertIs<Double>(glider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun transformedCurveGliderRejectsNonInvertibleMatrixAtomically() {
        val board = board("transformed-noninvertible")
        val source = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-1.0, 1.0),
                dataY = doubleArrayOf(0.0, 0.0),
                id = "source",
                name = "",
            ),
        )
        val transformed = curve(
            Curve.createTransformed(
                board = board,
                source = source,
                transformations = listOf(
                    transformation("scale", 0.0, 1.0),
                ),
                id = "transformed",
                name = "",
            ),
        )
        val before = board.objects.keys.toList()

        val failure = assertIs<
            GMResult.Err<GliderError.NonInvertibleCurveTransformation>
            >(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 1.0),
                slideObject = transformed,
                id = "glider",
                name = "",
            ),
        )

        assertEquals("transformed", failure.error.curveId)
        assertEquals(0, failure.error.chainIndex)
        assertEquals(before, board.objects.keys.toList())
        assertTrue("glider" !in transformed.childElements)
    }

    @Test
    fun arcAndSectorGlidersMatchOfficialProjectionAndParentUpdates() {
        val arcBoard = board("arc")
        val arcCenter = point(arcBoard, 0.0, 0.0, "arcCenter")
        val arcRadius = point(arcBoard, 2.0, 0.0, "arcRadius")
        val arcAngle = point(arcBoard, -2.0, 0.0, "arcAngle")
        val arc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = arcBoard,
                center = arcCenter,
                radiuspoint = arcRadius,
                anglepoint = arcAngle,
                id = "arc",
                name = "",
            ),
        ).value
        val arcGlider = glider(
            Glider.create(
                board = arcBoard,
                coordinates = doubleArrayOf(1.0, 1.8),
                slideObject = arc,
                id = "arcGlider",
                name = "",
            ),
        )

        assertPoint(
            x = 0.9712858623572641,
            y = 1.7483145522430756,
            point = arcGlider,
        )
        assertEquals(
            0.33858553278290476,
            assertIs<Double>(arcGlider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(arc, arcGlider.slideObject)
        assertSame(arcGlider, arc.childElements["arcGlider"])
        assertEquals(listOf("arc"), arcGlider.parents)

        arcRadius.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(0.0, 3.0),
        )
        arcBoard.update()
        arcBoard.update()
        assertPoint(
            x = -1.521383189632433,
            y = 2.585612730148087,
            point = arcGlider,
        )

        val sectorBoard = board("sector")
        val sectorCenter = point(
            sectorBoard,
            0.0,
            -2.0,
            "sectorCenter",
        )
        val sectorRadius = point(
            sectorBoard,
            2.0,
            -2.0,
            "sectorRadius",
        )
        val sectorAngle = point(
            sectorBoard,
            0.0,
            0.0,
            "sectorAngle",
        )
        val sector = assertIs<GMResult.Ok<Sector>>(
            Sector.create(
                board = sectorBoard,
                center = sectorCenter,
                radiuspoint = sectorRadius,
                anglepoint = sectorAngle,
                id = "sector",
                name = "",
            ),
        ).value
        val sectorGlider = glider(
            Glider.create(
                board = sectorBoard,
                coordinates = doubleArrayOf(1.0, -1.0),
                slideObject = sector,
                id = "sectorGlider",
                name = "",
            ),
        )

        assertPoint(
            x = 1.4142135623730951,
            y = -0.5857864376269051,
            point = sectorGlider,
        )
        assertEquals(
            0.5,
            assertIs<Double>(sectorGlider.position),
            absoluteTolerance = TOLERANCE,
        )
        assertSame(sector, sectorGlider.slideObject)
        assertSame(sectorGlider, sector.childElements["sectorGlider"])
        assertEquals(listOf("sector"), sectorGlider.parents)

        sectorAngle.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -2.0),
        )
        sectorBoard.update()
        sectorBoard.update()
        assertPoint(
            x = 1.2246467991473532e-16,
            y = 0.0,
            point = sectorGlider,
        )
    }

    @Test
    fun arcGliderSelectionClampsAndNormalizesLikeOfficial() {
        val board = board("arc-selection")
        val center = point(board, 0.0, 0.0, "center")
        val radius = point(board, 2.0, 0.0, "radius")
        val angle = point(board, 0.0, 2.0, "angle")
        val minorArc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radius,
                anglepoint = angle,
                selection = Arc.SELECTION_MINOR,
                id = "minorArc",
                name = "",
            ),
        ).value
        val minorGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, 0.0),
                slideObject = minorArc,
                id = "minorGlider",
                name = "",
            ),
        )
        assertPoint(
            x = 1.2246467991473532e-16,
            y = 2.0,
            point = minorGlider,
        )
        assertEquals(1.0, minorGlider.position)

        val majorArc = assertIs<GMResult.Ok<Arc>>(
            Arc.create(
                board = board,
                center = center,
                radiuspoint = radius,
                anglepoint = angle,
                selection = Arc.SELECTION_MAJOR,
                id = "majorArc",
                name = "",
            ),
        ).value
        val majorGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(-2.0, 0.0),
                slideObject = majorArc,
                id = "majorGlider",
                name = "",
            ),
        )
        assertPoint(
            x = -2.0,
            y = 2.4492935982947064e-16,
            point = majorGlider,
        )
        assertEquals(
            2.0 / 3.0,
            assertIs<Double>(majorGlider.position),
            absoluteTolerance = TOLERANCE,
        )
    }

    @Test
    fun curveRemovalAndFailuresRemainAtomicAndStructured() {
        val board = board("lifecycle")
        val curve = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-2.0, 0.0, 2.0),
                dataY = doubleArrayOf(0.0, 2.0, 0.0),
                id = "curve",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 1.0),
                slideObject = curve,
                id = "glider",
                name = "",
            ),
        )
        board.removeObject(curve)
        assertTrue(curve.id !in board.objects)
        assertTrue(glider.id !in board.objects)

        val otherBoard = board("other")
        val foreignCurve = curve(
            Curve.createData(
                board = otherBoard,
                dataX = doubleArrayOf(-1.0, 1.0),
                dataY = doubleArrayOf(0.0, 0.0),
                id = "foreign",
                name = "",
            ),
        )
        val before = board.objects.keys.toList()
        assertIs<GMResult.Err<GliderError.SlideObjectBoardMismatch>>(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = foreignCurve,
                id = "candidate",
            ),
        )
        assertEquals(before, board.objects.keys.toList())

        val focus1 = point(board, -2.0, 0.0, "focus1")
        val focus2 = point(board, 2.0, 0.0, "focus2")
        val ellipse = assertIs<GMResult.Ok<Curve>>(
            Ellipse.create(
                board = board,
                focus1 = focus1,
                focus2 = focus2,
                majorAxis = 6.0,
                id = "ellipse",
                name = "",
                centerId = "center",
            ),
        ).value
        val conicGlider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 4.0),
                slideObject = ellipse,
                id = "conicGlider",
                name = "",
            ),
        )
        assertEquals(
            0.0,
            conicGlider.X(),
            absoluteTolerance = CONIC_TOLERANCE,
        )
        assertEquals(
            2.236067977499285,
            conicGlider.Y(),
            absoluteTolerance = CONIC_TOLERANCE,
        )
        assertEquals(
            0.8410691715470333,
            assertIs<Double>(conicGlider.position),
            absoluteTolerance = CONIC_TOLERANCE,
        )

        point(board, 4.0, 4.0, "collision")
        val duplicateHost = curve(
            Curve.createData(
                board = board,
                dataX = doubleArrayOf(-1.0, 1.0),
                dataY = doubleArrayOf(0.0, 0.0),
                id = "duplicateHost",
                name = "",
            ),
        )
        val beforeDuplicate = board.objects.keys.toList()
        val duplicate = assertIs<
            GMResult.Err<GliderError.Registration>,
            >(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(0.0, 0.0),
                slideObject = duplicateHost,
                id = "collision",
            ),
        ).error
        assertEquals(
            BoardError.DuplicateElementId("collision"),
            duplicate.error,
        )
        assertEquals(beforeDuplicate, board.objects.keys.toList())
    }

    @Test
    fun dynamicCurveFailureIsExposedByTheGliderBoundary() {
        val board = board("dynamic-failure")
        val driver = point(board, -2.0, 2.0, "driver")
        val curve = curve(
            Curve.createFunctionGraph(
                board = board,
                ySource = "x * x",
                minimumSource = "driver.X()",
                maximumSource = "driver.Y()",
                sampleCount = 16,
                id = "curve",
                name = "",
            ),
        )
        val glider = glider(
            Glider.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                slideObject = curve,
                id = "glider",
                name = "",
            ),
        )

        driver.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(3.0, -3.0),
        )
        board.update()

        val gliderError = assertIs<GliderError.CurveEvaluation>(
            glider.evaluationError,
        )
        assertIs<CurveError.InvalidDomain>(gliderError.error)
    }

    private fun board(id: String): Board = Board(
        originX = 320.0,
        originY = 240.0,
        unitX = 40.0,
        unitY = 40.0,
        id = id,
    )

    private fun point(
        board: Board,
        x: Double,
        y: Double,
        id: String,
    ): Point =
        assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(x, y),
                id = id,
                name = id,
            ),
        ).value

    private fun curve(
        result: GMResult<Curve, CurveError>,
    ): Curve = assertIs<GMResult.Ok<Curve>>(result).value

    private fun glider(
        result: GMResult<Glider, GliderError>,
    ): Glider = assertIs<GMResult.Ok<Glider>>(result).value

    private fun transformation(
        type: String,
        vararg parameters: Double,
    ): Transformation = transformation(
        Transformation.create(type, parameters),
    )

    private fun transformation(
        result: GMResult<Transformation, TransformationError>,
    ): Transformation =
        assertIs<GMResult.Ok<Transformation>>(result).value

    private fun assertPoint(
        x: Double,
        y: Double,
        point: Point,
    ) {
        assertEquals(x, point.X(), absoluteTolerance = TOLERANCE)
        assertEquals(y, point.Y(), absoluteTolerance = TOLERANCE)
    }

    private companion object {
        const val TOLERANCE = 1.0e-6
        const val CONIC_TOLERANCE = 3.0e-6
    }
}
