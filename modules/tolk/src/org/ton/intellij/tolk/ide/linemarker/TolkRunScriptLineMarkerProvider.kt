package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction

class TolkRunScriptLineMarkerProvider : RunLineMarkerContributor() {
    private val contextActions = ExecutorAction.getActions(0)

    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val parent = element.parent
        if (parent is TolkFunction && parent.name == "main") {
            return Info(AllIcons.RunConfigurations.TestState.Run, contextActions) { "Run Script" }
        }

        return null
    }
}
