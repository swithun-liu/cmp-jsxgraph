/*
 * Official JSXGraph 1.13.3 GEONExT composition-reader fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-geonext-composition.mjs
 */
import {readFileSync} from "node:fs";
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
const sources = [
    "third_party/jsxgraph-src/examples/gxt/blubb52.nc.gxt",
    "third_party/jsxgraph-src/examples/gxt/blubb31.nc.gxt"
].map((path) => ({
    path,
    source: readFileSync(resolve(repositoryRoot, path), "utf8")
}));

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent("<main></main>");
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

    const evidence = await page.evaluate((fixtures) => {
        const summarize = (element) => {
            if (!element) {
                return null;
            }
            const summary = {
                id: element.id,
                name: element.name,
                elType: element.elType,
                parents: element.parents,
                fixed: element.evalVisProp?.("fixed"),
                visible: element.evalVisProp?.("visible")
            };
            if (element.elementClass === JXG.OBJECT_CLASS_POINT) {
                summary.coords = [element.X(), element.Y()];
            }
            if (element.elementClass === JXG.OBJECT_CLASS_LINE) {
                summary.points = [element.point1.id, element.point2.id];
                summary.straightFirst =
                    element.evalVisProp("straightfirst");
                summary.straightLast =
                    element.evalVisProp("straightlast");
                summary.firstArrow = element.evalVisProp("firstarrow");
                summary.lastArrow = element.evalVisProp("lastarrow");
                summary.parallelpoint = element.parallelpoint?.id ?? null;
                summary.point = element.point?.id ?? null;
            }
            if (element.elementClass === JXG.OBJECT_CLASS_CIRCLE) {
                summary.center = element.center.id;
                summary.point2 = element.point2?.id ?? null;
                summary.radius = element.Radius();
            }
            if (element.elType === "sector") {
                summary.points = [
                    element.point1.id,
                    element.point2.id,
                    element.point3.id
                ];
                summary.radius = element.Radius();
            }
            return summary;
        };

        return fixtures.map((fixture, fixtureIndex) => {
            const host = document.createElement("div");
            host.id = `board-${fixtureIndex}`;
            host.style.width = "640px";
            host.style.height = "480px";
            document.body.appendChild(host);
            const board = JXG.JSXGraph.initBoard(host.id, {
                boundingbox: [-10, 10, 10, -10],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
            const reader = new JXG.GeonextReader(board, fixture.source);
            const nodes = Array.from(
                reader.tree
                    .getElementsByTagName("elements")[0]
                    .childNodes
            ).filter((node) => node.nodeType === 1);
            const compositions = [];
            const errors = [];
            for (let index = 0; index < nodes.length; index += 1) {
                const node = nodes[index];
                try {
                    reader.readNode(nodes, index, board);
                } catch (error) {
                    errors.push({
                        index,
                        element: node.nodeName,
                        id: node.getElementsByTagName("id")[0]
                            ?.textContent ?? null,
                        message: error.message
                    });
                    continue;
                }
                if (node.nodeName !== "composition") {
                    continue;
                }
                const data = node.getElementsByTagName("data")[0];
                const outputIds = Array.from(
                    node.getElementsByTagName("output")
                ).map((output) =>
                    output.getElementsByTagName("id")[0].textContent
                );
                compositions.push({
                    definitionId:
                        node.getElementsByTagName("id")[0].textContent,
                    type:
                        data.getElementsByTagName("type")[0].textContent,
                    inputs: Array.from(
                        data.getElementsByTagName("input")
                    ).map((input) => input.textContent),
                    outerObject:
                        summarize(
                            board.select(
                                node.getElementsByTagName("id")[0]
                                    .textContent
                            )
                        ),
                    outputs: outputIds.map((id) =>
                        summarize(board.select(id))
                    )
                });
            }
            return {
                path: fixture.path,
                compositions,
                errors
            };
        });
    }, sources);
    console.log(JSON.stringify({
        version: await page.evaluate(() => JXG.version),
        fixtures: evidence
    }, null, 2));
} finally {
    await browser.close();
}
