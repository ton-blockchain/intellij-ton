package org.ton.intellij.tolk.codeInsight.hint

import com.intellij.codeInsight.hints.declarative.InlayHintsCollector
import com.intellij.codeInsight.hints.declarative.InlayHintsProvider
import com.intellij.codeInsight.hints.declarative.InlayTreeSink
import com.intellij.codeInsight.hints.declarative.SharedBypassCollector
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkFile

abstract class AbstractTolkInlayHintProvider : InlayHintsProvider {
    final override fun createCollector(
        file: PsiFile,
        editor: Editor
    ): InlayHintsCollector? {
        val project = editor.project ?: file.project
        if (project.isDefault || file !is TolkFile) return null

        return object : SharedBypassCollector {
            override fun collectFromElement(
                element: PsiElement,
                sink: InlayTreeSink
            ) {
                this@AbstractTolkInlayHintProvider.collectFromElement(element, sink)
            }
        }
    }

    protected abstract fun collectFromElement(element: PsiElement, sink: InlayTreeSink)
}