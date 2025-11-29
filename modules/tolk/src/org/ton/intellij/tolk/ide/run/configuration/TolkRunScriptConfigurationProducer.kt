package org.ton.intellij.tolk.ide.run.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.util.parentOfType

class TolkRunScriptConfigurationProducer : LazyRunConfigurationProducer<TolkRunScriptConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(TolkRunScriptConfigurationType.ID)!!
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: TolkRunScriptConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = getSourceElement(context.location?.psiElement) ?: return false
        if (element is PsiDirectory) {
            return false
        }

        val containingFile = element.containingFile ?: return false
        if (containingFile !is TolkFile) {
            return false
        }

        val parent = element.parentOfType<TolkFunction>()

        if (parent is TolkFunction && parent.name == "main") {
            return configuration.filename == containingFile.virtualFile.path
        }

        return false
    }

    override fun setupConfigurationFromContext(
        configuration: TolkRunScriptConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = getSourceElement(sourceElement.get()) ?: return false

        if (element is PsiDirectory) {
            return false
        }

        val parent = element.parentOfType<TolkFunction>()

        if (parent is TolkFunction && parent.name == "main") {
            val path = element.containingFile.virtualFile
            val name = path.name
            configuration.name = "Tolk Script $name"
            configuration.filename = path.path

            return true
        }

        return false
    }

    private fun getSourceElement(sourceElement: PsiElement?): PsiElement? {
        if (sourceElement is TolkFunction) {
            return sourceElement.nameIdentifier
        }
        return sourceElement
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext) =
        other.configuration !is TolkRunScriptConfiguration
}
