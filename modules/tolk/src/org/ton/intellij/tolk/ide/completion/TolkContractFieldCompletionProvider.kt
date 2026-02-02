package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.AutoPopupController
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.TemplateManager
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.icons.AllIcons
import com.intellij.patterns.ElementPattern
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.psi.TolkContractDefinition

object TolkContractFieldCompletionProvider : TolkCompletionProvider() {
    override val elementPattern: ElementPattern<out PsiElement> =
        psiElement().inside(TolkContractDefinition::class.java)

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet
    ) {
        val contract = parameters.position.parentOfType<TolkContractDefinition>() ?: return
        val existingFields = contract.contractDefinitionBody?.contractFieldList?.mapNotNull {
            it?.identifier?.text
        }?.toSet() ?: emptySet()

        val fields = mapOf(
            "author" to "Author of the contract",
            "version" to "Version of the contract",
            "description" to "Description of the contract",
            "symbolsNamespace" to "Namespace for contract symbols",
            "incomingMessages" to "Allowed incoming messages type",
            "incomingExternal" to "Allowed incoming external messages type",
            "storage" to "Persistent storage structure",
            "storageAtDeployment" to "Storage structure at deployment",
            "forceAbiExport" to "List of symbols to additionally export to ABI"
        )
        
        val typeFields = setOf(
            "incomingMessages",
            "incomingExternal",
            "storage",
            "storageAtDeployment",
            "forceAbiExport"
        )
        
        for ((field, description) in fields) {
            if (field in existingFields) continue

            result.addElement(
                LookupElementBuilder.create(field)
                    .withIcon(AllIcons.Nodes.Field)
                    .withTailText(" $description", true)
                    .bold()
                    .withInsertHandler { context, _ ->
                        val document = context.document
                        val offset = context.tailOffset
                        document.insertString(offset, ": ")
                        context.editor.caretModel.moveToOffset(offset + 2)
                        
                        val templateManager = TemplateManager.getInstance(context.project)
                        val template = templateManager.createTemplate("", "", "${'$'}VALUE$,")
                        template.addVariable("VALUE", ConstantNode(""), true)
                        templateManager.startTemplate(context.editor, template)

                        if (field in typeFields) {
                            AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
                        }
                    }
            )
        }
    }
}
