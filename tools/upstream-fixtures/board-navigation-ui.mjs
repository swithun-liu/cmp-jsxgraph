/*
 * Official JSXGraph 1.13.3 Board navigation UI fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/board-navigation-ui.mjs
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
        '<div id="board" style="width: 500px; height: 400px"></div>'
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
            boundingbox: [-5, 4, 5, -4],
            axis: false,
            keepaspectratio: false,
            showCopyright: false,
            showNavigation: true,
            showZoom: true,
            showClearTraces: true,
            showFullscreen: true,
            showScreenshot: true,
            showReload: true,
            fullscreen: {symbol: "F"},
            screenshot: {symbol: "S"},
            navbar: {
                strokeColor: "#123456",
                fillColor: "#abcdef",
                highlightFillColor: "#654321",
                padding: "3px",
                position: "absolute",
                fontSize: "15px",
                cursor: "crosshair",
                zIndex: "101",
                right: "6px",
                bottom: "7px"
            }
        });
        const navigation = document.querySelector("#board_navigationbar");
        const buttons = Array.from(navigation.children);
        const snapshot = () => Array.from(board.getBoundingBox(), (value) =>
            Object.is(value, -0) ? 0 : value
        );
        const viewport = {
            initial: snapshot()
        };
        document.querySelector("#board_navigation_out").click();
        viewport.zoomOut = snapshot();
        document.querySelector("#board_navigation_100").click();
        viewport.zoomReset = snapshot();
        document.querySelector("#board_navigation_in").click();
        viewport.zoomIn = snapshot();
        document.querySelector("#board_navigation_100").click();
        document.querySelector("#board_navigation_left").click();
        viewport.left = snapshot();
        document.querySelector("#board_navigation_right").click();
        document.querySelector("#board_navigation_down").click();
        viewport.downGlyph = snapshot();
        document.querySelector("#board_navigation_up").click();
        viewport.upGlyph = snapshot();

        let clearCount = 0;
        board.clearTraces = () => {
            clearCount += 1;
            return board;
        };
        document.querySelector("#board_navigation_cleartraces").click();

        let bubbled = 0;
        document.querySelector("#board").addEventListener(
            "pointerdown",
            () => {
                bubbled += 1;
            }
        );
        document.querySelector("#board_navigation_left").dispatchEvent(
            new PointerEvent("pointerdown", {bubbles: true})
        );

        return {
            version: JXG.version,
            navigation: {
                id: navigation.id,
                className: navigation.className,
                style: {
                    color: navigation.style.color,
                    backgroundColor: navigation.style.backgroundColor,
                    padding: navigation.style.padding,
                    position: navigation.style.position,
                    fontSize: navigation.style.fontSize,
                    cursor: navigation.style.cursor,
                    zIndex: navigation.style.zIndex,
                    right: navigation.style.right,
                    bottom: navigation.style.bottom
                }
            },
            buttons: buttons.map((button) => ({
                id: button.id,
                type: button.id.replace("board_navigation_", ""),
                text: button.textContent,
                classes: Array.from(button.classList),
                ariaHidden: button.getAttribute("aria-hidden"),
                paddingLeft: button.style.paddingLeft,
                paddingRight: button.style.paddingRight
            })),
            viewport,
            clearCount,
            bubbled
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
