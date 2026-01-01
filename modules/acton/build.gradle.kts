import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.intellij.platform.module")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        rustRover(version)
        bundledPlugin("org.toml.lang")
        bundledPlugin("com.intellij.modules.json")
        bundledModule("intellij.platform.coverage")
        testFramework(TestFrameworkType.Platform)
    }

    implementation(project(":util"))
}

intellijPlatform {
    buildSearchableOptions = false
}
