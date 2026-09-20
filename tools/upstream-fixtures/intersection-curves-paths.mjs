/*
 * Official JSXGraph 1.13.3 Curve/Arc/Sector/Polygon intersection fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/intersection-curves-paths.mjs
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
        const container = document.createElement("div");
        container.id = "board";
        container.style.width = "640px";
        container.style.height = "480px";
        document.querySelector("#fixtures").appendChild(container);
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });

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
        const snapshotPoint = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            originalType: point._org_type,
            elementClass: point.elementClass,
            coords: vector(point.coords.usrCoords),
            real: point.isReal,
            intersectionNumbers:
                point.intersectionNumbers === undefined
                    ? null
                    : point.intersectionNumbers.map((value) =>
                        typeof value === "function"
                            ? classify(value())
                            : classify(value)
                    ),
            parents: [...point.parents]
        });
        const snapshotElement = (element) => ({
            id: element.id,
            elType: element.elType,
            type: element.type,
            elementClass: element.elementClass,
            numberPoints: element.numberPoints ?? null,
            bezierDegree: element.bezierDegree ?? null,
            dataXExists: element.dataX !== null &&
                element.dataX !== undefined,
            stdform: vector(element.stdform)
        });
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const intersection = (parents, id, attributes = {}) =>
            board.create("intersection", parents, {
                id,
                name: "",
                withLabel: false,
                ...attributes
            });
        const attempt = (callback) => {
            try {
                const output = callback();
                board.update();
                return {
                    point: snapshotPoint(output),
                    error: null
                };
            } catch (error) {
                return {
                    point: null,
                    error: error.message
                };
            }
        };

        const horizontal = board.create(
            "line",
            [
                point([-7, 0], "horizontalStart"),
                point([7, 0], "horizontalEnd")
            ],
            {id: "horizontal", name: "", withLabel: false}
        );
        const shortSegment = board.create(
            "segment",
            [
                point([-1, 0], "segmentStart"),
                point([1, 0], "segmentEnd")
            ],
            {id: "shortSegment", name: "", withLabel: false}
        );
        const zigzag = board.create(
            "curve",
            [
                [-4, -2, 0, 2, 4],
                [3, -1, 3, -1, 3]
            ],
            {id: "zigzag", name: "", withLabel: false}
        );
        const horizontalCurve = board.create(
            "curve",
            [
                [-7, 7],
                [1, 1]
            ],
            {id: "horizontalCurve", name: "", withLabel: false}
        );
        const functionGraph = board.create(
            "functiongraph",
            [(x) => x * x - 1, -3, 3],
            {id: "functionGraph", name: "", withLabel: false}
        );
        const center = point([0, 0], "center");
        const circle = board.create(
            "circle",
            [center, 2.5],
            {id: "circle", name: "", withLabel: false}
        );
        const arcRadius = point([3, 0], "arcRadius");
        const arcAngle = point([0, 3], "arcAngle");
        const arc = board.create(
            "arc",
            [center, arcRadius, arcAngle],
            {id: "arc", name: "", withLabel: false}
        );
        const sector = board.create(
            "sector",
            [center, arcRadius, arcAngle],
            {id: "sector", name: "", withLabel: false}
        );
        const arcVertical = board.create(
            "line",
            [
                point([2, -5], "arcVerticalStart"),
                point([2, 5], "arcVerticalEnd")
            ],
            {id: "arcVertical", name: "", withLabel: false}
        );
        const polygonA = point([-2, -2], "polygonA");
        const polygonB = point([2, -2], "polygonB");
        const polygonC = point([2, 2], "polygonC");
        const polygonD = point([-2, 2], "polygonD");
        const polygon = board.create(
            "polygon",
            [polygonA, polygonB, polygonC, polygonD],
            {id: "polygon", name: "", withLabel: false}
        );
        const diamond = board.create(
            "polygon",
            [
                point([0, -3], "diamondA"),
                point([3, 0], "diamondB"),
                point([0, 3], "diamondC"),
                point([-3, 0], "diamondD")
            ],
            {id: "diamond", name: "", withLabel: false}
        );
        const overlap = board.create(
            "polygon",
            [
                point([1, -2], "overlapA"),
                point([4, -2], "overlapB"),
                point([4, 2], "overlapC"),
                point([1, 2], "overlapD")
            ],
            {id: "overlap", name: "", withLabel: false}
        );
        const sharedCorner = board.create(
            "polygon",
            [
                point([2, 2], "sharedCornerA"),
                point([4, 2], "sharedCornerB"),
                point([4, 4], "sharedCornerC"),
                point([2, 4], "sharedCornerD")
            ],
            {id: "sharedCorner", name: "", withLabel: false}
        );
        const triangleCurve = board.create(
            "curve",
            [
                [-3, 0, 3],
                [-3, 3, -3]
            ],
            {id: "triangleCurve", name: "", withLabel: false}
        );
        board.update();

        const cases = {};
        const createIndexed = (
            prefix,
            first,
            second,
            indexes,
            attributes = {}
        ) => {
            for (const index of indexes) {
                const suffix = String(index).replace("-", "negative");
                cases[`${prefix}${suffix}`] = attempt(() =>
                    intersection(
                        [first, second, index],
                        `${prefix}${suffix}`,
                        attributes
                    )
                );
            }
        };

        createIndexed(
            "curveLine",
            zigzag,
            horizontal,
            [0, 1, 2, 3, 4, -1, 1.5]
        );
        createIndexed(
            "lineCurve",
            horizontal,
            zigzag,
            [0, 1, 2, 3, 4]
        );
        createIndexed(
            "functionLine",
            functionGraph,
            horizontal,
            [0, 1, 2]
        );
        createIndexed(
            "curveCurve",
            zigzag,
            horizontalCurve,
            [0, 1, 2, 3, 4]
        );
        createIndexed("curveCircle", zigzag, circle, [0, 1, 2, 3, 4]);
        createIndexed("circleCurve", circle, zigzag, [0, 1, 2, 3, 4]);
        createIndexed("arcCurve", arc, horizontalCurve, [0, 1, 2]);
        createIndexed("sectorCurve", sector, horizontalCurve, [0, 1, 2]);
        createIndexed("arcCircle", arc, circle, [0, 1, 2]);
        createIndexed("sectorCircle", sector, circle, [0, 1, 2]);
        createIndexed("arcLineExtended", arc, arcVertical, [0, 1]);
        createIndexed(
            "arcLineClipped",
            arc,
            arcVertical,
            [0, 1],
            {alwaysIntersect: false}
        );
        createIndexed(
            "lineArcClipped",
            arcVertical,
            arc,
            [0, 1],
            {alwaysIntersect: false}
        );
        createIndexed("sectorLineExtended", sector, arcVertical, [0, 1]);
        createIndexed(
            "sectorLineClipped",
            sector,
            arcVertical,
            [0, 1],
            {alwaysIntersect: false}
        );
        createIndexed("polygonLine", polygon, horizontal, [0, 1, 2]);
        createIndexed("linePolygon", horizontal, polygon, [0, 1, 2]);
        createIndexed(
            "polygonSegmentExtended",
            polygon,
            shortSegment,
            [0, 1, 2]
        );
        createIndexed(
            "polygonSegmentClipped",
            polygon,
            shortSegment,
            [0, 1, 2],
            {alwaysIntersect: false}
        );

        const firstCurveLine = board.objects.curveLine0;
        cases.otherCurveLine = attempt(() =>
            board.create(
                "otherintersection",
                [zigzag, horizontal, firstCurveLine],
                {
                    id: "otherCurveLine",
                    name: "",
                    withLabel: false
                }
            )
        );
        const firstCurveCircle = board.objects.curveCircle0;
        cases.otherCurveCircle = attempt(() =>
            board.create(
                "otherintersection",
                [zigzag, circle, [firstCurveCircle]],
                {
                    id: "otherCurveCircle",
                    name: "",
                    withLabel: false
                }
            )
        );

        createIndexed(
            "polygonCircle",
            polygon,
            circle,
            [0, 1, 2, 3, 4, 5, 6, 7, 8, -1]
        );
        createIndexed("circlePolygon", circle, polygon, [0, 1, 2]);
        createIndexed(
            "polygonDiamond",
            polygon,
            diamond,
            [0, 1, 2, 3, 4, 5, 6, 7, 8]
        );
        createIndexed(
            "polygonOverlap",
            polygon,
            overlap,
            [0, 1, 2, 3, 4]
        );
        createIndexed(
            "polygonSharedCorner",
            polygon,
            sharedCorner,
            [0, 1, 2]
        );
        createIndexed(
            "polygonCurve",
            polygon,
            triangleCurve,
            [0, 1, 2, 3, 4]
        );
        createIndexed("polygonArc", polygon, arc, [0, 1, 2]);
        createIndexed("polygonSector", polygon, sector, [0, 1, 2]);
        cases.polygonCircleFractional = attempt(() =>
            intersection(
                [polygon, circle, 0.5],
                "polygonCircleFractional"
            )
        );
        cases.polygonCircleFunctionNaN = attempt(() =>
            intersection(
                [polygon, circle, () => Number.NaN],
                "polygonCircleFunctionNaN"
            )
        );

        const initial = Object.fromEntries(
            Object.entries(cases).map(([name, result]) => [
                name,
                result
            ])
        );

        arcAngle.setPositionDirectly(JXG.COORDS_BY_USER, [0, -3]);
        polygonC.setPositionDirectly(JXG.COORDS_BY_USER, [3, 2]);
        board.fullUpdate();
        const moved = Object.fromEntries(
            Object.keys(cases).map((name) => {
                const output = board.objects[name];
                return [
                    name,
                    output === undefined
                        ? cases[name]
                        : {
                            point: snapshotPoint(output),
                            error: null
                        }
                ];
            })
        );
        const movedDirectPathPath = {
            polygonCircle2: vector(
                JXG.Math.Geometry.meetPathPath(
                    polygon,
                    circle,
                    2,
                    board
                ).usrCoords
            ),
            polygonCircle3: vector(
                JXG.Math.Geometry.meetPathPath(
                    polygon,
                    circle,
                    3,
                    board
                ).usrCoords
            )
        };

        return {
            version: JXG.version,
            elements: Object.fromEntries(
                [
                    horizontal,
                    shortSegment,
                    zigzag,
                    horizontalCurve,
                    functionGraph,
                    circle,
                    arc,
                    sector,
                    polygon,
                    diamond,
                    overlap,
                    sharedCorner,
                    triangleCurve
                ].map((element) => [
                    element.id,
                    snapshotElement(element)
                ])
            ),
            initial,
            moved,
            movedDrivers: {
                arcAngle: snapshotPoint(arcAngle),
                polygonC: snapshotPoint(polygonC)
            },
            movedDirectPathPath
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
