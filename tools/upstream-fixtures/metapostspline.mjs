/*
 * Official JSXGraph 1.13.3 Metapostspline behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/metapostspline.mjs
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
        const snapshot = (curve) => ({
            elType: curve.elType,
            curveType: curve.evalVisProp("curvetype"),
            bezierDegree: curve.bezierDegree,
            numberPoints: curve.numberPoints,
            dataX: curve.dataX.map(classify),
            dataY: curve.dataY.map(classify),
            points: curve.points
                .slice(0, curve.numberPoints)
                .map((point) => [
                    classify(point.usrCoords[1]),
                    classify(point.usrCoords[2])
                ]),
            parents: [...curve.parents],
            childElements: Object.keys(curve.childElements)
        });
        const pointRelations = (points) => points.map((point) => ({
            id: point.id,
            parents: [...point.parents],
            childElements: Object.keys(point.childElements)
        }));
        const attempt = (operation) => {
            try {
                return {ok: true, value: operation()};
            } catch (error) {
                return {
                    ok: false,
                    name: error?.name ?? null,
                    message: error?.message ?? String(error)
                };
            }
        };

        const dynamicBoard = createBoard();
        const dynamicPoints = [
            dynamicBoard.create("point", [-3, -3], {id: "D0", name: ""}),
            dynamicBoard.create("point", [0, -3], {id: "D1", name: ""}),
            dynamicBoard.create("point", [4, -5], {id: "D2", name: ""}),
            dynamicBoard.create("point", [6, -2], {id: "D3", name: ""})
        ];
        let tension = 1;
        const dynamic = dynamicBoard.create(
            "metapostspline",
            [dynamicPoints, {tension: () => tension, isClosed: false}],
            {id: "dynamic", name: ""}
        );
        const dynamicInitial = snapshot(dynamic);
        tension = 2;
        dynamicPoints[2].setPosition(JXG.COORDS_BY_USER, [3, -1]);
        dynamicBoard.update();
        const dynamicUpdated = snapshot(dynamic);

        const closedBoard = createBoard();
        const closedPoints = [
            closedBoard.create("point", [-2, -1], {id: "C0", name: ""}),
            closedBoard.create("point", [2, -1], {id: "C1", name: ""}),
            closedBoard.create("point", [1, 3], {id: "C2", name: ""})
        ];
        const closed = closedBoard.create(
            "metapostspline",
            [closedPoints, {tension: 1, isClosed: true}],
            {id: "closed", name: ""}
        );

        const controlBoard = createBoard();
        const controlPoints = [
            controlBoard.create("point", [-3, -3], {id: "K0", name: ""}),
            controlBoard.create("point", [0, -3], {id: "K1", name: ""}),
            controlBoard.create("point", [4, -5], {id: "K2", name: ""}),
            controlBoard.create("point", [6, -2], {id: "K3", name: ""})
        ];
        const firstIndexOnly = controlBoard.create(
            "metapostspline",
            [
                controlPoints,
                {
                    tension: 1,
                    0: {type: "curl", curl: 2},
                    1: {type: "curl", curl: 7, direction: 45, tension: 3}
                }
            ],
            {id: "first-index-only", name: ""}
        );
        const secondIndex = controlBoard.create(
            "metapostspline",
            [
                controlPoints,
                {
                    tension: 1,
                    1: {
                        type: "curl",
                        curl: 7,
                        direction: [-30, 45],
                        tension: [2, 3]
                    }
                }
            ],
            {id: "second-index", name: ""}
        );

        const materializedBoard = createBoard();
        const materialized = materializedBoard.create(
            "metapostspline",
            [
                [[-4, 0], [-2, 3], [1, -2], [4, 2]],
                {tension: 1}
            ],
            {id: "materialized", name: ""}
        );
        const materializedPoints = materialized.parents.map(
            (id) => materializedBoard.select(id)
        );

        const separatedBoard = createBoard();
        const separated = separatedBoard.create(
            "metapostspline",
            [
                [[-4, -2, 1, 4], [0, 3, -2, 2]],
                {tension: 1}
            ],
            {
                id: "separated",
                name: "",
                isArrayOfCoordinates: false
            }
        );

        const invalidBoard = createBoard();
        const invalidPoints = [
            invalidBoard.create("point", [0, 0], {name: ""}),
            invalidBoard.create("point", [1, 1], {name: ""})
        ];
        const emptyControls = attempt(() => {
            const curve = invalidBoard.create(
                "metapostspline",
                [invalidPoints, {}],
                {name: ""}
            );
            return snapshot(curve);
        });
        const pointLikes = attempt(() => {
            const curve = invalidBoard.create(
                "metapostspline",
                [[[0, 0], [1, 1], [2, 0]], {tension: 1}],
                {name: "", createPoints: false}
            );
            return snapshot(curve);
        });
        const invalidParents = [
            attempt(() => invalidBoard.create("metapostspline", [], {})),
            attempt(
                () => invalidBoard.create(
                    "metapostspline",
                    [[[0, 0], [1, 1]], 1],
                    {}
                )
            )
        ];

        return {
            version: JXG.version,
            dynamic: {
                initial: dynamicInitial,
                updated: dynamicUpdated,
                relations: pointRelations(dynamicPoints)
            },
            closed: snapshot(closed),
            controls: {
                firstIndexOnly: snapshot(firstIndexOnly),
                secondIndex: snapshot(secondIndex)
            },
            materialized: {
                curve: snapshot(materialized),
                boardObjectCount: materializedBoard.numObjects,
                points: pointRelations(materializedPoints)
            },
            separated: snapshot(separated),
            invalid: {
                emptyControls,
                pointLikes,
                invalidParents
            }
        };
    });

    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
