/*
 * Official JSXGraph 1.13.3 SmartLabel behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/smartlabel.mjs
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
    await page.setContent('<main id="fixtures"></main>');
    await page.addStyleTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraph-1.13.3.css"
        )
    });
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(async () => {
        let boardIndex = 0;

        const createBoard = (width = 640, height = 480) => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.className = "jxgbox";
            container.style.width = `${width}px`;
            container.style.height = `${height}px`;
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors),
            descendants: Object.keys(element.descendants)
        });
        const settle = async (board) => {
            await new Promise((resolve) => setTimeout(resolve, 30));
            board.update();
            await new Promise((resolve) =>
                requestAnimationFrame(() => requestAnimationFrame(resolve))
            );
        };
        const rounded = (value) =>
            typeof value === "number" && Number.isFinite(value)
                ? Number(value.toFixed(9))
                : value;
        const evaluated = (element, name) => {
            try {
                return element.evalVisProp(name);
            } catch (error) {
                return `error: ${error.message}`;
            }
        };
        const styleSnapshot = (element) => {
            const node = element.rendNode;
            if (!node) {
                return null;
            }
            const style = getComputedStyle(node);
            const rect = node.getBoundingClientRect();
            return {
                className:
                    typeof node.className === "string"
                        ? node.className
                        : node.className?.baseVal ?? "",
                innerHTML: node.innerHTML,
                textContent: node.textContent,
                color: style.color,
                backgroundColor: style.backgroundColor,
                borderColor: style.borderColor,
                borderWidth: style.borderWidth,
                borderRadius: style.borderRadius,
                padding: style.padding,
                marginTop: style.marginTop,
                display: style.display,
                transform: style.transform,
                width: rounded(rect.width),
                height: rounded(rect.height)
            };
        };
        const snapshot = (label) => ({
            id: label.id,
            type: label.type,
            elType: label.elType,
            plaintext: label.plaintext,
            coordinates: label.coords.usrCoords.map(rounded),
            screenCoordinates: label.coords.scrCoords.map(rounded),
            value: Array.isArray(label.Value())
                ? label.Value().map(rounded)
                : rounded(label.Value()),
            dimension: label.Dimension(),
            unit: label.Unit(),
            unitArray: label.Unit([0, 1, 2]),
            measure: evaluated(label, "measure"),
            orientation: evaluated(label, "orientation"),
            rotate: rounded(evaluated(label, "rotate")),
            anchorX: evaluated(label, "anchorx"),
            anchorY: evaluated(label, "anchory"),
            visible: evaluated(label, "visible"),
            visibleCalculated: label.visPropCalc.visible,
            size: label.getSize().map(rounded),
            parent: label.parent?.id,
            parentObject: label.parentObject?.id,
            relations: relations(label),
            style: styleSnapshot(label)
        });

        const board = createBoard();
        const dynamic = {
            prefix: "P=",
            suffix: "!",
            unit: "cm",
            digits: 1,
            userText: "",
            pointMeasure: "coords",
            angleMeasure: "deg",
            manualMeasureCalls: 0
        };
        const point = board.create("point", [-5, 3], {
            id: "point",
            name: "",
            withLabel: false
        });
        const lineStart = board.create("point", [-2, 4], {
            id: "lineStart",
            name: "",
            withLabel: false
        });
        const lineEnd = board.create("point", [4, 1], {
            id: "lineEnd",
            name: "",
            withLabel: false
        });
        const line = board.create("segment", [lineStart, lineEnd], {
            id: "line",
            name: "",
            withLabel: false
        });
        const circleCenter = board.create("point", [-3, -1], {
            id: "circleCenter",
            name: "",
            withLabel: false
        });
        const circlePoint = board.create("point", [-1, -1], {
            id: "circlePoint",
            name: "",
            withLabel: false
        });
        const circle = board.create("circle", [circleCenter, circlePoint], {
            id: "circle",
            name: "c",
            withLabel: true,
            label: {visible: false}
        });
        const polygon = board.create(
            "polygon",
            [[1, -1], [5, -1], [4, -4], [1, -3]],
            {
                id: "polygon",
                name: "",
                withLabel: false,
                vertices: {withLabel: false}
            }
        );
        const angle = board.create(
            "angle",
            [[2, 5], [2, 2], [5, 2]],
            {
                id: "angle",
                name: "",
                withLabel: false,
                radius: 1.25
            }
        );
        const pointLabel = board.create(
            "smartlabel",
            [point],
            {
                id: "pointLabel",
                name: "",
                prefix: () => dynamic.prefix,
                suffix: () => dynamic.suffix,
                showPrefix: () => dynamic.prefix !== "",
                showSuffix: () => dynamic.suffix !== "",
                baseUnit: () => dynamic.unit,
                digits: () => dynamic.digits,
                useMathJax: false
            }
        );
        const pointDynamicMeasure = board.create(
            "smartlabel",
            [point],
            {
                id: "pointDynamicMeasure",
                name: "",
                measure: () => dynamic.pointMeasure,
                prefix: "dynamic=",
                useMathJax: false
            }
        );
        const pointColumn = board.create(
            "smartlabel",
            [point],
            {
                id: "pointColumn",
                name: "",
                prefix: "column=",
                baseUnit: "cm",
                units: "ignored",
                formatValue: "ignored",
                dir: "column",
                useMathJax: false
            }
        );
        const pointManual = board.create(
            "smartlabel",
            [point, "manual"],
            {
                id: "pointManual",
                name: "",
                measure: () => {
                    dynamic.manualMeasureCalls += 1;
                    return "coords";
                },
                useMathJax: false
            }
        );
        const lineLength = board.create(
            "smartlabel",
            [line],
            {
                id: "lineLength",
                name: "",
                baseUnit: "m",
                prefix: "L=",
                orientation: "parallel",
                useMathJax: false
            }
        );
        const lineSlope = board.create(
            "smartlabel",
            [line],
            {
                id: "lineSlope",
                name: "",
                measure: "slope",
                prefix: "s=",
                orientation: "orthogonal-inverted",
                useMathJax: false
            }
        );
        const circleRadius = board.create(
            "smartlabel",
            [circle],
            {
                id: "circleRadius",
                name: "",
                baseUnit: "cm",
                prefix: "r=",
                useMathJax: false
            }
        );
        const circleArea = board.create(
            "smartlabel",
            [circle],
            {
                id: "circleArea",
                name: "",
                measure: "area",
                units: {dim2: "sq"},
                prefix: "A=",
                cssClass: "smart-label-outline smart-label-circle",
                highlightCssClass:
                    "smart-label-outline smart-label-circle",
                useMathJax: false
            }
        );
        const circlePerimeter = board.create(
            "smartlabel",
            [circle],
            {
                id: "circlePerimeter",
                name: "",
                measure: "perimeter",
                prefix: "U=",
                cssClass: "smart-label-pure smart-label-circle",
                highlightCssClass:
                    "smart-label-pure smart-label-circle",
                useMathJax: false
            }
        );
        const polygonArea = board.create(
            "smartlabel",
            [polygon],
            {
                id: "polygonArea",
                name: "",
                baseUnit: "m",
                prefix: "A=",
                useMathJax: false
            }
        );
        const polygonPerimeter = board.create(
            "smartlabel",
            [polygon],
            {
                id: "polygonPerimeter",
                name: "",
                measure: "perimeter",
                prefix: "P=",
                useMathJax: false
            }
        );
        const angleDegrees = board.create(
            "smartlabel",
            [angle],
            {
                id: "angleDegrees",
                name: "",
                measure: "deg",
                baseUnit: "°",
                prefix: "a=",
                useMathJax: false
            }
        );
        const angleRadians = board.create(
            "smartlabel",
            [angle],
            {
                id: "angleRadians",
                name: "",
                measure: "rad",
                prefix: "rad=",
                useMathJax: false
            }
        );
        const angleDynamicMeasure = board.create(
            "smartlabel",
            [angle],
            {
                id: "angleDynamicMeasure",
                name: "",
                measure: () => dynamic.angleMeasure,
                prefix: "dynamicAngle=",
                useMathJax: false
            }
        );
        const userText = board.create(
            "smartlabel",
            [line, () => dynamic.userText],
            {
                id: "userText",
                name: "",
                measure: "length",
                prefix: "fallback=",
                orientation: "none",
                useMathJax: false
            }
        );
        board.update();
        await settle(board);

        const labels = [
            pointLabel,
            lineLength,
            lineSlope,
            circleRadius,
            circleArea,
            circlePerimeter,
            polygonArea,
            polygonPerimeter,
            angleDegrees,
            angleRadians,
            angleDynamicMeasure,
            pointDynamicMeasure,
            pointColumn,
            pointManual,
            userText
        ];
        const creationSemantics = {
            manualMeasureCalls: dynamic.manualMeasureCalls,
            manualText: pointManual.plaintext
        };
        const initial = labels.map(snapshot);
        point.setPosition(JXG.COORDS_BY_USER, [-4, 2]);
        lineEnd.setPosition(JXG.COORDS_BY_USER, [5, -2]);
        circlePoint.setPosition(JXG.COORDS_BY_USER, [0, -1]);
        polygon.vertices[1].setPosition(JXG.COORDS_BY_USER, [6, -1]);
        angle.point3.setPosition(JXG.COORDS_BY_USER, [0, 2]);
        dynamic.prefix = "";
        dynamic.suffix = "";
        dynamic.unit = "mm";
        dynamic.digits = 2;
        dynamic.userText = "manual";
        dynamic.pointMeasure = "unknown";
        dynamic.angleMeasure = "rad";
        board.update();
        await settle(board);
        const moved = labels.map(snapshot);

        const shortBoard = createBoard(320, 240);
        const shortLine = shortBoard.create(
            "segment",
            [[0, 0], [0.4, 0]],
            {id: "shortLine", name: "", withLabel: false}
        );
        const shortParallel = shortBoard.create(
            "smartlabel",
            [shortLine],
            {
                id: "shortParallel",
                name: "",
                prefix: "length=",
                orientation: "parallel",
                useMathJax: false
            }
        );
        const shortNone = shortBoard.create(
            "smartlabel",
            [shortLine],
            {
                id: "shortNone",
                name: "",
                prefix: "length=",
                orientation: "none",
                useMathJax: false
            }
        );
        const shortCircleCenter = shortBoard.create(
            "point",
            [3, 0],
            {
                id: "shortCircleCenter",
                name: "",
                visible: false,
                withLabel: false
            }
        );
        const shortCirclePoint = shortBoard.create(
            "point",
            [3.2, 0],
            {
                id: "shortCirclePoint",
                name: "",
                visible: false,
                withLabel: false
            }
        );
        const shortCircle = shortBoard.create(
            "circle",
            [shortCircleCenter, shortCirclePoint],
            {id: "shortCircle", name: "d", withLabel: true}
        );
        const shortCircleLabel = shortBoard.create(
            "smartlabel",
            [shortCircle],
            {
                id: "shortCircleLabel",
                name: "",
                prefix: "radius=",
                useMathJax: false
            }
        );
        shortBoard.update();
        await settle(shortBoard);

        const failures = {};
        for (const [name, parentFactory, attributes] of [
            ["missing", () => [], {}],
            ["text", (errorBoard) => [
                errorBoard.create("text", [0, 0, "x"], {
                    id: "plainText",
                    name: ""
                })
            ], {}],
            ["lineMeasure", (errorBoard) => [
                errorBoard.create(
                    "segment",
                    [[-1, 0], [1, 0]],
                    {id: "badLine", name: ""}
                )
            ], {measure: "area"}],
            ["lineFunctionMeasure", (errorBoard) => [
                errorBoard.create(
                    "segment",
                    [[-1, 0], [1, 0]],
                    {id: "dynamicLine", name: ""}
                )
            ], {measure: () => "length"}]
        ]) {
            const errorBoard = createBoard(120, 120);
            const parents = parentFactory(errorBoard);
            const before = errorBoard.objectsList.map(
                (element) => element.id
            );
            try {
                const result = errorBoard.create(
                    "smartlabel",
                    parents,
                    {id: `failure-${name}`, name: "", ...attributes}
                );
                failures[name] = {
                    error: "no error",
                    result:
                        result === null
                            ? null
                            : {
                                id: result?.id,
                                elType: result?.elType
                            },
                    before,
                    after: errorBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            } catch (error) {
                failures[name] = {
                    error: String(error.message),
                    before,
                    after: errorBoard.objectsList.map(
                        (element) => element.id
                    )
                };
            }
        }

        const removalBefore = {
            parent: relations(point),
            label: relations(pointLabel),
            objectOrder: board.objectsList.map((element) => element.id)
        };
        board.removeObject(pointLabel);
        const removalAfter = {
            parent: relations(point),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        return {
            version: JXG.version,
            creationSemantics,
            initial,
            moved,
            thresholds: [
                shortParallel,
                shortNone,
                shortCircleLabel
            ].map(snapshot),
            methodMap: {
                entries: Object.fromEntries(
                    [
                        "Value",
                        "V",
                        "Dimension",
                        "Unit",
                        "parent",
                        "parentObject"
                    ].map((key) => [key, pointLabel.methodMap[key]])
                ),
                value:
                    pointLabel[
                        pointLabel.methodMap.V
                    ](),
                dimension: pointLabel.Dimension(),
                unit: pointLabel.Unit(),
                parent: pointLabel.parent?.id,
                parentObject: pointLabel.parentObject?.id
            },
            failures,
            removal: {
                before: removalBefore,
                after: removalAfter
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
