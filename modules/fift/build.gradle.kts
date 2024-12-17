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

val generateFiftParser = task<GenerateParserTask>("generateFiftParser") {
    sourceFile.set(file("src/org/ton/intellij/fift/parser/FiftParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/fift/parser/FiftParser.java")
    pathToPsiRoot.set("/org/ton/intellij/fift/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateFiftLexer = task<GenerateLexerTask>("generateFiftLexer") {
    val input = "src/org/ton/intellij/fift/lexer/FiftLexer.flex"
    val output = "gen/org/ton/intellij/fift/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateFiftParser, generateFiftLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateFiftParser, generateFiftLexer)
}
