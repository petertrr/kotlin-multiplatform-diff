pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradle.develocity") version ("4.1")
    id("org.ajoberstar.reckon.settings") version ("0.19.2")
}

rootProject.name = "kotlin-multiplatform-diff"

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

develocity {
    buildScan {
        val isCI = System.getenv("CI").toBoolean()

        if (isCI) {
            termsOfUseUrl = "https://gradle.com/terms-of-service"
            termsOfUseAgree = "yes"
        }

        publishing {
            onlyIf { isCI }
        }
    }
}

reckon {
    setDefaultInferredScope("minor")
    setScopeCalc(calcScopeFromProp())

    stages("alpha", "rc", "final")
    setStageCalc(calcStageFromProp())
}
