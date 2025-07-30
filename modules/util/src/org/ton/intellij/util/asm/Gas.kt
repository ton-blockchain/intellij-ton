package org.ton.intellij.util.asm

fun instructionPresentation(gas: String?, stack: String?, format: String): String {
    if (gas.isNullOrEmpty()) {
        return ": no data"
    }
    return format.replace("{gas}", gas).replace("{stack}", getStackPresentation(stack))
}
