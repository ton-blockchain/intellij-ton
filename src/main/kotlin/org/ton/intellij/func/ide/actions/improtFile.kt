package org.ton.intellij.func.ide.actions

import com.intellij.codeInsight.hint.QuestionAction
import com.intellij.ide.util.DefaultPsiElementCellRenderer
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.openapi.util.RecursionManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.ui.popup.list.ListPopupImpl
import com.intellij.ui.popup.list.PopupListElementRenderer
import org.ton.intellij.func.psi.FuncIncludeDirective
import org.ton.intellij.func.psi.FuncPragmaDirective
import org.ton.intellij.func.psi.FuncPsiFactory
import org.ton.intellij.nullIfError
import java.awt.BorderLayout
import java.nio.file.Paths
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.ListCellRenderer
import kotlin.io.path.relativeTo

class FuncImportFileAction(
    val editor: Editor,
    val file: PsiFile,
    val suggestions: Set<PsiFile>
) : QuestionAction {
    val project get() = file.project

    override fun execute(): Boolean {
        PsiDocumentManager.getInstance(project).commitAllDocuments()

        if (suggestions.size == 1) {
            addIncludeDirective(project, file, suggestions.first())
        } else {
            chooseFileToImport()
        }

        return true
    }

    private fun chooseFileToImport() {
        val step = object : BaseListPopupStep<PsiFile>("File to import", suggestions.toList()) {
            override fun isAutoSelectionEnabled(): Boolean = false
            override fun isSpeedSearchEnabled(): Boolean = true
            override fun hasSubstep(selectedValue: PsiFile?): Boolean = true
            override fun getTextFor(value: PsiFile): String = value.name
            override fun getIconFor(value: PsiFile): Icon? = value.getIcon(0)

            override fun onChosen(selectedValue: PsiFile?, finalChoice: Boolean): PopupStep<*>? {
                if (selectedValue == null) return FINAL_CHOICE
                return doFinalStep {
                    PsiDocumentManager.getInstance(project).commitAllDocuments()
                    addIncludeDirective(project, file, selectedValue)
                }
            }
        }
        val popup = object : ListPopupImpl(project, step) {
            override fun getListElementRenderer(): ListCellRenderer<PsiFile> {
                @Suppress("UNCHECKED_CAST")
                val baseRenderer = super.getListElementRenderer() as PopupListElementRenderer<PsiFile>
                val psiRenderer = DefaultPsiElementCellRenderer()
                return ListCellRenderer { list, value, index, isSelected, cellHasFocus ->
                    val panel = JPanel(BorderLayout())
                    baseRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    panel.add(baseRenderer.nextStepLabel, BorderLayout.EAST)
                    panel.add(psiRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus))
                    panel
                }
            }
        }
        popup.showInBestPositionFor(editor)
    }

    companion object {
        fun isIncludedAlready(file: PsiFile, to: PsiFile): Boolean =
            if (file == to) true else {
                RecursionManager.doPreventingRecursion(file, true) {
                    file.children.asSequence()
                        .filterIsInstance<FuncIncludeDirective>()
                        .mapNotNull { nullIfError { it.includePath?.reference?.resolve()?.containingFile } }
                        .any { isIncludedAlready(it, to) }
                } ?: false
            }

        fun addIncludeDirective(project: Project, file: PsiFile, to: PsiFile) {
            CommandProcessor.getInstance().runUndoTransparentAction {
                ApplicationManager.getApplication().runWriteAction {
                    val after = file.children.filterIsInstance<FuncIncludeDirective>().lastOrNull()
                        ?: file.children.filterIsInstance<FuncPragmaDirective>().firstOrNull()
                    val factory = FuncPsiFactory(project)
                    file.addAfter(
                        factory.createIncludeDirective(buildIncludePath(file.virtualFile, to.virtualFile)),
                        after
                    )
                    file.addAfter(factory.createNewLine(), after)
                }
            }
        }

        fun buildIncludePath(source: VirtualFile, destination: VirtualFile): String =
            Paths.get(source.path).parent.relativeTo(Paths.get(destination.path)).toString()
    }
}