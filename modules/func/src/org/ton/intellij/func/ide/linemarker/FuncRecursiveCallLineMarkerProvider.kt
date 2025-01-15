package org.ton.intellij.func.ide.linemarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.FuncApplyExpression
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.FuncSpecialApplyExpression
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.document
import javax.swing.Icon

class FuncRecursiveCallLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun getIcon(): Icon = FuncIcons.RECURSIVE_CALL

    override fun getName(): String = "Recursive call"

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>,
    ) {
        val lines = HashSet<Int>()  // To prevent several markers on one line

        for (element in elements) {
            if (element !is FuncReferenceExpression) continue
            val parent = element.parent
            val isRecursive = when {
                parent is FuncApplyExpression && (parent.left as? FuncReferenceExpression).isRecursive -> true
                parent is FuncSpecialApplyExpression && (parent.left as? FuncReferenceExpression).isRecursive -> true
                else -> false
            }
            if (!isRecursive) continue
            val doc = element.containingFile.document ?: continue
            val lineNumber = doc.getLineNumber(element.textOffset)
            if (lineNumber !in lines) {
                lines.add(lineNumber)
                result.add(
                    LineMarkerInfo(
                        element.identifier,
                        element.identifier.textRange,
                        icon,
                        { name },
                        null,
                        GutterIconRenderer.Alignment.RIGHT,
                        { name }
                    )
                )
            }
        }
    }

    private val FuncReferenceExpression?.isRecursive: Boolean
        get() {
            val reference = this?.reference ?: return false
            val def = reference.resolve()
            return def != null && reference.element.ancestorStrict<FuncFunction>() == def
        }
}
