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
        result: CompletionResultSet,
    ) {
        val contract = parameters.position.parentOfType<TolkContractDefinition>() ?: return
        val existingFields = contract.contractDefinitionBody?.contractFieldList?.mapNotNull {
            it?.identifier?.text
        }?.toSet() ?: emptySet()

        val fields = mapOf(
            "author" to "Contract author exported to ABI as-is",
            "version" to "Contract version exported as-is, preferably semver",
            "description" to "Contract description exported as-is",
            "incomingMessages" to "Accepted internal messages type",
            "incomingExternal" to "Expected external message slice type",
            "storage" to "Persistent on-chain data shape",
            "storageAtDeployment" to "Initial storage shape used for address calculation",
            "forceAbiExport" to "Extra ABI types not reachable from storage, messages, or getters",
        )

        val typeFields = setOf(
            "incomingMessages",
            "incomingExternal",
            "storage",
            "storageAtDeployment",
            "forceAbiExport",
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
                        val template = templateManager.createTemplate("", "", "${'$'}VALUE$")
                        template.addVariable("VALUE", ConstantNode(""), true)
                        templateManager.startTemplate(context.editor, template)

                        if (field in typeFields) {
                            AutoPopupController.getInstance(context.project)?.scheduleAutoPopup(context.editor)
                        }
                    },
            )
        }
    }
}
