package org.ton.intellij.func.template

import com.intellij.ide.fileTemplates.DefaultTemplatePropertiesProvider
import com.intellij.psi.PsiDirectory
import org.ton.intellij.func.util.FuncStdlibPathProvider
import java.util.*

class FuncFileTemplateProvider : DefaultTemplatePropertiesProvider {
    override fun fillProperties(directory: PsiDirectory, props: Properties) {
        val project = directory.project
        val stdlibPath = FuncStdlibPathProvider.getStdlibPath(project)
        props.setProperty("STDLIB_PATH", stdlibPath)
    }
}
