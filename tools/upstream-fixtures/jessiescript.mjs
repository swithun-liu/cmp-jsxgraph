/*
 * Official JSXGraph 1.13.3 legacy Board.construct fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/jessiescript.mjs
 */
import fs from "node:fs";
import vm from "node:vm";
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(import.meta.url);
const official = require(
    resolve(
        repositoryRoot,
        "jsxgraph-debug-ui/src/androidMain/assets/" +
            "jsxgraphcore-1.13.3.js"
    )
);
const calls = [];

class Board {
    constructor() {
        this.elementsByName = {};
        this.definedMacros = null;
    }

    create(type, parents, attributes) {
        const elementClass = {
            line: 2,
            parallel: 2,
            normal: 2,
            functiongraph: 4,
            circle: 3
        }[type] ?? 1;
        const element = {
            id: attributes.id ?? `generated-${calls.length}`,
            name: attributes.name ?? "",
            elementClass,
            coords: {usrCoords: [1, 0, 0]},
            Dist: () => 2,
            addChild: () => {},
            setAttribute: () => {}
        };
        if (element.name) {
            this.elementsByName[element.name] = element;
        }
        this.elementsByName[element.id] = element;
        calls.push({
            type,
            parents: parents.map((parent) => {
                if (typeof parent === "function") {
                    return "function";
                }
                if (parent && typeof parent === "object") {
                    return parent.id;
                }
                return parent;
            }),
            attributes
        });
        return element;
    }

    update() {}
}

const JXG = {
    Board,
    OBJECT_CLASS_POINT: 1,
    OBJECT_CLASS_LINE: 2,
    OBJECT_CLASS_CIRCLE: 3,
    OBJECT_CLASS_CURVE: 4,
    GeonextParser: official.GeonextParser,
    exists: (value) => value !== null && value !== undefined,
    getReference: (board, name) => board.elementsByName[name]
};

vm.runInNewContext(
    fs.readFileSync(
        resolve(
            repositoryRoot,
            "third_party/jsxgraph-src/src/parser/jessiescript.js"
        ),
        "utf8"
    ),
    {JXG}
);

const source = [
    "A(0,0) nolabel",
    "B(2,0) nolabel",
    "g=[A B] nolabel",
    "c=k(A,2) nolabel",
    "P(g,1,0) nolabel",
    "I=g&c nolabel",
    "n=|_(g,A) nolabel",
    "alpha=<(B,A,P) nolabel",
    "M=1/2(A,B) nolabel",
    "f:x^2",
    "#hello(1,2)",
    "poly[A,B,M] nolabel",
    "Pair=Macro(U,V){m=1/2(U,V) nolabel;}",
    "pair=Pair(A,B)"
].join(";");

const board = new Board();
board.construct(source);

console.log(
    JSON.stringify(
        {
            calls,
            macros: board.definedMacros.macros
        },
        null,
        2
    )
);
