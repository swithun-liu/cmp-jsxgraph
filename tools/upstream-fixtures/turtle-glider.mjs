/*
 * Official JSXGraph 1.13.3 Turtle-backed Glider fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/turtle-glider.mjs
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
            boundingbox: [-5, 5, 16, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const turtle = board.create(
            "turtle",
            [[0, 0], 0],
            {id: "turtle", name: "", withLabel: false}
        );
        turtle.forward(4);
        turtle.penUp();
        turtle.moveTo([10, 0]);
        turtle.penDown();
        turtle.forward(4);

        const objectsBefore = Object.keys(board.objects);
        let error = null;
        try {
            board.create(
                "glider",
                [11, 2, turtle],
                {id: "glider", name: "", withLabel: false}
            );
        } catch (cause) {
            error = cause.message;
        }

        return {
            version: JXG.version,
            turtleId: turtle.id,
            boardHasTurtle:
                board.objects[turtle.id] === turtle,
            turtleMaxX: turtle.maxX(),
            error,
            objectIdsAdded:
                Object.keys(board.objects).filter(
                    (id) => !objectsBefore.includes(id)
                )
        };
    });

    process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
} finally {
    await browser.close();
}
