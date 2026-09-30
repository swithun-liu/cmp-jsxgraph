/*
 * Official JSXGraph 1.13.3 Quadtree fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/quadtree.mjs
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
    await page.setContent("<main></main>");
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const tree = new JXG.Math.Quadtree(
            [-4, 4, 4, -4],
            {capacity: 2, pointType: "object"}
        );
        const points = [
            {id: "northWestParent", x: -3, y: 3},
            {id: "southEastParent", x: 3, y: -3},
            {id: "northEastChild", x: 2, y: 2},
            {id: "southWestChild", x: -2, y: -2},
            {id: "centerBoundary", x: 0, y: 0},
            {id: "leftBoundary", x: -4, y: 1}
        ];
        const insertions = points.map((point) => tree.insert(point));
        const query = (x, y) => {
            const node = tree.query(x, y);
            return node ? node.points.map((point) => point.id) : null;
        };
        const boxItem = (id, xlb, xub, ylb, yub) => ({
            id,
            xlb,
            xub,
            ylb,
            yub
        });
        const boxTree = new JXG.Math.BoxQuadtree(
            3,
            3,
            [-4, 4, 4, -4]
        );
        boxTree.insert([
            boxItem("northWest", -3, -2, 2, 3),
            boxItem("north", -1, 1, 2, 3),
            boxItem("center", -1, 1, -1, 1)
        ]);
        const boxPlot = boxTree.plot();

        return {
            version: JXG.version,
            boxQuadtree: {
                all: boxTree
                    .find([-4, 4, 4, -4])
                    .map((item) => item.id),
                northWest: boxTree
                    .find([-4, 4, 0, 0])
                    .map((item) => item.id),
                stats: boxTree.analyzeTree(),
                plotX: boxPlot[0].map((value) =>
                    Number.isNaN(value) ? "NaN" : value
                ),
                plotY: boxPlot[1].map((value) =>
                    Number.isNaN(value) ? "NaN" : value
                )
            },
            contains: {
                left: tree.contains(-4, 1),
                right: tree.contains(4, 1),
                bottom: tree.contains(1, -4),
                top: tree.contains(1, 4),
                center: tree.contains(0, 0)
            },
            insertions,
            rootPoints: tree.points.map((point) => point.id),
            query: {
                northWest: query(-2, 2),
                northEast: query(2, 2),
                southWest: query(-2, -2),
                southEast: query(2, -2),
                center: query(0, 0),
                outside: query(5, 0)
            },
            near: tree.hasPoint(2.05, 2.05, 0.1),
            strictBoundary: tree.hasPoint(2.1, 2, 0.1),
            allPoints: tree.getAllPoints().map((point) => point.id)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
