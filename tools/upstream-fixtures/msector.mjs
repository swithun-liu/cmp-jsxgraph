/*
 * Official JSXGraph 1.13.3 Msector registration fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/msector.mjs
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
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const first = board.create("point", [3, 2], {
            id: "first",
            name: ""
        });
        const vertex = board.create("point", [0, 0], {
            id: "vertex",
            name: ""
        });
        const third = board.create("point", [-1, 3], {
            id: "third",
            name: ""
        });
        const before = {
            objectCount: board.objectsList.length,
            objectIds: Object.keys(board.objects),
            registeredFactoryType: typeof JXG.elements.msector,
            exportedFactoryType: typeof JXG.createMsector
        };
        let resultType = null;
        let error = null;

        try {
            const result = board.create(
                "msector",
                [first, vertex, third, 0.2],
                {id: "msector", name: ""}
            );
            resultType = typeof result;
        } catch (cause) {
            error = {
                name: cause?.name ?? null,
                message: cause?.message ?? null
            };
        }

        return {
            version: JXG.version,
            before,
            after: {
                objectCount: board.objectsList.length,
                objectIds: Object.keys(board.objects)
            },
            resultType,
            error
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
