rootProject.name = "intellij-ton"

gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL

enableFeaturePreview("VERSION_CATALOGS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}
