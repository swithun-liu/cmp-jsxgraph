/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core

import kotlin.math.hypot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class JsxGraphEngineTest {
    @Test
    fun sourceCreatesBoardElementsAndPortableScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(REFERENCE_SOURCE),
        ).value

        assertEquals(
            JsxGraphBoundingBox(-6.0, 5.0, 6.0, -5.0),
            scene.boundingBox,
        )
        assertTrue(scene.axis)
        assertTrue(scene.grid)
        assertTrue(scene.keepAspectRatio)
        assertEquals(4, scene.elements.size)

        val pointA = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[0],
        )
        assertEquals("A", pointA.id)
        assertEquals(JsxGraphPoint2D(1.0, 2.0), pointA.coordinates)
        assertEquals(4.0, pointA.size)
        assertEquals(JsxGraphColor(111, 119, 128), pointA.style.fillColor)

        val pointB = assertIs<JsxGraphSceneElement.Point>(
            scene.elements[1],
        )
        assertEquals(JsxGraphPoint2D(3.0, 1.0), pointB.coordinates)

        val line = assertIs<JsxGraphSceneElement.Line>(
            scene.elements[2],
        )
        assertEquals(pointA.coordinates, line.point1)
        assertEquals(pointB.coordinates, line.point2)
        assertEquals(false, line.straightFirst)
        assertEquals(false, line.straightLast)
        assertEquals(2.5, line.style.strokeWidth)

        val circle = assertIs<JsxGraphSceneElement.Circle>(
            scene.elements[3],
        )
        assertEquals(pointA.coordinates, circle.center)
        assertEquals(2.5, circle.radius)
        assertEquals(JsxGraphColor(22, 135, 122), circle.style.strokeColor)
    }

    @Test
    fun coordinateParentsCreateOnlyRequestedSceneElements() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "line",
                      "type": "line",
                      "parents": [[-2, -1], [3, 2]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": false
                      }
                    },
                    {
                      "id": "circle",
                      "type": "circle",
                      "parents": [[1, 1], 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(2, scene.elements.size)
        assertEquals("line", scene.elements[0].id)
        assertEquals("circle", scene.elements[1].id)
        assertEquals(
            JsxGraphPoint2D(1.0, 1.0),
            assertIs<JsxGraphSceneElement.Circle>(
                scene.elements[1],
            ).center,
        )
    }

    @Test
    fun orthogonalConstructionsRenderOnlyRequestedElementsAndTrackSourcePoint() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-4,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"baseLine",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"P",
                      "type":"point",
                      "parents":[3,-3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"projection",
                      "type":"orthogonalprojection",
                      "parents":["P","baseLine"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"foot",
                      "type":"perpendicularpoint",
                      "parents":["baseLine","P"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"normal",
                      "type":"perpendicular",
                      "parents":["P","baseLine"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"drop",
                      "type":"perpendicularsegment",
                      "parents":["baseLine","P"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"extendedDrop",
                      "type":"perpendicularsegment",
                      "parents":["baseLine","P"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":true,
                        "straightLast":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf(
                "A",
                "B",
                "baseLine",
                "P",
                "projection",
                "foot",
                "normal",
                "drop",
                "extendedDrop",
            ),
            session.scene.elements.map { it.id },
        )
        val baseLine = sceneLine(session.scene, "baseLine")
        val sourcePoint = scenePoint(session.scene, "P")
        val projection = scenePoint(session.scene, "projection")
        val foot = scenePoint(session.scene, "foot")
        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.07692307692307687,
                1.6153846153846156,
            ),
            projection.coordinates,
        )
        assertPointCoordinates(projection.coordinates, foot.coordinates)

        val normal = sceneLine(session.scene, "normal")
        assertEquals(true, normal.straightFirst)
        assertEquals(true, normal.straightLast)
        assertPerpendicularLine(baseLine, normal, sourcePoint.coordinates)

        val drop = sceneLine(session.scene, "drop")
        assertEquals(false, drop.straightFirst)
        assertEquals(false, drop.straightLast)
        assertPointCoordinates(projection.coordinates, drop.point1)
        assertPointCoordinates(sourcePoint.coordinates, drop.point2)
        val extendedDrop = sceneLine(session.scene, "extendedDrop")
        assertEquals(true, extendedDrop.straightFirst)
        assertEquals(true, extendedDrop.straightLast)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("P", JsxGraphPoint2D(0.0, 4.0)),
        ).value
        val movedProjection = scenePoint(moved, "projection")
        val movedFoot = scenePoint(moved, "foot")
        val expectedProjection = JsxGraphPoint2D(14.0 / 13.0, 31.0 / 13.0)
        assertPointCoordinates(
            expectedProjection,
            movedProjection.coordinates,
        )
        assertPointCoordinates(
            expectedProjection,
            movedFoot.coordinates,
        )
        assertPerpendicularLine(
            baseLine = sceneLine(moved, "baseLine"),
            perpendicular = sceneLine(moved, "normal"),
            through = JsxGraphPoint2D(0.0, 4.0),
        )
        val movedDrop = sceneLine(moved, "drop")
        assertPointCoordinates(expectedProjection, movedDrop.point1)
        assertPointCoordinates(JsxGraphPoint2D(0.0, 4.0), movedDrop.point2)
    }

    @Test
    fun parallelConstructionsRenderFiniteAndIdealFormsFromDocument() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-4,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"baseLine",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[3,-3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"parallelPoint",
                      "type":"parallelpoint",
                      "parents":["A","B","C"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"finiteParallel",
                      "type":"parallel",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":false,
                        "straightLast":false
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"idealParallel",
                      "type":"parallel",
                      "parents":["baseLine","C"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

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
            session.scene.elements.map { it.id },
        )
        assertPointCoordinates(
            JsxGraphPoint2D(9.0, 1.0),
            scenePoint(session.scene, "parallelPoint").coordinates,
        )
        val finite = sceneLine(session.scene, "finiteParallel")
        assertEquals(false, finite.straightFirst)
        assertEquals(false, finite.straightLast)
        assertPointCoordinates(JsxGraphPoint2D(3.0, -3.0), finite.point1)
        assertPointCoordinates(JsxGraphPoint2D(9.0, 1.0), finite.point2)
        val baseLine = sceneLine(session.scene, "baseLine")
        val ideal = sceneLine(session.scene, "idealParallel")
        assertEquals(true, ideal.straightFirst)
        assertEquals(true, ideal.straightLast)
        assertParallelLine(
            baseLine = baseLine,
            parallel = ideal,
            through = JsxGraphPoint2D(3.0, -3.0),
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-2.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(4.0, 1.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(1.0, -4.0)),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(7.0, -5.0),
            scenePoint(moved, "parallelPoint").coordinates,
        )
        val movedFinite = sceneLine(moved, "finiteParallel")
        assertPointCoordinates(JsxGraphPoint2D(1.0, -4.0), movedFinite.point1)
        assertPointCoordinates(JsxGraphPoint2D(7.0, -5.0), movedFinite.point2)
        assertParallelLine(
            baseLine = sceneLine(moved, "baseLine"),
            parallel = sceneLine(moved, "idealParallel"),
            through = JsxGraphPoint2D(1.0, -4.0),
        )
    }

    @Test
    fun arrowsRenderOfficialStaticDefaultsOverridesAndParallelUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-5,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[-1,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[2,-2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"base",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"defaultArrow",
                      "type":"arrow",
                      "parents":["A","B"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":true,
                        "straightLast":true
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"noHeadArrow",
                      "type":"arrow",
                      "parents":["A","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "lastArrow":false
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"doubleArrow",
                      "type":"arrow",
                      "parents":["B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "firstArrow":{
                          "type":2,
                          "size":4,
                          "highlightSize":5
                        },
                        "lastArrow":{
                          "type":3,
                          "size":7,
                          "highlightSize":8
                        }
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"parallelThree",
                      "type":"arrowparallel",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "lastArrow":false
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"parallelIdeal",
                      "type":"arrowparallel",
                      "parents":["base","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "firstArrow":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val defaultArrow = sceneLine(session.scene, "defaultArrow")
        assertFalse(defaultArrow.straightFirst)
        assertFalse(defaultArrow.straightLast)
        assertEquals(null, defaultArrow.firstArrow)
        assertEquals(
            JsxGraphArrowHead(type = 1, size = 6.0, highlightSize = 6.0),
            defaultArrow.lastArrow,
        )
        val noHeadArrow = sceneLine(session.scene, "noHeadArrow")
        assertEquals(null, noHeadArrow.firstArrow)
        assertEquals(null, noHeadArrow.lastArrow)
        val doubleArrow = sceneLine(session.scene, "doubleArrow")
        assertEquals(
            JsxGraphArrowHead(type = 2, size = 4.0, highlightSize = 5.0),
            doubleArrow.firstArrow,
        )
        assertEquals(
            JsxGraphArrowHead(type = 3, size = 7.0, highlightSize = 8.0),
            doubleArrow.lastArrow,
        )
        val finite = sceneLine(session.scene, "parallelThree")
        assertFalse(finite.straightFirst)
        assertFalse(finite.straightLast)
        assertEquals(null, finite.lastArrow)
        val ideal = sceneLine(session.scene, "parallelIdeal")
        assertFalse(ideal.straightFirst)
        assertTrue(ideal.straightLast)
        assertEquals(
            JsxGraphArrowHead(type = 1, size = 6.0, highlightSize = null),
            ideal.firstArrow,
        )
        assertEquals(
            JsxGraphArrowHead(type = 1, size = 6.0, highlightSize = 6.0),
            ideal.lastArrow,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-4.0, -1.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, 3.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(3.0, -3.0)),
        ).value
        val movedFinite = sceneLine(moved, "parallelThree")
        assertPointCoordinates(JsxGraphPoint2D(3.0, -3.0), movedFinite.point1)
        assertPointCoordinates(JsxGraphPoint2D(7.0, 1.0), movedFinite.point2)
        assertParallelLine(
            baseLine = sceneLine(moved, "base"),
            parallel = sceneLine(moved, "parallelIdeal"),
            through = JsxGraphPoint2D(3.0, -3.0),
        )
    }

    @Test
    fun arrowHeadAttributesRejectInvalidStaticConfiguration() {
        for (
            (attribute, expectedPath) in listOf(
                """{"type":8}""" to "lastarrow.type",
                """{"size":-1}""" to "lastarrow.size",
                """{"highlightSize":"large"}""" to
                    "lastarrow.highlightsize",
            )
        ) {
            val result = JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"a",
                      "type":"arrow",
                      "parents":["A","B"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "lastArrow":$attribute
                      }
                    }
                    """.trimIndent(),
                ),
            )
            assertEquals(
                expectedPath,
                assertIs<JsxGraphDocumentError.InvalidAttribute>(
                    assertIs<GMResult.Err<JsxGraphDocumentError>>(result).error,
                ).attribute,
            )
        }
    }

    @Test
    fun triangleCentersRenderAndTrackSourcePointsFromDocument() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-4,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[1,4],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[5,-2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"angleBisector",
                      "type":"bisector",
                      "parents":["A","B","C"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"triangleIncenter",
                      "type":"incenter",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fillColor":"#D97706"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"triangleIncircle",
                      "type":"incircle",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "strokeColor":"#16877A"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "angleBisector",
                "triangleIncenter",
                "triangleIncircle",
            ),
            session.scene.elements.map { it.id },
        )
        val initialBisector = sceneLine(
            session.scene,
            "angleBisector",
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 4.0),
            initialBisector.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.9014623820333578,
                3.0048666733319296,
            ),
            initialBisector.point2,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.6670070476375293,
                0.6370976762025347,
            ),
            scenePoint(
                session.scene,
                "triangleIncenter",
            ).coordinates,
        )
        val initialIncircle = sceneCircle(
            session.scene,
            "triangleIncircle",
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.6670070476375293,
                0.6370976762025347,
            ),
            initialIncircle.center,
        )
        assertEquals(
            2.142469462922355,
            initialIncircle.radius,
            absoluteTolerance = 1.0e-12,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-6.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, 3.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(4.0, -4.0)),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.42887834669531066,
                2.096637745012613,
            ),
            sceneLine(moved, "angleBisector").point2,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.9316296783367083,
                1.0376741014781923,
            ),
            scenePoint(moved, "triangleIncenter").coordinates,
        )
        val movedIncircle = sceneCircle(moved, "triangleIncircle")
        assertPointCoordinates(
            JsxGraphPoint2D(
                -0.9316296783367083,
                1.0376741014781923,
            ),
            movedIncircle.center,
        )
        assertEquals(
            1.7824673672181919,
            movedIncircle.radius,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun circumcenterAliasesAndCircumcircleRenderAndTrackDocumentParents() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-4,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[1,4],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[5,-2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"center",
                      "type":"circumcenter",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fixed":true,
                        "needsRegularUpdate":false
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"alias",
                      "type":"circumcirclemidpoint",
                      "parents":["A","B","C"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circumcircle",
                      "type":"circumcircle",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "strokeColor":"#16877A"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf("A", "B", "C", "center", "alias", "circumcircle"),
            session.scene.elements.map { it.id },
        )
        assertPointCoordinates(
            JsxGraphPoint2D(0.6, -0.6),
            scenePoint(session.scene, "center").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(0.6, -0.6),
            scenePoint(session.scene, "alias").coordinates,
        )
        assertFalse(scenePoint(session.scene, "center").draggable)
        assertEquals(
            JsxGraphPoint2D(0.6, -0.6),
            sceneCircle(session.scene, "circumcircle").center,
        )
        assertEquals(
            JsxGraphColor(22, 135, 122),
            sceneCircle(
                session.scene,
                "circumcircle",
            ).style.strokeColor,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-6.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, 3.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(4.0, -4.0)),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(0.6, -0.6),
            scenePoint(moved, "center").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.108695652173913,
                -2.847826086956522,
            ),
            scenePoint(moved, "alias").coordinates,
        )
        val movedCircle = sceneCircle(moved, "circumcircle")
        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.108695652173913,
                -2.847826086956522,
            ),
            movedCircle.center,
        )
        assertEquals(
            6.2164030835191495,
            movedCircle.radius,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun pointReflectionCreatorsRenderAndTrackDocumentParents() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"source",
                      "type":"point",
                      "parents":[-3,1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"mirror",
                      "type":"point",
                      "parents":[1,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"axisFirst",
                      "type":"point",
                      "parents":[0,-4],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"axisSecond",
                      "type":"point",
                      "parents":[0,4],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"axis",
                      "type":"line",
                      "parents":["axisFirst","axisSecond"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"reflected",
                      "type":"reflection",
                      "parents":["source","axis"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"mirrored",
                      "type":"mirrorelement",
                      "parents":["source","mirror"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"alias",
                      "type":"mirrorpoint",
                      "parents":["source","mirror"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

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
            session.scene.elements.map(JsxGraphSceneElement::id),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(3.0, 1.0),
            scenePoint(session.scene, "reflected").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, -3.0),
            scenePoint(session.scene, "mirrored").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, -3.0),
            scenePoint(session.scene, "alias").coordinates,
        )
        assertFalse(scenePoint(session.scene, "reflected").draggable)
        assertFalse(scenePoint(session.scene, "mirrored").draggable)
        assertFalse(scenePoint(session.scene, "alias").draggable)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "source",
                JsxGraphPoint2D(-2.0, 3.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "mirror",
                JsxGraphPoint2D(0.0, 0.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "axisSecond",
                JsxGraphPoint2D(2.0, 4.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                5.0588235294117645,
                1.2352941176470589,
            ),
            scenePoint(moved, "reflected").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.0, -3.0),
            scenePoint(moved, "mirrored").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.0, -3.0),
            scenePoint(moved, "alias").coordinates,
        )
    }

    @Test
    fun pointReflectionDocumentRejectsCoordinateSourceLikeOfficial() {
        val error = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"mirror",
                      "type":"point",
                      "parents":[1,-1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"output",
                      "type":"mirrorpoint",
                      "parents":[[-3,1],"mirror"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).error

        assertIs<JsxGraphDocumentError.ElementCreation>(error)
    }

    @Test
    fun bisectorLinesDocumentRejectsCompositionReturnExplicitly() {
        val error = assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"a",
                      "type":"point",
                      "parents":[-2,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"b",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"first",
                      "type":"line",
                      "parents":["a","b"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"pair",
                      "type":"bisectorlines",
                      "parents":["first","first"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).error

        val unsupported =
            assertIs<JsxGraphDocumentError.UnsupportedElementType>(error)
        assertEquals(3, unsupported.objectIndex)
        assertEquals("pair", unsupported.id)
        assertEquals("bisectorlines", unsupported.type)
    }

    @Test
    fun circumcenterCoordinateParentsDoNotLeakHelpersIntoDocumentScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"center",
                      "type":"circumcenter",
                      "parents":[[-4,-1],[1,4],[5,-2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circumcircle",
                      "type":"circumcircle",
                      "parents":[[-4,-1],[1,4],[5,-2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(listOf("center", "circumcircle"), scene.elements.map {
            it.id
        })
        assertPointCoordinates(
            JsxGraphPoint2D(0.6, -0.6),
            scenePoint(scene, "center").coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(0.6, -0.6),
            sceneCircle(scene, "circumcircle").center,
        )
    }

    @Test
    fun threePointCircleUpdatesItsImplicitCircumcenterInSession() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [-3, -2],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [3, -1],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fixed": false
                      }
                    },
                    {
                      "id": "C",
                      "type": "point",
                      "parents": [0, 3],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "circumcircle",
                      "type": "circle",
                      "parents": ["A", "B", "C"],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(4, session.scene.elements.size)
        val initialCircle = assertIs<JsxGraphSceneElement.Circle>(
            session.scene.elements.last(),
        )
        assertEquals(
            -0.2037037037037037,
            initialCircle.center.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -0.2777777777777778,
            initialCircle.center.y,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            hypot(
                -3.0 - initialCircle.center.x,
                -2.0 - initialCircle.center.y,
            ),
            initialCircle.radius,
            absoluteTolerance = 1.0e-12,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(4.0, -2.0),
            ),
        ).value
        val movedCircle = assertIs<JsxGraphSceneElement.Circle>(
            moved.elements.last(),
        )
        assertEquals(
            JsxGraphPoint2D(0.5, -0.7),
            movedCircle.center,
        )
        assertEquals(
            hypot(3.5, 1.3),
            movedCircle.radius,
            absoluteTolerance = 1.0e-12,
        )

        assertIs<GMResult.Err<JsxGraphInteractionError.SceneUpdate>>(
            session.movePoint(
                id = "B",
                coordinates = JsxGraphPoint2D(-1.5, 0.5),
            ),
        )
        assertEquals(moved, session.scene)
        assertEquals(
            JsxGraphPoint2D(4.0, -2.0),
            session.captureInteractionState().pointCoordinates["B"],
        )
    }

    @Test
    fun segmentTypeForcesFiniteEndpointsAndSupportsFixedLengthParent() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"segment",
                      "type":"segment",
                      "parents":[[-2,-1],[3,2]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":true,
                        "straightLast":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value
        val segment = assertIs<JsxGraphSceneElement.Line>(
            scene.elements.single(),
        )
        assertEquals(JsxGraphPoint2D(-2.0, -1.0), segment.point1)
        assertEquals(JsxGraphPoint2D(3.0, 2.0), segment.point2)
        assertEquals(false, segment.straightFirst)
        assertEquals(false, segment.straightLast)

        val fixedLength = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"fixed",
                      "type":"segment",
                      "parents":[[0,0],[2,0],3]
                    }
                    """.trimIndent(),
                ),
            ),
        ).value
        val fixedSegment = assertIs<JsxGraphSceneElement.Line>(
            fixedLength.elements.single(),
        )
        assertEquals(JsxGraphPoint2D(-1.0, 0.0), fixedSegment.point1)
        assertEquals(JsxGraphPoint2D(2.0, 0.0), fixedSegment.point2)

        val zeroLength = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"zero",
                      "type":"segment",
                      "parents":[[0,0],[2,0],-3],
                      "attributes":{"nonnegativeOnly":true}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value
        val zeroSegment = assertIs<JsxGraphSceneElement.Line>(
            zeroLength.elements.single(),
        )
        assertEquals(JsxGraphPoint2D(2.0, 0.0), zeroSegment.point1)
        assertEquals(zeroSegment.point1, zeroSegment.point2)

        val stringLength = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"string",
                      "type":"segment",
                      "parents":[[0,0],[2,0],"3"]
                    }
                    """.trimIndent(),
                ),
            ),
        ).value
        val stringSegment = assertIs<JsxGraphSceneElement.Line>(
            stringLength.elements.single(),
        )
        assertEquals(JsxGraphPoint2D(-1.0, 0.0), stringSegment.point1)
        assertEquals(JsxGraphPoint2D(2.0, 0.0), stringSegment.point2)

        val unsupportedLength =
            assertIs<JsxGraphDocumentError.ElementCreation>(
                assertError(
                    documentWithObjects(
                        """
                        {
                          "id":"unsupported",
                          "type":"segment",
                          "parents":[[0,0],[2,0],true]
                        }
                        """.trimIndent(),
                    ),
                ),
            )
        assertTrue("creatorName=segment" in unsupportedLength.reason)
        assertTrue("array, array, boolean" in unsupportedLength.reason)
    }

    @Test
    fun defaultsMatchTranslatedUpstreamOptions() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [0, 0],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "c",
                      "type": "circle",
                      "parents": ["A", 1],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val point = assertIs<JsxGraphSceneElement.Point>(scene.elements[0])
        assertEquals(JsxGraphColor(213, 94, 0), point.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), point.style.fillColor)
        assertEquals(3.0, point.size)
        val circle = assertIs<JsxGraphSceneElement.Circle>(scene.elements[1])
        assertEquals(JsxGraphColor(0, 114, 178), circle.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, circle.style.fillColor)
        assertEquals(2.0, circle.style.strokeWidth)
    }

    @Test
    fun curveDocumentsUseTranslatedFactoriesAndPreservePathBreaks() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-3, 5, 3, -1],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [0, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "visible": false
                      }
                    },
                    {
                      "id": "graph",
                      "type": "functiongraph",
                      "parents": ["A.Y() * x * x", -2, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 4
                      }
                    },
                    {
                      "id": "data",
                      "type": "curve",
                      "parents": [[-2, -1, 0], [1, 0]],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "step",
                      "type": "stepfunction",
                      "parents": [[0, 1, 3], [2, -1, 3]],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val graph = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements[1],
        )
        assertEquals(1.0, graph.style.strokeWidth)
        assertEquals("round", graph.lineCap)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, 8.0),
                JsxGraphPoint2D(-1.0, 2.0),
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(1.0, 2.0),
            ),
            graph.points,
        )

        val data = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements[2],
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, 1.0),
                JsxGraphPoint2D(-1.0, 0.0),
                null,
            ),
            data.points,
        )

        val step = assertIs<JsxGraphSceneElement.Curve>(
            scene.elements[3],
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 2.0),
                JsxGraphPoint2D(1.0, 2.0),
                JsxGraphPoint2D(1.0, -1.0),
                JsxGraphPoint2D(3.0, -1.0),
                JsxGraphPoint2D(3.0, 3.0),
            ),
            step.points,
        )
    }

    @Test
    fun derivativeDocumentsSupportContinuousAndDataCurves() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [2, 0],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "source",
                      "type": "functiongraph",
                      "parents": [
                        "A.X() * x * x + 3 * x - 1",
                        -3,
                        4
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    },
                    {
                      "id": "derivative",
                      "type": "derivative",
                      "parents": ["source"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8,
                        "strokeColor": "#D55E00"
                      }
                    },
                    {
                      "id": "data",
                      "type": "curve",
                      "parents": [
                        [-4, -1, 2, 5],
                        [-2, 2, -1, 3]
                      ],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "dataDerivative",
                      "type": "derivative",
                      "parents": ["data"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val derivative = sceneCurve(session.scene, "derivative")
        val derivativeFirst = assertIs<JsxGraphPoint2D>(
            derivative.points.first(),
        )
        val derivativeMiddle = assertIs<JsxGraphPoint2D>(
            derivative.points[4],
        )
        assertEquals(8, derivative.points.size)
        assertEquals(JsxGraphColor(213, 94, 0), derivative.style.strokeColor)
        assertEquals(-3.0, derivativeFirst.x)
        assertEquals(
            -9.0,
            derivativeFirst.y,
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            5.0,
            derivativeMiddle.y,
            absoluteTolerance = 1.0e-8,
        )

        val dataDerivative = sceneCurve(session.scene, "dataDerivative")
        assertEquals(8, dataDerivative.points.size)
        assertTrue(dataDerivative.points.take(4).all { it == null })
        val firstFiniteDataPoint = assertIs<JsxGraphPoint2D>(
            dataDerivative.points[4],
        )
        assertEquals(
            -4.0,
            firstFiniteDataPoint.x,
            absoluteTolerance = 1.0e-8,
        )
        assertEquals(
            4.0 / 3.0,
            firstFiniteDataPoint.y,
            absoluteTolerance = 1.0e-8,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(4.0, 0.0)),
        ).value
        val movedFirst = assertIs<JsxGraphPoint2D>(
            sceneCurve(moved, "derivative").points.first(),
        )
        assertEquals(
            -21.0,
            movedFirst.y,
            absoluteTolerance = 1.0e-8,
        )
    }

    @Test
    fun splineDocumentsSupportPointAndCoordinateArrayForms() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [2, 0],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [-2, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "C",
                      "type": "point",
                      "parents": [0, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "D",
                      "type": "point",
                      "parents": [4, 1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "pointSpline",
                      "type": "spline",
                      "parents": ["A", "B", "C", "D"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    },
                    {
                      "id": "arraySpline",
                      "type": "spline",
                      "parents": [[-3, -1, 2, 5], [1, 4, -2, 2]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val pointSpline = sceneCurve(session.scene, "pointSpline")
        assertEquals(8, pointSpline.points.size)
        assertEquals(
            JsxGraphPoint2D(-2.0, 2.0),
            pointSpline.points.first(),
        )
        val pointSplineMiddle = assertIs<JsxGraphPoint2D>(
            pointSpline.points[4],
        )
        assertEquals(1.0, pointSplineMiddle.x)
        assertEquals(
            -0.8,
            pointSplineMiddle.y,
            absoluteTolerance = 1.0e-14,
        )
        val arraySpline = sceneCurve(session.scene, "arraySpline")
        assertEquals(
            2.5085085085085086,
            assertIs<JsxGraphPoint2D>(arraySpline.points[3]).y,
            absoluteTolerance = 1.0e-14,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(3.0, -2.0)),
        ).value
        assertEquals(
            -2.4225352112676055,
            assertIs<JsxGraphPoint2D>(
                sceneCurve(moved, "pointSpline").points[4],
            ).y,
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun cardinalSplineDocumentsTrackPointUpdatesAndCoordinateModes() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "P0",
                      "type": "point",
                      "parents": [-4, 0],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "P1",
                      "type": "point",
                      "parents": [-2, 3],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "P2",
                      "type": "point",
                      "parents": [1, -2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "P3",
                      "type": "point",
                      "parents": [4, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "uniform",
                      "type": "cardinalspline",
                      "parents": [["P0", "P1", "P2", "P3"], 0.35, "uniform"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    },
                    {
                      "id": "coordinates",
                      "type": "cardinalspline",
                      "parents": [
                        [[-4, -2, 1, 4], [0, 3, -2, 2]],
                        0.5,
                        "uniform"
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "isArrayOfCoordinates": false,
                        "createPoints": false,
                        "points": {"visible": false},
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 8
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val uniform = sceneCurve(session.scene, "uniform")
        assertEquals(
            JsxGraphPoint2D(-0.54375, 0.45625000000000027),
            uniform.points[4],
        )
        val coordinates = sceneCurve(session.scene, "coordinates")
        assertEquals(8, coordinates.points.size)
        assertEquals(
            JsxGraphPoint2D(-0.5625, 0.4375),
            coordinates.points[4],
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("P2", JsxGraphPoint2D(2.0, -3.0)),
        ).value
        val movedUniform = sceneCurve(moved, "uniform")
        assertEquals(
            0.0,
            assertIs<JsxGraphPoint2D>(movedUniform.points[4]).x,
            absoluteTolerance = 1.0e-14,
        )
        assertEquals(
            -0.0875,
            assertIs<JsxGraphPoint2D>(movedUniform.points[4]).y,
            absoluteTolerance = 1.0e-14,
        )
    }

    @Test
    fun riemannSumDocumentsRenderClosedFilledCurves() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "single",
                      "type": "riemannsum",
                      "parents": ["x * x + 1", 3, "left", -1, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "between",
                      "type": "riemannsum",
                      "parents": [
                        ["x * 0.5", "x * x + 1"],
                        3,
                        "lower",
                        -1,
                        2
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "#336699",
                        "fillOpacity": 0.45
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val single = sceneCurve(scene, "single")
        assertEquals(15, single.points.size)
        assertEquals(JsxGraphColor(240, 228, 66), single.style.fillColor)
        assertEquals(0.3, single.style.fillOpacity)
        assertEquals(
            JsxGraphPoint2D(-1.0, 2.0),
            single.points.first(),
        )
        assertEquals(single.points.first(), single.points.last())

        val between = sceneCurve(scene, "between")
        assertEquals(15, between.points.size)
        assertEquals(JsxGraphColor(51, 102, 153), between.style.fillColor)
        assertEquals(0.45, between.style.fillOpacity)
        assertEquals(between.points.first(), between.points.last())
    }

    @Test
    fun boxPlotDocumentsRenderViewportSizedOutliersAndUpdate() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "driver",
                      "type": "point",
                      "parents": [2, 3],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "vertical",
                      "type": "boxplot",
                      "parents": [
                        [
                          "driver.Y() - 7",
                          "driver.X() - 4",
                          "0.5 * driver.X()",
                          "driver.X()",
                          "driver.Y() + 2",
                          [-6, 7]
                        ],
                        "driver.X() - 1",
                        "driver.Y() + 1"
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "smallWidth": 0.75,
                        "outlier": {"face": "plus", "size": 4}
                      }
                    },
                    {
                      "id": "horizontal",
                      "type": "boxplot",
                      "parents": [[-5, -3, -1, 2, 4, [-7, 6]], 3, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "dir": "horizontal",
                        "smallWidth": 0.25,
                        "outlier": {"face": "square", "size": 5}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val vertical = sceneCurve(session.scene, "vertical")
        assertEquals(JsxGraphColor(0, 114, 178), vertical.style.strokeColor)
        assertEquals(JsxGraphColor(0, 114, 178), vertical.style.fillColor)
        assertEquals(2.0, vertical.style.strokeWidth)
        assertEquals(0.2, vertical.style.fillOpacity)
        assertEquals(32, vertical.points.size)
        val verticalPoints = vertical.resolvePoints(
            cssPixelsPerUnitX = 40.0,
            cssPixelsPerUnitY = 30.0,
        )
        assertEquals(32, verticalPoints.size)
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, -4.0),
            assertIs(verticalPoints[0]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(0.9, -6.0),
            assertIs(verticalPoints[20]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, -6.0 - 4.0 / 30.0),
            assertIs(verticalPoints[23]),
        )

        val horizontal = sceneCurve(session.scene, "horizontal")
        val horizontalPoints = horizontal.resolvePoints(
            cssPixelsPerUnitX = 40.0,
            cssPixelsPerUnitY = 30.0,
        )
        assertEquals(32, horizontalPoints.size)
        assertPointCoordinates(
            JsxGraphPoint2D(-5.0, 3.0),
            assertIs(horizontalPoints[0]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-6.875, 3.0 - 5.0 / 30.0),
            assertIs(horizontalPoints[20]),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "driver",
                coordinates = JsxGraphPoint2D(4.0, 5.0),
            ),
        ).value
        val movedVertical = sceneCurve(moved, "vertical")
        val boxPlot = assertIs<JsxGraphBoxPlot>(movedVertical.boxPlot)
        assertEquals(listOf(-2.0, 0.0, 2.0, 4.0, 7.0), boxPlot.quantiles)
        assertEquals(3.0, boxPlot.axis)
        assertEquals(6.0, boxPlot.width)
    }

    @Test
    fun combDocumentsRenderOfficialDefaultsAndHiddenEndpointAttributes() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 8, 8, -8],
                  "objects": [
                    {
                      "id": "comb",
                      "type": "comb",
                      "parents": [[-1, 0], [1, 0]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "point1": {
                          "id": "helper1",
                          "name": "",
                          "visible": false,
                          "withLabel": false,
                          "fixed": true
                        },
                        "point2": {
                          "id": "helper2",
                          "name": "",
                          "visible": false,
                          "withLabel": false
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(1, scene.elements.size)
        val comb = sceneCurve(scene, "comb")
        assertEquals(33, comb.points.size)
        assertEquals(JsxGraphColor(0, 0, 255), comb.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, comb.style.fillColor)
        assertEquals(1.0, comb.style.strokeWidth)
        assertEquals(JsxGraphPoint2D(-1.0, 0.0), comb.points[0])
        assertPointCoordinates(
            JsxGraphPoint2D(-0.7690598923241496, 0.4),
            assertIs(comb.points[1]),
        )
        assertEquals(null, comb.points[2])
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 3.845925372767127e-16),
            assertIs(comb.points[31]),
        )
        assertEquals(null, comb.points[32])
    }

    @Test
    fun combResourceLimitsPreflightAndRollbackEndpointInteraction() {
        val staticLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"comb",
                      "type":"comb",
                      "parents":[[-2,0],[2,0]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "frequency":0.5
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 23),
            ),
        )
        assertEquals("comb", staticLimit.id)
        assertEquals(24, staticLimit.actual)

        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                source = documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"comb",
                      "type":"comb",
                      "parents":["A","B"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "frequency":1,
                        "width":1,
                        "angle":1.5707963267948966
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 9),
            ),
        ).value
        assertEquals(6, sceneCurve(session.scene, "comb").points.size)

        val update = assertIs<
            GMResult.Err<JsxGraphInteractionError.SceneUpdate>,
            >(
            session.movePoint("B", JsxGraphPoint2D(4.0, 0.0)),
        ).error
        val pointLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(update.error)
        assertEquals("comb", pointLimit.id)
        assertEquals(12, pointLimit.actual)
        assertEquals(
            JsxGraphPoint2D(2.0, 0.0),
            scenePoint(session.scene, "B").coordinates,
        )
        assertEquals(6, sceneCurve(session.scene, "comb").points.size)

        val visibleHelper = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"comb",
                      "type":"comb",
                      "parents":[[-1,0],[1,0]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "point1":{"visible":true}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("point1.visible", visibleHelper.attribute)
    }

    @Test
    fun inequalityDocumentsUseBoardBoundsAndOfficialDefaults() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [-3, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [3, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "line",
                      "type": "line",
                      "parents": ["A", "B"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "inequality",
                      "type": "inequality",
                      "parents": ["line"],
                      "attributes": {"name": "", "withLabel": false}
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val inequality = sceneCurve(scene, "inequality")
        assertEquals(5, inequality.points.size)
        assertEquals(
            JsxGraphColor.Transparent,
            inequality.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            inequality.style.fillColor,
        )
        assertEquals(0.2, inequality.style.fillOpacity)
        assertPointCoordinates(
            JsxGraphPoint2D(
                21.266252583997982,
                11.133126291998991,
            ),
            assertIs(inequality.points[0]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                5.166563145999497,
                43.33250516799596,
            ),
            assertIs(inequality.points[1]),
        )
    }

    @Test
    fun functionGraphInequalityPreservesBreaksAndResourceLimits() {
        val source = """
            {
              "boundingBox": [-8, 6, 8, -6],
              "objects": [
                {
                  "id": "function",
                  "type": "functiongraph",
                  "parents": [
                    "x == 0 ? 0 / 0 : x * x - 2",
                    -3,
                    3
                  ],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "doAdvancedPlot": false,
                    "numberPointsHigh": 8
                  }
                },
                {
                  "id": "inequality",
                  "type": "inequality",
                  "parents": ["function"],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "inverse": true
                  }
                }
              ]
            }
        """.trimIndent()
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(source),
        ).value
        val inequality = sceneCurve(scene, "inequality")
        assertEquals(18, inequality.points.size)
        assertEquals(null, inequality.points[9])
        assertPointCoordinates(
            JsxGraphPoint2D(-3.0, 19.0),
            assertIs(inequality.points[0]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(0.75, 19.0),
            assertIs(inequality.points[10]),
        )

        val pointLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 17),
            ),
        )
        assertEquals("inequality", pointLimit.id)
        assertEquals(18, pointLimit.actual)
    }

    @Test
    fun inequalityDocumentRejectsInvalidSourceAndInverse() {
        val invalidSource = assertIs<
            JsxGraphDocumentError.ElementCreation,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [0, 0],
                      "attributes": {"name": "", "withLabel": false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id": "inequality",
                      "type": "inequality",
                      "parents": ["A"],
                      "attributes": {"name": "", "withLabel": false}
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("inequality", invalidSource.id)
        assertTrue("InvalidInequalitySource" in invalidSource.reason)

        val invalidInverse = assertIs<
            JsxGraphDocumentError.ElementCreation,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id": "line",
                      "type": "line",
                      "parents": [[0, 0], [1, 1]],
                      "attributes": {"name": "", "withLabel": false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id": "inequality",
                      "type": "inequality",
                      "parents": ["line"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "inverse": 1
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("inequality", invalidInverse.id)
        assertTrue("InvalidAttributeType" in invalidInverse.reason)
    }

    @Test
    fun vectorFieldDocumentRendersStringComponentsAndOfficialStyle() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "field",
                      "type": "vectorfield",
                      "parents": [
                        ["y", "-x"],
                        [-1, 1, 1],
                        [-1, 1, 1]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "arrowHead": {"enabled": false}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val field = sceneCurve(scene, "field")
        assertEquals(12, field.points.size)
        assertEquals(JsxGraphColor(0, 114, 178), field.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, field.style.fillColor)
        assertEquals(0.5, field.style.strokeWidth)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-1.0, -1.0),
                JsxGraphPoint2D(-2.0, 0.0),
                null,
                JsxGraphPoint2D(-1.0, 1.0),
                JsxGraphPoint2D(0.0, 2.0),
                null,
                JsxGraphPoint2D(1.0, -1.0),
                JsxGraphPoint2D(0.0, -2.0),
                null,
                JsxGraphPoint2D(1.0, 1.0),
                JsxGraphPoint2D(2.0, 0.0),
                null,
            ),
            field.points,
        )
    }

    @Test
    fun vectorFieldSceneResolvesArrowSizeFromRendererCssPixels() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-2, 2, 2, -2],
                  "objects": [
                    {
                      "id": "field",
                      "type": "vectorfield",
                      "parents": [
                        ["1", "0"],
                        [0, 0, 0],
                        [0, 0, 0]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "arrowHead": {
                          "enabled": true,
                          "size": 8,
                          "angle": 0
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val field = sceneCurve(scene, "field")
        val vectorField = assertIs<JsxGraphVectorField>(field.vectorField)
        assertEquals(
            listOf(
                JsxGraphVectorFieldVector(
                    start = JsxGraphPoint2D(0.0, 0.0),
                    end = JsxGraphPoint2D(1.0, 0.0),
                ),
            ),
            vectorField.vectors,
        )
        val fortyPixelsPerUnit = field.resolvePoints(
            cssPixelsPerUnitX = 40.0,
            cssPixelsPerUnitY = 20.0,
        )
        val eightyPixelsPerUnit = field.resolvePoints(
            cssPixelsPerUnitX = 80.0,
            cssPixelsPerUnitY = 20.0,
        )

        assertEquals(7, fortyPixelsPerUnit.size)
        assertEquals(8.0, (1.0 - assertIs<JsxGraphPoint2D>(
            fortyPixelsPerUnit[3],
        ).x) * 40.0, absoluteTolerance = 1.0e-12)
        assertEquals(8.0, (1.0 - assertIs<JsxGraphPoint2D>(
            eightyPixelsPerUnit[3],
        ).x) * 80.0, absoluteTolerance = 1.0e-12)
        assertEquals(null, fortyPixelsPerUnit[2])
        assertEquals(null, fortyPixelsPerUnit[6])
    }

    @Test
    fun vectorFieldDocumentPreflightsPointLimitAndNestedAttributes() {
        val source = documentWithObjects(
            """
            {
              "id":"field",
              "type":"vectorfield",
              "parents":[["y","-x"],[-2,2,2],[-1,1,1]],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
        )
        val limit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 41),
            ),
        )
        assertEquals("field", limit.id)
        assertEquals(42, limit.actual)

        val invalidArrowHead = assertIs<
            JsxGraphDocumentError.UnsupportedAttribute,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"field",
                      "type":"vectorfield",
                      "parents":[["y","-x"],[-1,1,1],[-1,1,1]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "arrowHead":{"type":2}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("arrowhead.type", invalidArrowHead.attribute)
    }

    @Test
    fun slopeFieldDocumentUsesScalarNormalizationAndDefaultStyle() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "field",
                      "type": "slopefield",
                      "parents": [
                        "x - y",
                        [-2, 2, 2],
                        [-1, 1, 1]
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val field = sceneCurve(scene, "field")
        assertEquals(18, field.points.size)
        assertEquals(JsxGraphColor(0, 114, 178), field.style.strokeColor)
        assertEquals(JsxGraphColor.Transparent, field.style.fillColor)
        assertEquals(0.5, field.style.strokeWidth)
        assertEquals(
            listOf(
                JsxGraphPoint2D(-2.0, -1.0),
                JsxGraphPoint2D(
                    -1.2928932188134525,
                    -1.7071067811865475,
                ),
                null,
            ),
            field.points.take(3),
        )
        val vectorField = assertIs<JsxGraphVectorField>(field.vectorField)
        assertFalse(vectorField.arrowEnabled)
        assertEquals(6, vectorField.vectors.size)
    }

    @Test
    fun slopeFieldDocumentPreflightsPointLimitAndNestedAttributes() {
        val source = documentWithObjects(
            """
            {
              "id":"field",
              "type":"slopefield",
              "parents":["x-y",[-2,2,2],[-1,1,1]],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
        )
        val limit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 17),
            ),
        )
        assertEquals("field", limit.id)
        assertEquals(18, limit.actual)

        val invalidArrowHead = assertIs<
            JsxGraphDocumentError.UnsupportedAttribute,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"field",
                      "type":"slopefield",
                      "parents":["x-y",[-1,1,1],[-1,1,1]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "arrowHead":{"type":2}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("arrowhead.type", invalidArrowHead.attribute)
    }

    @Test
    fun ellipseDocumentRendersAndTracksMovedFoci() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"F1",
                      "type":"point",
                      "parents":[-3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"F2",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"ellipse",
                      "type":"ellipse",
                      "parents":["F1","F2",10],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "doAdvancedPlot":false,
                        "numberPointsHigh":32,
                        "strokeColor":"#16877A",
                        "foci":{"visible":false},
                        "center":{"visible":false}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf("F1", "F2", "ellipse"),
            session.scene.elements.map { it.id },
        )
        val initial = sceneCurve(session.scene, "ellipse")
        assertEquals(32, initial.points.size)
        assertEquals(JsxGraphColor(22, 135, 122), initial.style.strokeColor)
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, 0.0),
            assertIs(initial.points.first()),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("F1", JsxGraphPoint2D(-4.0, 0.0)),
        ).value
        val movedEllipse = sceneCurve(moved, "ellipse")
        assertPointCoordinates(
            JsxGraphPoint2D(4.5, 0.0),
            assertIs(movedEllipse.points.first()),
        )
    }

    @Test
    fun ellipseDocumentPreflightsCurveLimitsAndNestedAttributes() {
        val source = documentWithObjects(
            """
            {
              "id":"ellipse",
              "type":"ellipse",
              "parents":[[-3,0],[3,0],10],
              "attributes":{
                "name":"",
                "withLabel":false,
                "doAdvancedPlot":false,
                "numberPointsHigh":32
              }
            }
            """.trimIndent(),
        )
        val limit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 31),
            ),
        )
        assertEquals("ellipse", limit.id)
        assertEquals(32, limit.actual)

        val visibleFocus = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"ellipse",
                      "type":"ellipse",
                      "parents":[[-3,0],[3,0],10],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "doAdvancedPlot":false,
                        "numberPointsHigh":8,
                        "foci":{"visible":true}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("foci.visible", visibleFocus.attribute)
    }

    @Test
    fun hyperbolaDocumentRendersAndTracksMovedFoci() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"F1",
                      "type":"point",
                      "parents":[-3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"F2",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"hyperbola",
                      "type":"hyperbola",
                      "parents":["F1","F2",4],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "doAdvancedPlot":false,
                        "numberPointsHigh":32,
                        "strokeColor":"#16877A",
                        "foci":{"visible":false},
                        "center":{"visible":false}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf("F1", "F2", "hyperbola"),
            session.scene.elements.map { it.id },
        )
        val initial = sceneCurve(session.scene, "hyperbola")
        assertEquals(32, initial.points.size)
        assertEquals(JsxGraphColor(22, 135, 122), initial.style.strokeColor)
        assertPointCoordinates(
            JsxGraphPoint2D(2.0000004934802877, -0.0015707965335031702),
            assertIs(initial.points.first()),
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("F1", JsxGraphPoint2D(-4.0, 0.0)),
        ).value
        val movedHyperbola = sceneCurve(moved, "hyperbola")
        assertPointCoordinates(
            JsxGraphPoint2D(1.500000361885533, -0.0017278761300086334),
            assertIs(movedHyperbola.points.first()),
        )
    }

    @Test
    fun hyperbolaDocumentPreflightsCurveLimitsAndNestedAttributes() {
        val source = documentWithObjects(
            """
            {
              "id":"hyperbola",
              "type":"hyperbola",
              "parents":[[-3,0],[3,0],4],
              "attributes":{
                "name":"",
                "withLabel":false,
                "doAdvancedPlot":false,
                "numberPointsHigh":32
              }
            }
            """.trimIndent(),
        )
        val limit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = source,
                limits = JsxGraphEngineLimits(maxCurvePoints = 31),
            ),
        )
        assertEquals("hyperbola", limit.id)
        assertEquals(32, limit.actual)

        val visibleFocus = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"hyperbola",
                      "type":"hyperbola",
                      "parents":[[-3,0],[3,0],4],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "doAdvancedPlot":false,
                        "numberPointsHigh":8,
                        "foci":{"visible":true}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("foci.visible", visibleFocus.attribute)
    }

    @Test
    fun polygonDocumentsPreserveFillBordersAndImplicitVertices() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-1, 4, 5, -1],
                  "objects": [
                    {
                      "id": "triangle",
                      "type": "polygon",
                      "parents": [[0, 0], [4, 0], [0, 3]],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeWidth": 7,
                        "strokeOpacity": 0.4
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val polygon = assertIs<JsxGraphSceneElement.Polygon>(
            scene.elements.single(),
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(0.0, 0.0),
                JsxGraphPoint2D(4.0, 0.0),
                JsxGraphPoint2D(0.0, 3.0),
            ),
            polygon.vertices,
        )
        assertEquals(3, polygon.implicitVertices.size)
        assertTrue(polygon.withLines)
        assertTrue(polygon.isClosed)
        assertEquals(
            JsxGraphColor(240, 228, 66),
            polygon.style.fillColor,
        )
        assertEquals(0.3, polygon.style.fillOpacity)
        assertEquals(JsxGraphColor(0, 114, 178), polygon.style.strokeColor)
        assertEquals(0.4, polygon.style.strokeOpacity)
        assertEquals(7.0, polygon.style.strokeWidth)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            polygon.borderStyle.strokeColor,
        )
        assertEquals(1.0, polygon.borderStyle.strokeOpacity)
        assertEquals(1.0, polygon.borderStyle.strokeWidth)
        assertTrue(
            polygon.implicitVertices.all {
                it.style.fillColor == JsxGraphColor(213, 94, 0) &&
                    it.size == 3.0
            },
        )
    }

    @Test
    fun polygonalChainDocumentsPreserveAllVerticesAndOpenBorders() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-6, 5, 6, -5],
                  "objects": [
                    {
                      "id": "chain",
                      "type": "polygonalchain",
                      "parents": [[-5, 1], [-2, 4], [1, 2], [4, -3]],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val chain = assertIs<JsxGraphSceneElement.Polygon>(
            scene.elements.single(),
        )
        assertEquals(
            listOf(
                JsxGraphPoint2D(-5.0, 1.0),
                JsxGraphPoint2D(-2.0, 4.0),
                JsxGraphPoint2D(1.0, 2.0),
                JsxGraphPoint2D(4.0, -3.0),
            ),
            chain.vertices,
        )
        assertEquals(4, chain.implicitVertices.size)
        assertTrue(chain.withLines)
        assertFalse(chain.isClosed)
        assertEquals(JsxGraphColor.Transparent, chain.style.fillColor)
    }

    @Test
    fun parallelogramDocumentsRenderNestedHelperAndTrackParentUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [-4, -1],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [2, 3],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "C",
                      "type": "point",
                      "parents": [3, -3],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "P",
                      "type": "parallelogram",
                      "parents": ["A", "B", "C"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "#F0E442",
                        "parallelPoint": {
                          "id": "helper",
                          "name": "",
                          "withLabel": false,
                          "size": 6,
                          "strokeColor": "#7B4EA3",
                          "fillColor": "#7B4EA3",
                          "fixed": true
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val initial = assertIs<JsxGraphSceneElement.Polygon>(
            session.scene.elements.single { element -> element.id == "P" },
        )
        val initialHelper = initial.implicitVertices.single()

        assertEquals(
            listOf(
                JsxGraphPoint2D(-4.0, -1.0),
                JsxGraphPoint2D(2.0, 3.0),
                JsxGraphPoint2D(9.0, 1.0),
                JsxGraphPoint2D(3.0, -3.0),
            ),
            initial.vertices,
        )
        assertEquals("helper", initialHelper.id)
        assertEquals(JsxGraphPoint2D(9.0, 1.0), initialHelper.coordinates)
        assertEquals(6.0, initialHelper.size)
        assertEquals(
            JsxGraphColor(123, 78, 163),
            initialHelper.style.fillColor,
        )
        assertTrue(initialHelper.draggable)
        assertTrue(initial.isClosed)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(-2.0, 2.0),
            ),
        ).value
        val movedPolygon = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { element -> element.id == "P" },
        )
        assertEquals(
            JsxGraphPoint2D(7.0, -2.0),
            movedPolygon.implicitVertices.single().coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(7.0, -2.0),
            movedPolygon.vertices[2],
        )
    }

    @Test
    fun radicalAxisDocumentsRenderAndTrackCircleUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "center1",
                      "type": "point",
                      "parents": [-3, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "radius1",
                      "type": "point",
                      "parents": [-1, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "circle1",
                      "type": "circle",
                      "parents": ["center1", "radius1"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "center2",
                      "type": "point",
                      "parents": [2, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "radius2",
                      "type": "point",
                      "parents": [5, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "circle2",
                      "type": "circle",
                      "parents": ["center2", "radius2"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "axis",
                      "type": "radicalaxis",
                      "parents": ["circle1", "circle2"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": true,
                        "strokeColor": "#0072B2",
                        "strokeWidth": 3,
                        "point1": {
                          "id": "axisPoint1",
                          "name": "",
                          "withLabel": false
                        },
                        "point2": {
                          "id": "axisPoint2",
                          "name": "",
                          "withLabel": false
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val initial = sceneLine(session.scene, "axis")

        assertEquals(7, session.scene.elements.size)
        assertFalse(initial.straightFirst)
        assertTrue(initial.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), initial.style.strokeColor)
        assertEquals(3.0, initial.style.strokeWidth)
        assertPointCoordinates(
            JsxGraphPoint2D(
                1.6029411764705883,
                -3.8382352941176476,
            ),
            initial.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.5441176470588236,
                -2.073529411764706,
            ),
            initial.point2,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center1",
                JsxGraphPoint2D(-4.0, 1.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius1",
                JsxGraphPoint2D(-1.0, 1.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center2",
                JsxGraphPoint2D(1.0, -2.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius2",
                JsxGraphPoint2D(3.0, -2.0),
            ),
        ).value
        val movedAxis = sceneLine(moved, "axis")

        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.6323529411764706,
                -3.2205882352941178,
            ),
            movedAxis.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -1.5735294117647058,
                -1.4558823529411766,
            ),
            movedAxis.point2,
        )
    }

    @Test
    fun radicalAxisDocumentsRejectInvalidParentsAndNestedIdCollisions() {
        val invalidParent = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[1,0]},
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"axis",
                      "type":"radicalaxis",
                      "parents":["circle","A"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("axis", invalidParent.id)
        assertEquals("radicalaxis", invalidParent.type)
        assertTrue("UnsupportedParents" in invalidParent.reason)

        val nestedCollision = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[1,0]},
                    {"id":"C","type":"point","parents":[3,0]},
                    {"id":"D","type":"point","parents":[5,0]},
                    {
                      "id":"circle1",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"circle2",
                      "type":"circle",
                      "parents":["C","D"]
                    },
                    {
                      "id":"axis",
                      "type":"radicalaxis",
                      "parents":["circle1","circle2"],
                      "attributes": {
                        "point1": {"id":"temporary"},
                        "point2": {"id":"A"}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("axis", nestedCollision.id)
        assertTrue("RadicalAxisFactory" in nestedCollision.reason)
        assertTrue("DuplicateElementId" in nestedCollision.reason)
    }

    @Test
    fun polePointDocumentsRenderAndTrackCircleLineUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "center",
                      "type": "point",
                      "parents": [1, 1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "radius",
                      "type": "point",
                      "parents": [3, 1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "circle",
                      "type": "circle",
                      "parents": ["center", "radius"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "linePoint1",
                      "type": "point",
                      "parents": [-1, 4],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "linePoint2",
                      "type": "point",
                      "parents": [4, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "line",
                      "type": "line",
                      "parents": ["linePoint1", "linePoint2"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "pole",
                      "type": "polepoint",
                      "parents": ["line", "circle"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "size": 7,
                        "fillColor": "#D9553F"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val initial = scenePoint(session.scene, "pole")

        assertEquals(7, session.scene.elements.size)
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, 5.0),
            initial.coordinates,
        )
        assertEquals(7.0, initial.size)
        assertEquals(JsxGraphColor(217, 85, 63), initial.style.fillColor)
        assertFalse(initial.draggable)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center",
                JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius",
                JsxGraphPoint2D(1.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "linePoint1",
                JsxGraphPoint2D(-3.0, -1.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "linePoint2",
                JsxGraphPoint2D(2.0, 4.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                2.499999999999999,
                -2.499999999999999,
            ),
            scenePoint(moved, "pole").coordinates,
        )
    }

    @Test
    fun polePointDocumentsRejectInvalidParentsAndIdCollisions() {
        val invalidParent = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"pole",
                      "type":"polepoint",
                      "parents":["circle","A"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("pole", invalidParent.id)
        assertEquals("polepoint", invalidParent.type)
        assertTrue("UnsupportedParents" in invalidParent.reason)

        val collision = assertIs<
            GMResult.Err<JsxGraphDocumentError.DuplicateObjectId>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {"id":"C","type":"point","parents":[-1,-1]},
                    {"id":"D","type":"point","parents":[1,1]},
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"line",
                      "type":"line",
                      "parents":["C","D"]
                    },
                    {
                      "id":"A",
                      "type":"polepoint",
                      "parents":["circle","line"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("A", collision.id)
    }

    @Test
    fun tangentPolarDocumentsRenderAndTrackCirclePointUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "center",
                      "type": "point",
                      "parents": [1, 1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "radius",
                      "type": "point",
                      "parents": [4, 1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "circle",
                      "type": "circle",
                      "parents": ["center", "radius"],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "P",
                      "type": "point",
                      "parents": [5, 4],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "tangent",
                      "type": "tangent",
                      "parents": ["circle", "P"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": true,
                        "strokeColor": "#0072B2",
                        "point1": {
                          "id": "tangentPoint1",
                          "name": "",
                          "withLabel": false
                        },
                        "point2": {
                          "id": "tangentPoint2",
                          "name": "",
                          "withLabel": false
                        }
                      }
                    },
                    {
                      "id": "polar",
                      "type": "polar",
                      "parents": ["P", "circle"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#D55E00",
                        "point1": {
                          "id": "polarPoint1",
                          "name": "",
                          "withLabel": false
                        },
                        "point2": {
                          "id": "polarPoint2",
                          "name": "",
                          "withLabel": false
                        }
                      }
                    },
                    {
                      "id": "polarLine",
                      "type": "polarline",
                      "parents": ["P", "circle"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#009E73",
                        "point1": {
                          "id": "polarLinePoint1",
                          "name": "",
                          "withLabel": false
                        },
                        "point2": {
                          "id": "polarLinePoint2",
                          "name": "",
                          "withLabel": false
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(7, session.scene.elements.size)
        val tangent = sceneLine(session.scene, "tangent")
        val polar = sceneLine(session.scene, "polar")
        val polarLine = sceneLine(session.scene, "polarLine")
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), tangent.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), polar.style.strokeColor)
        assertEquals(JsxGraphColor(0, 158, 115), polarLine.style.strokeColor)
        for (line in listOf(tangent, polar, polarLine)) {
            assertPointCoordinates(
                JsxGraphPoint2D(2.8, 1.6),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(2.68, 1.76),
                line.point2,
            )
        }

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center",
                JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius",
                JsxGraphPoint2D(2.0, 2.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "P",
                JsxGraphPoint2D(2.0, -2.0),
            ),
        ).value
        for (id in listOf("tangent", "polar", "polarLine")) {
            val line = sceneLine(moved, id)
            assertPointCoordinates(
                JsxGraphPoint2D(-0.25, -0.25),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(-0.125, -0.125),
                line.point2,
            )
        }
    }

    @Test
    fun tangentPolarDocumentsSupportLineParentsWithoutImplicitPoints() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [-3, -1],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [4, 2],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "source",
                      "type": "line",
                      "parents": ["A", "B"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#666666"
                      }
                    },
                    {
                      "id": "P",
                      "type": "point",
                      "parents": [1, 5],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "tangent",
                      "type": "tangent",
                      "parents": ["source", "P"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": true,
                        "strokeColor": "#0072B2",
                        "point1": {"id": "A", "name": "ignored"},
                        "point2": {"id": "B", "name": "ignored"}
                      }
                    },
                    {
                      "id": "polar",
                      "type": "polar",
                      "parents": ["P", "source"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#D55E00"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(6, session.scene.elements.size)
        val source = sceneLine(session.scene, "source")
        val tangent = sceneLine(session.scene, "tangent")
        val polar = sceneLine(session.scene, "polar")
        assertPointCoordinates(source.point1, tangent.point1)
        assertPointCoordinates(source.point2, tangent.point2)
        assertPointCoordinates(source.point1, polar.point1)
        assertPointCoordinates(source.point2, polar.point2)
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(0, 114, 178), tangent.style.strokeColor)
        assertEquals(JsxGraphColor(213, 94, 0), polar.style.strokeColor)

        val afterParameterMove = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "P",
                JsxGraphPoint2D(7.0, 1.0),
            ),
        ).value
        assertPointCoordinates(
            source.point1,
            sceneLine(afterParameterMove, "tangent").point1,
        )
        assertPointCoordinates(
            source.point2,
            sceneLine(afterParameterMove, "tangent").point2,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "A",
                JsxGraphPoint2D(-5.0, 3.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "B",
                JsxGraphPoint2D(2.0, -4.0),
            ),
        ).value
        for (id in listOf("source", "tangent", "polar")) {
            val line = sceneLine(moved, id)
            assertPointCoordinates(
                JsxGraphPoint2D(-5.0, 3.0),
                line.point1,
            )
            assertPointCoordinates(
                JsxGraphPoint2D(2.0, -4.0),
                line.point2,
            )
        }
    }

    @Test
    fun tangentPolarDocumentsSupportCurveParentsAndDynamicUpdates() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {
                      "id": "functionCurve",
                      "type": "functiongraph",
                      "parents": ["x * x - 1", -4, 4],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 64
                      }
                    },
                    {
                      "id": "functionPoint",
                      "type": "point",
                      "parents": [2, 5],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "functionTangent",
                      "type": "tangent",
                      "parents": ["functionCurve", "functionPoint"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": true,
                        "strokeColor": "#0072B2",
                        "point1": {
                          "id": "functionTangentPoint1",
                          "name": ""
                        },
                        "point2": {
                          "id": "functionTangentPoint2",
                          "name": ""
                        }
                      }
                    },
                    {
                      "id": "functionPolar",
                      "type": "polar",
                      "parents": ["functionPoint", "functionCurve"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#D55E00",
                        "point1": {
                          "id": "functionPolarPoint1",
                          "name": ""
                        },
                        "point2": {
                          "id": "functionPolarPoint2",
                          "name": ""
                        }
                      }
                    },
                    {
                      "id": "parametricCurve",
                      "type": "curve",
                      "parents": [
                        "2 * cos(x)",
                        "sin(x)",
                        0,
                        6.283185307179586
                      ],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "doAdvancedPlot": false,
                        "numberPointsHigh": 64
                      }
                    },
                    {
                      "id": "parametricPoint",
                      "type": "point",
                      "parents": [3, 0.75],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "parametricTangent",
                      "type": "tangent",
                      "parents": ["parametricPoint", "parametricCurve"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#009E73",
                        "point1": {
                          "id": "parametricTangentPoint1",
                          "name": ""
                        },
                        "point2": {
                          "id": "parametricTangentPoint2",
                          "name": ""
                        }
                      }
                    },
                    {
                      "id": "plotCurve",
                      "type": "curve",
                      "parents": [
                        [-4, -1, 2, 5],
                        [-2, 2, -1, 3]
                      ],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "plotPoint",
                      "type": "point",
                      "parents": [0.25, 2.5],
                      "attributes": {"name": "", "withLabel": false}
                    },
                    {
                      "id": "plotTangent",
                      "type": "tangent",
                      "parents": ["plotCurve", "plotPoint"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "straightFirst": false,
                        "straightLast": false,
                        "strokeColor": "#CC79A7",
                        "point1": {
                          "id": "plotTangentPoint1",
                          "name": ""
                        },
                        "point2": {
                          "id": "plotTangentPoint2",
                          "name": ""
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        assertEquals(10, session.scene.elements.size)
        val functionTangent = sceneLine(
            session.scene,
            "functionTangent",
        )
        val functionPolar = sceneLine(session.scene, "functionPolar")
        val parametricTangent = sceneLine(
            session.scene,
            "parametricTangent",
        )
        val plotTangent = sceneLine(session.scene, "plotTangent")
        assertFalse(functionTangent.straightFirst)
        assertTrue(functionTangent.straightLast)
        assertFalse(plotTangent.straightFirst)
        assertFalse(plotTangent.straightLast)
        assertEquals(
            JsxGraphColor(0, 114, 178),
            functionTangent.style.strokeColor,
        )
        assertEquals(
            JsxGraphColor(213, 94, 0),
            functionPolar.style.strokeColor,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.5882352941184178,
                -0.6470588235263288,
            ),
            functionTangent.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                3.1094038429387445,
                1.1337477941451766,
            ),
            parametricTangent.point1,
            tolerance = 1.0e-9,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                0.16666666666666666,
                0.8333333333333334,
            ),
            plotTangent.point1,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "functionPoint",
                JsxGraphPoint2D(-1.0, 4.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "parametricPoint",
                JsxGraphPoint2D(-2.5, -1.25),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "plotPoint",
                JsxGraphPoint2D(4.0, 4.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(
                0.40000000000040004,
                1.1999999999992,
            ),
            sceneLine(moved, "functionTangent").point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(
                -2.505232495502133,
                -1.2464977648448397,
            ),
            sceneLine(moved, "parametricTangent").point1,
            tolerance = 1.0e-9,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.52, -1.64),
            sceneLine(moved, "plotTangent").point1,
        )
    }

    @Test
    fun tangentPolarDocumentsRejectInvalidParentsAndNestedIdCollisions() {
        val invalidParent = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"invalid",
                      "type":"tangent",
                      "parents":["circle","circle"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("invalid", invalidParent.id)
        assertEquals("tangent", invalidParent.type)
        assertTrue("UnsupportedParents" in invalidParent.reason)

        val polarLineWithLine = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {"id":"P","type":"point","parents":[3,1]},
                    {
                      "id":"source",
                      "type":"line",
                      "parents":["A","B"]
                    },
                    {
                      "id":"invalid",
                      "type":"polarline",
                      "parents":["source","P"]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("invalid", polarLineWithLine.id)
        assertEquals("polarline", polarLineWithLine.type)
        assertTrue("UnsupportedParents" in polarLineWithLine.reason)

        val nestedCollision = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {"id":"P","type":"point","parents":[3,1]},
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["A","B"]
                    },
                    {
                      "id":"line",
                      "type":"polarline",
                      "parents":["P","circle"],
                      "attributes": {
                        "point1": {"id":"temporary"},
                        "point2": {"id":"A"}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("line", nestedCollision.id)
        assertEquals("polarline", nestedCollision.type)
        assertTrue("TangentFactory" in nestedCollision.reason)
        assertTrue("DuplicateElementId" in nestedCollision.reason)
    }

    @Test
    fun tangentToDocumentExpandsStyledSceneAndTracksPointUpdates() {
        val sessionResult = JsxGraphEngine.createSession(
            documentWithObjects(
                """
                {
                  "id":"center",
                  "type":"point",
                  "parents":[1,1],
                  "attributes":{"name":"","withLabel":false}
                }
                """.trimIndent(),
                """
                {
                  "id":"radius",
                  "type":"point",
                  "parents":[4,1],
                  "attributes":{"name":"","withLabel":false}
                }
                """.trimIndent(),
                """
                {
                  "id":"circle",
                  "type":"circle",
                  "parents":["center","radius"],
                  "attributes":{"name":"","withLabel":false}
                }
                """.trimIndent(),
                """
                {
                  "id":"source",
                  "type":"point",
                  "parents":[5,4],
                  "attributes":{"name":"","withLabel":false}
                }
                """.trimIndent(),
                """
                {
                  "id":"tangent",
                  "type":"tangentto",
                  "parents":["circle","source",1],
                  "attributes":{
                    "name":"",
                    "withLabel":false,
                    "strokeColor":"#204060",
                    "strokeWidth":4,
                    "straightFirst":false,
                    "straightLast":true,
                    "point1":{
                      "id":"tangentPoint1",
                      "name":"",
                      "withLabel":false
                    },
                    "point2":{
                      "id":"tangentPoint2",
                      "name":"",
                      "withLabel":false
                    },
                    "polar":{
                      "id":"polar",
                      "name":"",
                      "withLabel":false,
                      "visible":true,
                      "strokeColor":"#A02040",
                      "strokeWidth":5,
                      "straightFirst":true,
                      "straightLast":false,
                      "point1":{
                        "id":"polarPoint1",
                        "name":"",
                        "withLabel":false
                      },
                      "point2":{
                        "id":"polarPoint2",
                        "name":"",
                        "withLabel":false
                      }
                    },
                    "point":{
                      "id":"intersection",
                      "name":"",
                      "withLabel":false,
                      "visible":true,
                      "fillColor":"#009E73",
                      "strokeColor":"#009E73",
                      "size":7
                    }
                  }
                }
                """.trimIndent(),
            ),
        )
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            sessionResult,
            sessionResult.toString(),
        ).value

        assertEquals(
            listOf(
                "center",
                "radius",
                "circle",
                "source",
                "polar",
                "intersection",
                "tangent",
            ),
            session.scene.elements.map(JsxGraphSceneElement::id),
        )
        val tangent = sceneLine(session.scene, "tangent")
        val polar = sceneLine(session.scene, "polar")
        val intersection = scenePoint(session.scene, "intersection")
        assertEquals(JsxGraphColor(32, 64, 96), tangent.style.strokeColor)
        assertEquals(4.0, tangent.style.strokeWidth)
        assertTrue(tangent.style.strokeDashPattern.isEmpty())
        assertFalse(tangent.straightFirst)
        assertTrue(tangent.straightLast)
        assertEquals(JsxGraphColor(160, 32, 64), polar.style.strokeColor)
        assertEquals(5.0, polar.style.strokeWidth)
        assertEquals(listOf(10.0, 10.0), polar.style.strokeDashPattern)
        assertTrue(polar.style.visible)
        assertTrue(polar.straightFirst)
        assertFalse(polar.straightLast)
        assertEquals(JsxGraphColor(0, 158, 115), intersection.style.fillColor)
        assertEquals(7.0, intersection.size)
        assertTrue(intersection.style.visible)
        assertFalse(intersection.draggable)
        assertPointCoordinates(
            JsxGraphPoint2D(3.88, 0.16000000000000036),
            intersection.coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.8, 1.6),
            polar.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.68, 1.76),
            polar.point2,
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "center",
                JsxGraphPoint2D(-2.0, 2.0),
            ),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "radius",
                JsxGraphPoint2D(2.0, 2.0),
            ),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                "source",
                JsxGraphPoint2D(3.0, -3.0),
            ),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(-2.73238075793812, -1.93238075793812),
            scenePoint(moved, "intersection").coordinates,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-0.6, 0.2),
            sceneLine(moved, "polar").point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-0.5, 0.3),
            sceneLine(moved, "polar").point2,
        )
    }

    @Test
    fun tangentToDocumentUsesWeightedObjectLimitBeforeBoardMutation() {
        val isolatedSource = documentWithObjects(
            """
            {
              "id":"tangent",
              "type":"tangentto",
              "parents":["missingCircle","missingPoint"]
            }
            """.trimIndent(),
        )
        assertEquals(
            JsxGraphDocumentError.ObjectLimitExceeded(
                limit = 2,
                actual = 3,
            ),
            assertError(
                source = isolatedSource,
                limits = JsxGraphEngineLimits(maxObjects = 2),
            ),
        )
        val admitted = assertIs<
            JsxGraphDocumentError.ElementCreation,
            >(
            assertError(
                source = isolatedSource,
                limits = JsxGraphEngineLimits(maxObjects = 3),
            ),
        )
        assertEquals(0, admitted.objectIndex)
        assertEquals("tangent", admitted.id)
        assertEquals("tangentto", admitted.type)

        val validSource = documentWithObjects(
            """
            {
              "id":"O",
              "type":"point",
              "parents":[0,0],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"R",
              "type":"point",
              "parents":[3,0],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"C",
              "type":"circle",
              "parents":["O","R"]
            }
            """.trimIndent(),
            """
            {
              "id":"P",
              "type":"point",
              "parents":[5,1],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"T",
              "type":"tangentto",
              "parents":["C","P"]
            }
            """.trimIndent(),
        )
        assertEquals(
            JsxGraphDocumentError.ObjectLimitExceeded(
                limit = 6,
                actual = 7,
            ),
            assertError(
                source = validSource,
                limits = JsxGraphEngineLimits(maxObjects = 6),
            ),
        )
        val admittedValid = JsxGraphEngine.parse(
            source = validSource,
            limits = JsxGraphEngineLimits(maxObjects = 7),
        )
        assertEquals(
            7,
            assertIs<GMResult.Ok<JsxGraphScene>>(
                admittedValid,
                admittedValid.toString(),
            ).value.elements.size,
        )
    }

    @Test
    fun tangentToNestedDiagnosticsRetainTheOriginalSourceIndex() {
        val invalidStyle = assertIs<
            JsxGraphDocumentError.InvalidAttribute,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"O",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"R",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"circle",
                      "parents":["O","R"]
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"P",
                      "type":"point",
                      "parents":[5,1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"T",
                      "type":"tangentto",
                      "parents":["C","P"],
                      "attributes":{
                        "polar":{"id":"polar"},
                        "point":{
                          "id":"contact",
                          "size":"large"
                        }
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"after",
                      "type":"point",
                      "parents":[1,1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals(4, invalidStyle.objectIndex)
        assertEquals("contact", invalidStyle.id)
        assertEquals("size", invalidStyle.attribute)

        val nestedCollision = assertIs<
            JsxGraphDocumentError.ElementCreation,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"O",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"R",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"circle",
                      "parents":["O","R"]
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"P",
                      "type":"point",
                      "parents":[5,1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"T",
                      "type":"tangentto",
                      "parents":["C","P"],
                      "attributes":{
                        "point":{"id":"O"}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals(4, nestedCollision.objectIndex)
        assertEquals("T", nestedCollision.id)
        assertEquals("tangentto", nestedCollision.type)
        assertTrue("DuplicateElementId(id=O)" in nestedCollision.reason)
    }

    @Test
    fun lineDocumentsResolveEveryOfficialDashPatternAndDashScale() {
        val lineObjects = (0..7).joinToString(separator = ",") { dash ->
            val scaled =
                if (dash == 6) {
                    ""","strokeWidth":4,"dashScale":true"""
                } else {
                    ""
                }
            """
            {
              "id":"dash$dash",
              "type":"line",
              "parents":["A","B"],
              "attributes":{
                "name":"",
                "withLabel":false,
                "dash":$dash$scaled
              }
            }
            """.trimIndent()
        }
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox":[-2,2,2,-2],
                  "objects":[
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-1,0],
                      "attributes":{"name":"","withLabel":false}
                    },
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[1,0],
                      "attributes":{"name":"","withLabel":false}
                    },
                    $lineObjects
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val expected = listOf(
            emptyList(),
            listOf(2.0, 2.0),
            listOf(5.0, 5.0),
            listOf(10.0, 10.0),
            listOf(20.0, 20.0),
            listOf(20.0, 10.0, 10.0, 10.0),
            listOf(40.0, 10.0, 20.0, 10.0),
            listOf(0.0, 5.0),
        )
        for (dash in expected.indices) {
            assertEquals(
                expected[dash],
                sceneLine(scene, "dash$dash").style.strokeDashPattern,
            )
        }

        val unsupported = assertIs<
            JsxGraphDocumentError.UnsupportedAttributeValue,
            >(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"line",
                      "type":"line",
                      "parents":[[-1,0],[1,0]],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "dash":8
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertEquals("dash", unsupported.attribute)
        assertEquals("8", unsupported.value)
    }

    @Test
    fun normalDocumentsRenderAllTranslatedBranchesAndTrackParents() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[-4,-2],"attributes":{"name":"","withLabel":false}},
                    {"id":"B","type":"point","parents":[3,2],"attributes":{"name":"","withLabel":false}},
                    {"id":"sourceLine","type":"line","parents":["A","B"],"attributes":{"name":"","withLabel":false}},
                    {"id":"P","type":"point","parents":[-1,4],"attributes":{"name":"","withLabel":false}},
                    {
                      "id":"lineNormal",
                      "type":"normal",
                      "parents":["sourceLine","P"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":false,
                        "straightLast":true,
                        "point":{"id":"lineNormalPoint","name":""}
                      }
                    },
                    {"id":"center","type":"point","parents":[2,-1],"attributes":{"name":"","withLabel":false}},
                    {"id":"radius","type":"point","parents":[5,-1],"attributes":{"name":"","withLabel":false}},
                    {"id":"circle","type":"circle","parents":["center","radius"],"attributes":{"name":"","withLabel":false}},
                    {"id":"Q","type":"point","parents":[4,3],"attributes":{"name":"","withLabel":false}},
                    {"id":"circleNormal","type":"normal","parents":["Q","circle"],"attributes":{"name":"","withLabel":false}},
                    {
                      "id":"curve",
                      "type":"functiongraph",
                      "parents":["0.5 * x * x - 2",-5,5],
                      "attributes":{"name":"","withLabel":false,"doAdvancedPlot":false,"numberPointsHigh":32}
                    },
                    {"id":"R","type":"point","parents":[-2,3],"attributes":{"name":"","withLabel":false}},
                    {
                      "id":"curveNormal",
                      "type":"normal",
                      "parents":["curve","R"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "point1":{"id":"curveNormalPoint1","name":""},
                        "point2":{"id":"curveNormalPoint2","name":""}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val sourceLine = sceneLine(session.scene, "sourceLine")
        val lineNormal = sceneLine(session.scene, "lineNormal")
        val circleNormal = sceneLine(session.scene, "circleNormal")
        val curveNormal = sceneLine(session.scene, "curveNormal")
        assertFalse(lineNormal.straightFirst)
        assertTrue(lineNormal.straightLast)
        assertPerpendicularLine(
            baseLine = sourceLine,
            perpendicular = lineNormal,
            through = JsxGraphPoint2D(-1.0, 4.0),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.0, -1.0),
            circleNormal.point1,
        )
        assertPointCoordinates(
            JsxGraphPoint2D(4.0, 3.0),
            circleNormal.point2,
        )
        assertEquals(
            0.0,
            (
                curveNormal.point2.x - curveNormal.point1.x
                ) +
                (
                    curveNormal.point2.y - curveNormal.point1.y
                    ) * -2.0,
            absoluteTolerance = 1.0e-9,
        )

        val movedLine = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(2.0, -4.0)),
        ).value
        assertPerpendicularLine(
            baseLine = sceneLine(movedLine, "sourceLine"),
            perpendicular = sceneLine(movedLine, "lineNormal"),
            through = JsxGraphPoint2D(-1.0, 4.0),
        )
        val movedCircle = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("center", JsxGraphPoint2D(-2.0, 2.0)),
        ).value
        assertPointCoordinates(
            JsxGraphPoint2D(-2.0, 2.0),
            sceneLine(movedCircle, "circleNormal").point1,
        )
        val movedCurve = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("R", JsxGraphPoint2D(1.5, 4.0)),
        ).value
        assertNotEquals(
            curveNormal.point1,
            sceneLine(movedCurve, "curveNormal").point1,
        )
    }

    @Test
    fun normalDocumentsRejectCoordinatePointAndNestedIdCollision() {
        val coordinatePoint = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {"id":"source","type":"line","parents":["A","B"]},
                    {"id":"normal","type":"normal","parents":["source",[1,4]]}
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("normal", coordinatePoint.type)
        assertTrue("UnsupportedParents" in coordinatePoint.reason)

        val collision = assertIs<
            GMResult.Err<JsxGraphDocumentError.ElementCreation>,
            >(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-8, 6, 8, -6],
                  "objects": [
                    {"id":"A","type":"point","parents":[0,0]},
                    {"id":"B","type":"point","parents":[2,0]},
                    {"id":"P","type":"point","parents":[3,1]},
                    {
                      "id":"curve",
                      "type":"functiongraph",
                      "parents":["x * x",-2,2],
                      "attributes":{"doAdvancedPlot":false,"numberPointsHigh":16}
                    },
                    {
                      "id":"normal",
                      "type":"normal",
                      "parents":["curve","P"],
                      "attributes":{
                        "point1":{"id":"temporary"},
                        "point2":{"id":"A"}
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).error
        assertEquals("normal", collision.type)
        assertTrue("NormalFactory" in collision.reason)
        assertTrue("DuplicateElementId" in collision.reason)
    }

    @Test
    fun regularPolygonDocumentsRenderGeneratedVerticesAndTrackParents() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                """
                {
                  "boundingBox": [-8, 10, 8, -4],
                  "objects": [
                    {
                      "id": "A",
                      "type": "point",
                      "parents": [-3, -1],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "B",
                      "type": "point",
                      "parents": [0, 2],
                      "attributes": {
                        "name": "",
                        "withLabel": false
                      }
                    },
                    {
                      "id": "regular",
                      "type": "regularpolygon",
                      "parents": ["A", "B", 5],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "fillColor": "#F0E442",
                        "vertices": {
                          "ids": ["C", "D", "E"],
                          "name": "",
                          "withLabel": false,
                          "size": 6,
                          "strokeColor": "#7B4EA3",
                          "fillColor": "#7B4EA3",
                          "fixed": true
                        }
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value
        val initial = assertIs<JsxGraphSceneElement.Polygon>(
            session.scene.elements.single { element ->
                element.id == "regular"
            },
        )

        assertEquals(
            listOf(
                JsxGraphPoint2D(-3.0, -1.0),
                JsxGraphPoint2D(0.0, 2.0),
                JsxGraphPoint2D(
                    -1.926118565760618,
                    5.780220532010303,
                ),
                JsxGraphPoint2D(
                    -6.116525305762879,
                    5.116525305762881,
                ),
                JsxGraphPoint2D(
                    -6.7802205320103015,
                    0.9261185657606195,
                ),
            ),
            initial.vertices,
        )
        assertEquals(
            listOf("C", "D", "E"),
            initial.implicitVertices.map { point -> point.id },
        )
        assertTrue(
            initial.implicitVertices.all { point ->
                point.size == 6.0 &&
                    point.style.fillColor ==
                    JsxGraphColor(123, 78, 163) &&
                    point.draggable
            },
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(-4.0, 1.0),
            ),
        ).value
        val movedPolygon = assertIs<JsxGraphSceneElement.Polygon>(
            moved.elements.single { element ->
                element.id == "regular"
            },
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                0.2850114612046367,
                6.113243059555562,
            ),
            actual = movedPolygon.vertices[2],
        )
    }

    @Test
    fun textDocumentsPreserveContentStyleAndAnchors() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                """
                {
                  "boundingBox": [-5, 5, 5, -5],
                  "objects": [
                    {
                      "id": "title",
                      "type": "text",
                      "parents": [-2, 3, "Plain text"],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "strokeColor": "#16877A",
                        "strokeOpacity": 0.75,
                        "fontSize": 18,
                        "fontUnit": "px",
                        "anchorX": "middle",
                        "anchorY": "top",
                        "display": "internal",
                        "parse": false
                      }
                    },
                    {
                      "id": "value",
                      "type": "text",
                      "parents": [1, -2, 3.14159],
                      "attributes": {
                        "name": "",
                        "withLabel": false,
                        "formatNumber": true,
                        "digits": 2
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        ).value

        val title = assertIs<JsxGraphSceneElement.Text>(scene.elements[0])
        assertEquals(JsxGraphPoint2D(-2.0, 3.0), title.coordinates)
        assertEquals("Plain text", title.content)
        assertEquals(18.0, title.fontSize)
        assertEquals("middle", title.anchorX)
        assertEquals("top", title.anchorY)
        assertEquals(JsxGraphColor(22, 135, 122), title.style.strokeColor)
        assertEquals(0.75, title.style.strokeOpacity)

        val value = assertIs<JsxGraphSceneElement.Text>(scene.elements[1])
        assertEquals("3.14", value.content)
        assertEquals(12.0, value.fontSize)
        assertEquals("left", value.anchorX)
        assertEquals("middle", value.anchorY)
        assertEquals(JsxGraphColor(0, 0, 0), value.style.strokeColor)
    }

    @Test
    fun malformedDocumentsReturnStructuredErrors() {
        assertIs<JsxGraphDocumentError.InvalidJson>(
            assertError("{"),
        )
        assertIs<JsxGraphDocumentError.InvalidField>(
            assertError("""{"boundingBox":[0, 1, 0, -1], "objects":[]}"""),
        )
        assertIs<JsxGraphDocumentError.UnsupportedSchemaVersion>(
            assertError(
                """
                {
                  "schemaVersion": 2,
                  "boundingBox": [-1, 1, 1, -1],
                  "objects": []
                }
                """.trimIndent(),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedField>(
            assertError(
                """
                {
                  "boundingBox": [-1, 1, 1, -1],
                  "objects": [],
                  "fallback": true
                }
                """.trimIndent(),
            ),
        )
    }

    @Test
    fun factoryAndRenderingFailuresRemainExplicit() {
        assertIs<JsxGraphDocumentError.DuplicateObjectId>(
            assertError(
                documentWithObjects(
                    """{"id":"A","type":"point","parents":[0,0]}""",
                    """{"id":"A","type":"point","parents":[1,1]}""",
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """{"id":"a","type":"angle","parents":[0,0,1]}""",
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"l",
                      "type":"line",
                      "parents":["missing-a","missing-b"]
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttribute>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "shadow":true
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "face":"square"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"t",
                      "type":"text",
                      "parents":[0,0,"x_{1}"],
                      "attributes":{
                        "name":"",
                        "withLabel":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"t",
                      "type":"text",
                      "parents":[0,0,"rotated"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "parse":false,
                        "rotate":30
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"f",
                      "type":"functiongraph",
                      "parents":["x * x",-2,2],
                      "attributes":{
                        "name":"",
                        "withLabel":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
        assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(
            assertError(
                documentWithObjects(
                    """
                    {
                      "id":"c",
                      "type":"circle",
                      "parents":[[0,0],2],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "center":{"visible":true}
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        )
    }

    @Test
    fun buildsCubicArcSectorAndAngleSceneElements() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[1,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"arc",
                      "type":"arc",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "selection":"minor",
                        "fillColor":"none"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"sector",
                      "type":"sector",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fillColor":"#F0E442"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"angle",
                      "type":"angle",
                      "parents":["B","A","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "radius":1.5,
                        "orthoType":"sector"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val arc = assertIs<JsxGraphSceneElement.Curve>(scene.elements[3])
        assertEquals(3, arc.bezierDegree)
        assertEquals(13, arc.points.size)
        assertEquals(0, arc.style.fillColor.alpha)

        val sector = assertIs<JsxGraphSceneElement.Curve>(scene.elements[4])
        assertEquals(19, sector.points.size)
        assertEquals(JsxGraphPoint2D(0.0, 0.0), sector.points.first())
        assertEquals(JsxGraphPoint2D(0.0, 0.0), sector.points.last())
        assertEquals(255, sector.style.fillColor.alpha)

        val angle = assertIs<JsxGraphSceneElement.Curve>(scene.elements[5])
        assertEquals(19, angle.points.size)
        assertEquals(JsxGraphPoint2D(1.5, 0.0), angle.points[3])
        assertEquals(null, angle.autoRadiusAngle)
    }

    @Test
    fun directionPointArcBuildsAndUpdatesThroughTheDocumentSession() {
        val source = documentWithObjects(
            """
            {"id":"O","type":"point","parents":[0,0],"attributes":{"name":"","withLabel":false}}
            """.trimIndent(),
            """
            {"id":"A","type":"point","parents":[2,0],"attributes":{"name":"","withLabel":false}}
            """.trimIndent(),
            """
            {"id":"C","type":"point","parents":[0,2],"attributes":{"name":"","withLabel":false}}
            """.trimIndent(),
            """
            {"id":"D","type":"point","parents":[0,-2],"attributes":{"name":"","withLabel":false}}
            """.trimIndent(),
            """
            {
              "id":"arc",
              "type":"arc",
              "parents":["O","A","C","D"],
              "attributes":{
                "name":"",
                "withLabel":false,
                "useDirection":true,
                "fillColor":"none"
              }
            }
            """.trimIndent(),
        )
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(source),
        ).value
        val initialScene = session.scene
        val initialArc = assertIs<JsxGraphSceneElement.Curve>(
            initialScene.elements[4],
        )
        val initialFirst = assertIs<JsxGraphPoint2D>(
            initialArc.points.first(),
        )
        val initialLast = assertIs<JsxGraphPoint2D>(
            initialArc.points.last(),
        )

        assertEquals(0.0, initialFirst.x, 1.0e-12)
        assertEquals(2.0, initialFirst.y, 1.0e-12)
        assertEquals(2.0, initialLast.x, 1.0e-12)
        assertEquals(0.0, initialLast.y, 1.0e-12)

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "D",
                coordinates = JsxGraphPoint2D(0.0, 3.0),
            ),
        ).value
        val movedArc = assertIs<JsxGraphSceneElement.Curve>(
            moved.elements[4],
        )
        val movedFirst = assertIs<JsxGraphPoint2D>(
            movedArc.points.first(),
        )
        val movedLast = assertIs<JsxGraphPoint2D>(
            movedArc.points.last(),
        )
        assertEquals(0.0, movedFirst.x, 1.0e-12)
        assertEquals(2.0, movedFirst.y, 1.0e-12)
        assertEquals(2.0, movedLast.x, 1.0e-12)
        assertEquals(0.0, movedLast.y, 1.0e-12)

        val movedTwice = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "D",
                coordinates = JsxGraphPoint2D(0.0, 3.0),
            ),
        ).value
        val updatedArc = assertIs<JsxGraphSceneElement.Curve>(
            movedTwice.elements[4],
        )
        val updatedFirst = assertIs<JsxGraphPoint2D>(
            updatedArc.points.first(),
        )
        val updatedLast = assertIs<JsxGraphPoint2D>(
            updatedArc.points.last(),
        )
        assertEquals(2.0, updatedFirst.x, 1.0e-12)
        assertEquals(0.0, updatedFirst.y, 1.0e-12)
        assertEquals(0.0, updatedLast.x, 1.0e-12)
        assertEquals(2.0, updatedLast.y, 1.0e-12)

        assertEquals(
            initialScene,
            assertIs<GMResult.Ok<JsxGraphScene>>(
                session.resetInteractionState(),
            ).value,
        )
    }

    @Test
    fun arcCompositionDocumentsBuildOnlyRequestedCurvesAndUpdate() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {"id":"A","type":"point","parents":[-4,-1],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"B","type":"point","parents":[1,4],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"C","type":"point","parents":[5,-2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {
                      "id":"semicircle",
                      "type":"semicircle",
                      "parents":["A","B"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "useDirection":true,
                        "strokeColor":"#D55E00"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circumcircleArc",
                      "type":"circumcirclearc",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "useDirection":false,
                        "strokeColor":"#0072B2"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"minorArc",
                      "type":"minorarc",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "selection":"major",
                        "strokeColor":"#009E73"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"majorArc",
                      "type":"majorarc",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "selection":"minor",
                        "strokeColor":"#CC79A7"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "semicircle",
                "circumcircleArc",
                "minorArc",
                "majorArc",
            ),
            session.scene.elements.map { element -> element.id },
        )
        val semicircle = sceneCurve(session.scene, "semicircle")
        val circumcircleArc = sceneCurve(
            session.scene,
            "circumcircleArc",
        )
        val minorArc = sceneCurve(session.scene, "minorArc")
        val majorArc = sceneCurve(session.scene, "majorArc")

        assertEquals(13, semicircle.points.size)
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 4.0),
            assertIs(semicircle.points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-4.0, -1.0),
            assertIs(semicircle.points.last()),
        )
        assertEquals(JsxGraphColor(213, 94, 0), semicircle.style.strokeColor)

        assertEquals(13, circumcircleArc.points.size)
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, -2.0),
            assertIs(circumcircleArc.points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-4.0, -1.0),
            assertIs(circumcircleArc.points.last()),
        )
        assertEquals(
            JsxGraphColor(0, 114, 178),
            circumcircleArc.style.strokeColor,
        )

        assertEquals(13, minorArc.points.size)
        assertEquals(13, majorArc.points.size)
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 4.0),
            assertIs(minorArc.points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 4.0),
            assertIs(majorArc.points.first()),
        )
        val minorMiddle = assertIs<JsxGraphPoint2D>(minorArc.points[6])
        val majorMiddle = assertIs<JsxGraphPoint2D>(majorArc.points[6])
        assertTrue(minorMiddle.y > majorMiddle.y)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-6.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, 3.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(4.0, -4.0)),
        ).value

        assertPointCoordinates(
            JsxGraphPoint2D(0.0, 3.0),
            assertIs(sceneCurve(moved, "semicircle").points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-6.0, 2.0),
            assertIs(sceneCurve(moved, "semicircle").points.last()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(4.0, -4.0),
            assertIs(sceneCurve(moved, "circumcircleArc").points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-6.0, 2.0),
            assertIs(sceneCurve(moved, "circumcircleArc").points.last()),
        )
    }

    @Test
    fun coordinateArcCompositionDocumentsExcludeImplicitHelpersFromScene() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"semicircle",
                      "type":"semicircle",
                      "parents":[[-4,-1],[1,4]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circumcircleArc",
                      "type":"circumcirclearc",
                      "parents":[[-4,-1],[1,4],[5,-2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"minorArc",
                      "type":"minorarc",
                      "parents":[[0,0],[2,0],[0,2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"majorArc",
                      "type":"majorarc",
                      "parents":[[0,0],[2,0],[0,2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf(
                "semicircle",
                "circumcircleArc",
                "minorArc",
                "majorArc",
            ),
            scene.elements.map { element -> element.id },
        )
        assertTrue(
            scene.elements.all { element ->
                element is JsxGraphSceneElement.Curve
            },
        )
        assertPointCoordinates(
            JsxGraphPoint2D(1.0, 4.0),
            assertIs(sceneCurve(scene, "semicircle").points.first()),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, -2.0),
            assertIs(sceneCurve(scene, "circumcircleArc").points.first()),
        )
    }

    @Test
    fun sectorCompositionDocumentsRenderOnlyRequestedCurvesAndUpdate() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {"id":"A","type":"point","parents":[-4,-1],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"B","type":"point","parents":[1,4],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"C","type":"point","parents":[5,-2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {
                      "id":"circumcircleSector",
                      "type":"circumcirclesector",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "useDirection":false,
                        "strokeColor":"#0072B2"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"minorSector",
                      "type":"minorsector",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "selection":"major",
                        "fillColor":"#F0E442"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"majorSector",
                      "type":"majorsector",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "selection":"minor",
                        "fillColor":"#009E73"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"nonreflex",
                      "type":"nonreflexangle",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "radius":2,
                        "selection":"major",
                        "orthoType":"sector"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"reflex",
                      "type":"reflexangle",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "radius":2,
                        "selection":"minor",
                        "orthoType":"sector"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertEquals(
            listOf(
                "A",
                "B",
                "C",
                "circumcircleSector",
                "minorSector",
                "majorSector",
                "nonreflex",
                "reflex",
            ),
            session.scene.elements.map { element -> element.id },
        )
        val circumcircle = sceneCurve(
            session.scene,
            "circumcircleSector",
        )
        assertPointCoordinates(
            JsxGraphPoint2D(5.0, -2.0),
            assertIs(circumcircle.points[3]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-4.0, -1.0),
            assertIs(circumcircle.points[15]),
        )
        assertTrue(
            sceneCurve(session.scene, "minorSector").points[6] !=
                sceneCurve(session.scene, "majorSector").points[6],
            message = "Minor and major sector paths must differ",
        )

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(-6.0, 2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, 3.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(4.0, -4.0)),
        ).value
        assertPointCoordinates(
            JsxGraphPoint2D(4.0, -4.0),
            assertIs(
                sceneCurve(moved, "circumcircleSector").points[3],
            ),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(-6.0, 2.0),
            assertIs(
                sceneCurve(moved, "circumcircleSector").points[15],
            ),
        )
    }

    @Test
    fun sectorDirectionPointDocumentRendersSelectedEndpointOrder() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                """
                {"id":"O","type":"point","parents":[0,0],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {"id":"A","type":"point","parents":[2,0],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {"id":"C","type":"point","parents":[0,2],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {"id":"D","type":"point","parents":[0,-2],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {
                  "id":"sector",
                  "type":"sector",
                  "parents":["O","A","C","D"],
                  "attributes":{
                    "name":"",
                    "withLabel":false,
                    "useDirection":true
                  }
                }
                """.trimIndent(),
                ),
            ),
        ).value

        val sector = sceneCurve(scene, "sector")
        assertPointCoordinates(
            JsxGraphPoint2D(0.0, 2.0),
            assertIs(sector.points[3]),
        )
        assertPointCoordinates(
            JsxGraphPoint2D(2.0, 0.0),
            assertIs(sector.points[15]),
        )
    }

    @Test
    fun automaticAngleRadiusIsResolvedFromViewportUnitX() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {"id":"A","type":"point","parents":[6,0],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"B","type":"point","parents":[0,0],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"C","type":"point","parents":[0,6],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {
                      "id":"angle",
                      "type":"angle",
                      "parents":["A","B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "radius":"auto",
                        "orthoType":"sector"
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val angle = assertIs<JsxGraphSceneElement.Curve>(scene.elements[3])
        val autoRadius = assertIs<JsxGraphAutoRadiusAngle>(
            angle.autoRadiusAngle,
        )
        assertEquals(JsxGraphPoint2D(6.0, 0.0), autoRadius.first)
        assertEquals(JsxGraphPoint2D(0.0, 0.0), autoRadius.vertex)
        assertEquals(JsxGraphPoint2D(0.0, 6.0), autoRadius.third)
        assertEquals(1.0, autoRadius.sign)
        assertEquals(
            0.5,
            assertIs<JsxGraphPoint2D>(
                angle.resolvePoints(cssPixelsPerUnitX = 100.0)[3],
            ).x,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun intersectionDocumentCreatesBothCircleLineBranches() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"O",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["O",3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-6,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[6,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"line",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"first",
                      "type":"intersection",
                      "parents":["circle","line",0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "size":5,
                        "alwaysIntersect":true
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"other",
                      "type":"otherintersection",
                      "parents":["circle","line","first"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "size":5,
                        "precision":0.001
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val first = scenePoint(scene, "first")
        val other = scenePoint(scene, "other")
        assertEquals(JsxGraphPoint2D(3.0, 0.0), first.coordinates)
        assertEquals(JsxGraphPoint2D(-3.0, 0.0), other.coordinates)
        assertTrue(first.isReal)
        assertTrue(other.isReal)
        assertFalse(first.draggable)
        assertFalse(other.draggable)
    }

    @Test
    fun intersectionDocumentSupportsCurveAndPolygonParents() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"curve",
                      "type":"curve",
                      "parents":[
                        [-4,-2,0,2,4],
                        [3,-1,3,-1,3]
                      ],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-5,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[5,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"line",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"curveIntersection",
                      "type":"intersection",
                      "parents":["curve","line",3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"firstCurveIntersection",
                      "type":"intersection",
                      "parents":["curve","line",0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"otherCurveIntersection",
                      "type":"otherintersection",
                      "parents":[
                        "curve",
                        "line",
                        "firstCurveIntersection"
                      ],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"polygon",
                      "type":"polygon",
                      "parents":[[-2,-2],[2,-2],[2,2],[-2,2]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"polygonIntersection",
                      "type":"intersection",
                      "parents":["polygon","line",1],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val curveIntersection = scenePoint(
            scene,
            "curveIntersection",
        ).coordinates
        assertEquals(2.5, curveIntersection.x, absoluteTolerance = 1.0e-12)
        assertEquals(0.0, curveIntersection.y, absoluteTolerance = 1.0e-12)
        val otherCurveIntersection = scenePoint(
            scene,
            "otherCurveIntersection",
        ).coordinates
        assertEquals(
            -1.5,
            otherCurveIntersection.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.0,
            otherCurveIntersection.y,
            absoluteTolerance = 1.0e-12,
        )
        val polygonIntersection = scenePoint(
            scene,
            "polygonIntersection",
        ).coordinates
        assertEquals(
            -2.0,
            polygonIntersection.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.0,
            polygonIntersection.y,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun polygonPathIntersectionsRenderAndRecomputeThroughDocumentSession() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                polygonPathIntersectionDocument("2"),
            ),
        ).value

        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                2.0,
                -1.499885402306805,
            ),
            actual = scenePoint(session.scene, "polygonCircle").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                2.0,
                1.4998854023067922,
            ),
            actual = scenePoint(session.scene, "circlePolygon").coordinates,
        )
        assertPointCoordinates(
            expected = JsxGraphPoint2D(-1.0, -2.0),
            actual = scenePoint(session.scene, "polygonDiamond").coordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "C",
                coordinates = JsxGraphPoint2D(3.0, 2.0),
            ),
        ).value
        assertPointCoordinates(
            expected = JsxGraphPoint2D(
                2.2058845193854877,
                -1.176461922458049,
            ),
            actual = scenePoint(moved, "polygonCircle").coordinates,
        )
    }

    @Test
    fun fractionalPolygonPathIndexIsStructuredDocumentError() {
        val error = assertIs<JsxGraphDocumentError.ElementCreation>(
            assertError(polygonPathIntersectionDocument("0.5")),
        )

        assertEquals("polygonCircle", error.id)
        assertEquals("intersection", error.type)
        assertTrue("ClipComputation" in error.reason)
        assertTrue("InvalidIntersectionIndex" in error.reason)
    }

    @Test
    fun finiteSegmentIntersectionRemainsInSceneAsNonReal() {
        val scene = assertIs<GMResult.Ok<JsxGraphScene>>(
            JsxGraphEngine.parse(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-1,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[1,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"segment",
                      "type":"segment",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"O",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["O",3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"clipped",
                      "type":"intersection",
                      "parents":["segment","circle",0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "alwaysIntersect":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val clipped = scenePoint(scene, "clipped")
        assertFalse(clipped.isReal)
        assertFalse(clipped.draggable)
        assertTrue(clipped.coordinates.x.isNaN())
        assertTrue(clipped.coordinates.y.isNaN())
    }

    @Test
    fun sessionCommitsRealToNonRealToRealIntersectionTransitions() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-4,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fixed":true
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[4,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fixed":true
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"segment",
                      "type":"segment",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"center",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"circle",
                      "type":"circle",
                      "parents":["center",2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"intersection",
                      "type":"intersection",
                      "parents":["circle","segment",0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "alwaysIntersect":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val initial = scenePoint(session.scene, "intersection")
        assertTrue(initial.isReal)
        assertEquals(JsxGraphPoint2D(2.0, 0.0), initial.coordinates)

        val nonReal = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "center",
                coordinates = JsxGraphPoint2D(0.0, 4.0),
            ),
        ).value
        val hidden = scenePoint(nonReal, "intersection")
        assertFalse(hidden.isReal)
        assertEquals(
            JsxGraphPoint2D(0.0, 4.0),
            session.captureInteractionState().pointCoordinates["center"],
        )

        val realAgain = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "center",
                coordinates = JsxGraphPoint2D(0.0, 0.0),
            ),
        ).value
        val restored = scenePoint(realAgain, "intersection")
        assertTrue(restored.isReal)
        assertEquals(JsxGraphPoint2D(2.0, 0.0), restored.coordinates)
    }

    @Test
    fun sessionReevaluatesArcRangeIntersectionAfterParentMove() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"O",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"R",
                      "type":"point",
                      "parents":[3,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"T",
                      "type":"point",
                      "parents":[0,3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"arc",
                      "type":"arc",
                      "parents":["O","R","T"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[-2,-5],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[-2,5],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"line",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"intersection",
                      "type":"intersection",
                      "parents":["arc","line",1],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "alwaysIntersect":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertFalse(scenePoint(session.scene, "intersection").isReal)
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "T",
                coordinates = JsxGraphPoint2D(0.0, -3.0),
            ),
        ).value
        val intersection = scenePoint(moved, "intersection")
        assertTrue(intersection.isReal)
        assertEquals(
            -2.0,
            intersection.coordinates.x,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            -2.23606797749979,
            intersection.coordinates.y,
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun sessionMovesFreePointsAndUpdatesDependentElements() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(REFERENCE_SOURCE),
        ).value
        val initialPoints = session.scene.elements
            .filterIsInstance<JsxGraphSceneElement.Point>()
        assertEquals(true, initialPoints[0].draggable)
        assertEquals(false, initialPoints[1].draggable)
        assertEquals(
            mapOf("A" to JsxGraphPoint2D(1.0, 2.0)),
            session.captureInteractionState().pointCoordinates,
        )

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint(
                id = "A",
                coordinates = JsxGraphPoint2D(2.0, 3.0),
            ),
        ).value
        val movedPoints = moved.elements
            .filterIsInstance<JsxGraphSceneElement.Point>()
        assertEquals(JsxGraphPoint2D(2.0, 3.0), movedPoints[0].coordinates)
        assertEquals(JsxGraphPoint2D(4.0, 2.0), movedPoints[1].coordinates)
        val movedLine = assertIs<JsxGraphSceneElement.Line>(
            moved.elements[2],
        )
        assertEquals(movedPoints[0].coordinates, movedLine.point1)
        assertEquals(movedPoints[1].coordinates, movedLine.point2)
        assertEquals(
            JsxGraphPoint2D(2.0, 3.0),
            assertIs<JsxGraphSceneElement.Circle>(
                moved.elements[3],
            ).center,
        )

        val reset = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.resetInteractionState(),
        ).value
        assertEquals(
            JsxGraphPoint2D(1.0, 2.0),
            assertIs<JsxGraphSceneElement.Point>(
                reset.elements[0],
            ).coordinates,
        )
    }

    @Test
    fun sessionRestoresStateAndRejectsInvalidMovesAtomically() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{"name":"","withLabel":false,"fixed":true}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"line",
                      "type":"line",
                      "parents":["A","B"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":false,
                        "straightLast":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertIs<GMResult.Err<JsxGraphInteractionError.PointNotDraggable>>(
            session.movePoint("B", JsxGraphPoint2D(3.0, 1.0)),
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.NonFiniteCoordinates>>(
            session.movePoint("A", JsxGraphPoint2D(Double.NaN, 1.0)),
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.SceneUpdate>>(
            session.movePoint("A", JsxGraphPoint2D(2.0, 0.0)),
        )
        assertEquals(
            JsxGraphPoint2D(0.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                session.scene.elements[0],
            ).coordinates,
        )

        val restored = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.restoreInteractionState(
                JsxGraphInteractionState(
                    pointCoordinates =
                        mapOf("A" to JsxGraphPoint2D(-1.5, 2.5)),
                ),
            ),
        ).value
        assertEquals(
            JsxGraphPoint2D(-1.5, 2.5),
            assertIs<JsxGraphSceneElement.Point>(
                restored.elements[0],
            ).coordinates,
        )
        assertIs<GMResult.Err<JsxGraphInteractionError.UnknownPoint>>(
            session.restoreInteractionState(
                JsxGraphInteractionState(
                    pointCoordinates =
                        mapOf("missing" to JsxGraphPoint2D(0.0, 0.0)),
                ),
            ),
        )
        assertEquals(
            JsxGraphPoint2D(-1.5, 2.5),
            assertIs<JsxGraphSceneElement.Point>(
                session.scene.elements[0],
            ).coordinates,
        )
    }

    @Test
    fun fixedLengthSegmentInteractionCouplesEndpointsAndRollsBackAllPoints() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[5,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"C",
                      "type":"point",
                      "parents":[2,-4],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"fixedSegment",
                      "type":"segment",
                      "parents":["A","B",5],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"guardLine",
                      "type":"line",
                      "parents":["B","C"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "straightFirst":false,
                        "straightLast":false
                      }
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertIs<GMResult.Err<JsxGraphInteractionError.SceneUpdate>>(
            session.movePoint("A", JsxGraphPoint2D(-1.0, -8.0)),
        )
        val rolledBack = session.captureInteractionState().pointCoordinates
        assertEquals(JsxGraphPoint2D(0.0, 0.0), rolledBack["A"])
        assertEquals(JsxGraphPoint2D(5.0, 0.0), rolledBack["B"])
        assertEquals(JsxGraphPoint2D(2.0, -4.0), rolledBack["C"])

        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("A", JsxGraphPoint2D(0.0, 3.0)),
        ).value
        val movedA = assertIs<JsxGraphSceneElement.Point>(moved.elements[0])
        val movedB = assertIs<JsxGraphSceneElement.Point>(moved.elements[1])
        assertEquals(JsxGraphPoint2D(0.0, 3.0), movedA.coordinates)
        assertEquals(
            5.0,
            pointDistance(movedA.coordinates, movedB.coordinates),
            absoluteTolerance = 1.0e-12,
        )
    }

    @Test
    fun fixedEndpointKeepsOwnershipDuringFixedLengthInteraction() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[0,0],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fixed":true
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"B",
                      "type":"point",
                      "parents":[2,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"fixedSegment",
                      "type":"segment",
                      "parents":["A","B",3],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        assertIs<GMResult.Err<JsxGraphInteractionError.PointNotDraggable>>(
            session.movePoint("A", JsxGraphPoint2D(1.0, 1.0)),
        )
        val moved = assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(4.0, 3.0)),
        ).value
        assertEquals(
            JsxGraphPoint2D(0.0, 0.0),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements[0],
            ).coordinates,
        )
        assertEquals(
            JsxGraphPoint2D(2.4, 1.8),
            assertIs<JsxGraphSceneElement.Point>(
                moved.elements[1],
            ).coordinates,
        )
    }

    @Test
    fun rejectsUnsupportedRightAngleDisplayInsteadOfChangingItsShape() {
        val error = assertError(
            documentWithObjects(
                """
                {"id":"A","type":"point","parents":[2,0],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {"id":"B","type":"point","parents":[0,0],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {"id":"C","type":"point","parents":[0,2],"attributes":{"name":"","withLabel":false}}
                """.trimIndent(),
                """
                {
                  "id":"angle",
                  "type":"angle",
                  "parents":["A","B","C"],
                  "attributes":{"name":"","withLabel":false,"radius":1}
                }
                """.trimIndent(),
            ),
        )

        val unsupported =
            assertIs<JsxGraphDocumentError.UnsupportedAttributeValue>(error)
        assertEquals("orthoType", unsupported.attribute)
        assertEquals("square", unsupported.value)
    }

    @Test
    fun boxPlotResourceLimitsCoverStaticAndDynamicOutliers() {
        val staticLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"box",
                      "type":"boxplot",
                      "parents":[[-2,-1,0,1,2,[3]],0,2],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "outlier":{"face":"circle","size":6}
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 38),
            ),
        )
        assertEquals("box", staticLimit.id)
        assertEquals(39, staticLimit.actual)

        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                source = documentWithObjects(
                    """
                    {
                      "id":"driver",
                      "type":"point",
                      "parents":[-1,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"box",
                      "type":"boxplot",
                      "parents":[
                        [
                          -2,
                          -1,
                          0,
                          1,
                          2,
                          "driver.X() > 0 ? [3] : []"
                        ],
                        0,
                        2
                      ],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "outlier":{"face":"circle","size":6}
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 38),
            ),
        ).value
        assertEquals(20, sceneCurve(session.scene, "box").points.size)

        val dynamicLimit = assertIs<JsxGraphInteractionError.SceneUpdate>(
            assertIs<GMResult.Err<JsxGraphInteractionError>>(
                session.movePoint(
                    id = "driver",
                    coordinates = JsxGraphPoint2D(1.0, 0.0),
                ),
            ).error,
        ).error
        val curveLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(dynamicLimit)
        assertEquals("box", curveLimit.id)
        assertEquals(39, curveLimit.actual)
        assertEquals(20, sceneCurve(session.scene, "box").points.size)
    }

    @Test
    fun resourceLimitsRejectInputBeforeConstruction() {
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxJsonDepth = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxCurvePoints = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxPolygonVertices = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.InvalidLimits>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxTextLength = 0),
            ),
        )
        assertIs<JsxGraphDocumentError.SourceLengthExceeded>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxSourceLength = 8),
            ),
        )
        assertIs<JsxGraphDocumentError.JsonDepthExceeded>(
            assertError(
                source = """{"nested":[[[[]]]]}""",
                limits = JsxGraphEngineLimits(maxJsonDepth = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.JsonValueLimitExceeded>(
            assertError(
                source = """{"boundingBox":[-1,1,1,-1],"objects":[]}""",
                limits = JsxGraphEngineLimits(maxJsonValues = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.ObjectLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """{"id":"A","type":"point","parents":[0,0]}""",
                    """{"id":"B","type":"point","parents":[1,1]}""",
                ),
                limits = JsxGraphEngineLimits(maxObjects = 1),
            ),
        )
        assertIs<JsxGraphDocumentError.CurvePointLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"f",
                      "type":"functiongraph",
                      "parents":["x",-1,1],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":4
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 3),
            ),
        )
        val derivativeLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"source",
                      "type":"functiongraph",
                      "parents":["x * x",-1,1],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":3
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"derivative",
                      "type":"derivative",
                      "parents":["source"],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":4
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 3),
            ),
        )
        assertEquals("derivative", derivativeLimit.id)
        assertEquals(4, derivativeLimit.actual)
        val stepLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"step",
                      "type":"stepfunction",
                      "parents":[[0,1,2],[2,1,3]]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 4),
            ),
        )
        assertEquals(5, stepLimit.actual)
        val splineLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"spline",
                      "type":"spline",
                      "parents":[[-1,0,1],[0,1,0]],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":5
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 4),
            ),
        )
        assertEquals("spline", splineLimit.id)
        assertEquals(5, splineLimit.actual)
        val cardinalSplineLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"cardinal",
                      "type":"cardinalspline",
                      "parents":[[[-1,0],[0,1],[1,0]],0.5],
                      "attributes":{
                        "doAdvancedPlot":false,
                        "numberPointsHigh":5
                      }
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 4),
            ),
        )
        assertEquals("cardinal", cardinalSplineLimit.id)
        assertEquals(5, cardinalSplineLimit.actual)
        val riemannLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"riemann",
                      "type":"riemannsum",
                      "parents":["x * x + 1",5,"left",-1,2]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 24),
            ),
        )
        assertEquals("riemann", riemannLimit.id)
        assertEquals(25, riemannLimit.actual)
        val dynamicRiemannSession = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                source = documentWithObjects(
                    """
                    {
                      "id":"A",
                      "type":"point",
                      "parents":[4,0],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"riemann",
                      "type":"riemannsum",
                      "parents":["x * x + 1","A.X()","left",-1,2],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxCurvePoints = 20),
            ),
        ).value
        assertEquals(
            20,
            sceneCurve(dynamicRiemannSession.scene, "riemann").points.size,
        )
        val dynamicRiemannLimit = assertIs<
            JsxGraphInteractionError.SceneUpdate,
            >(
            assertIs<GMResult.Err<JsxGraphInteractionError>>(
                dynamicRiemannSession.movePoint(
                    id = "A",
                    coordinates = JsxGraphPoint2D(6.0, 0.0),
                ),
            ).error,
        ).error
        val dynamicCurveLimit = assertIs<
            JsxGraphDocumentError.CurvePointLimitExceeded,
            >(dynamicRiemannLimit)
        assertEquals("riemann", dynamicCurveLimit.id)
        assertEquals(30, dynamicCurveLimit.actual)
        assertEquals(
            20,
            sceneCurve(dynamicRiemannSession.scene, "riemann").points.size,
        )
        assertIs<JsxGraphDocumentError.PolygonVertexLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"polygon",
                      "type":"polygon",
                      "parents":[[0,0],[1,0],[1,1],[0,1]]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.PolygonVertexLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"chain",
                      "type":"polygonalchain",
                      "parents":[[0,0],[1,0],[1,1],[0,1]]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 3),
            ),
        )
        assertIs<JsxGraphDocumentError.PolygonVertexLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"parallelogram",
                      "type":"parallelogram",
                      "parents":[[0,0],[1,0],[1,1]]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 2),
            ),
        )
        val regularPolygonLimit = assertIs<
            JsxGraphDocumentError.PolygonVertexLimitExceeded,
            >(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"regular",
                      "type":"regularpolygon",
                      "parents":[[0,0],[1,0],5]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxPolygonVertices = 4),
            ),
        )
        assertEquals(5, regularPolygonLimit.actual)
        assertIs<JsxGraphDocumentError.TextLengthLimitExceeded>(
            assertError(
                source = documentWithObjects(
                    """
                    {
                      "id":"text",
                      "type":"text",
                      "parents":[0,0,"four"]
                    }
                    """.trimIndent(),
                ),
                limits = JsxGraphEngineLimits(maxTextLength = 3),
            ),
        )
    }

    @Test
    fun curveBooleanDocumentsRenderFillAndTrackMovedParents() {
        val session = assertIs<GMResult.Ok<JsxGraphSession>>(
            JsxGraphEngine.createSession(
                documentWithObjects(
                    """
                    {"id":"A","type":"point","parents":[-3,-2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"B","type":"point","parents":[2,-2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"C","type":"point","parents":[2,2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {"id":"D","type":"point","parents":[-3,2],"attributes":{"name":"","withLabel":false}}
                    """.trimIndent(),
                    """
                    {
                      "id":"subject",
                      "type":"polygon",
                      "parents":["A","B","C","D"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"clip",
                      "type":"curve",
                      "parents":[[-1,4,4,-1],[-3,-3,1,1]],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"intersection",
                      "type":"curveintersection",
                      "parents":["subject","clip"],
                      "attributes":{
                        "name":"",
                        "withLabel":false,
                        "fillColor":"#009E73",
                        "fillOpacity":0.35,
                        "strokeColor":"#0072B2"
                      }
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"union",
                      "type":"curveunion",
                      "parents":["subject","clip"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                    """
                    {
                      "id":"difference",
                      "type":"curvedifference",
                      "parents":["subject","clip"],
                      "attributes":{"name":"","withLabel":false}
                    }
                    """.trimIndent(),
                ),
            ),
        ).value

        val intersection = sceneCurve(session.scene, "intersection")
        assertEquals(JsxGraphColor(0, 158, 115), intersection.style.fillColor)
        assertEquals(0.35, intersection.style.fillOpacity)
        assertCurvePoints(
            intersection,
            listOf(
                JsxGraphPoint2D(-1.0, -2.0),
                JsxGraphPoint2D(2.0, -2.0),
                JsxGraphPoint2D(2.0, 1.0),
                JsxGraphPoint2D(-1.0, 1.0),
                JsxGraphPoint2D(-1.0, -2.0),
            ),
        )
        assertEquals(9, sceneCurve(session.scene, "union").points.size)
        assertEquals(7, sceneCurve(session.scene, "difference").points.size)

        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("B", JsxGraphPoint2D(0.0, -2.0)),
        )
        assertIs<GMResult.Ok<JsxGraphScene>>(
            session.movePoint("C", JsxGraphPoint2D(0.0, 2.0)),
        )
        assertCurvePoints(
            sceneCurve(session.scene, "intersection"),
            listOf(
                JsxGraphPoint2D(-1.0, -2.0),
                JsxGraphPoint2D(0.0, -2.0),
                JsxGraphPoint2D(0.0, 1.0),
                JsxGraphPoint2D(-1.0, 1.0),
                JsxGraphPoint2D(-1.0, -2.0),
            ),
        )
    }

    private fun assertError(
        source: String,
        limits: JsxGraphEngineLimits = JsxGraphEngineLimits(),
    ): JsxGraphDocumentError =
        assertIs<GMResult.Err<JsxGraphDocumentError>>(
            JsxGraphEngine.parse(source, limits),
        ).error

    private fun documentWithObjects(
        vararg objects: String,
    ): String =
        """
        {
          "boundingBox": [-5, 5, 5, -5],
          "objects": [${objects.joinToString(",")}]
        }
        """.trimIndent()

    private fun polygonPathIntersectionDocument(
        index: String,
    ): String =
        documentWithObjects(
            """
            {
              "id":"A",
              "type":"point",
              "parents":[-2,-2],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"B",
              "type":"point",
              "parents":[2,-2],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"C",
              "type":"point",
              "parents":[2,2],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"D",
              "type":"point",
              "parents":[-2,2],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"polygon",
              "type":"polygon",
              "parents":["A","B","C","D"],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"O",
              "type":"point",
              "parents":[0,0],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"circle",
              "type":"circle",
              "parents":["O",2.5],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"diamond",
              "type":"polygon",
              "parents":[[0,-3],[3,0],[0,3],[-3,0]],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"polygonCircle",
              "type":"intersection",
              "parents":["polygon","circle",$index],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"circlePolygon",
              "type":"intersection",
              "parents":["circle","polygon",0],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
            """
            {
              "id":"polygonDiamond",
              "type":"intersection",
              "parents":["polygon","diamond",0],
              "attributes":{"name":"","withLabel":false}
            }
            """.trimIndent(),
        )

    private fun pointDistance(
        first: JsxGraphPoint2D,
        second: JsxGraphPoint2D,
    ): Double = hypot(first.x - second.x, first.y - second.y)

    private fun scenePoint(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Point =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneLine(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Line =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneCircle(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Circle =
        assertIs(scene.elements.single { it.id == id })

    private fun sceneCurve(
        scene: JsxGraphScene,
        id: String,
    ): JsxGraphSceneElement.Curve =
        assertIs(scene.elements.single { it.id == id })

    private fun assertPointCoordinates(
        expected: JsxGraphPoint2D,
        actual: JsxGraphPoint2D,
        tolerance: Double = 1.0e-12,
    ) {
        assertEquals(expected.x, actual.x, absoluteTolerance = tolerance)
        assertEquals(expected.y, actual.y, absoluteTolerance = tolerance)
    }

    private fun assertCurvePoints(
        curve: JsxGraphSceneElement.Curve,
        expected: List<JsxGraphPoint2D?>,
    ) {
        assertEquals(expected.size, curve.points.size)
        for (index in expected.indices) {
            val expectedPoint = expected[index]
            val actualPoint = curve.points[index]
            if (expectedPoint == null) {
                assertEquals(null, actualPoint)
            } else {
                assertPointCoordinates(
                    expectedPoint,
                    assertIs(actualPoint),
                )
            }
        }
    }

    private fun assertPerpendicularLine(
        baseLine: JsxGraphSceneElement.Line,
        perpendicular: JsxGraphSceneElement.Line,
        through: JsxGraphPoint2D,
    ) {
        val baseDx = baseLine.point2.x - baseLine.point1.x
        val baseDy = baseLine.point2.y - baseLine.point1.y
        val perpendicularDx =
            perpendicular.point2.x - perpendicular.point1.x
        val perpendicularDy =
            perpendicular.point2.y - perpendicular.point1.y
        assertEquals(
            0.0,
            baseDx * perpendicularDx + baseDy * perpendicularDy,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.0,
            (through.x - perpendicular.point1.x) * perpendicularDy -
                (through.y - perpendicular.point1.y) * perpendicularDx,
            absoluteTolerance = 1.0e-12,
        )
    }

    private fun assertParallelLine(
        baseLine: JsxGraphSceneElement.Line,
        parallel: JsxGraphSceneElement.Line,
        through: JsxGraphPoint2D,
    ) {
        val baseDx = baseLine.point2.x - baseLine.point1.x
        val baseDy = baseLine.point2.y - baseLine.point1.y
        val parallelDx = parallel.point2.x - parallel.point1.x
        val parallelDy = parallel.point2.y - parallel.point1.y
        assertEquals(
            0.0,
            baseDx * parallelDy - baseDy * parallelDx,
            absoluteTolerance = 1.0e-12,
        )
        assertEquals(
            0.0,
            (through.x - parallel.point1.x) * parallelDy -
                (through.y - parallel.point1.y) * parallelDx,
            absoluteTolerance = 1.0e-12,
        )
    }

    private companion object {
        val REFERENCE_SOURCE: String = """
            {
              "schemaVersion": 1,
              "boundingBox": [-6, 5, 6, -5],
              "axis": true,
              "grid": true,
              "keepAspectRatio": true,
              "objects": [
                {
                  "id": "A",
                  "type": "point",
                  "parents": [1, 2],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "size": 4,
                    "strokeColor": "#6F7780",
                    "fillColor": "#6F7780"
                  }
                },
                {
                  "id": "B",
                  "type": "point",
                  "parents": ["A.X() + 2", "A.Y() - 1"],
                  "attributes": {
                    "name": "",
                    "withLabel": false
                  }
                },
                {
                  "id": "l",
                  "type": "line",
                  "parents": ["A", "B"],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "strokeWidth": 2.5,
                    "straightFirst": false,
                    "straightLast": false
                  }
                },
                {
                  "id": "c",
                  "type": "circle",
                  "parents": ["A", 2.5],
                  "attributes": {
                    "name": "",
                    "withLabel": false,
                    "strokeColor": "#16877A"
                  }
                }
              ]
            }
        """.trimIndent()
    }
}
