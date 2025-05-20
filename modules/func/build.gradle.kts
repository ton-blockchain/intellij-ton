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
    compileOnly(project(":asm"))
    compileOnly(project(":util"))
}

val generateFuncParser = task<GenerateParserTask>("generateFuncParser") {
    sourceFile.set(file("src/org/ton/intellij/func/parser/FuncParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/func/parser/FuncParser.java")
    pathToPsiRoot.set("/org/ton/intellij/func/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateFuncLexer = task<GenerateLexerTask>("generateFuncLexer") {
    val input = "src/org/ton/intellij/func/lexer/FuncLexer.flex"
    val output = "gen/org/ton/intellij/func/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateFuncParser, generateFuncLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateFuncParser, generateFuncLexer)
}
