/*
 * Official JSXGraph 1.13.3 portable element-option behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/element-options.mjs
 */
import { createRequire } from "node:module";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const fixtureDirectory = dirname(fileURLToPath(import.meta.url));
const repositoryRoot = resolve(fixtureDirectory, "../..");
const require = createRequire(
    resolve(repositoryRoot, "tools/visual-parity/package.json")
);
const puppeteer = require("puppeteer");
const executablePath =
    process.env.PUPPETEER_EXECUTABLE_PATH ??
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<main>' +
            '<div id="board" style="width:500px;height:400px"></div>' +
            '<div id="canvas-board" style="width:500px;height:400px"></div>' +
        '</main>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const aliases = [
            "cross", "x",
            "circle", "o",
            "square", "[]",
            "plus", "+",
            "divide", "|",
            "minus", "-",
            "diamond", "<>",
            "diamond2", "<<>>",
            "triangleup", "A", "a", "^",
            "triangledown", "v",
            "triangleleft", "<",
            "triangleright", ">"
        ];
        const classify = (value) =>
            value === undefined ? "undefined" : value;
        const normalizedFaces = Object.fromEntries(
            aliases.map((alias) => [alias, JXG.normalizePointFace(alias)])
        );
        normalizedFaces.star = classify(JXG.normalizePointFace("star"));

        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 4, 5, -4],
            keepaspectratio: false,
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const point = (id, x, sizeUnit, zoom) =>
            board.create("point", [x, 0], {
                id,
                name: "",
                withLabel: false,
                face: "o",
                size: 2,
                sizeUnit,
                zoom,
                strokeWidth: 2
            });
        const points = {
            screenNoZoom: point("screen-no-zoom", -3, "screen", false),
            screenZoom: point("screen-zoom", -1, "screen", true),
            userNoZoom: point("user-no-zoom", 1, "user", false),
            userZoom: point("user-zoom", 3, "user", true)
        };
        const rotatedText = board.create("text", [0, 1, "rotated"], {
            id: "rotated-text",
            name: "",
            display: "internal",
            anchorX: "middle",
            anchorY: "middle",
            rotate: 90
        });
        const rotatedHtmlText = board.create(
            "text",
            [0, 2, "rotated html"],
            {
                id: "rotated-html-text",
                name: "",
                display: "html",
                anchorX: "middle",
                anchorY: "middle",
                rotate: 90
            }
        );
        const touchStart = board.create("point", [-2, -2], {
            id: "touch-start",
            name: "",
            withLabel: false,
            visible: true,
            size: 10,
            strokeWidth: 1
        });
        const touchEnd = board.create("point", [2, -2], {
            id: "touch-end",
            name: "",
            withLabel: false,
            visible: true,
            size: 6,
            strokeWidth: 2
        });
        const touchLine = board.create("segment", [touchStart, touchEnd], {
            id: "touch-line",
            margin: 25,
            firstArrow: {type: 7},
            lastArrow: {type: 7},
            touchFirstPoint: true,
            touchLastPoint: true
        });
        const radius = (pointValue) =>
            Number(pointValue.rendNode.getAttribute("rx"));
        const snapshot = () => ({
            unitX: board.unitX,
            unitY: board.unitY,
            zoomX: board.zoomX,
            zoomY: board.zoomY,
            radii: Object.fromEntries(
                Object.entries(points).map(([key, value]) => [
                    key,
                    radius(value)
                ])
            )
        });

        const initial = snapshot();
        const touchLineEndpoints = {
            x1: Number(touchLine.rendNode.getAttribute("x1")),
            y1: Number(touchLine.rendNode.getAttribute("y1")),
            x2: Number(touchLine.rendNode.getAttribute("x2")),
            y2: Number(touchLine.rendNode.getAttribute("y2"))
        };
        board.zoomIn();
        const zoomed = snapshot();
        const center = points.userZoom.coords.scrCoords;
        const hitTest = {
            at127: points.userZoom.hasPoint(center[1] + 127, center[2]),
            at129: points.userZoom.hasPoint(center[1] + 129, center[2])
        };
        const textTransform = board.renderer.joinTransforms(
            rotatedText,
            rotatedText.transformations
        );
        const handDrawingBoard = JXG.JSXGraph.initBoard("canvas-board", {
            boundingbox: [-5, 4, 5, -4],
            keepaspectratio: false,
            axis: false,
            renderer: "canvas",
            showCopyright: false,
            showNavigation: false
        });
        const handDrawingCurve = handDrawingBoard.create(
            "curve",
            [[-1, 1], [0, 0]],
            {
                id: "hand-drawing-curve",
                name: "",
                handDrawing: true,
                curveType: "plot",
                strokeWidth: 2,
                lineCap: "square"
            }
        );
        const context = handDrawingBoard.renderer.context;
        const randomValues = [
            0.0, 0.25, 0.5, 0.75,
            0.0, 0.25, 0.5, 0.75
        ];
        let randomCalls = 0;
        const canvasCalls = {
            moveTo: [],
            bezierCurveTo: []
        };
        const originalRandom = Math.random;
        const originalMoveTo = context.moveTo;
        const originalBezierCurveTo = context.bezierCurveTo;
        try {
            Math.random = () =>
                randomValues[randomCalls++ % randomValues.length];
            context.moveTo = (x, y) => {
                canvasCalls.moveTo.push([x, y]);
                originalMoveTo.call(context, x, y);
            };
            context.bezierCurveTo = (...values) => {
                canvasCalls.bezierCurveTo.push(values);
                originalBezierCurveTo.call(context, ...values);
            };
            handDrawingBoard.renderer.drawCurve(handDrawingCurve);
        } finally {
            Math.random = originalRandom;
            context.moveTo = originalMoveTo;
            context.bezierCurveTo = originalBezierCurveTo;
        }

        return {
            version: JXG.version,
            normalizedFaces,
            initial,
            zoomed,
            hitTest,
            touchLineEndpoints,
            textRotation: {
                value: rotatedText.evalVisProp("rotate"),
                transformationCount: rotatedText.transformations.length,
                screenMatrix: textTransform
            },
            htmlTextRotation: {
                value: rotatedHtmlText.evalVisProp("rotate"),
                transformationCount:
                    rotatedHtmlText.transformations.length,
                cssTransform: rotatedHtmlText.rendNode.style.transform,
                cssTransformOrigin:
                    rotatedHtmlText.rendNode.style.transformOrigin
            },
            handDrawing: {
                value: handDrawingCurve.evalVisProp("handdrawing"),
                numberPoints: handDrawingCurve.numberPoints,
                screenPoints: handDrawingCurve.points.map((value) => [
                    value.scrCoords[1],
                    value.scrCoords[2]
                ]),
                strokeWidth: handDrawingCurve.evalVisProp("strokewidth"),
                randomCalls,
                canvasCalls,
                lineCap: context.lineCap,
                lineJoin: context.lineJoin
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
