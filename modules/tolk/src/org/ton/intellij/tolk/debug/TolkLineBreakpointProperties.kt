package org.ton.intellij.tolk.debug

import com.intellij.util.xmlb.XmlSerializerUtil
import com.intellij.xdebugger.breakpoints.XBreakpointProperties

class TolkLineBreakpointProperties : XBreakpointProperties<TolkLineBreakpointProperties>() {
    override fun getState(): TolkLineBreakpointProperties = this

    override fun loadState(state: TolkLineBreakpointProperties) {
        XmlSerializerUtil.copyBean(state, this)
    }
}
