/*
 * Official JSXGraph 1.13.3 Point.isOn fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/point-ison.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const visualParityPackage = resolve(
    repositoryRoot,
    "tools/visual-parity/package.json"
);
const require = createRequire(visualParityPackage);
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
        '<main id="board" style="width:500px;height:500px"></main>'
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
        const start = board.create("point", [0, 0]);
        const end = board.create("point", [2, 0]);
        const segment = board.create("segment", [start, end]);
        const line = board.create("line", [start, end]);
        const points = {
            start,
            end,
            inside: board.create("point", [1, 0.1]),
            atTolerance: board.create("point", [1, 0.25]),
            beforeStart: board.create("point", [-1, 0]),
            afterEnd: board.create("point", [3, 0])
        };
        const result = {};
        for (const [name, point] of Object.entries(points)) {
            result[name] = {
                segment: point.isOn(segment, 0.25),
                line: point.isOn(line, 0.25)
            };
        }
        const alwaysIntersectPoint = board.create(
            "point",
            [-1, 0],
            {alwaysIntersect: true}
        );
        const innerCircle = board.create(
            "circle",
            [start, end],
            {hasInnerPoints: true}
        );
        const polygon = board.create(
            "polygon",
            [[0, 0], [2, 0], [2, 2], [0, 2]],
            {hasInnerPoints: false}
        );
        const innerPolygon = board.create(
            "polygon",
            [[0, 0], [2, 0], [2, 2], [0, 2]],
            {hasInnerPoints: true}
        );
        const visualPropertyResult = {
            alwaysIntersectSegment:
                alwaysIntersectPoint.isOn(segment, 0.25),
            circleInterior:
                board.create("point", [1, 0]).isOn(innerCircle, 0.25),
            circleOutsideWithinTolerance:
                board.create("point", [2.249, 0])
                    .isOn(innerCircle, 0.25),
            circleAtTolerance:
                board.create("point", [2.25, 0])
                    .isOn(innerCircle, 0.25),
            polygonInterior:
                board.create("point", [1, 1])
                    .isOn(innerPolygon, 0.25),
            polygonOutside:
                board.create("point", [3, 1])
                    .isOn(innerPolygon, 0.25),
            polygonHasInnerPoints:
                innerPolygon.evalVisProp("hasinnerpoints"),
            polygonDirectInterior:
                innerPolygon.pnpoly(1, 1, JXG.COORDS_BY_USER)
        };
        const polygonResult = {};
        for (const [name, coordinates] of Object.entries({
            boundary: [1, 0],
            nearBoundary: [1, 0.1],
            interior: [1, 1],
            outside: [3, 1]
        })) {
            polygonResult[name] =
                board.create("point", coordinates).isOn(polygon, 0.25);
        }
        const dataPlot = board.create(
            "curve",
            [[-2, 0, 2], [0, 0, 0]]
        );
        const functionGraph = board.create(
            "functiongraph",
            [() => 0, -2, 2]
        );
        const curveResult = {};
        for (const [name, coordinates] of Object.entries({
            exact: [1, 0],
            near: [1, 0.249],
            atTolerance: [1, 0.25],
            outsideDomain: [3, 0]
        })) {
            const point = board.create("point", coordinates);
            curveResult[name] = {
                dataPlot: point.isOn(dataPlot, 0.25),
                functionGraph: point.isOn(functionGraph, 0.25)
            };
        }
        const scale = board.create(
            "transform",
            [2, 1],
            {type: "scale"}
        );
        const translate = board.create(
            "transform",
            [1, 2],
            {type: "translate"}
        );
        const transformedCurve = board.create(
            "curve",
            [dataPlot, [scale, translate]]
        );
        const transformedCurveResult = {};
        for (const [name, coordinates] of Object.entries({
            exact: [-1, 2],
            near: [-1, 2.249],
            atTolerance: [-1, 2.25],
            geometricOnly: [1, 2],
            sourceOnly: [1, 0]
        })) {
            transformedCurveResult[name] =
                board.create("point", coordinates)
                    .isOn(transformedCurve, 0.25);
        }
        const turtle = board.create("turtle", [[0, 0], 0]);
        turtle.forward(2);
        turtle.left(90);
        turtle.forward(2);
        const safeIsOn = (point, element, tolerance) => {
            try {
                return {
                    returned: point.isOn(element, tolerance),
                    error: null
                };
            } catch (error) {
                return {
                    returned: null,
                    error: String(error)
                };
            }
        };
        const turtleResult = {};
        for (const [name, coordinates] of Object.entries({
            firstSegment: [1, 0],
            nearFirstSegment: [1, 0.199],
            atTolerance: [1, 0.2],
            elbowSegment: [2, 1],
            outside: [3, 1]
        })) {
            turtleResult[name] = safeIsOn(
                board.create("point", coordinates),
                turtle,
                0.2
            );
        }
        const turtleProbe = board.create("point", [1, 0]);
        let turtleDirectProjection;
        try {
            const projection = JXG.Math.Geometry.projectPointToCurve(
                turtleProbe,
                turtle.objects[0],
                board
            );
            turtleDirectProjection = {
                coordinates: projection[0].usrCoords,
                parameter: projection[1]
            };
        } catch (error) {
            turtleDirectProjection = {error: String(error)};
        }
        return {
            version: JXG.version,
            result,
            visualPropertyResult,
            polygonResult,
            curveResult,
            transformedCurveResult,
            turtleResult,
            turtleDirectProjection,
            turtleObjects: turtle.objects.map((element) => ({
                type: element.type,
                elementClass: element.elementClass,
                numberPoints: element.numberPoints ?? null
            }))
        };
    });

    process.stdout.write(`${JSON.stringify(evidence, null, 2)}\n`);
} finally {
    await browser.close();
}
