@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import io.github.petertrr.configurePublishing
import io.github.petertrr.ext.booleanProperty
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.detekt)
    alias(libs.plugins.versions)
    id("jacoco-convention")
}

group = "io.github.petertrr"
description = "A multiplatform Kotlin library for calculating text differences"

dependencies {
    detektPlugins(libs.detekt.formatting)
}

kotlin {
    explicitApi()

    compilerOptions {
        apiVersion = KotlinVersion.KOTLIN_1_9
        languageVersion = KotlinVersion.KOTLIN_1_9
    }

    jvm {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    // Minimum bytecode level is 52
                    jvmTarget = JvmTarget.JVM_1_8

                    // Output interfaces with default methods
                    freeCompilerArgs.addAll(
                        "-Xjvm-default=all",      // Output interfaces with default methods
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
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        nodejs()
        //d8()
        binaries.library()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmWasi {
        nodejs()
        binaries.library()
    }

    // native, see https://kotlinlang.org/docs/native-target-support.html
    // tier 1
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()
    iosArm64()

    // tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()

    // tier 3
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()
    mingwX64()
    watchosDeviceArm64()

    @OptIn(ExperimentalSwiftExportDsl::class)
    swiftExport {}

    sourceSets {
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

configurePublishing()

detekt {
    buildUponDefaultConfig = true
    config.setFrom(files("detekt.yml"))
    autoCorrect = booleanProperty("detektAutoCorrect", default = true)
}

tasks {
    withType<Detekt> {
        named("check") {
            dependsOn(this@withType)
        }
    }

    // skip tests which require XCode components to be installed
    named("tvosSimulatorArm64Test") { enabled = false }
    named("watchosSimulatorArm64Test") { enabled = false }
}
