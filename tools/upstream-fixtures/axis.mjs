/*
 * Official JSXGraph 1.13.3 two-dimensional Axis behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/axis.mjs
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
        const horizontal = board.create(
            "axis",
            [[0, 0], [1, 0]],
            {
                id: "horizontal",
                position: "fixed",
                anchor: "right",
                anchorDist: "25px",
                ticksAutoPos: true,
                ticks: {
                    id: "horizontalTicks",
                    ticks: [-2, 0, 3],
                    ticksDistance: undefined,
                    minorTicks: 0
                }
            }
        );
        const vertical = board.create(
            "axis",
            [[0, 0], [0, 1]],
            {
                id: "vertical",
                position: "fixed",
                anchor: "right",
                anchorDist: "10%",
                ticksAutoPos: true,
                ticks: {
                    id: "verticalTicks",
                    ticksDistance: 10,
                    minorTicks: 0,
                    drawZero: true,
                    insertTicks: false,
                    label: {
                        anchorX: "right",
                        offset: [6, 0]
                    }
                }
            }
        );
        const sticky = board.create(
            "axis",
            [[0, 8], [1, 8]],
            {
                id: "sticky",
                position: "sticky",
                anchor: "left"
            }
        );
        board.fullUpdate();

        const snapshot = (axis) => ({
            id: axis.id,
            name: axis.name,
            type: axis.type,
            elType: axis.elType,
            needsRegularUpdate: axis.needsRegularUpdate,
            isDraggable: axis.isDraggable,
            point1: axis.point1.coords.usrCoords.slice(),
            point2: axis.point2.coords.usrCoords.slice(),
            originalPoint1: axis._point1UsrCoordsOrg.slice(),
            originalPoint2: axis._point2UsrCoordsOrg.slice(),
            pointTypes: [axis.point1.type, axis.point2.type],
            pointDraggable: [
                axis.point1.isDraggable,
                axis.point2.isDraggable
            ],
            defaultTicks: {
                id: axis.defaultTicks.id,
                dump: axis.defaultTicks.dump,
                fixedTicks: axis.defaultTicks.fixedTicks,
                ticksDistance: axis.defaultTicks.evalVisProp("ticksdistance"),
                drawLabels: axis.defaultTicks.evalVisProp("drawlabels"),
                insertTicks: axis.defaultTicks.evalVisProp("insertticks"),
                majorHeight: axis.defaultTicks.evalVisProp("majorheight"),
                minorHeight: axis.defaultTicks.evalVisProp("minorheight"),
                tickEndings: axis.defaultTicks.evalVisProp("tickendings"),
                label: {
                    anchorX:
                        axis.defaultTicks.evalVisProp("label.anchorx"),
                    anchorY:
                        axis.defaultTicks.evalVisProp("label.anchory"),
                    offset:
                        axis.defaultTicks.evalVisProp("label.offset")
                }
            },
            relationships: {
                subsTicks:
                    axis.subs.ticks === axis.defaultTicks,
                inheritsTicks:
                    axis.inherits.includes(axis.defaultTicks),
                registered:
                    board.objects[axis.id] === axis &&
                    board.objects[axis.defaultTicks.id] ===
                        axis.defaultTicks
            }
        });

        return {
            version: JXG.version,
            horizontal: snapshot(horizontal),
            vertical: snapshot(vertical),
            sticky: snapshot(sticky)
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
