/*
 * Official JSXGraph 1.13.3 Geonext property-reader fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-geonext-properties.mjs
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
    await page.setContent("<main></main>");
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
            "third_party/jsxgraph-src/src/reader/geonext.js"
        )
    });

    const evidence = await page.evaluate(() => {
        const source = [
            "<GEONEXT><elements>",
            "<point><name>A</name><id>P1</id>",
            "<data><x>1.5</x><y>-2</y></data>",
            "<active>true</active><area>9</area><dash>0</dash>",
            "<draft>false</draft><visible>true</visible><trace>false</trace>",
            "<width>3</width><color>",
            "<stroke>#FF000080</stroke><lighting>#00FF0040</lighting>",
            "<fill>#11223320</fill><label>#44556600</label>",
            "<draft>#778899FF</draft></color>",
            "<showinfo>false</showinfo><showcoord>false</showcoord>",
            "<fix>false</fix><style>6</style>",
            "</point>",
            "<line><name>a</name><id>L1</id>",
            "<data><first>P1</first><last>gXOe0</last></data>",
            "<straight><first>false</first><last>true</last></straight>",
            "<draft>true</draft><visible>false</visible><trace>true</trace>",
            "<strokewidth>2</strokewidth><color>",
            "<stroke>#010203FF</stroke><lighting>#04050680</lighting>",
            "<fill>#07080940</fill><label>#0A0B0CFF</label>",
            "<draft>#0D0E0FFF</draft></color><style>11</style>",
            "</line>",
            "</elements><coordinates><viewport>",
            "<left>-4junk</left><top>5</top><right>6</right><bottom>-7</bottom>",
            "</viewport></coordinates></GEONEXT>"
        ].join("");
        const reader = Object.create(JXG.GeonextReader.prototype);
        const tree = JXG.XML.parse(source);
        const elements = tree.getElementsByTagName("elements")[0].childNodes;
        const pointNode = Array.from(elements).find(
            (node) => node.nodeName === "point"
        );
        const lineNode = Array.from(elements).find(
            (node) => node.nodeName === "line"
        );
        const build = (data, point) => {
            let attributes = point ? {strokewidth: 1} : {};
            attributes = reader.defProperties(attributes, data);
            attributes = reader.colorProperties(attributes, data);
            attributes = reader.visualProperties(attributes, data);
            attributes = reader.firstLevelProperties(attributes, data);
            attributes = reader.readNodes(attributes, data, "data");
            if (point) {
                attributes.fixed = JXG.str2Bool(
                    reader.gEBTN(data, "fix")
                );
            } else {
                attributes = reader.readNodes(
                    attributes,
                    data,
                    "straight",
                    "straight"
                );
            }
            return reader.transformProperties(
                attributes,
                point ? "point" : undefined
            );
        };

        const boardHost = document.createElement("div");
        boardHost.id = "board";
        boardHost.style.width = "640px";
        boardHost.style.height = "480px";
        document.body.appendChild(boardHost);
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-5, 5, 5, -5],
            axis: false,
            showCopyright: false,
            showNavigation: false
        });
        const color = [
            "<color>",
            "<stroke>#102030FF</stroke>",
            "<lighting>#203040FF</lighting>",
            "<fill>#30405040</fill>",
            "<label>#405060FF</label>",
            "<draft>#506070FF</draft>",
            "</color>"
        ].join("");
        const elementSource = [
            "<GEONEXT><elements>",
            "<point><name>A</name><id>P1</id>",
            "<data><x>1</x><y>2</y></data><fix>true</fix>",
            "<visible>true</visible><trace>false</trace>",
            "<style>3</style>",
            color,
            "</point>",
            "<point><name>B</name><id>P2</id>",
            "<data><x>4</x><y>2</y></data><fix>false</fix>",
            "<visible>true</visible><trace>false</trace>",
            "<style>3</style>",
            color,
            "</point>",
            "<point><name>C</name><id>P3</id>",
            "<data><x>2.5</x><y>-1</y></data><fix>false</fix>",
            "<visible>true</visible><trace>false</trace>",
            "<style>3</style>",
            color,
            "</point>",
            "<point><name>D</name><id>P4</id>",
            "<data><x>2.5</x><y>5</y></data><fix>false</fix>",
            "<visible>true</visible><trace>false</trace>",
            "<style>3</style>",
            color,
            "</point>",
            "<line><name>l</name><id>L1</id>",
            "<data><first>P1</first><last>P2</last></data>",
            "<straight><first>false</first><last>true</last></straight>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</line>",
            "<line><name>m</name><id>L2</id>",
            "<data><first>P3</first><last>P4</last></data>",
            "<straight><first>true</first><last>true</last></straight>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</line>",
            "<circle><name>c</name><id>C1</id>",
            "<data><midpoint>P1</midpoint><radius>P2</radius></data>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</circle>",
            "<circle><name>d</name><id>C2</id>",
            "<data><midpoint>P1</midpoint>",
            "<radiusvalue>2.5</radiusvalue></data>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</circle>",
            "<circle><name>e</name><id>C3</id>",
            "<data><midpoint>P2</midpoint>",
            "<radiusvalue>3</radiusvalue></data>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</circle>",
            "<intersection><name></name><id>I_LINES</id>",
            "<data><first>L1</first><last>L2</last></data>",
            "<first><name>I</name><id>I0</id>",
            "<visible>true</visible><trace>false</trace>",
            "<strokewidth>3</strokewidth><fix>false</fix>",
            "<style>3</style>",
            color,
            "</first></intersection>",
            "<intersection><name></name><id>I_CIRCLES</id>",
            "<data><first>C1</first><last>C3</last></data>",
            "<first><name>J</name><id>I1</id>",
            "<visible>true</visible><trace>false</trace>",
            "<strokewidth>3</strokewidth><fix>false</fix>",
            "<style>3</style>",
            color,
            "</first>",
            "<last><name>K</name><id>I2</id>",
            "<visible>true</visible><trace>false</trace>",
            "<strokewidth>3</strokewidth><fix>true</fix>",
            "<style>3</style>",
            color,
            "</last></intersection>",
            "<arrow><name>v</name><id>V1</id>",
            "<data><first>P2</first><last>P1</last></data>",
            "<straight><first>true</first><last>true</last></straight>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</arrow>",
            "<arc><name>a</name><id>A1</id>",
            "<data><midpoint>P1</midpoint><radius>P2</radius>",
            "<angle>P4</angle></data>",
            "<visible>true</visible><trace>false</trace>",
            "<firstarrow>true</firstarrow><lastarrow>false</lastarrow>",
            color,
            "</arc>",
            "<angle><name>w</name><id>ANG1</id>",
            "<data><first>P2</first><middle>P1</middle>",
            "<last>P4</last><radius>1.25</radius></data>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</angle>",
            "<polygon><name>triangle</name><id>POLY1</id>",
            "<data><vertex>P1</vertex><vertex>P2</vertex>",
            "<vertex>P3</vertex><vertex>P1</vertex></data>",
            "<visible>true</visible>",
            color,
            "<border><name>b1</name><id>B1</id>",
            "<straight><first>true</first><last>false</last></straight>",
            "<strokewidth>2</strokewidth><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            color,
            "</border>",
            "<border><name>b2</name><id>B2</id>",
            "<straight><first>false</first><last>true</last></straight>",
            "<strokewidth>3</strokewidth><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            color,
            "</border>",
            "<border><name>b3</name><id>B3</id>",
            "<straight><first>true</first><last>true</last></straight>",
            "<strokewidth>4</strokewidth><visible>true</visible>",
            "<draft>false</draft><trace>false</trace>",
            color,
            "</border>",
            "</polygon>",
            "<graph><name>f</name><id>G1</id>",
            "<data><function>x*x-1</function></data>",
            "<visible>true</visible><strokewidth>2</strokewidth>",
            color,
            "</graph>",
            "<parametercurve><name>q</name><id>PC1</id>",
            "<functionx>2*t</functionx><functiony>t*t</functiony>",
            "<min>-1</min><max>2</max>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</parametercurve>",
            "<slider><name>S</name><id>S1</id>",
            "<data><x>2.5</x><y>3</y><parent>L1</parent>",
            "<position>0.25</position></data>",
            "<visible>true</visible><trace>false</trace><fix>false</fix>",
            "<style>3</style>",
            color,
            "<animate><animated>false</animated><back>true</back>",
            "<speed>20</speed><start>0</start><stop>1</stop>",
            "<loop>2</loop><direction>false</direction></animate>",
            "<onpolygon>false</onpolygon>",
            "</slider>",
            "<tracecurve><name>trace</name><id>TC1</id>",
            "<tracepoint>S1</tracepoint><traceslider>S1</traceslider>",
            "</tracecurve>",
            "<text><id>T1</id><data><x>1.25</x><y>-0.5</y>",
            "<mp>Hello <b>world</b></mp>",
            "<content>fallback</content><parent></parent></data>",
            "<condition>shown</condition><fix>true</fix><digits>4</digits>",
            "<visible>true</visible><trace>false</trace>",
            color,
            "</text>",
            "<text><id>T2</id><data><x>0.5</x><y>1</y>",
            "<content>anchored</content><parent>L1</parent></data>",
            "<condition></condition><visible>false</visible>",
            "<trace>false</trace>",
            color,
            "</text>",
            "<text><id>oldVersionT3</id></text>",
            "<group><name>pair</name><id>GR1</id>",
            "<data><member>P1</member><member>P2</member>",
            "<member>S1</member></data>",
            color,
            "</group>",
            "</elements></GEONEXT>"
        ].join("");
        const elementTree = JXG.XML.parse(reader.prepareString(elementSource));
        const elementNodes = Array.from(
            elementTree.getElementsByTagName("elements")[0].childNodes
        ).filter((node) => node.nodeType === 1);
        const readErrors = [];
        for (let index = 0; index < elementNodes.length; index += 1) {
            try {
                reader.readNode(elementNodes, index, board);
            } catch (error) {
                readErrors.push({
                    type: elementNodes[index].nodeName,
                    message: error.message
                });
            }
        }
        const readPoint = (id) => {
            const point = board.select(id);
            return {
                id: point.id,
                name: point.name,
                coords: [point.X(), point.Y()],
                fixed: point.visProp.fixed
            };
        };
        const readLine = (id) => {
            const line = board.select(id);
            return {
                id: line.id,
                name: line.name,
                elType: line.elType,
                first: line.point1.id,
                last: line.point2.id,
                straightFirst: line.evalVisProp("straightfirst"),
                straightLast: line.evalVisProp("straightlast"),
                firstArrow: line.evalVisProp("firstarrow"),
                lastArrow: line.evalVisProp("lastarrow")
            };
        };
        const readCircle = (id) => {
            const circle = board.select(id);
            return {
                id: circle.id,
                name: circle.name,
                method: circle.method,
                center: circle.center.id,
                point2: circle.point2?.id ?? null,
                radius: circle.Radius()
            };
        };
        const readIntersection = (id) => {
            const point = board.select(id);
            return {
                id: point.id,
                name: point.name,
                coords: [point.X(), point.Y()],
                fixed: point.evalVisProp("fixed"),
                parents: point.parents
            };
        };
        const readArc = (id) => {
            const arc = board.select(id);
            return {
                id: arc.id,
                name: arc.name,
                center: arc.center.id,
                radiuspoint: arc.radiuspoint.id,
                anglepoint: arc.anglepoint.id,
                radius: arc.Radius(),
                firstArrow: arc.evalVisProp("firstarrow"),
                lastArrow: arc.evalVisProp("lastarrow")
            };
        };
        const readAngle = (id) => {
            const angle = board.select(id);
            return {
                id: angle.id,
                name: angle.name,
                parents: angle.parents,
                radius: angle.Radius(),
                radians: angle.Value("radians")
            };
        };
        const readPolygon = (id) => {
            const polygon = board.select(id);
            return {
                id: polygon.id,
                name: polygon.name,
                vertices: polygon.vertices.map((vertex) => vertex.id),
                borders: polygon.borders.map((border) => ({
                    id: border.id,
                    name: border.name,
                    first: border.point1.id,
                    last: border.point2.id,
                    straightFirst: border.evalVisProp("straightfirst"),
                    straightLast: border.evalVisProp("straightlast")
                }))
            };
        };
        const readCurve = (id, parameters) => {
            const curve = board.select(id);
            return {
                id: curve.id,
                name: curve.name,
                elType: curve.elType,
                curveType: curve.evalVisProp("curvetype"),
                min: curve.minX(),
                max: curve.maxX(),
                values: parameters.map((parameter) => [
                    curve.X(parameter),
                    curve.Y(parameter)
                ])
            };
        };
        const readTraceCurve = (id) => {
            const curve = board.select(id);
            const first = curve.points[0].usrCoords;
            const last = curve.points[curve.numberPoints - 1].usrCoords;
            return {
                id: curve.id,
                name: curve.name,
                curveType: curve.evalVisProp("curvetype"),
                numberPoints: curve.numberPoints,
                first: [first[1], first[2]],
                last: [last[1], last[2]]
            };
        };
        const slider = board.select("S1");
        const group = board.select("GR1");
        const readText = (id) => {
            const text = board.select(id);
            return {
                id: text.id,
                name: text.name,
                coords: [text.X(), text.Y()],
                relativeCoords: text.relativeCoords
                    ? [
                        text.relativeCoords.usrCoords[1],
                        text.relativeCoords.usrCoords[2]
                    ]
                    : null,
                orgText: text.orgText,
                plaintext: text.plaintext,
                anchor: text.element?.id ?? null,
                digits: text.evalVisProp("digits"),
                fixed: text.evalVisProp("fixed"),
                visible: text.evalVisProp("visible")
            };
        };

        return {
            version: JXG.version,
            point: build(pointNode, true),
            line: build(lineNode, false),
            viewport: reader.readViewPort(
                tree.getElementsByTagName("coordinates")[0]
            ),
            originIds: ["gOOe0", "gXOe0", "other"].map((id) =>
                reader.changeOriginIds({id: "board-"}, id)
            ),
            drawing: {
                points: [
                    readPoint("P1"),
                    readPoint("P2"),
                    readPoint("P3"),
                    readPoint("P4")
                ],
                line: readLine("L1"),
                pointCircle: readCircle("C1"),
                radiusCircle: readCircle("C2"),
                arrow: readLine("V1"),
                lineIntersection: readIntersection("I0"),
                circleIntersections: [
                    readIntersection("I1"),
                    readIntersection("I2")
                ],
                topIntersectionIds: [
                    board.select("I_LINES"),
                    board.select("I_CIRCLES")
                ].map((element) => element?.id ?? null),
                arc: readArc("A1"),
                angle: readAngle("ANG1"),
                polygon: readPolygon("POLY1"),
                graph: readCurve("G1", [-2, 0, 3]),
                parameterCurve: readCurve("PC1", [-1, 0.5, 2]),
                slider: {
                    id: slider.id,
                    name: slider.name,
                    coords: [slider.X(), slider.Y()],
                    position: slider.position,
                    parent: slider.slideObject.id,
                    onPolygon: slider.onPolygon,
                    isGeonext: slider.isGeonext,
                    fixed: slider.evalVisProp("fixed")
                },
                traceCurve: readTraceCurve("TC1"),
                texts: [readText("T1"), readText("T2")],
                oldVersionText: board.select("oldVersionT3")?.id ?? null,
                group: {
                    id: group.id,
                    name: group.name,
                    members: Object.keys(group.objects),
                    parents: group.getParents()
                },
                readErrors
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
