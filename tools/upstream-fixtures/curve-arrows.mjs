/*
 * Official JSXGraph 1.13.3 Curve/Arc/Sector arrow behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/curve-arrows.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.PUPPETEER_EXECUTABLE_PATH ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<main><div id="board" style="width:640px;height:480px"></div></main>'
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
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false,
                visible: false
            });
        const snapshot = (element) => {
            const width = element.evalVisProp("strokewidth");
            const arrows = board.renderer.getArrowHeadData(
                element,
                width,
                ""
            );
            const points = element.points.map((entry) => [
                entry.scrCoords[1],
                entry.scrCoords[2]
            ]);
            return {
                elementClass: element.elementClass,
                bezierDegree: element.bezierDegree,
                pointCount: points.length,
                firstPoint: points[0],
                secondPoint: points[1],
                penultimatePoint: points[points.length - 2],
                lastPoint: points[points.length - 1],
                arrows: {
                    first: arrows.evFirst,
                    last: arrows.evLast,
                    typeFirst: arrows.typeFirst,
                    typeLast: arrows.typeLast,
                    sizeFirst: arrows.sizeFirst,
                    sizeLast: arrows.sizeLast
                }
            };
        };

        const origin = point([0, 0], "origin");
        const first = point([2, 0], "first");
        const last = point([0, 2], "last");
        const curve = board.create(
            "curve",
            [[-2, 0, 2], [0, 2, 0]],
            {
                id: "curve",
                name: "",
                withLabel: false,
                firstArrow: true,
                lastArrow: {size: 4, highlightSize: 5}
            }
        );
        const arc = board.create("arc", [origin, first, last], {
            id: "arc",
            name: "",
            withLabel: false,
            firstArrow: {type: 2, size: 3},
            lastArrow: true
        });
        const sector = board.create(
            "sector",
            [origin, first, last],
            {
                id: "sector",
                name: "",
                withLabel: false,
                firstArrow: false,
                lastArrow: {type: 6}
            }
        );
        board.update();

        return {
            version: JXG.version,
            curve: snapshot(curve),
            arc: snapshot(arc),
            sector: snapshot(sector)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
