/*
 * Official JSXGraph 1.13.3 interpolation and spline-kernel fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/numerics-interpolation.mjs
 */
import {createRequire} from "node:module";
import {mkdtempSync, rmSync} from "node:fs";
import {tmpdir} from "node:os";
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
const userDataDir = mkdtempSync(
    resolve(tmpdir(), "cmp-jsxgraph-numerics-")
);

const browser = await puppeteer.launch({
    headless: true,
    executablePath,
    userDataDir,
    args: [
        "--disable-breakpad",
        "--disable-crash-reporter",
        "--disable-dev-shm-usage",
        "--no-sandbox"
    ]
});

try {
    const page = await browser.newPage();
    await page.setContent(
        '<main><div id="board" style="width:640px;height:480px"></div></main>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const Numerics = JXG.Math.Numerics;
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-10, 10, 10, -10],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const classify = (value) => {
            if (Array.isArray(value)) {
                return value.map(classify);
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
            if (value === undefined) {
                return "undefined";
            }
            if (value && typeof value === "object") {
                return Object.fromEntries(
                    Object.entries(value).map(([key, entry]) => [
                        key,
                        classify(entry)
                    ])
                );
            }
            return value;
        };
        const point = ([x, y]) => board.create("point", [x, y], {
            name: "",
            withLabel: false
        });
        const points = (coordinates) => coordinates.map(point);
        const sample = (curve, parameters, suspendedUpdate = false) =>
            parameters.map((parameter) => [
                parameter,
                curve[0](parameter, suspendedUpdate),
                curve[1](parameter, suspendedUpdate)
            ]);
        const capture = (action) => {
            try {
                return {
                    threw: false,
                    value: action()
                };
            } catch (error) {
                return {
                    threw: true,
                    name: error?.name ?? null,
                    message: String(error?.message ?? error)
                };
            }
        };

        const nevillePoints = points([
            [0, -2],
            [-1.5, 5],
            [1, 4],
            [3, 3]
        ]);
        const neville = Numerics.Neville(nevillePoints);
        const nevilleInitial = {
            domain: [neville[2], neville[3]()],
            samples: sample(neville, [0.5, 1, 4])
        };
        nevillePoints[1].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [-2, 7]
        );
        const nevilleMoved = [
            neville[0](0.5),
            neville[1](0.5, true)
        ];
        const nevilleUninitialized = Numerics.Neville(points([
            [2, 3],
            [4, 5]
        ]));

        const lagrangePoints = points([
            [-1, 2],
            [0, 0],
            [2, 1]
        ]);
        const lagrange = Numerics.lagrangePolynomial(lagrangePoints);
        const lagrangeInitial = {
            values: [-2, -1, 0.5, 0.75, 3].map((value) => [
                value,
                lagrange(value, value === 0.75)
            ]),
            coefficients: lagrange.getCoefficients(),
            term: lagrange.getTerm(),
            customTerm: lagrange.getTerm(2, "t", " * ")
        };
        lagrangePoints[2].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [3, 4]
        );
        const lagrangeMoved = {
            value: lagrange(1),
            coefficients: lagrange.getCoefficients(),
            term: lagrange.getTerm(3, "z", "*")
        };
        const duplicateLagrange = Numerics.lagrangePolynomial(points([
            [1, 2],
            [1, 3]
        ]));

        const cardinalPoints = points([
            [0, 0],
            [1, 2],
            [3, 3],
            [4, -1]
        ]);
        const uniformCardinal = Numerics.CardinalSpline(
            cardinalPoints,
            0.25
        );
        const centripetalCardinal = Numerics.CatmullRomSpline(
            cardinalPoints,
            "centripetal"
        );
        const uniformCardinalSamples = sample(
            uniformCardinal,
            [-1, 0.25, 1, 1.5, 2.75, 3, 3.25, 4]
        );
        const centripetalCardinalSamples = sample(
            centripetalCardinal,
            [0.25, 1.5, 2.75]
        );
        let tension = 0.25;
        let tensionEvaluations = 0;
        const dynamicCardinal = Numerics.CardinalSpline(
            cardinalPoints,
            () => {
                tensionEvaluations += 1;
                return tension;
            },
            "centripetal"
        );
        const dynamicCardinalInitial = {
            x: dynamicCardinal[0](0.5),
            y: dynamicCardinal[1](0.5),
            suspended: sample(dynamicCardinal, [0.75], true),
            tensionEvaluations
        };
        cardinalPoints[1].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [2, 5]
        );
        tension = 0.75;
        const dynamicCardinalMoved = {
            stale: sample(dynamicCardinal, [0.5], true),
            refreshed: sample(dynamicCardinal, [0.5]),
            tensionEvaluations
        };
        const duplicateCardinal = Numerics.CardinalSpline(
            points([
                [1, 2],
                [1, 2],
                [3, 4]
            ]),
            0.5,
            "centripetal"
        );

        const bezierPoints = points([
            [0, 0],
            [1, 3],
            [2, -1],
            [4, 2],
            [5, 5],
            [7, 0],
            [8, 1]
        ]);
        const bezier = Numerics.bezier(bezierPoints);
        const bezierInitial = {
            domain: [bezier[2], bezier[3]()],
            samples: sample(bezier, [-1, 0.25, 0.5, 0.999, 1.5, 2, 3])
        };
        bezierPoints[1].setPositionDirectly(
            JXG.COORDS_BY_USER,
            [2, 6]
        );
        const bezierMovedSuspended = sample(bezier, [0.5], true);
        const incompleteBezier = Numerics.bezier(
            bezierPoints.concat(points([
                [100, 100],
                [200, 200]
            ]))
        );
        const uninitializedBezier = Numerics.bezier(points([
            [0, 0],
            [1, 1],
            [2, 2],
            [3, 3]
        ]));

        const bsplinePoints = points([
            [0, 0],
            [1, 3],
            [2, -1],
            [4, 2],
            [5, 5],
            [7, 0]
        ]);
        const quadraticBSpline = Numerics.bspline(bsplinePoints, 3);
        const cubicBSpline = Numerics.bspline(bsplinePoints, 4);
        const excessiveBSpline = Numerics.bspline(bsplinePoints, 7);
        const zeroOrderBSpline = Numerics.bspline(bsplinePoints, 0);

        let regressionDegree = 1;
        let regressionDegreeEvaluations = 0;
        const regressionX = [-2, -1, 0, 1, 2];
        const regressionY = [5, 2, 1, 2, 5];
        const regression = Numerics.regressionPolynomial(
            () => {
                regressionDegreeEvaluations += 1;
                return regressionDegree;
            },
            regressionX.map((_, index) => () => regressionX[index]),
            regressionY.map((_, index) => () => regressionY[index])
        );
        const regressionInitial = {
            value: regression(0.5),
            term: regression.getTerm(),
            degreeEvaluations: regressionDegreeEvaluations
        };
        regressionDegree = 2;
        regressionX[0] = -3;
        regressionY[0] = 10;
        const regressionMoved = {
            suspended: regression(0.5, true),
            staleTerm: regression.getTerm(),
            refreshed: regression(0.5),
            refreshedTerm: regression.getTerm(),
            degreeEvaluations: regressionDegreeEvaluations
        };
        const fractionalRegression = Numerics.regressionPolynomial(
            2.9,
            [-2, -1, 0, 1, 2],
            [5, 2, 1, 2, 5]
        );

        return classify({
            version: JXG.version,
            neville: {
                initial: nevilleInitial,
                moved: nevilleMoved,
                uninitializedSuspended: [
                    nevilleUninitialized[0](0.25, true),
                    nevilleUninitialized[0](0, true)
                ]
            },
            lagrange: {
                initial: lagrangeInitial,
                moved: lagrangeMoved,
                duplicate: {
                    atNode: duplicateLagrange(1),
                    away: duplicateLagrange(2),
                    coefficients: duplicateLagrange.getCoefficients(),
                    term: duplicateLagrange.getTerm()
                }
            },
            cardinal: {
                uniform: uniformCardinalSamples,
                centripetal: centripetalCardinalSamples,
                dynamicInitial: dynamicCardinalInitial,
                dynamicMoved: dynamicCardinalMoved,
                duplicateFallback: sample(
                    duplicateCardinal,
                    [0.25, 1, 1.5, 3]
                )
            },
            bezier: {
                initial: bezierInitial,
                movedSuspended: bezierMovedSuspended,
                incomplete: {
                    end: incompleteBezier[3](),
                    samples: sample(incompleteBezier, [2, 5])
                },
                uninitializedSuspended: {
                    interior: sample(uninitializedBezier, [0.5], true),
                    endpointX: capture(() =>
                        uninitializedBezier[0](1, true)
                    ),
                    endpointY: capture(() =>
                        uninitializedBezier[1](1, true)
                    )
                }
            },
            bspline: {
                quadratic: sample(
                    quadraticBSpline,
                    [-1, 0.25, 0.5, 1, 1.5, 2, 2.5, 3, 4, 6, NaN]
                ),
                cubic: sample(cubicBSpline, [0.25, 0.5, 1, 1.5, 2.5, 3]),
                excessive: sample(excessiveBSpline, [0.25, 0.5, 1]),
                zeroOrder: sample(zeroOrderBSpline, [-1, 0.25, 6, 7])
            },
            regression: {
                initial: regressionInitial,
                moved: regressionMoved,
                fractionalDegree: {
                    value: fractionalRegression(0.5),
                    term: fractionalRegression.getTerm()
                }
            }
        });
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
    rmSync(userDataDir, {recursive: true, force: true});
}
