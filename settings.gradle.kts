pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.enterprise") version("3.16")
    id("org.ajoberstar.reckon.settings") version("0.18.0")
}

rootProject.name = "kotlin-multiplatform-diff"

dependencyResolutionManagement {
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
