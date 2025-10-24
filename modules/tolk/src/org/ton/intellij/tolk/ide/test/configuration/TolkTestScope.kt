package org.ton.intellij.tolk.ide.test.configuration

enum class TolkTestScope {
    Directory,
    File,
    Function;

    companion object {
        fun from(s: String): TolkTestScope = TolkTestScope.entries.find { it.name == s } ?: Directory
    }
}
