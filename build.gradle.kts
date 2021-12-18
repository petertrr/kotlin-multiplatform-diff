import io.github.petertrr.configurePublishing
import io.github.petertrr.configureVersioning
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    jacoco
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

// configure Jacoco-based code coverage reports for JVM tests executions
jacoco {
    toolVersion = "0.8.7"
}
val jvmTestTask by tasks.named<KotlinJvmTest>("jvmTest") {
    configure<JacocoTaskExtension> {
        // this is needed to generate jacoco/jvmTest.exec
        isEnabled = true
    }
}
val jacocoTestReportTask by tasks.register<JacocoReport>("jacocoTestReport") {
    executionData(jvmTestTask.extensions.getByType(JacocoTaskExtension::class.java).destinationFile)
    additionalSourceDirs(kotlin.sourceSets["commonMain"].kotlin.sourceDirectories)
    classDirectories.setFrom(file("$buildDir/classes/kotlin/jvm"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}
jvmTestTask.finalizedBy(jacocoTestReportTask)
jacocoTestReportTask.dependsOn(jvmTestTask)
