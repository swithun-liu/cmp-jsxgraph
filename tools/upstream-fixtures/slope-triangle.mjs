/*
 * Official JSXGraph 1.13.3 SlopeTriangle behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   CHROME_BIN=/path/to/chrome node tools/upstream-fixtures/slope-triangle.mjs
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
            return value;
        };
        const vector = (values) => values.map(classify);
        const coordinates = (point) => vector(point.coords.usrCoords);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors),
            descendants: Object.keys(element.descendants)
        });
        const pointSnapshot = (point) => ({
            id: point.id,
            elType: point.elType,
            type: point.type,
            coordinates: coordinates(point),
            fixed: point.visProp.fixed,
            visible: point.visProp.visible,
            dump: point.dump,
            relations: relations(point)
        });
        const lineSnapshot = (line) => ({
            id: line.id,
            elType: line.elType,
            type: line.type,
            point1: line.point1.id,
            point2: line.point2.id,
            straightFirst: line.visProp.straightfirst,
            straightLast: line.visProp.straightlast,
            firstArrow: line.visProp.firstarrow,
            lastArrow: line.visProp.lastarrow,
            visible: line.visProp.visible,
            dump: line.dump,
            relations: relations(line)
        });
        const triangleSnapshot = (triangle, board) => ({
            id: triangle.id,
            elType: triangle.elType,
            type: triangle.type,
            elementClass: triangle.elementClass,
            privateTangent: triangle._isPrivateTangent,
            slope: classify(triangle.Slope()),
            angleRadians: classify(triangle.getAngle()),
            angleDegrees: classify(triangle.getAngle("degrees")),
            deltaX: classify(triangle.DeltaX()),
            deltaY: classify(triangle.DeltaY()),
            direction: vector(triangle.Direction()),
            methodMap: Object.fromEntries(
                [
                    "Value",
                    "V",
                    "Slope",
                    "Angle",
                    "getAngle",
                    "DeltaX",
                    "DeltaY",
                    "Direction"
                ].map((name) => [name, triangle.methodMap[name]])
            ),
            vertices: triangle.vertices.map(pointSnapshot),
            borders: triangle.borders.map(lineSnapshot),
            tangent: lineSnapshot(triangle.tangent),
            glider: pointSnapshot(triangle.glider),
            basepoint: pointSnapshot(triangle.basepoint),
            baseline: lineSnapshot(triangle.baseline),
            toppoint: pointSnapshot(triangle.toppoint),
            label: {
                id: triangle.label.id,
                elType: triangle.label.elType,
                plaintext: triangle.label.plaintext,
                coordinates: coordinates(triangle.label),
                visible: triangle.label.visProp.visible,
                dump: triangle.label.dump,
                parents: [...triangle.label.parents]
            },
            aliases: {
                horizontal: triangle.borderHorizontal.id,
                vertical: triangle.borderVertical.id,
                parallel: triangle.borderParallel.id
            },
            subs: Object.fromEntries(
                Object.entries(triangle.subs).map(([key, element]) => [
                    key,
                    element.id
                ])
            ),
            inherits: triangle.inherits.map((element) => element.id),
            relations: relations(triangle),
            objectOrder: board.objectsList.map((element) => element.id)
        });
        const point = (board, coords, id) =>
            board.create("point", coords, {
                id,
                name: "",
                withLabel: false
            });
        const segment = (board, first, second, id) =>
            board.create("segment", [first, second], {
                id,
                name: "",
                withLabel: false
            });
        const triangleAttributes = (id) => ({
            id,
            name: "",
            digits: 3,
            prefix: "m=",
            suffix: "!",
            basepoint: {id: `${id}Base`, name: ""},
            baseline: {id: `${id}Baseline`, name: ""},
            glider: {id: `${id}Glider`, name: ""},
            toppoint: {id: `${id}Top`, name: ""},
            tangent: {id: `${id}Tangent`, name: ""}
        });
        const captureFailure = (create) => {
            const board = createBoard();
            const before = board.objectsList.map((element) => element.id);
            try {
                create(board);
                return {
                    threw: false,
                    before,
                    after: board.objectsList.map((element) => element.id)
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

        const tangentBoard = createBoard();
        const tangentA = point(tangentBoard, [-3, -1], "tangentA");
        const tangentB = point(tangentBoard, [3, 2], "tangentB");
        const tangentSource = segment(
            tangentBoard,
            tangentA,
            tangentB,
            "tangentSource"
        );
        const tangentPoint = tangentBoard.create(
            "glider",
            [0, 0.5, tangentSource],
            {id: "tangentPoint", name: ""}
        );
        const tangent = tangentBoard.create(
            "tangent",
            [tangentSource, tangentPoint],
            {id: "publicTangent", name: ""}
        );
        const tangentTriangle = tangentBoard.create(
            "slopetriangle",
            [tangent],
            triangleAttributes("tangentTriangle")
        );
        tangentBoard.update();
        const tangentInitial = triangleSnapshot(
            tangentTriangle,
            tangentBoard
        );
        tangentTriangle.glider.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-1.5, tangentPoint.Y()]
        );
        tangentBoard.update(tangentTriangle.glider);
        const tangentResized = triangleSnapshot(
            tangentTriangle,
            tangentBoard
        );
        tangentB.setPositionDirectly(JXG.COORDS_BY_USER, [3, 5]);
        tangentBoard.update();
        const tangentMoved = triangleSnapshot(
            tangentTriangle,
            tangentBoard
        );
        const tangentRemovalIds = tangentBoard.objectsList.map(
            (element) => element.id
        );
        tangentBoard.removeObject(tangentTriangle);
        const tangentRemoval = Object.fromEntries(
            tangentRemovalIds.map((id) => [
                id,
                Boolean(tangentBoard.objects[id])
            ])
        );

        const gliderBoard = createBoard();
        const gliderA = point(gliderBoard, [-4, 2], "gliderA");
        const gliderB = point(gliderBoard, [4, -2], "gliderB");
        const gliderSource = segment(
            gliderBoard,
            gliderA,
            gliderB,
            "gliderSource"
        );
        const sourceGlider = gliderBoard.create(
            "glider",
            [0, 0, gliderSource],
            {id: "sourceGlider", name: ""}
        );
        const gliderTriangle = gliderBoard.create(
            "slopetriangle",
            [sourceGlider],
            triangleAttributes("gliderTriangle")
        );
        gliderBoard.update();
        const gliderInitial = triangleSnapshot(
            gliderTriangle,
            gliderBoard
        );
        const privateTangentId = gliderTriangle.tangent.id;
        const gliderRemovalIds = gliderBoard.objectsList.map(
            (element) => element.id
        );
        gliderBoard.removeObject(gliderTriangle);
        const gliderRemoval = {
            privateTangentId,
            presence: Object.fromEntries(
                gliderRemovalIds.map((id) => [
                    id,
                    Boolean(gliderBoard.objects[id])
                ])
            )
        };

        const linePointBoard = createBoard();
        const linePointA = point(linePointBoard, [-2, 3], "linePointA");
        const linePointB = point(linePointBoard, [2, -3], "linePointB");
        const linePointSource = segment(
            linePointBoard,
            linePointA,
            linePointB,
            "linePointSource"
        );
        const linePoint = linePointBoard.create(
            "glider",
            [0, 0, linePointSource],
            {id: "linePoint", name: ""}
        );
        const linePointTriangle = linePointBoard.create(
            "slopetriangle",
            [linePointSource, linePoint],
            triangleAttributes("linePointTriangle")
        );
        linePointBoard.update();

        const failures = {
            noParents: captureFailure((board) => {
                board.create("slopetriangle", [], {id: "candidate"});
            }),
            pointParent: captureFailure((board) => {
                const candidatePoint = point(board, [0, 0], "point");
                board.create(
                    "slopetriangle",
                    [candidatePoint],
                    {id: "candidate"}
                );
            }),
            duplicateBasepoint: captureFailure((board) => {
                const a = point(board, [-2, 0], "a");
                const b = point(board, [2, 2], "b");
                const source = segment(board, a, b, "source");
                const sourcePoint = board.create(
                    "glider",
                    [0, 1, source],
                    {id: "sourcePoint"}
                );
                point(board, [5, 5], "duplicate");
                board.create(
                    "slopetriangle",
                    [source, sourcePoint],
                    {
                        id: "candidate",
                        basepoint: {id: "duplicate"}
                    }
                );
            })
        };

        return {
            version: JXG.version,
            tangent: {
                initial: tangentInitial,
                resized: tangentResized,
                moved: tangentMoved,
                removal: tangentRemoval
            },
            glider: {
                initial: gliderInitial,
                removal: gliderRemoval
            },
            linePoint: triangleSnapshot(
                linePointTriangle,
                linePointBoard
            ),
            failures
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
