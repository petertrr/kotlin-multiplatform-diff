import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnPlugin
import org.jetbrains.kotlin.gradle.targets.js.yarn.YarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnPlugin
import org.jetbrains.kotlin.gradle.targets.wasm.yarn.WasmYarnRootEnvSpec
import org.jetbrains.kotlin.gradle.targets.web.yarn.BaseYarnRootEnvSpec

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokka)
    alias(libs.plugins.detekt)
    alias(libs.plugins.vanniktech)
    jacoco
}

group = "io.github.petertrr"
description = "A multiplatform Kotlin library for calculating text differences"

dependencies {
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    explicitApi()
    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_2_3
        languageVersion = KotlinVersion.KOTLIN_2_3
    }

    jvm {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget = JvmTarget.JVM_1_8 // Minimum bytecode level is 52
                    jvmDefault = JvmDefaultMode.NO_COMPATIBILITY // Output interfaces with default methods
                    freeCompilerArgs.addAll(
                        "-Xno-param-assertions",  // Remove Intrinsics.checkNotNullParameter
                    )
                }
            }
        }

        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js {
        val testConfig: (KotlinJsTest).() -> Unit = {
            useMocha {
                // Override default 2s timeout
                timeout = "120s"
            }
        }

        browser {
            testTask(testConfig)
        }

        nodejs {
            testTask(testConfig)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
    }

    // Tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // Tier 3
    mingwX64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    watchosDeviceArm64()

    // Deprecated.
    // Should follow the same route as official Kotlin libraries
    @Suppress("DEPRECATION")
    linuxArm32Hfp()

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

mavenPublishing {
    coordinates(
        groupId = project.group.toString(),
        artifactId = project.name,
        version = project.version.toString(),
    )

    // Publishing to Maven Central requires the following Gradle properties:
    //   mavenCentralUsername=central_username
    //   mavenCentralPassword=central_password
    publishToMavenCentral()

    // Signing is enabled only if the key is actually provided.
    // We do not want missing signing info to block publication to local.
    val signingKey = project.providers.gradleProperty("signingInMemoryKey")

    if (signingKey.isPresent) {
        // Signing requires the following Gradle properties:
        //   signingInMemoryKeyId=pgp_key_id
        //   signingInMemoryKey=pgp_key
        //   signingInMemoryKeyPassword=pgp_key_password
        signAllPublications()
    }

    pom {
        name.set(project.name)
        description.set(project.description)
        url.set("https://github.com/petertrr/kotlin-multiplatform-diff")

        licenses {
            license {
                name.set("The Apache Software License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("petertrr")
                name.set("Petr Trifanov")
                email.set("peter.trifanov@gmail.com")
            }
        }

        scm {
            url.set("https://github.com/petertrr/kotlin-multiplatform-diff")
            connection.set("scm:git:git://github.com/petertrr/kotlin-multiplatform-diff.git")
        }
    }
}

jacoco {
    toolVersion = "0.8.13"
}

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("detekt.yml"))
    autoCorrect = project.providers.gradleProperty("detektAutoCorrect").map(String::toBoolean).getOrElse(true)
}

tasks {
    check {
        dependsOn(detekt)
    }

    val jvmTest = named<Test>("jvmTest")
    val jacocoReport = register<JacocoReport>("jacocoTestReport") {
        dependsOn(jvmTest)

        val commonMainSources = kotlin.sourceSets["commonMain"].kotlin.sourceDirectories
        val jvmMainSources = kotlin.sourceSets["jvmMain"].kotlin.sourceDirectories

        sourceDirectories.setFrom(files(commonMainSources, jvmMainSources))
        classDirectories.setFrom(layout.buildDirectory.file("classes/kotlin/jvm/main"))
        executionData.setFrom(layout.buildDirectory.files("jacoco/jvmTest.exec"))

        reports {
            xml.required = true
            html.required = true
        }
    }

    jvmTest.configure {
        finalizedBy(jacocoReport)
    }
}

val yarnConfig: BaseYarnRootEnvSpec.() -> Unit = {
    // Use the latest version of Yarn Classic
    version = "1.22.22"

    // This was previously set to false to avoid a nagging error message,
    // but we agreed on setting it to true (the default) to avoid possible
    // malicious scripts.
    // See review on https://github.com/petertrr/kotlin-multiplatform-diff/pull/155
    ignoreScripts = true
}

// Configure Yarn for all K/JS and K/WASM modules
plugins.withType<YarnPlugin> { the<YarnRootEnvSpec>().apply(yarnConfig) }
plugins.withType<WasmYarnPlugin> { the<WasmYarnRootEnvSpec>().apply(yarnConfig) }
