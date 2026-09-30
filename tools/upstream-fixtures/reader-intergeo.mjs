/*
 * Official JSXGraph 1.13.3 Intergeo parse and Board creation fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-intergeo.mjs
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
        '<main><div id="board" style="width:800px;height:600px"></div></main>'
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
            "third_party/jsxgraph-src/src/reader/intergeo.js"
        )
    });

    const evidence = await page.evaluate(() => {
        const source = [
            "<construction>",
            "  <elements>",
            "    <point id=\"H\"><homogeneous_coordinates>",
            "      <double>2junk</double><double>3</double><double>4</double>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"CR\"><homogeneous_coordinates>",
            "      <complex><double>2</double><double>0</double></complex>",
            "      <complex><double>3</double><double>0</double></complex>",
            "      <complex><double>4</double><double>0</double></complex>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"E\"><euclidian_coordinates>",
            "      <double>5</double><double>6</double>",
            "    </euclidian_coordinates></point>",
            "    <point id=\"P\"><polar_coordinates>",
            "      <double>2</double><double>1.5707963267948966</double>",
            "    </polar_coordinates></point>",
            "    <point id=\"BAD\"><homogeneous_coordinates>",
            "      <double>1</double><double>2</double>",
            "    </homogeneous_coordinates></point>",
            "    <point id=\"U\"><cartesian_coordinates>",
            "      <double>7</double><double>8</double>",
            "    </cartesian_coordinates></point>",
            "    <line id=\"L\"><homogeneous_coordinates>",
            "      <double>1</double><double>2</double><double>3</double>",
            "    </homogeneous_coordinates></line>",
            "    <line_segment id=\"S\"><homogeneous_coordinates>",
            "      <double>4</double><double>5</double><double>6</double>",
            "    </homogeneous_coordinates><homogeneous_coordinates>",
            "      <double>7</double><double>8</double><double>9</double>",
            "    </homogeneous_coordinates></line_segment>",
            "    <circle id=\"C\"><matrix>",
            "      <double>1</double><double>0</double><double>-1</double>",
            "      <double>0</double><double>1</double><double>-3</double>",
            "      <double>-1</double><double>-3</double><double>6</double>",
            "    </matrix></circle>",
            "    <conic id=\"Q\"><matrix>",
            "      <double>1</double><double>2</double><double>3</double>",
            "    </matrix></conic>",
            "    <polygon id=\"PG\"/>",
            "  </elements>",
            "  <constraints>",
            "    <line_through_two_points>",
            "      <line out=\"true\">L</line><point>H</point><point>E</point>",
            "    </line_through_two_points>",
            "    <free_point><point out=\"true\">H</point></free_point>",
            "    <translate><point out=\"true\">T</point><point>H</point>",
            "    </translate>",
            "  </constraints>",
            "</construction>"
        ].join("\n");
        const reader = new JXG.IntergeoReader({}, source);
        reader.objects = {};
        const diagnostics = [];
        const originalDebug = JXG.debug;
        JXG.debug = (...values) => {
            diagnostics.push(values.join(" "));
        };

        try {
            const elements = reader.tree.getElementsByTagName("elements");
            reader.readElements(elements);
            const constraintsElement =
                reader.tree.getElementsByTagName("constraints")[0];
            const constraints = Array.from(
                constraintsElement.childNodes
            ).filter((node) => node.nodeType === 1);

            for (const node of constraints) {
                if (
                    node.nodeName === "free_point" ||
                    node.nodeName === "translate"
                ) {
                    reader.readConstraint([{childNodes: [node]}], 0);
                }
            }

            const parseOnly = {
                version: JXG.version,
                objects: Object.values(reader.objects).map((object) => ({
                    id: object.id,
                    coords: object.coords,
                    exists: object.exists,
                    i2geoType: object.i2geoType
                })),
                constraints: constraints.map((node) => ({
                    name: node.nodeName,
                    parameters: reader.readParams(node)
                })),
                diagnostics
            };
            const drawSource = [
                "<construction>",
                "  <elements>",
                "    <point id=\"A\"><euclidean_coordinates>",
                "      <double>0</double><double>0</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"B\"><euclidean_coordinates>",
                "      <double>2</double><double>0</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"C\"><euclidean_coordinates>",
                "      <double>0</double><double>2</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"U\"><euclidean_coordinates>",
                "      <double>1</double><double>0</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"M\"><euclidean_coordinates>",
                "      <double>0</double><double>0</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"N\"><euclidean_coordinates>",
                "      <double>0</double><double>0</double>",
                "    </euclidean_coordinates></point>",
                "    <point id=\"G\"><homogeneous_coordinates>",
                "      <double>2</double><double>0</double><double>1</double>",
                "    </homogeneous_coordinates></point>",
                "    <line id=\"F\"><homogeneous_coordinates>",
                "      <double>1</double><double>0</double><double>-1</double>",
                "    </homogeneous_coordinates></line>",
                "    <vector id=\"V\"><homogeneous_coordinates>",
                "      <double>0</double><double>1</double><double>0</double>",
                "    </homogeneous_coordinates></vector>",
                "    <circle id=\"K\"><matrix>",
                "      <double>1</double><double>0</double><double>0</double>",
                "      <double>0</double><double>1</double><double>0</double>",
                "      <double>0</double><double>0</double><double>-4</double>",
                "    </matrix></circle>",
                "    <conic id=\"Q\"><matrix>",
                "      <double>1</double><double>0</double><double>0</double>",
                "      <double>0</double><double>1</double><double>0</double>",
                "      <double>0</double><double>0</double><double>-1</double>",
                "    </matrix></conic>",
                "  </elements>",
                "  <constraints>",
                "    <line_through_two_points>",
                "      <line>L</line><point>A</point><point>B</point>",
                "    </line_through_two_points>",
                "    <line_segment_by_points>",
                "      <line_segment>S</line_segment>",
                "      <point>B</point><point>C</point>",
                "    </line_segment_by_points>",
                "    <free_line><line>F</line></free_line>",
                "    <midpoint_of_two_points>",
                "      <point>M</point><point>A</point><point>B</point>",
                "    </midpoint_of_two_points>",
                "    <midpoint_of_line_segment>",
                "      <point>N</point><line_segment>S</line_segment>",
                "    </midpoint_of_line_segment>",
                "    <point_intersection_of_two_lines>",
                "      <point>X</point><line>L</line><line>F</line>",
                "    </point_intersection_of_two_lines>",
                "    <circle_by_center_and_point>",
                "      <circle>K</circle><point>A</point><point>C</point>",
                "    </circle_by_center_and_point>",
                "    <point_on_line><point>U</point><line>L</line>",
                "    </point_on_line>",
                "    <point_on_circle><point>G</point><circle>K</circle>",
                "    </point_on_circle>",
                "    <vector_from_point_to_point>",
                "      <vector>V</vector><point>A</point><point>C</point>",
                "    </vector_from_point_to_point>",
                "  </constraints>",
                "  <display/>",
                "</construction>"
            ].join("\n");
            const board = JXG.JSXGraph.initBoard("board", {
                boundingbox: [-5, 5, 5, -5],
                axis: false,
                showNavigation: false,
                showCopyright: false
            });
            const drawingReader = new JXG.IntergeoReader(
                board,
                drawSource
            );
            drawingReader.objects = {};
            drawingReader.read();
            const element = (name) => {
                const value = board.select(name);
                return {
                    id: value.id,
                    name: value.name,
                    elType: value.elType,
                    coords:
                        typeof value.X === "function"
                            ? [value.X(), value.Y()]
                            : null,
                    stdform: value.stdform?.slice(0, 3) ?? null,
                    straightFirst:
                        value.visProp?.straightfirst ?? null,
                    straightLast:
                        value.visProp?.straightlast ?? null
                };
            };
            const drawn = {
                origin: board.origin.scrCoords.slice(),
                unitX: board.unitX,
                unitY: board.unitY,
                A: element("A"),
                B: element("B"),
                C: element("C"),
                L: element("L"),
                S: element("S"),
                F: element("F"),
                M: element("M"),
                N: element("N"),
                X: element("X"),
                K: element("K"),
                Q: element("Q"),
                U: element("U"),
                G: element("G"),
                V: element("V")
            };
            JXG.JSXGraph.freeBoard(board);

            let isolatedBoardIndex = 0;
            const drawIsolatedConstraint = (constraint, names) => {
                isolatedBoardIndex += 1;
                const host = document.createElement("div");
                host.id = `isolated-board-${isolatedBoardIndex}`;
                host.style.width = "800px";
                host.style.height = "600px";
                document.body.appendChild(host);
                const isolatedBoard = JXG.JSXGraph.initBoard(host.id, {
                    boundingbox: [-5, 5, 5, -5],
                    axis: false,
                    showNavigation: false,
                    showCopyright: false
                });
                const isolatedSource = [
                    "<construction>",
                    "  <elements>",
                    "    <point id=\"A\"><euclidean_coordinates>",
                    "      <double>0</double><double>0</double>",
                    "    </euclidean_coordinates></point>",
                    "    <point id=\"B\"><euclidean_coordinates>",
                    "      <double>2</double><double>0</double>",
                    "    </euclidean_coordinates></point>",
                    "    <point id=\"C\"><euclidean_coordinates>",
                    "      <double>0</double><double>2</double>",
                    "    </euclidean_coordinates></point>",
                    constraint.includes("circle_by_three_points")
                        ? [
                              "    <circle id=\"R\"><matrix>",
                              "      <double>1</double><double>0</double><double>-1</double>",
                              "      <double>0</double><double>1</double><double>-1</double>",
                              "      <double>-1</double><double>-1</double><double>1</double>",
                              "    </matrix></circle>"
                          ].join("\n")
                        : "",
                    "  </elements>",
                    "  <constraints>",
                    constraint,
                    "  </constraints>",
                    "  <display/>",
                    "</construction>"
                ].join("\n");
                const isolatedReader = new JXG.IntergeoReader(
                    isolatedBoard,
                    isolatedSource
                );
                isolatedReader.objects = {};
                isolatedReader.read();
                const result = Object.fromEntries(
                    names.map((name) => {
                        const value = isolatedBoard.select(name);
                        return [
                            name,
                            {
                                id: value.id,
                                name: value.name,
                                elType: value.elType,
                                coords:
                                    typeof value.X === "function"
                                        ? [value.X(), value.Y()]
                                        : null,
                                center: value.center
                                    ? {
                                          id: value.center.id,
                                          name: value.center.name,
                                          coords: [
                                              value.center.X(),
                                              value.center.Y()
                                          ]
                                      }
                                    : null,
                                straightFirst:
                                    value.visProp?.straightfirst ?? null,
                                straightLast:
                                    value.visProp?.straightlast ?? null
                            }
                        ];
                    })
                );
                JXG.JSXGraph.freeBoard(isolatedBoard);
                host.remove();
                return result;
            };
            const circleByThreePoints = drawIsolatedConstraint(
                [
                    "    <circle_by_three_points>",
                    "      <circle>R</circle><point>A</point>",
                    "      <point>B</point><point>C</point>",
                    "    </circle_by_three_points>"
                ].join("\n"),
                ["R"]
            );
            const angleBisector = drawIsolatedConstraint(
                [
                    "    <angular_bisector_of_three_points>",
                    "      <line>D</line><point>A</point>",
                    "      <point>B</point><point>C</point>",
                    "    </angular_bisector_of_three_points>"
                ].join("\n"),
                ["D"]
            );
            const advancedHost = document.createElement("div");
            advancedHost.id = "advanced-board";
            advancedHost.style.width = "800px";
            advancedHost.style.height = "600px";
            document.body.appendChild(advancedHost);
            const advancedBoard = JXG.JSXGraph.initBoard(
                advancedHost.id,
                {
                    boundingbox: [-5, 5, 5, -5],
                    axis: false,
                    showNavigation: false,
                    showCopyright: false
                }
            );
            const advancedSource = [
                "<construction>",
                "  <elements>",
                "    <point id=\"A\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"B\"><euclidean_coordinates><double>2</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"C\"><euclidean_coordinates><double>0</double><double>2</double></euclidean_coordinates></point>",
                "    <point id=\"D\"><euclidean_coordinates><double>2</double><double>2</double></euclidean_coordinates></point>",
                "    <point id=\"CC1\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"CC2\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"CL1\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"CL2\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <point id=\"O\"><euclidean_coordinates><double>0</double><double>0</double></euclidean_coordinates></point>",
                "    <line id=\"F\"><homogeneous_coordinates><double>1</double><double>0</double><double>-1</double></homogeneous_coordinates></line>",
                "    <line id=\"H\"><homogeneous_coordinates><double>0</double><double>1</double><double>0</double></homogeneous_coordinates></line>",
                "    <line id=\"B1\"><homogeneous_coordinates><double>1</double><double>1</double><double>0</double></homogeneous_coordinates></line>",
                "    <line id=\"B2\"><homogeneous_coordinates><double>1</double><double>-1</double><double>0</double></homogeneous_coordinates></line>",
                "    <circle id=\"K\"><matrix><double>1</double><double>0</double><double>0</double><double>0</double><double>1</double><double>0</double><double>0</double><double>0</double><double>-4</double></matrix></circle>",
                "    <circle id=\"R\"><matrix><double>1</double><double>0</double><double>-2</double><double>0</double><double>1</double><double>0</double><double>-2</double><double>0</double><double>0</double></matrix></circle>",
                "  </elements>",
                "  <constraints>",
                "    <circle_by_center_and_point><circle>K</circle><point>A</point><point>C</point></circle_by_center_and_point>",
                "    <circle_by_center_and_point><circle>R</circle><point>B</point><point>D</point></circle_by_center_and_point>",
                "    <free_line><line>F</line></free_line>",
                "    <free_line><line>H</line></free_line>",
                "    <intersection_points_of_two_circles><point>CC1</point><point>CC2</point><circle>K</circle><circle>R</circle></intersection_points_of_two_circles>",
                "    <intersection_points_of_circle_and_line><point>CL1</point><point>CL2</point><circle>K</circle><line>F</line></intersection_points_of_circle_and_line>",
                "    <other_intersection_point_of_circle_and_line><point>O</point><point>CL1</point><circle>K</circle><line>F</line></other_intersection_point_of_circle_and_line>",
                "    <angular_bisectors_of_two_lines><line>B1</line><line>B2</line><line>F</line><line>H</line></angular_bisectors_of_two_lines>",
                "  </constraints>",
                "  <display/>",
                "</construction>"
            ].join("\n");
            const advancedReader = new JXG.IntergeoReader(
                advancedBoard,
                advancedSource
            );
            advancedReader.objects = {};
            advancedReader.read();
            const advanced = Object.fromEntries(
                ["CC1", "CC2", "CL1", "CL2", "O", "B1", "B2"].map(
                    (name) => {
                        const value = advancedBoard.select(name);
                        return [
                            name,
                            {
                                id: value.id,
                                name: value.name,
                                elType: value.elType,
                                coords:
                                    typeof value.X === "function"
                                        ? [value.X(), value.Y()]
                                        : null,
                                stdform:
                                    value.stdform?.slice(0, 3) ?? null
                            }
                        ];
                    }
                )
            );
            JXG.JSXGraph.freeBoard(advancedBoard);
            advancedHost.remove();
            return {
                parseOnly,
                drawn,
                circleByThreePoints,
                angleBisector,
                advanced
            };
        } finally {
            JXG.debug = originalDebug;
        }
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
