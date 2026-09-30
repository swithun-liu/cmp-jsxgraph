/*
 * Official JSXGraph 1.13.3 TraceEnPoche read fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/reader-tracenpoche.mjs
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

const source = [
    "@options;",
    "@figure;",
    "A=point(1,2);",
    "B=point(3,4);",
    "C=point(0,3);",
    "X1=point(-2,0);",
    "X2=point(2,0);",
    "Y1=point(2,-2);",
    "Y2=point(2,2);",
    "target=droite(X1,X2);",
    "direction=droite(Y1,Y2);",
    "r=reel(0.5,0,2,0.25){sansnom};",
    "P=pointsur(A,B,r);",
    "midline=mediatrice(A,B);",
    "Q=projete(C,target,direction);",
    "f=fonction(carre(x)+racine(4)+tan(x));",
    "tanline=tangente(f,r);",
    "homo=homothetie(A,2);",
    "hImage=image(homo,B);",
    "turn=rotation(A,90);",
    "rImage=image(turn,B);",
    "mirror=symetrie(A);",
    "sImage=image(mirror,B);",
    "shift=translation(A,B);",
    "tImage=image(shift,A);",
    "flip=reflexion(target);",
    "fImage=image(flip,B);",
    "N=image(7,8);"
].join("\n");

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
            "third_party/jsxgraph-src/src/reader/tracenpoche.js"
        )
    });

    const evidence = await page.evaluate((fixtureSource) => {
        const board = JXG.JSXGraph.initBoard("board", {
            boundingbox: [-10, 10, 10, -10],
            axis: false,
            showNavigation: false,
            showCopyright: false
        });
        const reader = new JXG.TracenpocheReader(board, fixtureSource);
        const figure = fixtureSource.slice(
            fixtureSource.indexOf("@figure;") + 8
        );
        const generated = reader.parse(
            reader.tokenize(
                figure,
                "=<>!+-*&|/%^#",
                "=<>&|"
            ),
            "tep"
        );
        const tep = {};
        try {
            reader.parseOptions(board);
            reader.board = board;
            new Function("that", "tep", generated)(reader, tep);
            for (const [name, value] of Object.entries(tep)) {
                if (typeof value?.setAttribute === "function") {
                    value.setAttribute({name});
                }
            }
        } catch (error) {
            return {
                version: JXG.version,
                generated,
                error: error.message
            };
        }
        board.update();

        const selected = (name) =>
            tep[name] ?? board.elementsByName[name] ?? board.select(name);
        const point = (name) => {
            const element = selected(name);
            return {
                type: element?.elType ?? null,
                id: element?.id ?? null,
                name: element?.name ?? null,
                x: typeof element?.X === "function" ? element.X() : null,
                y: typeof element?.Y === "function" ? element.Y() : null
            };
        };
        const line = (name) => {
            const element = selected(name);
            return {
                type: element?.elType ?? null,
                id: element?.id ?? null,
                name: element?.name ?? null,
                stdform: element?.stdform?.slice(0, 3) ?? null
            };
        };
        const before = {
            slider: selected("r").Value(),
            point: point("P"),
            tangent: line("tanline")
        };
        selected("r").setValue(1.25);
        board.update();

        return {
            version: JXG.version,
            boundingBox: board.getBoundingBox(),
            before,
            after: {
                slider: selected("r").Value(),
                point: point("P"),
                tangent: line("tanline")
            },
            midpointBisector: line("midline"),
            parallelProjection: point("Q"),
            homothety: point("hImage"),
            rotation: point("rImage"),
            symmetry: point("sImage"),
            translation: point("tImage"),
            reflection: point("fImage"),
            numericImage: point("N"),
            functionAtHalf:
                typeof selected("f")?.Y === "function"
                    ? selected("f").Y(0.5)
                    : null,
            functionSelection: {
                type: selected("f")?.elType ?? null,
                id: selected("f")?.id ?? null,
                name: selected("f")?.name ?? null
            },
            names: [
                "r",
                "P",
                "midline",
                "Q",
                "f",
                "tanline",
                "hImage",
                "rImage",
                "sImage",
                "tImage",
                "fImage",
                "N"
            ].map((name) => ({
                name,
                id: selected(name).id,
                type: selected(name).elType
            }))
        };
    }, source);
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
