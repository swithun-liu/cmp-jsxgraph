/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class Parse3DTest {
    @Test
    fun asciiStlParsingMatchesOfficialFixture() {
        val source = """
            solid first
             facet normal 0 0 0
              outer loop
               vertex 0 0 0
               vertex 1 0 0
               vertex 0 1 0
              endloop
             endfacet
             facet normal 0 0 0
              outer loop
               vertex 0.0000005 0 0
               vertex 0.000001 0 0
               vertex 1 0 0
              endloop
             endfacet
            endsolid first
            solid second
             facet normal 0 0 0
              outer loop
               vertex 1tail .5 -2e1
              endloop
             endfacet
            endsolid second
        """.trimIndent()

        val polyhedra = assertIs<
            GMResult.Ok<List<ParsedPolyhedron3D>>,
        >(Parse3D.STL(source)).value

        assertEquals(2, polyhedra.size)
        assertEquals(
            listOf(
                listOf(0.0, 0.0, 0.0),
                listOf(1.0, 0.0, 0.0),
                listOf(0.0, 1.0, 0.0),
                listOf(0.000001, 0.0, 0.0),
            ),
            polyhedra[0].vertices,
        )
        assertEquals(
            listOf(
                listOf(0, 1, 2),
                listOf(0, 3, 1),
            ),
            polyhedra[0].faces,
        )
        assertEquals(
            listOf(listOf(1.0, 0.5, -20.0)),
            polyhedra[1].vertices,
        )
        assertEquals(listOf(listOf(0)), polyhedra[1].faces)
    }

    @Test
    fun incompleteSolidMatchesOfficialEmptyResult() {
        val result = assertIs<
            GMResult.Ok<List<ParsedPolyhedron3D>>,
        >(
            Parse3D.STL(
                "solid open\nfacet normal 0 0 0\nvertex 1 2 3",
            ),
        )

        assertEquals(emptyList(), result.value)
    }

    @Test
    fun malformedStructureAndInvalidLimitsFailStructurally() {
        val outsideFacet = assertIs<
            GMResult.Err<Parse3DError.VertexOutsideFacet>,
        >(Parse3D.STL("solid broken\nvertex 1 2 3"))
        assertEquals(2, outsideFacet.error.line)

        assertIs<GMResult.Err<Parse3DError.InvalidLimits>>(
            Parse3D.STL(
                "",
                Parse3DLimits(maxSourceLength = 0),
            ),
        )
        assertIs<GMResult.Err<Parse3DError.SourceLengthExceeded>>(
            Parse3D.STL(
                "solid",
                Parse3DLimits(maxSourceLength = 4),
            ),
        )
    }

    @Test
    fun parserChecksResourceLimitsBeforeGrowingCollections() {
        val vertexLimit = assertIs<
            GMResult.Err<Parse3DError.VertexLimitExceeded>,
        >(
            Parse3D.STL(
                "solid m\nfacet normal 0 0 0\n" +
                    "vertex 0 0 0\nvertex 1 0 0",
                Parse3DLimits(maxVerticesPerPolyhedron = 1),
            ),
        )
        assertEquals(1, vertexLimit.error.limit)

        assertIs<GMResult.Err<Parse3DError.FaceLimitExceeded>>(
            Parse3D.STL(
                "solid m\nfacet normal 0 0 0\nfacet normal 0 0 0",
                Parse3DLimits(maxFacesPerPolyhedron = 1),
            ),
        )
        assertIs<GMResult.Err<Parse3DError.FaceVertexLimitExceeded>>(
            Parse3D.STL(
                "solid m\nfacet normal 0 0 0\n" +
                    "vertex 0 0 0\nvertex 0 0 0",
                Parse3DLimits(maxVerticesPerFace = 1),
            ),
        )
        assertIs<GMResult.Err<Parse3DError.PolyhedronLimitExceeded>>(
            Parse3D.STL(
                "solid a\nendsolid a\nsolid b\nendsolid b",
                Parse3DLimits(maxPolyhedra = 1),
            ),
        )
    }
}
