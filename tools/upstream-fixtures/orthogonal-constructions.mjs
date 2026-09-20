/*
 * Official JSXGraph 1.13.3 orthogonal-construction behavior fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/orthogonal-constructions.mjs
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
            return value;
        };
        const coordinates = (pointOrCoords) =>
            (pointOrCoords.coords || pointOrCoords).usrCoords.map(classify);
        const relations = (element) => ({
            parents: [...element.parents],
            children: Object.keys(element.childElements),
            ancestors: Object.keys(element.ancestors)
        });
        const snapshot = (element) => {
            const value = {
                id: element.id,
                elType: element.elType,
                type: element.type,
                elementClass: element.elementClass,
                relations: relations(element)
            };
            if (element.coords) {
                value.coords = coordinates(element);
            }
            if (element.stdform) {
                value.stdform = element.stdform.map(classify);
            }
            if (element.point1 && element.point2) {
                value.point1 = {
                    id: element.point1.id,
                    coords: coordinates(element.point1)
                };
                value.point2 = {
                    id: element.point2.id,
                    coords: coordinates(element.point2)
                };
                value.straightFirst = element.visProp.straightfirst;
                value.straightLast = element.visProp.straightlast;
            }
            if (element.point) {
                value.point = snapshot(element.point);
            }
            return value;
        };
        const construction = (
            board,
            type,
            parents,
            id,
            extraAttributes = {}
        ) => {
            const attributes = {
                withLabel: false,
                ...extraAttributes
            };
            if (id !== undefined) {
                attributes.id = id;
                attributes.name = id;
            }
            return board.create(type, parents, attributes);
        };

        const board = createBoard();
        const first = construction(board, "point", [-4, -1], "first");
        const second = construction(board, "point", [2, 3], "second");
        const line = construction(board, "line", [first, second], "baseLine");
        const off = construction(board, "point", [3, -3], "off");
        const on = construction(board, "point", [-1, 1], "on");

        const projection = construction(
            board,
            "orthogonalprojection",
            [off, line],
            "projection"
        );
        const projectionReversed = construction(
            board,
            "orthogonalprojection",
            [line, off],
            "projectionReversed"
        );
        const perpendicularPoint = construction(
            board,
            "perpendicularpoint",
            [off, line],
            "perpendicularPoint"
        );
        const perpendicularPointOn = construction(
            board,
            "perpendicularpoint",
            [on, line],
            "perpendicularPointOn"
        );
        const perpendicularPointFirst = construction(
            board,
            "perpendicularpoint",
            [line, first],
            "perpendicularPointFirst"
        );
        const perpendicularPointSecond = construction(
            board,
            "perpendicularpoint",
            [second, line],
            "perpendicularPointSecond"
        );
        const perpendicular = construction(
            board,
            "perpendicular",
            [line, off],
            "perpendicular"
        );
        const perpendicularReversed = construction(
            board,
            "perpendicular",
            [off, line],
            "perpendicularReversed"
        );
        const perpendicularSegment = construction(
            board,
            "perpendicularsegment",
            [line, off],
            "perpendicularSegment"
        );
        const perpendicularSegmentExtended = construction(
            board,
            "perpendicularsegment",
            [line, off],
            "perpendicularSegmentExtended",
            {
                straightFirst: true,
                straightLast: true
            }
        );
        const perpendicularSegmentOn = construction(
            board,
            "perpendicularsegment",
            [on, line],
            "perpendicularSegmentOn"
        );
        const perpendicularSegmentFirst = construction(
            board,
            "perpendicularsegment",
            [line, first],
            "perpendicularSegmentFirst"
        );
        const perpendicularSegmentSecond = construction(
            board,
            "perpendicularsegment",
            [second, line],
            "perpendicularSegmentSecond"
        );

        const named = {
            projection,
            projectionReversed,
            perpendicularPoint,
            perpendicularPointOn,
            perpendicularPointFirst,
            perpendicularPointSecond,
            perpendicular,
            perpendicularReversed,
            perpendicularSegment,
            perpendicularSegmentExtended,
            perpendicularSegmentOn,
            perpendicularSegmentFirst,
            perpendicularSegmentSecond
        };
        const initial = {
            elements: Object.fromEntries(
                Object.entries(named).map(([name, element]) => [
                    name,
                    snapshot(element)
                ])
            ),
            sourceRelations: {
                first: relations(first),
                second: relations(second),
                line: relations(line),
                off: relations(off),
                on: relations(on)
            },
            perpendicularResults: {
                off: (() => {
                    const result = JXG.Math.Geometry.perpendicular(
                        line,
                        off,
                        board
                    );
                    return {
                        coords: coordinates(result[0]),
                        change: result[1]
                    };
                })(),
                on: (() => {
                    const result = JXG.Math.Geometry.perpendicular(
                        line,
                        on,
                        board
                    );
                    return {
                        coords: coordinates(result[0]),
                        change: result[1]
                    };
                })(),
                first: (() => {
                    const result = JXG.Math.Geometry.perpendicular(
                        line,
                        first,
                        board
                    );
                    return {
                        coords: coordinates(result[0]),
                        change: result[1]
                    };
                })(),
                second: (() => {
                    const result = JXG.Math.Geometry.perpendicular(
                        line,
                        second,
                        board
                    );
                    return {
                        coords: coordinates(result[0]),
                        change: result[1]
                    };
                })()
            }
        };

        off.setPositionDirectly(JXG.COORDS_BY_USER, [0, 4]);
        first.setPositionDirectly(JXG.COORDS_BY_USER, [-5, 2]);
        board.update();
        const moved = Object.fromEntries(
            Object.entries(named).map(([name, element]) => [
                name,
                snapshot(element)
            ])
        );

        const degenerateBoard = createBoard();
        const degenerateFirst = construction(
            degenerateBoard,
            "point",
            [2, 1],
            "degenerateFirst"
        );
        const degenerateSecond = construction(
            degenerateBoard,
            "point",
            [2, 1],
            "degenerateSecond"
        );
        const degenerateLine = construction(
            degenerateBoard,
            "line",
            [degenerateFirst, degenerateSecond],
            "degenerateLine"
        );
        const degeneratePoint = construction(
            degenerateBoard,
            "point",
            [-1, 4],
            "degeneratePoint"
        );
        const degenerate = {};
        for (
            const type of [
                "orthogonalprojection",
                "perpendicularpoint",
                "perpendicular",
                "perpendicularsegment"
            ]
        ) {
            try {
                degenerate[type] = snapshot(
                    construction(
                        degenerateBoard,
                        type,
                        [degeneratePoint, degenerateLine],
                        `degenerate-${type}`
                    )
                );
            } catch (error) {
                degenerate[type] = {
                    error: String(error)
                };
            }
        }

        const removalCase = (type, coordinateParent) => {
            const removalBoard = createBoard();
            const a = construction(removalBoard, "point", [-4, -1], "a");
            const b = construction(removalBoard, "point", [2, 3], "b");
            const sourceLine = construction(
                removalBoard,
                "line",
                [a, b],
                "sourceLine"
            );
            const source = coordinateParent
                ? [3, -3]
                : construction(removalBoard, "point", [3, -3], "source");
            const previousIds = new Set(
                removalBoard.objectsList.map((element) => element.id)
            );
            const output = construction(
                removalBoard,
                type,
                [sourceLine, source],
                coordinateParent ? undefined : "output"
            );
            const sourcePoint = coordinateParent
                ? (() => {
                    return removalBoard.objectsList.find(
                        (element) =>
                            !previousIds.has(element.id) &&
                            element !== output &&
                            element.elementClass === 1 &&
                            element.coords.usrCoords[0] === 1 &&
                            element.X() === 3 &&
                            element.Y() === -3
                    );
                })()
                : source;
            const helperPoint = output.point || null;
            const ids = {
                a: a.id,
                b: b.id,
                sourceLine: sourceLine.id,
                source: sourcePoint?.id,
                output: output.id,
                helperPoint: helperPoint?.id
            };
            const before = {
                output: snapshot(output),
                source: sourcePoint ? snapshot(sourcePoint) : null,
                objectCount: removalBoard.objectsList.length
            };
            removalBoard.removeObject(output);
            const after = Object.fromEntries(
                Object.entries(ids).map(([name, id]) => [
                    name,
                    id === undefined
                        ? null
                        : Boolean(removalBoard.objects[id])
                ])
            );
            return {
                ids,
                before,
                after,
                objectCount: removalBoard.objectsList.length
            };
        };
        const removal = {};
        for (
            const type of [
                "orthogonalprojection",
                "perpendicularpoint",
                "perpendicular",
                "perpendicularsegment"
            ]
        ) {
            removal[type] = {
                existing: removalCase(type, false),
                coordinates: removalCase(type, true)
            };
        }

        return {
            version: JXG.version,
            initial,
            moved,
            degenerate,
            removal
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
