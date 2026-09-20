/*
 * Official JSXGraph 1.13.3 fixed-length Segment behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/segment-fixed-length.mjs
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
        const driver = board.create("point", [3, 0], {
            id: "driver",
            name: "Driver"
        });
        const stringSegment = board.create(
            "segment",
            [[0, 2], [2, 2], "Driver.X()"]
        );
        const functionSegment = board.create(
            "segment",
            [[0, 0], [2, 0], function () {
                return driver.X() + 1;
            }]
        );
        const clampedSegment = board.create(
            "segment",
            [[0, -2], [2, -2], function () {
                return driver.X() - 10;
            }],
            {nonnegativeOnly: true}
        );

        const snapshot = () => ({
            driverX: driver.X(),
            string: {
                point1: [stringSegment.point1.X(), stringSegment.point1.Y()],
                point2: [stringSegment.point2.X(), stringSegment.point2.Y()],
                length: stringSegment.L()
            },
            function: {
                point1: [
                    functionSegment.point1.X(),
                    functionSegment.point1.Y()
                ],
                point2: [
                    functionSegment.point2.X(),
                    functionSegment.point2.Y()
                ],
                length: functionSegment.L()
            },
            clamped: {
                point1: [
                    clampedSegment.point1.X(),
                    clampedSegment.point1.Y()
                ],
                point2: [
                    clampedSegment.point2.X(),
                    clampedSegment.point2.Y()
                ],
                length: clampedSegment.L()
            }
        });

        const initial = snapshot();
        driver.setPositionDirectly(JXG.COORDS_BY_USER, [12, 0]);
        board.update();
        const moved = snapshot();

        let throwing;
        const objectCountBeforeThrow = board.objectsList.length;
        try {
            board.create(
                "segment",
                [[0, -4], [2, -4], function () {
                    throw new Error("fixture failure");
                }]
            );
            throwing = {threw: false};
        } catch (error) {
            throwing = {
                threw: true,
                message: String(error.message),
                retainedObjects:
                    board.objectsList.length - objectCountBeforeThrow
            };
        }

        const nonNumeric = board.create(
            "segment",
            [[0, 4], [2, 4], function () {
                return "not-a-number";
            }]
        );

        return {
            version: JXG.version,
            initial,
            moved,
            throwing,
            nonNumeric: {
                point1: [nonNumeric.point1.X(), nonNumeric.point1.Y()],
                point2: [nonNumeric.point2.X(), nonNumeric.point2.Y()],
                length: nonNumeric.L()
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
