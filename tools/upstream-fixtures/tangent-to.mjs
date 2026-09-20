/*
 * Official JSXGraph 1.13.3 tangentto Circle behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/tangent-to.mjs
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
            if (value === undefined) {
                return "undefined";
            }
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (value === Number.POSITIVE_INFINITY) {
                return "Infinity";
            }
            if (value === Number.NEGATIVE_INFINITY) {
                return "-Infinity";
            }
            if (Object.is(value, -0)) {
                return "-0";
            }
            return value;
        };
        const vector = (values) => Array.from(values, classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            name: point.name,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            coordinates: vector(point.coords.usrCoords),
            constrained: point.isConstrained,
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
            visible: point.visProp.visible,
            size: point.visProp.size,
            dump: point.dump,
            relations: relations(point)
        });
        const snapshotLine = (line) => ({
            id: line.id,
            name: line.name,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            stdform: vector(line.stdform),
            constrained: line.constrained,
            draggable: line.isDraggable,
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            visible: line.visProp.visible,
            strokeColor: line.visProp.strokecolor,
            strokeWidth: line.visProp.strokewidth,
            dash: line.visProp.dash,
            parents: [...line.parents],
            inherits: line.inherits.map((element) => element.id),
            subs: Object.fromEntries(
                Object.entries(line.subs).map(([key, value]) => [
                    key,
                    value.id
                ])
            ),
            glider: line.glider?.id ?? null,
            point1: snapshotPoint(line.point1),
            point2: snapshotPoint(line.point2),
            relations: relations(line)
        });
        const snapshotTangentTo = (line) => ({
            line: snapshotLine(line),
            pointProperty: line.point?.id ?? null,
            polarProperty: line.polar?.id ?? null,
            point: snapshotPoint(line.point),
            polar: snapshotLine(line.polar)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const circle = (board, center, radiusPoint, id) =>
            board.create("circle", [center, radiusPoint], {
                id,
                name: "",
                withLabel: false
            });
        const tangentToAttributes = (prefix, overrides = {}) => ({
            id: `${prefix}Tangent`,
            name: "",
            withLabel: false,
            strokeColor: "#204060",
            strokeWidth: 4,
            dash: 1,
            point1: {
                id: `${prefix}TangentPoint1`,
                name: "",
                withLabel: false
            },
            point2: {
                id: `${prefix}TangentPoint2`,
                name: "",
                withLabel: false
            },
            polar: {
                id: `${prefix}Polar`,
                name: "",
                withLabel: false,
                visible: true,
                strokeColor: "#a02040",
                strokeWidth: 5,
                dash: 2,
                point1: {
                    id: `${prefix}PolarPoint1`,
                    name: "",
                    withLabel: false
                },
                point2: {
                    id: `${prefix}PolarPoint2`,
                    name: "",
                    withLabel: false
                }
            },
            point: {
                id: `${prefix}Intersection`,
                name: "",
                withLabel: false,
                visible: true,
                size: 7
            },
            ...overrides
        });
        const objectOrder = (board) =>
            board.objectsList.map((element) => element.id);
        const presence = (board, ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
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
        const center = point(board, [1, 1], "center");
        const radius = point(board, [4, 1], "radius");
        const sourceCircle = circle(board, center, radius, "sourceCircle");
        const sourcePoint = point(board, [5, 4], "sourcePoint");
        const first = board.create(
            "tangentto",
            [sourceCircle, sourcePoint],
            tangentToAttributes("first")
        );
        const second = board.create(
            "tangentto",
            [sourceCircle, sourcePoint, 1],
            tangentToAttributes("second")
        );
        board.update();
        const initial = {
            first: snapshotTangentTo(first),
            second: snapshotTangentTo(second),
            sourceRelations: {
                center: relations(center),
                radius: relations(radius),
                circle: relations(sourceCircle),
                point: relations(sourcePoint)
            },
            objectOrder: objectOrder(board)
        };
        center.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 2]);
        radius.setPositionDirectly(JXG.COORDS_BY_USER, [2, 2]);
        sourcePoint.setPositionDirectly(JXG.COORDS_BY_USER, [3, -3]);
        board.update();
        const moved = {
            first: snapshotTangentTo(first),
            second: snapshotTangentTo(second),
            sourceRelations: {
                center: relations(center),
                radius: relations(radius),
                circle: relations(sourceCircle),
                point: relations(sourcePoint)
            }
        };

        const defaultBoard = createBoard();
        const defaultCenter = point(defaultBoard, [0, 0], "defaultCenter");
        const defaultRadius = point(defaultBoard, [3, 0], "defaultRadius");
        const defaultCircle = circle(
            defaultBoard,
            defaultCenter,
            defaultRadius,
            "defaultCircle"
        );
        const defaultPoint = point(defaultBoard, [5, 1], "defaultPoint");
        const defaultTangent = defaultBoard.create(
            "tangentto",
            [defaultCircle, defaultPoint],
            {
                id: "defaultTangent",
                name: "",
                withLabel: false
            }
        );
        defaultBoard.update();
        const defaultAttributes = {
            tangent: snapshotLine(defaultTangent),
            point: snapshotPoint(defaultTangent.point),
            polar: snapshotLine(defaultTangent.polar),
            objectOrder: objectOrder(defaultBoard)
        };

        const indexBoard = createBoard();
        const indexCenter = point(indexBoard, [0, 0], "indexCenter");
        const indexRadius = point(indexBoard, [3, 0], "indexRadius");
        const indexCircle = circle(
            indexBoard,
            indexCenter,
            indexRadius,
            "indexCircle"
        );
        const indexPoint = point(indexBoard, [5, 1], "indexPoint");
        const indexCases = [
            ["omitted", []],
            ["zero", [0]],
            ["one", [1]],
            ["two", [2]],
            ["negative", [-1]],
            ["fractional", [0.5]],
            ["nan", [Number.NaN]],
            ["positiveInfinity", [Number.POSITIVE_INFINITY]],
            ["negativeInfinity", [Number.NEGATIVE_INFINITY]],
            ["string", ["1"]],
            ["null", [null]]
        ].map(([name, tail]) => {
            const before = objectOrder(indexBoard);
            let output = null;
            const result = captureError(() => {
                output = indexBoard.create(
                    "tangentto",
                    [indexCircle, indexPoint, ...tail],
                    tangentToAttributes(`index${name}`)
                );
                return output;
            });
            indexBoard.update();
            return {
                name,
                result,
                pointCoordinates: output
                    ? vector(output.point.coords.usrCoords)
                    : null,
                tangentStdform: output ? vector(output.stdform) : null,
                addedObjects: objectOrder(indexBoard).slice(before.length)
            };
        });

        const geometryCases = [
            ["outside", [4, 2]],
            ["on", [0, 3]],
            ["inside", [1, 1]],
            ["center", [0, 0]]
        ].map(([name, coordinates]) => {
            const geometryBoard = createBoard();
            const geometryCenter = point(
                geometryBoard,
                [0, 0],
                `${name}Center`
            );
            const geometryRadius = point(
                geometryBoard,
                [3, 0],
                `${name}Radius`
            );
            const geometryCircle = circle(
                geometryBoard,
                geometryCenter,
                geometryRadius,
                `${name}Circle`
            );
            const geometryPoint = point(
                geometryBoard,
                coordinates,
                `${name}Point`
            );
            const tangent = geometryBoard.create(
                "tangentto",
                [geometryCircle, geometryPoint],
                tangentToAttributes(name)
            );
            geometryBoard.update();
            return {
                name,
                output: snapshotTangentTo(tangent),
                objectOrder: objectOrder(geometryBoard)
            };
        });

        const degenerateBoard = createBoard();
        const degenerateCenter = point(
            degenerateBoard,
            [2, -1],
            "degenerateCenter"
        );
        const degenerateRadius = point(
            degenerateBoard,
            [2, -1],
            "degenerateRadius"
        );
        const degenerateCircle = circle(
            degenerateBoard,
            degenerateCenter,
            degenerateRadius,
            "degenerateCircle"
        );
        const degeneratePoint = point(
            degenerateBoard,
            [4, 3],
            "degeneratePoint"
        );
        const degenerateTangent = degenerateBoard.create(
            "tangentto",
            [degenerateCircle, degeneratePoint],
            tangentToAttributes("degenerate")
        );
        degenerateBoard.update();
        const degenerate = {
            output: snapshotTangentTo(degenerateTangent),
            objectOrder: objectOrder(degenerateBoard)
        };

        const coordinateBoard = createBoard();
        const coordinateCenter = point(
            coordinateBoard,
            [0, 0],
            "coordinateCenter"
        );
        const coordinateRadius = point(
            coordinateBoard,
            [2, 0],
            "coordinateRadius"
        );
        const coordinateCircle = circle(
            coordinateBoard,
            coordinateCenter,
            coordinateRadius,
            "coordinateCircle"
        );
        const coordinateBefore = objectOrder(coordinateBoard);
        const coordinateResult = captureError(() =>
            coordinateBoard.create(
                "tangentto",
                [coordinateCircle, [4, 3]],
                tangentToAttributes("coordinate")
            )
        );
        const coordinateAdded = objectOrder(coordinateBoard).slice(
            coordinateBefore.length
        );
        const coordinateParentCase = {
            result: coordinateResult,
            addedObjects: coordinateAdded
        };

        const removalBoard = createBoard();
        const removalCenter = point(
            removalBoard,
            [0, 0],
            "removalCenter"
        );
        const removalRadius = point(
            removalBoard,
            [2, 0],
            "removalRadius"
        );
        const removalCircle = circle(
            removalBoard,
            removalCenter,
            removalRadius,
            "removalCircle"
        );
        const removalPoint = point(
            removalBoard,
            [4, 3],
            "removalPoint"
        );
        const removalBefore = objectOrder(removalBoard);
        const removalTangent = removalBoard.create(
            "tangentto",
            [removalCircle, removalPoint],
            tangentToAttributes("removal")
        );
        const removalAdded = objectOrder(removalBoard).slice(
            removalBefore.length
        );
        const removalInitial = {
            output: snapshotTangentTo(removalTangent),
            addedObjects: removalAdded
        };
        removalBoard.removeObject(removalTangent);
        const removalAfterTangent = {
            presence: presence(removalBoard, removalAdded),
            circle: relations(removalCircle),
            point: relations(removalPoint)
        };

        const parentRemovalBoard = createBoard();
        const parentRemovalCenter = point(
            parentRemovalBoard,
            [0, 0],
            "parentRemovalCenter"
        );
        const parentRemovalRadius = point(
            parentRemovalBoard,
            [2, 0],
            "parentRemovalRadius"
        );
        const parentRemovalCircle = circle(
            parentRemovalBoard,
            parentRemovalCenter,
            parentRemovalRadius,
            "parentRemovalCircle"
        );
        const parentRemovalPoint = point(
            parentRemovalBoard,
            [4, 3],
            "parentRemovalPoint"
        );
        const parentRemovalBefore = objectOrder(parentRemovalBoard);
        parentRemovalBoard.create(
            "tangentto",
            [parentRemovalCircle, parentRemovalPoint],
            tangentToAttributes("parentRemoval")
        );
        const parentRemovalAdded = objectOrder(parentRemovalBoard).slice(
            parentRemovalBefore.length
        );
        parentRemovalBoard.removeObject(parentRemovalPoint);
        const removalAfterSourcePoint = presence(
            parentRemovalBoard,
            [
                parentRemovalPoint.id,
                parentRemovalCircle.id,
                ...parentRemovalAdded
            ]
        );

        const invalidParentBoard = createBoard();
        const invalidA = point(invalidParentBoard, [-2, 0], "invalidA");
        const invalidB = point(invalidParentBoard, [2, 0], "invalidB");
        const invalidLine = invalidParentBoard.create(
            "line",
            [invalidA, invalidB],
            { id: "invalidLine", name: "", withLabel: false }
        );
        const invalidPoint = point(
            invalidParentBoard,
            [3, 2],
            "invalidPoint"
        );
        const invalidParentCases = [
            ["line", [invalidLine, invalidPoint]],
            ["reversed", [invalidPoint, invalidLine]],
            ["missingPoint", [invalidLine]],
            ["coordinateLeak", [invalidLine, [4, 3]]]
        ].map(([name, parents]) => {
            const before = objectOrder(invalidParentBoard);
            const result = captureError(() =>
                invalidParentBoard.create(
                    "tangentto",
                    parents,
                    tangentToAttributes(`invalid${name}`)
                )
            );
            return {
                name,
                result,
                addedObjects: objectOrder(invalidParentBoard).slice(
                    before.length
                )
            };
        });

        const ellipseBoard = createBoard();
        const focus1 = point(ellipseBoard, [-3, 0], "focus1");
        const focus2 = point(ellipseBoard, [3, 0], "focus2");
        const ellipsePoint = point(ellipseBoard, [0, 5], "ellipsePoint");
        const ellipse = ellipseBoard.create(
            "ellipse",
            [focus1, focus2, ellipsePoint],
            { id: "ellipse", name: "", withLabel: false }
        );
        const ellipseSource = point(
            ellipseBoard,
            [7, 4],
            "ellipseSource"
        );
        const ellipseTangent = ellipseBoard.create(
            "tangentto",
            [ellipse, ellipseSource, 0],
            tangentToAttributes("ellipse")
        );
        ellipseBoard.update();
        const conicBoundary = {
            output: snapshotTangentTo(ellipseTangent),
            conicType: ellipse.type,
            conicElementClass: ellipse.elementClass,
            objectOrder: objectOrder(ellipseBoard)
        };

        const duplicateAt = (stage) => {
            const failureBoard = createBoard();
            const failureCenter = point(
                failureBoard,
                [0, 0],
                `${stage}Center`
            );
            const failureRadius = point(
                failureBoard,
                [2, 0],
                `${stage}Radius`
            );
            const failureCircle = circle(
                failureBoard,
                failureCenter,
                failureRadius,
                `${stage}Circle`
            );
            const failurePoint = point(
                failureBoard,
                [4, 3],
                `${stage}Point`
            );
            point(failureBoard, [-7, -5], `${stage}Taken`);
            const attributes = tangentToAttributes(stage);
            if (stage === "polar") {
                attributes.polar.id = `${stage}Taken`;
            } else if (stage === "intersection") {
                attributes.point.id = `${stage}Taken`;
            } else {
                attributes.id = `${stage}Taken`;
            }
            const before = objectOrder(failureBoard);
            const result = captureError(() =>
                failureBoard.create(
                    "tangentto",
                    [failureCircle, failurePoint],
                    attributes
                )
            );
            return {
                stage,
                result,
                addedObjects: objectOrder(failureBoard).slice(before.length),
                sourceRelations: {
                    circle: relations(failureCircle),
                    point: relations(failurePoint)
                }
            };
        };
        const failureAt = (stage) => {
            const failureBoard = createBoard();
            const failureCenter = point(
                failureBoard,
                [0, 0],
                `${stage}FailureCenter`
            );
            const failureRadius = point(
                failureBoard,
                [2, 0],
                `${stage}FailureRadius`
            );
            const failureCircle = circle(
                failureBoard,
                failureCenter,
                failureRadius,
                `${stage}FailureCircle`
            );
            const failurePoint = point(
                failureBoard,
                [4, 3],
                `${stage}FailurePoint`
            );
            const originalCreate = failureBoard.create.bind(failureBoard);
            failureBoard.create = (elementType, parents, attributes) => {
                if (elementType === stage) {
                    throw new Error(`injected ${stage} failure`);
                }
                return originalCreate(elementType, parents, attributes);
            };
            const before = objectOrder(failureBoard);
            const result = captureError(() =>
                originalCreate(
                    "tangentto",
                    [failureCircle, failurePoint],
                    tangentToAttributes(`${stage}Failure`)
                )
            );
            return {
                stage,
                result,
                addedObjects: objectOrder(failureBoard).slice(before.length),
                sourceRelations: {
                    circle: relations(failureCircle),
                    point: relations(failurePoint)
                }
            };
        };

        return {
            version: JXG.version,
            initial,
            moved,
            defaultAttributes,
            indexCases,
            geometryCases,
            degenerate,
            coordinateParentCase,
            removalInitial,
            removalAfterTangent,
            removalAfterSourcePoint,
            invalidParentCases,
            conicBoundary,
            duplicateCollisions: [
                duplicateAt("polar"),
                duplicateAt("intersection"),
                duplicateAt("tangent")
            ],
            stagedFailures: [
                failureAt("polar"),
                failureAt("intersection"),
                failureAt("tangent")
            ]
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
