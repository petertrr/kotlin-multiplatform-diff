pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.17.6")
    id("org.ajoberstar.reckon.settings") version("0.18.2")
}

rootProject.name = "kotlin-multiplatform-diff"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

extensions.configure<org.ajoberstar.reckon.gradle.ReckonExtension> {
    setDefaultInferredScope("minor")
    scopeFromProp()
    stageFromProp("alpha", "rc", "final")  // version string will be based on last commit; when checking out a tag, that
}

if (System.getenv("CI") != null) {
    gradleEnterprise {
        buildScan {
            termsOfServiceUrl = "https://gradle.com/terms-of-service"
            termsOfServiceAgree = "yes"
        }
    }
}
