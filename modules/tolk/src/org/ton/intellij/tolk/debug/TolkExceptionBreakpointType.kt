package org.ton.intellij.tolk.debug

import com.intellij.xdebugger.breakpoints.XBreakpoint
import com.intellij.xdebugger.breakpoints.XBreakpointType

class TolkExceptionBreakpointType : XBreakpointType<XBreakpoint<TolkExceptionBreakpointProperties>, TolkExceptionBreakpointProperties>(
    "tolk-exception-breakpoint",
    "Tolk Exceptions"
) {
    override fun getDisplayText(breakpoint: XBreakpoint<TolkExceptionBreakpointProperties>): String {
        return "Uncaught Exceptions"
    }

    override fun createProperties(): TolkExceptionBreakpointProperties {
        return TolkExceptionBreakpointProperties()
    }

    override fun isAddBreakpointButtonVisible(): Boolean = false

    override fun createDefaultBreakpoint(
        creator: XBreakpointType.XBreakpointCreator<TolkExceptionBreakpointProperties>
    ): XBreakpoint<TolkExceptionBreakpointProperties> {
        return creator.createBreakpoint(createProperties())
    }
}
