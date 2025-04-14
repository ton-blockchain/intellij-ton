package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkReferenceTypeExpression
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypeParameterListOwner
import org.ton.intellij.tolk.sdk.TolkSdkManager
import org.ton.intellij.tolk.stub.index.TolkTypeDefIndex
import org.ton.intellij.util.parentOfType
import org.ton.intellij.util.psiElement

object TolkTypeCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement<PsiElement>().withParent(psiElement<TolkReferenceTypeExpression>())

    private val primitiveTypes = listOf(
        "int", "slice", "cell", "builder", "tuple", "bool", "address"
//        "continuation"
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val project = parameters.originalFile.project
        val position = parameters.position
        val parameterListOwner = position.parentOfType<TolkTypeParameterListOwner>()
        parameterListOwner?.typeParameterList?.typeParameterList?.forEach { type ->
            result.addElement(
                LookupElementBuilder.createWithIcon(type)
            )
        }
        primitiveTypes.forEach { type ->
            result.addElement(
                LookupElementBuilder.create(type).withBoldness(true)
            )
        }
        val typeCandidates = HashSet<TolkTypeDef>()
        val tolkSdk = TolkSdkManager[project].getSdkRef().resolve(project)
        if (tolkSdk != null) {
            VfsUtilCore.iterateChildrenRecursively(tolkSdk.stdlibFile, null) {
                val tolkFile = it.findPsiFile(project) as? TolkFile
                if (tolkFile != null) {
                    tolkFile.typeDefs.forEach { typeDef ->
                        typeCandidates.add(typeDef)
                    }
                }
                true
            }
        }
        val prefix = CompletionUtil.findReferenceOrAlphanumericPrefix(parameters)

        StubIndex.getInstance().processAllKeys(TolkTypeDefIndex.KEY, project) {key ->
            if (key.startsWith(prefix)) {
                StubIndex.getInstance().processElements(
                    TolkTypeDefIndex.KEY,
                    key,
                    project,
                    GlobalSearchScope.allScope(project),
                    TolkTypeDef::class.java
                ) {
                    typeCandidates.add(it)
                    true
                }
            }
            true
        }

        typeCandidates.forEach { typeDef ->
            result.addElement(
                LookupElementBuilder.createWithIcon(typeDef)
                    .withInsertHandler { context, item ->
                        val file = item.psiElement?.containingFile
                        val insertFile = context.file as? TolkFile ?: return@withInsertHandler
                        val includeCandidateFile = file as? TolkFile ?: return@withInsertHandler
                        insertFile.import(includeCandidateFile)
                    }
            )
        }


    }
}
