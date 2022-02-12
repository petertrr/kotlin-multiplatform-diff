plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation("org.ajoberstar.reckon:reckon-gradle:0.14.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.6.10")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get()}")
}