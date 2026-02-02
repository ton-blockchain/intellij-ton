package org.ton.intellij.acton.ide

import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.openapi.project.Project

class ActonHighlightingPassFactoryRegistrar : TextEditorHighlightingPassFactoryRegistrar {
    override fun registerHighlightingPassFactory(registrar: TextEditorHighlightingPassRegistrar, project: Project) {
        ActonExternalLinterPassFactory(project, registrar)
    }
}
