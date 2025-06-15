import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.6.0"
}

rootProject.name = "intellij-ton"
gradle.startParameter.showStacktrace = ShowStacktrace.ALWAYS_FULL

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS

    repositories {
        mavenCentral()
        intellijPlatform {
            defaultRepositories()
        }
        maven(url = "https://jitpack.io")
    }
}

//include(":blueprint")
module("asm")
module("tolk")
module("util")
module("func")
//module("fc2tolk")
//module("fc2tolk-js")
module("boc")
module("tlb")
module("fift")
module("blueprint")

fun module(name: String) {
    include("$name")
    project(":$name").projectDir = file("modules/$name")
}
