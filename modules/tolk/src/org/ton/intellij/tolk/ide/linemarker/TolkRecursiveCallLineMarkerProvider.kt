package org.ton.intellij.tolk.ide.linemarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.FunctionUtil
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression

class TolkRecursiveCallLineMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>?>) {
        val lines = mutableSetOf<Int>()
        for (element in elements) {
            if (!element.isValid || element !is TolkCallExpression) continue

            val function = resolveCalledFunction(element) ?: continue
            if (!function.isValid) continue

            if (isRecursiveCall(element, function)) {
                val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile) ?: continue
                val lineNumber = document.getLineNumber(element.textOffset)
                if (!lines.contains(lineNumber)) {
                    result.add(RecursiveMethodCallMarkerInfo(element.argumentList.lparen))
                }

                lines.add(lineNumber)
            }
        }
    }

    fun resolveCalledFunction(call: TolkCallExpression): TolkFunction? {
        if (!call.isValid) return null
        val expr = call.expression
        if (expr is TolkReferenceExpression) {
            // foo()
            return expr.reference?.resolve() as? TolkFunction
        }
        if (expr is TolkDotExpression) {
            // int.foo()
            // 10.foo()
            return expr.fieldLookup?.reference?.resolve() as? TolkFunction
        }
        return null
    }

    private class RecursiveMethodCallMarkerInfo(methodCall: PsiElement) : LineMarkerInfo<PsiElement?>(
        methodCall,
        methodCall.textRange,
        AllIcons.Gutter.RecursiveMethod,
        FunctionUtil.constant("Recursive call"),
        null,
        GutterIconRenderer.Alignment.RIGHT,
        { "Recursive call" }
    )

    private fun isRecursiveCall(element: PsiElement, function: TolkFunction): Boolean {
        return element.parentOfType<TolkFunction>() == function
    }
}
