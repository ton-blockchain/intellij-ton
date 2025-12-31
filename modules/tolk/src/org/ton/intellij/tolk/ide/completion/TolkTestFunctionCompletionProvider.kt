package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PatternCondition
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiWhiteSpace
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.util.prevLeaf

object TolkTestFunctionCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        PlatformPatterns.psiElement()
            .withSuperParent(2, TolkFile::class.java)
            .with(object : PatternCondition<PsiElement>("atLineStart") {
                override fun accepts(t: PsiElement, context: ProcessingContext?): Boolean {
                    val file = t.containingFile.originalFile
                    if (file.virtualFile?.name?.endsWith(".test.tolk") == false) return false

                    // accept
                    // <caret>
                    // but not
                    // fun <caret>
                    return t.prevSibling == null && t.prevLeaf is PsiWhiteSpace
                }
            })

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        result.addElement(
            LookupElementBuilder.create("get fun test")
                .withIcon(AllIcons.Actions.RealIntentionBulb)
                .withTailText(" Generates test function", true)
                .withInsertHandler { context, item ->
                    // We need to remove the automatically inserted `get fun test`
                    // and insert the correct text instead
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + "get fun test".length)
                    TemplateStringInsertHandler(
                        "get fun `test-\$name$`() {\n\$END$\n}", true,
                        "name" to ConstantNode(""),
                    ).handleInsert(context, item)
                }
        )
    }
}
