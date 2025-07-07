
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

val generateAsmParser = tasks.register<GenerateParserTask>("generateAsmParser") {
    sourceFile.set(file("src/org/ton/intellij/asm/parser/AsmParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/asm/parser/AsmParser.java")
    pathToPsiRoot.set("/org/ton/intellij/asm/psi")
    purgeOldFiles.set(true)
    dependsOn(":util:composedJar")
}

val generateAsmLexer = tasks.register<GenerateLexerTask>("generateAsmLexer") {
    sourceFile.set(file("src/org/ton/intellij/asm/lexer/AsmLexer.flex"))
    targetOutputDir.set(file("gen/org/ton/intellij/asm/lexer"))
    purgeOldFiles.set(true)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateAsmParser, generateAsmLexer)
}
