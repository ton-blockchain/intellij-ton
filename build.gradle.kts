plugins {
    id("org.jetbrains.intellij") version "1.3.1"
    kotlin("jvm") version "1.6.10"
    java
}

group = "com.github.andreypfau"
version = "0.1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

sourceSets["main"].java.srcDirs("src/main/gen")

intellij {
    version.set("2021.3.1")
}

tasks {
    patchPluginXml {
        untilBuild.set("221.*")
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}