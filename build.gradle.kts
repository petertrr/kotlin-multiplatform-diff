import io.github.petertrr.configurePublishing
import io.github.petertrr.configureVersioning
import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform")
    id("jacoco-convention")
    alias(libs.plugins.detekt)
}

configureVersioning()
group = "io.github.petertrr"
description = "A multiplatform Kotlin library for calculating text differences"

kotlin {
    explicitApi()

    jvm()
    js(BOTH) {
        browser()
        nodejs()
    }
    // setup native compilation
    linuxX64()
    mingwX64()
    macosX64()

    sourceSets {
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        // platform-specific dependencies are needed to use actual test runners
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit5"))
                implementation(libs.junit.jupiter.engine)
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

configurePublishing()

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

detekt {
    buildUponDefaultConfig = true
    config = files("detekt.yml")
    autoCorrect = (findProperty("detektAutoCorrect") as String?)?.toBoolean() ?: true
}
dependencies {
    detektPlugins(libs.detekt.formatting)
}
tasks.withType<Detekt> {
    tasks.getByName("check").dependsOn(this)
}
