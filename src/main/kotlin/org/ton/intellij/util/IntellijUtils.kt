package org.ton.intellij.util

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

val org.intellij.markdown.ast.ASTNode.textRange: TextRange
    get() = TextRange(startOffset, endOffset)

inline fun <reified T : PsiElement> PsiElement.ancestorStrict(): T? =
    PsiTreeUtil.getParentOfType(this, T::class.java, /* strict */ true)

inline fun <reified T : PsiElement> PsiElement.childOfType(): T? =
    PsiTreeUtil.getChildOfType(this, T::class.java)

inline fun <reified T : PsiElement> PsiElement.descendantOfTypeStrict(): T? =
    PsiTreeUtil.findChildOfType(this, T::class.java, /* strict */ true)

fun PsiElement.findExistingEditor(): Editor? {
    ApplicationManager.getApplication().assertReadAccessAllowed()

    val containingFile = containingFile
    if (!containingFile.isValid) return null

    val file = containingFile?.virtualFile ?: return null
    val document = FileDocumentManager.getInstance().getDocument(file) ?: return null

    val editorFactory = EditorFactory.getInstance()
    val editors = editorFactory.getEditors(document)
    return editors.firstOrNull()
}
