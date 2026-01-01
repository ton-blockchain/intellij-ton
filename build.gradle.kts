
import groovy.xml.XmlParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
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
    id("org.jetbrains.grammarkit") version "2023.3.0.1"
    id("org.jetbrains.changelog") version "2.5.0"
}

allprojects {
    apply(plugin = "kotlin")

    tasks.withType<KotlinCompile> {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
        compilerOptions.freeCompilerArgs.add("-Xjvm-default=all")
    }

    dependencies {
        testImplementation("junit:junit:4.13.2")
    }

    java {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
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
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        rustRover(version)
        bundledPlugin("org.toml.lang")
        testFramework(TestFrameworkType.Platform)
    }
    implementation(project(":util"))
    implementation(project(":blueprint"))
    implementation(project(":asm"))
    implementation(project(":boc"))
    implementation(project(":acton"))
    implementation(project(":tolk"))
    implementation(project(":fift"))
    implementation(project(":func"))
    implementation(project(":tlb"))
    implementation(project(":tasm"))
}

intellijPlatform {
    autoReload.set(false)
    instrumentCode.set(false)
    buildSearchableOptions.set(false)
    pluginConfiguration {
        id = "org.ton.intellij-ton"
        name = "TON"
        version = project.version.toString()
        description = """
        TON Blockchain Development Plugin — a JetBrains plugin that brings first-class TON blockchain support to IntelliJ-based IDEs.

        - Syntax highlighting, code completion, navigation and inspections for Tolk, FunC, Fift (including assembly), TL-B schemas and TASM (TON Assembly)
        - Integration with Blueprint
        - Works in IntelliJ IDEA, WebStorm, PyCharm, GoLand and other JetBrains IDEs

        Everything you need to develop, test and ship TON smart contracts—right from your editor.
        """.trimIndent()
        changeNotes.set(
            provider {
                changelog.renderItem(changelog.getLatest(), Changelog.OutputType.HTML)
            }
        )
        ideaVersion {
            sinceBuild.set("242")
            untilBuild = provider { null }
        }
        vendor {
            name = "TON Core"
            url = "https://github.com/ton-blockchain/intellij-ton"
            email = "andreypfau@ton.org"
        }
    }
    pluginVerification {
        ides {
            recommended()
            select {

            }
        }
    }

    buildSearchableOptions = false
}

val mergePluginJarsTask = tasks.register<Jar>("mergePluginJars") {
    duplicatesStrategy = DuplicatesStrategy.FAIL
    archiveBaseName.set("intellij-ton")

    exclude("META-INF/MANIFEST.MF")
    exclude("**/classpath.index")

    doFirst {
        val sandboxTask = tasks.prepareSandbox.get()
        val pluginLibDir = sandboxTask.destinationDir.resolve("${sandboxTask.pluginName.get()}/lib")
        val pluginJars = pluginLibDir.listFiles().orEmpty().filter { it.isPluginJar() }
        for (file in pluginJars) {
            from(zipTree(file))
        }
    }
}

tasks {
    prepareSandbox {
        finalizedBy(mergePluginJarsTask)
        enabled = true
    }
    withType<RunIdeTask> {
        // Force `mergePluginJarTask` be executed before any task based on `RunIdeBase` (for example, `runIde` or `buildSearchableOptions`).
        // Otherwise, these tasks fail because of implicit dependency.
        // Should be dropped when jar merging is implemented in `gradle-intellij-plugin` itself
        dependsOn(mergePluginJarsTask)
    }
    verifyPlugin {
        dependsOn(mergePluginJarsTask)
    }
}

changelog {
    version.set(version)
    path.set("${project.projectDir}/CHANGELOG.md")
    header.set(provider { "[${version.get()}]" })
    itemPrefix.set("-")
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    headerParserRegex.set("""(\d+(?:\.\d+)+)""".toRegex())
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}

fun prop(name: String, default: (() -> String?)? = null) = extra.properties[name] as? String
    ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")

fun File.isPluginJar(): Boolean {
    if (!isFile) return false
    if (extension != "jar") return false
    return zipTree(this).files.any { it.isManifestFile() }
}

fun File.isManifestFile(): Boolean {
    if (extension != "xml") return false
    val rootNode = try {
        val parser = XmlParser()
        parser.parse(this)
    } catch (e: Exception) {
        logger.error("Failed to parse $path", e)
        return false
    }
    return rootNode.name() == "idea-plugin"
}
