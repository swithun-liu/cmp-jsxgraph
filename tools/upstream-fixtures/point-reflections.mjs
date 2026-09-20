/*
 * Official JSXGraph 1.13.3 point-reflection creator behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/point-reflections.mjs
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
            coords: vector(point.coords.usrCoords),
            initialCoords: vector(point.initialCoords.usrCoords),
            actualCoords: vector(point.actualCoords.usrCoords),
            baseElement:
                point.baseElement === null ? null : point.baseElement.id,
            transformations: point.transformations.map(
                (transformation) => transformation.transformationType
            ),
            draggable: point.isDraggable,
            fixed: point.visProp.fixed,
            needsRegularUpdate: point.needsRegularUpdate,
            dump: point.dump,
            relations: relations(point)
        });
        const point = (board, coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const presence = (board, ids) =>
            Object.fromEntries(
                ids.map((id) => [id, Boolean(board.objects[id])])
            );

        const board = createBoard();
        const source = point(board, [-3, 1], "source");
        const mirror = point(board, [1, -1], "mirror");
        const axisFirst = point(board, [0, -4], "axisFirst");
        const axisSecond = point(board, [0, 4], "axisSecond");
        const axis = board.create("line", [axisFirst, axisSecond], {
            id: "axis",
            name: "",
            withLabel: false
        });
        const reflected = board.create("reflection", [source, axis], {
            id: "reflected",
            name: "reflected-name",
            fixed: true,
            needsRegularUpdate: false,
            withLabel: false
        });
        const reflectedDynamic = board.create(
            "reflection",
            [source, axis],
            {
                id: "reflectedDynamic",
                name: "",
                withLabel: false
            }
        );
        const mirrored = board.create(
            "mirrorelement",
            [source, mirror],
            {
                id: "mirrored",
                name: "",
                withLabel: false
            }
        );
        const mirrorAlias = board.create(
            "mirrorpoint",
            [source, mirror],
            {
                id: "mirrorAlias",
                name: "",
                withLabel: false
            }
        );
        const snapshot = () => ({
            reflected: snapshotPoint(reflected),
            reflectedDynamic: snapshotPoint(reflectedDynamic),
            mirrored: snapshotPoint(mirrored),
            mirrorAlias: snapshotPoint(mirrorAlias),
            sources: {
                source: relations(source),
                mirror: relations(mirror),
                axisFirst: relations(axisFirst),
                axisSecond: relations(axisSecond),
                axis: relations(axis)
            }
        });

        board.update();
        const initial = snapshot();
        source.setPositionDirectly(JXG.COORDS_BY_USER, [-2, 3]);
        mirror.setPositionDirectly(JXG.COORDS_BY_USER, [0, 0]);
        axisSecond.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [2, 4]
        );
        board.update();
        const moved = snapshot();

        const reflectionRemovalBoard = createBoard();
        const reflectionSource = point(
            reflectionRemovalBoard,
            [-3, 1],
            "reflectionSource"
        );
        const reflectionAxisFirst = point(
            reflectionRemovalBoard,
            [0, -4],
            "reflectionAxisFirst"
        );
        const reflectionAxisSecond = point(
            reflectionRemovalBoard,
            [0, 4],
            "reflectionAxisSecond"
        );
        const reflectionAxis = reflectionRemovalBoard.create(
            "line",
            [reflectionAxisFirst, reflectionAxisSecond],
            {
                id: "reflectionAxis",
                name: "",
                withLabel: false
            }
        );
        const reflectionOutput = reflectionRemovalBoard.create(
            "reflection",
            [reflectionSource, reflectionAxis],
            {
                id: "reflectionOutput",
                name: "",
                withLabel: false
            }
        );
        reflectionRemovalBoard.removeObject(reflectionSource);
        const reflectionAfterSourceRemoval = presence(
            reflectionRemovalBoard,
            ["reflectionSource", "reflectionAxis", "reflectionOutput"]
        );
        reflectionRemovalBoard.removeObject(reflectionAxis);
        const reflectionAfterAxisRemoval = presence(
            reflectionRemovalBoard,
            ["reflectionSource", "reflectionAxis", "reflectionOutput"]
        );

        const mirrorRemovalBoard = createBoard();
        const mirrorSource = point(
            mirrorRemovalBoard,
            [-3, 1],
            "mirrorSource"
        );
        const mirrorCenter = point(
            mirrorRemovalBoard,
            [1, -1],
            "mirrorCenter"
        );
        const mirrorOutput = mirrorRemovalBoard.create(
            "mirrorelement",
            [mirrorSource, mirrorCenter],
            {
                id: "mirrorOutput",
                name: "",
                withLabel: false
            }
        );
        mirrorRemovalBoard.removeObject(mirrorSource);
        const mirrorAfterSourceRemoval = presence(
            mirrorRemovalBoard,
            ["mirrorSource", "mirrorCenter", "mirrorOutput"]
        );
        mirrorRemovalBoard.removeObject(mirrorCenter);
        const mirrorAfterCenterRemoval = presence(
            mirrorRemovalBoard,
            ["mirrorSource", "mirrorCenter", "mirrorOutput"]
        );

        const outputRemovalBoard = createBoard();
        const outputSource = point(
            outputRemovalBoard,
            [-3, 1],
            "outputSource"
        );
        const outputCenter = point(
            outputRemovalBoard,
            [1, -1],
            "outputCenter"
        );
        const outputMirror = outputRemovalBoard.create(
            "mirrorpoint",
            [outputSource, outputCenter],
            {
                id: "outputMirror",
                name: "",
                withLabel: false
            }
        );
        outputRemovalBoard.removeObject(outputMirror);
        const outputRemoval = presence(
            outputRemovalBoard,
            ["outputSource", "outputCenter", "outputMirror"]
        );

        const failures = {};
        const failureBoard = createBoard();
        const failurePoint = point(
            failureBoard,
            [0, 0],
            "failurePoint"
        );
        const failureMirror = point(
            failureBoard,
            [1, 1],
            "failureMirror"
        );
        const failureLine = failureBoard.create(
            "line",
            [[0, -2], [0, 2]],
            {
                id: "failureLine",
                name: "",
                withLabel: false
            }
        );
        const duplicateId = "duplicate";
        point(failureBoard, [3, 3], duplicateId);
        const objectCountBeforeDuplicate =
            Object.keys(failureBoard.objects).length;
        for (const [name, callback] of Object.entries({
            reflectionCoordinateSource: () =>
                failureBoard.create(
                    "reflection",
                    [[-2, 1], failureLine]
                ),
            reflectionWrongReflector: () =>
                failureBoard.create(
                    "reflection",
                    [failurePoint, failureMirror]
                ),
            mirrorCoordinateSource: () =>
                failureBoard.create(
                    "mirrorelement",
                    [[-2, 1], failureMirror]
                ),
            mirrorCoordinateCenter: () =>
                failureBoard.create(
                    "mirrorelement",
                    [failurePoint, [1, 1]]
                ),
            mirrorpointCoordinateSource: () =>
                failureBoard.create(
                    "mirrorpoint",
                    [[-2, 1], failureMirror]
                ),
            mirrorTooFew: () =>
                failureBoard.create(
                    "mirrorelement",
                    [failurePoint]
                ),
            duplicateOutputId: () =>
                failureBoard.create(
                    "mirrorpoint",
                    [failurePoint, failureMirror],
                    {id: duplicateId}
                )
        })) {
            try {
                const output = callback();
                failures[name] = {
                    error: null,
                    output: output?.id ?? null
                };
            } catch (failure) {
                failures[name] = {
                    error: failure.message,
                    output: null
                };
            }
        }

        return {
            version: JXG.version,
            initial,
            moved,
            reflectionAfterSourceRemoval,
            reflectionAfterAxisRemoval,
            mirrorAfterSourceRemoval,
            mirrorAfterCenterRemoval,
            outputRemoval,
            failures,
            duplicateObjectCount: {
                before: objectCountBeforeDuplicate,
                after: Object.keys(failureBoard.objects).length
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
