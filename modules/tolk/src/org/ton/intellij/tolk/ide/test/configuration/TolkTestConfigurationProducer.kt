package org.ton.intellij.tolk.ide.test.configuration

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.ton.intellij.tolk.TolkFileType
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.impl.isTestFunction
import org.ton.intellij.util.parentOfType

class TolkTestConfigurationProducer : LazyRunConfigurationProducer<TolkTestConfiguration>() {
    override fun getConfigurationFactory(): ConfigurationFactory =
        ConfigurationTypeUtil.findConfigurationType(TolkTestConfigurationType.ID)!!
            .configurationFactories[0]

    override fun isConfigurationFromContext(
        configuration: TolkTestConfiguration,
        context: ConfigurationContext,
    ): Boolean {
        val element = getSourceElement(context.location?.psiElement) ?: return false
        if (element is PsiDirectory) {
            return configuration.scope == TolkTestScope.Directory &&
                    configuration.directory == element.virtualFile.path
        }

        val containingFile = element.containingFile ?: return false
        if (containingFile !is TolkFile) {
            return false
        }

        if (!TestSourcesFilter.isTestSources(containingFile.virtualFile, element.project)) {
            return false
        }

        val parent = element.parentOfType<TolkFunction>()

        if (parent is TolkFunction && parent.isTestFunction()) {
            val functionName = parent.name
            return configuration.scope == TolkTestScope.Function &&
                    configuration.filename == containingFile.virtualFile.path &&
                    configuration.pattern == functionName
        }

        return configuration.scope == TolkTestScope.File &&
                configuration.directory == containingFile.virtualFile.parent.path &&
                configuration.filename == containingFile.virtualFile.path
    }

    override fun setupConfigurationFromContext(
        configuration: TolkTestConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>,
    ): Boolean {
        val element = getSourceElement(sourceElement.get()) ?: return false

        if (element is PsiDirectory) {
            val testFiles = findTestFilesRecursively(element, element.project)
            if (testFiles.isEmpty()) {
                return false
            }

            configuration.scope = TolkTestScope.Directory
            configuration.name = "Tolk Test ${element.name}"
            configuration.directory = element.virtualFile.path
            configuration.filename = testFiles.firstOrNull()?.virtualFile?.path ?: ""

            return true
        }

        val parent = element.parentOfType<TolkFunction>()

        if (parent is TolkFunction && parent.isTestFunction()) {
            val functionName = parent.name ?: return false
            configuration.scope = TolkTestScope.Function
            configuration.name = "Tolk Test $functionName"
            configuration.directory = element.containingFile.virtualFile.parent.path
            configuration.filename = element.containingFile.virtualFile.path
            configuration.pattern = functionName

            return true
        }

        if (element.containingFile !is TolkFile) {
            return false
        }

        val file = element.containingFile.virtualFile.name
        configuration.scope = TolkTestScope.File
        configuration.name = "Tolk Test $file"
        configuration.directory = element.containingFile.virtualFile.parent.path
        configuration.filename = element.containingFile.virtualFile.path

        return true
    }

    private fun findTestFilesRecursively(
        directory: PsiDirectory,
        project: com.intellij.openapi.project.Project,
        currentDepth: Int = 0,
        maxDepth: Int = 10,
    ): List<TolkFile> {
        if (currentDepth > maxDepth) return emptyList()

        val result = mutableListOf<TolkFile>()

        val allScope = GlobalSearchScope.allScope(project)
        val tolkFiles = directory.getFiles(GlobalSearchScope.getScopeRestrictedByFileTypes(allScope, TolkFileType))
        result.addAll(tolkFiles.filterIsInstance<TolkFile>().filter { it.isTestFile() })

        if (currentDepth < maxDepth) {
            directory.subdirectories.forEach { subDir ->
                result.addAll(findTestFilesRecursively(subDir, project, currentDepth + 1, maxDepth))
            }
        }

        return result
    }

    private fun getSourceElement(sourceElement: PsiElement?): PsiElement? {
        if (sourceElement is TolkFunction) {
            return sourceElement.nameIdentifier
        }
        return sourceElement
    }

    override fun shouldReplace(self: ConfigurationFromContext, other: ConfigurationFromContext) =
        other.configuration !is TolkTestConfiguration
}
