/*
 * Official JSXGraph 1.13.3 Dump utility fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/dump.mjs
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

const strCases = [
    "plain",
    "function (x) { return x; }",
    2,
    null
].map((value) => {
    const result = JXG.Dump.str(value);
    return {
        type: typeof result,
        value: result
    };
});

const nested = {
    text: "a'b\"c\\d\nx",
    integer: 1,
    decimal: 1.25,
    boolean: true,
    nil: null,
    array: [1, "x", null, false],
    nested: {z: 2}
};
const ordered = {};
ordered.beta = 1;
ordered["10"] = 2;
ordered["2"] = 3;
ordered.alpha = 4;

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            str: strCases,
            jcan: {
                nested: JXG.Dump.toJCAN(nested),
                emptyArray: JXG.Dump.toJCAN([]),
                emptyObject: JXG.Dump.toJCAN({}),
                nan: JXG.Dump.toJCAN(Number.NaN),
                positiveInfinity:
                    JXG.Dump.toJCAN(Number.POSITIVE_INFINITY),
                negativeInfinity:
                    JXG.Dump.toJCAN(Number.NEGATIVE_INFINITY),
                negativeZero: JXG.Dump.toJCAN(-0),
                undefinedValue: String(JXG.Dump.toJCAN(undefined)),
                functionValue:
                    String(JXG.Dump.toJCAN(function identity(x) {
                        return x;
                    })),
                propertyOrder: JXG.Dump.toJCAN(ordered)
            },
            parameters: JXG.Dump.arrayToParamStr(
                [1, "x", null, [true, 2]],
                JXG.Dump.toJCAN
            )
        },
        null,
        2
    )
);
