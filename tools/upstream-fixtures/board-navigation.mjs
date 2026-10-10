/*
 * Official JSXGraph 1.13.3 Board viewport/navigation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/board-navigation.mjs
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
        const createBoard = (attributes = {}) => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = `${attributes.width ?? 500}px`;
            container.style.height = `${attributes.height ?? 400}px`;
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: attributes.boundingbox ?? [-5, 4, 5, -4],
                maxboundingbox: attributes.maxboundingbox ??
                    [-Infinity, Infinity, Infinity, -Infinity],
                keepaspectratio: attributes.keepaspectratio ?? false,
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const values = (array) => Array.from(array, (value) =>
            Object.is(value, -0) ? 0 : value
        );
        const snapshot = (board) => ({
            boundingBox: values(board.getBoundingBox()),
            origin: values(board.origin.scrCoords),
            unitX: board.unitX,
            unitY: board.unitY,
            zoomX: board.zoomX,
            zoomY: board.zoomY
        });

        const navigation = createBoard();
        const initial = snapshot(navigation);
        navigation.initMoveOrigin(250, 200);
        navigation.moveOrigin(300, 230, true);
        const panned = snapshot(navigation);
        navigation.setBoundingBox([-5, 4, 5, -4], false, "reset");
        navigation.zoomIn(2, 1);
        const zoomedAtPoint = snapshot(navigation);
        navigation.zoomOut(2, 1);
        const zoomedBack = snapshot(navigation);
        navigation.clickLeftArrow();
        const left = snapshot(navigation);
        navigation.clickRightArrow();
        navigation.clickUpArrow();
        const up = snapshot(navigation);
        navigation.clickDownArrow();
        const navigationReset = snapshot(navigation);

        const points = createBoard();
        points.create("point", [-7, 3], {name: "", visible: true});
        points.create("point", [4, -5], {name: "", visible: true});
        points.zoomAllPoints();
        const allPoints = snapshot(points);

        const elements = createBoard();
        const p1 = elements.create(
            "point",
            [-2, 1],
            {id: "p1", name: "", visible: true}
        );
        const p2 = elements.create(
            "point",
            [4, -3],
            {id: "p2", name: "", visible: true}
        );
        elements.zoomElements([p1, p2]);
        const selectedElements = snapshot(elements);

        const bounded = createBoard({
            maxboundingbox: [-6, 5, 6, -5]
        });
        bounded.setBoundingBox([-7, 4, 5, -4], false, "update");
        const rejected = snapshot(bounded);
        bounded.setBoundingBox([-4, 3, 4, -3], false, "update");
        const accepted = snapshot(bounded);

        const aspect = createBoard({
            width: 640,
            height: 480,
            boundingbox: [-8, 6, 8, -6],
            keepaspectratio: true
        });
        const keyboard = createBoard();
        const keyboardPoint = keyboard.create(
            "point",
            [0, 0],
            {id: "keyboardPoint", name: "", visible: true}
        );
        const keyEvent = (keyCode, modifiers = {}) => ({
            target: {
                id: `${keyboard.container}_${keyboardPoint.id}`
            },
            keyCode,
            shiftKey: modifiers.shiftKey ?? false,
            ctrlKey: modifiers.ctrlKey ?? false,
            preventDefault() {}
        });
        const keyboardResults = {
            rightHandled: keyboard.keyDownListener(keyEvent(39)),
            afterRight: values(keyboardPoint.coords.usrCoords)
        };
        keyboardResults.upHandled = keyboard.keyDownListener(keyEvent(38));
        keyboardResults.afterUp = values(keyboardPoint.coords.usrCoords);
        keyboardResults.panHandled = keyboard.keyDownListener(
            keyEvent(37, {shiftKey: true})
        );
        keyboardResults.afterPan = snapshot(keyboard);
        keyboardResults.zoomHandled = keyboard.keyDownListener(
            keyEvent(171, {shiftKey: true})
        );
        keyboardResults.afterZoom = snapshot(keyboard);
        keyboardResults.resetHandled = keyboard.keyDownListener(
            keyEvent(79, {shiftKey: true})
        );
        keyboardResults.afterReset = snapshot(keyboard);

        return {
            version: JXG.version,
            navigation: {
                initial,
                panned,
                zoomedAtPoint,
                zoomedBack,
                left,
                up,
                navigationReset
            },
            zoomToContent: {
                allPoints,
                selectedElements
            },
            maxBoundingBox: {
                rejected,
                accepted
            },
            keepAspectRatio: snapshot(aspect),
            keyboard: keyboardResults
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
