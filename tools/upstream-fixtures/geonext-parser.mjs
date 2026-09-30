/*
 * Official JSXGraph 1.13.3 GeonextParser fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/geonext-parser.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(import.meta.url);
const JXG = require(
    resolve(
        repositoryRoot,
        "jsxgraph-debug-ui/src/androidMain/assets/" +
            "jsxgraphcore-1.13.3.js"
    )
);
const parser = JXG.GeonextParser;

const capture = (operation) => {
    try {
        return {value: operation()};
    } catch (cause) {
        return {
            error: {
                name: cause?.name ?? null,
                message: cause?.message ?? null
            }
        };
    }
};

const board = {
    elementsByName: {
        A: {id: "idA"},
        B: {id: "id_B"},
        "C[1]": {id: "idC"}
    }
};

const dependencies = [];
const dependencyBoard = {
    elementsByName: {
        A: {
            elementClass: JXG.OBJECT_CLASS_POINT,
            addChild: () => dependencies.push("A")
        },
        B: {
            elementClass: JXG.OBJECT_CLASS_TEXT,
            evalVisProp: () => false,
            addChild: () => dependencies.push("B")
        },
        Label: {
            elementClass: JXG.OBJECT_CLASS_TEXT,
            evalVisProp: () => true,
            addChild: () => dependencies.push("Label")
        }
    }
};
parser.findDependencies(
    {name: "dependent", board: dependencyBoard},
    "Dist(A,B)+X(Label)"
);

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            replacePow: [
                "x ^ 2",
                "sin(x)^2",
                "(x+1)^(y+2)",
                "a^b^c",
                "^x",
                "x^",
                "x^(2",
                "((x)^2"
            ].map((source) => [source, capture(() => parser.replacePow(source))]),
            replaceIf: [
                "If(a,b,c)",
                "If(a,If(b,c,d),e)",
                "x+If(a,b,c)+If(d,e,f)",
                "If(a,b)",
                "If(a,b,c"
            ].map((source) => [source, capture(() => parser.replaceIf(source))]),
            replaceNameById: [
                "X(A)+Y(B)+L(A)+V(B)",
                "Dist(A,B)",
                "Deg(A,B,C[1])+Rad(A,B,C[1])"
            ].map((source) => [
                source,
                parser.replaceNameById(source, board, false),
                parser.replaceNameById(source, board, true)
            ]),
            replaceIdByObj: [
                "X(idA)+Y(id_B)+L(idA)+V(id_B)",
                "Dist(idA,id_B)",
                "Deg(idA,id_B,idC)+Rad(idA,id_B,idC)",
                "N(x+1)"
            ].map((source) => [source, parser.replaceIdByObj(source)]),
            conversions: [
                "x^2",
                "sin(x^2)",
                "If(True,Abs(-2),Pi)",
                "X(A)+Dist(A,B)",
                "&lt;x&gt;&amp;False",
                "fasle+Trunc(1.2)"
            ].map((source) => [
                source,
                parser.geonext2JS(source, board),
                parser.gxt2jc(source, board)
            ]),
            dependencies
        },
        null,
        2
    )
);
