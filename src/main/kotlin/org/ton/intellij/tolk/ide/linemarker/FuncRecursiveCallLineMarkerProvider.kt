package org.ton.intellij.tolk.ide.linemarker

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.util.ancestorStrict
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
    }

    private val TolkReferenceExpression?.isRecursive: Boolean
        get() {
            val reference = this?.reference ?: return false
            val def = reference.resolve()
            return def != null && reference.element.ancestorStrict<TolkFunction>() == def
        }
}
