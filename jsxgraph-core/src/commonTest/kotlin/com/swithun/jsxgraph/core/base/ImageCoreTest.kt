package com.swithun.jsxgraph.core.base

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.parser.JessieCodeConstantCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeNumericCoordinateFunction
import com.swithun.jsxgraph.core.parser.JessieCodeRuntimeValue
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ImageCoreTest {
    @Test
    fun hitTestingAndParentExportFollowImageGeometry() {
        val board = Board(
            originX = 100.0,
            originY = 100.0,
            unitX = 10.0,
            unitY = 10.0,
        )
        val image = assertIs<GMResult.Ok<Image>>(
            Image.create(
                board = board,
                urlTerm = JessieCodeConstantCoordinateFunction(
                    JessieCodeRuntimeValue.StringValue("asset:image"),
                ),
                coordinates = doubleArrayOf(0.0, 0.0),
                sizeTerms = listOf(
                    JessieCodeNumericCoordinateFunction(2.0),
                    JessieCodeNumericCoordinateFunction(1.0),
                ),
                id = "image",
                name = "",
            ),
        ).value

        assertTrue(image.hasPoint(110.0, 95.0))
        assertFalse(image.hasPoint(140.0, 95.0, precision = 0.0))
        val parents = image.parentDefinitions()
        assertEquals("asset:image", parents[0])
        assertContentEquals(
            doubleArrayOf(1.0, 0.0, 0.0),
            assertIs<DoubleArray>(parents[1]),
        )
        assertContentEquals(
            doubleArrayOf(2.0, 1.0),
            assertIs<DoubleArray>(parents[2]),
        )

        val translation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create(
                type = "translate",
                parameters = doubleArrayOf(3.0, 4.0),
            ),
        ).value
        image.addTransform(image, translation)
        image.fullUpdate()

        assertTrue(image.hasPoint(140.0, 55.0, precision = 0.0))
        assertFalse(image.hasPoint(110.0, 95.0, precision = 0.0))
    }
}
