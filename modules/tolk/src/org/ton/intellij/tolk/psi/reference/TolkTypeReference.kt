package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.TolkIncludeDefinitionMixin
import org.ton.intellij.tolk.psi.impl.resolveFile

class TolkTypeReference(
    element: TolkElement,
) : PsiReferenceBase.Poly<TolkElement>(
    element
) {
    val identifier get() = element.node.findChildByType(TolkElementTypes.IDENTIFIER)!!

    override fun calculateDefaultRangeInElement(): TextRange? {
        val identifier = identifier
        return TextRange(identifier.startOffsetInParent, identifier.textLength)
    }

    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        return buildList<ResolveResult> {
            val typeParameterName = identifier.text.removeSurrounding("`")
            val owner = myElement.parentOfType<TolkTypeParameterListOwner>()
            if (owner != null) {
                if (typeParameterName == "self" && owner is TolkFunction) {
                    val selfParameter = owner.parameterList?.parameterList?.find { it.name == "self" }
                    if (selfParameter != null) {
                        add(PsiElementResolveResult(selfParameter))
                    }
                }

                if (myElement.parentOfType<TolkFunctionReceiver>() == null) {
                    val genericType = owner.resolveGenericType(typeParameterName)
                    if (genericType != null && genericType.parameter.psi != element) {
                        add(PsiElementResolveResult(genericType.parameter.psi))
                    }
                }
            }

            val file = myElement.containingFile as? TolkFile ?: return@buildList
            val typeDefResults = collectTypeDefResults(file, typeParameterName)
            addAll(typeDefResults)
        }.toTypedArray()
    }

    private fun collectTypeDefResults(file: TolkFile, target: String): List<PsiElementResolveResult> {
        val results = ArrayList<PsiElementResolveResult>()

        val includedFiles = HashSet<TolkFile>()
        val commonStdlib =
            TolkIncludeDefinitionMixin.resolveTolkImport(file.project, file, "@stdlib/common")
        if (commonStdlib != null) {
            val tolkCommonStdlib = commonStdlib.findPsiFile(file.project) as? TolkFile
            if (tolkCommonStdlib != null) {
                includedFiles.add(tolkCommonStdlib)
            }
        }
        file.includeDefinitions.forEach {
            val resolvedFile = it.resolveFile(it.project)
            if (resolvedFile != null) {
                val resolvedTolkFile = resolvedFile.findPsiFile(it.project) as? TolkFile
                if (resolvedTolkFile != null) {
                    includedFiles.add(resolvedTolkFile)
                }
            }
        }
        includedFiles.add(file)

        includedFiles.forEach { file ->
            file.typeDefs.forEach { typeDef ->
                if (typeDef.name == target) {
                    results.add(PsiElementResolveResult(typeDef))
                }
            }
            file.structs.forEach { struct ->
                if (struct.name == target) {
                    results.add(PsiElementResolveResult(struct))
                }
            }
        }
        return results
    }
}
