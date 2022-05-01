package org.ton.intellij.toncli

object ToncliConstants {

    const val MANIFEST_FILE = "toncli.yml"
    const val LOCK_FILE = "toncli.lock"

    object ProjectLayout {
        val sources = listOf("src", "func", "examples")
        val tests = listOf("tests")
    }
}