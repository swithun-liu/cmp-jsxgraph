/*
 * Official JSXGraph 1.13.3 reader registry and Graph reader fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-graph.mjs
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
            "third_party/jsxgraph-src/src/reader/graph.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const capture = (operation) => {
            try {
                return {value: operation()};
            } catch (cause) {
                return {
                    error: {
                        name: cause?.name ?? null,
                        message: cause?.message ?? null
                    }
                };
            }
        };
        const normalizeGraph = (graph) => ({
            n: graph.n,
            nodes: graph.nodes.map((node) => ({
                name: node.name,
                coords: node.coords.map((value) =>
                    Number.isNaN(value) ? "NaN" : value
                )
            })),
            adjMatrix: graph.adjMatrix.map((row) =>
                row.map((value) =>
                    value === Infinity ? "Infinity" : value
                )
            ),
            nodenumbers: graph.nodenumbers,
            weighted: graph.weighted,
            directed: graph.directed
        });
        const parseGraph = (source) => {
            const calls = [];
            const board = {
                setBoundingBox(boundingBox, keepAspectRatio) {
                    calls.push({boundingBox, keepAspectRatio});
                }
            };
            const reader = new JXG.GraphReader(board, source);
            return {
                graph: normalizeGraph(reader.parseData(false)),
                calls
            };
        };
        const drawGraph = (source) => {
            const board = JXG.JSXGraph.initBoard("board", {
                boundingbox: [-5, 5, 5, -5],
                axis: false,
                showNavigation: false,
                showCopyright: false
            });
            const randomValues = [0.25, 0.75];
            const originalRandom = Math.random;
            Math.random = () => randomValues.shift();
            try {
                const reader = new JXG.GraphReader(board, source);
                reader.read();
                const graph = board.addedGraph;
                const normalizeSegment = (segment) =>
                    segment === null
                        ? null
                        : {
                              edgeId: segment.edge.id,
                              weight:
                                  typeof segment.weight === "number"
                                      ? segment.weight
                                      : {
                                            id: segment.weight.id,
                                            plaintext:
                                                segment.weight.plaintext,
                                            coords: [
                                                segment.weight.X(),
                                                segment.weight.Y()
                                            ],
                                            anchorId:
                                                segment.weight.element.id
                                        },
                              straightFirst:
                                  segment.edge.visProp.straightfirst,
                              straightLast:
                                  segment.edge.visProp.straightlast,
                              firstArrow:
                                  segment.edge.visProp.firstarrow,
                              lastArrow:
                                  segment.edge.visProp.lastarrow,
                              endpoints: [
                                  segment.edge.point1.name,
                                  segment.edge.point2.name
                              ]
                          };
                return {
                    boundingBox: board.getBoundingBox(),
                    objectCount: Object.keys(board.objects).length,
                    nodes: graph.nodes.map((node) => ({
                        name: node.name,
                        id: node.reference.id,
                        coords: [
                            node.reference.X(),
                            node.reference.Y()
                        ]
                    })),
                    segments: graph.segments.map((row) =>
                        row.map(normalizeSegment)
                    ),
                    sharedEdges: [
                        graph.segments[0][1] === graph.segments[1][0],
                        graph.segments[1][2] === graph.segments[2][1]
                    ]
                };
            } finally {
                Math.random = originalRandom;
                JXG.JSXGraph.freeBoard(board);
            }
        };

        const registryEvents = [];
        const registryBoard = {};
        function FirstReader(board, source) {
            registryEvents.push(`first:new:${source}`);
            this.read = () => {
                registryEvents.push("first:read");
                board.reader = "first";
            };
        }
        function SecondReader(board, source) {
            registryEvents.push(`second:new:${source}`);
            this.read = () => {
                registryEvents.push("second:read");
                board.reader = "second";
            };
        }
        JXG.registerReader(FirstReader, ["FixtureReader"]);
        JXG.registerReader(SecondReader, ["fixturereader"]);
        JXG.FileReader.parseString(
            "payload",
            registryBoard,
            "FIXTUREREADER",
            () => registryEvents.push("callback")
        );
        JXG.FileReader.parseString(
            "ignored",
            registryBoard,
            "jessiecode",
            () => registryEvents.push("jessiecode:callback")
        );

        return {
            version: JXG.version,
            registry: {
                events: registryEvents,
                selected: registryBoard.reader,
                unknown: capture(() =>
                    JXG.FileReader.parseString(
                        "",
                        registryBoard,
                        "missing-reader"
                    )
                )
            },
            undirected: parseGraph([
                "-5 5 5 -5",
                "graph",
                "3",
                "A 1 2",
                "B",
                "C -3 4",
                "A B",
                "B C"
            ].join("\n")),
            drawnUndirected: drawGraph([
                "-10 10 10 -10",
                "graph",
                "3",
                "A 1 2",
                "B",
                "C -3 4",
                "A B",
                "B C"
            ].join("\n")),
            drawnDirectedWeighted: drawGraph([
                "-10 10 10 -10",
                "digraph",
                "3",
                "A 0 0",
                "B 2 0",
                "C 0 2",
                "A B 7",
                "B A 4",
                "B C -2"
            ].join("\n")),
            directedWeighted: parseGraph([
                "-10 10 10 -10",
                "digraph",
                "3",
                "A",
                "B 2 3",
                "C",
                "A B 7",
                "B C -2",
                "C A 0"
            ].join("\n")),
            parseIntPrefix: parseGraph([
                "-5x 5.9 5 -5",
                "graph",
                "1",
                "A 2x -3.8"
            ].join("\n")),
            malformedEdge: capture(() =>
                parseGraph([
                    "-5 5 5 -5",
                    "graph",
                    "1",
                    "A",
                    "A missing"
                ].join("\n"))
            )
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
