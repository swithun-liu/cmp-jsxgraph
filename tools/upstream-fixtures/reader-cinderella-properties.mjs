/*
 * Official JSXGraph 1.13.3 Cinderella reader fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-cinderella-properties.mjs
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
    await page.setContent(
        '<main>' +
            '<div id="board" style="width:500px;height:500px"></div>' +
            '<div id="advanced" style="width:500px;height:500px"></div>' +
            '<div id="remaining" style="width:500px;height:500px"></div>' +
            '<div id="compass" style="width:500px;height:500px"></div>' +
        '</main>'
    );
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "jsxgraph-debug-ui/src/androidMain/assets/" +
                "jsxgraphcore-1.13.3.js"
        )
    });
    await page.addScriptTag({
        path: resolve(
            repositoryRoot,
            "third_party/jsxgraph-src/src/reader/cinderella.js"
        )
    });

    const evidence = await page.evaluate(() => {
        const reader = Object.create(JXG.CinderellaReader.prototype);
        const pointWithBorder = [
            "(\"A\"):=FreePoint([]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"color\",\"2\");",
            "\"A\".setAttribute(\"pointborder\",\"true\");"
        ];
        const pointWithoutBorder = [
            "(\"B\"):=FreePoint([]);",
            "\"B\".setAppearance(5,12,1,0,0,9,true,false);",
            "\"B\".noPBorder();",
            "\"B\".setAttribute(\"pointborder\",\"false\");"
        ];
        const circleVisibility = [
            "(\"C0\"):=CircleMP(\"A\",\"B\");",
            "\"C0\".setAppearance(14,5,2,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"5\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"4\");"
        ];
        const circleAlpha = [
            "(\"C1\"):=CircleMP(\"A\",\"B\");",
            "\"C1\".setAppearance(11,5,4,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"4\");",
            "\"C1\".setAttribute(\"fillalpha\",\"0.25\");"
        ];
        const dashedLine = [
            "(\"a\"):=Join(\"A\",\"B\");",
            "\"a\".setAppearance(9,5,5,0,0,9,true,false);",
            "\"a\".setAttribute(\"linedashing\",\"true\");"
        ];
        const solidLine = [
            "(\"b\"):=Join(\"A\",\"B\");",
            "\"b\".setAppearance(11,5,2,0,0,9,true,false);",
            "\"b\".setAttribute(\"linedashing\",\"false\");"
        ];

        const properties = {
            version: JXG.version,
            colors: ["0", "2tail", "6", "14", "21", "22", "bad"].map(
                (value) => reader.calculateColor(value)
            ),
            pointWithBorder: reader.readPointProperties(pointWithBorder, 0),
            pointWithoutBorder: reader.readPointProperties(
                pointWithoutBorder,
                0
            ),
            circleVisibility: reader.readCircleProperties(
                circleVisibility,
                0
            ),
            circleAlpha: reader.readCircleProperties(circleAlpha, 0),
            dashedLine: reader.readLineProperties(dashedLine, 0),
            solidLine: reader.readLineProperties(solidLine, 0)
        };
        const source = [
            "<cindyscript>",
            "(\"A\"):=FreePoint([2+i*0,-6+i*0,2+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"color\",\"2\");",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([4+i*0,-2+i*0,2+i*0]);",
            "\"B\".setAppearance(3,4,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"color\",\"3\");",
            "\"B\".setAttribute(\"pointborder\",\"false\");",
            "(\"D\"):=FreePoint([3+i*0,-3+i*0,1+i*0]);",
            "\"D\".setAppearance(4,5,1,0,0,9,true,false);",
            "\"D\".setAttribute(\"pointborder\",\"true\");",
            "(\"l\"):=Join(\"A\",\"B\");",
            "\"l\".setAppearance(9,5,5,0,0,9,true,false);",
            "\"l\".setAttribute(\"linedashing\",\"true\");",
            "(\"s\"):=Segment(\"A\",\"B\");",
            "\"s\".setAppearance(11,5,2,0,0,9,true,false);",
            "\"s\".setAttribute(\"linedashing\",\"false\");",
            "(\"C0\"):=CircleMP(\"A\",\"B\");",
            "\"C0\".setAppearance(14,5,2,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"5\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"4\");",
            "(\"C1\"):=CircleByFixedRadius(\"A\",9+i*0);",
            "\"C1\".setAppearance(11,5,4,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"4\");",
            "\"C1\".setAttribute(\"fillalpha\",\"0.25\");",
            "{\"G\",null,[1+i*0,0+i*0,1+i*0]}:=" +
                "PointOnCircle(\"C0\",[1+i*0,2+i*0,0+i*0]);",
            "\"G\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"G\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"H\",[1+i*0,0+i*0,1+i*0]}:=" +
                "PointOnCircle(\"C0\",[1+i*0,2+i*0,0+i*0]);",
            "\"H\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"H\".setAttribute(\"pointborder\",\"true\");",
            "(\"I\"):=PointOnLine(\"l\",[2+i*0,-6+i*0,2+i*0]);",
            "\"I\".setAppearance(4,5,1,0,0,9,true,false);",
            "\"I\".setAttribute(\"pointborder\",\"true\");",
            "(\"J\"):=Mid(\"A\",\"B\");",
            "\"J\".setAppearance(5,5,1,0,0,9,true,false);",
            "\"J\".setAttribute(\"pointborder\",\"true\");",
            "(\"C2\"):=CircleBy3(\"A\",\"B\",\"D\");",
            "\"C2\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C2\".setAttribute(\"colorfill\",\"7\");",
            "\"C2\".setAttribute(\"visibilityfill\",\"5\");",
            "setOriginX(12.5);",
            "setOriginY(20);",
            "setScale(50);",
            "</cindyscript>"
        ].join("\n");
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        const drawingReader = new JXG.CinderellaReader(board, source);
        drawingReader.read();
        const point = (reference) => {
            const value =
                typeof reference === "string"
                    ? board.select(reference)
                    : reference;
            return {
                id: value.id,
                name: value.name,
                coords: [value.X(), value.Y()],
                fillColor: value.visProp.fillcolor,
                strokeColor: value.visProp.strokecolor
            };
        };
        const line = (name) => {
            const value = board.select(name);
            return {
                id: value.id,
                name: value.name,
                straightFirst: value.visProp.straightfirst,
                straightLast: value.visProp.straightlast,
                strokeColor: value.visProp.strokecolor,
                strokeWidth: value.visProp.strokewidth,
                dash: value.visProp.dash
            };
        };
        const circle = (name) => {
            const value = board.select(name);
            return {
                id: value.id,
                name: value.name,
                radius: value.Radius(),
                strokeColor: value.visProp.strokecolor,
                fillColor: value.visProp.fillcolor,
                fillOpacity: value.visProp.fillopacity,
                strokeWidth: value.visProp.strokewidth
            };
        };
        const drawn = {
            A: point("A"),
            B: point("B"),
            D: point("D"),
            l: line("l"),
            s: line("s"),
            C0: circle("C0"),
            C1: circle("C1"),
            G: point("G"),
            H: point("H"),
            I: point("I"),
            J: point("J"),
            C2: {
                ...circle("C2"),
                center: point(board.select("C2").center)
            },
            zoomX: board.zoomX,
            zoomY: board.zoomY,
            origin: board.origin.scrCoords.slice()
        };
        const advancedSource = [
            "<cindyscript>",
            "(\"A\"):=FreePoint([-2+i*0,0+i*0,1+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"B\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"C\"):=FreePoint([0+i*0,-3+i*0,1+i*0]);",
            "\"C\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"C\".setAttribute(\"pointborder\",\"true\");",
            "(\"D\"):=FreePoint([-3+i*0,-2+i*0,1+i*0]);",
            "\"D\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"D\".setAttribute(\"pointborder\",\"true\");",
            "(\"E\"):=FreePoint([3+i*0,-2+i*0,1+i*0]);",
            "\"E\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"E\".setAttribute(\"pointborder\",\"true\");",
            "(\"a\"):=Join(\"A\",\"B\");",
            "\"a\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"a\".setAttribute(\"linedashing\",\"false\");",
            "(\"b\"):=Parallel(\"a\",\"C\");",
            "\"b\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"b\".setAttribute(\"linedashing\",\"false\");",
            "(\"d\"):=Orthogonal(\"b\",\"E\");",
            "\"d\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"d\".setAttribute(\"linedashing\",\"false\");",
            "(\"Q\"):=ConicBy5(\"A\",\"B\",\"C\",\"D\",\"E\");",
            "\"Q\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Q\".setAttribute(\"colorfill\",\"7\");",
            "\"Q\".setAttribute(\"visibilityfill\",\"5\");",
            "{null,\"El\",[1+i*0,0+i*0,0+i*0]}:=" +
                "ConicFoci(\"A\",\"B\",\"C\");",
            "\"El\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"El\".setAttribute(\"colorfill\",\"7\");",
            "\"El\".setAttribute(\"visibilityfill\",\"5\");",
            "{\"Hy\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "ConicFociH(\"A\",\"B\",\"E\");",
            "\"Hy\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Hy\".setAttribute(\"colorfill\",\"7\");",
            "\"Hy\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Pa\"):=ConicParabolaPL(\"C\",\"a\");",
            "\"Pa\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Pa\".setAttribute(\"colorfill\",\"7\");",
            "\"Pa\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Poly0\"):=Poly(\"A\",\"B\",\"E\",\"D\");",
            "\"Poly0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Poly0\".setAttribute(\"colorfill\",\"7\");",
            "\"Poly0\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"Ar\"):=Arc(\"A\",\"C\",\"B\");",
            "\"Ar\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"Ar\".setAttribute(\"colorfill\",\"7\");",
            "\"Ar\".setAttribute(\"visibilityfill\",\"5\");",
            "(\"t\"):=Through(\"D\",[1+i*0,-2+i*0,0+i*0]);",
            "\"t\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"t\".setAttribute(\"linedashing\",\"false\");",
            "setScale(50);",
            "</cindyscript>"
        ].join("\n");
        const advancedBoard = JXG.JSXGraph.initBoard("advanced", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        new JXG.CinderellaReader(advancedBoard, advancedSource).read();
        const advancedLine = (name) => {
            const value = advancedBoard.select(name);
            return {
                stdform: value.stdform.slice(0, 3),
                point1: [value.point1.X(), value.point1.Y()],
                point2: [value.point2.X(), value.point2.Y()]
            };
        };
        const advancedCurve = (name) => {
            const value = advancedBoard.select(name);
            return {
                center: [value.center.X(), value.center.Y()],
                samples: [0, Math.PI / 2, Math.PI].map(
                    (parameter) => [
                        value.X(parameter),
                        value.Y(parameter)
                    ]
                )
            };
        };
        const advanced = {
            parallel: advancedLine("b"),
            orthogonal: advancedLine("d"),
            conic: advancedCurve("Q"),
            ellipse: advancedCurve("El"),
            hyperbola: advancedCurve("Hy"),
            parabola: advancedCurve("Pa"),
            polygon: {
                area: advancedBoard.select("Poly0").Area(),
                perimeter: advancedBoard.select("Poly0").Perimeter(),
                vertices: advancedBoard.select("Poly0").vertices.map(
                    (vertex) => [vertex.X(), vertex.Y()]
                )
            },
            arc: {
                center: [
                    advancedBoard.select("Ar").center.X(),
                    advancedBoard.select("Ar").center.Y()
                ],
                radius: advancedBoard.select("Ar").Radius(),
                radians: advancedBoard.select("Ar").Value("radians")
            },
            through: advancedLine("t")
        };
        const remainingSource = [
            "<cindyscript>",
            "(\"O\"):=FreePoint([0+i*0,0+i*0,1+i*0]);",
            "\"O\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"O\".setAttribute(\"pointborder\",\"true\");",
            "(\"AX\"):=FreePoint([-2+i*0,0+i*0,1+i*0]);",
            "\"AX\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"AX\".setAttribute(\"pointborder\",\"true\");",
            "(\"BX\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"BX\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"BX\".setAttribute(\"pointborder\",\"true\");",
            "(\"AY\"):=FreePoint([0+i*0,-2+i*0,1+i*0]);",
            "\"AY\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"AY\".setAttribute(\"pointborder\",\"true\");",
            "(\"BY\"):=FreePoint([0+i*0,2+i*0,1+i*0]);",
            "\"BY\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"BY\".setAttribute(\"pointborder\",\"true\");",
            "(\"P\"):=FreePoint([-3+i*0,-1+i*0,1+i*0]);",
            "\"P\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"P\".setAttribute(\"pointborder\",\"true\");",
            "(\"Q\"):=FreePoint([3+i*0,-1+i*0,1+i*0]);",
            "\"Q\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"Q\".setAttribute(\"pointborder\",\"true\");",
            "(\"LX\"):=Join(\"AX\",\"BX\");",
            "\"LX\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LX\".setAttribute(\"linedashing\",\"false\");",
            "(\"LY\"):=Join(\"AY\",\"BY\");",
            "\"LY\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LY\".setAttribute(\"linedashing\",\"false\");",
            "(\"LC\"):=Join(\"P\",\"Q\");",
            "\"LC\".setAppearance(9,5,2,0,0,9,true,false);",
            "\"LC\".setAttribute(\"linedashing\",\"false\");",
            "{\"BFirst\",\"BSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BSecond\".setAppearance(8,5,1,0,0,9,true,false);",
            "\"BSecond\".setAttribute(\"linedashing\",\"false\");",
            "\"BFirst\".setAppearance(9,5,3,0,0,9,true,false);",
            "\"BFirst\".setAttribute(\"linedashing\",\"true\");",
            "{\"BOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BOnlyFirst\".setAppearance(9,5,3,0,0,9,true,false);",
            "\"BOnlyFirst\".setAttribute(\"linedashing\",\"true\");",
            "{null,\"BOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "AngularBisector(\"LX\",\"LY\",\"O\");",
            "\"BOnlySecond\".setAppearance(8,5,1,0,0,9,true,false);",
            "\"BOnlySecond\".setAttribute(\"linedashing\",\"false\");",
            "(\"M\"):=Meet(\"LX\",\"LY\");",
            "\"M\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"M\".setAttribute(\"pointborder\",\"true\");",
            "(\"C0\"):=CircleMP(\"O\",\"BX\");",
            "\"C0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"7\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"0\");",
            "(\"C1\"):=CircleMP(\"BX\",\"O\");",
            "\"C1\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C1\".setAttribute(\"colorfill\",\"7\");",
            "\"C1\".setAttribute(\"visibilityfill\",\"0\");",
            "{\"CLFirst\",\"CLSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLSecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLSecond\".setAttribute(\"pointborder\",\"true\");",
            "\"CLFirst\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"CLFirst\".setAttribute(\"pointborder\",\"true\");",
            "{\"CLOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLOnlyFirst\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLOnlyFirst\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"CLOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionConicLine(\"C0\",\"LC\");",
            "\"CLOnlySecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CLOnlySecond\".setAttribute(\"pointborder\",\"true\");",
            "{\"CCFirst\",\"CCSecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCSecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCSecond\".setAttribute(\"pointborder\",\"true\");",
            "\"CCFirst\".setAppearance(3,5,1,0,0,9,true,false);",
            "\"CCFirst\".setAttribute(\"pointborder\",\"true\");",
            "{\"CCOnlyFirst\",null,[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCOnlyFirst\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCOnlyFirst\".setAttribute(\"pointborder\",\"true\");",
            "{null,\"CCOnlySecond\",[1+i*0,0+i*0,0+i*0]}:=" +
                "IntersectionCircleCircle(\"C0\",\"C1\");",
            "\"CCOnlySecond\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"CCOnlySecond\".setAttribute(\"pointborder\",\"true\");",
            "setScale(50);",
            "</cindyscript>"
        ].join("\n");
        const remainingBoard = JXG.JSXGraph.initBoard("remaining", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        new JXG.CinderellaReader(
            remainingBoard,
            remainingSource
        ).read();
        const remainingLine = (name) => {
            const value = remainingBoard.select(name);
            return {
                name: value.name,
                stdform: value.stdform.slice(0, 3)
            };
        };
        const remainingPoint = (name) => {
            const value = remainingBoard.select(name);
            return {
                name: value.name,
                coords: [value.X(), value.Y()],
                elType: value.elType
            };
        };
        const remaining = {
            bisectors: [
                "BFirst",
                "BSecond",
                "BOnlyFirst",
                "BOnlySecond"
            ].map(remainingLine),
            meet: remainingPoint("M"),
            circleLine: [
                "CLFirst",
                "CLSecond",
                "CLOnlyFirst",
                "CLOnlySecond"
            ].map(remainingPoint),
            circleCircle: [
                "CCFirst",
                "CCSecond",
                "CCOnlyFirst",
                "CCOnlySecond"
            ].map(remainingPoint)
        };
        const compassSource = [
            "<cindyscript>",
            "(\"A\"):=FreePoint([-2+i*0,0+i*0,1+i*0]);",
            "\"A\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"A\".setAttribute(\"pointborder\",\"true\");",
            "(\"B\"):=FreePoint([2+i*0,0+i*0,1+i*0]);",
            "\"B\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"B\".setAttribute(\"pointborder\",\"true\");",
            "(\"C\"):=FreePoint([0+i*0,-3+i*0,1+i*0]);",
            "\"C\".setAppearance(2,5,1,0,0,9,true,false);",
            "\"C\".setAttribute(\"pointborder\",\"true\");",
            "(\"C0\"):=Compass(\"A\",\"B\",\"C\");",
            "\"C0\".setAppearance(11,5,3,0,0,9,false,false);",
            "\"C0\".setAttribute(\"colorfill\",\"7\");",
            "\"C0\".setAttribute(\"visibilityfill\",\"5\");",
            "setScale(50);",
            "</cindyscript>"
        ].join("\n");
        const compassBoard = JXG.JSXGraph.initBoard("compass", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        let compass;
        try {
            new JXG.CinderellaReader(compassBoard, compassSource).read();
            compass = {errorName: null, errorMessage: null};
        } catch (error) {
            compass = {
                errorName: error.name,
                errorMessage: error.message
            };
        }
        JXG.JSXGraph.freeBoard(board);
        JXG.JSXGraph.freeBoard(advancedBoard);
        JXG.JSXGraph.freeBoard(remainingBoard);
        JXG.JSXGraph.freeBoard(compassBoard);
        return {properties, drawn, advanced, remaining, compass};
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
