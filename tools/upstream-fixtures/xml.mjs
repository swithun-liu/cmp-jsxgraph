/*
 * Official JSXGraph 1.13.3 XML utility fixture.
 *
 * Run after installing tools/visual-parity dependencies:
 *   node tools/upstream-fixtures/xml.mjs
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
    const evidence = await page.evaluate(() => {
        const captureNode = (node) => ({
            nodeType: node.nodeType,
            nodeName: node.nodeName,
            nodeValue: node.nodeValue,
            data: "data" in node ? node.data : null,
            attributes: node.attributes
                ? Array.from(node.attributes, (attribute) => ({
                    name: attribute.name,
                    value: attribute.value,
                    nodeType: attribute.nodeType,
                    nodeName: attribute.nodeName,
                    nodeValue: attribute.nodeValue
                }))
                : [],
            children: Array.from(node.childNodes, captureNode)
        });
        const source = [
            "<?probe fixture?>",
            "<root xmlns:p=\"urn:probe\" plain=\"A &amp; B\" " +
                "numeric=\"&#x1F600;\">",
            "  <p:item p:key=\"first\">alpha &lt; beta</p:item>",
            "  <!--keep-comment-->",
            "  <![CDATA[raw <xml> & value]]>",
            "  <group>",
            "    <item id=\"one\"/>",
            "    <nested><item id=\"two\"> value </item></nested>",
            "  </group>",
            "  <item id=\"three\"/>",
            "</root>"
        ].join("\n");
        const tree = JXG.XML.parse(source);
        const root = tree.documentElement;
        const items = root.getElementsByTagName("item");
        const prefixed = root.getElementsByTagName("p:item")[0];
        const entityTree = JXG.XML.parse(
            "<!DOCTYPE root [<!ENTITY custom \"custom-value\">" +
                "<!ENTITY nested \"&custom;:nested\">]>" +
                "<root>&nested;</root>"
        );
        const malformed = JXG.XML.parse("<root><open></root>");

        return {
            version: JXG.version,
            tree: captureNode(tree),
            queries: {
                documentRoot: tree.documentElement.nodeName,
                missingAttribute: root.getAttribute("missing"),
                plainAttribute: root.getAttribute("plain"),
                numericAttribute: root.getAttribute("numeric"),
                namespaceAttribute: prefixed.getAttribute("p:key"),
                prefixedText: prefixed.firstChild.data,
                itemOrder: Array.from(
                    items,
                    (item) => item.getAttribute("id")
                ),
                wildcardOrder: Array.from(
                    root.getElementsByTagName("*"),
                    (element) => element.nodeName
                ),
                firstChild: root.firstChild.nodeName,
                firstChildNextSibling:
                    root.firstChild.nextSibling.nodeName
            },
            doctypeAndEntities: captureNode(entityTree),
            malformed: {
                documentElement: malformed.documentElement.nodeName,
                parserErrors:
                    malformed.getElementsByTagName("parsererror").length,
                throws: false
            }
        };
    });
    console.log(JSON.stringify(evidence, null, 2));
} finally {
    await browser.close();
}
