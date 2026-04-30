package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.testframework.AbstractTestProxy
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
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
        val testContext = context.testContext() ?: return false
        testContext.findActonToml(configuration.project) ?: return false

        return configuration.testMode == testContext.mode &&
            configuration.testTarget == testContext.target &&
            configuration.testFunctionName == testContext.functionName
    }

    override fun setupConfigurationFromContext(
        configuration: ActonCommandConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val testContext = context.testContext() ?: return false
        val actonToml = testContext.findActonToml(configuration.project) ?: return false
        testContext.sourceElement?.let(sourceElement::set)

        configuration.command = "test"
        configuration.workingDirectory = actonToml.workingDir
        configuration.name = testContext.displayName
        configuration.testMode = testContext.mode
        configuration.testTarget = testContext.target
        configuration.testFunctionName = testContext.functionName
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

    private fun ConfigurationContext.testContext(): TestContext? = treeTestContext() ?: psiTestContext()

    private fun ConfigurationContext.treeTestContext(): TestContext? {
        val selectedTest = AbstractTestProxy.DATA_KEY.getData(dataContext) ?: return null
        val selection = TolkTestTreeSelection.resolveSelection(selectedTest) ?: return null
        val sourceElement = TolkTestLocator.findLocation(project, selectedTest.locationUrl)?.psiElement
        return TestContext(
            selection.mode,
            selection.target,
            selection.functionName,
            selection.displayName,
            sourceElement,
        )
    }

    private fun ConfigurationContext.psiTestContext(): TestContext? {
        val element =
            LangDataKeys.PSI_ELEMENT_ARRAY.getData(dataContext)?.singleOrNull() ?: location?.psiElement ?: return null

        if (element is PsiDirectory) {
            if (!element.containsTolkTests()) return null
            return TestContext(
                mode = ActonCommand.Test.TestMode.DIRECTORY,
                target = element.virtualFile.path,
                functionName = "",
                displayName = "Test ${element.name}",
                sourceElement = element,
            )
        }

        val containingFile = element.containingFile as? TolkFile ?: return null
        if (!containingFile.isTestFile()) return null
        val function = element.parentOfType<TolkFunction>()

        if (function != null && function.isTestFunction()) {
            val functionName = function.name ?: return null
            return TestContext(
                mode = ActonCommand.Test.TestMode.FUNCTION,
                target = containingFile.virtualFile.path,
                functionName = functionName,
                displayName = "Test $functionName",
                sourceElement = function,
            )
        }

        return TestContext(
            mode = ActonCommand.Test.TestMode.FILE,
            target = containingFile.virtualFile.path,
            functionName = "",
            displayName = "Test ${containingFile.name}",
            sourceElement = containingFile,
        )
    }

    private data class TestContext(
        val mode: ActonCommand.Test.TestMode,
        val target: String,
        val functionName: String,
        val displayName: String,
        val sourceElement: PsiElement?,
    )

    private fun TestContext.findActonToml(project: Project): ActonToml? {
        val sourceVirtualFile = when (val element = sourceElement) {
            is PsiDirectory -> element.virtualFile
            else -> element?.containingFile?.originalFile?.virtualFile
        }
        return if (sourceVirtualFile != null) {
            ActonToml.find(project, sourceVirtualFile)
        } else {
            ActonToml.find(project)
        }
    }
}
