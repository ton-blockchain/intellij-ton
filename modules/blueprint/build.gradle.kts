plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdeaUltimate(version)
        bundledPlugin("JavaScript")
    }
    compileOnly(project(":util"))
}

