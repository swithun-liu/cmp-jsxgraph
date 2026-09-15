/*
 * Official JSXGraph 1.13.3 behavior fixture for function and map expressions.
 *
 * Run after cloning the pinned upstream source into third_party/jsxgraph-src:
 *   node --no-warnings tools/upstream-fixtures/jessiecode-functions-maps.mjs
 */
import JessieCode from "../../third_party/jsxgraph-src/src/parser/jessiecode.js";

const createJessieCode = () => {
    const element = {
        id: "P1",
        name: "A",
        type: 1,
        elType: "point",
        elementClass: 1,
        visProp: {}
    };
    const jessieCode = new JessieCode();
    jessieCode.board = {
        elementsByName: {A: element},
        objects: {P1: element},
        options: {jc: {compile: false}},
        select: (name) => name === "A" || name === "P1" ? element : name
    };
    return jessieCode;
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

const summarizeValue = (value) => {
    if (value === undefined) {
        return {kind: "undefined"};
    }
    if (typeof value === "function") {
        return {
            kind: "function",
            isMap: value.isMap,
            parameters: value.scope.args,
            dependencies: Object.keys(value.deps)
        };
    }
    if (Array.isArray(value)) {
        return value.map(summarizeValue);
    }
    return value;
};

const cases = [
    "function () { return 3; };",
    "function (x, y) { return x + y; };",
    "map () -> 3;",
    "map (x, y) -> x + y;",
    "f = function (x, y) { return x + y; }; f(2, 3);",
    "f = function (x) { return x; }; [f(), f(2, 3)];",
    "f = function (x, x) { return x; }; f(1, 2);",
    "f = function (x) { return x; 9; }; f(2);",
    "a = 2; f = function (x) { return a + x; }; a = 5; f(3);",
    "outer = function (x) { return function (y) { return x + y; }; }; add = outer(2); add(3);",
    "outer = function (x) { return function (y) { return x + y; }; }; a = outer(2); b = outer(4); [a(1), b(1)];",
    "f = function (x) { local = x; return local; }; [f(2), f(3)];",
    "f = function (x) { return A + x; }; f;",
    "f = function (A) { return A; }; f;",
    "m = map (x) -> x + 1; m(2);",
    "m = map (x) -> x; m(2);",
    "m = map (x) -> sin(x); m(0);",
    "map (x) -> true;",
    "map (x) -> (x = 1);"
];

console.log(JSON.stringify(cases.map((source) => {
    const jessieCode = createJessieCode();
    try {
        return {
            source,
            ast: summarizeNode(jessieCode.getAST(source, false, true)),
            value: summarizeValue(jessieCode.parse(source, false, true)),
            scopes: jessieCode.scopes.map((scope) => ({
                parameters: scope.args,
                locals: Object.keys(scope.locals)
            }))
        };
    } catch (error) {
        return {
            source,
            error: error.message
        };
    }
}), null, 2));
