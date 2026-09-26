/*
 * Official JSXGraph 1.13.3 Hatch/Hash behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/hatch.mjs
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

        function segment(id, y) {
            return board.create("segment", [[-4, y], [4, y]], {
                id,
                name: "",
                withLabel: false
            });
        }

        const defaultParent = segment("defaultParent", 2);
        const defaultHatch = board.create("hatch", [defaultParent, 3], {
            id: "defaultHatch",
            name: ""
        });
        const shifted = board.create(
            "hatch",
            [segment("shiftedParent", 1), 2],
            {
                id: "shifted",
                name: "",
                anchor: 0.2,
                ticksDistance: 0.4,
                tickEndings: [1, 1],
                face: ">"
            }
        );
        const alias = board.create(
            "hash",
            [segment("aliasParent", 0), 2.5],
            {
                id: "alias",
                name: ""
            }
        );
        const zero = board.create(
            "hatch",
            [segment("zeroParent", -1), 0],
            {
                id: "zero",
                name: ""
            }
        );
        const negative = board.create(
            "hatch",
            [segment("negativeParent", -2), -3],
            {
                id: "negative",
                name: ""
            }
        );
        const nan = board.create(
            "hatch",
            [segment("nanParent", -3), Number.NaN],
            {
                id: "nan",
                name: ""
            }
        );
        board.update();

        const initial = {
            defaultHatch: snapshot(defaultHatch, defaultParent),
            shifted: snapshot(shifted, shifted.line),
            alias: snapshot(alias, alias.line),
            zero: snapshot(zero, zero.line),
            negative: snapshot(negative, negative.line),
            nan: snapshot(nan, nan.line)
        };

        defaultParent.point2.setPosition(
            JXG.COORDS_BY_USER,
            [4, 4]
        );
        board.update();
        const moved = snapshot(defaultHatch, defaultParent);

        const failures = {
            nonNumericCount: captureFailure(() =>
                board.create("hatch", [defaultParent, "3"], {
                    id: "nonNumericCount",
                    name: ""
                })
            ),
            unsupportedParent: captureFailure(() =>
                board.create("hatch", [defaultParent.point1, 2], {
                    id: "unsupportedParent",
                    name: ""
                })
            )
        };

        function captureFailure(block) {
            try {
                block();
                return {threw: false};
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        }

        function snapshot(hatch, parent) {
            return {
                id: hatch.id,
                elType: hatch.elType,
                type: hatch.type,
                fixedTicks: hatch.fixedTicks,
                equidistant: hatch.equidistant,
                ticksDistance: hatch.evalVisProp("ticksdistance"),
                anchor: hatch.evalVisProp("anchor"),
                drawZero: hatch.evalVisProp("drawzero"),
                drawLabels: hatch.evalVisProp("drawlabels"),
                majorHeight: hatch.evalVisProp("majorheight"),
                face: hatch.evalVisProp("face"),
                strokeWidth: hatch.evalVisProp("strokewidth"),
                strokeColor: hatch.evalVisProp("strokecolor"),
                inheritedByParent: parent.inherits.includes(hatch),
                paths: hatch.ticks.map((tick) => ({
                    x: tick[0].slice(),
                    y: tick[1].slice(),
                    major: tick[2]
                }))
            };
        }

        return {
            version: JXG.version,
            unitX: board.unitX,
            unitY: board.unitY,
            initial,
            moved,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
