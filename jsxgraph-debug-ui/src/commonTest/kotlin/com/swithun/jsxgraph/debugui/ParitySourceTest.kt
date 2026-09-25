package com.swithun.jsxgraph.debugui

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.JsxGraphArrowHead
import com.swithun.jsxgraph.core.JsxGraphBoundingBox
import com.swithun.jsxgraph.core.JsxGraphBoxPlot
import com.swithun.jsxgraph.core.JsxGraphColor
import com.swithun.jsxgraph.core.JsxGraphInteractionState
import com.swithun.jsxgraph.core.JsxGraphPoint2D
import com.swithun.jsxgraph.core.JsxGraphScene
import com.swithun.jsxgraph.core.JsxGraphSceneElement
import com.swithun.jsxgraph.debugui.generated.productionCorpusCases
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ParitySourceTest {
    @Test
    fun productionCorpusCasesResolveThroughTheWebAuditRegistry() {
        assertEquals(30, productionCorpusCases.size)
        productionCorpusCases.forEach { productionCase ->
            val resolved = assertIs<GMResult.Ok<JsxGraphParityCase>>(
                JsxGraphParityCorpus.find(productionCase.id),
            ).value
            assertEquals(productionCase.source, resolved.source)
            assertIs<GMResult.Ok<JsxGraphScene>>(
                parseParitySource(resolved.source),
                productionCase.id,
            )
        }
    }

    @Test
    fun parityCorpusHasUniqueResolvableCases() {
        val cases = JsxGraphParityCorpus.cases
        assertEquals(91, cases.size)
        assertEquals(
            JsxGraphParityCorpus.DEFAULT_CASE_ID,
            cases.first().id,
        )
        assertEquals(
            30,
            cases.count { parityCase ->
                parityCase.suite == JsxGraphParitySuite.Production
            },
        )
        assertEquals(
            61,
            cases.count { parityCase ->
                parityCase.suite == JsxGraphParitySuite.Focused
            },
        )
        assertEquals(cases.size, cases.map { parityCase -> parityCase.id }.distinct().size)
        assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find(JsxGraphParityCorpus.DEFAULT_CASE_ID),
        )
        assertIs<GMResult.Err<String>>(
            JsxGraphParityCorpus.find("missing_case"),
        )
    }

    @Test
    fun stepFunctionsFocusedCasePreservesExpandedPathsAndBreaks() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("step_functions"),
        ).value
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(parityCase.source),
        ).value

        assertEquals(
            listOf(
                JsxGraphPoint2D(-5.0, -3.0),
                JsxGraphPoint2D(-3.0, -3.0),
                JsxGraphPoint2D(-3.0, -1.0),
                JsxGraphPoint2D(-1.0, -1.0),
                JsxGraphPoint2D(-1.0, 2.0),
                JsxGraphPoint2D(1.0, 2.0),
                JsxGraphPoint2D(1.0, 0.0),
                JsxGraphPoint2D(3.0, 0.0),
                JsxGraphPoint2D(3.0, 3.0),
                JsxGraphPoint2D(5.0, 3.0),
                JsxGraphPoint2D(5.0, 1.0),
            ),
            curve(scene, "risingSteps").points,
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-5.0, 4.5),
                JsxGraphPoint2D(-3.5, 4.5),
                JsxGraphPoint2D(-3.5, 3.5),
                JsxGraphPoint2D(-2.0, 3.5),
                null,
                null,
                null,
            ),
            curve(scene, "mismatchedSteps").points,
        )
    }

    @Test
    fun polygonalChainFocusedCaseStaysOpenAfterMovingItsLastPoint() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("polygonal_chains"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val chain = assertIs<JsxGraphSceneElement.Polygon>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "chain"
            },
        )

        assertFalse(chain.isClosed)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-5.0, -3.0),
                JsxGraphPoint2D(-3.0, 3.0),
                JsxGraphPoint2D(0.0, -1.0),
                JsxGraphPoint2D(4.0, 3.0),
            ),
            chain.vertices,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "D",
                coordinates = JsxGraphPoint2D(5.0, -2.0),
            ),
        ).value
        val movedChain = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { element -> element.id == "chain" },
        )
        assertFalse(movedChain.isClosed)
        assertEquals(
            JsxGraphPoint2D(5.0, -2.0),
            movedChain.vertices.last(),
        )
    }

    @Test
    fun parallelogramFocusedCaseTracksItsExposedParallelPoint() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("parallelograms"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val parallelogram = assertIs<JsxGraphSceneElement.Polygon>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "P"
            },
        )
        val helper = parallelogram.implicitVertices.single()

        assertTrue(parallelogram.isClosed)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-4.0, -2.0),
                JsxGraphPoint2D(-1.0, 2.0),
                JsxGraphPoint2D(5.0, 1.0),
                JsxGraphPoint2D(2.0, -3.0),
            ),
            parallelogram.vertices,
        )
        assertEquals("helper", helper.id)
        assertEquals(JsxGraphPoint2D(5.0, 1.0), helper.coordinates)
        assertEquals(7.0, helper.size)
        assertEquals(JsxGraphColor(123, 78, 163), helper.style.fillColor)
        assertTrue(helper.draggable)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(0.0, 2.0),
            ),
        ).value
        val movedParallelogram = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { element -> element.id == "P" },
        )
        assertEquals(
            JsxGraphPoint2D(3.0, 6.0),
            movedParallelogram.vertices[2],
        )
        assertEquals(
            movedParallelogram.vertices[2],
            movedParallelogram.implicitVertices.single().coordinates,
        )
    }

    @Test
    fun regularPolygonFocusedCaseTracksItsRotationChain() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("regular_polygons"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val regular = assertIs<JsxGraphSceneElement.Polygon>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "regular"
            },
        )

        assertTrue(regular.isClosed)
        assertEquals(5, regular.vertices.size)
        assertEquals(
            listOf("C", "D", "E"),
            regular.implicitVertices.map { point -> point.id },
        )
        assertEquals(
            0.9270509831248427,
            regular.vertices[2].x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.8531695488854605,
            regular.vertices[2].y,
            absoluteTolerance = 1.0e-12,
        )
        assertTrue(
            regular.implicitVertices.all { point ->
                point.draggable &&
                    point.style.fillColor ==
                    JsxGraphColor(123, 78, 163)
            },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(1.0, 0.0),
            ),
        ).value
        val movedRegular = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { element ->
                element.id == "regular"
            },
        )
        assertEquals(
            0.3339549449094832,
            movedRegular.vertices[2].x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            4.4222600539305095,
            movedRegular.vertices[2].y,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun radicalAxisFocusedCaseTracksItsCirclePowerDifference() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("radical_axis"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initial = line(jessieCodeSession.scene, "axis")

        assertEquals(7, jessieCodeSession.scene.elements.size)
        assertTrue(initial.straightFirst)
        assertTrue(initial.straightLast)
        assertEquals(JsxGraphColor(123, 78, 163), initial.style.strokeColor)
        assertPointEquals(
            JsxGraphPoint2D(
                1.6029411764705883,
                -3.8382352941176476,
            ),
            initial.point1,
        )
        assertPointEquals(
            JsxGraphPoint2D(
                0.5441176470588236,
                -2.073529411764706,
            ),
            initial.point2,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "radius1",
                coordinates = JsxGraphPoint2D(-1.0, 1.0),
            ),
        ).value
        val movedAxis = line(moved, "axis")
        assertNotEquals(initial.point1, movedAxis.point1)
        assertNotEquals(initial.point2, movedAxis.point2)
    }

    @Test
    fun polePointFocusedCaseTracksItsCircleAndLineParents() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("pole_point"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initial = point(jessieCodeSession.scene, "pole")

        assertEquals(
            listOf(
                "center",
                "radiusPoint",
                "sourceCircle",
                "linePoint1",
                "linePoint2",
                "sourceLine",
                "pole",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertPointEquals(
            JsxGraphPoint2D(5.0, 5.0),
            initial.coordinates,
        )
        assertEquals(JsxGraphColor(123, 78, 163), initial.style.fillColor)
        assertFalse(initial.draggable)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "center",
                coordinates = JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "radiusPoint",
                coordinates = JsxGraphPoint2D(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "linePoint1",
                coordinates = JsxGraphPoint2D(-3.0, -1.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "linePoint2",
                coordinates = JsxGraphPoint2D(2.0, 4.0),
            ),
        ).value
        assertPointEquals(
            JsxGraphPoint2D(2.5, -2.5),
            point(moved, "pole").coordinates,
        )
    }

    @Test
    fun tangentPolarFocusedCaseTracksItsSharedCircle() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("tangent_polar_circle"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialLines = listOf(
            line(jessieCodeSession.scene, "tangentLine"),
            line(jessieCodeSession.scene, "polarAlias"),
            line(jessieCodeSession.scene, "polarLine"),
        )

        assertEquals(
            listOf(
                "center",
                "radiusPoint",
                "sourceCircle",
                "onCirclePoint",
                "offCirclePoint",
                "reversePoint",
                "tangentLine",
                "polarAlias",
                "polarLine",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            listOf(
                JsxGraphColor(0, 114, 178),
                JsxGraphColor(213, 94, 0),
                JsxGraphColor(0, 158, 115),
            ),
            initialLines.map { line -> line.style.strokeColor },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "radiusPoint",
                coordinates = JsxGraphPoint2D(1.0, 0.0),
            ),
        ).value
        val movedLines = listOf(
            line(moved, "tangentLine"),
            line(moved, "polarAlias"),
            line(moved, "polarLine"),
        )
        for (index in initialLines.indices) {
            assertNotEquals(initialLines[index].point1, movedLines[index].point1)
            assertNotEquals(initialLines[index].point2, movedLines[index].point2)
        }
    }

    @Test
    fun tangentToFocusedCaseTracksBothBranchesAndExposedHelpers() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("tangent_to_circle"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initial = jessieCodeSession.scene

        assertEquals(
            listOf(
                "center",
                "radiusPoint",
                "sourceCircle",
                "sourcePoint",
                "firstPolar",
                "firstIntersection",
                "firstTangent",
                "secondPolar",
                "secondIntersection",
                "secondTangent",
            ),
            initial.elements.map(JsxGraphSceneElement::id),
        )
        assertEquals(
            JsxGraphColor(0, 114, 178),
            line(initial, "firstTangent").style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            line(initial, "secondTangent").style.strokeColor,
        )
        assertEquals(
            listOf(10.0, 10.0),
            line(initial, "firstPolar").style.strokeDashPattern,
        )
        assertTrue(line(initial, "firstPolar").style.visible)
        assertFalse(line(initial, "secondPolar").style.visible)
        assertPointEquals(
            JsxGraphPoint2D(1.0000000000000073, 4.0),
            point(initial, "firstIntersection").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(3.88, 0.16000000000000036),
            point(initial, "secondIntersection").coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "sourcePoint",
                coordinates = JsxGraphPoint2D(3.0, -3.0),
            ),
        ).value
        assertNotEquals(
            line(initial, "firstTangent").point1,
            line(moved, "firstTangent").point1,
        )
        assertNotEquals(
            line(initial, "secondTangent").point1,
            line(moved, "secondTangent").point1,
        )
        assertPointEquals(
            JsxGraphPoint2D(3.88997487421324, 0.19498743710662),
            point(moved, "firstIntersection").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-0.08997487421324, -1.79498743710662),
            point(moved, "secondIntersection").coordinates,
        )
    }

    @Test
    fun tangentLineFocusedCaseSharesAndTracksSourceEndpoints() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("tangent_line"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialSource = line(jessieCodeSession.scene, "sourceLine")
        val initialTangents = listOf(
            line(jessieCodeSession.scene, "tangentForward"),
            line(jessieCodeSession.scene, "polarReverse"),
            line(jessieCodeSession.scene, "tangentSegment"),
        )

        assertEquals(
            listOf(
                "A",
                "B",
                "sourceLine",
                "P",
                "tangentForward",
                "polarReverse",
                "tangentSegment",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            listOf(
                JsxGraphColor(0, 114, 178),
                JsxGraphColor(213, 94, 0),
                JsxGraphColor(0, 158, 115),
            ),
            initialTangents.map { line -> line.style.strokeColor },
        )
        assertEquals(
            listOf(
                false to true,
                true to false,
                false to false,
            ),
            initialTangents.map { line ->
                line.straightFirst to line.straightLast
            },
        )
        for (line in initialTangents) {
            assertPointEquals(initialSource.point1, line.point1)
            assertPointEquals(initialSource.point2, line.point2)
        }

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(2.0, -3.0),
            ),
        ).value
        val movedSource = line(moved, "sourceLine")
        val movedTangents = listOf(
            line(moved, "tangentForward"),
            line(moved, "polarReverse"),
            line(moved, "tangentSegment"),
        )
        assertNotEquals(initialSource.point2, movedSource.point2)
        for (line in movedTangents) {
            assertPointEquals(movedSource.point1, line.point1)
            assertPointEquals(movedSource.point2, line.point2)
        }
    }

    @Test
    fun tangentCurveFocusedCaseTracksNearestCurveGeometry() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("tangent_curve"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialTangents = listOf(
            line(jessieCodeSession.scene, "functionTangent"),
            line(jessieCodeSession.scene, "parametricPolar"),
            line(jessieCodeSession.scene, "plotTangent"),
        )

        assertEquals(
            listOf(
                "functionCurve",
                "functionPoint",
                "functionTangent",
                "parametricCurve",
                "parametricPoint",
                "parametricPolar",
                "plotCurve",
                "plotPoint",
                "plotTangent",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            listOf(
                JsxGraphColor(0, 114, 178),
                JsxGraphColor(213, 94, 0),
                JsxGraphColor(123, 78, 163),
            ),
            initialTangents.map { line -> line.style.strokeColor },
        )
        assertEquals(
            listOf(
                true to true,
                false to true,
                false to false,
            ),
            initialTangents.map { line ->
                line.straightFirst to line.straightLast
            },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "plotPoint",
                coordinates = JsxGraphPoint2D(4.5, 5.0),
            ),
        ).value
        assertEquals(
            initialTangents[0],
            line(moved, "functionTangent"),
        )
        assertEquals(
            initialTangents[1],
            line(moved, "parametricPolar"),
        )
        assertNotEquals(
            initialTangents[2].point1,
            line(moved, "plotTangent").point1,
        )
        assertNotEquals(
            initialTangents[2].point2,
            line(moved, "plotTangent").point2,
        )
    }

    @Test
    fun normalFocusedCaseCoversEveryTranslatedParentBranch() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("normal_constructions"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val normalIds = listOf(
            "lineNormal",
            "circleNormal",
            "functionNormal",
            "parameterNormal",
            "plotNormal",
        )
        val initial = normalIds.map { id ->
            line(jessieCodeSession.scene, id)
        }

        assertEquals(
            listOf(
                JsxGraphColor(0, 114, 178),
                JsxGraphColor(213, 94, 0),
                JsxGraphColor(0, 158, 115),
                JsxGraphColor(123, 78, 163),
                JsxGraphColor(198, 93, 33),
            ),
            initial.map { line -> line.style.strokeColor },
        )
        assertEquals(
            false to true,
            initial.last().straightFirst to initial.last().straightLast,
        )

        val moves = listOf(
            "B" to JsxGraphPoint2D(-5.0, 1.0),
            "center" to JsxGraphPoint2D(-1.5, 3.5),
            "functionPoint" to JsxGraphPoint2D(6.0, -2.0),
            "parameterPoint" to JsxGraphPoint2D(-4.0, -5.0),
            "plotPoint" to JsxGraphPoint2D(2.5, -1.0),
        )
        var moved = jessieCodeSession.scene
        for ((id, coordinates) in moves) {
            moved = assertIs<GMResult.Ok<JsxGraphScene>>(
                jessieCodeSession.session.movePoint(id, coordinates),
            ).value
        }
        val movedNormals = normalIds.map { id -> line(moved, id) }
        for (index in initial.indices) {
            assertTrue(
                initial[index].point1 != movedNormals[index].point1 ||
                    initial[index].point2 != movedNormals[index].point2,
            )
        }
    }

    @Test
    fun derivativeCurveFocusedCaseTracksItsCoefficientPoint() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("derivative_curve"),
        ).value
        val parityResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            parityResult,
            parityResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialSource = curve(jessieCodeSession.scene, "sourceCurve")
        val initialDerivative =
            curve(jessieCodeSession.scene, "derivativeCurve")

        assertEquals(
            listOf("A", "sourceCurve", "derivativeCurve"),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(128, initialSource.points.size)
        assertEquals(128, initialDerivative.points.size)
        assertEquals(
            JsxGraphColor(213, 94, 0),
            initialDerivative.style.strokeColor,
        )
        assertPointEquals(
            JsxGraphPoint2D(-7.0, -2.5),
            initialDerivative.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-2.0, 0.0),
            initialDerivative.points[64],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(0.6, 5.0),
            ),
        ).value
        val movedSource = curve(moved, "sourceCurve")
        val movedDerivative = curve(moved, "derivativeCurve")
        assertNotEquals(initialSource.points.first(), movedSource.points.first())
        assertNotEquals(
            initialDerivative.points.first(),
            movedDerivative.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-7.0, -6.0),
            movedDerivative.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-2.0, 0.0),
            movedDerivative.points[64],
        )
    }

    @Test
    fun splineCurvesFocusedCaseTracksPointsAndTension() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("spline_curves"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialNatural =
            curve(jessieCodeSession.scene, "naturalSpline")
        val initialCardinal =
            curve(jessieCodeSession.scene, "cardinalSpline")

        assertEquals(128, initialNatural.points.size)
        assertEquals(128, initialCardinal.points.size)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            initialNatural.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(0, 158, 115),
            initialCardinal.style.strokeColor,
        )

        val movedNaturalScene = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(-6.0, 5.0),
            ),
        ).value
        assertNotEquals(
            initialNatural.points,
            curve(movedNaturalScene, "naturalSpline").points,
        )

        val movedCardinalScene = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "tension",
                coordinates = JsxGraphPoint2D(0.8, -5.5),
            ),
        ).value
        assertNotEquals(
            initialCardinal.points,
            curve(movedCardinalScene, "cardinalSpline").points,
        )
    }

    @Test
    fun riemannSumsFocusedCaseTracksBarCountAndFill() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("riemann_sums"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val single = curve(jessieCodeSession.scene, "single")
        val between = curve(jessieCodeSession.scene, "between")

        assertEquals(25, single.points.size)
        assertEquals(JsxGraphColor(240, 228, 66), single.style.fillColor)
        assertEquals(0.45, single.style.fillOpacity)
        assertEquals(20, between.points.size)
        assertEquals(JsxGraphColor(86, 180, 233), between.style.fillColor)
        assertEquals(0.35, between.style.fillOpacity)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "bars",
                coordinates = JsxGraphPoint2D(6.0, -6.0),
            ),
        ).value
        assertEquals(30, curve(moved, "between").points.size)
        assertNotEquals(
            between.points,
            curve(moved, "between").points,
        )
    }

    @Test
    fun boxPlotsFocusedCaseTracksDynamicGeometryAndOutliers() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("box_plots"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val staticVertical =
            curve(jessieCodeSession.scene, "staticVertical")
        val staticHorizontal =
            curve(jessieCodeSession.scene, "staticHorizontal")
        val dynamic = curve(jessieCodeSession.scene, "dynamicBox")

        assertEquals(58, staticVertical.points.size)
        assertEquals(32, staticHorizontal.points.size)
        assertEquals(32, dynamic.points.size)
        assertEquals(
            JsxGraphColor(86, 180, 233),
            staticVertical.style.fillColor,
        )
        assertEquals(
            "horizontal",
            assertIs<JsxGraphBoxPlot>(staticHorizontal.boxPlot).direction,
        )
        val initialDynamic = assertIs<JsxGraphBoxPlot>(dynamic.boxPlot)
        assertEquals(listOf(-4.0, -2.0, 0.0, 2.0, 4.0), initialDynamic.quantiles)
        assertEquals(5.0, initialDynamic.axis)
        assertEquals(2.5, initialDynamic.width)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(6.5, -5.5),
            ),
        ).value
        val movedDynamic = assertIs<JsxGraphBoxPlot>(
            curve(moved, "dynamicBox").boxPlot,
        )
        assertEquals(
            listOf(-2.5, -0.5, 1.5, 3.5, 5.5),
            movedDynamic.quantiles,
        )
        assertEquals(6.5, movedDynamic.axis)
        assertEquals(3.25, movedDynamic.width)
    }

    @Test
    fun combsFocusedCaseTracksFunctionAttributesAndReverse() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("combs"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val staticComb = curve(jessieCodeSession.scene, "staticComb")
        val reverseComb = curve(jessieCodeSession.scene, "reverseComb")
        val dynamicComb = curve(jessieCodeSession.scene, "dynamicComb")

        assertEquals(24, staticComb.points.size)
        assertEquals(JsxGraphColor(0, 0, 255), staticComb.style.strokeColor)
        assertEquals(21, reverseComb.points.size)
        assertEquals(JsxGraphColor(213, 94, 0), reverseComb.style.strokeColor)
        assertEquals(42, dynamicComb.points.size)
        assertEquals(
            JsxGraphPoint2D(-7.0, -2.5),
            dynamicComb.points.first(),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(6.5, -5.8),
            ),
        ).value
        val movedDynamic = curve(moved, "dynamicComb")
        assertEquals(36, movedDynamic.points.size)
        assertEquals(
            JsxGraphPoint2D(7.0, -2.5),
            movedDynamic.points.first(),
        )
        assertNotEquals(dynamicComb.points, movedDynamic.points)
    }

    @Test
    fun inequalitiesFocusedCaseTracksLineFunctionAndInverseUpdates() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("inequalities"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initialLine = curve(jessieCodeSession.scene, "lineRegion")
        val initialFunction =
            curve(jessieCodeSession.scene, "functionRegion")

        assertEquals(5, initialLine.points.size)
        assertEquals(138, initialFunction.points.size)
        assertTrue(initialFunction.points.any { point -> point == null })
        assertEquals(
            JsxGraphColor(86, 180, 233),
            initialLine.style.fillColor,
        )
        assertEquals(
            JsxGraphColor(230, 159, 0),
            initialFunction.style.fillColor,
        )

        val lineMoved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "lineB",
                coordinates = JsxGraphPoint2D(-2.0, 1.0),
            ),
        ).value
        assertNotEquals(
            initialLine.points,
            curve(lineMoved, "lineRegion").points,
        )

        val functionMoved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "functionDriver",
                coordinates = JsxGraphPoint2D(2.0, -5.8),
            ),
        ).value
        val movedFunction = curve(functionMoved, "functionRegion")
        assertEquals(138, movedFunction.points.size)
        assertTrue(movedFunction.points.any { point -> point == null })
        assertNotEquals(initialFunction.points, movedFunction.points)
    }

    @Test
    fun vectorFieldsFocusedCaseTracksMeshScaleAndArrowUpdates() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("vector_fields"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val staticField = curve(jessieCodeSession.scene, "staticField")
        val dynamicField = curve(jessieCodeSession.scene, "dynamicField")

        assertEquals(75, staticField.points.size)
        assertEquals(JsxGraphColor(0, 158, 115), staticField.style.strokeColor)
        assertEquals(171, dynamicField.points.size)
        assertEquals(JsxGraphColor(213, 94, 0), dynamicField.style.strokeColor)
        assertPointEquals(
            JsxGraphPoint2D(-0.08000000000000007, -3.64),
            assertIs(dynamicField.points[1]),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(6.0, -5.8),
            ),
        ).value
        val movedField = curve(moved, "dynamicField")
        assertEquals(135, movedField.points.size)
        assertPointEquals(
            JsxGraphPoint2D(-1.8080000000000003, -3.532),
            assertIs(movedField.points[1]),
        )
        assertNotEquals(dynamicField.points, movedField.points)
    }

    @Test
    fun slopeFieldsFocusedCaseTracksNormalizedSlopeAndArrowUpdates() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("slope_fields"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val staticField = curve(jessieCodeSession.scene, "staticField")
        val dynamicField = curve(jessieCodeSession.scene, "dynamicField")

        assertEquals(75, staticField.points.size)
        assertEquals(JsxGraphColor(0, 158, 115), staticField.style.strokeColor)
        assertEquals(175, dynamicField.points.size)
        assertEquals(JsxGraphColor(213, 94, 0), dynamicField.style.strokeColor)
        assertPointEquals(
            JsxGraphPoint2D(
                1.505362840396907,
                -4.3234322178540205,
            ),
            assertIs(dynamicField.points[1]),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(6.0, -5.8),
            ),
        ).value
        val movedField = curve(moved, "dynamicField")
        assertEquals(135, movedField.points.size)
        assertPointEquals(
            JsxGraphPoint2D(
                1.3249708261509086,
                -4.6759393183938895,
            ),
            assertIs(movedField.points[1]),
        )
        assertNotEquals(dynamicField.points, movedField.points)
    }

    @Test
    fun ellipsesFocusedCaseTracksPointAndNumericForms() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("ellipses"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val pointEllipse = curve(
            jessieCodeSession.scene,
            "pointEllipse",
        )
        val numericArc = curve(
            jessieCodeSession.scene,
            "numericArc",
        )

        assertEquals(192, pointEllipse.points.size)
        assertEquals(128, numericArc.points.size)
        assertEquals(
            JsxGraphColor(22, 135, 122),
            pointEllipse.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            numericArc.style.strokeColor,
        )
        assertPointEquals(
            JsxGraphPoint2D(-1.3944487245360109, 1.0),
            pointEllipse.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(7.0, -1.0),
            numericArc.points.first(),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(-5.0, 5.0),
            ),
        ).value
        val movedEllipse = curve(moved, "pointEllipse")
        assertPointEquals(
            JsxGraphPoint2D(-0.5278640450004204, 1.0),
            movedEllipse.points.first(),
        )
        assertNotEquals(pointEllipse.points, movedEllipse.points)
    }

    @Test
    fun hyperbolasFocusedCaseTracksPointAndNumericForms() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("hyperbolas"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val pointLeft = curve(
            jessieCodeSession.scene,
            "pointLeft",
        )
        val pointRight = curve(
            jessieCodeSession.scene,
            "pointRight",
        )
        val numericBranch = curve(
            jessieCodeSession.scene,
            "numericBranch",
        )

        assertEquals(128, pointLeft.points.size)
        assertEquals(128, pointRight.points.size)
        assertEquals(128, numericBranch.points.size)
        assertEquals(
            JsxGraphColor(22, 135, 122),
            pointLeft.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(22, 135, 122),
            pointRight.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            numericBranch.style.strokeColor,
        )
        assertPointEquals(
            JsxGraphPoint2D(
                -9.036722429581532,
                -0.42427317722847246,
            ),
            pointLeft.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(
                -0.08932339488075414,
                -0.9492333854692252,
            ),
            pointRight.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(
                2.586895294597027,
                -3.5095836840160466,
            ),
            numericBranch.points.first(),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(1.0, 4.0),
            ),
        ).value
        val movedLeft = curve(moved, "pointLeft")
        val movedRight = curve(moved, "pointRight")
        assertPointEquals(
            JsxGraphPoint2D(
                -9.459662542069646,
                -1.0053180553970305,
            ),
            movedLeft.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(
                -0.905561293058736,
                -0.7481079645938296,
            ),
            movedRight.points.first(),
        )
        assertNotEquals(pointLeft.points, movedLeft.points)
        assertNotEquals(pointRight.points, movedRight.points)
    }

    @Test
    fun parabolasFocusedCaseTracksPointLineAndCoordinateForms() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("parabolas"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val pointParabola = curve(
            jessieCodeSession.scene,
            "pointParabola",
        )
        val coordinateParabola = curve(
            jessieCodeSession.scene,
            "coordinateParabola",
        )

        assertEquals(192, pointParabola.points.size)
        assertEquals(128, coordinateParabola.points.size)
        assertEquals(
            JsxGraphColor(22, 135, 122),
            pointParabola.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            coordinateParabola.style.strokeColor,
        )
        assertPointEquals(
            JsxGraphPoint2D(
                -5.447236382643819,
                0.4373440623817374,
            ),
            assertIs(pointParabola.points.first()),
        )
        assertPointEquals(
            JsxGraphPoint2D(
                5.447236382643819,
                -0.4373440623817374,
            ),
            assertIs(coordinateParabola.points.first()),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "focus",
                coordinates = JsxGraphPoint2D(-3.0, 2.0),
            ),
        ).value
        val movedParabola = curve(moved, "pointParabola")
        assertPointEquals(
            JsxGraphPoint2D(
                -4.929648510191759,
                1.2497920831756502,
            ),
            assertIs(movedParabola.points.first()),
        )
        assertNotEquals(pointParabola.points, movedParabola.points)
        assertEquals(
            coordinateParabola.points,
            curve(moved, "coordinateParabola").points,
        )
    }

    @Test
    fun pathIntersectionFocusedCaseResolvesAllTranslatedBranches() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("intersection_paths"),
        ).value
        val sessionResult = createParitySession(parityCase.source)
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            sessionResult,
            sessionResult.toString(),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val scene = jessieCodeSession.scene

        assertPointEquals(
            JsxGraphPoint2D(-4.5, 2.0),
            point(scene, "first").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-3.5, 2.0),
            point(scene, "second").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-1.1716229673654848, -1.0),
            point(scene, "arcHit").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(6.828377032634515, -1.0),
            point(scene, "sectorHit").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(2.0, -4.0),
            point(scene, "polygonHit0").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-2.0, -4.0),
            point(scene, "polygonHit1").coordinates,
        )
    }

    @Test
    fun polygonPathIntersectionCaseTracksOrderingAndMovedVertex() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("polygon_path_intersections"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val scene = jessieCodeSession.scene

        assertPointEquals(
            JsxGraphPoint2D(-1.4998405909909862, -2.0),
            point(scene, "pc0").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(2.0, -1.499885402306805),
            point(scene, "pc2").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(1.499989160969136, 2.0),
            point(scene, "pc4").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-2.0, 1.4998756368270512),
            point(scene, "pc6").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(2.0, 1.4998854023067922),
            point(scene, "reverse").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-1.0, -2.0),
            point(scene, "pp0").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(2.0, -1.0),
            point(scene, "pp2").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(1.0, 2.0),
            point(scene, "pp4").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(-2.0, 1.0),
            point(scene, "pp6").coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(3.0, 2.0),
            ),
        ).value
        assertPointEquals(
            JsxGraphPoint2D(
                2.2058845193854877,
                -1.176461922458049,
            ),
            point(moved, "pc2").coordinates,
        )
    }

    @Test
    fun curveBooleanClippingCaseTracksAllOperationsAndMovedVertex() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("curve_boolean_clipping"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val initial = jessieCodeSession.scene

        assertEquals(5, curve(initial, "iOutput").points.size)
        assertEquals(9, curve(initial, "uOutput").points.size)
        assertEquals(7, curve(initial, "dOutput").points.size)
        assertEquals(
            JsxGraphPoint2D(-1.0, -2.0),
            assertIs(curve(initial, "uOutput").points.first()),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "uC",
                coordinates = JsxGraphPoint2D(0.0, 3.0),
            ),
        ).value
        assertNotEquals(
            curve(initial, "uOutput").points,
            curve(moved, "uOutput").points,
        )
        assertEquals(
            curve(initial, "iOutput").points,
            curve(moved, "iOutput").points,
        )
        assertEquals(
            curve(initial, "dOutput").points,
            curve(moved, "dOutput").points,
        )
    }

    @Test
    fun directionPointArcCaseProducesTheSelectedLongPath() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("arc_direction_point"),
        ).value
        val sceneResult = parseParitySource(parityCase.source)
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            sceneResult,
            sceneResult.toString(),
        ).value
        val arc = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.last(),
        )
        val first = assertIs<JsxGraphPoint2D>(arc.points.first())
        val last = assertIs<JsxGraphPoint2D>(arc.points.last())

        assertEquals(13, arc.points.size)
        assertEquals(0.0, first.x, 1.0e-12)
        assertEquals(2.6, first.y, 1.0e-12)
        assertEquals(2.6, last.x, 1.0e-12)
        assertEquals(0.0, last.y, 1.0e-12)
    }

    @Test
    fun arcCompositionFocusedCaseTracksAllFourCurvesFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("arc_compositions"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val documentSession =
            assertIs<JsxGraphParitySession.ConstructionDocument>(paritySession)
        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "semicircleArc",
                "circumcircleRoute",
                "minorRoute",
                "majorRoute",
            ),
            documentSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            listOf(9, 9, 9, 8, 8, 8, 8),
            documentSession.scene.elements.map { element ->
                element.style.layer
            },
        )

        val initialSemicircle = curve(
            scene = documentSession.scene,
            id = "semicircleArc",
        )
        val initialCircumcircle = curve(
            scene = documentSession.scene,
            id = "circumcircleRoute",
        )
        val initialMinor = curve(
            scene = documentSession.scene,
            id = "minorRoute",
        )
        val initialMajor = curve(
            scene = documentSession.scene,
            id = "majorRoute",
        )
        for (arc in listOf(
            initialSemicircle,
            initialCircumcircle,
            initialMinor,
            initialMajor,
        )) {
            assertEquals(13, arc.points.size)
        }
        assertPointEquals(
            JsxGraphPoint2D(1.0, 4.0),
            initialSemicircle.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -1.0),
            initialSemicircle.points.last(),
        )
        assertPointEquals(
            JsxGraphPoint2D(5.0, -2.0),
            initialCircumcircle.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -1.0),
            initialCircumcircle.points.last(),
        )
        assertNotEquals(initialMinor.points[6], initialMajor.points[6])

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            documentSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(0.0, -4.0),
            ),
        ).value
        val movedSemicircle = curve(moved, "semicircleArc")
        val movedCircumcircle = curve(moved, "circumcircleRoute")
        assertPointEquals(
            JsxGraphPoint2D(0.0, -4.0),
            movedSemicircle.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -1.0),
            movedSemicircle.points.last(),
        )
        assertPointEquals(
            JsxGraphPoint2D(5.0, -2.0),
            movedCircumcircle.points.first(),
        )
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -1.0),
            movedCircumcircle.points.last(),
        )
        assertNotEquals(
            initialSemicircle.points[6],
            movedSemicircle.points[6],
        )
        assertNotEquals(
            initialCircumcircle.points[6],
            movedCircumcircle.points[6],
        )
    }

    @Test
    fun circumcircleCreatorFocusedCaseTracksBothCentersAndCircleFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("circumcircle_creators"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf("A", "B", "C", "center", "alias", "output"),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )

        val initialCenter = point(jessieCodeSession.scene, "center")
        val initialAlias = point(jessieCodeSession.scene, "alias")
        val initialCircle = circle(jessieCodeSession.scene, "output")
        assertPointEquals(
            JsxGraphPoint2D(0.25, -0.5),
            initialCenter.coordinates,
        )
        assertEquals(initialCenter.coordinates, initialAlias.coordinates)
        assertEquals(initialCenter.coordinates, initialCircle.center)
        assertEquals(
            4.506939094329987,
            initialCircle.radius,
            absoluteTolerance = 1.0e-12,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(1.0, 2.0),
            ),
        ).value
        val movedCenter = point(moved, "center")
        val movedAlias = point(moved, "alias")
        val movedCircle = circle(moved, "output")
        assertPointEquals(
            JsxGraphPoint2D(
                0.06756756756756757,
                -1.9594594594594594,
            ),
            movedCenter.coordinates,
        )
        assertEquals(movedCenter.coordinates, movedAlias.coordinates)
        assertEquals(movedCenter.coordinates, movedCircle.center)
        assertEquals(
            4.067769591822385,
            movedCircle.radius,
            absoluteTolerance = 1.0e-12,
        )
        assertNotEquals(initialCenter.coordinates, movedCenter.coordinates)
    }

    @Test
    fun pointReflectionFocusedCaseTracksAllThreeCreatorsFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("point_reflections"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf(
                "source",
                "mirror",
                "axisFirst",
                "axisSecond",
                "axis",
                "reflected",
                "mirrored",
                "alias",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertPointEquals(
            JsxGraphPoint2D(3.0, 1.0),
            point(jessieCodeSession.scene, "reflected").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(3.0, -3.0),
            point(jessieCodeSession.scene, "mirrored").coordinates,
        )
        assertEquals(
            point(jessieCodeSession.scene, "mirrored").coordinates,
            point(jessieCodeSession.scene, "alias").coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "source",
                coordinates = JsxGraphPoint2D(-2.0, 2.0),
            ),
        ).value
        assertPointEquals(
            JsxGraphPoint2D(2.0, 2.0),
            point(moved, "reflected").coordinates,
        )
        assertPointEquals(
            JsxGraphPoint2D(2.0, -4.0),
            point(moved, "mirrored").coordinates,
        )
        assertEquals(
            point(moved, "mirrored").coordinates,
            point(moved, "alias").coordinates,
        )
    }

    @Test
    fun bisectorLinesFocusedCaseTracksBothOutputsFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("bisector_lines"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "D",
                "first",
                "second",
                "bisectorFirst",
                "bisectorSecond",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        val initialFirst = line(
            jessieCodeSession.scene,
            "bisectorFirst",
        )
        val initialSecond = line(
            jessieCodeSession.scene,
            "bisectorSecond",
        )
        assertEquals(7, initialFirst.style.layer)
        assertEquals(6.0, initialFirst.style.strokeWidth)
        assertEquals(7, initialSecond.style.layer)
        assertEquals(3.0, initialSecond.style.strokeWidth)
        assertPointEquals(
            JsxGraphPoint2D(
                -0.14987187202709523,
                1.668124255252562,
            ),
            initialFirst.point1,
        )
        assertPointEquals(
            JsxGraphPoint2D(
                1.3457036003260912,
                1.471804427762934,
            ),
            initialSecond.point1,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(-2.0, -3.0),
            ),
        ).value
        assertNotEquals(
            initialFirst.point1,
            line(moved, "bisectorFirst").point1,
        )
        assertNotEquals(
            initialSecond.point1,
            line(moved, "bisectorSecond").point1,
        )
    }

    @Test
    fun sectorCompositionFocusedCaseTracksAllFiveCurvesFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("sector_compositions"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        val ids = listOf(
            "circumRegion",
            "minorRegion",
            "majorRegion",
            "nonreflexRegion",
            "reflexRegion",
        )
        assertEquals(
            listOf("A", "B", "C") + ids,
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            listOf(9, 9, 9, 3, 3, 3, 3, 3),
            jessieCodeSession.scene.elements.map { element ->
                element.style.layer
            },
        )

        val initial = ids.associateWith { id ->
            curve(jessieCodeSession.scene, id)
        }
        initial.values.forEach { sector ->
            assertEquals(19, sector.points.size)
        }
        val initialCircum = initial.getValue("circumRegion")
        assertPointEquals(
            JsxGraphPoint2D(4.0, -3.0),
            initialCircum.points[3],
        )
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -2.0),
            initialCircum.points[15],
        )
        assertNotEquals(
            initial.getValue("minorRegion").points[6],
            initial.getValue("majorRegion").points[6],
        )
        assertNotEquals(
            initial.getValue("nonreflexRegion").points[6],
            initial.getValue("reflexRegion").points[6],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(0.0, -4.0),
            ),
        ).value
        val movedCircum = curve(moved, "circumRegion")
        assertPointEquals(
            JsxGraphPoint2D(-4.0, -2.0),
            movedCircum.points[3],
        )
        assertPointEquals(
            JsxGraphPoint2D(4.0, -3.0),
            movedCircum.points[15],
        )
        for (id in ids) {
            assertNotEquals(
                initial.getValue(id).points[6],
                curve(moved, id).points[6],
                id,
            )
        }
    }

    @Test
    fun defaultSourceProducesTheNativeScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(DEFAULT_PARITY_SOURCE),
        ).value
        assertEquals(4, scene.elements.size)
        val fixedPoint = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
        )
        assertEquals(-4.0, fixedPoint.coordinates.x)
        assertEquals(-2.0, fixedPoint.coordinates.y)
        val controlPoint = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[1],
        )
        assertEquals(3.2, controlPoint.coordinates.x)
        assertEquals(2.1, controlPoint.coordinates.y)
        val circle = assertIs<JsxGraphSceneElement.Circle>(
            scene.elements[3],
        )
        assertEquals(2.35, circle.radius)
    }

    @Test
    fun segmentCasesUseTheNativeFiniteEndpointDefaults() {
        for (caseId in listOf("finite_segment", "jessiecode_native_source")) {
            val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
                JsxGraphParityCorpus.find(caseId),
            ).value
            val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
                parseParitySource(parityCase.source),
            ).value
            val segment = scene.elements
                .filterIsInstance<JsxGraphSceneElement.Line>()
                .single()

            assertEquals(false, segment.straightFirst, caseId)
            assertEquals(false, segment.straightLast, caseId)
        }
    }

    @Test
    fun everyCorpusSourceProducesANativeScene() {
        for (parityCase in JsxGraphParityCorpus.cases) {
            assertIs<GMResult.Ok<JsxGraphScene>>(
                parseParitySource(parityCase.source),
                parityCase.id,
            )
        }
    }

    @Test
    fun jessieCodeEnvelopeUsesTheSameSourceForANativeSession() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("jessiecode_native_source"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        val jessieCodeInput = assertIs<JsxGraphParityInput.JessieCode>(input)
        assertEquals("jxgbox", jessieCodeInput.boardOptions.containerId)
        assertEquals(
            JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
            jessieCodeInput.boardOptions.boundingBox,
        )

        val session = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(session)
        assertEquals(
            listOf("sourcePoint", "targetPoint", "route", "region"),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(jessieCodeInput.source, jessieCodeSession.session.code.trim())
    }

    @Test
    fun functionRadiusFocusedCaseProducesTheNativeCircle() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("function_circle_radius"),
        ).value
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(parityCase.source),
        ).value
        val circle = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Circle>()
            .single()

        assertEquals(2.0, circle.radius)
    }

    @Test
    fun transformedPointFocusedCaseUsesOneSourceForNativeGeometry() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("transformed_points"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        assertIs<JsxGraphParityInput.JessieCode>(input)

        val sceneResult = parseParitySource(parityCase.source)
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            sceneResult,
            sceneResult.toString(),
        ).value
        assertEquals(8, scene.elements.size)
        assertEquals(
            JsxGraphPoint2D(-1.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { element ->
                    element.id == "shifted"
                },
            ).coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(1.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                scene.elements.single { element ->
                    element.id == "mirrored"
                },
            ).coordinates,
        )
    }

    @Test
    fun point3DProjectionFocusedCaseUsesProxyGeometryAndDragLifecycle() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("point3d_projection"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        assertIs<JsxGraphParityInput.ConstructionDocument>(input)

        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val session = assertIs<
            JsxGraphParitySession.ConstructionDocument
            >(paritySession).session
        assertEquals(27, session.scene.elements.size)
        assertEquals(
            listOf("source3d", "homogeneous3d", "transformed3d"),
            session.scene.elements
                .map(JsxGraphSceneElement::id)
                .filter {
                    it in setOf(
                        "source3d",
                        "homogeneous3d",
                        "transformed3d",
                    )
                },
        )

        val source = point(session.scene, "source3d")
        assertEquals(
            -0.7566557074166769,
            source.coordinates.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -0.7886977433927291,
            source.coordinates.y,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(JsxGraphColor.Transparent, source.style.fillColor)
        assertEquals(JsxGraphColor(217, 85, 63), source.style.strokeColor)
        assertEquals(3.0, source.style.strokeWidth)
        assertEquals(
            setOf("source3d"),
            session.captureInteractionState().pointCoordinates.keys,
        )

        val transformedBefore =
            point(session.scene, "transformed3d").coordinates
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "source3d",
                coordinates = JsxGraphPoint2D(0.0, 0.0),
            ),
        ).value
        assertPointEquals(
            JsxGraphPoint2D(0.0, 0.0),
            point(moved, "source3d").coordinates,
        )
        assertNotEquals(
            transformedBefore,
            point(moved, "transformed3d").coordinates,
        )
    }

    @Test
    fun spatialLinesPlanesFocusedCaseIncludesVisibleMesh3D() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("spatial_lines_planes"),
        ).value
        assertTrue("mesh3d" in parityCase.features)

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(parityCase.source),
        ).value
        val mesh = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Curve>()
            .single { curve ->
                curve.points.size == 58 &&
                    curve.style.strokeColor ==
                    JsxGraphColor(154, 154, 154)
            }

        assertTrue(mesh.style.visible)
        assertEquals(0.6, mesh.style.strokeOpacity)
        assertEquals(12, mesh.style.layer)
    }

    @Test
    fun plane3DSurfacesFocusedCaseCoversEveryFiniteSurfaceMode() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("plane3d_surfaces"),
        ).value
        assertTrue("rectangle-tiling" in parityCase.features)
        assertTrue("triangle-tiling" in parityCase.features)
        assertTrue("shader" in parityCase.features)
        assertTrue("colormap" in parityCase.features)

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(parityCase.source),
        ).value
        val curves =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
        for (id in listOf("colors", "shader", "colormap")) {
            assertEquals(0, curves.single { it.id == id }.points.size)
        }
        val visibleFaces = curves.filter {
            it.style.visible && it.points.size in setOf(4, 5)
        }
        assertEquals(35, visibleFaces.size)
        assertEquals(23, visibleFaces.count { it.points.size == 4 })
        assertEquals(12, visibleFaces.count { it.points.size == 5 })
        assertTrue(
            visibleFaces.map { it.style.fillColor }.distinct().size >= 5,
        )
    }

    @Test
    fun view3DDefaultAxesFocusedCaseExpandsTicksAndLabels() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("view3d_default_axes"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        assertIs<JsxGraphParityInput.ConstructionDocument>(input)

        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(parityCase.source),
        ).value
        val lines =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>()
        val curves =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
        val ticks = curves.filter { curve -> curve.ticks3D != null }
        val labels =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Text>()

        assertEquals(63, scene.elements.size)
        assertEquals(15, lines.size)
        assertEquals(3, lines.count { line -> line.style.visible })
        assertEquals(15, curves.size)
        assertEquals(3, ticks.size)
        assertEquals(
            6,
            curves.count {
                it.style.strokeColor == JsxGraphColor(154, 154, 154)
            },
        )
        assertTrue(ticks.all { curve -> curve.points.size == 33 })
        assertEquals(33, labels.size)
        assertEquals(
            mapOf(
                "-5" to 1,
                "-4" to 2,
                "-3" to 3,
                "-2" to 3,
                "-1" to 3,
                "0" to 3,
                "1" to 3,
                "2" to 3,
                "3" to 3,
                "4" to 3,
                "5" to 3,
                "6" to 2,
                "7" to 1,
            ),
            labels.groupingBy { label -> label.content }.eachCount(),
        )
    }

    @Test
    fun view3DCenterAxesFocusedCasePreservesHiddenOfficialOrigin() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("view3d_center_axes"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        assertIs<JsxGraphParityInput.ConstructionDocument>(input)

        val result = parseParitySource(parityCase.source)
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            result,
            result.toString(),
        ).value
        val lines =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Line>()
        val curves =
            scene.elements.filterIsInstance<JsxGraphSceneElement.Curve>()
        val origin = scene.elements
            .filterIsInstance<JsxGraphSceneElement.Point>()
            .single()

        assertEquals(28, scene.elements.size)
        assertEquals(15, lines.size)
        assertEquals(3, lines.count { line -> line.style.visible })
        assertEquals(12, curves.size)
        assertTrue(curves.none { curve -> curve.style.visible })
        assertFalse(origin.style.visible)
        assertFalse(origin.draggable)
        assertFalse(origin.isReal)
        assertEquals(JsxGraphPoint2D(0.0, 0.0), origin.coordinates)
    }

    @Test
    fun functionCoordinateFocusedCaseTracksTheDriver() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("function_coordinate_points"),
        ).value
        val input = assertIs<GMResult.Ok<JsxGraphParityInput>>(
            parseParityInput(parityCase.source),
        ).value
        assertIs<JsxGraphParityInput.JessieCode>(input)

        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(7, jessieCodeSession.scene.elements.size)
        assertEquals(
            JsxGraphPoint2D(-1.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                jessieCodeSession.scene.elements.single { element ->
                    element.id == "arrayPoint"
                },
            ).coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(-4.0, 1.0),
            assertIs<JsxGraphSceneElement.Point>(
                jessieCodeSession.scene.elements.single { element ->
                    element.id == "homogeneousPoint"
                },
            ).coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(-1.0, 1.0),
            ),
        ).value
        assertEquals(
            JsxGraphPoint2D(1.0, 3.0),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { element ->
                    element.id == "arrayPoint"
                },
            ).coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(4.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { element ->
                    element.id == "mixedPoint"
                },
            ).coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(-2.0, 4.0),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { element ->
                    element.id == "homogeneousPoint"
                },
            ).coordinates,
        )
    }

    @Test
    fun parallelFocusedCaseTracksFiniteAndIdealFormsFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("parallel_constructions"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf(
                "A",
                "B",
                "baseLine",
                "C",
                "parallelPoint",
                "finiteParallel",
                "idealParallel",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )
        assertEquals(
            JsxGraphPoint2D(5.0, -4.0),
            assertIs<JsxGraphSceneElement.Point>(
                jessieCodeSession.scene.elements.single { element ->
                    element.id == "parallelPoint"
                },
            ).coordinates,
        )
        val finite = assertIs<JsxGraphSceneElement.Line>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "finiteParallel"
            },
        )
        assertEquals(false, finite.straightFirst)
        assertEquals(false, finite.straightLast)
        assertEquals(JsxGraphPoint2D(3.0, -3.0), finite.point1)
        assertEquals(JsxGraphPoint2D(5.0, -4.0), finite.point2)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(0.0, 4.0),
            ),
        ).value
        assertEquals(
            JsxGraphPoint2D(2.0, 3.0),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements.single { element ->
                    element.id == "parallelPoint"
                },
            ).coordinates,
        )
        val movedFinite = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element ->
                element.id == "finiteParallel"
            },
        )
        assertEquals(JsxGraphPoint2D(0.0, 4.0), movedFinite.point1)
        assertEquals(JsxGraphPoint2D(2.0, 3.0), movedFinite.point2)

        val baseLine = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element -> element.id == "baseLine" },
        )
        val ideal = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element -> element.id == "idealParallel" },
        )
        assertEquals(true, ideal.straightFirst)
        assertEquals(true, ideal.straightLast)
        assertEquals(JsxGraphPoint2D(0.0, 4.0), ideal.point1)
        val baseDeltaX = baseLine.point2.x - baseLine.point1.x
        val baseDeltaY = baseLine.point2.y - baseLine.point1.y
        val idealDeltaX = ideal.point2.x - ideal.point1.x
        val idealDeltaY = ideal.point2.y - ideal.point1.y
        assertEquals(
            0.0,
            baseDeltaX * idealDeltaY - baseDeltaY * idealDeltaX,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun lineArrowFocusedCaseCoversAllTypesAndParallelDrag() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("line_arrows"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val documentSession =
            assertIs<JsxGraphParitySession.ConstructionDocument>(
                paritySession,
            )
        assertEquals(15, documentSession.scene.elements.size)
        for (type in 1..7) {
            val arrow = assertIs<JsxGraphSceneElement.Line>(
                documentSession.scene.elements.single { element ->
                    element.id == "arrowType$type"
                },
            )
            assertEquals(false, arrow.straightFirst)
            assertEquals(false, arrow.straightLast)
            assertEquals(type, arrow.lastArrow?.type)
        }
        val doubleArrow = assertIs<JsxGraphSceneElement.Line>(
            documentSession.scene.elements.single { element ->
                element.id == "doubleArrow"
            },
        )
        assertEquals(
            JsxGraphArrowHead(type = 2, size = 5.0, highlightSize = null),
            doubleArrow.firstArrow,
        )
        assertEquals(
            JsxGraphArrowHead(type = 3, size = 7.0, highlightSize = null),
            doubleArrow.lastArrow,
        )
        val ideal = assertIs<JsxGraphSceneElement.Line>(
            documentSession.scene.elements.single { element ->
                element.id == "idealArrowParallel"
            },
        )
        assertEquals(false, ideal.straightFirst)
        assertEquals(true, ideal.straightLast)
        assertEquals(7, ideal.lastArrow?.type)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            documentSession.session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(4.0, -3.0),
            ),
        ).value
        val finite = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element ->
                element.id == "finiteArrowParallel"
            },
        )
        assertEquals(JsxGraphPoint2D(4.0, -3.0), finite.point1)
        assertEquals(JsxGraphPoint2D(6.0, -2.0), finite.point2)
        assertEquals(4, finite.lastArrow?.type)
    }

    @Test
    fun triangleCentersFocusedCaseTracksAllConstructionsFromOneSource() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("triangle_centers"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "edgeAB",
                "edgeBC",
                "edgeCA",
                "angleBisector",
                "triangleIncenter",
                "triangleIncircle",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )

        val initialBisector = assertIs<JsxGraphSceneElement.Line>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "angleBisector"
            },
        )
        assertEquals(JsxGraphPoint2D(0.0, 4.0), initialBisector.point1)
        assertEquals(
            -0.034421443730739576,
            initialBisector.point2.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            3.0005925934777693,
            initialBisector.point2.y,
            absoluteTolerance = 1.0e-12,
        )
        val initialIncenter = assertIs<JsxGraphSceneElement.Point>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "triangleIncenter"
            },
        )
        assertEquals(
            -0.14589803375031543,
            initialIncenter.coordinates.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -0.23606797749978967,
            initialIncenter.coordinates.y,
            absoluteTolerance = 1.0e-12,
        )
        val initialIncircle = assertIs<JsxGraphSceneElement.Circle>(
            jessieCodeSession.scene.elements.single { element ->
                element.id == "triangleIncircle"
            },
        )
        assertEquals(initialIncenter.coordinates, initialIncircle.center)
        assertEquals(
            2.2283532364172802,
            initialIncircle.radius,
            absoluteTolerance = 1.0e-12,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(1.0, 4.0),
            ),
        ).value
        val movedEdge = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element -> element.id == "edgeBC" },
        )
        assertEquals(JsxGraphPoint2D(1.0, 4.0), movedEdge.point2)
        val movedBisector = assertIs<JsxGraphSceneElement.Line>(
            moved.elements.single { element ->
                element.id == "angleBisector"
            },
        )
        assertEquals(
            0.47185792553202427,
            movedBisector.point2.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            3.118325401232056,
            movedBisector.point2.y,
            absoluteTolerance = 1.0e-12,
        )
        val movedIncenter = assertIs<JsxGraphSceneElement.Point>(
            moved.elements.single { element ->
                element.id == "triangleIncenter"
            },
        )
        assertEquals(
            0.20042643751066208,
            movedIncenter.coordinates.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            3.625499775858468,
            movedIncenter.coordinates.y,
            absoluteTolerance = 1.0e-12,
        )
        val movedIncircle = assertIs<JsxGraphSceneElement.Circle>(
            moved.elements.single { element ->
                element.id == "triangleIncircle"
            },
        )
        assertEquals(movedIncenter.coordinates, movedIncircle.center)
        assertEquals(
            0.37450022414153233,
            movedIncircle.radius,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun intersectionFocusedCaseTracksRealAndNonRealBranches() {
        val parityCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("intersection_points"),
        ).value
        val paritySession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(parityCase.source),
        ).value
        val jessieCodeSession =
            assertIs<JsxGraphParitySession.JessieCode>(paritySession)
        assertEquals(
            listOf(
                "O",
                "C",
                "lineA",
                "lineB",
                "mainLine",
                "segmentA",
                "segmentB",
                "finiteSegment",
                "first",
                "other",
                "extended",
                "clipped",
            ),
            jessieCodeSession.scene.elements.map { element -> element.id },
        )

        val first = point(jessieCodeSession.scene, "first")
        val other = point(jessieCodeSession.scene, "other")
        val extended = point(jessieCodeSession.scene, "extended")
        val clipped = point(jessieCodeSession.scene, "clipped")
        assertEquals(JsxGraphPoint2D(3.0, 0.0), first.coordinates)
        assertEquals(JsxGraphPoint2D(-3.0, 0.0), other.coordinates)
        assertEquals(
            2.23606797749979,
            extended.coordinates.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(2.0, extended.coordinates.y)
        assertEquals(false, clipped.isReal)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "O",
                coordinates = JsxGraphPoint2D(0.0, 4.0),
            ),
        ).value
        assertEquals(false, point(moved, "first").isReal)
        assertEquals(false, point(moved, "other").isReal)
        assertEquals(true, point(moved, "extended").isReal)
        assertEquals(false, point(moved, "clipped").isReal)
        assertEquals(
            JsxGraphPoint2D(0.0, 4.0),
            point(moved, "O").coordinates,
        )

        val restored = assertIs<GMResult.Ok<JsxGraphScene>>(
            jessieCodeSession.session.movePoint(
                id = "O",
                coordinates = JsxGraphPoint2D(0.0, 0.0),
            ),
        ).value
        assertEquals(true, point(restored, "first").isReal)
        assertEquals(true, point(restored, "other").isReal)
    }

    @Test
    fun textParitySourceEvaluatesDynamicContent() {
        val textCase = assertIs<GMResult.Ok<JsxGraphParityCase>>(
            JsxGraphParityCorpus.find("text"),
        ).value
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            parseParitySource(textCase.source),
        ).value
        val texts = scene.elements.filterIsInstance<JsxGraphSceneElement.Text>()

        assertEquals(5, texts.size)
        assertEquals("A.x = 2.0", texts.last().content)
    }

    @Test
    fun baselineSessionRestoresTheDraggableControlPoint() {
        val nativeSession = assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(DEFAULT_PARITY_SOURCE),
        ).value
        val session = assertIs<JsxGraphParitySession.ConstructionDocument>(
            nativeSession,
        ).session
        assertEquals(
            setOf("B"),
            session.captureInteractionState().pointCoordinates.keys,
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(1.5, 3.0)),
        ).value
        assertEquals(
            JsxGraphPoint2D(1.5, 3.0),
            assertIs<JsxGraphSceneElement.Line>(
                moved.elements[2],
            ).point2,
        )

        val restoredNativeSession =
            assertIs<GMResult.Ok<JsxGraphParitySession>>(
            createParitySession(DEFAULT_PARITY_SOURCE),
        ).value
        val restoredSession =
            assertIs<JsxGraphParitySession.ConstructionDocument>(
                restoredNativeSession,
            ).session
        val restored = assertIs<GMResult.Ok<JsxGraphScene>>(
            restoredSession.restoreInteractionState(
                JsxGraphInteractionState(
                    pointCoordinates =
                        session.captureInteractionState().pointCoordinates,
                ),
            ),
        ).value
        assertEquals(
            JsxGraphPoint2D(1.5, 3.0),
            assertIs<JsxGraphSceneElement.Point>(
                restored.elements[1],
            ).coordinates,
        )
    }

    @Test
    fun malformedAndUnsupportedSourcesReturnExplicitErrors() {
        assertIs<GMResult.Err<String>>(parseParitySource("not-json"))
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                """
                {
                  "schemaVersion": 2,
                  "boundingBox": [-6, 5, 6, -5],
                  "objects": []
                }
                """.trimIndent(),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"parents\": [[0.5, 0.6], 2.35]",
                    "\"parents\": [[0.5, 0.6], \"missing\"]",
                ),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                DEFAULT_PARITY_SOURCE.replace(
                    "\"strokeWidth\": 2.5",
                    "\"strokeWidth\": -1",
                ),
            ),
        )
        assertIs<GMResult.Err<String>>(
            parseParitySource(
                """
                {
                  "schemaVersion": 1,
                  "inputKind": "javascript",
                  "boardOptions": {
                    "containerId": "jxgbox",
                    "boundingBox": [-5, 5, 5, -5],
                    "axis": false,
                    "grid": false,
                    "keepAspectRatio": false
                  },
                  "source": "alert('not supported')"
                }
                """.trimIndent(),
            ),
        )
    }

    private fun curve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs<JsxGraphSceneElement.Curve>(
            scene.elements.single { element -> element.id == id },
        )

    private fun point(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs<JsxGraphSceneElement.Point>(
            scene.elements.single { element -> element.id == id },
        )

    private fun circle(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Circle =
        assertIs<JsxGraphSceneElement.Circle>(
            scene.elements.single { element -> element.id == id },
        )

    private fun line(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single { element -> element.id == id },
        )

    private fun assertPointEquals(
        expected: JsxGraphPoint2D,
        actual: Any?,
    ) {
        val point = assertIs<JsxGraphPoint2D>(actual)
        assertEquals(expected.x, point.x, absoluteTolerance = 1.0e-12)
        assertEquals(expected.y, point.y, absoluteTolerance = 1.0e-12)
    }
}
