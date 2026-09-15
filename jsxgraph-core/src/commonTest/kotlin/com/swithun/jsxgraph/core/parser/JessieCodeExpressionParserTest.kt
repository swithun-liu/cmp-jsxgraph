package com.swithun.jsxgraph.core.parser

import com.swithun.jsxgraph.core.GMResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class JessieCodeExpressionParserTest {
    @Test
    fun literalsMatchOfficialAstValuesAndMathFlags() {
        val nullNode = expression("null;")
        assertEquals(JessieCodeAstNodeType.CONSTANT, nullNode.type)
        assertEquals(JessieCodeAstValue.Null, nullNode.value)
        assertEquals(false, nullNode.isMath)

        val trueNode = expression("true;")
        assertEquals(
            JessieCodeAstValue.Boolean(true),
            trueNode.value,
        )
        assertEquals(false, trueNode.isMath)

        val stringNode = expression("\"a\\\"b\";")
        assertEquals(
            JessieCodeAstValue.Text("a\\\"b"),
            stringNode.value,
        )
        assertEquals(false, stringNode.isMath)

        val numberNode = expression("12.5e-2;")
        assertEquals(
            JessieCodeAstValue.Number(0.125),
            numberNode.value,
        )
        assertEquals(true, numberNode.isMath)

        val nanNode = expression("NaN;")
        val nanValue = assertIs<JessieCodeAstValue.Number>(
            nanNode.value,
        )
        assertTrue(nanValue.value.isNaN())
        assertEquals(true, nanNode.isMath)

        val infinityNode = expression("Infinity;")
        assertEquals(
            JessieCodeAstValue.Number(Double.POSITIVE_INFINITY),
            infinityNode.value,
        )
        assertEquals(true, infinityNode.isMath)

        val variable = expression("radius;")
        assertEquals(JessieCodeAstNodeType.VARIABLE, variable.type)
        assertEquals(
            JessieCodeAstValue.Text("radius"),
            variable.value,
        )
        assertEquals(null, variable.isMath)
    }

    @Test
    fun arithmeticPrecedenceAndAssociativityMatchOfficialAst() {
        assertEquals(
            "op_sub(" +
                "op_add(number:1.0," +
                "op_mul(number:2.0,op_exp(number:3.0,number:4.0)))," +
                "op_mod(op_div(number:5.0,number:6.0),number:7.0))",
            describe(
                expression(
                    "1 + 2 * 3 ^ 4 - 5 / 6 % 7;",
                ),
            ),
        )
        assertEquals(
            "op_neg(op_exp(number:2.0,number:4.0))",
            describe(expression("-2^4;")),
        )
        assertEquals(
            "op_exp(number:2.0,op_exp(number:3.0,number:2.0))",
            describe(expression("2^3^2;")),
        )
        assertEquals(
            "op_exp(number:2.0,op_neg(number:3.0))",
            describe(expression("2^-3;")),
        )
        assertEquals(
            "op_mul(variable:a,variable:b)",
            describe(expression("+a*b;")),
        )
    }

    @Test
    fun conditionalLogicalAndRelationalNodesMatchOfficialAst() {
        assertEquals(
            "op_conditional(" +
                "variable:a,variable:b," +
                "op_conditional(variable:c,variable:d,variable:e))",
            describe(expression("a ? b : c ? d : e;")),
        )
        assertEquals(
            "op_or(" +
                "op_not(variable:a)," +
                "op_and(variable:b,op_eq(variable:c,variable:d)))",
            describe(expression("!a || b && c == d;")),
        )
        assertEquals(
            "op_geq(op_lt(variable:a,variable:b),variable:c)",
            describe(expression("a < b >= c;")),
        )
        assertEquals(false, expression("a ? b : c;").isMath)
        assertEquals(false, expression("a || b;").isMath)
        assertEquals(false, expression("a < b;").isMath)
    }

    @Test
    fun callsPropertiesIndexesAndArraysMatchOfficialChildShape() {
        val node = expression(
            "foo.bar[2](x, [1, \"s\", null]).baz;",
        )

        assertEquals(
            "op_property(" +
                "op_execfun(" +
                "op_extvalue(" +
                "op_property(variable:foo,text:bar)," +
                "number:2.0)," +
                "list[variable:x," +
                "op_array(list[number:1.0,string:s,null])])," +
                "text:baz)",
            describe(node),
        )
        assertEquals(true, node.isMath)

        val emptyArray = expression("[];")
        assertEquals("op_array(list[])", describe(emptyArray))
        assertEquals(false, emptyArray.isMath)
    }

    @Test
    fun objectLiteralsMatchOfficialChildShapeAndFlags() {
        val emptyObject = expression("<< >>;")
        assertEquals(
            "op_emptyobject(empty-object)",
            describe(emptyObject),
        )
        assertEquals(false, emptyObject.isMath)

        val objectLiteral = expression(
            "<< a: 1, \"b\": 2 + 3, 7: \"seven\" >>;",
        )
        assertEquals(
            "op_proplst_val(" +
                "op_proplst(" +
                "op_proplst(" +
                "op_prop(text:a,number:1.0)," +
                "op_prop(string:b," +
                "op_add(number:2.0,number:3.0)))," +
                "op_prop(number:7.0,string:seven)))",
            describe(objectLiteral),
        )
        assertEquals(false, objectLiteral.isMath)

        val propertyList = childNode(objectLiteral, 0)
        val finalProperty = childNode(propertyList, 1)
        assertEquals(null, propertyList.isMath)
        assertEquals(null, finalProperty.isMath)
        assertEquals(
            JessieCodeAstLocation(1, 3, 1, 19),
            propertyList.location,
        )
        assertEquals(
            JessieCodeAstLocation(1, 3, 1, 7),
            childNode(propertyList, 0).location,
        )
        assertEquals(
            JessieCodeAstLocation(1, 21, 1, 22),
            finalProperty.location,
        )
        assertEquals(
            null,
            childNode(finalProperty, 0).isMath,
        )
    }

    @Test
    fun generatedActionLocationsMatchOfficialAst() {
        val arithmetic = expression(
            "1 + 2 * 3 ^ 4 - 5 / 6 % 7;",
        )
        assertEquals(
            JessieCodeAstLocation(
                line = 1,
                column = 0,
                endLine = 1,
                endColumn = 13,
            ),
            arithmetic.location,
        )

        val parenthesized = expression("(a + b) * c;")
        assertEquals(
            JessieCodeAstLocation(
                line = 1,
                column = 0,
                endLine = 1,
                endColumn = 7,
            ),
            parenthesized.location,
        )
        val innerAdd = childNode(parenthesized, 0)
        assertEquals(
            JessieCodeAstLocation(
                line = 1,
                column = 1,
                endLine = 1,
                endColumn = 2,
            ),
            innerAdd.location,
        )

        val multiline = expression(
            "1 +\n  2 *\n  foo.bar[0];",
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 1),
            multiline.location,
        )
        val multiply = childNode(multiline, 1)
        assertEquals(
            JessieCodeAstLocation(2, 2, 2, 3),
            multiply.location,
        )
        val indexed = childNode(multiply, 1)
        assertEquals(
            JessieCodeAstLocation(3, 2, 3, 9),
            indexed.location,
        )
    }

    @Test
    fun programWrapperMatchesOfficialSingleExpressionShape() {
        val program = parse("radius + 1;")

        assertEquals(
            JessieCodeAstValue.Text("op_none"),
            program.value,
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 0),
            program.location,
        )
        assertEquals(2, program.children.size)
        assertEquals(
            "op_none()",
            describe(childNode(program, 0)),
        )
        assertEquals(
            "op_add(variable:radius,number:1.0)",
            describe(childNode(program, 1)),
        )

        val emptyProgram = parse("")
        assertEquals("op_none()", describe(emptyProgram))
    }

    @Test
    fun assignmentsAndStatementListsMatchOfficialAst() {
        val program = parse("a = b = 3; a + b;")

        assertEquals(
            "op_none(" +
                "op_none(" +
                "op_none()," +
                "op_assign(" +
                "variable:a," +
                "op_assign(variable:b,number:3.0)))," +
                "op_add(variable:a,variable:b))",
            describe(program),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 10),
            program.location,
        )
        val assignment = childNode(childNode(program, 0), 1)
        assertEquals(false, assignment.isMath)
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 1),
            assignment.location,
        )
        assertEquals(
            JessieCodeAstLocation(1, 4, 1, 5),
            childNode(assignment, 1).location,
        )

        val parenthesized = expression("(a) = 1;")
        assertEquals(
            "op_assign(variable:a,number:1.0)",
            describe(parenthesized),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 3),
            parenthesized.location,
        )
    }

    @Test
    fun ifBlocksAndEmptyStatementsMatchOfficialAst() {
        val conditional = expression(
            "if (false) 1; else { 2; 3; }",
        )

        assertEquals(
            "op_if_else(" +
                "boolean:false," +
                "number:1.0," +
                "op_block(" +
                "op_none(" +
                "op_none(op_none(),number:2.0)," +
                "number:3.0)))",
            describe(conditional),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 2),
            conditional.location,
        )
        val block = childNode(conditional, 2)
        assertEquals(
            JessieCodeAstLocation(1, 19, 1, 20),
            block.location,
        )
        assertEquals(
            JessieCodeAstLocation(1, 19, 1, 23),
            childNode(block, 0).location,
        )

        assertEquals(
            "op_if(" +
                "boolean:true," +
                "op_if_else(boolean:false,number:1.0,number:2.0))",
            describe(
                expression(
                    "if (true) if (false) 1; else 2;",
                ),
            ),
        )
        assertEquals("op_none()", describe(expression(";")))
    }

    @Test
    fun loopStatementsMatchOfficialAst() {
        val whileLoop = expression(
            "while (a < 2) a = a + 1;",
        )
        assertEquals(
            "op_while(" +
                "op_lt(variable:a,number:2.0)," +
                "op_assign(" +
                "variable:a," +
                "op_add(variable:a,number:1.0)))",
            describe(whileLoop),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 5),
            whileLoop.location,
        )

        val forLoop = expression(
            "for (i = 0; i < 2; i = i + 1) i;",
        )
        assertEquals(
            "op_for(" +
                "op_assign(variable:i,number:0.0)," +
                "op_lt(variable:i,number:2.0)," +
                "op_assign(" +
                "variable:i," +
                "op_add(variable:i,number:1.0))," +
                "variable:i)",
            describe(forLoop),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 3),
            forLoop.location,
        )

        val doWhileLoop = expression(
            "do a = a + 1; while (a < 2);",
        )
        assertEquals(
            "op_do(" +
                "op_assign(" +
                "variable:a," +
                "op_add(variable:a,number:1.0))," +
                "op_lt(variable:a,number:2.0))",
            describe(doWhileLoop),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 2),
            doWhileLoop.location,
        )
    }

    @Test
    fun returnAndDeleteStatementsMatchOfficialAst() {
        assertEquals(
            "op_return(raw-undefined)",
            describe(expression("return;")),
        )

        val returnValue = expression("return 3;")
        assertEquals(
            "op_return(number:3.0)",
            describe(returnValue),
        )
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 6),
            returnValue.location,
        )

        val delete = expression("delete a")
        assertEquals("op_delete(text:a)", describe(delete))
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 6),
            delete.location,
        )
        assertEquals(
            "op_none(" +
                "op_none(op_none(),op_delete(text:a))," +
                "op_none())",
            describe(parse("delete a;")),
        )
    }

    @Test
    fun parserErrorsRetainOffendingAndJisonParserLocations() {
        val missingOperand = error("1 + ;")
        val unexpected = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(missingOperand)
        assertEquals(
            JessieCodeTokenType.SEMICOLON,
            unexpected.token.type,
        )
        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(4, 1, 4),
                end = JessieCodeSourcePosition(5, 1, 5),
            ),
            unexpected.token.location,
        )
        assertEquals(
            JessieCodeSourceLocation(
                start = JessieCodeSourcePosition(2, 1, 2),
                end = JessieCodeSourcePosition(3, 1, 3),
            ),
            unexpected.parserLocation,
        )
        assertEquals(
            officialExpressionStartTokens,
            unexpected.expected,
        )

        val missingColon = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("a ? b;"))
        assertEquals(
            listOf(JessieCodeTokenType.COLON),
            missingColon.expected,
        )
        assertEquals(
            JessieCodeSourcePosition(5, 1, 5),
            missingColon.token.location.start,
        )
        assertEquals(
            JessieCodeSourcePosition(4, 1, 4),
            missingColon.parserLocation.start,
        )

        val missingSemicolon = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("1"))
        assertEquals(
            JessieCodeTokenType.EOF,
            missingSemicolon.token.type,
        )
        assertEquals(
            listOf(JessieCodeTokenType.SEMICOLON),
            missingSemicolon.expected,
        )
        assertEquals(
            JessieCodeSourcePosition(1, 1, 1),
            missingSemicolon.token.location.start,
        )
    }

    @Test
    fun lexerRuleOrderQuirkRemainsVisibleToParser() {
        val error = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("a != b;"))

        assertEquals(JessieCodeTokenType.NOT, error.token.type)
        assertEquals("!", error.token.lexeme)
        assertEquals(
            JessieCodeSourcePosition(2, 1, 2),
            error.token.location.start,
        )
        assertEquals(
            listOf(JessieCodeTokenType.SEMICOLON),
            error.expected,
        )
    }

    @Test
    fun functionsAndMapsMatchOfficialAst() {
        val function = expression(
            "function (x, y) { return x + y; };",
        )
        assertEquals(
            "op_function(" +
                "text-list[x,y]," +
                "op_block(" +
                "op_none(" +
                "op_none()," +
                "op_return(op_add(variable:x,variable:y)))))",
            describe(function),
        )
        assertEquals(false, function.isMath)
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 8),
            function.location,
        )

        val map = expression("map (x, y) -> x + y;")
        assertEquals(
            "op_map(" +
                "text-list[x,y]," +
                "op_add(variable:x,variable:y))",
            describe(map),
        )
        assertEquals(null, map.isMath)
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 3),
            map.location,
        )

        assertEquals(
            "op_function(" +
                "text-list[]," +
                "op_block(op_none(op_none(),op_return(number:3.0))))",
            describe(expression("function () { return 3; };")),
        )
        assertEquals(
            "op_map(text-list[],number:3.0)",
            describe(expression("map () -> 3;")),
        )
    }

    @Test
    fun creatorAttributeListsMatchOfficialAst() {
        val call = expression(
            "capture(1, 2) << strokeColor: \"red\", size: 3 >>;",
        )
        assertEquals(
            "op_execfun(" +
                "variable:capture," +
                "list[number:1.0,number:2.0]," +
                "list[" +
                "op_proplst_val(" +
                "op_proplst(" +
                "op_prop(text:strokeColor,string:red)," +
                "op_prop(text:size,number:3.0)))]," +
                "flag:true)",
            describe(call),
        )
        assertEquals(false, call.isMath)
        assertEquals(
            JessieCodeAstLocation(1, 0, 1, 7),
            call.location,
        )

        val named = expression("capture(1) style;")
        val attribute = assertIs<JessieCodeAstChild.NodeList>(
            named.children[2],
        ).value.single()
        assertEquals("variable:style", describe(attribute))
        assertEquals(true, attribute.isMath)
        assertEquals(
            true,
            assertIs<JessieCodeAstChild.BooleanFlag>(
                named.children[3],
            ).value,
        )
    }

    @Test
    fun unsupportedAndMalformedFunctionGrammarIsStructured() {
        val use = assertIs<
            JessieCodeParserError.UnsupportedSyntax
            >(error("use board"))
        assertEquals("unary statements", use.feature)

        val attributes = assertIs<
            JessieCodeParserError.UnsupportedSyntax
            >(error("1 << a: 1 >>;"))
        assertEquals("call attribute lists", attributes.feature)

        val parameter = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("function (x,) { return x; };"))
        assertEquals(
            listOf(JessieCodeTokenType.IDENTIFIER),
            parameter.expected,
        )

        val block = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("function (x) return x;"))
        assertEquals(
            listOf(JessieCodeTokenType.LEFT_BRACE),
            block.expected,
        )

        val arrow = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("map (x) x;"))
        assertEquals(
            listOf(JessieCodeTokenType.ARROW),
            arrow.expected,
        )

        val attribute = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("capture(1) style, ;"))
        assertEquals(
            setOf(
                JessieCodeTokenType.IDENTIFIER,
                JessieCodeTokenType.SHIFT_LEFT,
            ),
            attribute.expected.toSet(),
        )
    }

    @Test
    fun invalidAssignmentGrammarReturnsStructuredErrors() {
        val invalid = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("a + b = 2;"))
        assertEquals(JessieCodeTokenType.ASSIGN, invalid.token.type)
        assertEquals(
            listOf(JessieCodeTokenType.SEMICOLON),
            invalid.expected,
        )

        assertEquals(
            "op_assign(number:1.0,number:2.0)",
            describe(expression("1 = 2;")),
        )
        assertEquals(
            "op_assign(" +
                "op_execfun(variable:foo,list[])," +
                "number:2.0)",
            describe(expression("foo() = 2;")),
        )
    }

    @Test
    fun malformedIfAndBlockStatementsReturnStructuredErrors() {
        val missingClosingParenthesis = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("if (true 1;"))
        assertEquals(
            listOf(JessieCodeTokenType.RIGHT_PARENTHESIS),
            missingClosingParenthesis.expected,
        )

        val missingClosingBrace = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("{ 1;"))
        assertEquals(
            listOf(JessieCodeTokenType.RIGHT_BRACE),
            missingClosingBrace.expected,
        )
    }

    @Test
    fun malformedObjectLiteralsReturnStructuredErrors() {
        val missingColon = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("<< a 1 >>;"))
        assertEquals(
            listOf(JessieCodeTokenType.COLON),
            missingColon.expected,
        )

        val trailingComma = assertIs<
            JessieCodeParserError.UnexpectedToken
            >(error("<< a: 1, >>;"))
        assertEquals(
            listOf(
                JessieCodeTokenType.IDENTIFIER,
                JessieCodeTokenType.STRING,
                JessieCodeTokenType.NUMBER,
                JessieCodeTokenType.NAN,
                JessieCodeTokenType.INFINITY,
            ),
            trailingComma.expected,
        )
    }

    @Test
    fun lexerAndAstLimitsReturnStructuredErrors() {
        val lexerError = JessieCodeExpressionParser(
            lexerLimits = JessieCodeLexerLimits(
                maxSourceLength = 2,
                maxTokens = 10,
            ),
        ).parse("123;")
        assertEquals(
            JessieCodeLexerError.SourceLengthExceeded(
                limit = 2,
                actual = 4,
            ),
            assertIs<
                JessieCodeParserError.Lexer
                >(
                assertIs<
                    GMResult.Err<JessieCodeParserError>
                    >(lexerError).error,
            ).error,
        )

        val invalidLimits = JessieCodeExpressionParser(
            parserLimits = JessieCodeParserLimits(
                maxAstNodes = 0,
                maxAstDepth = 0,
                maxParserNesting = 65,
            ),
        ).parse("")
        assertEquals(
            JessieCodeParserError.InvalidLimits(
                maxAstNodes = 0,
                maxAstDepth = 0,
                maxParserNesting = 65,
            ),
            assertIs<
                GMResult.Err<JessieCodeParserError.InvalidLimits>
                >(invalidLimits).error,
        )

        val nodeLimit = JessieCodeExpressionParser(
            parserLimits = JessieCodeParserLimits(
                maxAstNodes = 3,
                maxAstDepth = 256,
                maxParserNesting = 64,
            ),
        ).parse("1 + 2;")
        assertEquals(
            JessieCodeParserError.AstNodeLimitExceeded(
                limit = 3,
                location = JessieCodeSourcePosition(0, 1, 0),
            ),
            assertIs<
                GMResult.Err<
                    JessieCodeParserError.AstNodeLimitExceeded
                    >
                >(nodeLimit).error,
        )

        val deepAstSource = buildString {
            repeat(300) { index ->
                if (index > 0) {
                    append('+')
                }
                append('1')
            }
            append(';')
        }
        val astDepthLimit =
            JessieCodeExpressionParser().parse(deepAstSource)
        assertEquals(
            JessieCodeParserError.AstDepthLimitExceeded(
                limit = 256,
                location = JessieCodeSourcePosition(0, 1, 0),
            ),
            assertIs<
                GMResult.Err<
                    JessieCodeParserError.AstDepthLimitExceeded
                    >
                >(astDepthLimit).error,
        )

        val deepSource = buildString {
            repeat(300) {
                append('(')
            }
            append('1')
            repeat(300) {
                append(')')
            }
            append(';')
        }
        val nestingLimit = JessieCodeExpressionParser().parse(deepSource)
        val nestingError = assertIs<
            GMResult.Err<
                JessieCodeParserError.ParserNestingLimitExceeded
                >
            >(nestingLimit).error
        assertEquals(64, nestingError.limit)
        assertEquals(
            JessieCodeSourcePosition(64, 1, 64),
            nestingError.location,
        )
    }

    private fun parse(source: String): JessieCodeAstNode =
        assertIs<GMResult.Ok<JessieCodeAstNode>>(
            JessieCodeExpressionParser().parse(source),
        ).value

    private fun expression(source: String): JessieCodeAstNode =
        childNode(parse(source), 1)

    private fun error(source: String): JessieCodeParserError =
        assertIs<GMResult.Err<JessieCodeParserError>>(
            JessieCodeExpressionParser().parse(source),
        ).error

    private fun childNode(
        node: JessieCodeAstNode,
        index: Int,
    ): JessieCodeAstNode =
        assertIs<JessieCodeAstChild.Node>(
            node.children[index],
        ).value

    private fun describe(node: JessieCodeAstNode): String {
        val value = when (val value = node.value) {
            is JessieCodeAstValue.Text -> value.value
            is JessieCodeAstValue.Number -> "number:${value.value}"
            is JessieCodeAstValue.Boolean -> {
                "boolean:${value.value}"
            }

            JessieCodeAstValue.Null -> "null"
        }
        val prefix = when (node.type) {
            JessieCodeAstNodeType.OPERATION -> value
            JessieCodeAstNodeType.VARIABLE -> "variable:$value"
            JessieCodeAstNodeType.CONSTANT -> value
            JessieCodeAstNodeType.BOOLEAN_CONSTANT -> value
            JessieCodeAstNodeType.STRING -> "string:$value"
        }
        if (node.children.isEmpty()) {
            return if (node.type == JessieCodeAstNodeType.OPERATION) {
                "$prefix()"
            } else {
                prefix
            }
        }
        return prefix + node.children.joinToString(
            separator = ",",
            prefix = "(",
            postfix = ")",
        ) { child ->
            when (child) {
                is JessieCodeAstChild.Node -> describe(child.value)
                is JessieCodeAstChild.NodeList -> {
                    child.value.joinToString(
                        separator = ",",
                        prefix = "list[",
                        postfix = "]",
                    ) { describe(it) }
                }

                is JessieCodeAstChild.Text -> {
                    "text:${child.value}"
                }
                is JessieCodeAstChild.TextList -> {
                    child.value.joinToString(
                        separator = ",",
                        prefix = "text-list[",
                        postfix = "]",
                    )
                }
                is JessieCodeAstChild.BooleanFlag -> {
                    "flag:${child.value}"
                }
                JessieCodeAstChild.EmptyObject -> "empty-object"
                JessieCodeAstChild.Undefined -> "raw-undefined"
            }
        }
    }

    private companion object {
        val officialExpressionStartTokens = listOf(
            JessieCodeTokenType.LEFT_PARENTHESIS,
            JessieCodeTokenType.IDENTIFIER,
            JessieCodeTokenType.PLUS,
            JessieCodeTokenType.MINUS,
            JessieCodeTokenType.NOT,
            JessieCodeTokenType.LEFT_BRACKET,
            JessieCodeTokenType.NULL,
            JessieCodeTokenType.TRUE,
            JessieCodeTokenType.FALSE,
            JessieCodeTokenType.STRING,
            JessieCodeTokenType.NUMBER,
            JessieCodeTokenType.NAN,
            JessieCodeTokenType.INFINITY,
            JessieCodeTokenType.SHIFT_LEFT,
            JessieCodeTokenType.FUNCTION,
            JessieCodeTokenType.MAP,
        )
    }
}
