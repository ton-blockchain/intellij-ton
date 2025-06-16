package org.ton.intellij.boc

private const val BOC_EDITOR_TYPE_ID = "org.ton.intellij.boc.ui.editor"
private const val FILE_SIZE_LIMIT = 1024 * 1024 * 10 // 10 MB

//class BocFileEditorProvider : FileEditorProvider, DumbAware {
//
//    override fun accept(project: Project, file: VirtualFile): Boolean =
//        file.fileType is BocFileType && file.length < FILE_SIZE_LIMIT && runCatching {
//            disassembleBoc(file.contentsToByteArray(true))
//        }.isSuccess
//
//    override fun acceptRequiresReadAction() = false
//
//    override fun getEditorTypeId(): String = BOC_EDITOR_TYPE_ID
//
//    override fun getPolicy(): FileEditorPolicy {
//        @Suppress("UnstableApiUsage")
//        return FileEditorPolicy.HIDE_OTHER_EDITORS
//    }
//
//    @OptIn(ExperimentalStdlibApi::class)
//    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
//        val byteContents = file.contentsToByteArray()
//        val disassembledText =
//            try {
//                val disassembledFile = disassembleBoc(byteContents)
//                disassembledFile.prettyPrint(includeTvmCell = false)
//            } catch (e: Exception) {
//                "Exception while disassembling!\n\n$e"
//            }
//
//        val psiFactory = PsiFileFactory.getInstance(project)
//        val previewVirtualFile = psiFactory.createFileFromText("tvm_", AsmLanguage, disassembledText)
//        val mainVirtualFile =  psiFactory.createFileFromText("boc_", PlainTextLanguage.INSTANCE, byteContents.toHexString())
//
//        val mainEditor = createTextEditor(project, mainVirtualFile.virtualFile)
//        val preview = createTextEditor(project, previewVirtualFile.virtualFile)
//        return TextEditorWithPreview(mainEditor, preview)
//    }
//
//    private fun createTextEditor(project: Project, file: VirtualFile): TextEditor =
//        TextEditorProvider.getInstance().createEditor(project, file) as TextEditor
//}
