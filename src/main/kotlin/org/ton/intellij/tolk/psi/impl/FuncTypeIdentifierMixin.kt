package org.ton.intellij.tolk.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkTypeIdentifier

abstract class TolkTypeIdentifierMixin(node: ASTNode) : ASTWrapperPsiElement(node), TolkTypeIdentifier {
    override fun getReferences(): Array<PsiReference> = arrayOf(
        object : PsiReferenceBase.Poly<TolkTypeIdentifier>(
            this, TextRange(0, textLength), false
        ) {
            override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
                return buildList<ResolveResult> {
                    PsiTreeUtil.treeWalkUp(myElement, null) { scope, _ ->
                        if (scope is TolkFunction) {
                            val typeParameterList = scope.typeParameterList
                            for (funcTypeParameter in typeParameterList) {
                                if (myElement.identifier.textMatches(funcTypeParameter.name ?: continue)) {
                                    add(PsiElementResolveResult(funcTypeParameter))
                                }
                            }
                            return@treeWalkUp false
                        }
                        true
                    }
                }.toTypedArray()
            }
        }
    )

    override fun getReference(): PsiReference? = references.firstOrNull()
}
