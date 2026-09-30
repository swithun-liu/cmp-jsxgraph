/*
 * Official JSXGraph 1.13.3 ASCII STL parser fixture.
 *
 * Run with:
 *   node tools/upstream-fixtures/parse3d.mjs
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

const source = `solid first
 facet normal 0 0 0
  outer loop
   vertex 0 0 0
   vertex 1 0 0
   vertex 0 1 0
  endloop
 endfacet
 facet normal 0 0 0
  outer loop
   vertex 0.0000005 0 0
   vertex 0.000001 0 0
   vertex 1 0 0
  endloop
 endfacet
endsolid first
solid second
 facet normal 0 0 0
  outer loop
   vertex 1tail .5 -2e1
  endloop
 endfacet
endsolid second`;

console.log(
    JSON.stringify(
        {
            version: JXG.version,
            parsed: JXG.Parse3D.STL(source),
            unclosed: JXG.Parse3D.STL(
                "solid open\nfacet normal 0 0 0\nvertex 1 2 3"
            ),
            vertexBeforeFacet: capture(() =>
                JXG.Parse3D.STL("solid broken\nvertex 1 2 3")
            )
        },
        null,
        2
    )
);
