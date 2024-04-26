package org.ton.intellij.tact.linemarkers

import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tact.TactIcons
import org.ton.intellij.tact.psi.*
import org.ton.intellij.util.ancestorStrict
import javax.swing.Icon

class TactImplLineMarkerProvider : RelatedItemLineMarkerProvider() {
    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        if (element.elementType != TactElementTypes.IDENTIFIER) return
        val abstractable = element.parent as? TactAbstractable ?: return
        val superItem = abstractable.superItem ?: return
        val superTrait = superItem.ancestorStrict<TactTrait>() ?: return

        val icon: Icon
        val action: String
        val name = superTrait.name ?: return
        val type = when (superItem) {
            is TactFunction -> "method"
            is TactConstant -> "constant"
            else -> return
        }

        if (superItem.isAbstract) {
            icon = TactIcons.OVERRIDING_METHOD
            action = "Overrides"
        } else {
            icon = TactIcons.IMPLEMENTING_METHOD
            action = "Implements"
        }

        val marker = NavigationGutterIconBuilder
            .create(icon)
            .setTarget(superItem)
            .setTooltipText("$action $type in `$name`")
            .createLineMarkerInfo(element)

        result.add(marker)
    }
}
