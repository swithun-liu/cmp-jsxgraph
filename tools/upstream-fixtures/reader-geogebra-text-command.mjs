/*
 * Official JSXGraph 1.13.3 Geogebra Text-command fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-geogebra-text-command.mjs
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
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "third_party/jsxgraph-src/src/reader/geogebra.js"
        )
    });

    const evidence = await page.evaluate(() => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 4, 5, -4],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const source = [
            '<geogebra format="5.0">',
            "<euclidianView><coordSystem ",
            'xZero="250" yZero="200" scale="50" yscale="50"/>',
            '<evSettings grid="false" axes="false"/>',
            "</euclidianView><kernel><decimals val=\"4\"/></kernel>",
            "<construction>",
            '<expression label="T" exp="&quot;hello&quot;"/>',
            '<command name="Text"><input a0="1"/><output a0="T"/>',
            "</command>",
            '<element type="text" label="T">',
            '<coords x="1.25" y="-0.5" z="1"/>',
            "</element></construction></geogebra>"
        ].join("");
        const reader = new JXG.GeogebraReader(board, source);
        reader.read();
        const text = board.select("T");

        return {
            version: JXG.version,
            id: text.id,
            name: text.name,
            type: text.type,
            elementClass: text.elementClass,
            coordinates: [text.X(), text.Y()],
            plaintext: text.plaintext
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
