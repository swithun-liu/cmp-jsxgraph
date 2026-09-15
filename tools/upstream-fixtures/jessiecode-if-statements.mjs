/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode if statements.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-if-statements.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createJessieCode = () => {
    const jessieCode = new JessieCode();
    jessieCode.board = {
        elementsByName: {},
        objects: {},
        options: {jc: {compile: false}},
        select: (name) => name
    };
    return jessieCode;
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
    "if (true) 7;",
    "if (false) 7;",
    "if (false) 7; else 9;",
    "if (true) { 1; 2; }",
    "if (true) if (false) 1; else 2;",
    "a = 1; if (true) { a = 2; } a;",
    "if (false) { missing(); } 4;",
    ";"
];

console.log(JSON.stringify(cases.map((source) => {
    const jessieCode = createJessieCode();
    return {
        source,
        ast: summarizeNode(jessieCode.getAST(source, false, true)),
        value: jessieCode.parse(source, false, true)
    };
}), null, 2));
