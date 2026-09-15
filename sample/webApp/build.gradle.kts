import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "cmp-jsxgraph.js"
            }
        }
        binaries.executable()
    }

    sourceSets {
        wasmJsMain {
            resources.srcDir(
                rootProject.file("jsxgraph-debug-ui/src/androidMain/assets"),
            )
            dependencies {
                implementation(project(":jsxgraph-debug-ui"))
                implementation(compose.ui)
                implementation(libs.kotlinx.browser)
            }
        }
    }
}
