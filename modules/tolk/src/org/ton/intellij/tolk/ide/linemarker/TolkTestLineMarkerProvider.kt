package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.TestStateStorage
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.ide.test.TolkTestStateMagnitude
import org.ton.intellij.tolk.ide.test.configuration.TolkTestLocator
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.impl.isTestFunction

class TolkTestLineMarkerProvider : RunLineMarkerContributor() {
    private val contextActions = ExecutorAction.getActions(0)

    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val parent = element.parent
        if (parent is TolkFunction && parent.isTestFunction()) {
            val state = getTestState(parent)

            val icon = when {
                state?.let(TolkTestStateMagnitude::isPassed) == true ->
                    AllIcons.RunConfigurations.TestState.Green2

                state?.let(TolkTestStateMagnitude::isFailure) == true ->
                    AllIcons.RunConfigurations.TestState.Red2

                else -> AllIcons.RunConfigurations.TestState.Run
            }

            return Info(icon, contextActions) { "Run Test" }
        }

        return null
    }

    companion object {
        fun getTestState(element: TolkNamedElement): TestStateStorage.Record? {
            val fullUrl = TolkTestLocator.getTestUrl(element)
            val storage = TestStateStorage.getInstance(element.project)
            return storage.getState(fullUrl)
        }
    }
}
