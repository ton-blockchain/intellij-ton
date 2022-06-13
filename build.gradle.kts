import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.tasks.PublishPluginTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Clock
import java.time.Instant

val channel = prop("publishChannel")
val isCI = System.getenv("CI") != null
val psiViewerPlugin = "PsiViewer:${prop("psiViewerPluginVersion")}"
val graziePlugin = "tanvd.grazi"
val ideaVersion = prop("ideaVersion")

val Project.dependencyCachePath
    get(): String {
        val cachePath = file("${rootProject.projectDir}/deps")
        // If cache path doesn't exist, we need to create it manually
        // because otherwise gradle-intellij-plugin will ignore it
        if (!cachePath.exists()) {
            cachePath.mkdirs()
        }
        return cachePath.absolutePath
    }

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.7.0"
    id("org.jetbrains.intellij") version "1.5.3"
    id("org.jetbrains.grammarkit") version "2021.2.2"
    id("org.jetbrains.changelog") version "1.3.1"
}

group = prop("pluginGroup")
version = prop("pluginVersion")

if (channel != "release" && channel != "stable") {
    val buildSuffix = prop("buildNumber") {
        Instant.now(Clock.systemUTC()).toString().substring(2, 16).replace("[-T:]".toRegex(), "")
    }
    version = "$version-${channel.toUpperCase()}+$buildSuffix"
}

println("intellij-ton version: $version")

repositories {
    mavenCentral()
    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
}

configurations {
    all {
        // Allows using project dependencies instead of IDE dependencies during compilation and test running
        resolutionStrategy.sortArtifacts(ResolutionStrategy.SortOrder.DEPENDENCY_FIRST)
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("com.charleskorn.kaml:kaml:0.43.0")

    testImplementation(kotlin("test-junit"))
}

sourceSets {
    main {
        java.srcDirs("src/gen")
    }
}

intellij {
    version.set(ideaVersion)
    val pluginList = listOf(
            psiViewerPlugin,
            graziePlugin
    )
    plugins.set(pluginList)
    downloadSources.set(!isCI)
    updateSinceUntilBuild.set(false)
    instrumentCode.set(false)
    ideaDependencyCachePath.set(dependencyCachePath)
    sandboxDir.set("$buildDir/$ideaVersion-sandbox")
}

idea {
    module {
        generatedSourceDirs.add(file("src/gen"))
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

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

val generateFuncLexer = generateLexer("Func")
val generateFiftLexer = generateLexer("Fift")
val generateTlbLexer = generateLexer("Tlb")
val generateFuncParser = generateParser("Func")
val generateFiftParser = generateParser("Fift")
val generateTlbParser = generateParser("Tlb")

fun generateLexer(language: String) = task<GenerateLexerTask>("generate${language}Lexer") {
    source.set("src/main/grammars/${language}Lexer.flex")
    targetDir.set("src/gen/org/ton/intellij/${language.toLowerCase()}/lexer")
    targetClass.set("_${language}Lexer")
    purgeOldFiles.set(true)
}

fun generateParser(language: String) = task<GenerateParserTask>("generate${language}Parser") {
    source.set("src/main/grammars/${language}Parser.bnf")
    targetRoot.set("src/gen")
    pathToParser.set("/org/ton/intellij/${language}/parser/${language}Parser.java")
    pathToPsiRoot.set("/org/ton/intellij/${language}/psi")
    purgeOldFiles.set(true)
}

tasks {
    runIde { enabled = true }
    prepareSandbox { enabled = true }
    withType<KotlinCompile> {
        dependsOn(
                generateFuncLexer,
                generateFiftLexer,
                generateTlbLexer,

                generateFuncParser,
                generateFiftParser,
                generateTlbParser
        )
    }
    buildSearchableOptions {
        enabled = prop("enableBuildSearchableOptions").toBoolean()
    }
    patchPluginXml {
        sinceBuild.set("212")
        changeNotes.set(provider {
            changelog.run {
                getLatest()
            }.toHTML()
        })
    }
    withType<PublishPluginTask> {
        token.set(prop("publishToken"))
        channels.set(listOf(channel))
    }
    compileKotlin {
        kotlinOptions.jvmTarget = "11"
    }
    test {
        useJUnitPlatform()
    }
    task("resolveDependencies") {
        doLast {
            rootProject.allprojects
                    .map { it.configurations }
                    .flatMap { it.filter { c -> c.isCanBeResolved } }
                    .forEach { it.resolve() }
        }
    }
}

fun prop(name: String, default: (()->String?)? = null) = extra.properties[name] as? String
        ?: default?.invoke() ?: error("Property `$name` is not defined in gradle.properties")
