/*
 * Official JSXGraph 1.13.3 Geogebra property-reader fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-geogebra-properties.mjs
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
        '<main><div id="board" style="width:500px;height:500px"></div></main>'
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
        const reader = Object.create(JXG.GeogebraReader.prototype);
        const parseElement = (body) =>
            JXG.XML.parse(
                `<geogebra><construction><element>${body}</element>` +
                    "</construction></geogebra>"
            ).getElementsByTagName("element")[0];
        const transform = (body) => {
            const element = parseElement(body);
            const colored = reader.colorProperties(element, {});
            return reader.visualProperties(element, colored);
        };
        const rich = transform(
            "<objColor alpha=\"0.4\" r=\"5\" g=\"16\" b=\"255\"/>" +
                "<show object=\"false\" label=\"true\"/>" +
                "<pointSize val=\"7tail\"/><pointStyle val=\"4\"/>" +
                "<slopeTriangleSize val=\"3\"/>" +
                "<lineStyle thickness=\"5\" type=\"15\"/>" +
                "<labelOffset x=\"-2.5\" y=\"4\"/>" +
                "<trace val=\"true\"/><fix val=\"false\"/>"
        );
        const styleZero = transform(
            "<objColor alpha=\"0.25\" r=\"255\" g=\"0\" b=\"16\"/>" +
                "<pointStyle val=\"0\"/>"
        );
        const noColor = transform(
            "<show object=\"true\" label=\"false\"/>" +
                "<pointStyle val=\"2\"/><lineStyle thickness=\"0\" type=\"30\"/>"
        );
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        const anchor = board.create("point", [2, 3], {
            name: "A",
            withLabel: false
        });
        reader.board = board;
        const coordinateResult = (body) => {
            const output = {};
            const result = reader.coordinates(output, parseElement(body));
            if (result === false) {
                return false;
            }
            return {
                x: typeof result.x === "function" ? result.x() : result.x,
                y: typeof result.y === "function" ? result.y() : result.y,
                z: result.z
            };
        };
        const anchoredBody =
            "<startPoint exp=\"A\"/>" +
            "<labelOffset x=\"50\" y=\"25\"/>";
        const anchoredBefore = coordinateResult(anchoredBody);
        anchor.moveTo([4, -1], 0);
        board.update();
        const anchoredAfter = coordinateResult(anchoredBody);
        const coordinates = {
            direct: coordinateResult(
                "<coords x=\"4.5\" y=\"-2\" z=\"3\"/>" +
                    "<labelOffset x=\"50\" y=\"25\"/>"
            ),
            startPoint: coordinateResult(
                "<startPoint x=\"1\" y=\"2\" z=\"3\"/>"
            ),
            anchoredBefore,
            anchoredAfter,
            absoluteScreen: coordinateResult(
                "<absoluteScreenLocation x=\"300\" y=\"150\"/>" +
                    "<labelOffset x=\"50\" y=\"25\"/>"
            ),
            missing: coordinateResult("<show object=\"true\"/>")
        };
        reader.tree = JXG.XML.parse(
            "<geogebra><construction>" +
                "<element type=\"point\" label=\"A\"/>" +
                "<element type=\"numeric\" label=\"n\"/>" +
                "<element type=\"function\" label=\"f\"/>" +
                "<expression label=\"n\" exp=\"2+3\"/>" +
                "<expression label=\"f\" exp=\"x+1\"/>" +
                "</construction></geogebra>"
        );
        const lookup = (name, expression = false) => {
            const result = reader.getElement(name, expression);
            return result === false
                ? false
                : {
                      nodeName: result.nodeName,
                      label: result.getAttribute("label"),
                      exp: result.getAttribute("exp")
                  };
        };
        const attributes = {sentinel: 1};
        const lookupResults = {
            element: lookup("A"),
            expressionLabel: lookup("f", true),
            expressionValue: lookup("2+3", true),
            missing: lookup("missing"),
            boardPropertiesIdentity:
                reader.boardProperties(null, null, attributes) === attributes
        };
        JXG.JSXGraph.freeBoard(board);

        return {
            version: JXG.version,
            rich,
            styleZero,
            noColor,
            coordinates,
            lookup: lookupResults,
            vectors: [
                reader.isGGBVector([1, 2, 3]),
                reader.isGGBVector([0, 2, 3]),
                reader.isGGBVector([1, 2]),
                reader.isGGBVector("1,2,3")
            ]
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
