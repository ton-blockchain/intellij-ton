package org.ton.intellij.tolk.toolchain

import com.intellij.openapi.components.*
import com.intellij.util.xmlb.XmlSerializerUtil

interface TolkKnownToolchains {
    var knownToolchains: Set<String>

    fun isKnown(homePath: String): Boolean

    fun add(toolchain: TolkToolchain)

    companion object : TolkKnownToolchains by TolkKnownToolchainsState
}

@State(
    name = "TolkToolchains",
    storages = [Storage("TolkToolchains.xml")]
)
@Service
class TolkKnownToolchainsState : PersistentStateComponent<TolkKnownToolchainsState?>, TolkKnownToolchains {
    override var knownToolchains: Set<String> = emptySet()

    override fun getState() = this

    override fun loadState(state: TolkKnownToolchainsState) = XmlSerializerUtil.copyBean(state, this)

    override fun isKnown(homePath: String): Boolean {
        return knownToolchains.contains(homePath)
    }

    override fun add(toolchain: TolkToolchain) {
        knownToolchains = knownToolchains + toolchain.homePath
    }

    companion object : TolkKnownToolchains {
        val INSTANCE get() = service<TolkKnownToolchainsState>()

        override var knownToolchains: Set<String>
            get() = INSTANCE.knownToolchains
            set(value) {
                INSTANCE.knownToolchains = value
            }

        override fun isKnown(homePath: String): Boolean  = INSTANCE.isKnown(homePath)

        override fun add(toolchain: TolkToolchain) = INSTANCE.add(toolchain)
    }
}
