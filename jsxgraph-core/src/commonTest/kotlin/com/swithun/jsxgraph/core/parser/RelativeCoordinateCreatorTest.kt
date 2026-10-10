package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.CoordsElement
import com.swithun.jsxgraph.core.base.ForeignObject
import com.swithun.jsxgraph.core.base.Image
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.Text
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RelativeCoordinateCreatorTest {
    @Test
    fun nativeCreatorsExposeRelativeAndSliderCoordinateContracts() {
        val board = Board(
            originX = 320.0,
            originY = 240.0,
            unitX = 40.0,
            unitY = 40.0,
            canvasWidth = 640.0,
            canvasHeight = 480.0,
            id = "board",
        )
        evaluate(
            source =
                """
                A = point(1, 2) <<
                    id: "anchor", name: "", withLabel: false
                >>;
                point(0.5, -1) <<
                    id: "relativePoint", name: "",
                    withLabel: false, anchor: A
                >>;
                image(
                    "data:image/gif;base64,R0lGODlhAQABAIAAAAAAAP///ywAAAAAAQABAAACAUwAOw==",
                    [0.25, -0.5],
                    [1, 1]
                ) <<
                    id: "relativeImage", name: "", anchor: A
                >>;
                foreignobject(
                    "<span>relative</span>",
                    [-0.75, 0.25],
                    [1, 1]
                ) <<
                    id: "relativeForeignObject", name: "", anchor: A
                >>;
                text(1, -2, "relative") <<
                    id: "relativeText", name: "", anchor: A
                >>;
                text(10, 20, "label") <<
                    id: "relativeLabel", name: "", anchor: A,
                    isLabel: true, offset: [5, 7]
                >>;
                S = slider(
                    [-4, -4],
                    [4, -4],
                    [-2, 1, 6]
                ) <<
                    id: "slider", name: "",
                    withLabel: false, withTicks: false
                >>;
                point(
                    S,
                    function () { return S.Value() + 1; }
                ) <<
                    id: "sliderPoint", name: "", withLabel: false
                >>;
                """.trimIndent(),
            board = board,
        )
        board.update()

        val anchor = assertIs<Point>(board.select("anchor"))
        val point = assertIs<Point>(board.select("relativePoint"))
        val image = assertIs<Image>(board.select("relativeImage"))
        val foreignObject =
            assertIs<ForeignObject>(board.select("relativeForeignObject"))
        val text = assertIs<Text>(board.select("relativeText"))
        val label = assertIs<Text>(board.select("relativeLabel"))
        val slider = assertIs<Slider>(board.select("slider"))
        val sliderPoint = assertIs<Point>(board.select("sliderPoint"))

        assertCoordinates(point, 1.5, 1.0)
        assertCoordinates(image, 1.25, 1.5)
        assertCoordinates(foreignObject, 0.25, 2.25)
        assertCoordinates(text, 2.0, 0.0)
        assertCoordinates(label, 1.375, 1.675)
        assertCoordinates(sliderPoint, 1.0, 2.0)
        assertEquals(listOf("slider"), sliderPoint.parents)

        slider.setValue(5.0)
        anchor.setPositionDirectly(
            Const.COORDS_BY_USER,
            doubleArrayOf(-2.0, -1.0),
        )
        board.update()

        assertCoordinates(point, -1.5, -2.0)
        assertCoordinates(image, -1.75, -1.5)
        assertCoordinates(foreignObject, 0.25, 2.25)
        assertCoordinates(text, -1.0, -3.0)
        assertCoordinates(label, -1.625, -1.325)
        assertCoordinates(sliderPoint, 5.0, 6.0)
    }

    private fun evaluate(
        source: String,
        board: Board,
    ): JessieCodeRuntimeValue {
        val ast = assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value
        return assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(
            JessieCodeEvaluator().evaluate(
                ast,
                JessieCodeRuntimeEnvironment(board = board),
            ),
        ).value
    }

    private fun assertCoordinates(
        element: CoordsElement,
        x: Double,
        y: Double,
    ) {
        assertEquals(x, element.X(), absoluteTolerance = 1.0e-12)
        assertEquals(y, element.Y(), absoluteTolerance = 1.0e-12)
    }
}
