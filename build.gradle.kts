plugins {
    kotlin("multiplatform") version "1.4.30"
}

group = "com.github.petertrr"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

kotlin {
    jvm()
    sourceSets {
        val commonMain by getting {
            dependencies {

            }
        }
    }
}
