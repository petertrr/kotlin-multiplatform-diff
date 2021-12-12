plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.1")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.6.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
}