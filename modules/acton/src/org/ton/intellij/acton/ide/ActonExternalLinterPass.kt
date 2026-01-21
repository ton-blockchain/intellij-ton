package org.ton.intellij.acton.ide

import com.intellij.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory
import com.intellij.codeHighlighting.TextEditorHighlightingPass
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx
import com.intellij.codeInsight.daemon.impl.FileStatusMap
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil
import com.intellij.codeInsight.multiverse.CodeInsightContextManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import com.intellij.util.ui.update.MergingUpdateQueue
import com.intellij.util.ui.update.Update
import org.ton.intellij.acton.settings.externalLinterSettings
import java.nio.file.Paths

class ActonExternalLinterPass(
    private val factory: ActonExternalLinterPassFactory,
    private val file: PsiFile,
    private val editor: Editor,
) : TextEditorHighlightingPass(file.project, editor.document), DumbAware {

    private val isUnitTestMode: Boolean get() = ApplicationManager.getApplication().isUnitTestMode

    private val highlights: MutableList<HighlightInfo> = mutableListOf()

    @Volatile
    private var annotationInfo: Lazy<ActonExternalLinterResult?>? = null

    @Volatile
    private var disposable: Disposable = myProject

    override fun doCollectInformation(progress: ProgressIndicator) {
        highlights.clear()
        if (file.virtualFile?.extension != "tolk" || !isAnnotationPassEnabled) return

        val workingDirectory = Paths.get(file.project.basePath ?: return)

        val moduleOrProject: Disposable = ModuleUtil.findModuleForFile(file) ?: myProject
        disposable = myProject.messageBus.createDisposableOnAnyPsiChange()
            .also { Disposer.register(moduleOrProject, it) }

        annotationInfo = ActonExternalLinterUtils.checkLazily(
            myProject,
            workingDirectory,
        )
    }

    override fun doApplyInformationToEditor() {
        if (file.virtualFile?.extension != "tolk") return

        val annotationInfo = annotationInfo
        if (annotationInfo == null || !isAnnotationPassEnabled) {
            disposable = myProject
            doFinish(emptyList())
            return
        }

        class ActonUpdate : Update(file) {
            val updateFile: PsiFile = file

            override fun setRejected() {
                super.setRejected()
                doFinish(highlights)
            }

            override fun canEat(update: Update): Boolean = updateFile == (update as? ActonUpdate)?.updateFile

            override fun run() {
                BackgroundTaskUtil.runUnderDisposeAwareIndicator(disposable) {
                    val annotationResult = annotationInfo.value ?: return@runUnderDisposeAwareIndicator
                    runReadAction {
                        ProgressManager.checkCanceled()
                        doApply(annotationResult)
                        ProgressManager.checkCanceled()
                        doFinish(highlights)
                    }
                }
            }
        }

        val update = ActonUpdate()
        if (isUnitTestMode) {
            update.run()
        } else {
            factory.scheduleExternalActivity(update)
        }
    }

    private fun doApply(annotationResult: ActonExternalLinterResult) {
        if (file.virtualFile?.extension != "tolk" || !file.isValid) return
        try {
            highlights.addHighlightsForFile(file, annotationResult)
        } catch (t: Throwable) {
            if (t is ProcessCanceledException) throw t
            LOG.error(t)
        }
    }

    private fun doFinish(highlights: List<HighlightInfo>) {
        invokeLater(ModalityState.stateForComponent(editor.component)) {
            if (Disposer.isDisposed(disposable)) return@invokeLater
            UpdateHighlightersUtil.setHighlightersToEditor(
                myProject,
                document,
                0,
                file.textLength,
                highlights,
                colorsScheme,
                id
            )
            DaemonCodeAnalyzerEx.getInstanceEx(myProject).fileStatusMap.markFileUpToDate(document, id)
        }
    }

    private val isAnnotationPassEnabled: Boolean
        get() = myProject.externalLinterSettings.enabled

    companion object {
        private val LOG = logger<ActonExternalLinterPass>()
    }
}

class ActonExternalLinterPassFactory(
    project: Project,
    registrar: TextEditorHighlightingPassRegistrar,
) : DirtyScopeTrackingHighlightingPassFactory {

    private val myPassId: Int = registrar.registerTextEditorHighlightingPass(
        this,
        null,
        null,
        false,
        -1
    )

    private val externalLinterQueue = MergingUpdateQueue(
        "ActonExternalLinterQueue",
        TIME_SPAN,
        true,
        MergingUpdateQueue.ANY_COMPONENT,
        project,
        null,
        false
    )

    override fun createHighlightingPass(file: PsiFile, editor: Editor): TextEditorHighlightingPass? {
        FileStatusMap.getDirtyTextRange(editor.document, file, myPassId) ?: return null
        return ActonExternalLinterPass(this, file, editor)
    }

    override fun getPassId(): Int = myPassId

    fun scheduleExternalActivity(update: Update) = externalLinterQueue.queue(update)

    companion object {
        private const val TIME_SPAN: Int = 100
    }
}