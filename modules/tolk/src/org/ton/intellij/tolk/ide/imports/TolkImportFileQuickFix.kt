package org.ton.intellij.tolk.ide.imports

import com.intellij.codeInsight.FileModificationService
import com.intellij.codeInsight.daemon.impl.ShowAutoImportPass
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.intention.HighPriorityAction
import com.intellij.codeInspection.HintAction
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.ui.SimpleColoredComponent
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.IncorrectOperationException
import com.intellij.util.ui.JBUI
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.psi.TolkCallExpression
import org.ton.intellij.tolk.psi.TolkConstVar
import org.ton.intellij.tolk.psi.TolkDotExpression
import org.ton.intellij.tolk.psi.TolkEnum
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkGlobalVar
import org.ton.intellij.tolk.psi.TolkReferenceExpression
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.reference.TolkSymbolReference
import org.ton.intellij.tolk.psi.reference.TolkTypeReference
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.pathString
import kotlin.io.path.relativeTo

class TolkImportFileQuickFix : LocalQuickFixAndIntentionActionOnPsiElement, HintAction, HighPriorityAction {
    private val symbolToResolve: String
    private var filesToImport: List<SmartPsiElementPointer<TolkFile>>? = null

    constructor(element: PsiElement, file: TolkFile) : super(element) {
        symbolToResolve = ""
        filesToImport = listOf(SmartPointerManager.createPointer(file))
    }

    constructor(reference: PsiReference) : super(reference.element) {
        symbolToResolve = reference.canonicalText
    }

    override fun showHint(editor: Editor) = doAutoImportOrShowHint(editor, true)

    override fun getText(): String {
        val element = startElement ?: return "Import file"
        return "Import " + getText(element, findImportVariants(element))
    }

    override fun getFamilyName() = "Import file"

    override operator fun invoke(
        project: Project, file: PsiFile, editor: Editor?,
        startElement: PsiElement, endElement: PsiElement,
    ) {
        if (!FileModificationService.getInstance().prepareFileForWrite(file)) return
        perform(findImportVariants(startElement), file, editor)
    }

    override fun isAvailable(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement,
    ): Boolean {
        val reference = getReference(startElement)
        return file is TolkFile &&
                file.manager.isInProject(file) &&
                reference != null &&
                reference.resolve() == null &&
                findImportVariants(startElement).isNotEmpty() &&
                notQualified(startElement)
    }

    private fun findImportVariants(element: PsiElement): List<SmartPsiElementPointer<TolkFile>> {
        if (filesToImport == null) {
            filesToImport = findImportVariants(symbolToResolve, element).map { SmartPointerManager.createPointer(it) }
        }
        return filesToImport!!
    }

    fun doAutoImportOrShowHint(editor: Editor, showHint: Boolean): Boolean {
        val element: PsiElement? = startElement
        if (element == null || !element.isValid) {
            return false
        }

        val reference = getReference(element)
        if (reference == null || reference.resolve() != null) {
            return false
        }

        val filesToImport = findImportVariants(element)
        if (filesToImport.isEmpty()) {
            return false
        }

        val file = element.containingFile
        val firstFileToImport = filesToImport.firstOrNull()

        if (filesToImport.size == 1) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                CommandProcessor.getInstance().runUndoTransparentAction { perform(file, firstFileToImport) }
                return true
            }
        }

        if (!showHint) return false

        if (ApplicationManager.getApplication().isUnitTestMode) return false
        if (HintManager.getInstance().hasShownHintsThatWillHideByOtherHint(true)) return false
        val referenceRange = reference.rangeInElement.shiftRight(element.textRange.startOffset)

        HintManager.getInstance().showQuestionHint(
            editor,
            ShowAutoImportPass.getMessage(filesToImport.size > 1, filesToImport.first().relativePath(file)),
            referenceRange.startOffset,
            referenceRange.endOffset
        ) {
            if (file.isValid && !editor.isDisposed) {
                perform(filesToImport, file, editor)
            }
            true
        }
        return true
    }

    private fun perform(filesToImport: List<SmartPsiElementPointer<TolkFile>>, containingFile: PsiFile, editor: Editor?) {
        LOG.assertTrue(
            editor != null || filesToImport.size == 1,
            "Cannot invoke fix with ambiguous imports on null editor"
        )

        if (ApplicationManager.getApplication().isUnitTestMode) {
            perform(containingFile, filesToImport.minBy { it.virtualFile.path.length })
        }

        if (filesToImport.size == 1) {
            perform(containingFile, filesToImport.first())
            return
        }

        if (filesToImport.size > 1 && editor != null) {
            val renderer = SelectionAwareListCellRenderer<SmartPsiElementPointer<TolkFile>> { file ->
                val name = runReadAction { startElement.text }
                val relativePath = file.relativePath(containingFile)

                SimpleColoredComponent().apply {
                    icon = TolkIcons.FILE
                    append(name ?: "unknown")
                    append(" ($relativePath)", SimpleTextAttributes.GRAY_ATTRIBUTES)
                    border = JBUI.Borders.empty(2, 4)
                }
            }

            val builder = JBPopupFactory.getInstance().createPopupChooserBuilder(filesToImport)
                .setRequestFocus(true)
                .setTitle("Files to Import")
                .setRenderer(renderer)
                .setItemChosenCallback { item ->
                    perform(containingFile, item)
                }

            val popup = builder.createPopup()
            popup.showInBestPositionFor(editor)
            return
        }

        val files = filesToImport.joinToString(", ")
        throw IncorrectOperationException("Cannot invoke fix with ambiguous imports on editor ()$editor. Files: $files")
    }

    private fun perform(file: PsiFile, fileToImport: SmartPsiElementPointer<TolkFile>?) {
        if (file !is TolkFile || fileToImport == null) return
        val fileToImportPsi = fileToImport.dereference() ?: return
        if (!canBeFileImported(fileToImportPsi)) return

        CommandProcessor.getInstance().executeCommand(file.project, {
            runWriteAction {
                if (!isAvailable) return@runWriteAction
                file.import(fileToImportPsi)
            }
        }, "Add Import", null)
    }

    companion object {
        private fun getReference(element: PsiElement): PsiReference? {
            if (!element.isValid) return null

            for (reference in element.references) {
                if (isSupportedReference(reference)) {
                    return reference
                }
            }

            return null
        }

        private fun canBeFileImported(file: TolkFile): Boolean {
            if (file.name == "common.tolk") {
                // This can be a some stdlib file, check if it contains map<K, V>
                return file.structs.find { it.name == "map" } != null
            }

            val path = file.virtualFile.path
            val normalizedPath = path.replace(File.separatorChar, '/')
            return !normalizedPath.contains("test/") &&
                    !normalizedPath.contains("test-failed/")
        }

        private fun isSupportedReference(reference: PsiReference?) = reference is TolkSymbolReference || reference is TolkTypeReference

        private fun getText(element: PsiElement, filesToImport: List<SmartPsiElementPointer<TolkFile>>): String {
            if (filesToImport.isEmpty()) return ""
            val containingFile = element.containingFile ?: return ""
            return "'" + filesToImport.first().relativePath(containingFile) + "'? " + if (filesToImport.size > 1) "(multiple choices...) " else ""
        }

        private fun notQualified(startElement: PsiElement?): Boolean {
            if (startElement !is TolkReferenceExpression && startElement !is TolkReferenceTypeExpression) return false
            val parent = startElement.parent
            if (parent is TolkDotExpression) {
                val left = parent.expression
                if (startElement.isEquivalentTo(left)) {
                    // Foo.bar()
                    // ^^^ startElement
                    return true
                }

                // Foo.bar
                //     ^^^ startElement, potentially field

                val grand = parent.parent
                if (grand is TolkCallExpression) {
                    // Foo.bar()
                    //     ^^^ startElement, likely method
                    return true
                }

                // Foo.bar
                //     ^^^ startElement, field access
                return false
            }

            // foo, foo(), etc.
            return true
        }

        fun findImportVariants(symbolToImport: String, context: PsiElement): List<TolkFile> {
            val candidates = TolkNamedElementIndex.find(symbolToImport, context.project, null).filter {
                it is TolkFunction || it is TolkStruct || it is TolkTypeDef || it is TolkConstVar || it is TolkGlobalVar || it is TolkEnum
            }
            val files = candidates.mapNotNull { it.containingFile }
            return files
                .filterIsInstance<TolkFile>()
                .filter { canBeFileImported(it) } // filter non-importable files
        }
    }
}

fun SmartPsiElementPointer<TolkFile>.relativePath(file: PsiFile): String {
    val path = virtualFile.path
    val containingFile = file.virtualFile.parent?.path ?: ""

    val sdk = project.tolkSettings.toolchain.stdlibDir
    val sdkPath = sdk?.path
    if (sdkPath != null && path.contains(sdkPath)) {
        // @stdlib/gas-payments
        return path.replace(sdkPath, "@stdlib").removeSuffix(".tolk")
    }

    return Path(path).relativeTo(Path(containingFile)).pathString.removeSuffix(".tolk")
}
