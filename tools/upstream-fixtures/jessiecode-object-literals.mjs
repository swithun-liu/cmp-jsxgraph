/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode object literals.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-object-literals.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const jessieCode = new JessieCode();
jessieCode.board = {
    elementsByName: {},
    objects: {},
    options: {jc: {compile: false}},
    select: (name) => name
};

const summarizeNode = (value) => {
    if (Array.isArray(value)) {
        return value.map(summarizeNode);
    }
    if (value === null || typeof value !== "object") {
        return value;
    }
    if (value.type === undefined) {
        return {kind: "raw-object"};
    }
    return {
        type: value.type,
        value: value.value,
        children: value.children.map(summarizeNode),
        isMath: value.isMath ?? null,
        line: value.line,
        column: value.col,
        endLine: value.eline,
        endColumn: value.ecol
    };
};

const cases = [
    "<< >>;",
    "<< a: 1, \"b\": 2 + 3, 7: \"seven\" >>;",
    "<< a: 1, a: 2 >>.a;",
    "<< nested: << ok: true >> >>.nested.ok;"
];

console.log(JSON.stringify(cases.map((source) => {
    const ast = jessieCode.getAST(source, false, true);
    return {
        source,
        expressionAst: summarizeNode(ast.children[1]),
        value: jessieCode.parse(source, false, true)
    };
}), null, 2));
