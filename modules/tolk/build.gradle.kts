
import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdeaCommunity(version)
        bundledModule("com.intellij.dev")
    }
    compileOnly(project(":modules:asm"))
    compileOnly(project(":modules:util"))
}

val generateTolkParser = task<GenerateParserTask>("generateTolkParser") {
    sourceFile.set(file("src/org/ton/intellij/tolk/parser/TolkParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/tolk/parser/TolkParser.java")
    pathToPsiRoot.set("/org/ton/intellij/tolk/psi")
    purgeOldFiles.set(true)

    dependsOn(":modules:util:composedJar")
}

val generateTolkLexer = task<GenerateLexerTask>("generateTolkLexer") {
    val input = "src/org/ton/intellij/tolk/lexer/TolkLexer.flex"
    val output = "gen/org/ton/intellij/tolk/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateTolkParser, generateTolkLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateTolkParser, generateTolkLexer)
}