package org.ton.intellij.tolk.debug

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import org.ton.intellij.tolk.TolkFileType

object TolkDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = TolkFileType

    @Suppress("OVERRIDE_DEPRECATION")
    override fun createDocument(
        project: Project,
        text: String,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode
    ): Document = EditorFactory.getInstance().createDocument(text)
}
