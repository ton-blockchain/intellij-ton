package org.ton.intellij.tolk.refactor

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tolk.psi.TolkExpression
import org.ton.intellij.tolk.psi.TolkVar

data class TolkIntroduceOperation(
    var project: Project,
    var editor: Editor,
    var file: PsiFile,
    var expression: TolkExpression? = null,
    var occurrences: List<PsiElement?> = emptyList(),
    var name: String = "",
    var variable: TolkVar? = null,
    var replaceAll: Boolean = false,
)
