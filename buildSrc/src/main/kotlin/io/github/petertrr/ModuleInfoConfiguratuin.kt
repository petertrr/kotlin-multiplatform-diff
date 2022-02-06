package io.github.petertrr

import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named

fun Project.createModuleInfoCompilation() = tasks.create<JavaCompile>("compileJavaModuleInfo") {
//    val compileKotlinJvm = tasks.getByName<KotlinCompile>("compileKotlinJvm")
    val sourceDir = file("src/jvm9Main")
//    val targetDir = compileKotlinJvm.destinationDir.resolve("../java9/")

    // Use a Java 11 compiler for the module info.
    javaCompiler.set(project.extensions.getByType<JavaToolchainService>().compilerFor {
        languageVersion.set(JavaLanguageVersion.of(11))
    })

//    dependsOn(compileKotlinJvm)
    source(sourceDir)





    // Configure the JAR task so that it will include the compiled module-info class file.
    tasks.named<Jar>("jvmJar") {
        dependsOn(this@create)
        manifest {
            attributes("Multi-Release" to true)
        }
//        from(targetDir) {
//            into("META-INF/versions/9/")
//        }
    }
}
