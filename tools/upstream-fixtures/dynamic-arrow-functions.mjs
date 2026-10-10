/*
 * Official JSXGraph 1.13.3 dynamic arrow-attribute behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/dynamic-arrow-functions.mjs
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
        const driver = board.create("point", [1, -3], {
            id: "driver",
            name: "",
            withLabel: false
        });
        const first = board.create("point", [-3, 1], {
            id: "first",
            name: "",
            withLabel: false,
            visible: false
        });
        const last = board.create("point", [3, 1], {
            id: "last",
            name: "",
            withLabel: false,
            visible: false
        });
        const segment = board.create("segment", [first, last], {
            id: "segment",
            name: "",
            withLabel: false,
            firstArrow: () =>
                driver.X() < 2
                    ? {type: 2, size: 4, highlightsize: 5}
                    : false,
            lastArrow: {
                type: () => (driver.X() < 2 ? 3 : 6),
                size: () => driver.X() + 5,
                highlightSize: () => driver.X() + 6
            }
        });
        const curve = board.create(
            "curve",
            [[-3, 0, 3], [-1, 2, -1]],
            {
                id: "curve",
                name: "",
                withLabel: false,
                firstArrow: () => driver.X() >= 2,
                lastArrow: {
                    type: () => (driver.X() < 2 ? 4 : 7),
                    size: () => driver.X() + 2,
                    highlightSize: () => driver.X() + 3
                }
            }
        );

        const snapshot = (element) => {
            const width = element.evalVisProp("strokewidth");
            const normal = board.renderer.getArrowHeadData(
                element,
                width,
                ""
            );
            const highlighted = board.renderer.getArrowHeadData(
                element,
                width,
                "highlight"
            );
            return {
                normal: {
                    first: Boolean(normal.evFirst),
                    last: Boolean(normal.evLast),
                    typeFirst: normal.typeFirst ?? null,
                    typeLast: normal.typeLast ?? null,
                    sizeFirst: normal.sizeFirst,
                    sizeLast: normal.sizeLast
                },
                highlighted: {
                    first: Boolean(highlighted.evFirst),
                    last: Boolean(highlighted.evLast),
                    typeFirst: highlighted.typeFirst ?? null,
                    typeLast: highlighted.typeLast ?? null,
                    sizeFirst: highlighted.sizeFirst,
                    sizeLast: highlighted.sizeLast
                }
            };
        };

        board.update();
        const initial = {
            segment: snapshot(segment),
            curve: snapshot(curve)
        };
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [3, -3]);
        board.update();
        const moved = {
            segment: snapshot(segment),
            curve: snapshot(curve)
        };

        return {
            version: JXG.version,
            initial,
            moved
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
