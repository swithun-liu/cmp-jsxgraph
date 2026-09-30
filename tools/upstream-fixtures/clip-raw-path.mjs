/*
 * Official JSXGraph 1.13.3 Clip raw-path fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/clip-raw-path.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

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
    await page.setContent('<main id="board"></main>');
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-6, 6, 6, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = board.create("point", [0, 0]);
        const coords = new JXG.Coords(
            JXG.COORDS_BY_USER,
            [1, 1],
            board
        );
        const mixed = [
            point,
            coords,
            [2, 2],
            [2, 2],
            [2, 6, 8],
            [NaN, NaN]
        ];
        const path = JXG.Math.Clip._getPath(mixed, board).map((node) => ({
            position: node.pos,
            coordinates: node.coords.usrCoords.map((value) =>
                Number.isNaN(value) ? "NaN" : value
            )
        }));
        const subject = [
            [-3, -2],
            [2, -2],
            [2, 2],
            [-3, 2]
        ];
        const clip = [
            [-1, -3],
            [4, -3],
            [4, 1],
            [-1, 1]
        ];
        const operations = {};
        for (const operation of ["intersection", "union", "difference"]) {
            const result = JXG.Math.Clip[operation](
                subject,
                clip,
                board
            );
            operations[operation] = result.map((axis) =>
                axis.map((value) => Number.isNaN(value) ? "NaN" : value)
            );
        }
        return {
            version: JXG.version,
            path,
            operations
        };
    });

    process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
} finally {
    await browser.close();
}
