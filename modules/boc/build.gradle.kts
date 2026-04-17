plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        rustRover(version)
    }
    implementation(project(":acton"))
    implementation(project(":util"))
    implementation(project(":tasm"))
}
