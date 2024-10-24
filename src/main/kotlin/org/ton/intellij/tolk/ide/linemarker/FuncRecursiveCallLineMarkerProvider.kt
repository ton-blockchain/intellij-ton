package org.ton.intellij.tolk.ide.linemarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkApplyExpression
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkSpecialApplyExpression
import org.ton.intellij.util.ancestorStrict
import org.ton.intellij.util.document
import javax.swing.Icon

class TolkRecursiveCallLineMarkerProvider : LineMarkerProviderDescriptor() {
    override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? = null

    override fun getIcon(): Icon = TolkIcons.RECURSIVE_CALL

    override fun getName(): String = "Recursive call"

    override fun collectSlowLineMarkers(
        elements: MutableList<out PsiElement>,
        result: MutableCollection<in LineMarkerInfo<*>>,
    ) {
        return // TODO: fix
        val lines = HashSet<Int>()  // To prevent several markers on one line

        for (element in elements) {
            if (element !is TolkReferenceExpression) continue
            val parent = element.parent
            val isRecursive = when {
//                parent is TolkApplyExpression && (parent.left as? TolkReferenceExpression).isRecursive -> true
                parent is TolkSpecialApplyExpression && (parent.left as? TolkReferenceExpression).isRecursive -> true
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

    private val TolkReferenceExpression?.isRecursive: Boolean
        get() {
            val reference = this?.reference ?: return false
            val def = reference.resolve()
            return def != null && reference.element.ancestorStrict<TolkFunction>() == def
        }
}
