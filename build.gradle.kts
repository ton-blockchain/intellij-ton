import java.time.Clock
import java.time.Instant
plugins {
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm") version "1.6.10"
    java
    idea
}

group = "com.github.andreypfau"
version = "0.4.0"
val isSnapshot = false

if (isSnapshot) {
    val time = Instant.now(Clock.systemUTC())
    val formattedTime = time.toString().substring(2,16).replace("[-T:]".toRegex(), "")
    version = "$version-SNAPSHOT-$formattedTime"
    println("version: $version")
}

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
    patchPluginXml {
        sinceBuild.set("212")
        changeNotes.set(File(rootProject.projectDir, "src/main/resources/META-INF/change-notes.html").readText())
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
}


