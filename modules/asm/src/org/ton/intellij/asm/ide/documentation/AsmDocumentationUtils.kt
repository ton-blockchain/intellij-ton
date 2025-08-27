package org.ton.intellij.asm.ide.documentation

import org.ton.intellij.asm.ide.AsmColor
import org.ton.intellij.util.doc.DocumentationUtils

object AsmDocumentationUtils : DocumentationUtils() {
    val asAsmInstruction = loadKey(AsmColor.INSTRUCTION.textAttributesKey)
    val asComment = loadKey(AsmColor.COMMENT.textAttributesKey)
}
