import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import javax.xml.parsers.DocumentBuilderFactory

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

val publishedModules = listOf(
    "jsxgraph-core",
    "jsxgraph-compose",
)

allprojects {
    group = "com.swithun"
    version = "0.1.0"
}

subprojects {
    pluginManager.withPlugin("maven-publish") {
        if (project.name in publishedModules) {
            extensions.configure<PublishingExtension> {
                repositories {
                    maven {
                        name = "build"
                        url = rootProject.layout.buildDirectory.dir(
                            "maven-repository",
                        ).get().asFile.toURI()
                    }
                }
                publications.withType<MavenPublication>().configureEach {
                    pom {
                        name.set(project.name)
                        description.set(
                            "Native JSXGraph translation for Kotlin and " +
                                "Compose Multiplatform.",
                        )
                        url.set(
                            "https://github.com/swithun-liu/cmp-jsxgraph",
                        )
                        licenses {
                            license {
                                name.set("MIT License")
                                url.set("https://opensource.org/licenses/MIT")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("swithun")
                                name.set("swithun")
                            }
                        }
                        scm {
                            url.set(
                                "https://github.com/swithun-liu/cmp-jsxgraph",
                            )
                            connection.set(
                                "scm:git:https://github.com/" +
                                    "swithun-liu/cmp-jsxgraph.git",
                            )
                            developerConnection.set(
                                "scm:git:ssh://git@github.com/" +
                                    "swithun-liu/cmp-jsxgraph.git",
                            )
                        }
                    }
                }
            }
        }
    }
}

tasks.register("verifyPublicationCoordinates") {
    group = "verification"
    description = "Verifies production Maven coordinates and POM metadata."
    dependsOn(
        publishedModules.map { module ->
            ":$module:generatePomFileForKotlinMultiplatformPublication"
        },
    )

    doLast {
        val documentBuilderFactory = DocumentBuilderFactory.newInstance()
        documentBuilderFactory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl",
            true,
        )
        val documentBuilder = documentBuilderFactory.newDocumentBuilder()

        publishedModules.forEach { module ->
            val pom = rootProject.layout.projectDirectory
                .file(
                    "$module/build/publications/" +
                        "kotlinMultiplatform/pom-default.xml",
                )
                .asFile
            val document = documentBuilder.parse(pom)
            val actualGroup = document
                .getElementsByTagName("groupId")
                .item(0)
                .textContent
            val actualArtifact = document
                .getElementsByTagName("artifactId")
                .item(0)
                .textContent
            val actualVersion = document
                .getElementsByTagName("version")
                .item(0)
                .textContent
            val license = document
                .getElementsByTagName("license")
                .item(0)
                ?.textContent
                .orEmpty()
            val expected =
                "${project.group}:$module:${project.version}"
            val actual =
                "$actualGroup:$actualArtifact:$actualVersion"

            if (actual != expected) {
                throw GradleException(
                    "Expected publication $expected, found $actual",
                )
            }
            if ("MIT License" !in license) {
                throw GradleException(
                    "Publication $actual does not declare the MIT license",
                )
            }
            logger.lifecycle("Verified publication $actual")
        }
    }
}
