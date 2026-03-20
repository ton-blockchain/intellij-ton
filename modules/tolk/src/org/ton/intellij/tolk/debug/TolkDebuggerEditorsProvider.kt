package org.ton.intellij.tolk.debug

import com.intellij.openapi.fileTypes.FileType
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import org.ton.intellij.tolk.TolkFileType

object TolkDebuggerEditorsProvider : XDebuggerEditorsProvider() {
    override fun getFileType(): FileType = TolkFileType
}
