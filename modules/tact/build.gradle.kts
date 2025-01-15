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
    }
    compileOnly(project(":util"))
}

val generateTactParser = task<GenerateParserTask>("generateTactParser") {
    sourceFile.set(file("src/org/ton/intellij/tact/parser/TactParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/tact/parser/TactParser.java")
    pathToPsiRoot.set("/org/ton/intellij/tact/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateTactLexer = task<GenerateLexerTask>("generateTactLexer") {
    val input = "src/org/ton/intellij/tact/lexer/TactLexer.flex"
    val output = "gen/org/ton/intellij/tact/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateTactParser, generateTactLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateTactParser, generateTactLexer)
}
