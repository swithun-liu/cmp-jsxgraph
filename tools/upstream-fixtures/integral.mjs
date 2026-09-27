/*
 * Official JSXGraph 1.13.3 Integral behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/integral.mjs
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
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        let boardIndex = 0;

        const createBoard = () => {
            const id = `board-${boardIndex++}`;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 6, 8, -6],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const classify = (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (value === undefined) {
                return "undefined";
            }
            return value;
        };
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const pointSnapshot = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            coordinates: point.coords.usrCoords.map(classify),
            position: classify(point.position),
            visible: point.evalVisProp("visible"),
            fixed: point.evalVisProp("fixed"),
            withLabel: point.evalVisProp("withlabel"),
            size: point.evalVisProp("size"),
            strokeColor: point.evalVisProp("strokecolor"),
            fillColor: point.evalVisProp("fillcolor"),
            fillOpacity: point.evalVisProp("fillopacity"),
            layer: point.evalVisProp("layer"),
            dump: point.dump,
            relations: relations(point)
        });
        const curveCoordinates = (curve) =>
            curve.dataX.map((x, index) => ({
                x: classify(x),
                y: classify(curve.dataY[index])
            }));
        const snapshot = (integral) => ({
            id: integral.id,
            elType: integral.elType,
            type: integral.type,
            elementClass: integral.elementClass,
            curveType: integral.evalVisProp("curvetype"),
            axis: integral.evalVisProp("axis"),
            withLabel: integral.evalVisProp("withlabel"),
            fixed: integral.evalVisProp("fixed"),
            strokeWidth: integral.evalVisProp("strokewidth"),
            strokeOpacity: integral.evalVisProp("strokeopacity"),
            fillColor: integral.evalVisProp("fillcolor"),
            fillOpacity: integral.evalVisProp("fillopacity"),
            numberPoints: integral.numberPoints,
            value: classify(integral.Value()),
            coordinates: curveCoordinates(integral),
            parents: [...integral.parents],
            inherits: integral.inherits.map((element) => element?.id ?? null),
            subs: Object.fromEntries(
                Object.entries(integral.subs).map(([key, value]) => [
                    key,
                    value?.id ?? null
                ])
            ),
            curveLeft: pointSnapshot(integral.curveLeft),
            baseLeft: pointSnapshot(integral.baseLeft),
            curveRight: pointSnapshot(integral.curveRight),
            baseRight: pointSnapshot(integral.baseRight),
            label: integral.label
                ? {
                    id: integral.label.id,
                    elType: integral.label.elType,
                    coordinates:
                        integral.label.coords.usrCoords.map(classify),
                    plaintext: integral.label.plaintext,
                    visible: integral.label.evalVisProp("visible"),
                    fontSize: integral.label.evalVisProp("fontsize"),
                    digits: integral.label.evalVisProp("digits"),
                    offset: integral.label.evalVisProp("offset"),
                    dump: integral.label.dump,
                    relations: relations(integral.label)
                }
                : null,
            relations: relations(integral),
            objectOrder: integral.board.objectsList.map(
                (element) => element.id
            )
        });
        const captureError = (parents, attributes = {}) => {
            const board = createBoard();
            const curve = board.create(
                "functiongraph",
                [(x) => x * x + 1, -4, 4],
                {id: "curve", name: "", withLabel: false}
            );
            const resolvedParents = parents(curve);
            const before = board.objectsList.map((element) => element.id);
            try {
                const value = board.create(
                    "integral",
                    resolvedParents,
                    {
                        id: "candidate",
                        name: "",
                        withLabel: false,
                        ...attributes
                    }
                );
                return {
                    threw: false,
                    before,
                    after: board.objectsList.map((element) => element.id),
                    value: snapshot(value)
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error),
                    before,
                    after: board.objectsList.map((element) => element.id)
                };
            }
        };

        const basicBoard = createBoard();
        const basicCurve = basicBoard.create(
            "functiongraph",
            [(x) => x * x - 2, -5, 5],
            {id: "basicCurve", name: "", withLabel: false}
        );
        const basic = basicBoard.create(
            "integral",
            [[-2, 3], basicCurve],
            {
                id: "basic",
                name: "",
                curveLeft: {id: "basicCurveLeft", name: ""},
                baseLeft: {id: "basicBaseLeft", name: ""},
                curveRight: {id: "basicCurveRight", name: ""},
                baseRight: {id: "basicBaseRight", name: ""},
                label: {
                    id: "basicLabel",
                    name: "",
                    offset: [12, -8]
                }
            }
        );
        basicBoard.update();

        const reverseBoard = createBoard();
        const reverseCurve = reverseBoard.create(
            "functiongraph",
            [(x) => x + 1, -5, 5],
            {id: "reverseCurve", name: "", withLabel: false}
        );
        const reverse = reverseBoard.create(
            "integral",
            [reverseCurve, [3, -2]],
            {
                id: "reverse",
                name: "",
                withLabel: false
            }
        );
        reverseBoard.update();

        const dynamicBoard = createBoard();
        const dynamicCurve = dynamicBoard.create(
            "functiongraph",
            [(x) => Math.cos(x) * x, -5, 5],
            {id: "dynamicCurve", name: "", withLabel: false}
        );
        let start = -2;
        let end = 2;
        const dynamic = dynamicBoard.create(
            "integral",
            [[() => start, () => end], dynamicCurve],
            {
                id: "dynamic",
                name: "",
                withLabel: true,
                curveLeft: {id: "dynamicCurveLeft", name: ""},
                baseLeft: {id: "dynamicBaseLeft", name: ""},
                curveRight: {id: "dynamicCurveRight", name: ""},
                baseRight: {id: "dynamicBaseRight", name: ""},
                label: {id: "dynamicLabel", name: ""}
            }
        );
        dynamicBoard.update();
        const dynamicInitial = snapshot(dynamic);
        start = -1;
        end = 3;
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamic);

        const dragBoard = createBoard();
        const dragCurve = dragBoard.create(
            "functiongraph",
            [(x) => 0.5 * x * x - 1, -5, 5],
            {id: "dragCurve", name: "", withLabel: false}
        );
        const drag = dragBoard.create(
            "integral",
            [[-2, 2], dragCurve],
            {
                id: "drag",
                name: "",
                withLabel: false,
                curveLeft: {id: "dragCurveLeft", name: ""},
                baseLeft: {id: "dragBaseLeft", name: ""},
                curveRight: {id: "dragCurveRight", name: ""},
                baseRight: {id: "dragBaseRight", name: ""}
            }
        );
        dragBoard.update();
        const dragInitial = snapshot(drag);
        drag.curveRight.setPosition(
            JXG.COORDS_BY_USER,
            [4, 3]
        );
        dragBoard.update(drag.curveRight);
        const dragUpdated = snapshot(drag);

        const yBoard = createBoard();
        const yCurve = yBoard.create(
            "curve",
            [(t) => t * t - 1, (t) => t, -2, 2],
            {id: "yCurve", name: "", withLabel: false}
        );
        const yAxis = yBoard.create(
            "integral",
            [[-1.5, 1.5], yCurve],
            {
                id: "yAxis",
                name: "",
                axis: "y",
                withLabel: true,
                curveLeft: {id: "yCurveLeft", name: ""},
                baseLeft: {id: "yBaseLeft", name: ""},
                curveRight: {id: "yCurveRight", name: ""},
                baseRight: {id: "yBaseRight", name: ""},
                label: {id: "yLabel", name: ""}
            }
        );
        yBoard.update();

        const removalBoard = createBoard();
        const removalCurve = removalBoard.create(
            "functiongraph",
            [(x) => x * x, -4, 4],
            {id: "removalCurve", name: "", withLabel: false}
        );
        const removable = removalBoard.create(
            "integral",
            [[-1, 2], removalCurve],
            {
                id: "removable",
                name: "",
                curveLeft: {id: "removeCurveLeft", name: ""},
                baseLeft: {id: "removeBaseLeft", name: ""},
                curveRight: {id: "removeCurveRight", name: ""},
                baseRight: {id: "removeBaseRight", name: ""},
                label: {id: "removeLabel", name: ""}
            }
        );
        removalBoard.update();
        const removalBefore = snapshot(removable);
        removalBoard.removeObject(removable);
        const removalAfter = {
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            ),
            objects: [
                "removalCurve",
                "removable",
                "removeCurveLeft",
                "removeBaseLeft",
                "removeCurveRight",
                "removeBaseRight",
                "removeLabel"
            ].map((id) => ({
                id,
                present: removalBoard.objects[id] !== undefined
            })),
            curve: relations(removalCurve)
        };

        return {
            version: JXG.version,
            basic: snapshot(basic),
            reverse: snapshot(reverse),
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated
            },
            drag: {
                initial: dragInitial,
                updated: dragUpdated
            },
            yAxis: snapshot(yAxis),
            invalid: {
                missingCurve: captureError(() => [[-1, 1]]),
                nonArrayInterval: captureError((curve) => [0, curve]),
                shortInterval: captureError((curve) => [[-1], curve]),
                invalidBound: captureError(
                    (curve) => [[{}, 1], curve]
                ),
                unsupportedCurve: captureError(() => [[-1, 1], 5]),
                invalidAxis: captureError(
                    (curve) => [[-1, 1], curve],
                    {axis: "z"}
                )
            },
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
