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
        webstorm(version)
    }
    compileOnly(project(":util"))
}

val generateTasmParser = tasks.register<GenerateParserTask>("generateTasmParser") {
    sourceFile.set(file("src/org/ton/intellij/tasm/parser/TasmParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/tasm/parser/TasmParser.java")
    pathToPsiRoot.set("/org/ton/intellij/tasm/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateTasmLexer = tasks.register<GenerateLexerTask>("generateTasmLexer") {
    val input = "src/org/ton/intellij/tasm/lexer/TasmLexer.flex"
    val output = "gen/org/ton/intellij/tasm/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateTasmParser, generateTasmLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateTasmParser, generateTasmLexer)
}