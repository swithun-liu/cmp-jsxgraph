/*
 * Official JSXGraph 1.13.3 behavior fixture for JessieCode assignments.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-assignments.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createJessieCode = () => {
    const jessieCode = new JessieCode();
    jessieCode.board = {
        elementsByName: {},
        objects: {},
        options: {jc: {compile: false}},
        select: (name) => name,
        update: () => {}
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

const summarizeValue = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (!Array.isArray(value)) {
        return value;
    }
    return {
        kind: "array",
        length: value.length,
        entries: Array.from({length: value.length}, (_, index) => ({
            present: Object.prototype.hasOwnProperty.call(value, index),
            value: summarizeValue(value[index])
        })),
        properties: Object.fromEntries(
            Object.keys(value)
                .filter((key) => !/^(0|[1-9][0-9]*)$/.test(key))
                .map((key) => [key, summarizeValue(value[key])])
        )
    };
};

const cases = [
    "a = b = 3; a + b;",
    "a = << x: 1 >>; a.x = 9; a.x;",
    "a = []; a[2] = 7; a;",
    "a = []; a[-1] = 4; a;",
    "a = []; a[1.2] = 4; a[1.2];",
    "a = [1, 2, 3]; a.length = 1; a;"
];

const results = cases.map((source) => {
    const jessieCode = createJessieCode();
    const ast = jessieCode.getAST(source, false, true);
    return {
        source,
        ast: summarizeNode(ast),
        value: summarizeValue(jessieCode.parse(source, false, true))
    };
});

for (const source of ["1 = 2;", "foo() = 2;"]) {
    const jessieCode = createJessieCode();
    try {
        jessieCode.parse(source, false, true);
    } catch (error) {
        results.push({
            source,
            error: String(error)
        });
    }
}

console.log(JSON.stringify(results, null, 2));
