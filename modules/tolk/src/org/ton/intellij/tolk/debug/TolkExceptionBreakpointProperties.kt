package org.ton.intellij.tolk.debug

import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class TolkExceptionBreakpointProperties : XBreakpointProperties<TolkExceptionBreakpointProperties>() {
    override fun getState(): TolkExceptionBreakpointProperties = this

    override fun loadState(state: TolkExceptionBreakpointProperties) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
