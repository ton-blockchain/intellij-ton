package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.impl.rawReturnType
import org.ton.intellij.func.type.ty.FuncTyInt
import org.ton.intellij.func.type.ty.FuncTyUnit
import org.ton.intellij.util.parentOfType

object FuncReturnCompletionProvider : FuncCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = FuncCompletionPatterns.inBlock()

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val outerFunction = parameters.position.parentOfType<FuncFunction>() ?: return

        val returnTy = outerFunction.rawReturnType
        if (returnTy is FuncTyUnit) {
            result.addElement(
                LookupElementBuilder.create("return ();")
                    .bold()
            )
            return
        }

        result.addElement(
            LookupElementBuilder.create("return")
                .bold()
                .withTailText(" expr;", true)
                .withInsertHandler(
                    TemplateStringInsertHandler(
                        " \$expr$;", true, "expr" to ConstantNode("")
                    )
                )
        )

        if (returnTy is FuncTyInt) {
            result.addElement(
                LookupElementBuilder.create("return 0;")
                    .bold()
            )
        }
    }
}
