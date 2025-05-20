plugins {
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        webstorm(version)
        bundledPlugin("org.intellij.plugins.markdown")
    }
}
