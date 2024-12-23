import org.jetbrains.grammarkit.tasks.GenerateLexerTask
import org.jetbrains.grammarkit.tasks.GenerateParserTask
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        intellijIdeaCommunity(version)
        testFramework(TestFrameworkType.Platform)
    }
    compileOnly(project(":util"))
    testImplementation(kotlin("test"))
}

val generateTlbParser = task<GenerateParserTask>("generateTlbParser") {
    sourceFile.set(file("src/org/ton/intellij/tlb/parser/TlbParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/tlb/parser/TlbParser.java")
    pathToPsiRoot.set("/org/ton/intellij/tlb/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateTlbLexer = task<GenerateLexerTask>("generateTlbLexer") {
    val input = "src/org/ton/intellij/tlb/lexer/TlbLexer.flex"
    val output = "gen/org/ton/intellij/tlb/lexer"
    sourceFile.set(file(input))
    targetOutputDir.set(file(output))
    purgeOldFiles.set(true)
}

tasks.test {
    useJUnit()
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(generateTlbParser, generateTlbLexer)
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn(generateTlbParser, generateTlbLexer)
}
