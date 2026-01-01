package org.ton.intellij.tolk.ide.intentions

import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThrowableRunnable
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.TolkStruct
import java.util.zip.CRC32

class TolkAddPackPrefixIntention : PsiElementBaseIntentionAction() {
    override fun getFamilyName(): String = TolkBundle.message("intention.add.pack.prefix.family.name")
    override fun getText(): String = TolkBundle.message("intention.add.pack.prefix.text")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val struct = findStruct(element) ?: return false
        return struct.structConstructorTag == null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val struct = findStruct(element) ?: return
        val structName = struct.name ?: return

        val crc32 = CRC32()
        crc32.update(structName.toByteArray(Charsets.UTF_8))
        val crcValue = crc32.value.toInt()

        val hexValue = "0x" + crcValue.toUInt().toString(16).padStart(8, '0')

        WriteCommandAction.writeCommandAction(project)
            .withName("Add pack prefix to struct $structName")
            .run(ThrowableRunnable {
                val structKeyword = struct.structKeyword
                val document = struct.containingFile.viewProvider.document ?: return@ThrowableRunnable

                val insertOffset = structKeyword.textRange.endOffset
                document.insertString(insertOffset, " ($hexValue)")
            })
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    private fun findStruct(element: PsiElement): TolkStruct? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is TolkStruct) {
                return current
            }
            current = current.parent
        }
        return null
    }
}
