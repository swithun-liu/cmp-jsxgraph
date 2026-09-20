/*
 * Official JSXGraph 1.13.3 Curve/Point Tangent behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/tangent-curve.mjs
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
            const id = `board-${boardIndex}`;
            boardIndex += 1;
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
            return value;
        };
        const vector = (values) => values.map(classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            coordinates: vector(point.coords.usrCoords),
            constrained: point.isConstrained,
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
            relations: relations(point)
        });
        const snapshotCurve = (curve) => ({
            id: curve.id,
            elType: curve.elType,
            type: curve.type,
            elementClass: curve.elementClass,
            curveType: curve.visProp.curvetype,
            bezierDegree: curve.bezierDegree,
            numberPoints: curve.numberPoints,
            relations: relations(curve)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            constrained: line.constrained,
            draggable: line.isDraggable,
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            parents: [...line.parents],
            inherits: line.inherits.map((element) => element.id),
            glider: line.glider?.id ?? null,
            point1: snapshotPoint(line.point1),
            point2: snapshotPoint(line.point2),
            relations: relations(line)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const tangent = (board, kind, parents, id, attributes = {}) =>
            board.create(kind, parents, {
                id,
                name: "",
                withLabel: false,
                point1: {
                    id: `${id}Point1`,
                    name: "",
                    withLabel: false
                },
                point2: {
                    id: `${id}Point2`,
                    name: "",
                    withLabel: false
                },
                ...attributes
            });
        const presence = (board, elements) =>
            Object.fromEntries(
                elements.map((element) => [
                    element.id,
                    board.objects[element.id] === element
                ])
            );
        const captureError = (action) => {
            try {
                const value = action();
                return {
                    threw: false,
                    id: value?.id ?? null,
                    elType: value?.elType ?? null
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };

        const board = createBoard();
        const functionCurve = board.create(
            "functiongraph",
            [(x) => x * x - 1, -4, 4],
            {id: "functionCurve", name: "", withLabel: false}
        );
        const functionPoint = point(board, [2, 5], "functionPoint");
        const functionTangent = tangent(
            board,
            "tangent",
            [functionCurve, functionPoint],
            "functionTangent",
            {straightFirst: false, straightLast: true}
        );
        const functionPolar = tangent(
            board,
            "polar",
            [functionPoint, functionCurve],
            "functionPolar",
            {straightFirst: true, straightLast: false}
        );

        const parametricCurve = board.create(
            "curve",
            [
                (t) => 2 * Math.cos(t),
                (t) => Math.sin(t),
                0,
                2 * Math.PI
            ],
            {id: "parametricCurve", name: "", withLabel: false}
        );
        const parametricPoint = point(
            board,
            [3, 0.75],
            "parametricPoint"
        );
        const parametricTangent = tangent(
            board,
            "tangent",
            [parametricPoint, parametricCurve],
            "parametricTangent"
        );
        const shiftedParametricCurve = board.create(
            "curve",
            [
                (t) => 2 * Math.cos(t) + 3.5,
                (t) => 1.4 * Math.sin(t) - 1,
                0,
                2 * Math.PI
            ],
            {
                id: "shiftedParametricCurve",
                name: "",
                withLabel: false
            }
        );
        const shiftedParametricPoint = point(
            board,
            [6, 1.2],
            "shiftedParametricPoint"
        );
        const shiftedParametricTangent = tangent(
            board,
            "tangent",
            [shiftedParametricPoint, shiftedParametricCurve],
            "shiftedParametricTangent"
        );

        const plotCurve = board.create(
            "curve",
            [[-4, -1, 2, 5], [-2, 2, -1, 3]],
            {id: "plotCurve", name: "", withLabel: false}
        );
        const plotPoint = point(board, [0.25, 2.5], "plotPoint");
        const plotTangent = tangent(
            board,
            "tangent",
            [plotCurve, plotPoint],
            "plotTangent",
            {straightFirst: false, straightLast: false}
        );
        board.update();

        const initial = {
            curves: [
                functionCurve,
                parametricCurve,
                shiftedParametricCurve,
                plotCurve
            ].map(snapshotCurve),
            points: [
                functionPoint,
                parametricPoint,
                shiftedParametricPoint,
                plotPoint
            ].map(snapshotPoint),
            tangents: [
                functionTangent,
                functionPolar,
                parametricTangent,
                shiftedParametricTangent,
                plotTangent
            ].map(snapshotLine),
            objectOrder: board.objectsList.map((element) => element.id)
        };

        functionPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-1, 4]
        );
        parametricPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2.5, -1.25]
        );
        plotPoint.setPositionDirectly(JXG.COORDS_BY_USER, [4, 4]);
        board.update();
        const moved = {
            points: [
                functionPoint,
                parametricPoint,
                plotPoint
            ].map(snapshotPoint),
            tangents: [
                functionTangent,
                functionPolar,
                parametricTangent,
                plotTangent
            ].map(snapshotLine)
        };

        const jessieCodeBoard = createBoard();
        jessieCodeBoard.jc.parse(
            'parameterCurve = curve(' +
                '"2 * cos(x)", "sin(x)", ' +
                '0, 6.283185307179586' +
                ') << id: "jessieParameterCurve", name: "", ' +
                'doAdvancedPlot: false, numberPointsHigh: 128 >>; ' +
                'parameterPoint = point(3, 0.75) << ' +
                'id: "jessieParameterPoint", name: "" >>; ' +
                'parameterTangent = tangent(' +
                'parameterPoint, parameterCurve' +
                ') << id: "jessieParameterTangent", name: "", ' +
                'point1: << id: "jessieParameterTangentPoint1", ' +
                'name: "" >>, ' +
                'point2: << id: "jessieParameterTangentPoint2", ' +
                'name: "" >> >>; ' +
                'shiftedCurve = curve(' +
                '"2 * cos(x) + 3.5", ' +
                '"1.4 * sin(x) - 1", ' +
                '0, 6.283185307179586' +
                ') << id: "jessieShiftedCurve", name: "", ' +
                'doAdvancedPlot: false, numberPointsHigh: 128 >>; ' +
                'shiftedPoint = point(6, 1.2) << ' +
                'id: "jessieShiftedPoint", name: "" >>; ' +
                'shiftedPolar = polar(shiftedPoint, shiftedCurve) << ' +
                'id: "jessieShiftedPolar", name: "", ' +
                'point1: << id: "jessieShiftedPolarPoint1", name: "" >>, ' +
                'point2: << id: "jessieShiftedPolarPoint2", name: "" >> >>;'
        );
        jessieCodeBoard.update();
        const jessieCode = {
            parameterCurve: snapshotCurve(
                jessieCodeBoard.objects.jessieParameterCurve
            ),
            parameterPoint: snapshotPoint(
                jessieCodeBoard.objects.jessieParameterPoint
            ),
            parameterTangent: snapshotLine(
                jessieCodeBoard.objects.jessieParameterTangent
            ),
            curve: snapshotCurve(
                jessieCodeBoard.objects.jessieShiftedCurve
            ),
            point: snapshotPoint(
                jessieCodeBoard.objects.jessieShiftedPoint
            ),
            tangent: snapshotLine(
                jessieCodeBoard.objects.jessieShiftedPolar
            )
        };
        jessieCodeBoard.objects.jessieParameterPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2.5, -1.25]
        );
        jessieCodeBoard.update();
        jessieCode.parameterMoved = snapshotLine(
            jessieCodeBoard.objects.jessieParameterTangent
        );

        const removalBoard = createBoard();
        const removalCurve = removalBoard.create(
            "functiongraph",
            [(x) => x * x, -4, 4],
            {id: "removalCurve", name: "", withLabel: false}
        );
        const removalPoint = point(
            removalBoard,
            [1, 1],
            "removalPoint"
        );
        const removedTangent = tangent(
            removalBoard,
            "tangent",
            [removalCurve, removalPoint],
            "removedTangent"
        );
        const removedPoint1 = removedTangent.point1;
        const removedPoint2 = removedTangent.point2;
        removalBoard.removeObject(removedTangent);
        const directRemoval = {
            presence: presence(
                removalBoard,
                [
                    removalCurve,
                    removalPoint,
                    removedTangent,
                    removedPoint1,
                    removedPoint2
                ]
            ),
            curve: relations(removalCurve),
            point: relations(removalPoint),
            objectOrder: removalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const pointRemovalBoard = createBoard();
        const pointRemovalCurve = pointRemovalBoard.create(
            "functiongraph",
            [(x) => x * x, -4, 4],
            {id: "pointRemovalCurve", name: "", withLabel: false}
        );
        const removedPoint = point(
            pointRemovalBoard,
            [1, 1],
            "removedPoint"
        );
        const pointRemovalTangent = tangent(
            pointRemovalBoard,
            "tangent",
            [pointRemovalCurve, removedPoint],
            "pointRemovalTangent"
        );
        const pointRemovalPoint1 = pointRemovalTangent.point1;
        const pointRemovalPoint2 = pointRemovalTangent.point2;
        pointRemovalBoard.removeObject(removedPoint);
        const pointRemoval = {
            presence: presence(
                pointRemovalBoard,
                [
                    pointRemovalCurve,
                    removedPoint,
                    pointRemovalTangent,
                    pointRemovalPoint1,
                    pointRemovalPoint2
                ]
            ),
            curve: relations(pointRemovalCurve),
            objectOrder: pointRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const curveRemovalBoard = createBoard();
        const removedCurve = curveRemovalBoard.create(
            "functiongraph",
            [(x) => x * x, -4, 4],
            {id: "removedCurve", name: "", withLabel: false}
        );
        const curveRemovalPoint = point(
            curveRemovalBoard,
            [1, 1],
            "curveRemovalPoint"
        );
        const curveRemovalTangent = tangent(
            curveRemovalBoard,
            "tangent",
            [removedCurve, curveRemovalPoint],
            "curveRemovalTangent"
        );
        const curveRemovalPoint1 = curveRemovalTangent.point1;
        const curveRemovalPoint2 = curveRemovalTangent.point2;
        curveRemovalBoard.removeObject(removedCurve);
        curveRemovalBoard.update();
        const curveRemoval = {
            presence: presence(
                curveRemovalBoard,
                [
                    removedCurve,
                    curveRemovalPoint,
                    curveRemovalTangent,
                    curveRemovalPoint1,
                    curveRemovalPoint2
                ]
            ),
            point: relations(curveRemovalPoint),
            tangent: snapshotLine(curveRemovalTangent),
            objectOrder: curveRemovalBoard.objectsList.map(
                (element) => element.id
            )
        };

        const degenerateBoard = createBoard();
        const duplicatePlot = degenerateBoard.create(
            "curve",
            [[1, 1], [2, 2]],
            {id: "duplicatePlot", name: "", withLabel: false}
        );
        const duplicatePoint = point(
            degenerateBoard,
            [4, 5],
            "duplicatePoint"
        );
        const duplicateTangent = tangent(
            degenerateBoard,
            "tangent",
            [duplicatePlot, duplicatePoint],
            "duplicateTangent"
        );
        const onePointPlot = degenerateBoard.create(
            "curve",
            [[3], [-2]],
            {id: "onePointPlot", name: "", withLabel: false}
        );
        const onePoint = point(
            degenerateBoard,
            [3, -2],
            "onePoint"
        );
        const onePointTangent = captureError(
            () => tangent(
                degenerateBoard,
                "tangent",
                [onePointPlot, onePoint],
                "onePointTangent"
            )
        );
        degenerateBoard.update();

        const invalidBoard = createBoard();
        const invalidCurve = invalidBoard.create(
            "functiongraph",
            [(x) => x, -4, 4],
            {id: "invalidCurve", name: "", withLabel: false}
        );
        const invalidPoint = point(
            invalidBoard,
            [1, 1],
            "invalidPoint"
        );
        const otherBoard = createBoard();
        const foreignCurve = otherBoard.create(
            "functiongraph",
            [(x) => x * x, -4, 4],
            {id: "foreignCurve", name: "", withLabel: false}
        );
        const foreignPoint = point(
            otherBoard,
            [2, 4],
            "foreignPoint"
        );
        const invalid = {
            polarLineWithCurve: captureError(
                () => tangent(
                    invalidBoard,
                    "polarline",
                    [invalidCurve, invalidPoint],
                    "invalidPolarLine"
                )
            ),
            crossBoardCurve: captureError(
                () => tangent(
                    invalidBoard,
                    "tangent",
                    [foreignCurve, invalidPoint],
                    "crossBoardCurve"
                )
            ),
            crossBoardPoint: captureError(
                () => tangent(
                    invalidBoard,
                    "tangent",
                    [invalidCurve, foreignPoint],
                    "crossBoardPoint"
                )
            )
        };

        return {
            version: JXG.version,
            initial,
            moved,
            jessieCode,
            directRemoval,
            pointRemoval,
            curveRemoval,
            degenerate: {
                duplicatePlot: snapshotCurve(duplicatePlot),
                duplicateTangent: snapshotLine(duplicateTangent),
                onePointPlot: snapshotCurve(onePointPlot),
                onePointTangent
            },
            invalid
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
