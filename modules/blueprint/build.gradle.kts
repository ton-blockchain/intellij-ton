plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        webstorm(version)
        bundledPlugin("JavaScript")
    }
    compileOnly(project(":util"))
    compileOnly(project(":tolk"))
    compileOnly(project(":func"))
}
