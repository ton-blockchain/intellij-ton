package org.ton.intellij.acton

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

object ActonIcons {
    val TON: Icon = IconLoader.getIcon("/icons/ton_symbol.svg", ActonIcons::class.java)
    val TS_WRAPPER: Icon = IconLoader.getIcon("/icons/typeScript.svg", ActonIcons::class.java)
    val DISASSEMBLE: Icon = IconLoader.getIcon("/icons/asm.svg", ActonIcons::class.java)
}
