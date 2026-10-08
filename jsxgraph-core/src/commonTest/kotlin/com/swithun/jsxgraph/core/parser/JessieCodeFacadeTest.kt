package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JessieCodeFacadeTest {
    @Test
    fun facadeScopeOperationsShareStateWithParser() {
        val jessieCode = JessieCode()
        jessieCode.letvar(
            "a",
            JessieCodeRuntimeValue.NumberValue(2.0),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(5.0),
            value(jessieCode.parse("a + 3;", storeSource = false)),
        )

        val nested = jessieCode.pushScope(listOf("x"))
        assertEquals(1, nested.id)
        assertEquals(nested.id, jessieCode.isParameter("x")?.id)
        jessieCode.letvar(
            "x",
            JessieCodeRuntimeValue.NumberValue(7.0),
        )
        assertEquals(nested.id, jessieCode.isLocalVariable("x")?.id)
        assertEquals(
            "x",
            jessieCode.findSymbol(
                JessieCodeRuntimeValue.NumberValue(7.0),
            )?.name,
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            value(jessieCode.parse("x;", storeSource = false)),
        )

        jessieCode.popScope()
        assertNull(jessieCode.isLocalVariable("x"))
        assertEquals(
            JessieCodeRuntimeValue.UndefinedValue,
            jessieCode.resolve("x"),
        )
    }

    @Test
    fun nativeCompilerRoundTripsManipulatedSource() {
        val jessieCode = JessieCode()
        val source = "a = (1 + 2) * 3; a;"
        val compiled = value(
            jessieCode.manipulate(source, storeSource = false),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(9.0),
            value(JessieCode().parse("$compiled;", storeSource = false)),
        )
        assertEquals(
            "14",
            value(
                jessieCode.format(
                    "2 + 3 * 4;",
                    JessieCodeFormatOptions(minParentheses = true),
                ),
            ).trim(),
        )
    }

    @Test
    fun attributesAreDeepCopiedAndLaterValuesWin() {
        val nested = JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "color" to JessieCodeRuntimeValue.StringValue("red"),
            ),
        )
        val first = JessieCodeRuntimeValue.ObjectValue(
            mapOf("label" to nested),
        )
        val second = JessieCodeRuntimeValue.ObjectValue(
            mapOf(
                "label" to JessieCodeRuntimeValue.ObjectValue(
                    mapOf(
                        "size" to
                            JessieCodeRuntimeValue.NumberValue(12.0),
                    ),
                ),
            ),
        )
        val merged = value(
            JessieCode().mergeAttributes(listOf(first, second)),
        )
        val mergedLabel = assertIs<JessieCodeRuntimeValue.ObjectValue>(
            merged.properties.getValue("label"),
        )

        assertNotSame(nested, mergedLabel)
        assertEquals(
            JessieCodeRuntimeValue.StringValue("red"),
            mergedLabel.properties["color"],
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(12.0),
            mergedLabel.properties["size"],
        )
        nested.properties["color"] =
            JessieCodeRuntimeValue.StringValue("blue")
        assertEquals(
            JessieCodeRuntimeValue.StringValue("red"),
            mergedLabel.properties["color"],
        )
    }

    @Test
    fun builtInsModulesOperandsAndDiagnosticsAreStructured() {
        val jessieCode = JessieCode()
        assertTrue(jessieCode.isBuiltIn("sin"))
        assertTrue(jessieCode.isMathMethod("sin"))
        assertFalse(jessieCode.isMathMethod("E"))
        assertTrue(
            jessieCode.addBuiltIn(
                "twice",
                JessieCodeCallable { arguments, _ ->
                    val value = assertIs<
                        JessieCodeRuntimeValue.NumberValue
                        >(arguments.single())
                    GMResult.Ok(
                        JessieCodeRuntimeValue.NumberValue(
                            value.value * 2.0,
                        ),
                    )
                },
            ),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(6.0),
            value(jessieCode.parse("twice(3);", storeSource = false)),
        )
        assertFalse(
            jessieCode.addBuiltIn(
                "twice",
                JessieCodeCallable { _, _ ->
                    GMResult.Ok(JessieCodeRuntimeValue.NullValue)
                },
            ),
        )
        assertEquals(
            JessieCodeModule.NUMERICS,
            value(jessieCode.importModule("MATH/NUMERICS")),
        )
        assertTrue("twice" in jessieCode.getPossibleOperands().functions)

        jessieCode._debug("debug")
        jessieCode._warn("warning", line = 2)
        jessieCode._log("log", line = 3)
        assertIs<GMResult.Err<JessieCodeFacadeError>>(
            jessieCode._error("error", line = 4),
        )
        assertEquals(
            listOf(
                JessieCodeLogLevel.DEBUG,
                JessieCodeLogLevel.WARNING,
                JessieCodeLogLevel.LOG,
                JessieCodeLogLevel.ERROR,
            ),
            jessieCode.diagnostics.map { it.level },
        )
    }

    private fun <T> value(
        result: GMResult<T, *>,
    ): T = assertIs<GMResult.Ok<T>>(result).value
}
