package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.intellij.codeInsight.codeVision.settings.CodeVisionGroupSettingProvider
import org.ton.intellij.tolk.TolkBundle

/** Gives the gas group its own name and description in Editor | Inlay Hints | Code Vision. */
class TolkGasCodeVisionSettingsProvider : CodeVisionGroupSettingProvider {
    override val groupId: String get() = TolkGasCodeVisionProvider.ID
    override val groupName: String get() = TolkBundle.message("code.vision.gas.name")
    override val description: String get() = TolkBundle.message("code.vision.gas.description")
}
