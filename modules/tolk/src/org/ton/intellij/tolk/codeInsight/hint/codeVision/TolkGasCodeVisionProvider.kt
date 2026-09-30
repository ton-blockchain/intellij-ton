package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.intellij.codeInsight.codeVision.CodeVisionAnchorKind
import com.intellij.codeInsight.codeVision.CodeVisionEntry
import com.intellij.codeInsight.codeVision.CodeVisionProvider
import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.codeVision.ui.model.ClickableTextCodeVisionEntry
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiDocumentManager
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.ide.assembly.TolkAssemblyPreviewManager
import org.ton.intellij.tolk.psi.TolkFile

/** Batches all eligible declarations in an editor into one asynchronous Acton disassembly request. */
class TolkGasCodeVisionProvider : CodeVisionProvider<Unit> {
    override val id: String get() = ID
    override val name: String get() = TolkBundle.message("code.vision.gas.name")
    override val defaultAnchor: CodeVisionAnchorKind get() = CodeVisionAnchorKind.Top
    override val relativeOrderings: List<CodeVisionRelativeOrdering> get() = emptyList()

    override fun precomputeOnUiThread(editor: Editor) = Unit

    override fun computeForEditor(editor: Editor, uiData: Unit): List<Pair<TextRange, CodeVisionEntry>> =
        runReadAction {
            val project = editor.project ?: return@runReadAction emptyList()
            val file = PsiDocumentManager.getInstance(project).getPsiFile(editor.document) as? TolkFile
                ?: return@runReadAction emptyList()
            val source = file.virtualFile?.takeIf { it.isInLocalFileSystem } ?: return@runReadAction emptyList()
            val config = ActonToml.find(file.project, source) ?: return@runReadAction emptyList()
            val service = file.project.service<TolkGasCodeVisionService>()
            val documents = FileDocumentManager.getInstance()
            // Acton reads saved sources, including imports. Never attach disk results to unsaved code.
            if (documents.unsavedDocuments.any { document ->
                    documents.getFile(document)?.let { it.extension == "tolk" || it.name == "Acton.toml" } == true
                }
            ) {
                return@runReadAction emptyList()
            }

            val functions = file.functions.filter { function ->
                function.name != null &&
                    function.name != "onBouncedMessage" &&
                    function.functionBody?.blockStatement != null &&
                    function.functionReceiver == null &&
                    function.typeParameterList == null &&
                    function.parameterList?.parameterList.orEmpty().none { it.mutateKeyword != null }
            }
            if (functions.isEmpty()) return@runReadAction emptyList()
            val results = service.request(source, config.workingDir, functions.mapNotNull { it.name })
            functions.mapNotNull { function ->
                val result = results[function.name] ?: return@mapNotNull null
                val gas = result.gas_estimate
                val text = TolkBundle.message("code.vision.gas.text", gas.value.toString())
                val tooltip = buildList {
                    add(TolkBundle.message("code.vision.gas.tooltip"))
                    if (gas.has_dynamic_cost) add(TolkBundle.message("code.vision.gas.dynamic"))
                    if (gas.has_control_flow) add(TolkBundle.message("code.vision.gas.control.flow"))
                    if (gas.unknown_instructions > 0) {
                        add(TolkBundle.message("code.vision.gas.unknown", gas.unknown_instructions))
                    }
                }.joinToString("\n")
                val sourceLine = editor.document.getLineNumber(function.textRange.startOffset)
                val entry = ClickableTextCodeVisionEntry(
                    text = text,
                    providerId = id,
                    tooltip = tooltip,
                    onClick = { _, clickedEditor ->
                        val project = clickedEditor.project
                        if (project != null && !project.isDisposed) {
                            TolkAssemblyPreviewManager.openFunction(
                                project,
                                source,
                                result,
                                sourceLine,
                            )
                        }
                    },
                )
                function.textRange to entry
            }
        }

    companion object {
        const val ID: String = "tolk.gas"
    }
}
