package org.ton.intellij.tolk.ide.assembly

import com.intellij.ui.JBColor
import java.awt.Color

internal data class TolkAssemblyBlockColors(
    val sourceBackground: JBColor,
    val assemblyBackground: JBColor,
    val dividerFill: JBColor,
)

internal object TolkAssemblyBlockPalette {
    private val palette = listOf(
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xEEF5FF), Color(0x29435F)),
            assemblyBackground = JBColor(Color(0xEEF5FF), Color(0x29435F)),
            dividerFill = JBColor(Color(0xEEF5FF), Color(0x29435F)),
        ),
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xEFFAEC), Color(0x233F30)),
            assemblyBackground = JBColor(Color(0xEFFAEC), Color(0x233F30)),
            dividerFill = JBColor(Color(0xEFFAEC), Color(0x233F30)),
        ),
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xFFF5E8), Color(0x50361F)),
            assemblyBackground = JBColor(Color(0xFFF5E8), Color(0x50361F)),
            dividerFill = JBColor(Color(0xFFF5E8), Color(0x50361F)),
        ),
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xF8F0FF), Color(0x433058)),
            assemblyBackground = JBColor(Color(0xF8F0FF), Color(0x433058)),
            dividerFill = JBColor(Color(0xF8F0FF), Color(0x433058)),
        ),
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xFFF0F3), Color(0x532D38)),
            assemblyBackground = JBColor(Color(0xFFF0F3), Color(0x532D38)),
            dividerFill = JBColor(Color(0xFFF0F3), Color(0x532D38)),
        ),
        TolkAssemblyBlockColors(
            sourceBackground = JBColor(Color(0xEDF7F7), Color(0x1F4445)),
            assemblyBackground = JBColor(Color(0xEDF7F7), Color(0x1F4445)),
            dividerFill = JBColor(Color(0xEDF7F7), Color(0x1F4445)),
        ),
    )

    fun colorsFor(blockIndex: Int): TolkAssemblyBlockColors = palette[Math.floorMod(blockIndex, palette.size)]
}
