/*
 * Official JSXGraph 1.13.3 behavior fixture for return and delete statements.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-return-delete.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createJessieCode = () => {
    const removed = [];
    const element = {
        id: "P1",
        name: "A",
        type: 1,
        elementClass: 1
    };
    const jessieCode = new JessieCode();
    jessieCode.board = {
        elementsByName: {A: element},
        objects: {P1: element},
        options: {jc: {compile: false}},
        select: (name) => name === "A" ? element : name,
        removeObject: (value) => removed.push(value.id)
    };
    return {jessieCode, removed};
};

const summarizeNode = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
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

const summarizeValue = (value) =>
    value === undefined ? {kind: "undefined"} : value;

const cases = [
    "return;",
    "return 3;",
    "return 3; 4;",
    "if (true) return 3; 4;",
    "delete A",
    "delete A;"
];

console.log(JSON.stringify(cases.map((source) => {
    const {jessieCode, removed} = createJessieCode();
    return {
        source,
        ast: summarizeNode(jessieCode.getAST(source, false, true)),
        value: summarizeValue(jessieCode.parse(source, false, true)),
        removed
    };
}), null, 2));
