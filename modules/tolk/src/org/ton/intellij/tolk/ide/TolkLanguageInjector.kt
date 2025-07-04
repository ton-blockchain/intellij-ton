package org.ton.intellij.tolk.ide

import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.TolkLanguage
import org.ton.intellij.tolk.psi.TolkArgument
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkStringLiteral

class TolkLanguageInjector : MultiHostInjector {

    private fun shouldInjectExpectType(element: TolkStringLiteral): Boolean {
        val argument = element.parent?.parent as? TolkArgument ?: return false
        val callExpr = argument.parent?.parent as? TolkCallExpression ?: return false
        val calleeRef = callExpr.expression as? TolkReferenceExpression ?: return false
        return calleeRef.referenceName == "__expect_type"
    }

    override fun getLanguagesToInject(
        registrar: MultiHostRegistrar,
        context: PsiElement
    ) {
        if (context !is TolkStringLiteral) return
        val rawStr = context.rawString ?: return

        if (shouldInjectExpectType(context)) {
            val contentRange = rawStr.textRangeInParent
            registrar.startInjecting(TolkLanguage)
                .addPlace("type __DUMMY = ", ";", context, contentRange)
                .doneInjecting()
            return
        }
    }

    override fun elementsToInjectIn(): List<Class<out PsiElement?>?> {
        return listOf(TolkStringLiteral::class.java)
    }
}
