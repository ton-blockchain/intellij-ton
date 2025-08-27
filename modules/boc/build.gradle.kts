plugins {
    id("org.jetbrains.intellij.platform.module")
    id("org.jetbrains.grammarkit")
}

dependencies {
    intellijPlatform {
        val version = providers.gradleProperty("platformVersion")
        webstorm(version)
    }
    implementation(project(":util"))
    implementation(project(":tasm"))
//    implementation("com.github.espritoxyz.ton-disassembler:tvm-opcodes:07ccc49") {
//        isTransitive = false
//    }
//    implementation("com.github.espritoxyz.ton-disassembler:tvm-disasm:07ccc49") {
//        isTransitive = false
//    }
//    implementation("org.ton:ton-kotlin-hashmap-tlb:0.3.1") {
//        exclude(module = "annotations")
//        exclude(module = "kotlin-stdlib")
//        exclude(module = "kotlin-stdlib-jdk7")
//        exclude(module = "kotlin-stdlib-jdk8")
//        exclude(module = "kotlin-stdlib-common")
//        exclude(module = "kotlinx-coroutines-core")
//        exclude(module = "kotlinx-coroutines-jdk8")
//        exclude(module = "kotlinx-serialization-core")
//        exclude(module = "kotlinx-serialization-json")
//        exclude(module = "slf4j-api")
//        exclude(module = "kotlin-reflect")
//        exclude(module = "kotlinx-crypto-md")
//        exclude(module = "kotlinx-crypto-cipher")
//        exclude(module = "kotlinx-crypto-aes")
////        exclude(module = "kotlinx-crypto-sha2")
////        exclude(module = "kotlinx-crypto-digest")
//        exclude(module = "curve25519-kotlin")
////        exclude(module = "ton-kotlin-tlb")
//    }
}
