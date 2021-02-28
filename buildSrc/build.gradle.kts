plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.ajoberstar.reckon:reckon-gradle:0.13.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.4.20")
    implementation("io.github.gradle-nexus:publish-plugin:1.0.0")
}