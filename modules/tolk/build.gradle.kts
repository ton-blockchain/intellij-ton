
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
        webstorm(version)
        bundledPlugin("com.intellij.dev")
        testFramework(TestFrameworkType.Platform)
    }

    implementation(project(":asm"))
    implementation(project(":util"))
    testImplementation(project(":util"))
}

val generateTolkParser = tasks.register<GenerateParserTask>("generateTolkParser") {
    sourceFile.set(file("src/org/ton/intellij/tolk/parser/TolkParser.bnf"))
    targetRootOutputDir.set(file("gen"))
    pathToParser.set("/org/ton/intellij/tolk/parser/TolkParser.java")
    pathToPsiRoot.set("/org/ton/intellij/tolk/psi")
    purgeOldFiles.set(true)

    dependsOn(":util:composedJar")
}

val generateTolkLexer = tasks.register<GenerateLexerTask>("generateTolkLexer") {
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
