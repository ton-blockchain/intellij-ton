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
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression

class TolkRecursiveCallMarkerProvider : LineMarkerProvider {
    override fun getLineMarkerInfo(element: PsiElement) = null

    override fun collectSlowLineMarkers(elements: List<PsiElement>, result: MutableCollection<in LineMarkerInfo<*>?>) {
        val lines = mutableSetOf<Int>()
        for (element in elements) {
            if (element !is TolkCallExpression) continue

            val resolve = (element.expression as? TolkReferenceExpression)?.reference?.resolve() as? TolkFunction ?: continue

            if (isRecursiveCall(element, resolve)) {
                val document = PsiDocumentManager.getInstance(element.project).getDocument(element.containingFile) ?: continue
                val lineNumber = document.getLineNumber(element.textOffset)
                if (!lines.contains(lineNumber)) {
                    result.add(RecursiveMethodCallMarkerInfo(element.expression))
                }

                lines.add(lineNumber)
            }
        }
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
