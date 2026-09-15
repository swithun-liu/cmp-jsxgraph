package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import com.swithun.jsxgraph.core.base.Board
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class JessieCodeSessionTest {
    @Test
    fun globalsFunctionsAndBoardSelectionPersistAcrossParseCalls() {
        val firstBoard = board("first")
        val secondBoard = board("second")
        val session = JessieCodeSession(
            environment = JessieCodeRuntimeEnvironment(
                board = firstBoard,
                boardsByContainer = mapOf(
                    "secondcontainer" to secondBoard,
                ),
            ),
        )

        assertEquals(
            JessieCodeRuntimeValue.NumberValue(1.0),
            value(session.parse("a = 1;")),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(3.0),
            value(session.parse("a = a + 2;")),
        )
        assertIs<JessieCodeRuntimeValue.FunctionValue>(
            value(
                session.parse(
                    "f = function (x) { return a + x; };",
                ),
            ),
        )
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(7.0),
            value(session.parse("f(4);")),
        )

        value(session.parse("use secondcontainer;"))
        val selectedBoard =
            assertIs<JessieCodeRuntimeValue.BoardReference>(
                value(session.parse("\$board;")),
            )
        assertSame(secondBoard, selectedBoard.board)
        assertEquals(
            JessieCodeRuntimeValue.NumberValue(3.0),
            value(session.parse("a;")),
        )
    }

    @Test
    fun sourceStorageAndFailuresAreExplicit() {
        val session = JessieCodeSession()
        value(session.parse("a = 1;"))
        value(session.parse("a;", storeSource = false))
        assertEquals("a = 1;\n", session.code)

        val parserError = assertIs<
            GMResult.Err<JessieCodeSessionError>
            >(session.parse("1 + ;", storeSource = false)).error
        assertIs<JessieCodeSessionError.Parser>(parserError)

        val runtimeError = assertIs<
            GMResult.Err<JessieCodeSessionError>
            >(session.parse("use missing", storeSource = false)).error
        assertIs<JessieCodeRuntimeError.BoardNotFound>(
            assertIs<JessieCodeSessionError.Runtime>(
                runtimeError,
            ).error,
        )

        val historyLimit = JessieCodeSession(
            sessionLimits = JessieCodeSessionLimits(
                maxStoredSourceLength = 3,
            ),
        ).parse("1 + 1;")
        assertIs<JessieCodeSessionError.SourceHistoryLimitExceeded>(
            assertIs<GMResult.Err<JessieCodeSessionError>>(
                historyLimit,
            ).error,
        )

        val invalidLimits = JessieCodeSession(
            sessionLimits = JessieCodeSessionLimits(
                maxStoredSourceLength = -1,
            ),
        ).parse("1;", storeSource = false)
        assertIs<JessieCodeSessionError.InvalidLimits>(
            assertIs<GMResult.Err<JessieCodeSessionError>>(
                invalidLimits,
            ).error,
        )
    }

    @Test
    fun evaluationBudgetResetsForEveryParseCall() {
        val session = JessieCodeSession(
            evaluatorLimits = JessieCodeEvaluatorLimits(
                maxEvaluationSteps = 10,
            ),
        )

        repeat(20) {
            assertEquals(
                JessieCodeRuntimeValue.NumberValue(1.0),
                value(session.parse("1;", storeSource = false)),
            )
        }
    }

    private fun value(
        result: GMResult<
            JessieCodeRuntimeValue,
            JessieCodeSessionError,
            >,
    ): JessieCodeRuntimeValue =
        assertIs<GMResult.Ok<JessieCodeRuntimeValue>>(result).value

    private fun board(id: String): Board =
        Board(
            originX = 0.0,
            originY = 0.0,
            unitX = 1.0,
            unitY = 1.0,
            id = id,
        )
}
