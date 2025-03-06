package org.ton.intellij.tolk.dataFlow

import com.intellij.codeInspection.dataFlow.lang.ir.ControlFlow
import com.intellij.codeInspection.dataFlow.lang.ir.DataFlowIRProvider
import com.intellij.codeInspection.dataFlow.value.DfaValueFactory
import com.intellij.psi.PsiElement

class TolkDataFlowIRProvider : DataFlowIRProvider {
    override fun createControlFlow(
        factory: DfaValueFactory,
        psiBlock: PsiElement
    ): ControlFlow? {
        TODO("Not yet implemented")
    }
}
