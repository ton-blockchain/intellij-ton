package org.ton.intellij.tolk.type

class TolkExpressionFlowContext(
    val outFlow: TolkFlowContext,
    val trueFlow: TolkFlowContext,
    val falseFlow: TolkFlowContext
) {
    constructor(outFlow: TolkFlowContext, cloneFlowForCondition: Boolean) : this(
        outFlow,
        if (cloneFlowForCondition) TolkFlowContext(outFlow) else outFlow,
        if (cloneFlowForCondition) TolkFlowContext(outFlow) else outFlow
    )
}
