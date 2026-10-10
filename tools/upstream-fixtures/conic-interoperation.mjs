/*
 * Official JSXGraph 1.13.3 Ellipse construction interoperation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/conic-interoperation.mjs
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
        const point = (coordinates, id) =>
            board.create("point", coordinates, {
                id,
                name: "",
                withLabel: false
            });
        const lineAttributes = (id) => ({
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
        const vector = (values) => Array.from(values, (value) => {
            if (Number.isNaN(value)) {
                return "NaN";
            }
            if (!Number.isFinite(value)) {
                return value > 0 ? "Infinity" : "-Infinity";
            }
            return value;
        });
        const snapshotPoint = (value) => ({
            id: value.id,
            coordinates: vector(value.coords.usrCoords),
            parents: [...value.parents],
            slideObject: value.slideObject?.id ?? null,
            position: value.position ?? null
        });
        const snapshotLine = (value) => ({
            id: value.id,
            elType: value.elType,
            stdform: vector(value.stdform),
            parents: [...value.parents],
            point1: snapshotPoint(value.point1),
            point2: snapshotPoint(value.point2)
        });
        const present = (elements) =>
            Object.fromEntries(
                elements.map((element) => [
                    element.id,
                    board.objects[element.id] === element
                ])
            );
        const createOpenConicScenario = (kind, prefix) => {
            let conic;
            let onPoint;
            let mutableParents;
            if (kind === "hyperbola") {
                const focus1 = point([-3, 0], `${prefix}Focus1`);
                const focus2 = point([3, 0], `${prefix}Focus2`);
                onPoint = point([5, 2], `${prefix}OnPoint`);
                conic = board.create(
                    "hyperbola",
                    [focus1, focus2, onPoint],
                    {id: prefix, name: "", withLabel: false}
                );
                mutableParents = {
                    move() {
                        onPoint.setPositionDirectly(
                            JXG.COORDS_BY_USER,
                            [6, 3]
                        );
                    }
                };
            } else {
                const focus = point([1, 1], `${prefix}Focus`);
                const directrixStart = point(
                    [-1, 5],
                    `${prefix}DirectrixStart`
                );
                const directrixEnd = point(
                    [-1, -5],
                    `${prefix}DirectrixEnd`
                );
                const directrix = board.create(
                    "line",
                    [directrixStart, directrixEnd],
                    lineAttributes(`${prefix}Directrix`)
                );
                onPoint = point([1, 3], `${prefix}OnPoint`);
                conic = board.create(
                    "parabola",
                    [focus, directrix],
                    {id: prefix, name: "", withLabel: false}
                );
                mutableParents = {
                    move() {
                        focus.setPositionDirectly(
                            JXG.COORDS_BY_USER,
                            [2, 2]
                        );
                        directrixStart.setPositionDirectly(
                            JXG.COORDS_BY_USER,
                            [-2, 5]
                        );
                        directrixEnd.setPositionDirectly(
                            JXG.COORDS_BY_USER,
                            [-2, -5]
                        );
                    }
                };
            }

            const offPoint = point([0, 0], `${prefix}OffPoint`);
            const incidencePoint = board.create(
                "point",
                [
                    () => conic.X(0),
                    () => conic.Y(0)
                ],
                {
                    id: `${prefix}IncidencePoint`,
                    name: "",
                    withLabel: false
                }
            );
            const externalPoint = point(
                kind === "hyperbola" ? [0, 6] : [-5, 1],
                `${prefix}ExternalPoint`
            );
            const line = board.create(
                "line",
                kind === "hyperbola"
                    ? [[-8, 2], [8, 2]]
                    : [[3, -6], [3, 6]],
                lineAttributes(`${prefix}SourceLine`)
            );
            const tangent = board.create(
                "tangent",
                [onPoint, conic],
                lineAttributes(`${prefix}Tangent`)
            );
            const polar = board.create(
                "polar",
                [conic, externalPoint],
                lineAttributes(`${prefix}Polar`)
            );
            const normal = board.create(
                "normal",
                [externalPoint, conic],
                lineAttributes(`${prefix}Normal`)
            );
            const pole = board.create(
                "polepoint",
                [conic, line],
                {
                    id: `${prefix}Pole`,
                    name: "",
                    withLabel: false
                }
            );
            const firstIntersection = board.create(
                "intersection",
                [line, conic, 0],
                {
                    id: `${prefix}FirstIntersection`,
                    name: "",
                    withLabel: false
                }
            );
            const otherIntersection = board.create(
                "otherintersection",
                [conic, line, firstIntersection],
                {
                    id: `${prefix}OtherIntersection`,
                    name: "",
                    withLabel: false
                }
            );
            const tangentTo = board.create(
                "tangentto",
                [conic, externalPoint, 0],
                {
                    ...lineAttributes(`${prefix}TangentTo`),
                    point: {
                        id: `${prefix}TangentToIntersection`,
                        name: "",
                        withLabel: false
                    },
                    polar: lineAttributes(`${prefix}TangentToPolar`)
                }
            );
            const glider = board.create(
                "glider",
                kind === "hyperbola"
                    ? [4, 3, conic]
                    : [3, 3, conic],
                {
                    id: `${prefix}Glider`,
                    name: "",
                    withLabel: false
                }
            );
            board.update();

            const elements = [
                conic,
                tangent,
                polar,
                normal,
                pole,
                firstIntersection,
                otherIntersection,
                tangentTo,
                glider
            ];
            const snapshot = () => ({
                plotting: {
                    doAdvancedPlot: conic.evalVisProp("doadvancedplot"),
                    plotVersion: conic.evalVisProp("plotversion"),
                    numberPoints: conic.numberPoints
                },
                quadraticform: conic.quadraticform.map(vector),
                incidence: {
                    definingPoint: onPoint.isOn(conic, 1e-6),
                    generatedPoint: incidencePoint.isOn(conic, 1e-6),
                    off: offPoint.isOn(conic, 1e-6)
                },
                tangent: snapshotLine(tangent),
                polar: snapshotLine(polar),
                normal: snapshotLine(normal),
                pole: snapshotPoint(pole),
                firstIntersection: snapshotPoint(firstIntersection),
                otherIntersection: snapshotPoint(otherIntersection),
                tangentTo: {
                    line: snapshotLine(tangentTo),
                    point: snapshotPoint(tangentTo.point),
                    polar: snapshotLine(tangentTo.polar)
                },
                glider: snapshotPoint(glider),
                present: present(elements)
            });

            const initial = snapshot();
            mutableParents.move();
            externalPoint.setPositionDirectly(
                JXG.COORDS_BY_USER,
                kind === "hyperbola" ? [1, 7] : [-6, 2]
            );
            board.update();
            const moved = snapshot();
            board.removeObject(conic);
            const removed = {
                present: present(elements),
                objectIds: Object.keys(board.objects)
            };
            return {initial, moved, removed};
        };

        const focus1 = point([-3, 0], "focus1");
        const focus2 = point([3, 0], "focus2");
        const ellipsePoint = point([0, 5], "ellipsePoint");
        const ellipse = board.create(
            "ellipse",
            [focus1, focus2, ellipsePoint],
            {id: "ellipse", name: "", withLabel: false}
        );
        const tangentPoint = point([0, 5], "tangentPoint");
        const externalPoint = point([7, 4], "externalPoint");
        const line = board.create(
            "line",
            [[-8, 4], [8, 4]],
            lineAttributes("sourceLine")
        );
        const tangent = board.create(
            "tangent",
            [tangentPoint, ellipse],
            lineAttributes("tangent")
        );
        const polar = board.create(
            "polar",
            [ellipse, externalPoint],
            lineAttributes("polar")
        );
        const normal = board.create(
            "normal",
            [externalPoint, ellipse],
            lineAttributes("normal")
        );
        const pole = board.create(
            "polepoint",
            [ellipse, line],
            {id: "pole", name: "", withLabel: false}
        );
        const firstIntersection = board.create(
            "intersection",
            [line, ellipse, 0],
            {id: "firstIntersection", name: "", withLabel: false}
        );
        const otherIntersection = board.create(
            "otherintersection",
            [ellipse, line, firstIntersection],
            {id: "otherIntersection", name: "", withLabel: false}
        );
        const tangentTo = board.create(
            "tangentto",
            [ellipse, externalPoint, 0],
            {
                ...lineAttributes("tangentTo"),
                point: {
                    id: "tangentToIntersection",
                    name: "",
                    withLabel: false
                },
                polar: lineAttributes("tangentToPolar")
            }
        );
        const glider = board.create(
            "glider",
            [0, 7, ellipse],
            {id: "glider", name: "", withLabel: false}
        );
        board.update();

        const elements = [
            ellipse,
            tangent,
            polar,
            normal,
            pole,
            firstIntersection,
            otherIntersection,
            tangentTo,
            glider
        ];
        const snapshot = () => ({
            ellipse: {
                majorAxis: ellipse.majorAxis(),
                center: snapshotPoint(ellipse.center),
                parents: [...ellipse.parents]
            },
            tangent: snapshotLine(tangent),
            polar: snapshotLine(polar),
            normal: snapshotLine(normal),
            pole: snapshotPoint(pole),
            firstIntersection: snapshotPoint(firstIntersection),
            otherIntersection: snapshotPoint(otherIntersection),
            tangentTo: {
                line: snapshotLine(tangentTo),
                point: snapshotPoint(tangentTo.point),
                polar: snapshotLine(tangentTo.polar)
            },
            glider: snapshotPoint(glider),
            present: present(elements)
        });

        const initial = snapshot();
        focus1.setPositionDirectly(JXG.COORDS_BY_USER, [-4, 1]);
        focus2.setPositionDirectly(JXG.COORDS_BY_USER, [2, -1]);
        ellipsePoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [1, 4]
        );
        externalPoint.setPositionDirectly(
            JXG.COORDS_BY_USER,
            [6, 3]
        );
        board.update();
        const moved = snapshot();

        board.removeObject(ellipse);
        const removed = {
            present: present(elements),
            objectIds: Object.keys(board.objects)
        };
        const hyperbola = createOpenConicScenario(
            "hyperbola",
            "hyperbola"
        );
        const parabola = createOpenConicScenario(
            "parabola",
            "parabola"
        );

        return {
            version: JXG.version,
            initial,
            moved,
            removed,
            hyperbola,
            parabola
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
