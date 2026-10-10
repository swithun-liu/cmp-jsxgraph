/*
 * Official JSXGraph 1.13.3 GEONExT CAS-point fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-geonext-cas.mjs
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
            "third_party/jsxgraph-src/src/reader/geonext.js"
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
            "<GEONEXT><elements>",
            "<cas><name>Q</name><id>CAS1</id>",
            "<data><x>-2.5</x><y>3.5</y></data>",
            "<fix>true</fix><visible>true</visible><trace>false</trace>",
            "</cas></elements></GEONEXT>"
        ].join("");
        const reader = Object.create(JXG.GeonextReader.prototype);
        const tree = JXG.XML.parse(reader.prepareString(source));
        const nodes = Array.from(
            tree.getElementsByTagName("elements")[0].childNodes
        ).filter((node) => node.nodeType === 1);
        reader.readNode(nodes, 0, board);
        const point = board.select("CAS1");

        return {
            version: JXG.version,
            nodeName: nodes[0].nodeName,
            id: point.id,
            name: point.name,
            elementClass: point.elementClass,
            type: point.type,
            coordinates: [point.X(), point.Y()],
            fixed: point.evalVisProp("fixed"),
            strokeColor: point.evalVisProp("strokecolor"),
            fillColor: point.evalVisProp("fillcolor"),
            withLabel: point.evalVisProp("withlabel")
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
