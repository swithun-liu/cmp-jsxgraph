/*
 * Official JSXGraph 1.13.3 View3D / Point3D lifecycle fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/view3d-point3d.mjs
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
        const matrix = (rows) => rows.map(vector);
        const createBoard = () => {
            const id = `board-${boardIndex}`;
            boardIndex += 1;
            const container = document.createElement("div");
            container.id = id;
            container.style.width = "640px";
            container.style.height = "480px";
            document.querySelector("#fixtures").appendChild(container);
            return JXG.JSXGraph.initBoard(id, {
                boundingbox: [-8, 8, 8, -8],
                axis: false,
                showCopyright: false,
                showNavigation: false
            });
        };
        const createView = (board, projection, id) =>
            board.create(
                "view3d",
                [
                    [-5, -4],
                    [8, 7],
                    [[-5, 5], [-4, 6], [-3, 7]]
                ],
                {
                    id,
                    name: "",
                    projection,
                    axesPosition: "none",
                    depthOrder: {enabled: false},
                    az: {slider: {visible: false, start: 1}},
                    el: {slider: {visible: false, start: 0.3}},
                    bank: {slider: {visible: false, start: 0}}
                }
            );
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            descendants: Object.keys(element.descendants),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshotView = (view) => ({
            id: view.id,
            elType: view.elType,
            type: view.type,
            elementClass: view.elementClass,
            llftCorner: vector(view.llftCorner),
            size: vector(view.size),
            bbox3D: matrix(view.bbox3D),
            angles: {
                az: classify(view.angles.az),
                el: classify(view.angles.el),
                bank: classify(view.angles.bank)
            },
            projectionType: view.projectionType,
            matrix3DRot: matrix(view.matrix3DRot),
            matrix3DRotShift: matrix(view.matrix3DRotShift),
            matrix3D: matrix(view.matrix3D),
            viewPortTransform: view.viewPortTransform
                ? matrix(view.viewPortTransform)
                : null,
            boxToCam: matrix(view.boxToCam),
            focalDist: classify(view.focalDist),
            namedObjectIds: Object.keys(view.objects).filter(
                (objectId) => !objectId.startsWith("jxgBoard")
            )
        });
        const snapshotPoint = (point) => ({
            id: point.id,
            name: point.name,
            elType: point.elType,
            type: point.type,
            elementClass: point.elementClass,
            is3D: point.is3D,
            coordinates: vector(point.coords),
            initialCoordinates: vector(point.initialCoords),
            finite: point.testIfFinite(),
            draggable2D: point.element2D.draggable(),
            element2D: {
                id: point.element2D.id,
                coordinates: vector(point.element2D.coords.usrCoords),
                parents: [...point.element2D.parents],
                dump: point.element2D.dump,
                viewId: point.element2D.view.id
            },
            transformations: point.transformations.map(
                (transform) => transform.transformationType
            ),
            baseElement: point.baseElement?.id ?? null,
            zIndex: classify(point.zIndex),
            relations: relations(point)
        });
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
        const view = createView(board, "parallel", "view");
        let dynamic = [2, -1, 3];
        let dynamicX = -2;
        const numeric = view.create("point3d", [1, 2, 2], {
            id: "numeric",
            name: "",
            withLabel: false,
            size: 4
        });
        const homogeneous = view.create("point3d", [2, 4, 6, 8], {
            id: "homogeneous",
            name: "",
            withLabel: false
        });
        const functionPoint = view.create("point3d", () => dynamic, {
            id: "functionPoint",
            name: "",
            withLabel: false,
            fixed: true
        });
        const mixedPoint = view.create(
            "point3d",
            [() => dynamicX, 3, () => numeric.Z() - 1],
            {
                id: "mixedPoint",
                name: "",
                withLabel: false,
                fixed: true
            }
        );
        const translation = view.create(
            "transform3d",
            [2, -3, 4],
            {type: "translate"}
        );
        const transformed = view.create(
            "point3d",
            [numeric, translation],
            {
                id: "transformed",
                name: "",
                withLabel: false,
                fixed: true
            }
        );

        board.update();
        const initial = {
            view: snapshotView(view),
            points: [
                numeric,
                homogeneous,
                functionPoint,
                mixedPoint,
                transformed
            ].map(snapshotPoint),
            projection: vector(view.project3DTo2D([1, 1, 2, 2])),
            cubeClamp: (() => {
                const result = view.project3DToCube([1, -8, 9, -4]);
                return [vector(result[0]), result[1]];
            })(),
            inside: view.isInCube([1, 1, 2, 2]),
            outside: view.isInCube([1, 8, 2, 2]),
            intersectionPositive: classify(
                view.intersectionLineCube(
                    [1, 0, 0, 0],
                    [1, 2, 3],
                    Number.POSITIVE_INFINITY
                )
            ),
            intersectionNegative: classify(
                view.intersectionLineCube(
                    [1, 0, 0, 0],
                    [1, 2, 3],
                    Number.NEGATIVE_INFINITY
                )
            )
        };

        dynamic = [-3, 4, 1];
        dynamicX = 5;
        numeric.setPosition([2, -2, 1]);
        board.update();
        const updated = [
            numeric,
            functionPoint,
            mixedPoint,
            transformed
        ].map(snapshotPoint);

        translation.applyOnce(numeric);
        board.update();
        const afterApplyOnce = snapshotPoint(numeric);

        numeric.setPosition([2, 8, 10, 12]);
        board.update();
        const homogeneousSetPosition = snapshotPoint(numeric);

        view.removeObject(functionPoint);
        const afterRemoval = {
            boardHasPoint: Boolean(board.objects.functionPoint),
            boardHasProxy: Boolean(
                board.objects[initial.points[2].element2D.id]
            ),
            viewHasPoint: Boolean(view.objects.functionPoint)
        };

        const centralBoard = createBoard();
        const centralView = createView(
            centralBoard,
            "central",
            "centralView"
        );
        const centralPoint = centralView.create(
            "point3d",
            [1, 2, 2],
            {
                id: "centralPoint",
                name: "",
                withLabel: false
            }
        );
        centralBoard.update();
        const central = {
            view: snapshotView(centralView),
            point: snapshotPoint(centralPoint),
            projection: vector(
                centralView.project3DTo2D([1, 1, 2, 2])
            ),
            worldToFocalHomogeneous: vector(
                centralView.worldToFocal([1, 1, 2, 2])
            ),
            worldToFocalAffine: vector(
                centralView.worldToFocal([1, 1, 2, 2], false)
            )
        };

        return {
            version: JXG.version,
            initial,
            updated,
            afterApplyOnce,
            homogeneousSetPosition,
            afterRemoval,
            central,
            malformed: {
                noCoordinates: captureError(
                    () => view.create("point3d", [], {name: ""})
                ),
                twoCoordinates: captureError(
                    () => view.create("point3d", [1, 2], {name: ""})
                )
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
