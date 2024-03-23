package org.ton.intellij.func.psi.impl

import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.impl.source.resolve.ResolveCache
import org.ton.intellij.func.psi.*
import org.ton.intellij.util.parentOfType

class FuncReference(
    element: FuncReferenceExpression,
    rangeInElement: TextRange,
) : PsiReferenceBase.Poly<FuncReferenceExpression>(element, rangeInElement, false) {
    val identifier: PsiElement get() = element.identifier

    private val resolver = ResolveCache.PolyVariantResolver<FuncReference> { t, incompleteCode ->
        if (!myElement.isValid) return@PolyVariantResolver ResolveResult.EMPTY_ARRAY

        val inference = element.inference
        val inferenceResolved = inference?.getResolvedRefs(element)
        if (!inferenceResolved.isNullOrEmpty()) {
            return@PolyVariantResolver inferenceResolved.toTypedArray()
        }
        val name = identifier.text.let {
            if (it.startsWith('.')) it.substring(1) else it
        }
        val file = element.containingFile as? FuncFile
        if (file != null) {
            val contextFunction = element.parentOfType<FuncFunction>()
            val includes = file.collectIncludedFiles(true)

            includes.forEach { includedFile ->
                includedFile.constVars.forEach { constVar ->
                    if (constVar.name == name) {
                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(constVar))
                    }
                }
                includedFile.globalVars.forEach { globalVar ->
                    if (globalVar.name == name) {
                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(globalVar))
                    }
                }
                includedFile.functions.forEach { function ->
                    val functionName = function.name
                    if (functionName == name) {
                        return@PolyVariantResolver arrayOf(PsiElementResolveResult(function))
                    }
                    if (name.startsWith("~") && functionName != null && !functionName.startsWith("~")) {
                        val tensorList = (function.typeReference as? FuncTensorType)?.typeReferenceList
                        if (tensorList?.size == 2 && functionName == name.substring(1)) {
                            return@PolyVariantResolver arrayOf(PsiElementResolveResult(function))
                        }
                    }

                    if (function == contextFunction) {
                        return@PolyVariantResolver ResolveResult.EMPTY_ARRAY
                    }
                }
            }
        }

        ResolveResult.EMPTY_ARRAY
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        if (!myElement.isValid) return ResolveResult.EMPTY_ARRAY
        return ResolveCache.getInstance(myElement.project).resolveWithCaching(this, resolver, false, incompleteCode)
    }

    override fun handleElementRename(newElementName: String): PsiElement {
        return element.identifier.replace(FuncPsiFactory[element.project].createIdentifier(newElementName))
    }
}
