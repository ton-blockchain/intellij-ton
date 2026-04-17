package org.ton.intellij.tolk.debug

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.XExpression
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.EvaluationMode
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import org.ton.intellij.tolk.TolkFileType

object TolkDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = TolkFileType

    override fun createDocument(
        project: Project,
        expression: XExpression,
        sourcePosition: XSourcePosition?,
        mode: EvaluationMode,
    ): Document = EditorFactory.getInstance().createDocument(expression.expression)
}
