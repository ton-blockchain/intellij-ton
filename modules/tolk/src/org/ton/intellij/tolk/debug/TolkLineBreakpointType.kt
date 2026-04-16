package org.ton.intellij.tolk.debug

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.xdebugger.breakpoints.XLineBreakpoint
import com.intellij.xdebugger.breakpoints.XLineBreakpointType
import com.intellij.xdebugger.evaluation.XDebuggerEditorsProvider
import org.ton.intellij.tolk.TolkFileType

class TolkLineBreakpointType :
    XLineBreakpointType<TolkLineBreakpointProperties>(
        "tolk-line-breakpoint",
        "Tolk",
    ) {
    override fun canPutAt(file: VirtualFile, line: Int, project: Project): Boolean =
        file.fileType == TolkFileType && line >= 0

    override fun createBreakpointProperties(file: VirtualFile, line: Int): TolkLineBreakpointProperties =
        TolkLineBreakpointProperties()

    override fun getEditorsProvider(
        breakpoint: XLineBreakpoint<TolkLineBreakpointProperties>,
        project: Project,
    ): XDebuggerEditorsProvider = TolkDebuggerEditorsProvider
}
