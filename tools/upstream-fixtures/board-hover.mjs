/*
 * Official JSXGraph 1.13.3 Board hover lifecycle fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/board-hover.mjs
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
        '<style>html,body{margin:0;padding:0}</style>' +
            '<div id="board" style="width:500px;height:500px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(async () => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = board.create("point", [0, 0], {
            id: "point",
            name: "",
            withLabel: false,
            size: 8,
            highlight: true
        });
        const events = [];
        for (const eventName of ["over", "move", "out"]) {
            point.on(eventName, () => events.push(eventName));
        }
        const pointerEvent = (x, y, timeStamp) => ({
            clientX: x,
            clientY: y,
            screenX: x,
            screenY: y,
            pointerType: "mouse",
            timeStamp,
            target: board.containerObj,
            preventDefault() {},
            stopPropagation() {}
        });
        const nextFrame = () =>
            new Promise((resolveFrame) => setTimeout(resolveFrame, 30));
        const snapshot = () => ({
            highlighted: point.highlighted,
            mouseover: point.mouseover,
            highlightedIds: Object.keys(board.highlightedObjects),
            focusIds: [...board.focusObjects],
            events: [...events]
        });

        const center = [
            point.coords.scrCoords[1],
            point.coords.scrCoords[2]
        ];
        board.pointerMoveListener(pointerEvent(center[0], center[1], 100));
        const entered = snapshot();
        await nextFrame();
        board.pointerMoveListener(pointerEvent(center[0], center[1], 150));
        const moved = snapshot();
        await nextFrame();
        board.pointerMoveListener(pointerEvent(400, 400, 200));
        const exitedElement = snapshot();
        await nextFrame();
        board.pointerMoveListener(pointerEvent(center[0], center[1], 250));
        board.pointerLeaveListener(pointerEvent(center[0], center[1], 300));
        const leftBoard = snapshot();

        board.keyFocusInListener({
            target: {id: `${board.container}_${point.id}`}
        });
        board.dehighlightAll();
        const focused = snapshot();
        board.keyFocusOutListener({});
        const blurred = snapshot();

        return {
            version: JXG.version,
            entered,
            moved,
            exitedElement,
            leftBoard,
            focused,
            blurred
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
