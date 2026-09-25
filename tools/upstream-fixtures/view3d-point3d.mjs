/*
 * Official JSXGraph 1.13.3 View3D / Point3D / Line3D / Plane3D / Axis3D /
 * Polyhedron3D lifecycle fixture.
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
        const createView = (board, projection, id, attributes = {}) =>
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
                    bank: {slider: {visible: false, start: 0}},
                    ...attributes
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
        const snapshotLine = (line) => ({
            id: line.id,
            name: line.name,
            elType: line.elType,
            type: line.type,
            elementClass: line.elementClass,
            is3D: line.is3D,
            point: line.point.id,
            vector: vector(line.vec),
            range: vector(
                line.range.map(
                    (value) =>
                        typeof value === "function" ? value() : value
                )
            ),
            point1: vector(line.point1.coords),
            point2: vector(line.point2.coords),
            endpoints: line.endpoints.map((point) => ({
                coordinates: vector(point.coords),
                projected: vector(point.element2D.coords.usrCoords)
            })),
            element2D: {
                id: line.element2D.id,
                point1: vector(line.element2D.point1.coords.usrCoords),
                point2: vector(line.element2D.point2.coords.usrCoords),
                straightFirst: line.element2D.evalVisProp("straightfirst"),
                straightLast: line.element2D.evalVisProp("straightlast"),
                parents: [...line.element2D.parents],
                dump: line.element2D.dump,
                viewId: line.element2D.view.id
            },
            transformations: line.transformations.map(
                (transform) => transform.transformationType
            ),
            baseElement: line.baseElement?.id ?? null,
            zIndex: classify(line.zIndex),
            relations: relations(line)
        });
        const snapshotPlane = (plane) => ({
            id: plane.id,
            name: plane.name,
            elType: plane.elType,
            type: plane.type,
            elementClass: plane.elementClass,
            is3D: plane.is3D,
            point: plane.point.id,
            vector1: vector(plane.vec1),
            vector2: vector(plane.vec2),
            rangeU: vector(
                plane.range_u.map(
                    (value) =>
                        typeof value === "function" ? value() : value
                )
            ),
            rangeV: vector(
                plane.range_v.map(
                    (value) =>
                        typeof value === "function" ? value() : value
                )
            ),
            normal: vector(plane.normal),
            d: classify(plane.d),
            at: vector(plane.F(0.5, -0.25)),
            element2D: {
                id: plane.element2D.id,
                dataX: vector(plane.element2D.dataX),
                dataY: vector(plane.element2D.dataY),
                parents: [...plane.element2D.parents],
                dump: plane.element2D.dump,
                viewId: plane.element2D.view.id
            },
            transformations: plane.transformations.map(
                (transform) => transform.transformationType
            ),
            baseElement: plane.baseElement?.id ?? null,
            relations: relations(plane)
        });
        const snapshotMesh = (mesh) => ({
            id: mesh.id,
            elType: mesh.elType,
            type: mesh.type,
            elementClass: mesh.elementClass,
            dataX: vector(mesh.dataX),
            dataY: vector(mesh.dataY),
            stepWidthU: classify(mesh.evalVisProp("stepwidthu")),
            stepWidthV: classify(mesh.evalVisProp("stepwidthv")),
            layer: classify(mesh.evalVisProp("layer")),
            strokeColor: mesh.evalVisProp("strokecolor"),
            strokeOpacity: classify(mesh.evalVisProp("strokeopacity")),
            relations: relations(mesh)
        });
        const snapshotFace = (face) => ({
            id: face.id,
            elType: face.elType,
            type: face.type,
            faceNumber: face.faceNumber,
            vertices: [...face.polyhedron.faces[face.faceNumber]],
            normal: vector(face.normal),
            d: classify(face.d),
            vec1: vector(face.vec1),
            vec2: vector(face.vec2),
            zIndex: classify(face.zIndex),
            element2D: {
                id: face.element2D.id,
                dataX: vector(face.element2D.dataX),
                dataY: vector(face.element2D.dataY),
                fillColor: face.element2D.evalVisProp("fillcolor"),
                fillOpacity: classify(
                    face.element2D.evalVisProp("fillopacity")
                ),
                strokeWidth: classify(
                    face.element2D.evalVisProp("strokewidth")
                ),
                parents: [...face.element2D.parents],
                dump: face.element2D.dump,
                viewId: face.element2D.view.id
            },
            transformations: face.transformations.map(
                (transform) => transform.transformationType
            ),
            baseElement: face.baseElement?.id ?? null,
            relations: relations(face)
        });
        const snapshotPolyhedron = (polyhedron) => ({
            id: polyhedron.id,
            name: polyhedron.name,
            elType: polyhedron.elType,
            type: polyhedron.type,
            numberFaces: polyhedron.numberFaces,
            vertexKeys: Object.keys(polyhedron.def.vertices),
            coordinates: Object.fromEntries(
                Object.entries(polyhedron.def.coords).map(
                    ([key, coordinates]) => [key, vector(coordinates)]
                )
            ),
            faces: polyhedron.faces.map(snapshotFace),
            stl: polyhedron.toSTL("fixture"),
            relations: relations(polyhedron)
        });
        const snapshotTicks = (ticks) => ({
            id: ticks.id,
            name: ticks.name,
            elType: ticks.elType,
            dataX: vector(ticks.dataX),
            dataY: vector(ticks.dataY),
            labels: (ticks.labels ?? []).map((label) => ({
                coordinates3D: vector(label.coords),
                coordinates2D: vector(label.element2D.coords.usrCoords),
                plaintext: label.element2D.plaintext
            })),
            relations: relations(ticks)
        });
        const snapshotDefaultAxes = (view) => {
            const axes = view.defaultAxes;
            if (axes === null) {
                return {
                    exists: false,
                    roles: [],
                    concreteRoles: [],
                    nullRoles: [],
                    directMemberCount: 0,
                    registeredDirectMemberCount: 0,
                    members: []
                };
            }
            const roles = Object.keys(axes);
            const concreteRoles = roles.filter(
                (role) => axes[role] !== null && axes[role] !== undefined
            );
            const members = concreteRoles.map((role) => {
                const member = axes[role];
                return {
                    role,
                    id: member.id,
                    elType: member.elType,
                    type: member.type,
                    is3D: member.is3D,
                    viewId: member.view?.id ?? null,
                    registeredInView: view.objects[member.id] === member,
                    labelCount:
                        member.elType === "curve"
                            ? (member.labels ?? []).length
                            : null
                };
            });
            return {
                exists: true,
                roles,
                concreteRoles,
                nullRoles: roles.filter(
                    (role) => axes[role] === null
                ),
                directMemberCount: members.length,
                registeredDirectMemberCount: members.filter(
                    (member) => member.registeredInView
                ).length,
                members
            };
        };
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
        const automaticNoneAxes = snapshotDefaultAxes(view);
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
        const twoPointLine = view.create(
            "line3d",
            [numeric, functionPoint],
            {
                id: "twoPointLine",
                name: "",
                withLabel: false,
                straightFirst: true,
                straightLast: true
            }
        );
        const directionLine = view.create(
            "line3d",
            [
                mixedPoint,
                [() => numeric.X() - 1, 2, -1],
                [Number.NEGATIVE_INFINITY, 2]
            ],
            {
                id: "directionLine",
                name: "",
                withLabel: false
            }
        );
        const copiedDirectionLine = view.create(
            "line3d",
            [
                homogeneous,
                directionLine,
                [-2, Number.POSITIVE_INFINITY]
            ],
            {
                id: "copiedDirectionLine",
                name: "",
                withLabel: false
            }
        );
        const transformedLine = view.create(
            "line3d",
            [directionLine, translation],
            {
                id: "transformedLine",
                name: "",
                withLabel: false
            }
        );
        const axis = view.create(
            "axis3d",
            [
                [-5, 6, -3],
                [5, 6, -3]
            ],
            {
                id: "axis",
                name: "",
                withLabel: false
            }
        );
        const finitePlane = view.create(
            "plane3d",
            [
                numeric,
                [1, 0, 0],
                [0, 1, 1],
                [-2, 3],
                [-1, 2]
            ],
            {
                id: "finitePlane",
                name: "",
                withLabel: false,
                type: "wireframe"
            }
        );
        const threePointPlane = view.create(
            "plane3d",
            [
                numeric,
                functionPoint,
                mixedPoint,
                [-2, 2],
                [-3, 1]
            ],
            {
                id: "threePointPlane",
                name: "",
                withLabel: false,
                type: "wireframe"
            }
        );
        const transformedPlane = view.create(
            "plane3d",
            [
                finitePlane,
                translation,
                [-2, 3],
                [-1, 2]
            ],
            {
                id: "transformedPlane",
                name: "",
                withLabel: false,
                type: "wireframe"
            }
        );
        const directMesh = view.create(
            "mesh3d",
            [
                [2, 1, 2, 2],
                [0, 1, 0, 0],
                [0, 0, 1, 1],
                [-2, 3],
                [-1, 2]
            ],
            {
                id: "directMesh",
                name: "",
                stepWidthU: 2
            }
        );
        const polyhedron = view.create(
            "polyhedron3d",
            [
                {
                    a: numeric,
                    b: [() => dynamicX, -1, 0],
                    c: () => [dynamicX, 3, 0],
                    d: [1, -2, 3, 1]
                },
                [
                    [
                        ["a", "b", "c"],
                        {
                            fillColor: "green",
                            fillOpacity: 0.5,
                            strokeWidth: 4
                        }
                    ],
                    ["a", "c"],
                    ["d"]
                ]
            ],
            {
                name: "",
                fillColorArray: ["red", "blue"],
                fillOpacity: 0.6,
                shader: {enabled: false}
            }
        );
        const transformedPolyhedron = view.create(
            "polyhedron3d",
            [polyhedron, translation],
            {
                name: "",
                fillColorArray: ["yellow"]
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
            lines: [
                twoPointLine,
                directionLine,
                copiedDirectionLine,
                transformedLine,
                axis
            ].map(snapshotLine),
            planes: [
                finitePlane,
                threePointPlane,
                transformedPlane
            ].map(snapshotPlane),
            meshes: [
                finitePlane.mesh3d,
                threePointPlane.mesh3d,
                transformedPlane.mesh3d,
                directMesh
            ].map(snapshotMesh),
            polyhedra: [
                polyhedron,
                transformedPolyhedron
            ].map(snapshotPolyhedron),
            planeProjection: {
                coordinates: vector(
                    finitePlane.projectCoords(
                        [1, 1, 2, 3],
                        [0, 0]
                    )
                ),
                parameters: (() => {
                    const parameters = [0, 0];
                    finitePlane.projectCoords(
                        [1, 1, 2, 3],
                        parameters
                    );
                    return vector(parameters);
                })()
            },
            lineProjection: {
                coordinates: vector(
                    directionLine.projectCoords(
                        [1, 3, 2],
                        [0]
                    )
                ),
                parameter: (() => {
                    const parameters = [0];
                    directionLine.projectCoords([1, 3, 2], parameters);
                    return classify(parameters[0]);
                })(),
                screen: vector(
                    directionLine.projectScreenCoords([0.25, -0.5])
                )
            },
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
        const updatedLines = [
            twoPointLine,
            directionLine,
            copiedDirectionLine,
            transformedLine,
            axis
        ].map(snapshotLine);
        const updatedPlanes = [
            finitePlane,
            threePointPlane,
            transformedPlane
        ].map(snapshotPlane);
        const updatedMeshes = [
            finitePlane.mesh3d,
            threePointPlane.mesh3d,
            transformedPlane.mesh3d,
            directMesh
        ].map(snapshotMesh);
        const updatedPolyhedra = [
            polyhedron,
            transformedPolyhedron
        ].map(snapshotPolyhedron);

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
        const axesBoard = createBoard();
        const axesView = createView(axesBoard, "parallel", "axesView");
        const axes = axesView.create(
            "axes3d",
            [],
            {
                axesPosition: "border",
                xAxisBorder: {
                    name: "",
                    withLabel: false,
                    ticks3d: {visible: false}
                },
                yAxisBorder: {
                    name: "",
                    withLabel: false,
                    ticks3d: {visible: false}
                },
                zAxisBorder: {
                    name: "",
                    withLabel: false,
                    ticks3d: {visible: false}
                }
            }
        );
        axesBoard.update();
        const axes3d = {
            roles: Object.keys(axes),
            xAxisBorder: snapshotLine(axes.xAxisBorder),
            xPlaneRear: snapshotPlane(axes.xPlaneRear),
            xPlaneRearYAxis: snapshotLine(axes.xPlaneRearYAxis),
            ticks: [
                axes.xAxisBorderTicks?.elType ?? null,
                axes.yAxisBorderTicks?.elType ?? null,
                axes.zAxisBorderTicks?.elType ?? null
            ]
        };
        const automaticAxesBoard = createBoard();
        const automaticBorderView = createView(
            automaticAxesBoard,
            "parallel",
            "automaticBorderView",
            {axesPosition: "border"}
        );
        automaticAxesBoard.update();
        const automaticDefaultAxes = {
            none: automaticNoneAxes,
            border: snapshotDefaultAxes(automaticBorderView)
        };
        const ticksBoard = createBoard();
        const ticksView = createView(
            ticksBoard,
            "parallel",
            "ticksView"
        );
        let ticksPoint = [1, -2, 1, -1];
        let ticksDirectionX = 2;
        const ticks = ticksView.create(
            "ticks3d",
            [
                () => ticksPoint,
                [() => ticksDirectionX, 0, 0],
                4,
                [0, 1, 0]
            ],
            {
                id: "ticks",
                name: "",
                ticksDistance: 2,
                majorHeight: 12,
                tickEndings: [0.25, 0.75],
                drawLabels: true,
                label: {
                    anchorX: "middle",
                    anchorY: "middle"
                }
            }
        );
        ticksBoard.update();
        const ticks3d = {
            initial: snapshotTicks(ticks)
        };
        ticksPoint = [1, -1, 2, 0.5];
        ticksDirectionX = 4;
        ticksBoard.update();
        ticks3d.updated = snapshotTicks(ticks);

        return {
            version: JXG.version,
            initial,
            updated,
            updatedLines,
            updatedPlanes,
            updatedMeshes,
            updatedPolyhedra,
            afterApplyOnce,
            homogeneousSetPosition,
            afterRemoval,
            central,
            axes3d,
            automaticDefaultAxes,
            ticks3d,
            malformed: {
                noCoordinates: captureError(
                    () => view.create("point3d", [], {name: ""})
                ),
                twoCoordinates: captureError(
                    () => view.create("point3d", [1, 2], {name: ""})
                ),
                invalidLineDirection: captureError(
                    () => view.create(
                        "line3d",
                        [numeric, [1, 2], [-1, 1]],
                        {name: ""}
                    )
                )
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
