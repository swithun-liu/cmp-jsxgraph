/*
 * Official JSXGraph 1.13.3 SketchCurve behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/sketchcurve.mjs
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
        const container = document.createElement("div");
        container.id = "board";
        container.style.width = "640px";
        container.style.height = "480px";
        document.querySelector("#fixtures").appendChild(container);
        const board = JXG.JSXGraph.initBoard(container.id, {
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const snapshot = (curve) => ({
            id: curve.id,
            name: curve.name,
            elType: curve.elType,
            type: curve.type,
            elementClass: curve.elementClass,
            curveType: curve.evalVisProp("curvetype"),
            dataX: [...curve.dataX],
            dataY: [...curve.dataY],
            numberPoints: curve.numberPoints,
            parents: [...curve.parents],
            attributes: {
                visible: curve.evalVisProp("visible"),
                strokeColor: curve.evalVisProp("strokecolor"),
                highlight: curve.evalVisProp("highlight"),
                strokeWidth: curve.evalVisProp("strokewidth"),
                lineCap: curve.evalVisProp("linecap"),
                deleteOnUp: curve.evalVisProp("deleteonup"),
                maxLength: curve.evalVisProp("maxlength")
            }
        });

        const defaults = board.create(
            "sketchcurve",
            [],
            {id: "defaults", name: ""}
        );
        const custom = board.create(
            "sketchcurve",
            [1, 2, 3],
            {
                id: "custom",
                name: "ignored-parents",
                strokeColor: "#123456",
                strokeWidth: 4,
                visible: false,
                deleteOnUp: true,
                maxLength: 2
            }
        );
        custom.dataX.push(1, 2, 3);
        custom.dataY.push(4, 5, 6);
        custom.prepareUpdate().update();

        return {
            version: JXG.version,
            defaults: snapshot(defaults),
            custom: snapshot(custom)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
