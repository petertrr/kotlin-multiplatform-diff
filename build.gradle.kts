import org.gradle.nativeplatform.platform.internal.DefaultNativePlatform.getCurrentOperatingSystem
import org.jetbrains.kotlin.gradle.targets.jvm.tasks.KotlinJvmTest

plugins {
    kotlin("multiplatform") version "1.4.30"
    jacoco
    id("maven-publish")
}

group = "io.github.petertrr"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    explicitApi()

    jvm()
    js(IR) {
        browser()
        nodejs()
    }
    // setup native compilation
    val os = getCurrentOperatingSystem()
    val hostTarget = when {
        os.isLinux -> linuxX64()
        os.isWindows -> mingwX64()
        else -> throw GradleException("Host OS '${os.name}' is not supported in Kotlin/Native $project.")
    }

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
                implementation("org.junit.jupiter:junit-jupiter-engine:5.0.0")
            }
        }
        val jsTest by getting {
            dependencies {
                implementation(kotlin("test-js"))
            }
        }
    }
}

publishing {
    repositories {
        mavenLocal()
    }
}

tasks.withType<KotlinJvmTest> {
    useJUnitPlatform()
}

// configure Jacoco-based code coverage reports for JVM tests executions
jacoco {
    toolVersion = "0.8.6"
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
        xml.isEnabled = true
        html.isEnabled = true
    }
}
jvmTestTask.finalizedBy(jacocoTestReportTask)
jacocoTestReportTask.dependsOn(jvmTestTask)