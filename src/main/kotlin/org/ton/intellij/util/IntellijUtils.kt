package org.ton.intellij.util

import com.intellij.extapi.psi.StubBasedPsiElementBase
import com.intellij.lang.ASTNode
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.impl.PsiDocumentManagerBase
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.psi.tree.IElementType
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil

fun tokenSetOf(vararg tokens: IElementType) = TokenSet.create(*tokens)

val org.intellij.markdown.ast.ASTNode.textRange: TextRange
    get() = TextRange(startOffset, endOffset)

val PsiFile.document: Document?
    get() = viewProvider.document

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

val PsiElement.contexts: Sequence<PsiElement>
    get() = generateSequence(this) {
        if (it is PsiFile) null else it.context
    }

fun ASTNode?.isWhitespaceOrEmpty(): Boolean {
    return this == null || textLength == 0 || elementType == TokenType.WHITE_SPACE
}

@Suppress("UNCHECKED_CAST")
inline val <T : StubElement<*>> StubBasedPsiElement<T>.greenStub: T?
    get() = (this as? StubBasedPsiElementBase<T>)?.greenStub

fun checkCommitIsNotInProgress(project: Project) {
    val app = ApplicationManager.getApplication()
    if ((app.isUnitTestMode || app.isInternal) && app.isDispatchThread) {
        if ((PsiDocumentManager.getInstance(project) as PsiDocumentManagerBase).isCommitInProgress) {
            error("Accessing indices during PSI event processing can lead to typing performance issues")
        }
    }
}

inline fun <Key : Any, reified Psi : PsiElement> getElements(
    indexKey: StubIndexKey<Key, Psi>,
    key: Key, project: Project,
    scope: GlobalSearchScope?
): Collection<Psi> =
    StubIndex.getElements(indexKey, key, project, scope, Psi::class.java)
