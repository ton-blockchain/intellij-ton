package org.ton.intellij.func.psi.impl

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncTypeIdentifier

abstract class FuncTypeIdentifierMixin(node: ASTNode) : ASTWrapperPsiElement(node), FuncTypeIdentifier {
    override fun getReferences(): Array<PsiReference> = arrayOf(
        object : PsiReferenceBase.Poly<FuncTypeIdentifier>(
            this, TextRange(0, textLength), false
        ) {
            override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
                return buildList<ResolveResult> {
                    PsiTreeUtil.treeWalkUp(myElement, null) { scope, _ ->
                        if (scope is FuncFunction) {
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
