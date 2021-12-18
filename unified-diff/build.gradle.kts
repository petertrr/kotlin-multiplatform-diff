import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    jacoco
}

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

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
        val commonMain by getting {
            dependencies {
                implementation(projects.core)
                implementation(libs.okio)
            }
        }
    }
}
