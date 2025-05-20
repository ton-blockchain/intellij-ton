plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        webstorm(version)
    }
    compileOnly(project(":func"))
    compileOnly(project(":tolk"))
    compileOnly(project(":util"))
}
