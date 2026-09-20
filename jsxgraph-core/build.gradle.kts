import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    id("maven-publish")
}

val normalizedKlibCompilerArguments = listOf(
    "-Xklib-normalize-absolute-path",
    "-Xklib-relative-path-base=${rootProject.projectDir.absolutePath}",
)

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    android {
        namespace = "com.swithun.jsxgraph.core"
        compileSdk = 36
        minSdk = 24
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm()
    wasmJs {
        browser()
        compilerOptions {
            freeCompilerArgs.addAll(normalizedKlibCompilerArguments)
        }
    }
    iosX64 {
        compilerOptions {
            freeCompilerArgs.addAll(normalizedKlibCompilerArguments)
        }
    }
    iosArm64 {
        compilerOptions {
            freeCompilerArgs.addAll(normalizedKlibCompilerArguments)
        }
    }
    iosSimulatorArm64 {
        compilerOptions {
            freeCompilerArgs.addAll(normalizedKlibCompilerArguments)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
