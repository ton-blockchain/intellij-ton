
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Clock
import java.time.Instant

val publishChannel = prop("publishChannel")
val pluginVersion = prop("pluginVersion").let { pluginVersion ->
    if (publishChannel != "release" && publishChannel != "stable") {
        val buildSuffix = prop("buildNumber") {
            Instant.now(Clock.systemUTC()).toString().substring(2, 16).replace("[-T:]".toRegex(), "")
        }
        "$pluginVersion-${publishChannel.uppercase()}+$buildSuffix"
    } else {
        pluginVersion
    }
}
version = pluginVersion

plugins {
    kotlin("jvm") version "2.0.10"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.changelog") version "2.2.1"
}

allprojects {
    apply(plugin = "kotlin")

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
        compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    }

    sourceSets {
        main {
            kotlin.srcDir("src")
            java.srcDirs("gen")
            resources.srcDir("resources")
        }
        test {
            kotlin.srcDir("test")
            resources.srcDir("testResources")
        }
    }
    tasks {
        test {
            useJUnitPlatform()
        }
    }
}

tasks {
    buildSearchableOptions {
        enabled = false
    }
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        create(IntelliJPlatformType.IntellijIdeaUltimate, version)

        pluginModule(implementation(project(":util")))
        pluginModule(implementation(project(":asm")))
        pluginModule(implementation(project(":tolk")))
        pluginModule(implementation(project(":func")))
        pluginModule(implementation(project(":tact")))
        pluginModule(implementation(project(":boc")))
        pluginModule(implementation(project(":tlb")))
        pluginModule(implementation(project(":fift")))
        pluginModule(implementation(project(":blueprint")))
        pluginModule(implementation(project(":fc2tolk-js")))
    }
}

intellijPlatform {
    pluginConfiguration {
        id = "org.ton.intellij-ton"
        name = "TON"
        version = project.version.toString()
        description = """
        TON Blockchain Development Plugin for IntelliJ: Adds support for TON blockchain programming languages,
        including FunC, Tolk, Fift, Tact, and TL-B schemas.
        Ideal for Web3 developers working within the TON ecosystem.
        """.trimIndent()
        changeNotes.set(
            provider {
                changelog.renderItem(changelog.getLatest(), Changelog.OutputType.HTML)
            }
        )
        ideaVersion {
            sinceBuild.set("243")
            untilBuild = provider { null }
        }
        vendor {
            name = "TON Core"
            url = "https://github.com/ton-blockchain/intellij-ton"
            email = "andreypfau@ton.org"
        }
    }
}

changelog {
    version.set(version)
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

fun prop(name: String, default: (() -> String?)? = null) = extra.properties[name] as? String
    ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")
