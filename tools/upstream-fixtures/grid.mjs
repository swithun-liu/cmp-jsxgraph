/*
 * Official JSXGraph 1.13.3 two-dimensional Grid behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/grid.mjs
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
        '<div id="board" style="width: 500px; height: 500px"></div>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    const evidence = await page.evaluate(() => {
        const boardOptions = {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            grid: false,
            resize: {enabled: false},
            showCopyright: false,
            showNavigation: false
        };

        const round = (value) => (
            Number.isFinite(value)
                ? Math.round(value * 1000000) / 1000000
                : String(value)
        );
        const normalizedData = (values) => values
            .slice(0, 24)
            .map(round);
        const snapshotCurve = (curve) => ({
            id: curve.id,
            name: curve.name,
            type: curve.type,
            elementClass: curve.elementClass,
            elType: curve.elType,
            dump: curve.dump,
            needsRegularUpdate: curve.needsRegularUpdate,
            bezierDegree: curve.bezierDegree,
            dataLength: [curve.dataX.length, curve.dataY.length],
            pointCount: curve.points.length,
            firstDataX: normalizedData(curve.dataX),
            firstDataY: normalizedData(curve.dataY),
            attributes: {
                visible: curve.evalVisProp("visible"),
                face: curve.evalVisProp("face"),
                size: curve.evalVisProp("size"),
                margin: curve.evalVisProp("margin"),
                drawZero: curve.evalVisProp("drawzero"),
                polygonVertices:
                    curve.evalVisProp("polygonvertices"),
                majorStep: curve.evalVisProp("majorstep"),
                minorElements:
                    curve.evalVisProp("minorelements"),
                forceSquare: curve.evalVisProp("forcesquare"),
                includeBoundaries:
                    curve.evalVisProp("includeboundaries"),
                strokeColor: curve.evalVisProp("strokecolor"),
                strokeWidth: curve.evalVisProp("strokewidth"),
                strokeOpacity: curve.evalVisProp("strokeopacity"),
                fillColor: curve.evalVisProp("fillcolor"),
                fillOpacity: curve.evalVisProp("fillopacity"),
                lineCap: curve.evalVisProp("linecap")
            }
        });
        const snapshotGrid = (board, major) => ({
            major: snapshotCurve(major),
            minor: snapshotCurve(major.minorGrid),
            relationships: {
                majorMinor: major.minorGrid.majorGrid === major,
                inheritsMinor:
                    major.inherits.includes(major.minorGrid),
                registeredMajor: board.objects[major.id] === major,
                registeredMinor:
                    board.objects[major.minorGrid.id] ===
                        major.minorGrid,
                boardGridIds: board.grids.map((grid) => grid.id),
                parentIds: major.getParents().map((parent) => parent.id),
                sameParents:
                    JSON.stringify(major.getParents().map((p) => p.id)) ===
                    JSON.stringify(
                        major.minorGrid.getParents().map((p) => p.id)
                    )
            }
        });
        const withBoard = (action, options = {}) => {
            const board = JXG.JSXGraph.initBoard(
                "board",
                {...boardOptions, ...options}
            );
            try {
                return action(board);
            } finally {
                JXG.JSXGraph.freeBoard(board);
            }
        };

        const defaultGrid = withBoard((board) => {
            const major = board.create("grid", [], {});
            board.fullUpdate();
            const minor = major.minorGrid;
            const majorPoint = major.points.find((point) =>
                Number.isFinite(point.scrCoords[1]) &&
                    Number.isFinite(point.scrCoords[2])
            );
            const minorPoint = minor.points.find((point) =>
                Number.isFinite(point.scrCoords[1]) &&
                    Number.isFinite(point.scrCoords[2])
            );
            return {
                ...snapshotGrid(board, major),
                hitTesting: {
                    major: major.hasPoint(
                        majorPoint.scrCoords[1],
                        majorPoint.scrCoords[2]
                    ),
                    minor: minor.hasPoint(
                        minorPoint.scrCoords[1],
                        minorPoint.scrCoords[2]
                    )
                }
            };
        });

        const themes = Array.from({length: 7}, (_, theme) => (
            withBoard((board) => {
                const major = board.create("grid", [], {theme});
                board.fullUpdate();
                return {
                    theme,
                    major: snapshotCurve(major),
                    minor: snapshotCurve(major.minorGrid)
                };
            })
        ));

        const unitAndSquareCases = [
            {
                name: "px-percent",
                attributes: {
                    majorStep: ["50px", "25%"],
                    minorElements: [1, 2],
                    major: {face: "line"}
                }
            },
            {
                name: "fraction-max",
                attributes: {
                    majorStep: ["0.2fr", "100px"],
                    forceSquare: "max",
                    major: {face: "plus", size: "80%"},
                    minorElements: 0
                }
            },
            {
                name: "numeric-min",
                attributes: {
                    majorStep: [1, 2],
                    forceSquare: "min",
                    major: {face: "diamond2", size: [12, 8]},
                    minorElements: 0
                }
            }
        ].map(({name, attributes}) => withBoard((board) => {
            const major = board.create("grid", [], attributes);
            board.fullUpdate();
            return {name, ...snapshotGrid(board, major)};
        }));

        const boundaryAndZeroCases = [
            {
                name: "exclude-boundaries-hide-zero",
                attributes: {
                    majorStep: 1,
                    includeBoundaries: false,
                    major: {face: "plus", size: 10, drawZero: false},
                    minorElements: 1,
                    minor: {face: "point", drawZero: false}
                }
            },
            {
                name: "include-boundaries-partial-zero",
                attributes: {
                    majorStep: 1,
                    includeBoundaries: true,
                    major: {
                        face: "square",
                        size: 10,
                        drawZero: {origin: false, x: true, y: false}
                    },
                    minorElements: 1,
                    minor: {
                        face: "circle",
                        size: 4,
                        drawZero: {x: false, y: true}
                    }
                }
            }
        ].map(({name, attributes}) => withBoard((board) => {
            const major = board.create("grid", [], attributes);
            board.fullUpdate();
            return {name, ...snapshotGrid(board, major)};
        }));

        const parentAxes = withBoard((board) => {
            const xAxis = board.create(
                "axis",
                [[0, 0], [1, 0]],
                {
                    id: "xAxis",
                    ticks: {
                        ticksDistance: 2,
                        minorTicks: 1,
                        insertTicks: false
                    }
                }
            );
            const yAxis = board.create(
                "axis",
                [[0, 0], [0, 1]],
                {
                    id: "yAxis",
                    ticks: {
                        ticksDistance: 2.5,
                        minorTicks: 3,
                        insertTicks: false
                    }
                }
            );
            const major = board.create(
                "grid",
                [xAxis, yAxis],
                {
                    majorStep: "auto",
                    minorElements: "auto",
                    major: {face: "line"},
                    minor: {face: "point"}
                }
            );
            board.fullUpdate();
            return snapshotGrid(board, major);
        });

        const runtimeMutation = withBoard((board) => {
            const xAxis = board.create(
                "axis",
                [[0, 0], [1, 0]],
                {
                    id: "runtimeXAxis",
                    ticks: {
                        ticksDistance: 2,
                        minorTicks: 1,
                        insertTicks: false
                    }
                }
            );
            const yAxis = board.create(
                "axis",
                [[0, 0], [0, 1]],
                {
                    id: "runtimeYAxis",
                    ticks: {
                        ticksDistance: 2.5,
                        minorTicks: 3,
                        insertTicks: false
                    }
                }
            );
            const major = board.create(
                "grid",
                [xAxis, yAxis],
                {
                    id: "runtimeGrid",
                    majorStep: "auto",
                    minorElements: "auto",
                    major: {face: "line"},
                    minor: {face: "point"}
                }
            );
            const minor = major.minorGrid;
            board.fullUpdate();
            const before = snapshotGrid(board, major);

            major.setAttribute({
                majorStep: [2, 1],
                forceSquare: "max",
                includeBoundaries: true,
                face: "circle",
                size: 10,
                margin: 4,
                drawZero: {origin: false, x: true, y: false},
                polygonVertices: 7
            });
            minor.setAttribute({
                minorElements: [1, 2],
                includeBoundaries: true,
                face: "plus",
                size: 6,
                margin: 2,
                drawZero: {origin: false, x: false, y: true},
                polygonVertices: 5
            });
            board.fullUpdate();
            const structural = snapshotGrid(board, major);

            xAxis.setAttribute({
                position: "fixed",
                anchor: "right",
                anchorDist: "20%",
                ticks: {
                    ticksDistance: 1.5,
                    minorTicks: 2,
                    labels: ["zero", "one"],
                    label: {
                        offset: [8, -3],
                        anchorX: "middle",
                        anchorY: "top"
                    }
                }
            });
            yAxis.setAttribute({
                ticks: {ticksDistance: 3, minorTicks: 4}
            });
            major.setAttribute({
                majorStep: ["auto", "auto"],
                forceSquare: false
            });
            minor.setAttribute({
                minorElements: ["auto", "auto"]
            });
            board.fullUpdate();
            const inheritedAxisTicks = snapshotGrid(board, major);

            return {
                before,
                structural,
                inheritedAxisTicks,
                axisTicks: {
                    x: {
                        point1: xAxis.point1.coords.usrCoords.map(round),
                        ticksDistance:
                            xAxis.ticks[0].evalVisProp("ticksdistance"),
                        minorTicks:
                            xAxis.ticks[0].evalVisProp("minorticks"),
                        labels: xAxis.ticks[0].evalVisProp("labels"),
                        label: {
                            offset:
                                xAxis.ticks[0].evalVisProp("label.offset"),
                            anchorX:
                                xAxis.ticks[0].evalVisProp("label.anchorx"),
                            anchorY:
                                xAxis.ticks[0].evalVisProp("label.anchory")
                        }
                    },
                    y: {
                        ticksDistance:
                            yAxis.ticks[0].evalVisProp("ticksdistance"),
                        minorTicks:
                            yAxis.ticks[0].evalVisProp("minorticks")
                    }
                }
            };
        });

        const nestedIdentity = withBoard((board) => {
            const major = board.create(
                "grid",
                [],
                {
                    id: "outerId",
                    name: "outerName",
                    major: {
                        id: "majorId",
                        name: "majorName"
                    },
                    minor: {
                        id: "minorId",
                        name: "minorName"
                    }
                }
            );
            return snapshotGrid(board, major);
        });

        const lifecycle = withBoard((board) => {
            const major = board.create(
                "grid",
                [],
                {id: "major", name: "major"}
            );
            const minor = major.minorGrid;
            const before = {
                objectIds: Object.keys(board.objects),
                gridIds: board.grids.map((grid) => grid.id)
            };
            board.removeObject(major);
            return {
                before,
                afterRemoveMajor: {
                    objectIds: Object.keys(board.objects),
                    gridIds: board.grids.map((grid) => grid.id),
                    majorRegistered: board.objects[major.id] === major,
                    minorRegistered: board.objects[minor.id] === minor
                }
            };
        });

        const removeGrids = withBoard((board) => {
            board.create("grid", [], {id: "first"});
            board.create("grid", [], {id: "second"});
            const before = {
                objectIds: Object.keys(board.objects),
                gridIds: board.grids.map((grid) => grid.id)
            };
            board.removeGrids();
            return {
                before,
                after: {
                    objectIds: Object.keys(board.objects),
                    gridIds: board.grids.map((grid) => grid.id)
                }
            };
        });

        const failures = withBoard((board) => {
            const point = board.create("point", [1, 1], {id: "point"});
            const invalidBefore = Object.keys(board.objects);
            let invalidParent;
            try {
                board.create("grid", [point], {id: "invalid"});
                invalidParent = {threw: false};
            } catch (error) {
                invalidParent = {
                    threw: true,
                    message: error.message,
                    objectIdsAfter: Object.keys(board.objects),
                    unchanged:
                        JSON.stringify(invalidBefore) ===
                        JSON.stringify(Object.keys(board.objects))
                };
            }

            const collisionBefore = Object.keys(board.objects);
            let minorCollision;
            const create = board.create;
            let curveCreations = 0;
            board.create = function (elementType, parents, attributes) {
                if (elementType === "curve") {
                    curveCreations += 1;
                    if (curveCreations === 2) {
                        throw new Error("fixture-induced minor failure");
                    }
                }
                return create.call(this, elementType, parents, attributes);
            };
            try {
                board.create("grid", [], {id: "leakedMajor"});
                minorCollision = {threw: false};
            } catch (error) {
                minorCollision = {
                    threw: true,
                    message: error.message,
                    objectIdsBefore: collisionBefore,
                    objectIdsAfter: Object.keys(board.objects),
                    majorLeaked:
                        Object.prototype.hasOwnProperty.call(
                            board.objects,
                            "leakedMajor"
                        )
                };
            } finally {
                board.create = create;
            }
            return {invalidParent, minorCollision};
        });

        return {
            version: JXG.version,
            defaultGrid,
            themes,
            unitAndSquareCases,
            boundaryAndZeroCases,
            parentAxes,
            runtimeMutation,
            nestedIdentity,
            lifecycle,
            removeGrids,
            failures
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
