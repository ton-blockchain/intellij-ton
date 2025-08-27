package org.ton.intellij.tlb.inspection.fix

import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.ton.intellij.tlb.TlbBundle
import org.ton.intellij.tlb.settings.TlbSettingsConfigurable

class TlbConfigureGlobalBlockTlbFix : LocalQuickFix {
    override fun getName(): String = TlbBundle.message("tlb.inspection.configure.global.block.tlb.fix.name")

    override fun getFamilyName(): String = name

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        invokeLater { ShowSettingsUtil.getInstance().showSettingsDialog(project, TlbSettingsConfigurable::class.java) }
    }

    override fun availableInBatchMode(): Boolean = false
    override fun startInWriteAction(): Boolean = false
    override fun generatePreview(project: Project, previewDescriptor: ProblemDescriptor): IntentionPreviewInfo = IntentionPreviewInfo.EMPTY
}
