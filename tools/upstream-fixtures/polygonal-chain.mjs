/*
 * Official JSXGraph 1.13.3 PolygonalChain behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/polygonal-chain.mjs
 */
import {createRequire} from "node:module";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const visualParityPackage = resolve(
    repositoryRoot,
    "tools/visual-parity/package.json"
);
const require = createRequire(visualParityPackage);
const puppeteer = require("puppeteer");
const executablePath =
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width: 640px; height: 480px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const coordinates = (value) => [value.X(), value.Y()];
        const relations = (element) => ({
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshot = (chain) => ({
            id: chain.id,
            elType: chain.elType,
            type: chain.type,
            elementClass: chain.elementClass,
            withLines: chain.withLines,
            fillColor: chain.visProp.fillcolor,
            vertices: chain.vertices.map((vertex) => ({
                id: vertex.id,
                coordinates: coordinates(vertex)
            })),
            borders: chain.borders.map((border) => ({
                id: border.id,
                point1: border.point1.id,
                point2: border.point2.id
            })),
            area: chain.Area(),
            perimeter: chain.Perimeter(),
            length: chain.L(),
            boundingBox: chain.boundingBox(),
            relations: relations(chain)
        });
        const presence = (ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
            );

        const a = point([-5, 1], "A");
        const b = point([-2, 4], "B");
        const c = point([1, 2], "C");
        const d = point([4, -3], "D");
        const registered = board.create(
            "polygonalchain",
            [a, b, c, d],
            {id: "registered", name: "", withLabel: false}
        );
        const registeredInitial = snapshot(registered);
        d.setPositionDirectly(JXG.COORDS_BY_USER, [6, -4]);
        board.update();
        const registeredMoved = snapshot(registered);

        const implicit = board.create(
            "polygonalchain",
            [[-6, -2], [-3, -4], [0, -1]],
            {
                id: "implicit",
                name: "",
                withLabel: false,
                fillColor: "#ff0000"
            }
        );
        const implicitVertexIds =
            implicit.vertices.map((vertex) => vertex.id);
        const implicitSnapshot = snapshot(implicit);

        const borderless = board.create(
            "polygonalchain",
            [[1, 4], [3, 3], [5, 5]],
            {
                id: "borderless",
                name: "",
                withLabel: false,
                withLines: false
            }
        );
        const borderlessSnapshot = snapshot(borderless);

        const degenerate = [];
        for (const parents of [[], [[7, 1]], [[7, 1], [7, -1]]]) {
            try {
                const chain = board.create("polygonalchain", parents, {
                    name: "",
                    withLabel: false
                });
                degenerate.push(snapshot(chain));
            } catch (error) {
                degenerate.push({error: String(error)});
            }
        }

        const beforeInvalid = board.objectsList.length;
        let invalid;
        try {
            board.create("polygonalchain", [a, "invalid-parent"]);
            invalid = {threw: false};
        } catch (error) {
            invalid = {
                threw: true,
                message: String(error),
                retainedObjects: board.objectsList.length - beforeInvalid
            };
        }

        const registeredIds = [
            "A",
            "B",
            "C",
            "D",
            "registered",
            ...registered.borders.map((border) => border.id)
        ];
        board.removeObject(registered);
        const registeredAfterRemoval = presence(registeredIds);

        const implicitIds = [
            "implicit",
            ...implicitVertexIds,
            ...implicit.borders.map((border) => border.id)
        ];
        board.removeObject(implicit);
        const implicitAfterRemoval = presence(implicitIds);

        return {
            version: JXG.version,
            registeredInitial,
            registeredMoved,
            registeredAfterRemoval,
            registeredPointRelations: {
                A: relations(a),
                B: relations(b),
                C: relations(c),
                D: relations(d)
            },
            implicitSnapshot,
            implicitAfterRemoval,
            borderlessSnapshot,
            degenerate,
            invalid
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
