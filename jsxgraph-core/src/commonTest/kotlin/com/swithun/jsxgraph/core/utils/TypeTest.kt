/*
 * Copyright (c) 2026 swithun
 * SPDX-License-Identifier: MIT
 */
package com.swithun.jsxgraph.core.utils

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import com.swithun.jsxgraph.core.base.Const
import com.swithun.jsxgraph.core.base.Coords
import com.swithun.jsxgraph.core.base.GeometryElement
import com.swithun.jsxgraph.core.base.Group
import com.swithun.jsxgraph.core.base.Point
import com.swithun.jsxgraph.core.base.Slider
import com.swithun.jsxgraph.core.base.SliderAttributes
import com.swithun.jsxgraph.core.base.Transformation
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class TypeTest {
    @Test
    fun basicTypePredicatesMatchOfficialJavaScriptCategories() {
        assertTrue(Type.isString(""))
        assertFalse(Type.isString(1))
        assertTrue(Type.isNumber(2))
        assertTrue(Type.isNumber(Double.NaN))
        assertFalse(Type.isNumber(Double.NaN, acceptNaN = false))
        assertTrue(Type.isNumber("3", acceptStringNumber = true))
        assertTrue(Type.isNumber("NaN", acceptStringNumber = true))
        assertTrue(Type.isNumber("1e-7", acceptStringNumber = true))
        assertTrue(Type.isNumber("0.000001", acceptStringNumber = true))
        assertTrue(Type.isNumber("1e+21", acceptStringNumber = true))
        assertFalse(Type.isNumber("3.0", acceptStringNumber = true))
        assertFalse(
            Type.isNumber(
                "NaN",
                acceptStringNumber = true,
                acceptNaN = false,
            ),
        )
        assertTrue(Type.isFunction { 1 })
        assertFalse(Type.isFunction(mapOf("value" to 1)))
        assertFalse(Type.isFunction(null))

        assertTrue(Type.isArray(listOf(1)))
        assertTrue(Type.isArray(intArrayOf(1)))
        assertFalse(Type.isArray(mapOf("0" to 1)))
        assertFalse(Type.isObject(listOf(1)))
        assertTrue(Type.isObject(mapOf("value" to 1)))
        assertTrue(Type.isObject(null))
        assertFalse(Type.isObject(DumpUndefined))

        assertFalse(Type.exists(null))
        assertFalse(Type.exists(DumpUndefined))
        assertTrue(Type.exists(""))
        assertFalse(Type.exists("", checkEmptyString = true))
        assertEquals("default", Type.def(null, "default"))
        assertEquals(false, Type.def(false, "default"))
        assertEquals("", Type.def("", "default"))
    }

    @Test
    fun modelPredicatesMatchOfficialTypedAndStructuralRules() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = "board",
        )
        val point = assertIs<GMResult.Ok<Point>>(
            Point.create(
                board = board,
                coordinates = doubleArrayOf(1.0, 2.0),
                id = "point",
                name = "A",
            ),
        ).value
        val point3D = GeometryElement(
            board = board,
            id = "point3d",
            type = Const.OBJECT_TYPE_POINT3D,
        )
        assertIs<GMResult.Ok<String>>(
            board.setId(point3D, "point3d"),
        )
        val group = assertIs<GMResult.Ok<Group>>(
            Group.create(
                board = board,
                parents = listOf(point),
                id = "group",
            ),
        ).value
        val transformation = assertIs<GMResult.Ok<Transformation>>(
            Transformation.create(
                type = "translate",
                parameters = doubleArrayOf(1.0, 2.0),
            ),
        ).value
        val boardShape = mapOf<String, Any?>(
            "BOARD_MODE_NONE" to 0,
            "objects" to emptyMap<String, Any?>(),
            "jc" to emptyMap<String, Any?>(),
            "update" to {},
            "containerObj" to emptyMap<String, Any?>(),
            "id" to "board",
        )

        assertTrue(Type.isBoard(board))
        assertTrue(Type.isBoard(boardShape))
        assertFalse(Type.isBoard(boardShape - "containerObj"))
        assertFalse(Type.isBoard(boardShape + ("id" to 1)))
        assertFalse(Type.isBoard(null))

        assertTrue(Type.isId(board, "point"))
        assertFalse(Type.isId(board, ""))
        assertFalse(Type.isId(board, point))
        assertTrue(Type.isName(board, "A"))
        assertFalse(Type.isName(board, "point"))
        assertFalse(Type.isName(board, point))
        assertTrue(Type.isGroup(board, group.id))
        assertFalse(Type.isGroup(board, ""))
        assertFalse(Type.isGroup(board, group))

        assertTrue(Type.isPoint(point))
        assertFalse(Type.isPoint(point3D))
        assertTrue(
            Type.isPoint(
                mapOf("elementClass" to Const.OBJECT_CLASS_POINT),
            ),
        )
        assertFalse(Type.isPoint(null))

        assertTrue(Type.isPoint3D(point3D))
        assertFalse(Type.isPoint3D(point))
        assertTrue(
            Type.isPoint3D(
                mapOf("type" to Const.OBJECT_TYPE_POINT3D),
            ),
        )
        assertFalse(Type.isPoint3D(null))

        assertTrue(okBoolean(Type.isPointType(board, emptyList<Any?>())))
        assertTrue(okBoolean(Type.isPointType(board, intArrayOf(1))))
        assertFalse(
            okBoolean(
                Type.isPointType(
                    board = board,
                    value = { emptyList<Any?>() },
                ),
            ),
        )
        assertTrue(
            okBoolean(
                Type.isPointType(
                    board = board,
                    value = { listOf(1, 2) },
                ),
            ),
        )
        assertTrue(okBoolean(Type.isPointType(board, point.id)))
        assertTrue(okBoolean(Type.isPointType(board, point.name)))
        assertTrue(okBoolean(Type.isPointType(board, point)))
        assertFalse(okBoolean(Type.isPointType(board, "missing")))

        assertFalse(
            okBoolean(Type.isPointType3D(board, emptyList<Any?>())),
        )
        assertFalse(
            okBoolean(Type.isPointType3D(board, intArrayOf(1, 2))),
        )
        assertTrue(
            okBoolean(Type.isPointType3D(board, intArrayOf(1, 2, 3))),
        )
        assertFalse(
            okBoolean(
                Type.isPointType3D(
                    board = board,
                    value = { listOf(1, 2) },
                ),
            ),
        )
        assertTrue(
            okBoolean(
                Type.isPointType3D(
                    board = board,
                    value = { listOf(1, 2, 3) },
                ),
            ),
        )
        assertTrue(okBoolean(Type.isPointType3D(board, point3D)))
        assertFalse(okBoolean(Type.isPointType3D(board, point)))
        assertFalse(okBoolean(Type.isPointType3D(board, "missing")))

        assertIs<GMResult.Err<TypeError.PointTypeEvaluationFailed>>(
            Type.isPointType(
                board = board,
                value = {
                    throw IllegalStateException("point callback failed")
                },
            ),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedPointTypeFunction>>(
            Type.isPointType(
                board = board,
                value = { _: Any? -> listOf(1, 2) },
            ),
        )

        assertTrue(Type.isTransformationOrArray(transformation))
        assertTrue(Type.isTransformationOrArray(listOf(transformation)))
        assertTrue(
            Type.isTransformationOrArray(
                listOf(transformation, emptyMap<String, Any?>()),
            ),
        )
        assertTrue(
            Type.isTransformationOrArray(listOf(listOf(transformation))),
        )
        assertTrue(
            Type.isTransformationOrArray(
                mapOf("type" to Const.OBJECT_TYPE_TRANSFORMATION),
            ),
        )
        assertFalse(Type.isTransformationOrArray(emptyList<Any?>()))
        assertFalse(
            Type.isTransformationOrArray(
                listOf(emptyMap<String, Any?>()),
            ),
        )
        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        assertFalse(Type.isTransformationOrArray(cyclic))
    }

    @Test
    fun prototypeAndMethodMapAdaptersMatchOfficialCopyRules() {
        val superConstructor = Any()
        val superMethod: () -> String = { "super" }
        val superObject = TypePrototypeDescriptor(
            constructor = superConstructor,
            properties = linkedMapOf(
                "superMethod" to superMethod,
                "sharedValue" to "super",
                "methodMap" to linkedMapOf(
                    "Base" to "base",
                    "Shared" to "super",
                    "nested" to linkedMapOf(
                        "fromSuper" to true,
                    ),
                ),
            ),
        )
        val subObject = TypePrototypeDescriptor(
            constructor = Any(),
            properties = linkedMapOf(
                "subMethod" to { "sub" },
                "sharedValue" to "sub",
                "methodMap" to linkedMapOf(
                    "Own" to "own",
                    "Shared" to "sub",
                    "nested" to linkedMapOf(
                        "fromSub" to true,
                    ),
                ),
            ),
        )

        okUnit(
            Type.copyPrototypeMethods(
                subObject = subObject,
                superObject = superObject,
                constructorName = "superConstructor",
            ),
        )

        assertEquals(
            listOf(
                "subMethod",
                "sharedValue",
                "methodMap",
                "superConstructor",
                "superMethod",
            ),
            subObject.properties.keys.toList(),
        )
        assertSame(
            superConstructor,
            subObject.properties["superConstructor"],
        )
        assertSame(superMethod, subObject.properties["superMethod"])
        assertEquals("super", subObject.properties["sharedValue"])
        val copiedMethodMap = assertIs<Map<String, Any?>>(
            subObject.properties["methodMap"],
        )
        assertEquals(
            linkedMapOf(
                "Own" to "own",
                "Shared" to "super",
                "nested" to linkedMapOf(
                    "fromSub" to true,
                    "fromSuper" to true,
                ),
                "Base" to "base",
            ),
            copiedMethodMap,
        )

        val direct = TypePrototypeDescriptor(
            constructor = Any(),
            properties = linkedMapOf(
                "methodMap" to linkedMapOf(
                    "A" to "a",
                    "nested" to linkedMapOf("left" to 1),
                ),
            ),
        )
        val directBefore = direct.properties["methodMap"]
        okUnit(
            Type.copyMethodMap(
                objectClass = direct,
                extension = linkedMapOf(
                    "B" to "b",
                    "nested" to linkedMapOf("right" to 2),
                ),
            ),
        )
        val directAfter = assertIs<Map<String, Any?>>(
            direct.properties["methodMap"],
        )
        assertFalse(directAfter === directBefore)
        assertEquals(
            linkedMapOf(
                "A" to "a",
                "nested" to linkedMapOf(
                    "left" to 1,
                    "right" to 2,
                ),
                "B" to "b",
            ),
            directAfter,
        )

        val objectExtension = TypeMethodMapInstance(copiedMethodMap)
        assertFalse(objectExtension.hasOwnMethodMap)
        okUnit(
            Type.extendInstanceMethodMap(
                instance = objectExtension,
                extension = linkedMapOf(
                    "Instance" to "instance",
                    "nested" to linkedMapOf(
                        "fromInstance" to true,
                    ),
                ),
            ),
        )
        assertTrue(objectExtension.hasOwnMethodMap)
        assertEquals(
            linkedMapOf(
                "Own" to "own",
                "Shared" to "super",
                "nested" to linkedMapOf(
                    "fromSub" to true,
                    "fromSuper" to true,
                    "fromInstance" to true,
                ),
                "Base" to "base",
                "Instance" to "instance",
            ),
            objectExtension.methodMap,
        )
        assertEquals(
            linkedMapOf(
                "fromSub" to true,
                "fromSuper" to true,
            ),
            assertIs<Map<String, Any?>>(
                copiedMethodMap["nested"],
            ),
        )

        val stringExtension = TypeMethodMapInstance(copiedMethodMap)
        okUnit(
            Type.extendInstanceMethodMap(
                instance = stringExtension,
                extension = "alias",
                extensionValue = "target",
            ),
        )
        assertEquals("target", stringExtension.methodMap["alias"])

        val ignoredExtension = TypeMethodMapInstance(copiedMethodMap)
        okUnit(
            Type.extendInstanceMethodMap(
                instance = ignoredExtension,
                extension = "alias",
                extensionValue = null,
            ),
        )
        assertFalse("alias" in ignoredExtension.methodMap)

        val invalid = TypePrototypeDescriptor(
            constructor = Any(),
            properties = linkedMapOf("methodMap" to 1),
        )
        assertIs<GMResult.Err<TypeError.InvalidMethodMap>>(
            Type.copyMethodMap(invalid),
        )
    }

    @Test
    fun traceCloneAdapterMatchesOfficialFilteringAndCacheDefaults() {
        val coordinates = Any()
        val boardReference = Any()
        val traceStrokeColor: () -> String = { "trace" }
        val source = object : TypeCloneSource {
            override val id: String = "point"
            override var numTraces: Int = 2
            override val coords: Any? = coordinates
            override val visProp: Map<String, Any?> = linkedMapOf(
                "visible" to { true },
                "strokecolor" to { "base" },
                "strokewidth" to 2,
                "traceattributes" to linkedMapOf(
                    "strokecolor" to traceStrokeColor,
                    "fillcolor" to "fill",
                    "ariaLabel" to "skip",
                    "highlightStrokeColor" to "skip",
                    "attractorDistance" to 5,
                    "label" to mapOf("visible" to true),
                    "needsRegularUpdate" to true,
                    "infoboxDigits" to 4,
                    "tabindex" to 8,
                ),
            )
            override val traceLayer: Any? = 7
            override val board: Any? = boardReference
            override val elementClass: Int = Const.OBJECT_CLASS_POINT

            override fun eval(
                value: Any?,
            ): GMResult<Any?, TypeError> =
                if (value is Function0<*>) {
                    GMResult.Ok(value())
                } else {
                    GMResult.Ok(value)
                }

            override fun evalVisProp(
                property: String,
            ): GMResult<Any?, TypeError> =
                eval(visProp[property])
        }

        val clone = assertIs<GMResult.Ok<TypeCloneObject>>(
            Type.getCloneObject(source),
        ).value

        assertEquals("pointT2", clone.id)
        assertEquals(3, source.numTraces)
        assertSame(coordinates, clone.coords)
        assertSame(boardReference, clone.board)
        assertEquals(Const.OBJECT_CLASS_POINT, clone.elementClass)
        assertEquals(
            linkedMapOf<String, Any?>(
                "visible" to true,
                "strokecolor" to "trace",
                "strokewidth" to 2,
                "traceattributes" to linkedMapOf<String, Any?>(
                    "strokecolor" to traceStrokeColor,
                    "fillcolor" to "fill",
                    "arialabel" to "skip",
                    "highlightstrokecolor" to "skip",
                    "attractordistance" to 5,
                    "label" to linkedMapOf<String, Any?>(
                        "visible" to true,
                    ),
                    "needsregularupdate" to true,
                    "infoboxdigits" to 4,
                    "tabindex" to 8,
                ),
                "fillcolor" to "fill",
                "tabindex" to null,
                "layer" to 7,
                "highlight" to false,
            ),
            clone.visProp,
        )
        assertEquals(defaultVisPropOld(), clone.visPropOld)
        assertEquals(
            mapOf<String, Any?>("visible" to true),
            clone.visPropCalc,
        )
        assertEquals("trace", clone.evalVisProp("strokecolor"))
        assertEquals("value", clone.eval("value"))

        val clearTarget = object : TypeVisualPropertyCacheOwner {
            override var visPropOld: MutableMap<String, Any?> =
                linkedMapOf("stale" to true)
        }
        assertSame(clearTarget, Type.clearVisPropOld(clearTarget))
        assertEquals(defaultVisPropOld(), clearTarget.visPropOld)
    }

    @Test
    fun str2BoolMatchesOfficialExistenceAndTypeRules() {
        assertTrue(Type.str2Bool(null))
        assertTrue(Type.str2Bool(DumpUndefined))
        assertTrue(Type.str2Bool(true))
        assertFalse(Type.str2Bool(false))
        assertTrue(Type.str2Bool("true"))
        assertTrue(Type.str2Bool("TRUE"))
        assertFalse(Type.str2Bool("false"))
        assertFalse(Type.str2Bool(" true "))
        assertFalse(Type.str2Bool(1))
        assertFalse(Type.str2Bool(emptyMap<String, Any?>()))
    }

    @Test
    fun cssUtilitiesMatchOfficialStringTransformSemantics() {
        assertEquals(
            emptyMap(),
            assertIs<GMResult.Ok<Map<String, Any?>>>(
                Type.cssParse(null),
            ).value,
        )
        assertEquals(
            linkedMapOf(
                "color" to "blue",
                "background-color" to "yellow",
            ),
            cssMap("color:blue; background-color:yellow"),
        )
        assertEquals(
            mapOf("color" to "blue"),
            cssMap(" color : blue ; "),
        )
        assertEquals(mapOf("a" to ""), cssMap("a:"))
        assertEquals(mapOf("a" to "\n"), cssMap("a:\\n"))
        assertEquals(mapOf("a" to "\"x\""), cssMap("a:\\\"x\\\""))
        assertEquals(
            listOf("2", "10", "a"),
            cssMap("10:c;2:b;a:x").keys.toList(),
        )
        assertEquals(
            mapOf("a" to "b"),
            cssMap("\u00A0a\u00A0:\u00A0b\u00A0;\u00A0"),
        )
        assertIs<GMResult.Err<TypeError.CssParseFailed>>(
            Type.cssParse(""),
        )
        assertIs<GMResult.Err<TypeError.CssParseFailed>>(
            Type.cssParse("a:1;;b:2"),
        )
        assertIs<GMResult.Err<TypeError.CssParseFailed>>(
            Type.cssParse("a:1:2"),
        )
        assertIs<GMResult.Err<TypeError.CssParseFailed>>(
            Type.cssParse("a"),
        )

        assertEquals(
            listOf(
                CssKeyValuePair("color", "blue"),
                CssKeyValuePair("backgroundColor", "yellow"),
            ),
            cssPairs("color:blue; background-color:yellow"),
        )
        assertEquals(
            listOf(
                CssKeyValuePair("a", "1"),
                CssKeyValuePair("b", "2"),
            ),
            cssPairs("a:1;;b:2"),
        )
        assertEquals(
            listOf(CssKeyValuePair("a", "1")),
            cssPairs("a:1:2"),
        )
        assertEquals(
            listOf(CssKeyValuePair("a", "")),
            cssPairs("a:"),
        )
        assertEquals(
            listOf(CssKeyValuePair("WebkitTransform", "x")),
            cssPairs("-webkit-transform: x"),
        )
        assertEquals(
            listOf(CssKeyValuePair("-CustomName", "v")),
            cssPairs("--custom-name:v"),
        )
        assertEquals(
            listOf(CssKeyValuePair("AB", "c")),
            cssPairs("A-B:c"),
        )
        assertIs<GMResult.Err<TypeError.InvalidCssDeclaration>>(
            Type.css2js("a"),
        )

        assertEquals("", Type.cssStringify(null))
        assertEquals("", Type.cssStringify(emptyList<Any?>()))
        assertEquals(
            "color:red; width:2; nan:NaN; inf:Infinity;",
            Type.cssStringify(
                linkedMapOf(
                    "color" to "red",
                    "width" to 2,
                    "skip" to true,
                    "nan" to Double.NaN,
                    "inf" to Double.POSITIVE_INFINITY,
                    "nil" to null,
                    "nested" to mapOf("x" to 1),
                ),
            ),
        )
        assertEquals(
            "2:b; 10:c; a:x;",
            Type.cssStringify(
                linkedMapOf(
                    "10" to "c",
                    "2" to "b",
                    "a" to "x",
                ),
            ),
        )
    }

    @Test
    fun evalSliderEvaluatesTranslatedSliderAndPassesOtherValuesThrough() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val slider = assertIs<GMResult.Ok<Slider>>(
            Slider.create(
                board = board,
                startCoordinates = doubleArrayOf(0.0, 0.0),
                endCoordinates = doubleArrayOf(10.0, 0.0),
                range = doubleArrayOf(0.0, 7.0, 10.0),
                attributes = SliderAttributes(
                    id = "slider",
                    name = "",
                    withTicks = false,
                    withLabel = false,
                ),
            ),
        ).value
        val raw = linkedMapOf<String, Any?>(
            "type" to Const.OBJECT_TYPE_GLIDER,
            "Value" to 9,
        )

        assertEquals(7.0, Type.evalSlider(slider))
        assertEquals(3, Type.evalSlider(3))
        assertEquals(null, Type.evalSlider(null))
        assertSame(raw, Type.evalSlider(raw))
    }

    @Test
    fun bindPreservesOwnerAndArgumentsWithStructuredFailures() {
        val owner = linkedMapOf("base" to 10)
        val bound = Type.bind(
            function = TypeOwnerCallable { current, arguments ->
                current.getValue("base") +
                    assertIs<Int>(arguments[0]) +
                    assertIs<Int>(arguments[1])
            },
            owner = owner,
        )

        assertEquals(15, okAny(bound(2, 3)))
        owner["base"] = 20
        assertEquals(25, okAny(bound(2, 3)))

        val failure = Type.bind(
            function = TypeOwnerCallable<Unit> { _, _ ->
                throw IllegalStateException("bound failed")
            },
            owner = Unit,
        )
        assertIs<GMResult.Err<TypeError.BoundFunctionFailed>>(failure())
    }

    @Test
    fun filterElementsMatchesOfficialDirectAndVisualPropertyRules() {
        val items = listOf(
            linkedMapOf<String, Any?>(
                "id" to "a",
                "rank" to 1,
                "Value" to { 5 },
                "visProp" to mapOf(
                    "visible" to true,
                    "color" to { "red" },
                ),
            ),
            linkedMapOf<String, Any?>(
                "id" to "b",
                "rank" to 2,
                "Value" to { 3 },
                "visProp" to mapOf(
                    "visible" to false,
                    "color" to "blue",
                ),
            ),
            linkedMapOf<String, Any?>(
                "id" to "c",
                "rank" to 1,
                "visProp" to mapOf("visible" to true),
            ),
        )

        assertEquals(
            listOf("a", "c"),
            elementIds(
                Type.filterElements(
                    values = items,
                    predicate = TypeFilterPredicate { item ->
                        item["rank"] == 1
                    },
                ),
            ),
        )
        assertEquals(
            listOf("a", "c"),
            elementIds(
                Type.filterElements(
                    values = items,
                    filter = linkedMapOf(
                        "rank" to TypeFilterCriterion.Exact(1.0),
                        "visible" to TypeFilterCriterion.Exact(true),
                    ),
                    accessor = TypeMapElementPropertyAccessor,
                ),
            ),
        )
        assertEquals(
            listOf("a"),
            elementIds(
                Type.filterElements(
                    values = items,
                    filter = mapOf(
                        "Value" to TypeFilterCriterion.Predicate { value ->
                            (value as? Number)?.toDouble()?.let { it > 4.0 }
                                ?: false
                        },
                    ),
                    accessor = TypeMapElementPropertyAccessor,
                ),
            ),
        )
        assertEquals(
            listOf("a"),
            elementIds(
                Type.filterElements(
                    values = items,
                    filter = mapOf(
                        "color" to TypeFilterCriterion.Exact("red"),
                    ),
                    accessor = TypeMapElementPropertyAccessor,
                ),
            ),
        )

        assertIs<GMResult.Err<TypeError.FilterPredicateFailed>>(
            Type.filterElements(
                values = items,
                predicate = TypeFilterPredicate {
                    throw IllegalStateException("predicate failed")
                },
            ),
        )
        assertIs<GMResult.Err<TypeError.FilterPropertyEvaluationFailed>>(
            Type.filterElements(
                values = listOf(
                    mapOf<String, Any?>(
                        "Value" to {
                            throw IllegalStateException("property failed")
                        },
                    ),
                ),
                filter = mapOf(
                    "Value" to TypeFilterCriterion.Exact(1),
                ),
                accessor = TypeMapElementPropertyAccessor,
            ),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedFilterPropertyFunction>>(
            Type.filterElements(
                values = listOf(
                    mapOf<String, Any?>(
                        "Value" to { value: Any? -> value },
                    ),
                ),
                filter = mapOf(
                    "Value" to TypeFilterCriterion.Exact(1),
                ),
                accessor = TypeMapElementPropertyAccessor,
            ),
        )
    }

    @Test
    fun cloneUtilitiesPreserveOfficialReferenceAndCallableShape() {
        val prototype = linkedMapOf<String, Any?>("base" to 1)
        val cloned = Type.clone(prototype)
        assertEquals(listOf("prototype"), cloned.keys.toList())
        assertSame(prototype, cloned["prototype"])

        val nested = linkedMapOf<String, Any?>("x" to 1)
        val callable = Type.cloneAndCopy(
            value = prototype,
            additions = linkedMapOf(
                "value" to 2,
                "nested" to nested,
            ),
        )
        assertEquals(listOf("value", "nested"), callable.keys())
        assertSame(prototype, callable.prototype)
        assertEquals(2, callable["value"])
        assertSame(nested, callable["nested"])
        assertSame(DumpUndefined, callable())

        val overriding = Type.cloneAndCopy(
            value = prototype,
            additions = mapOf("prototype" to "override"),
        )
        assertEquals("override", overriding.prototype)
        assertTrue(overriding.keys().isEmpty())
    }

    @Test
    fun evaluateRecursesIntoInputArraysButNotFunctionResults() {
        val two: () -> Any? = { 2 }
        val three: () -> Any? = { 3 }
        val four: () -> Any? = { 4 }
        val six: () -> Any? = { 6 }
        val returnedArray = mutableListOf<Any?>(5, six)
        val returnArray: () -> Any? = { returnedArray }
        val input = listOf<Any?>(
            1,
            two,
            listOf<Any?>(
                three,
                listOf<Any?>(four),
            ),
            returnArray,
        )

        assertEquals("value", okAny(Type.evaluate("value")))
        val evaluated = assertIs<List<*>>(okAny(Type.evaluate(input)))
        assertFalse(evaluated === input)
        assertEquals(1, evaluated[0])
        assertEquals(2, evaluated[1])
        val nested = assertIs<List<*>>(evaluated[2])
        assertFalse(nested === input[2])
        assertEquals(3, nested[0])
        assertEquals(4, assertIs<List<*>>(nested[1])[0])
        assertSame(returnedArray, evaluated[3])
        assertSame(six, returnedArray[1])
    }

    @Test
    fun evaluateFailuresAreStructured() {
        assertIs<GMResult.Err<TypeError.InvalidEvaluationLimit>>(
            Type.evaluate(
                value = 1,
                limits = TypeEvaluationLimits(maxDepth = -1),
            ),
        )
        assertIs<GMResult.Err<TypeError.EvaluationDepthLimitExceeded>>(
            Type.evaluate(
                value = listOf(listOf(1)),
                limits = TypeEvaluationLimits(maxDepth = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.EvaluationValueLimitExceeded>>(
            Type.evaluate(
                value = listOf(1),
                limits = TypeEvaluationLimits(maxValues = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedEvaluationFunction>>(
            Type.evaluate(
                value = { value: Int -> value + 1 },
            ),
        )
        assertIs<GMResult.Err<TypeError.EvaluationFailed>>(
            Type.evaluate(
                value = {
                    throw IllegalStateException("evaluation failed")
                },
            ),
        )

        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        assertIs<GMResult.Err<TypeError.CyclicEvaluationValue>>(
            Type.evaluate(cyclic),
        )
    }

    @Test
    fun toJsonMatchesNativeAndNoQuoteOfficialPaths() {
        val ordered = linkedMapOf<String, Any?>()
        ordered["beta"] = 1
        ordered["10"] = 2
        ordered["2"] = 3
        ordered["alpha"] = 4
        ordered["omitted"] = DumpUndefined
        ordered["callback"] = { 5 }
        val value = linkedMapOf<String, Any?>(
            "text" to "a'b\"c\\d\nx",
            "number" to 1.25,
            "exponent" to 1.0e-7,
            "nil" to null,
            "array" to listOf(
                DumpUndefined,
                { 1 },
                Double.NaN,
                Double.POSITIVE_INFINITY,
                -0.0,
            ),
            "ordered" to ordered,
        )

        assertEquals(
            "{\"text\":\"a'b\\\"c\\\\d\\nx\",\"number\":1.25," +
                "\"exponent\":1e-7,\"nil\":null," +
                "\"array\":[null,null,null,null,0]," +
                "\"ordered\":{\"2\":3,\"10\":2,\"beta\":1,\"alpha\":4}}",
            okJson(Type.toJSON(value)),
        )
        val noQuoteValue = linkedMapOf<String, Any?>(
            "text" to "a'b\"c\\d\nx",
            "nil" to null,
            "array" to listOf(
                DumpUndefined,
                { 1 },
                Double.NaN,
            ),
            "ordered" to ordered,
        )
        assertEquals(
            "{text:'a\\'b\\\"c\\d\nx',nil:null,array:[0,0,NaN]," +
                "ordered:{2:3,10:2,beta:1,alpha:4,omitted:0," +
                "callback:0} } ",
            okJson(Type.toJSON(noQuoteValue, noQuote = true)),
        )
        assertIs<TypeJsonValue.Undefined>(
            okJsonValue(Type.toJSON(DumpUndefined)),
        )
        assertIs<TypeJsonValue.Undefined>(
            okJsonValue(Type.toJSON(value = { 1 })),
        )
        assertEquals(
            "0",
            okJson(Type.toJSON(DumpUndefined, noQuote = true)),
        )
        assertEquals(
            "0",
            okJson(Type.toJSON({ 1 }, noQuote = true)),
        )
    }

    @Test
    fun toJsonFailuresAreStructured() {
        assertIs<GMResult.Err<TypeError.InvalidJsonLimit>>(
            Type.toJSON(
                value = null,
                limits = TypeJsonLimits(maxDepth = -1),
            ),
        )
        assertIs<GMResult.Err<TypeError.JsonDepthLimitExceeded>>(
            Type.toJSON(
                value = listOf(listOf(1)),
                limits = TypeJsonLimits(maxDepth = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.JsonValueLimitExceeded>>(
            Type.toJSON(
                value = listOf(1),
                limits = TypeJsonLimits(maxValues = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.JsonOutputLimitExceeded>>(
            Type.toJSON(
                value = "too long",
                limits = TypeJsonLimits(maxOutputLength = 3),
            ),
        )
        assertIs<GMResult.Err<TypeError.InvalidJsonObjectKey>>(
            Type.toJSON(mapOf(1 to "value")),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedJsonValue>>(
            Type.toJSON(Any()),
        )

        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        assertIs<GMResult.Err<TypeError.CyclicJsonValue>>(
            Type.toJSON(cyclic),
        )
    }

    @Test
    fun cmpArraysUsesRecursiveJavaScriptStrictEquality() {
        val shared = Any()

        assertTrue(Type.cmpArrays(listOf(1, -0.0), listOf(1.0, 0)))
        assertTrue(
            Type.cmpArrays(
                listOf(1, listOf("x", true)),
                listOf(1.0, listOf("x", true)),
            ),
        )
        assertFalse(Type.cmpArrays(listOf(Double.NaN), listOf(Double.NaN)))
        assertTrue(Type.cmpArrays(listOf(shared), listOf(shared)))
        assertFalse(Type.cmpArrays(listOf(Any()), listOf(Any())))
        assertFalse(
            Type.cmpArrays(
                listOf(listOf(1)),
                listOf(mapOf(0 to 1, "length" to 1)),
            ),
        )
    }

    @Test
    fun uniqueArrayMutatesEarlierDuplicatesAndKeepsLastOccurrence() {
        val values = mutableListOf<Any?>(1, 2, 1.0, "x", "x")
        assertEquals(
            listOf(2, 1.0, "x"),
            Type.uniqueArray(values),
        )
        assertEquals(
            listOf<Any?>("", 2, 1.0, "", "x"),
            values,
        )

        val nested = mutableListOf<Any?>(
            listOf(1),
            listOf(2),
            listOf(1.0),
        )
        assertEquals(
            listOf(listOf(2), listOf(1.0)),
            Type.uniqueArray(nested),
        )
        assertEquals(
            listOf<Any?>(
                emptyList<Any?>(),
                listOf(2),
                listOf(1.0),
            ),
            nested,
        )

        val missing = mutableListOf<Any?>(null, DumpUndefined, "x")
        assertEquals(listOf("x"), Type.uniqueArray(missing))
        assertEquals(listOf<Any?>("", "", "x"), missing)
    }

    @Test
    fun uniqueArrayPreservesIdentityAndNaNDistinction() {
        val shared = Any()
        val references = mutableListOf<Any?>(shared, shared)
        assertEquals(listOf(shared), Type.uniqueArray(references))
        assertEquals("", references[0])
        assertTrue(references[1] === shared)

        val nanValues = mutableListOf<Any?>(Double.NaN, Double.NaN)
        val result = Type.uniqueArray(nanValues)
        assertEquals(2, result.size)
        assertTrue((result[0] as Double).isNaN())
        assertTrue((result[1] as Double).isNaN())
    }

    @Test
    fun toUniqueArrayFloatSortsWithoutChangingTheSource() {
        val source = listOf(
            2.3,
            4.0,
            kotlin.math.PI,
            2.300001,
            kotlin.math.PI + 0.000000001,
        )
        assertEquals(
            listOf(2.3, kotlin.math.PI, 4.0),
            Type.toUniqueArrayFloat(source, eps = 0.00001),
        )
        assertEquals(2.3, source.first())
        assertEquals(
            listOf(1.0, 1.1),
            Type.toUniqueArrayFloat(
                listOf(1.0, 1.1, 1.2),
                eps = 0.1,
            ),
        )
        assertEquals(
            listOf(
                Double.NEGATIVE_INFINITY,
                1.0,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
            ),
            Type.toUniqueArrayFloat(
                listOf(
                    Double.POSITIVE_INFINITY,
                    1.0,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY,
                ),
                eps = 0.1,
            ),
        )
    }

    @Test
    fun listSearchRemovalAndConcatUseJavaScriptIdentity() {
        val shared = Any()
        assertEquals(
            0,
            Type.indexOf(listOf<Any?>(1), 1.0),
        )
        assertEquals(
            -1,
            Type.indexOf(listOf<Any?>(Double.NaN), Double.NaN),
        )
        assertEquals(0, Type.indexOf(listOf(shared), shared))
        assertEquals(-1, Type.indexOf(listOf(Any()), Any()))
        assertEquals(
            1,
            Type.indexOf(
                values = listOf(
                    mapOf<String, Any?>("id" to 1),
                    mapOf<String, Any?>("id" to 2),
                ),
                value = 2.0,
                property = "id",
            ),
        )
        assertTrue(Type.isInArray(listOf<Any?>(1, 2), 2.0))
        assertFalse(
            Type.isInArray(listOf<Any?>(Double.NaN), Double.NaN),
        )

        val scalarValues =
            mutableListOf<Any?>(1, 2, 1.0, Double.NaN, shared)
        assertTrue(
            Type.removeElementFromArray(scalarValues, 1.0) === scalarValues,
        )
        assertEquals(
            listOf<Any?>(2, 1.0, Double.NaN, shared),
            scalarValues,
        )

        val ordinary = mutableListOf(1)
        assertTrue(Type.concat(ordinary, listOf(2, 3)) === ordinary)
        assertEquals(listOf(1, 2, 3), ordinary)
        val self = mutableListOf(1, 2)
        Type.concat(self, self)
        assertEquals(listOf(1, 2, 1, 2), self)
    }

    @Test
    fun keyDeduplicationAndObjectLookupMatchOfficialOrdering() {
        assertEquals(
            listOf("0", "1", "2", "10", "beta", "01", "NaN", "Infinity"),
            okStrings(
                Type.eliminateDuplicates(
                    listOf(
                        "10",
                        "2",
                        "beta",
                        1,
                        "1",
                        "01",
                        -0.0,
                        Double.NaN,
                        Double.POSITIVE_INFINITY,
                        "__proto__",
                        "beta",
                    ),
                ),
            ),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedDuplicateValue>>(
            Type.eliminateDuplicates(listOf(null)),
        )

        val properties = linkedMapOf<String, Any?>(
            "beta" to 1,
            "10" to 2,
            "2" to 3,
            "alpha" to 4,
        )
        assertEquals(
            listOf("2", "10", "beta", "alpha"),
            Type.keys(properties),
        )
        val shared = Any()
        assertTrue(
            Type.isInObject(
                mapOf("number" to 1, "object" to shared),
                1.0,
            ),
        )
        assertTrue(Type.isInObject(mapOf("object" to shared), shared))
        assertFalse(Type.isInObject(mapOf("object" to Any()), Any()))
        assertFalse(
            Type.isInObject(
                mapOf("number" to Double.NaN),
                Double.NaN,
            ),
        )
    }

    @Test
    fun swapIsInPlaceAndRejectsInvalidKotlinIndices() {
        val values = mutableListOf<Any?>(1, "x", true)
        val result = assertIs<GMResult.Ok<MutableList<Any?>>>(
            Type.swap(values, 0, 2),
        ).value
        assertSame(values, result)
        assertEquals(listOf<Any?>(true, "x", 1), values)
        assertIs<GMResult.Err<TypeError.InvalidArrayIndex>>(
            Type.swap(values, -1, 0),
        )
        assertIs<GMResult.Err<TypeError.InvalidArrayIndex>>(
            Type.swap(values, 0, values.size),
        )
    }

    @Test
    fun coordinateMatrixAndEmptyChecksMatchOfficialShape() {
        val board = Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
        )
        val coordinates = listOf(
            Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(2.0, 3.0),
                board = board,
            ),
            Coords(
                method = Const.COORDS_BY_USER,
                coordinates = doubleArrayOf(-1.0, 4.0),
                board = board,
            ),
        )

        val matrix = Type.coordsArrayToMatrix(coordinates, split = false)
        assertContentEquals(doubleArrayOf(2.0, 3.0), matrix[0])
        assertContentEquals(doubleArrayOf(-1.0, 4.0), matrix[1])
        val split = Type.coordsArrayToMatrix(coordinates, split = true)
        assertContentEquals(doubleArrayOf(2.0, -1.0), split[0])
        assertContentEquals(doubleArrayOf(3.0, 4.0), split[1])

        assertTrue(Type.isEmpty(emptyMap<Any?, Any?>()))
        assertTrue(Type.isEmpty(emptyList<Any?>()))
        assertTrue(Type.isEmpty(emptyArray<Any?>()))
        assertTrue(Type.isEmpty(""))
        assertFalse(Type.isEmpty(mapOf("x" to 1)))
        assertFalse(Type.isEmpty(listOf(1)))
        assertFalse(Type.isEmpty(arrayOf(1)))
        assertFalse(Type.isEmpty("x"))
    }

    @Test
    fun deepCopyMatchesOfficialNestedMergeRules() {
        val base = linkedMapOf<String, Any?>(
            "Alpha" to 1,
            "nested" to linkedMapOf<String, Any?>(
                "left" to 1,
                "shared" to linkedMapOf<String, Any?>("x" to 1),
            ),
            "array" to listOf(
                linkedMapOf<String, Any?>("a" to 1),
                2,
            ),
            "scalar" to "base",
        )
        val secondary = linkedMapOf<String, Any?>(
            "nested" to linkedMapOf<String, Any?>(
                "right" to 2,
                "shared" to linkedMapOf<String, Any?>("y" to 2),
            ),
            "array" to listOf(
                linkedMapOf<String, Any?>("b" to 3),
            ),
            "scalar" to "override",
            "added" to true,
        )

        assertEquals(base, okAny(Type.deepCopy(base)))
        assertEquals(
            linkedMapOf<String, Any?>(
                "Alpha" to 1,
                "nested" to linkedMapOf<String, Any?>(
                    "left" to 1,
                    "shared" to linkedMapOf<String, Any?>(
                        "x" to 1,
                        "y" to 2,
                    ),
                    "right" to 2,
                ),
                "array" to listOf(
                    linkedMapOf<String, Any?>("b" to 3),
                ),
                "scalar" to "override",
                "added" to true,
            ),
            okAny(Type.deepCopy(base, secondary)),
        )
        assertEquals(
            linkedMapOf<String, Any?>(
                "alpha" to 1,
                "nested" to linkedMapOf<String, Any?>(
                    "left" to 1,
                    "shared" to linkedMapOf<String, Any?>(
                        "x" to 1,
                        "y" to 2,
                    ),
                    "right" to 2,
                ),
                "array" to listOf(
                    linkedMapOf<String, Any?>("b" to 3),
                ),
                "scalar" to "override",
                "added" to true,
            ),
            okAny(Type.deepCopy(base, secondary, toLower = true)),
        )
        assertEquals(
            listOf(1, mapOf("a" to 2)),
            okAny(
                Type.deepCopy(
                    value = listOf(1, mapOf("A" to 2)),
                    secondary = mapOf("x" to 3),
                    toLower = true,
                ),
            ),
        )
        assertEquals(
            "x",
            okAny(
                Type.deepCopy(
                    value = "x",
                    secondary = mapOf("a" to 1),
                    toLower = true,
                ),
            ),
        )

        val callback = { 1 }
        val handle = linkedMapOf<String, Any?>(
            "board" to Any(),
            "id" to "point",
        )
        assertSame(callback, okAny(Type.deepCopy(callback)))
        val references = assertIs<Map<String, Any?>>(
            okAny(
                Type.deepCopy(
                    mapOf(
                        "callback" to callback,
                        "handle" to handle,
                        "handles" to listOf(handle),
                    ),
                ),
            ),
        )
        assertSame(callback, references["callback"])
        assertEquals("point", references["handle"])
        assertEquals(listOf("point"), references["handles"])
    }

    @Test
    fun keysToLowerCasePreservesReverseCollisionAndHandleRules() {
        val collision = linkedMapOf<String, Any?>(
            "firstArrow" to 1,
            "firstarrow" to 2,
        )
        assertEquals(
            mapOf("firstarrow" to 1),
            okLower(Type.keysToLowerCase(collision)),
        )

        val array = listOf(mapOf("KeepCase" to 2))
        val handle = mapOf<String, Any?>(
            "board" to true,
            "KeepCase" to 3,
        )
        val nested = linkedMapOf<String, Any?>(
            "Outer" to mapOf("Inner" to 1),
            "Items" to array,
            "Handle" to handle,
            "Zed" to 3,
        )
        val result = okLower(Type.keysToLowerCase(nested))
        assertEquals(
            mapOf("inner" to 1),
            result["outer"],
        )
        assertSame(array, result["items"])
        assertSame(handle, result["handle"])
        assertEquals(3, result["zed"])
    }

    @Test
    fun deepCopyFailuresAreStructured() {
        assertIs<GMResult.Err<TypeError.InvalidCopyLimit>>(
            Type.deepCopy(
                value = null,
                limits = TypeCopyLimits(maxDepth = -1),
            ),
        )
        assertIs<GMResult.Err<TypeError.CopyDepthLimitExceeded>>(
            Type.deepCopy(
                value = listOf(listOf(1)),
                limits = TypeCopyLimits(maxDepth = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.CopyValueLimitExceeded>>(
            Type.deepCopy(
                value = listOf(1),
                limits = TypeCopyLimits(maxValues = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.InvalidCopyObjectKey>>(
            Type.deepCopy(mapOf(1 to "value")),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedCopyValue>>(
            Type.deepCopy(Any()),
        )

        val cyclic = mutableListOf<Any?>()
        cyclic.add(cyclic)
        assertIs<GMResult.Err<TypeError.CyclicCopyValue>>(
            Type.deepCopy(cyclic),
        )
    }

    @Test
    fun copyAttributesMatchesOfficialDefaultOverrideAndLabelOrder() {
        val options = linkedMapOf<String, Any?>(
            "elements" to linkedMapOf<String, Any?>(
                "Visible" to true,
                "StrokeColor" to "base",
                "nested" to linkedMapOf<String, Any?>(
                    "base" to 1,
                    "replace" to "elements",
                ),
                "label" to linkedMapOf<String, Any?>(
                    "fontSize" to 10,
                    "fromElements" to true,
                ),
            ),
            "layer" to linkedMapOf<String, Any?>(
                "line" to 5,
                "board" to 0,
                "custom" to 7,
            ),
            "label" to linkedMapOf<String, Any?>(
                "fontSize" to 12,
                "color" to "global",
                "globalOnly" to true,
            ),
            "line" to linkedMapOf<String, Any?>(
                "strokeColor" to "line",
                "nested" to linkedMapOf<String, Any?>(
                    "line" to 2,
                    "replace" to "line",
                ),
                "label" to linkedMapOf<String, Any?>(
                    "fontSize" to 14,
                    "lineOnly" to true,
                ),
                "point1" to linkedMapOf<String, Any?>(
                    "size" to 3,
                    "nested" to linkedMapOf<String, Any?>(
                        "point" to 4,
                    ),
                    "label" to linkedMapOf<String, Any?>(
                        "fontSize" to 16,
                        "pointOnly" to true,
                    ),
                ),
            ),
            "board" to linkedMapOf<String, Any?>(
                "showNavigation" to true,
                "label" to linkedMapOf<String, Any?>("ignored" to true),
            ),
            "custom" to linkedMapOf<String, Any?>(
                "customDefault" to 1,
                "label" to linkedMapOf<String, Any?>(
                    "customLabel" to true,
                ),
            ),
        )
        val attributes = linkedMapOf<String, Any?>(
            "StrokeColor" to "user",
            "NESTED" to linkedMapOf<String, Any?>(
                "User" to 3,
                "Replace" to "user",
            ),
            "LABEL" to linkedMapOf<String, Any?>(
                "color" to "user",
                "userOnly" to true,
            ),
            "Point1" to linkedMapOf<String, Any?>(
                "SIZE" to 8,
                "NESTED" to linkedMapOf<String, Any?>(
                    "UserPoint" to 5,
                ),
                "LABEL" to linkedMapOf<String, Any?>(
                    "color" to "point-user",
                ),
            ),
        )

        val line = attributeMap(
            Type.copyAttributes(
                attributes = attributes,
                options = options,
                path = listOf("line"),
            ),
        )
        assertEquals(true, line["visible"])
        assertEquals("user", line["strokecolor"])
        assertEquals(5, line["layer"])
        assertEquals(
            mapOf(
                "base" to 1,
                "replace" to "user",
                "line" to 2,
                "user" to 3,
            ),
            line["nested"],
        )
        assertEquals(
            mapOf(
                "fontsize" to 14,
                "color" to "user",
                "globalonly" to true,
                "lineonly" to true,
                "fromelements" to true,
                "useronly" to true,
            ),
            line["label"],
        )

        val point1 = attributeMap(
            Type.copyAttributes(
                attributes = attributes,
                options = options,
                path = listOf("line", "point1"),
            ),
        )
        assertEquals(8, point1["size"])
        assertEquals(
            mapOf("point" to 4, "userpoint" to 5),
            point1["nested"],
        )
        assertEquals(
            mapOf(
                "fontsize" to 16,
                "color" to "point-user",
                "globalonly" to true,
                "pointonly" to true,
            ),
            point1["label"],
        )

        assertEquals(
            mapOf(
                "layer" to 0,
                "shownavigation" to false,
                "label" to mapOf("ignored" to true),
            ),
            attributeMap(
                Type.copyAttributes(
                    attributes = mapOf("ShowNavigation" to false),
                    options = options,
                    path = listOf("board"),
                ),
            ),
        )

        val custom = attributeMap(
            Type.copyAttributes(
                attributes = attributes,
                options = options,
                path = listOf("custom"),
            ),
        )
        assertEquals(7, custom["layer"])
        assertEquals(1, custom["customdefault"])
        assertEquals(
            mapOf(
                "fontsize" to 12,
                "color" to "user",
                "globalonly" to true,
                "customlabel" to true,
                "useronly" to true,
            ),
            custom["label"],
        )

        val missing = attributeMap(
            Type.copyAttributes(
                attributes = attributes,
                options = options,
                path = listOf("missing"),
            ),
        )
        assertFalse("layer" in missing)
        assertEquals("user", missing["strokecolor"])
        assertEquals(
            mapOf(
                "fontsize" to 12,
                "color" to "user",
                "globalonly" to true,
                "useronly" to true,
            ),
            missing["label"],
        )
    }

    @Test
    fun mergeMutatesTargetsWithOfficialObjectAndArrayRules() {
        val target = linkedMapOf<String, Any?>(
            "board" to linkedMapOf<String, Any?>(
                "showNavigation" to true,
                "keep" to 1,
            ),
            "values" to mutableListOf<Any?>(
                linkedMapOf<String, Any?>("x" to 1),
                linkedMapOf<String, Any?>("keep" to true),
                3,
                "tail",
            ),
            "lastArrow" to false,
        )
        val result = okAny(
            Type.merge(
                target = target,
                source = linkedMapOf<String, Any?>(
                    "board" to mapOf(
                        "showNavigation" to false,
                        "showInfobox" to true,
                    ),
                    "values" to listOf(
                        mapOf("y" to 2),
                        mapOf("z" to 3),
                        4,
                    ),
                    "lastArrow" to mapOf("type" to 7),
                    "added" to "new",
                ),
            ),
        )

        assertSame(target, result)
        assertEquals(
            linkedMapOf<String, Any?>(
                "board" to linkedMapOf<String, Any?>(
                    "showNavigation" to false,
                    "keep" to 1,
                    "showInfobox" to true,
                ),
                "values" to mutableListOf<Any?>(
                    linkedMapOf<String, Any?>(
                        "x" to 1,
                        "y" to 2,
                    ),
                    linkedMapOf<String, Any?>(
                        "keep" to true,
                        "z" to 3,
                    ),
                    4,
                    "tail",
                ),
                "lastArrow" to linkedMapOf<String, Any?>(
                    "type" to 7,
                ),
                "added" to "new",
            ),
            target,
        )
        assertEquals(
            mapOf("type" to 7),
            okAny(
                Type.merge(
                    target = false,
                    source = mapOf("type" to 7),
                ),
            ),
        )

        val nullTarget = linkedMapOf<String, Any?>(
            "nested" to linkedMapOf<String, Any?>("keep" to 1),
        )
        okAny(
            Type.merge(
                target = nullTarget,
                source = mapOf("nested" to null),
            ),
        )
        assertEquals(
            mapOf<String, Any?>(
                "nested" to mapOf("keep" to 1),
            ),
            nullTarget,
        )
    }

    @Test
    fun mergeAttrMatchesAlwaysLowerAndFlatHandleRules() {
        val handle = linkedMapOf<String, Any?>(
            "board" to true,
            "id" to "point",
        )
        val array = mutableListOf<Any?>(3)
        val attributes = linkedMapOf<String, Any?>(
            "Mixed" to linkedMapOf<String, Any?>("Left" to 1),
            "mixed" to linkedMapOf<String, Any?>("right" to 2),
            "shadow" to linkedMapOf<String, Any?>(
                "enabled" to true,
                "blur" to 3,
            ),
            "draft" to false,
            "anchor" to linkedMapOf<String, Any?>("local" to true),
            "array" to mutableListOf<Any?>(1, 2),
            "keepUndefined" to "base",
        )

        okUnit(
            Type.mergeAttr(
                attributes = attributes,
                special = linkedMapOf(
                    "Mixed" to mapOf("Added" to 3),
                    "shadow" to mapOf(
                        "blur" to 5,
                        "Color" to "black",
                    ),
                    "draft" to mapOf("StrokeWidth" to 4),
                    "anchor" to handle,
                    "array" to array,
                    "keepUndefined" to DumpUndefined,
                ),
                toLower = false,
                ignoreUndefinedSpecials = false,
            ),
        )

        assertFalse(attributes.containsKey("Mixed"))
        assertEquals(
            mapOf(
                "right" to 2,
                "left" to 1,
                "added" to 3,
            ),
            attributes["mixed"],
        )
        assertEquals(
            mapOf(
                "enabled" to true,
                "blur" to 5,
                "color" to "black",
            ),
            attributes["shadow"],
        )
        assertEquals(
            mapOf("strokewidth" to 4),
            attributes["draft"],
        )
        assertSame(handle, attributes["anchor"])
        assertSame(array, attributes["array"])
        assertTrue(attributes.containsKey("keepundefined"))
        assertSame(DumpUndefined, attributes["keepundefined"])

        val ignored = linkedMapOf<String, Any?>(
            "keepUndefined" to "base",
        )
        okUnit(
            Type.mergeAttr(
                attributes = ignored,
                special = mapOf("keepUndefined" to DumpUndefined),
                ignoreUndefinedSpecials = true,
            ),
        )
        assertEquals(
            mapOf<String, Any?>("keepundefined" to "base"),
            ignored,
        )
    }

    @Test
    fun mergeFailuresAreStructured() {
        assertIs<GMResult.Err<TypeError.InvalidMergeLimit>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = emptyMap<String, Any?>(),
                limits = TypeMergeLimits(maxDepth = -1),
            ),
        )
        assertIs<GMResult.Err<TypeError.MergeDepthLimitExceeded>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = mapOf("outer" to mapOf("inner" to 1)),
                limits = TypeMergeLimits(maxDepth = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.MergeValueLimitExceeded>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = mapOf("value" to 1),
                limits = TypeMergeLimits(maxValues = 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.InvalidMergeTarget>>(
            Type.merge(
                target = null,
                source = mapOf("value" to 1),
            ),
        )
        assertIs<GMResult.Err<TypeError.InvalidMergeObjectKey>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = mapOf(1 to "value"),
            ),
        )
        assertIs<GMResult.Err<TypeError.UnsupportedMergeValue>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = Any(),
            ),
        )

        val cyclic = linkedMapOf<String, Any?>()
        cyclic["self"] = cyclic
        assertIs<GMResult.Err<TypeError.CyclicMergeValue>>(
            Type.merge(
                target = linkedMapOf<String, Any?>(),
                source = cyclic,
            ),
        )
    }

    @Test
    fun parseNumberMatchesOfficialUnitPrecedenceAndConversion() {
        assertEquals(100.0, Type.parseNumber("50%", 200.0))
        assertEquals(400.0, Type.parseNumber(" 2 fr ", 200.0))
        assertEquals(12.0, Type.parseNumber("12px", 200.0))
        assertEquals(24.0, Type.parseNumber("12px", 200.0, 2.0))
        assertEquals(
            17.0,
            ok(Type.parseNumber("12px", 200.0) { value -> value + 5.0 }),
        )
        assertEquals(
            100.0,
            ok(Type.parseNumber("50%", 200.0) { Double.NaN }),
        )
        assertEquals(0.12, Type.parseNumber("1 % 2", 1.0))
        assertEquals(
            0.12,
            Type.parseNumber("1\u00A0%\u00A02", 1.0),
        )
        assertEquals(100.0, Type.parseNumber("1e2px", 200.0))
        assertEquals(3.5, Type.parseNumber("3.5xyz", 200.0))
        assertTrue(Type.parseNumber("foo", 200.0).isNaN())
        assertTrue(Type.parseNumber(true, 200.0).isNaN())

        assertIs<GMResult.Err<TypeError.PixelConversionFailed>>(
            Type.parseNumber("12px", 200.0) {
                throw IllegalStateException("conversion failed")
            },
        )
    }

    @Test
    fun decimalAdjustmentAndFormattingMatchOfficialFixture() {
        assertEquals(3.0, Type.round10(3.14159))
        assertEquals(3.14, Type.round10(3.14159, -2))
        assertEquals(0.0, Type.round10(3.14159, 2))
        assertEquals(-3.15, Type.floor10(-3.14159, -2))
        assertEquals(3.15, Type.ceil10(3.14159, -2))
        assertEquals(-100.0, Type.floor10(-3.14159, 2))
        assertEquals(100.0, Type.ceil10(3.14159, 2))
        assertEquals(
            Double.POSITIVE_INFINITY,
            Type.round10(Double.POSITIVE_INFINITY),
        )
        assertTrue(
            Type.round10(Double.POSITIVE_INFINITY, -2).isNaN(),
        )

        assertEquals("3.14", okString(Type.toFixed(3.14159, 2)))
        assertEquals("1.01", okString(Type.toFixed(1.005, 2)))
        assertEquals("0.00", okString(Type.toFixed(-0.000001, 2)))
        assertEquals(
            "Infinity",
            okString(Type.toFixed(Double.POSITIVE_INFINITY, 0)),
        )
        assertEquals(
            "NaN",
            okString(Type.toFixed(Double.POSITIVE_INFINITY, 2)),
        )
        assertEquals("3", okString(Type.trunc(3.14159)))
        assertEquals("3.14", okString(Type.trunc(3.14159, 2)))
        assertIs<GMResult.Err<TypeError.InvalidDigits>>(
            Type.toFixed(1.0, -1),
        )
        assertIs<GMResult.Err<TypeError.InvalidDigits>>(
            Type.toFixed(1.0, 101),
        )

        assertEquals(
            AutoDigitsValue.Formatted("1.23"),
            Type.autoDigits(1.2345),
        )
        assertEquals(
            AutoDigitsValue.Formatted("0.0123"),
            Type.autoDigits(0.012345),
        )
        assertEquals(
            AutoDigitsValue.Formatted("0.001235"),
            Type.autoDigits(0.0012345),
        )
        assertEquals(
            AutoDigitsValue.Raw(0.000012345),
            Type.autoDigits(0.000012345),
        )
        assertEquals(
            AutoDigitsValue.Formatted("NaN"),
            Type.autoDigits(Double.POSITIVE_INFINITY),
        )
        val nan = assertIs<AutoDigitsValue.Raw>(
            Type.autoDigits(Double.NaN),
        )
        assertTrue(nan.value.isNaN())
    }

    @Test
    fun parsePositionMatchesOfficialTokenOverwriteOrder() {
        assertEquals(
            ParsedPosition(side = "", pos = ""),
            Type.parsePosition(""),
        )
        assertEquals(
            ParsedPosition(side = "left", pos = "top"),
            Type.parsePosition("left top"),
        )
        assertEquals(
            ParsedPosition(side = "right", pos = "bottom"),
            Type.parsePosition("bottom right"),
        )
        assertEquals(
            ParsedPosition(side = "right", pos = "center"),
            Type.parsePosition("left,right,center"),
        )
        assertEquals(
            ParsedPosition(side = "", pos = "left\tright"),
            Type.parsePosition("left\tright"),
        )
    }

    @Test
    fun stringUtilitiesMatchOfficialFixture() {
        assertEquals(
            "&amp;&lt;tag&gt;\"",
            Type.escapeHTML("&<tag>\""),
        )
        assertEquals(
            "A&B<x><broken",
            Type.unescapeHTML("<b>A&amp;B</b>&lt;x&gt;<broken"),
        )
        assertEquals(
            "&lt;b&gt;x&lt;/b&gt;",
            Type.sanitizeHTML("<b>x</b>"),
        )
        assertEquals("A&B", Type.sanitizeHTML("A&B"))
        assertEquals("", Type.capitalize(""))
        assertEquals("Hello", Type.capitalize("hELLO"))
        assertEquals("SSabc", Type.capitalize("ßABC"))

        val trimNumberCases = mapOf(
            "0001200" to "12",
            "0.00100" to "0.001",
            "1000" to "1",
            "0" to "",
            "0000" to "",
            "-00120" to "-0012",
            "001,2300" to "1,23",
        )
        for ((source, expected) in trimNumberCases) {
            assertEquals(expected, Type.trimNumber(source))
        }

        assertEquals("x", Type.trim("  x\t"))
        assertEquals("x", Type.trim("\u00A0x\u00A0"))
        assertEquals("x", Type.trim("\uFEFFx\uFEFF"))
        assertEquals("x", Type.trim("\u2003x\u2003"))
    }

    @Test
    fun fractionAndStackConversionsMatchOfficialFixture() {
        assertEquals("0", Type.toFraction(0.0))
        assertEquals("3", Type.toFraction(3.0))
        assertEquals("1/3", Type.toFraction(1.0 / 3.0))
        assertEquals("\\frac{1}{3}", Type.toFraction(1.0 / 3.0, true))
        assertEquals("3 1/3", Type.toFraction(10.0 / 3.0))
        assertEquals("-3 \\frac{1}{3}", Type.toFraction(-10.0 / 3.0, true))
        assertEquals("2", Type.toFraction(2.0001))

        assertEquals(
            StackExpression.Scalar("PI + EULER"),
            Type.stack2jsxgraph(" %pi + %e "),
        )
        assertEquals(
            StackExpression.ArrayValue(
                listOf(
                    "PI*x",
                    "1.618033988749895",
                    "0.5772156649015329",
                ),
            ),
            Type.stack2jsxgraph("[%pi*x, %phi, %gamma]"),
        )
        assertEquals(
            StackExpression.ArrayValue(listOf("")),
            Type.stack2jsxgraph("[]"),
        )
        assertEquals(
            StackExpression.ArrayValue(listOf("a", "")),
            Type.stack2jsxgraph("[a,]"),
        )
        assertEquals(
            StackExpression.ArrayValue(listOf(" a", "b ")),
            Type.stack2jsxgraph("[ a , b ]"),
        )
        assertEquals(
            StackExpression.ArrayValue(listOf("a", "b")),
            Type.stack2jsxgraph("[a\u00A0,\u00A0b]"),
        )
    }

    private fun ok(
        result: GMResult<Double, TypeError>,
    ): Double = assertIs<GMResult.Ok<Double>>(result).value

    private fun okString(
        result: GMResult<String, TypeError>,
    ): String = assertIs<GMResult.Ok<String>>(result).value

    private fun okStrings(
        result: GMResult<List<String>, TypeError>,
    ): List<String> = assertIs<GMResult.Ok<List<String>>>(result).value

    private fun okAny(
        result: GMResult<Any?, TypeError>,
    ): Any? = assertIs<GMResult.Ok<Any?>>(result).value

    private fun okBoolean(
        result: GMResult<Boolean, TypeError>,
    ): Boolean = assertIs<GMResult.Ok<Boolean>>(result).value

    private fun okLower(
        result: GMResult<Map<String, Any?>, TypeError>,
    ): Map<String, Any?> =
        assertIs<GMResult.Ok<Map<String, Any?>>>(result).value

    private fun okUnit(
        result: GMResult<Unit, TypeError>,
    ) {
        assertIs<GMResult.Ok<Unit>>(result)
    }

    private fun okJsonValue(
        result: GMResult<TypeJsonValue, TypeError>,
    ): TypeJsonValue =
        assertIs<GMResult.Ok<TypeJsonValue>>(result).value

    private fun okJson(
        result: GMResult<TypeJsonValue, TypeError>,
    ): String =
        assertIs<TypeJsonValue.Serialized>(
            okJsonValue(result),
        ).value

    private fun cssMap(value: Any?): Map<String, Any?> =
        assertIs<GMResult.Ok<Map<String, Any?>>>(
            Type.cssParse(value),
        ).value

    private fun cssPairs(value: String): List<CssKeyValuePair> =
        assertIs<GMResult.Ok<List<CssKeyValuePair>>>(
            Type.css2js(value),
        ).value

    private fun attributeMap(
        result: GMResult<Map<String, Any?>, TypeError>,
    ): Map<String, Any?> =
        assertIs<GMResult.Ok<Map<String, Any?>>>(result).value

    private fun elementIds(
        result: GMResult<List<Map<String, Any?>>, TypeError>,
    ): List<Any?> =
        assertIs<GMResult.Ok<List<Map<String, Any?>>>>(result)
            .value
            .map { item -> item["id"] }

    private fun defaultVisPropOld(): Map<String, Any?> =
        linkedMapOf(
            "cssclass" to "",
            "cssdefaultstyle" to "",
            "cssstyle" to "",
            "fillcolor" to "",
            "fillopacity" to "",
            "firstarrow" to false,
            "fontsize" to -1,
            "lastarrow" to false,
            "left" to -100000,
            "linecap" to "",
            "shadow" to false,
            "strokecolor" to "",
            "strokeopacity" to "",
            "strokewidth" to "",
            "tabindex" to -100000,
            "transitionduration" to 0,
            "top" to -100000,
            "visible" to null,
        )
}
