import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.getValue
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    kotlin("multiplatform")
    jacoco
}

kotlin {
    // to register `KotlinJvmTest` tasks before configuring Jacoco
    jvm()
}

// configure Jacoco-based code coverage reports for JVM tests executions
jacoco {
    toolVersion = "0.8.7"
}
val jvmTestTask by tasks.named<Test>("jvmTest") {
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
