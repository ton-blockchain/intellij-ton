package org.ton.intellij.tlb.psi

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.parentOfType

class TlbReference(
    val project: Project,
    element: TlbParamTypeExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<TlbParamTypeExpression>(element, rangeInElement, false) {
    private val resolver = ResolveCache.PolyVariantResolver<TlbReference> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        val name = t.element.identifier?.text ?: return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
        val results = ArrayList<ResolveResult>()

        PsiTreeUtil.treeWalkUp(t.element, null) { scope, lastParent ->
            if (scope is TlbParamList) {
                for (field in scope.parentOfType<TlbFieldListOwner>()?.fieldList?.fieldList.orEmpty()) {
                    if (field is TlbNamedElement && field.name == name) {
                        results.add(PsiElementResolveResult(field))
                        if (!incompleteCode) {
                            return@treeWalkUp false
                        }
                    }
                }
                return@treeWalkUp false
            }
            if (scope is TlbFieldList) {
                for (field in scope.fieldList) {
                    if (field == lastParent) {
                        return@treeWalkUp false
                    }
                    if (field is TlbNamedElement && field.name == name) {
                        results.add(PsiElementResolveResult(field))
                        if (!incompleteCode) {
                            return@treeWalkUp false
                        }
                    }
                }
                false
            } else {
                true
            }
        }
        if (results.isNotEmpty()) {
            return@PolyVariantResolver results.toTypedArray()
        }

        val argumentList = (element.parent as? TlbApplyTypeExpression)?.argumentList?.typeExpressionList ?: emptyList()

        val tlbFile = t.element.containingFile as? TlbFile
        tlbFile?.findResultTypes(name)?.forEach { resultType ->
            val parameterList = resultType.paramList.typeExpressionList
            if (incompleteCode || matchArgumentAndParams(argumentList, parameterList)) {
                results.add(PsiElementResolveResult(resultType))
            }
        }

        results.toTypedArray()
    }

    fun matchArgumentAndParams(args: List<TlbTypeExpression>, params: List<TlbTypeExpression>): Boolean {
        if (args.size != params.size) return false
        return args.zip(params).all { (arg, param) ->
            if (arg is TlbIntTypeExpression && param is TlbIntTypeExpression) {
                arg.text == param.text
            } else {
                true
            }
        }
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        element.identifier?.replace(project.tlbPsiFactory.createIdentifier(newElementName))
        return element
    }

    override fun multiResolve(incompleteCode: Boolean): Array<out ResolveResult> {
       return ResolveCache.getInstance(project)
            .resolveWithCaching(this, resolver, true, incompleteCode)
    }
}