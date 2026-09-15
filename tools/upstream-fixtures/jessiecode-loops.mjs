/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode loops.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-loops.mjs
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
    "a = 0; while (a < 3) a = a + 1; a;",
    "a = 0; do a = a + 1; while (a < 3); a;",
    "s = 0; for (i = 0; i < 4; i = i + 1) s = s + i; s;",
    "while (false) missing();",
    "for (i = 0; false; missing()) missing();"
];

console.log(JSON.stringify(cases.map((source) => {
    const jessieCode = createJessieCode();
    return {
        source,
        ast: summarizeNode(jessieCode.getAST(source, false, true)),
        value: jessieCode.parse(source, false, true)
    };
}), null, 2));
