/*
 * Official JSXGraph 1.13.3 standalone Face3D behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/face3d.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.CHROME_BIN ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent('<main id="fixtures"></main>');
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        let boardIndex = 0;
        const createBoard = () => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const createView = (board) => board.create(
            "view3d",
            [
                [-5, -4],
                [8, 7],
                [[-5, 5], [-4, 6], [-3, 7]]
            ],
            {
                id: `view-${boardIndex}`,
                name: "",
                projection: "parallel",
                axesPosition: "none"
            }
        );
        const createSolid = (view, xValue) => view.create(
            "polyhedron3d",
            [
                {
                    a: () => [xValue(), -1, 0],
                    b: [2, -1, 0],
                    c: [2, 3, 0],
                    d: [-2, 3, 1]
                },
                [
                    ["a", "b", "c"],
                    ["a", "c", "d"]
                ]
            ],
            {
                name: "",
                fillColorArray: ["red", "blue"],
                shader: {enabled: false}
            }
        );
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            return value;
        };
        const snapshot = (face, definition) => ({
            id: face.id,
            elType: face.elType,
            type: face.type,
            faceNumber: face.faceNumber,
            sharedDefinition: face.polyhedron === definition,
            parents: [...face.parents],
            childElements: Object.keys(face.childElements),
            inherits: face.inherits.map((element) => element.id),
            proxyParents: [...face.element2D.parents],
            proxyDump: face.element2D.dump,
            proxyX: Array.from(face.element2D.dataX).map(classify),
            proxyY: Array.from(face.element2D.dataY).map(classify),
            normal: Array.from(face.normal).map(classify),
            d: classify(face.d),
            fillColor: face.element2D.evalVisProp("fillcolor"),
            fillOpacity: face.element2D.evalVisProp("fillopacity"),
            strokeWidth: face.element2D.evalVisProp("strokewidth")
        });
        const attempt = (operation) => {
            try {
                const value = operation();
                return {
                    ok: true,
                    id: value?.id ?? null,
                    elType: value?.elType ?? typeof value
                };
            } catch (error) {
                return {
                    ok: false,
                    name: error?.name ?? null,
                    message: error?.message ?? String(error)
                };
            }
        };

        const board = createBoard();
        const view = createView(board);
        let x = -2;
        const solid = createSolid(view, () => x);
        const selected = view.create(
            "face3d",
            [solid.def, 1],
            {
                id: "selected",
                name: "",
                fillColor: "#123456",
                fillOpacity: 0.7,
                strokeWidth: 3
            }
        );
        board.update();
        const initial = snapshot(selected, solid.def);
        x = -4;
        board.update();
        const updated = snapshot(selected, solid.def);

        const invalidIndexBoard = createBoard();
        const invalidIndexView = createView(invalidIndexBoard);
        const invalidIndexSolid = createSolid(
            invalidIndexView,
            () => -2
        );
        const invalidIndex = attempt(() => invalidIndexView.create(
            "face3d",
            [invalidIndexSolid.def, 8],
            {id: "invalid-index", name: ""}
        ));

        const invalidParentBoard = createBoard();
        const invalidParentView = createView(invalidParentBoard);
        const invalidParentSolid = createSolid(
            invalidParentView,
            () => -2
        );
        const invalidParent = attempt(() => invalidParentView.create(
            "face3d",
            [invalidParentSolid, 0],
            {id: "invalid-parent", name: ""}
        ));

        const duplicate = attempt(() => view.create(
            "face3d",
            [solid.def, 0],
            {id: "selected", name: ""}
        ));

        return {
            version: JXG.version,
            direct: {
                initial,
                updated,
                sourceFaceId: solid.faces[1].id,
                sharedDefinitionWithSource:
                    selected.polyhedron === solid.faces[1].polyhedron
            },
            invalidIndex,
            invalidParent,
            duplicate
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
