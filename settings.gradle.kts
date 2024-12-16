import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    id("org.jetbrains.intellij.platform.settings") version "2.2.0"
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
include(":modules:asm")
include(":modules:tolk")
include(":modules:util")
