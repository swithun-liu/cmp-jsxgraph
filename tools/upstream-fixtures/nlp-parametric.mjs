/*
 * Official JSXGraph 1.13.3 COBYLA and parametric projection fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/nlp-parametric.mjs
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
        const unconstrainedVariables = [3, -4];
        const unconstrainedStatus = JXG.Math.Nlp.FindMinimum(
            (n, m, variables) =>
                (variables[0] - 1) * (variables[0] - 1) +
                (variables[1] + 2) * (variables[1] + 2),
            2,
            0,
            unconstrainedVariables,
            1,
            1e-7,
            0,
            400
        );
        const unconstrainedEvaluations =
            JXG.Math.Nlp.GetLastNumberOfEvaluations();

        const constrainedVariables = [2, 2];
        const constrainedStatus = JXG.Math.Nlp.FindMinimum(
            (n, m, variables, constraints) => {
                constraints[0] =
                    variables[0] + variables[1] - 1;
                return (
                    variables[0] * variables[0] +
                    variables[1] * variables[1]
                );
            },
            2,
            1,
            constrainedVariables,
            1,
            1e-7,
            0,
            400
        );
        const constrainedEvaluations =
            JXG.Math.Nlp.GetLastNumberOfEvaluations();

        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-6, 5, 6, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const view = board.create(
            "view3d",
            [
                [-5, -4],
                [10, 8],
                [[-5, 5], [-4, 4], [-3, 5]]
            ],
            {
                name: "",
                projection: "parallel",
                axesPosition: "none",
                xPlaneRear: {visible: false, type: "wireframe"},
                yPlaneRear: {visible: false, type: "wireframe"},
                zPlaneRear: {visible: false, type: "wireframe"},
                depthOrder: {enabled: false},
                az: {slider: {visible: false, start: 1}},
                el: {slider: {visible: false, start: 0.3}},
                bank: {slider: {visible: false, start: 0}}
            }
        );
        const curve = view.create(
            "curve3d",
            [
                (parameter) => Math.cos(parameter),
                (parameter) => Math.sin(parameter),
                (parameter) => parameter / Math.PI,
                [0, 2 * Math.PI]
            ],
            {name: "", numberPointsHigh: 5}
        );
        const surface = view.create(
            "parametricsurface3d",
            [
                (u, v) => u,
                (u, v) => v,
                (u, v) => u + v,
                [-2, 2],
                [-2, 2]
            ],
            {name: "", stepsU: 1, stepsV: 1}
        );
        board.update();

        const curveParameters = [0.3];
        const curveProjection = curve.projectCoords(
            [1, 0.2, 1.2, 0.7],
            curveParameters
        );
        const surfaceParameters = [0.1, 0.1];
        const surfaceProjection = surface.projectCoords(
            [1, 0.8, -0.4, 0.2],
            surfaceParameters
        );

        return {
            version: JXG.version,
            unconstrained: {
                status: unconstrainedStatus,
                variables: unconstrainedVariables,
                evaluations: unconstrainedEvaluations
            },
            constrained: {
                status: constrainedStatus,
                variables: constrainedVariables,
                evaluations: constrainedEvaluations
            },
            curve: {
                projection: curveProjection,
                parameters: curveParameters
            },
            surface: {
                projection: surfaceProjection,
                parameters: surfaceParameters
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
