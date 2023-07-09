pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.13.4")
}

rootProject.name = "kotlin-multiplatform-diff"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

if (System.getenv("CI") != null) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
