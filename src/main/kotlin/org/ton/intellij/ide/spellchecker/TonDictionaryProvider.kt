package org.ton.intellij.ide.spellchecker

import com.intellij.spellchecker.BundledDictionaryProvider

class TonDictionaryProvider : BundledDictionaryProvider {
    override fun getBundledDictionaries(): Array<String> = arrayOf("/ton.dic")
}