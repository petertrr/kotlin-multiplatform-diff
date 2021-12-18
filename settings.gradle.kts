rootProject.name = "kotlin-multiplatform-diff"
include("core")

enableFeaturePreview("VERSION_CATALOGS")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}
