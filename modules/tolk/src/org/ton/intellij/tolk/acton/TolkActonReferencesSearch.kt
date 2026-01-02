package org.ton.intellij.tolk.acton

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.UsageSearchContext
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor
import org.toml.lang.psi.TomlKeySegment

class TolkActonReferencesSearch : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.elementToSearch
        if (element !is TomlKeySegment) return
        val name = element.name ?: return
        
        val optimizer = queryParameters.optimizer
        optimizer.searchWord(name, queryParameters.effectiveSearchScope, UsageSearchContext.IN_STRINGS, true, element)
    }
}
