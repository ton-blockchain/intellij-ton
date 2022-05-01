package org.ton.intellij.toncli

import kotlinx.serialization.Serializable

@Serializable
data class ToncliConfig(
        val modules: List<Module>
) {

    @Serializable
    data class Module(
            val name: String,
            val sources: List<String> = emptyList(),
            val tests: List<String> = emptyList()
    )
}