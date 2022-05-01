package org.ton.intellij.spelling

import com.intellij.spellchecker.BundledDictionaryProvider

class TonDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> = arrayOf("/org.ton.intellij/spelling/ton.dic")
}