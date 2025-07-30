plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.25"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        webstorm(version)
        bundledPlugin("org.intellij.plugins.markdown")
    }
}

intellijPlatform {
    instrumentCode.set(false)
}
