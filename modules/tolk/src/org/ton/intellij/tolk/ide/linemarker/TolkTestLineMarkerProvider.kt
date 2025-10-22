package org.ton.intellij.tolk.ide.linemarker

import com.intellij.execution.TestStateStorage
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.execution.testframework.TestIconMapper
import com.intellij.execution.testframework.sm.runner.states.TestStateInfo
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import org.ton.intellij.tolk.ide.test.configuration.TolkTestLocator
import org.ton.intellij.tolk.psi.TolkElementTypes
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.impl.isGetMethod

class TolkTestLineMarkerProvider : RunLineMarkerContributor() {
    private val contextActions = ExecutorAction.getActions(0)

    override fun getInfo(element: PsiElement): Info? {
        if (element.elementType != TolkElementTypes.IDENTIFIER) return null

        val parent = element.parent
        if (parent is TolkFunction) {
            if (!parent.isTestFunction()) {
                return null
            }

            val magnitude = getTestState(parent)
                ?.let { TestIconMapper.getMagnitude(it.magnitude) }

            val icon = when (magnitude) {
                TestStateInfo.Magnitude.PASSED_INDEX,
                TestStateInfo.Magnitude.COMPLETE_INDEX -> AllIcons.RunConfigurations.TestState.Green2

                TestStateInfo.Magnitude.ERROR_INDEX,
                TestStateInfo.Magnitude.FAILED_INDEX -> AllIcons.RunConfigurations.TestState.Red2

                else -> AllIcons.RunConfigurations.TestState.Run_run
            }

            return Info(icon, contextActions) { "Run Test" }
        }

        return null
    }

    private fun TolkFunction.isTestFunction(): Boolean {
        if (!isGetMethod) return false
        val name = name ?: return false
        return name.startsWith("test_") || name.startsWith("test-")
    }

    companion object {
        fun getTestState(element: TolkNamedElement): TestStateStorage.Record? {
            val fullUrl = TolkTestLocator.getTestUrl(element)
            val storage = TestStateStorage.getInstance(element.project)
            return storage.getState(fullUrl)
        }
    }
}
