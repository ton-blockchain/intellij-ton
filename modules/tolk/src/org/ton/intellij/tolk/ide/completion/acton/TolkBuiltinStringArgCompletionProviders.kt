package org.ton.intellij.tolk.ide.completion.acton

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.util.ProcessingContext
import org.ton.intellij.acton.cli.ActonToml
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.impl.isGetMethod
import org.ton.intellij.tolk.psi.impl.isTestFunction
import org.ton.intellij.tolk.stub.index.TolkFunctionIndex

object TolkWalletNameCompletionProvider : TolkStringArgumentCompletionProvider() {
    override fun shouldAddCompletions(functionName: String, qualifierName: String?, argumentIndex: Int): Boolean =
        functionName == "wallet" && qualifierName == "net" && argumentIndex == 0

    override fun addStringCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
        functionName: String,
        qualifierName: String?,
        argumentIndex: Int,
    ) {
        val project = parameters.editor.project ?: return
        val actonToml = ActonToml.find(project) ?: return
        val wallets = actonToml.getWallets()
        for (wallet in wallets) {
            result.addElement(
                LookupElementBuilder.create(wallet.name)
                    .withIcon(AllIcons.General.User)
                    .withTailText(if (wallet.isLocal) " (local)" else " (global)", true)
            )
        }
    }
}

object TolkContractIdCompletionProvider : TolkStringArgumentCompletionProvider() {
    override fun shouldAddCompletions(functionName: String, qualifierName: String?, argumentIndex: Int): Boolean =
        functionName == "build" && qualifierName == null && argumentIndex == 0

    override fun addStringCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
        functionName: String,
        qualifierName: String?,
        argumentIndex: Int,
    ) {
        val project = parameters.editor.project ?: return
        val actonToml = ActonToml.find(project) ?: return
        val contractIds = actonToml.getContractIds()
        for (id in contractIds) {
            result.addElement(
                LookupElementBuilder.create(id)
                    .withIcon(AllIcons.Nodes.Class)
            )
        }
    }
}

object TolkGetMethodCompletionProvider : TolkStringArgumentCompletionProvider() {
    override fun shouldAddCompletions(functionName: String, qualifierName: String?, argumentIndex: Int): Boolean =
        functionName == "runGetMethod" && qualifierName == "net" && argumentIndex == 1

    override fun addStringCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
        functionName: String,
        qualifierName: String?,
        argumentIndex: Int,
    ) {
        val project = parameters.editor.project ?: return
        TolkFunctionIndex.processAllElements(project) { function ->
            if (!function.isGetMethod) return@processAllElements true
            if (function.isTestFunction()) return@processAllElements true

            val file = function.containingFile.originalFile as? TolkFile ?: return@processAllElements true
            if (file.isActonFile()) return@processAllElements true

            function.name?.let { name ->
                result.addElement(
                    LookupElementBuilder.create(name)
                        .withIcon(TolkIcons.METHOD)
                        .withTailText(" ${file.name}", true)
                )
            }
            true
        }
    }
}
