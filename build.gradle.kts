plugins {
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm") version "1.6.10"
    java
    idea
}

group = "com.github.andreypfau"
version = "0.1.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
}

sourceSets["main"].java.srcDirs("src/main/gen")

intellij {
    version.set("LATEST-EAP-SNAPSHOT")
    updateSinceUntilBuild.set(false)
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}
