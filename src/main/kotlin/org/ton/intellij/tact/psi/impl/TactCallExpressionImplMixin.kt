package org.ton.intellij.tact.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiReference
import org.ton.intellij.tact.psi.*
import org.ton.intellij.tact.resolve.TactFieldReference
import org.ton.intellij.tact.type.TactTyMap
import org.ton.intellij.tact.type.TactTyRef
import org.ton.intellij.tact.type.selfInferenceResult
import org.ton.intellij.util.ancestorStrict

abstract class TactCallExpressionImplMixin(node: ASTNode) : ASTWrapperPsiElement(node), TactCallExpression {
    override fun getReference(): PsiReference? {
        val identifier = identifier
        val ref = TactFieldReference(this, identifier.textRangeInParent)
        when (identifier.text) {
            "ton",
            "pow",
            "require",
            "address",
            "cell",
            "dump",
            "emptyMap",
            "sha256" -> {
                if (isStaticCall()) {
                    return null
                }
            }

            "get",
            "set",
            "asCell" -> {
                val parent = parent
                if (parent is TactDotExpression) {
                    val leftTy = parent.expressionList.firstOrNull()?.let { left ->
                        parent.ancestorStrict<TactInferenceContextOwner>()?.selfInferenceResult?.getExprTy(left)
                    }
                    if (leftTy is TactTyMap) {
                        return null
                    }
                }
            }

            "toCell" -> {
                val parent = parent
                if (parent is TactDotExpression) {
                    val leftTy = parent.expressionList.firstOrNull()?.let { left ->
                        parent.ancestorStrict<TactInferenceContextOwner>()?.selfInferenceResult?.getExprTy(left)
                    }
                    if (leftTy is TactTyRef && (leftTy.item is TactStruct || leftTy.item is TactMessage)) {
                        return null
                    }
                }
            }
        }
        return ref
    }
}

fun TactCallExpression.isStaticCall(): Boolean {
    val parent = parent
    return if (parent is TactDotExpression) {
        parent.expressionList.firstOrNull() == this
    } else {
        true
    }
}
