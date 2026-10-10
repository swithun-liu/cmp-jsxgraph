/*
 * Official JSXGraph 1.13.3 non-Point reflection/mirror fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/element-reflections.mjs
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
    process.env.CHROME_BIN ||
    "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    args: ["--disable-dev-shm-usage", "--no-sandbox"]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<div id="board" style="width: 640px; height: 480px"></div>'
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
            boundingbox: [-8, 6, 8, -6],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const vector = (values) => Array.from(values, (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (!Number.isFinite(value)) {
                return value > 0 ? "Infinity" : "-Infinity";
            }
            return value;
        });
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors),
            inherits: (element.inherits || []).map((value) => value.id)
        });
        const base = (element) => ({
            id: element.id,
            elType: element.elType,
            type: element.type,
            elementClass: element.elementClass,
            needsRegularUpdate: element.needsRegularUpdate,
            draggable: element.isDraggable,
            relations: relations(element)
        });
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const line = (first, second, id) =>
            board.create("line", [first, second], {
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
                }
            });
        const snapshotPoint = (element) => ({
            ...base(element),
            coordinates: vector(element.coords.usrCoords),
            transformations: element.transformations.map(
                (value) => value.transformationType
            ),
            baseElement: element.baseElement?.id ?? null
        });
        const snapshotLine = (element) => ({
            ...base(element),
            stdform: vector(element.stdform),
            point1: snapshotPoint(element.point1),
            point2: snapshotPoint(element.point2)
        });
        const snapshotCurve = (element) => ({
            ...base(element),
            curveType: element.curveType,
            numberPoints: element.numberPoints,
            points: element.points.map((value) =>
                vector(value.usrCoords)
            ),
            transformations: element.transformations.map(
                (value) => value.transformationType
            )
        });
        const snapshotPolygon = (element) => ({
            ...base(element),
            vertices: element.vertices.map(snapshotPoint),
            borders: element.borders.map(snapshotLine)
        });
        const snapshotCircle = (element) => ({
            ...base(element),
            center: snapshotPoint(element.center),
            radius: element.Radius(),
            quadraticform: element.quadraticform.map(vector)
        });
        const snapshot = (element) => {
            if (element.type === JXG.OBJECT_TYPE_POLYGON) {
                return snapshotPolygon(element);
            }
            if (element.elementClass === JXG.OBJECT_CLASS_LINE) {
                return snapshotLine(element);
            }
            if (element.elementClass === JXG.OBJECT_CLASS_CIRCLE) {
                return snapshotCircle(element);
            }
            return snapshotCurve(element);
        };

        const axisStart = point([0, -5], "axisStart");
        const axisEnd = point([0, 5], "axisEnd");
        const axis = line(axisStart, axisEnd, "axis");
        const mirror = point([1, -1], "mirror");

        const lineStart = point([-3, -2], "lineStart");
        const lineEnd = point([-1, 1], "lineEnd");
        const sourceLine = line(lineStart, lineEnd, "sourceLine");
        const sourceCurve = board.create(
            "curve",
            [[-4, -3, -2], [-1, 2, 0]],
            {id: "sourceCurve", name: "", withLabel: false}
        );
        const sourcePolygon = board.create(
            "polygon",
            [[-5, -3], [-3, -4], [-2, -2]],
            {
                id: "sourcePolygon",
                name: "",
                withLabel: false,
                vertices: {name: "", withLabel: false},
                borders: {name: "", withLabel: false}
            }
        );
        const circleCenter = point([-3, 2], "circleCenter");
        const circleRadius = point([-2, 2], "circleRadius");
        const sourceCircle = board.create(
            "circle",
            [circleCenter, circleRadius],
            {id: "sourceCircle", name: "", withLabel: false}
        );
        const arcCenter = point([-4, 1], "arcCenter");
        const arcRadius = point([-3, 1], "arcRadius");
        const arcAngle = point([-4, 2], "arcAngle");
        const sourceArc = board.create(
            "arc",
            [arcCenter, arcRadius, arcAngle],
            {id: "sourceArc", name: "", withLabel: false}
        );
        const sectorCenter = point([-4, -1], "sectorCenter");
        const sectorRadius = point([-3, -1], "sectorRadius");
        const sectorAngle = point([-4, -2], "sectorAngle");
        const sourceSector = board.create(
            "sector",
            [sectorCenter, sectorRadius, sectorAngle],
            {id: "sourceSector", name: "", withLabel: false}
        );

        const sources = {
            line: sourceLine,
            curve: sourceCurve,
            polygon: sourcePolygon,
            circle: sourceCircle,
            arc: sourceArc,
            sector: sourceSector
        };
        const reflected = {};
        const mirrored = {};
        for (const [name, source] of Object.entries(sources)) {
            reflected[name] = board.create(
                "reflection",
                [source, axis],
                {
                    id: `reflected${name}`,
                    name: "",
                    withLabel: false,
                    center: {
                        id: `reflected${name}Center`,
                        name: "",
                        withLabel: false
                    },
                    vertices: {name: "", withLabel: false},
                    borders: {name: "", withLabel: false},
                    point1: {name: "", withLabel: false},
                    point2: {name: "", withLabel: false}
                }
            );
            mirrored[name] = board.create(
                "mirrorelement",
                [source, mirror],
                {
                    id: `mirrored${name}`,
                    name: "",
                    withLabel: false,
                    center: {
                        id: `mirrored${name}Center`,
                        name: "",
                        withLabel: false
                    },
                    vertices: {name: "", withLabel: false},
                    borders: {name: "", withLabel: false},
                    point1: {name: "", withLabel: false},
                    point2: {name: "", withLabel: false}
                }
            );
        }
        const projectiveCircleReflection = board.create(
            "reflection",
            [sourceCircle, axis],
            {
                id: "projectiveCircleReflection",
                name: "",
                withLabel: false,
                type: "projective",
                center: {
                    id: "projectiveCircleReflectionCenter",
                    name: "",
                    withLabel: false
                }
            }
        );
        const projectiveCircleMirror = board.create(
            "mirrorelement",
            [sourceCircle, mirror],
            {
                id: "projectiveCircleMirror",
                name: "",
                withLabel: false,
                type: "projective",
                center: {
                    id: "projectiveCircleMirrorCenter",
                    name: "",
                    withLabel: false
                }
            }
        );
        board.update();

        const capture = () => ({
            sources: Object.fromEntries(
                Object.entries(sources).map(([name, element]) => [
                    name,
                    snapshot(element)
                ])
            ),
            reflected: Object.fromEntries(
                Object.entries(reflected).map(([name, element]) => [
                    name,
                    snapshot(element)
                ])
            ),
            mirrored: Object.fromEntries(
                Object.entries(mirrored).map(([name, element]) => [
                    name,
                    snapshot(element)
                ])
            ),
            projectiveCircleReflection:
                snapshot(projectiveCircleReflection),
            projectiveCircleMirror:
                snapshot(projectiveCircleMirror),
            axis: snapshotLine(axis),
            mirror: snapshotPoint(mirror),
            objectOrder: board.objectsList.map((element) => element.id)
        });
        const initial = capture();

        lineStart.setPositionDirectly(JXG.COORDS_BY_USER, [-2, -1]);
        lineEnd.setPositionDirectly(JXG.COORDS_BY_USER, [0, 3]);
        sourceCurve.dataX = [-3, -2, -1];
        sourceCurve.dataY = [0, 3, 1];
        sourcePolygon.vertices[0].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-4, -2]
        );
        circleCenter.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2, 3]
        );
        circleRadius.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [0, 3]
        );
        arcRadius.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 1]);
        sectorAngle.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2, -3]
        );
        axisEnd.setPositionDirectly(JXG.COORDS_BY_USER, [2, 5]);
        mirror.setPositionDirectly(JXG.COORDS_BY_USER, [0, 1]);
        board.update();
        const moved = capture();

        return {
            version: JXG.version,
            initial,
            moved
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
