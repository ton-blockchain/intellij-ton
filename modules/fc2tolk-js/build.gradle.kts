plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdeaUltimate(version)
    }

    compileOnly(project(":util"))
    compileOnly(project(":func"))
}