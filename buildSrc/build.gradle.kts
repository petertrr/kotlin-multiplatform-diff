plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
dependencies {
    implementation("org.ajoberstar.reckon:reckon-gradle:0.18.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.8.10")
    implementation("io.github.gradle-nexus:publish-plugin:1.3.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.findVersion("kotlin").get()}")
}