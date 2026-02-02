package org.ton.intellij.tolk.codeInsight.hint.codeVision

import com.intellij.codeInsight.codeVision.CodeVisionRelativeOrdering
import com.intellij.codeInsight.hints.codeVision.ReferencesCodeVisionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiSearchHelper
import com.intellij.psi.search.PsiSearchHelper.SearchCostResult
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isEntryPoint
import org.ton.intellij.tolk.psi.impl.isGetMethod
import java.util.concurrent.atomic.AtomicInteger

private const val ID = "tolk.references"

class TolkReferencesCodeVisionProvider : ReferencesCodeVisionProvider() {
    override val relativeOrderings: List<CodeVisionRelativeOrdering> get() = emptyList()
    override val id: String get() = ID

    override fun acceptsFile(file: PsiFile): Boolean = file is TolkFile

    override fun acceptsElement(element: PsiElement): Boolean {
        return when (element) {
            is TolkFunction -> !element.isGetMethod && !element.isEntryPoint
            is TolkStruct,
            is TolkTypeDef,
            is TolkGlobalVar,
            is TolkConstVar -> true

            else -> false
        }
    }

    override fun getHint(element: PsiElement, file: PsiFile): String? = getVisionInfo(element, file)?.text

    override fun getVisionInfo(element: PsiElement, file: PsiFile): CodeVisionInfo? {
        if (!element.isValid || element !is PsiNamedElement) return null
        val elementName = element.name ?: return null
        val scope = GlobalSearchScope.projectScope(element.project)
        val costSearchOutsideCurrentFile =
            PsiSearchHelper.getInstance(element.project).isCheapEnoughToSearch(elementName, scope, file)
        if (costSearchOutsideCurrentFile == SearchCostResult.TOO_MANY_OCCURRENCES) return null

        val limit = 50
        val usagesCount = AtomicInteger()
        ReferencesSearch.search(ReferencesSearch.SearchParameters(element, scope, false))
            .allowParallelProcessing()
            .forEach(Processor {
                if (it == null) true
                else if (element.reference == it) true
                else {
                    usagesCount.incrementAndGet() <= limit
                }
            })

        val usagesCountValue = usagesCount.get()
        return CodeVisionInfo(
            text = TolkBundle.message(
                "inlay.hints.usages.text",
                usagesCountValue,
                if (usagesCountValue >= limit) 1 else 0
            ),
            count = usagesCountValue,
            countIsExact = usagesCountValue < limit
        )
    }
}
