package org.ton.intellij.fc2tolk.symbols

import org.ton.intellij.func.psi.FuncTypeParameter

abstract class FTTypeParameterSymbol : JKSymbol

class FTFuncTypeParameterSymbol(
    override val target: FuncTypeParameter
) : FTTypeParameterSymbol()