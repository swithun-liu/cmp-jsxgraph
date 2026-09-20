import { existsSync, mkdirSync, statSync } from "node:fs";
import { resolve } from "node:path";
import puppeteer from "puppeteer";
import {
    repositoryRoot,
    selectedParityCaseIds
} from "./corpus.mjs";

const baseUrl = process.env.BASE_URL ?? "http://127.0.0.1:8093/";
const outputDirectory = resolve(
    repositoryRoot,
    process.env.OUTPUT_DIR ?? "captures/local/web-parity/current"
);
const viewportWidth = readPositiveNumber("VIEWPORT_WIDTH", 1200);
const viewportHeight = readPositiveNumber("VIEWPORT_HEIGHT", 900);
const minimumCaptureBytes = readPositiveNumber(
    "MIN_CAPTURE_BYTES",
    5_000
);
const interactionTrace = readInteractionTrace();
const previews = ["native", "official"];
const pageErrors = [];

mkdirSync(outputDirectory, {recursive: true});
const browser = await puppeteer.launch(browserLaunchOptions());
try {
    const page = await browser.newPage();
    await page.setViewport({
        width: viewportWidth,
        height: viewportHeight,
        deviceScaleFactor: 1
    });
    page.on("pageerror", (error) => {
        pageErrors.push(error.message);
    });
    page.on("console", (message) => {
        if (
            message.type() === "error" ||
            message.text().includes("error compiling function")
        ) {
            pageErrors.push(message.text());
        }
    });

    for (const caseId of selectedParityCaseIds()) {
        for (const preview of previews) {
            const errorCountBeforeNavigation = pageErrors.length;
            const url = new URL(baseUrl);
            url.searchParams.set("audit", "true");
            url.searchParams.set("preview", preview);
            url.searchParams.set("caseId", caseId);
            await page.goto(url.href, {
                waitUntil: "domcontentloaded",
                timeout: 60_000
            });
            await waitForPreview(page, preview);
            await page.evaluate(() => new Promise((resolveFrame) => {
                requestAnimationFrame(() => requestAnimationFrame(resolveFrame));
            }));
            await new Promise((resolveWait) => setTimeout(resolveWait, 500));
            let beforeInteraction = null;
            if (interactionTrace?.caseId === caseId) {
                beforeInteraction = await page.screenshot({
                    omitBackground: false
                });
                await dragPoint(page, interactionTrace);
                await page.evaluate(() => new Promise((resolveFrame) => {
                    requestAnimationFrame(() => requestAnimationFrame(resolveFrame));
                }));
                await new Promise((resolveWait) => setTimeout(resolveWait, 250));
            }

            const target = resolve(
                outputDirectory,
                `${caseId}_${preview}.png`
            );
            const capture = await page.screenshot({
                path: target,
                omitBackground: false
            });
            if (
                beforeInteraction !== null &&
                Buffer.compare(
                    Buffer.from(beforeInteraction),
                    Buffer.from(capture)
                ) === 0
            ) {
                throw new Error(
                    `${caseId}/${preview} interaction produced no visual change`
                );
            }
            const captureBytes = statSync(target).size;
            if (captureBytes < minimumCaptureBytes) {
                throw new Error(
                    `${caseId}/${preview} produced only ${captureBytes} bytes`
                );
            }
            const newErrors = pageErrors.slice(errorCountBeforeNavigation);
            if (newErrors.length > 0) {
                throw new Error(
                    `${caseId}/${preview} logged errors: ${newErrors.join(" | ")}`
                );
            }
            console.log(
                `${caseId}/${preview}: ${captureBytes} bytes`
            );
        }
    }
} finally {
    await browser.close();
}

async function dragPoint(page, trace) {
    const moves = trace.moves ?? [{from: trace.from, to: trace.to}];
    for (const move of moves) {
        const start = userToScreen(
            move.from,
            trace.boundingBox,
            viewportWidth,
            viewportHeight
        );
        const end = userToScreen(
            move.to,
            trace.boundingBox,
            viewportWidth,
            viewportHeight
        );
        await page.mouse.move(start.x, start.y);
        await page.mouse.down();
        await page.mouse.move(end.x, end.y, {steps: 100});
        await new Promise((resolveWait) => setTimeout(resolveWait, 100));
        await page.mouse.move(end.x, end.y);
        await new Promise((resolveWait) => setTimeout(resolveWait, 100));
        await page.mouse.up();
    }
}

function userToScreen(point, bounds, width, height) {
    const requestedWidth = bounds.right - bounds.left;
    const requestedHeight = bounds.top - bounds.bottom;
    const scale = Math.min(
        width / requestedWidth,
        height / requestedHeight
    );
    const centerX = (bounds.left + bounds.right) * 0.5;
    const centerY = (bounds.top + bounds.bottom) * 0.5;
    const left = centerX - width * 0.5 / scale;
    const top = centerY + height * 0.5 / scale;
    return {
        x: (point.x - left) * scale,
        y: (top - point.y) * scale
    };
}

function readInteractionTrace() {
    const name = process.env.INTERACTION_TRACE;
    if (name === undefined || name.length === 0) {
        return null;
    }
    const traces = {
        baseline_point_drag: {
            caseId: "baseline_geometry",
            from: {x: 3.2, y: 2.1},
            to: {x: 1.1, y: 0.55},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        direction_point_arc_flip: {
            caseId: "prod_arc_direction_route",
            from: {x: 0, y: -2.2},
            to: {x: 0, y: 3.8},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        arc_composition_parent_drag: {
            caseId: "arc_compositions",
            from: {x: 1, y: 4},
            to: {x: 0, y: -4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        sector_composition_parent_drag: {
            caseId: "sector_compositions",
            from: {x: 0, y: 4},
            to: {x: 0, y: -4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        circumcircle_creator_parent_drag: {
            caseId: "circumcircle_creators",
            from: {x: 0, y: 4},
            to: {x: 1, y: 2},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        point_reflection_source_drag: {
            caseId: "point_reflections",
            from: {x: -3, y: 1},
            to: {x: -2, y: 2},
            boundingBox: {
                left: -6,
                top: 6,
                right: 6,
                bottom: -6
            }
        },
        bisector_lines_parent_drag: {
            caseId: "bisector_lines",
            from: {x: -4, y: -1},
            to: {x: -2, y: -3},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        circumcircle_parent_drag: {
            caseId: "prod_geometry_circumcircle",
            from: {x: 3, y: -1},
            to: {x: 4, y: -2},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        midpoint_parent_drag: {
            caseId: "prod_geometry_midpoints",
            from: {x: 4, y: 2},
            to: {x: 2, y: 4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        orthogonal_parent_drag: {
            caseId: "prod_geometry_orthogonal_constructions",
            from: {x: 3, y: -3},
            to: {x: 0, y: 4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        dynamic_circle_radius_clamp: {
            caseId: "prod_interaction_dynamic_circle_radius",
            from: {x: 3, y: 0},
            to: {x: -2, y: 1.5},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        dynamic_segment_length: {
            caseId: "prod_geometry_fixed_length_segment",
            from: {x: 3, y: -2.5},
            to: {x: 5, y: -2.5},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        transformed_point_driver: {
            caseId: "transformed_points",
            from: {x: 2, y: 0},
            to: {x: 5, y: 0},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        function_coordinate_driver: {
            caseId: "function_coordinate_points",
            from: {x: -3, y: -2},
            to: {x: -1, y: 1},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        parallel_parent_drag: {
            caseId: "parallel_constructions",
            from: {x: 3, y: -3},
            to: {x: 0, y: 4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        line_arrow_parent_drag: {
            caseId: "line_arrows",
            from: {x: 0, y: -4.8},
            to: {x: 4, y: -3},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        triangle_center_parent_drag: {
            caseId: "triangle_centers",
            from: {x: 4, y: -3},
            to: {x: 1, y: 4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        intersection_circle_parent_drag: {
            caseId: "intersection_points",
            from: {x: 0, y: 0},
            to: {x: 0, y: 4},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        polygon_path_parent_drag: {
            caseId: "polygon_path_intersections",
            from: {x: 2, y: 2},
            to: {x: 3, y: 2},
            boundingBox: {
                left: -5,
                top: 5,
                right: 5,
                bottom: -5
            }
        },
        polygonal_chain_parent_drag: {
            caseId: "polygonal_chains",
            from: {x: 4, y: 3},
            to: {x: 5, y: -2},
            boundingBox: {
                left: -6,
                top: 5,
                right: 6,
                bottom: -5
            }
        },
        parallelogram_parent_drag: {
            caseId: "parallelograms",
            from: {x: 2, y: -3},
            to: {x: 3, y: 1},
            boundingBox: {
                left: -7,
                top: 7,
                right: 7,
                bottom: -7
            }
        },
        regular_polygon_parent_drag: {
            caseId: "regular_polygons",
            from: {x: 0, y: -2},
            to: {x: 1, y: 0},
            boundingBox: {
                left: -7,
                top: 6,
                right: 7,
                bottom: -6
            }
        },
        radical_axis_parent_drag: {
            caseId: "radical_axis",
            from: {x: -1, y: -1},
            to: {x: -1, y: 1},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        pole_point_parent_drag: {
            caseId: "pole_point",
            moves: [
                {from: {x: 1, y: 1}, to: {x: -2, y: 2}},
                {from: {x: 3, y: 1}, to: {x: 1, y: 2}},
                {from: {x: -1, y: 4}, to: {x: -3, y: -1}},
                {from: {x: 4, y: -1}, to: {x: 2, y: 4}}
            ],
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        tangent_polar_circle_parent_drag: {
            caseId: "tangent_polar_circle",
            from: {x: 2, y: 0},
            to: {x: 1, y: 0},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        tangent_to_point_drag: {
            caseId: "tangent_to_circle",
            from: {x: 5, y: 4},
            to: {x: 3, y: -3},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        tangent_line_parent_drag: {
            caseId: "tangent_line",
            from: {x: 3, y: 2},
            to: {x: 2, y: -3},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        tangent_curve_point_drag: {
            caseId: "tangent_curve",
            from: {x: 0, y: 3.5},
            to: {x: 4.5, y: 5},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        normal_constructions_parent_drag: {
            caseId: "normal_constructions",
            from: {x: -6, y: 3},
            to: {x: -5, y: 1},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        derivative_curve_coefficient_drag: {
            caseId: "derivative_curve",
            from: {x: 0.25, y: 5},
            to: {x: 0.6, y: 5},
            boundingBox: {
                left: -8,
                top: 6,
                right: 8,
                bottom: -6
            }
        },
        spline_curves_tension_drag: {
            caseId: "spline_curves",
            from: {x: 0.35, y: -5.5},
            to: {x: 0.8, y: -5.5},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        riemann_sums_bar_count_drag: {
            caseId: "riemann_sums",
            from: {x: 4, y: -6},
            to: {x: 6, y: -6},
            boundingBox: {
                left: -8,
                top: 7,
                right: 8,
                bottom: -7
            }
        },
        box_plots_driver_drag: {
            caseId: "box_plots",
            from: {x: 5, y: -5.5},
            to: {x: 6.5, y: -5.5},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        comb_driver_drag: {
            caseId: "combs",
            from: {x: 4, y: -5.8},
            to: {x: 6.5, y: -5.8},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        inequality_line_parent_drag: {
            caseId: "inequalities",
            from: {x: -3, y: 3},
            to: {x: -2, y: 1},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        inequality_function_driver_drag: {
            caseId: "inequalities",
            from: {x: 1, y: -5.8},
            to: {x: 2, y: -5.8},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        vector_field_driver_drag: {
            caseId: "vector_fields",
            from: {x: 3, y: -5.8},
            to: {x: 6, y: -5.8},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        slope_field_driver_drag: {
            caseId: "slope_fields",
            from: {x: 3, y: -5.8},
            to: {x: 6, y: -5.8},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        ellipse_point_drag: {
            caseId: "ellipses",
            from: {x: -5, y: 4},
            to: {x: -5, y: 5},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        hyperbola_point_drag: {
            caseId: "hyperbolas",
            from: {x: 0, y: 3},
            to: {x: 1, y: 4},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        parabola_focus_drag: {
            caseId: "parabolas",
            from: {x: -4, y: 1},
            to: {x: -3, y: 2},
            boundingBox: {
                left: -10,
                top: 7,
                right: 10,
                bottom: -7
            }
        },
        curve_boolean_parent_drag: {
            caseId: "curve_boolean_clipping",
            from: {x: 1, y: 2},
            to: {x: 0, y: 3},
            boundingBox: {
                left: -9,
                top: 5,
                right: 9,
                bottom: -5
            }
        }
    };
    const trace = traces[name];
    if (trace === undefined) {
        throw new Error(`Unknown interaction trace: ${name}`);
    }
    return trace;
}

function browserLaunchOptions() {
    const candidates = [
        process.env.PUPPETEER_EXECUTABLE_PATH,
        "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    ].filter(Boolean);
    const executablePath = candidates.find(existsSync);
    return {
        headless: true,
        args: ["--disable-dev-shm-usage", "--no-sandbox"],
        ...(executablePath === undefined ? {} : {executablePath})
    };
}

async function waitForPreview(page, preview) {
    const outcomeHandle = await page.waitForFunction(
        (selectedPreview) => {
            const roots = [document];
            for (let index = 0; index < roots.length; index += 1) {
                const root = roots[index];
                if (selectedPreview === "native") {
                    const canvas = root.querySelector?.("canvas");
                    if (canvas?.width > 0 && canvas?.height > 0) {
                        return {status: "ready"};
                    }
                } else {
                    const frame = root.querySelector?.(
                        'iframe[title="Official JSXGraph 1.13.3 rendering"]'
                    );
                    const error = frame?.contentDocument?.querySelector(
                        "#error[style*='display: flex']"
                    );
                    if (error !== null && error !== undefined) {
                        return {
                            status: "error",
                            message: error.textContent?.trim() ??
                                "Unknown official rendering error"
                        };
                    }
                    const svg = frame?.contentDocument?.querySelector(
                        "#jxgbox svg"
                    );
                    if (
                        svg !== null &&
                        svg !== undefined &&
                        svg.getBoundingClientRect().width > 0 &&
                        svg.getBoundingClientRect().height > 0
                    ) {
                        return {status: "ready"};
                    }
                }
                root.querySelectorAll?.("*").forEach((element) => {
                    if (element.shadowRoot !== null) {
                        roots.push(element.shadowRoot);
                    }
                });
            }
            return null;
        },
        {timeout: 60_000},
        preview
    );
    const outcome = await outcomeHandle.jsonValue();
    await outcomeHandle.dispose();
    if (outcome.status === "error") {
        throw new Error(outcome.message);
    }
}

function readPositiveNumber(name, fallback) {
    const value = Number(process.env[name] ?? fallback);
    if (!Number.isFinite(value) || value <= 0) {
        throw new Error(`${name} must be a positive number`);
    }
    return value;
}
