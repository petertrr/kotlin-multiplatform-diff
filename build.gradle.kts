@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import io.github.petertrr.configurePublishing
import io.github.petertrr.ext.booleanProperty
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.detekt)
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
            compilerOptions.configure {
                // Minimum bytecode level is 52
                jvmTarget = JvmTarget.JVM_1_8

                // Output interfaces with default methods
                freeCompilerArgs.add("-Xjvm-default=all")
            }
        }

        testRuns.configureEach {
            executionTask.configure {
                useJUnitPlatform()
            }
        }
    }

    js {
        browser()
        nodejs()
    }

    linuxX64()
    mingwX64()
    macosX64()

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
}
