package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.CompletionUtil.DUMMY_IDENTIFIER_TRIMMED
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.codeInsight.lookup.LookupElementRenderer
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.FuncElement
import org.ton.intellij.func.psi.FuncExpressionStatement
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncReferenceExpression
import org.ton.intellij.func.psi.impl.FuncReference

class FuncReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val expression = PsiTreeUtil.getParentOfType(parameters.position, FuncReferenceExpression::class.java)
        val originalFile = parameters.originalFile
        if (expression != null && !expression.identifier.textMatches(DUMMY_IDENTIFIER_TRIMMED)) {
            println("parent: '${expression.identifier.text}'")
            println("add completion expression: $expression | ${expression.text}")
            fillVariantsByReference(expression, expression.reference, originalFile, result)
        }
    }

    private fun fillVariantsByReference(
        originalElement: FuncElement,
        reference: PsiReference?,
        file: PsiFile,
        result: CompletionResultSet,
    ) {
        if (reference == null) return
        when (reference) {
            is PsiMultiReference -> {
                reference.references.let {
                    it.sortWith(PsiMultiReference.COMPARATOR)
                    return fillVariantsByReference(originalElement, it.firstOrNull(), file, result)
                }
            }

            is FuncReference -> {
                reference.processResolveVariants(MyProcessor(originalElement, result), implicitStdlib = false)
            }
        }
    }

    private class MyProcessor(
        val originalElement: FuncElement,
        val result: CompletionResultSet,
    ) : PsiScopeProcessor {
        private val processedNames = HashSet<String>()

        override fun execute(element: PsiElement, state: ResolveState): Boolean {
            addElement(element, originalElement, processedNames, result)
            return true
        }
    }

    private object FunctionParameterRenderer : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {

        }
    }

    private object FunctionRenderer : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            val psiElement = element.psiElement
            if (psiElement !is FuncFunction) return
            presentation.icon = FuncIcons.FUNCTION
            presentation.itemText = element.lookupString
        }
    }

    companion object {
        const val KEYWORD_PRIORITY = 20.0
        const val CONTEXT_KEYWORD_PRIORITY = 25.0
        const val NOT_IMPORTED_FUNCTION_PRIORITY = 3.0
        const val FUNCTION_PRIORITY = NOT_IMPORTED_FUNCTION_PRIORITY + 10.0


        fun addElement(
            element: PsiElement,
            context: FuncElement,
            processedNames: MutableSet<String>,
            set: CompletionResultSet,
        ) {
            val lookup = createLookupElement(element, context) ?: return
            if (processedNames.add(lookup.lookupString)) {
                set.addElement(lookup)
            }
        }

        fun createLookupElement(
            element: PsiElement,
            originalElement: FuncElement,
        ): LookupElement? {
            return when (element) {
                is FuncFunction -> {
                    val functionName = element.name ?: return null
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                            .createWithSmartPointer(functionName, element)
                            .withRenderer(FunctionRenderer)
                            .withInsertHandler { context, item ->
                                println(originalElement.text)
                                ParenthesesInsertHandler.NO_PARAMETERS.handleInsert(context, item)
                                val parentOriginalElement = originalElement.parent
                                if (parentOriginalElement is FuncExpressionStatement) {

                                }
                            },
                        FUNCTION_PRIORITY
                    )
                }
//                is FuncFunctionParameter -> {
////                    val name = element.name ?: return null
////                    PrioritizedLookupElement.withPriority(
////                        LookupElementBuilder
////                            .createWithSmartPointer(name, element)
////                            .withRenderer()
////                    )
//                }
                else -> null
            }
        }
    }
}