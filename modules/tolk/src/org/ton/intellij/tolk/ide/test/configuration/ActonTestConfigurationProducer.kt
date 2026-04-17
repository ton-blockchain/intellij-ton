package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.ton.intellij.acton.cli.ActonCommand
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.acton.runconfig.ActonCommandConfiguration
import org.ton.intellij.acton.runconfig.ActonCommandConfigurationType
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.isTestFunction
import org.ton.intellij.util.parentOfType

class ActonTestConfigurationProducer : LazyRunConfigurationProducer<ActonCommandConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory = ActonCommandConfigurationType.getInstance().factory

    override fun isConfigurationFromContext(
        configuration: ActonCommandConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        if (configuration.command != "test") return false
        val element = context.location?.psiElement ?: return false
        ActonToml.find(configuration.project) ?: return false

        if (element is PsiDirectory) {
            if (!element.containsTolkTests()) return false
            return configuration.testMode == ActonCommand.Test.TestMode.DIRECTORY &&
                configuration.testTarget == element.virtualFile.path
        }

        val containingFile = element.containingFile as? TolkFile ?: return false
        val function = element.parentOfType<TolkFunction>()

        if (function != null && function.isTestFunction()) {
            return configuration.testMode == ActonCommand.Test.TestMode.FUNCTION &&
                configuration.testTarget == containingFile.virtualFile.path &&
                configuration.testFunctionName == function.name
        }

        return configuration.testMode == ActonCommand.Test.TestMode.FILE &&
            configuration.testTarget == containingFile.virtualFile.path
    }

    override fun setupConfigurationFromContext(
        configuration: ActonCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = sourceElement.get() ?: return false
        val actonToml = ActonToml.find(configuration.project) ?: return false

        configuration.command = "test"
        configuration.workingDirectory = actonToml.workingDir

        if (element is PsiDirectory) {
            if (!element.containsTolkTests()) return false
            configuration.name = "Test ${element.name}"
            configuration.testMode = ActonCommand.Test.TestMode.DIRECTORY
            configuration.testTarget = element.virtualFile.path
            return true
        }

        val containingFile = element.containingFile as? TolkFile ?: return false
        if (!containingFile.isTestFile()) return false
        val function = element.parentOfType<TolkFunction>()

        if (function != null && function.isTestFunction()) {
            val functionName = function.name ?: return false
            configuration.name = "Test $functionName"
            configuration.testMode = ActonCommand.Test.TestMode.FUNCTION
            configuration.testTarget = containingFile.virtualFile.path
            configuration.testFunctionName = functionName
            return true
        }

        configuration.name = "Test ${containingFile.name}"
        configuration.testMode = ActonCommand.Test.TestMode.FILE
        configuration.testTarget = containingFile.virtualFile.path
        return true
    }

    private fun PsiDirectory.containsTolkTests(): Boolean {
        val fileIndex = ProjectFileIndex.getInstance(project)
        return virtualFile.containsTolkTests(fileIndex)
    }

    private fun com.intellij.openapi.vfs.VirtualFile.containsTolkTests(fileIndex: ProjectFileIndex): Boolean {
        if (!fileIndex.isInContent(this)) return false
        if (!isDirectory) {
            return fileType == TolkFileType && name.endsWith(".test.tolk")
        }
        return children.any { child -> child.containsTolkTests(fileIndex) }
    }
}
