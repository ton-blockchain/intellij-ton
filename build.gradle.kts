
import org.gradle.configurationcache.extensions.capitalized
import org.jetbrains.changelog.Changelog
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
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
    kotlin("jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.grammarkit") version "2022.3.2.2"
    id("org.jetbrains.changelog") version "2.2.0"
}

allprojects {
    apply(plugin = "kotlin")

    repositories {
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
        kotlinOptions.freeCompilerArgs += "-Xjvm-default=all"
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
    }
}

dependencies {
    implementation("me.alllex.parsus:parsus-jvm:0.6.1")
    implementation("com.github.andreypfau.tlb:tlb-jvm:54070d9405")

    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdeaCommunity(version)

        pluginModule(project(":modules:util"))
        pluginModule(project(":modules:asm"))
        pluginModule(project(":modules:tolk"))
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
            sinceBuild.set("232")
            untilBuild = provider { null }
        }
        vendor {
            name = "TON Core"
            url = "https://github.com/ton-blockchain/intellij-ton"
            email = "andreypfau@ton.org"
        }
    }
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

val generateFuncLexer = generateLexer("Func")
val generateFuncParser = generateParser("Func")

val generateTactLexer = generateLexer("Tact")
val generateTactParser = generateParser("Tact")

val generateFiftLexer = generateLexer("Fift")
val generateFiftParser = generateParser("Fift")

val generateTlbLexer = generateLexer("Tlb")
val generateTlbParser = generateParser("Tlb")

val compileKotlin = tasks.named("compileKotlin") {
    dependsOn(
        generateFuncParser, generateFuncLexer,
        generateTactParser, generateTactLexer,
        generateFiftParser, generateFiftLexer,
        generateTlbParser, generateTlbLexer,
    )
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

tasks {
    jar {
        from({
            configurations.runtimeClasspath.get().filter { file ->
                !file.nameWithoutExtension.startsWith("kotlin-stdlib") &&
                        !file.nameWithoutExtension.startsWith("annotations")
            }.map {
                if (it.isDirectory) it
                else zipTree(it)
            }
        })
    }
}

fun prop(name: String, default: (() -> String?)? = null) = extra.properties[name] as? String
    ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")

fun generateParser(language: String, suffix: String = "", config: GenerateParserTask.() -> Unit = {}) =
    task<GenerateParserTask>("generate${language.capitalized()}Parser${suffix.capitalized()}") {
        sourceFile.set(file("src/main/grammar/${language}Parser.bnf"))
        targetRootOutputDir.set(file("src/gen"))
        pathToParser.set("/org/ton/intellij/${language.lowercase()}/parser/${language}Parser.java")
        pathToPsiRoot.set("/org/ton/intellij/${language.lowercase()}/psi")
        purgeOldFiles.set(true)
        config()
    }

fun generateLexer(language: String) = task<GenerateLexerTask>("generate${language}Lexer") {
    sourceFile.set(file("src/main/grammar/${language}Lexer.flex"))
    targetOutputDir.set(file("src/gen/org/ton/intellij/${language.lowercase()}/lexer"))
    purgeOldFiles.set(true)
}
