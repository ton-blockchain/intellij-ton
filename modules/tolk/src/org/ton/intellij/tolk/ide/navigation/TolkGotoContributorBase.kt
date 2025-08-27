package org.ton.intellij.tolk.ide.navigation

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.GotoClassContributor
import com.intellij.navigation.NavigationItem
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.StubIndex
import com.intellij.psi.stubs.StubIndexKey
import com.intellij.util.Processor
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkNamedElement
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.type.render

open class TolkGotoContributorBase<T : TolkNamedElement>(
    private val clazz: Class<T>,
    private vararg val indexKeys: StubIndexKey<String, T>,
) : GotoClassContributor, ChooseByNameContributorEx {

    override fun processNames(processor: Processor<in String>, scope: GlobalSearchScope, filter: IdFilter?) {
        for (key in indexKeys) {
            ProgressManager.checkCanceled()
            StubIndex.getInstance().processAllKeys(key, processor, scope, filter)
        }
    }

    override fun processElementsWithName(name: String, processor: Processor<in NavigationItem>, parameters: FindSymbolParameters) {
        for (key in indexKeys) {
            ProgressManager.checkCanceled()
            StubIndex.getInstance().processElements(
                key, name, parameters.project, parameters.searchScope, parameters.idFilter,
                clazz, processor
            )
        }
    }

    override fun getQualifiedName(item: NavigationItem): String? {
        if (item !is TolkNamedElement) return null
        if (item is TolkFunction && item.hasReceiver) {
            return item.receiverTy.render() + "." + item.name
        }
        return item.name
    }

    override fun getQualifiedNameSeparator() = "."
}
