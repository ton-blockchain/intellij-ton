package org.ton.intellij.asm.ide.documentation

import com.intellij.lang.documentation.AbstractDocumentationProvider
import com.intellij.lang.documentation.DocumentationMarkup
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.intellij.markdown.flavours.MarkdownFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.ton.intellij.asm.ide.documentation.AsmDocumentationUtils.asAsmInstruction
import org.ton.intellij.asm.ide.documentation.AsmDocumentationUtils.asComment
import org.ton.intellij.asm.ide.documentation.AsmDocumentationUtils.colorize
import org.ton.intellij.asm.psi.AsmInstruction
import org.ton.intellij.util.asm.findInstruction
import org.ton.intellij.util.asm.getStackPresentation

class AsmDocumentationProvider : AbstractDocumentationProvider() {
    override fun generateDoc(element: PsiElement?, originalElement: PsiElement?) = when (element) {
        is AsmInstruction -> element.generateDoc()
        else              -> null
    }

    override fun getCustomDocumentationElement(editor: Editor, file: PsiFile, contextElement: PsiElement?, targetOffset: Int): PsiElement? {
        val parent = contextElement?.parent
        if (parent is AsmInstruction) {
            return parent
        }
        return super.getCustomDocumentationElement(editor, file, contextElement, targetOffset)
    }
}

private class FiftDocMarkdownFlavourDescriptor(
    private val gfm: MarkdownFlavourDescriptor = GFMFlavourDescriptor(useSafeLinks = false, absolutizeAnchorLinks = true),
) : MarkdownFlavourDescriptor by gfm

fun documentationAsHtml(documentationText: String): String {
    val flavour = FiftDocMarkdownFlavourDescriptor()
    val root = MarkdownParser(flavour).buildMarkdownTreeFromString(documentationText)
    return HtmlGenerator(documentationText, root, flavour).generateHtml()
}

fun AsmInstruction.generateDoc(): String {
    val info = findInstruction(text, listOf()) ?: return "unknown instruction"

    val stackInfo = "<li>Stack (top is on the right): <code>${getStackPresentation(info.doc.stack)}</code></li>"

    val gas = info.doc.gas.ifEmpty { "unknown" }

    val actualInstructionDescription = mutableListOf(
        wrapDefinition(colorize(info.mnemonic, asAsmInstruction)),
        DocumentationMarkup.CONTENT_START,
        "<ul>",
        stackInfo,
        "<li>Gas: <code>$gas</code></li>",
        "</ul>",
        documentationAsHtml(info.doc.description),
        DocumentationMarkup.CONTENT_END,
    )

    val alias = info.aliasInfo
    if (alias != null) {
        val operandsStr = formatOperands(alias.operands) + " "
        val aliasInfoDescription = " (alias of $operandsStr${alias.aliasOf})"

        val aliasStackInfo = alias.docStack?.let {
            "<li>Stack (top is on the right): <code>${getStackPresentation(it)}</code></li>"
        } ?: ""

        val withAliasDescription = listOf(
            wrapDefinition(colorize(alias.mnemonic, asAsmInstruction) + colorize(aliasInfoDescription, asComment)),
            DocumentationMarkup.CONTENT_START,
            "<ul>",
            aliasStackInfo,
            "</ul>",
            documentationAsHtml(alias.description ?: ""),
            DocumentationMarkup.CONTENT_END,
            "<hr>",
            "Aliased instruction info:",
            "<br>",
        ) + actualInstructionDescription
        return withAliasDescription.joinToString("\n")
    }

    return actualInstructionDescription.joinToString("\n")
}

fun formatOperands(operands: Map<String, Any?>): String {
    return operands.values.joinToString(" ") { it.toString() }
}

fun wrapDefinition(content: String): String = DocumentationMarkup.DEFINITION_START + content + DocumentationMarkup.DEFINITION_END
