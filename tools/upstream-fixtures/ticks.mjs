/*
 * Official JSXGraph 1.13.3 two-dimensional Ticks behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/ticks.mjs
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
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width: 500px; height: 500px"></div>'
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
        const p1 = board.create("point", [-2, 0], {
            id: "p1",
            name: "",
            withLabel: false
        });
        const p2 = board.create("point", [2, 0], {
            id: "p2",
            name: "",
            withLabel: false
        });
        const line = board.create("segment", [p1, p2], {
            id: "line",
            name: "",
            withLabel: false
        });
        const fixedLine = board.create(
            "ticks",
            [line, [-10, -1, 0, 1, 10]],
            {
                id: "fixedLine",
                name: "",
                drawLabels: true,
                labels: ["out-left", "minus", "zero", "plus", "out-right"],
                majorHeight: 10,
                label: {offset: [10, 0]}
            }
        );
        const numericArgument = board.create(
            "ticks",
            [line, 2],
            {
                id: "numericArgument",
                name: "",
                drawLabels: true,
                drawZero: true,
                minorTicks: 0
            }
        );
        const curve = board.create(
            "functiongraph",
            [(x) => x * x, -2, 2],
            {
                id: "curve",
                name: "",
                withLabel: false,
                doAdvancedPlot: false,
                numberPointsHigh: 8
            }
        );
        const curveTicks = board.create(
            "ticks",
            [curve, [0, 1, 2, 4]],
            {
                id: "curveTicks",
                name: "",
                drawLabels: true,
                labels: ["left", "inside", "middle", "right"],
                majorHeight: 10
            }
        );
        const faceGreater = board.create(
            "ticks",
            [
                board.create("segment", [[-4, 3], [4, 3]], {
                    name: "",
                    visible: false
                }),
                [4]
            ],
            {
                name: "",
                face: ">",
                majorHeight: 20
            }
        );
        const faceLess = board.create(
            "ticks",
            [
                board.create("segment", [[-4, 2], [4, 2]], {
                    name: "",
                    visible: false
                }),
                [4]
            ],
            {
                name: "",
                face: "<",
                majorHeight: 20
            }
        );
        const finiteEndings = board.create(
            "ticks",
            [
                board.create("segment", [[-4, 1], [4, 1]], {
                    name: "",
                    visible: false
                }),
                [4]
            ],
            {
                name: "",
                majorHeight: 20,
                majorTickEndings: [2, -1]
            }
        );
        const infiniteOneSide = board.create(
            "ticks",
            [
                board.create("segment", [[-4, 0], [4, 0]], {
                    name: "",
                    visible: false
                }),
                [4]
            ],
            {
                name: "",
                majorHeight: -1,
                majorTickEndings: [1, 0],
                ignoreInfiniteTickEndings: false
            }
        );
        const infiniteBothSides = board.create(
            "ticks",
            [
                board.create("segment", [[-4, -1], [4, -1]], {
                    name: "",
                    visible: false
                }),
                [4]
            ],
            {
                name: "",
                majorHeight: -1,
                majorTickEndings: [1, 0],
                ignoreInfiniteTickEndings: true
            }
        );
        const polar = board.create(
            "ticks",
            [
                board.create("segment", [[0, 0], [1, 0]], {
                    name: "",
                    visible: false
                }),
                [1, 2, 8]
            ],
            {
                name: "",
                type: "polar"
            }
        );
        board.update();

        const initial = {
            fixedLine: snapshot(fixedLine),
            numericArgument: snapshot(numericArgument),
            curve: {
                minX: curve.minX(),
                maxX: curve.maxX(),
                points: curve.points.map((point) => point.usrCoords.slice())
            },
            curveTicks: snapshot(curveTicks),
            faceGreater: snapshot(faceGreater),
            faceLess: snapshot(faceLess),
            finiteEndings: snapshot(finiteEndings),
            infiniteOneSide: snapshot(infiniteOneSide),
            infiniteBothSides: snapshot(infiniteBothSides),
            polar: {
                pathCount: polar.ticks.length,
                paths: polar.ticks.map((tick) => ({
                    pointCount: tick[0].length,
                    first: [tick[0][0], tick[1][0]],
                    quarter: [tick[0][45], tick[1][45]],
                    half: [tick[0][90], tick[1][90]],
                    major: tick[2]
                }))
            }
        };

        p2.setPosition(JXG.COORDS_BY_USER, [2, 2]);
        board.update();
        const moved = snapshot(fixedLine);

        let functionArgument;
        try {
            board.create("ticks", [line, () => 1], {
                id: "functionArgument",
                name: ""
            });
            functionArgument = {threw: false};
        } catch (error) {
            functionArgument = {
                threw: true,
                name: error?.name ?? null,
                message: String(error?.message ?? error)
            };
        }

        function snapshot(ticks) {
            return {
                id: ticks.id,
                fixedTicks: ticks.fixedTicks,
                equidistant: ticks.equidistant,
                ticksDelta: ticks.ticksDelta,
                paths: ticks.ticks.map((tick) => ({
                    x: tick[0].slice(),
                    y: tick[1].slice(),
                    major: tick[2]
                })),
                labels: ticks.labelsData.map((label) =>
                    label === null
                        ? null
                        : {
                            x: label.x,
                            y: label.y,
                            text: label.t,
                            index: label.i
                        }
                )
            };
        }

        return {
            version: JXG.version,
            unitX: board.unitX,
            unitY: board.unitY,
            initial,
            moved,
            functionArgument
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
