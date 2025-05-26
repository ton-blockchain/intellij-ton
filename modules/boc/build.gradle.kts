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
    implementation("com.github.espritoxyz.ton-disassembler:tvm-opcodes:07ccc49")
    implementation("org.ton:ton-kotlin-tvm:0.3.1")
}
