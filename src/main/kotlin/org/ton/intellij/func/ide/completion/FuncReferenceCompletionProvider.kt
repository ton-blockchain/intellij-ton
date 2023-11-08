package org.ton.intellij.func.ide.completion

import com.intellij.codeInsight.TailType
import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionProvider
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.completion.PrioritizedLookupElement
import com.intellij.codeInsight.completion.util.ParenthesesInsertHandler
import com.intellij.codeInsight.lookup.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.psi.ResolveState
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference
import com.intellij.psi.scope.PsiScopeProcessor
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.ide.completion.FuncCompletionContributor.Companion.FUNCTION_PRIORITY
import org.ton.intellij.func.ide.completion.FuncCompletionContributor.Companion.VAR_PRIORITY
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.psi.impl.FuncReference

class FuncReferenceCompletionProvider : CompletionProvider<CompletionParameters>() {
    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        val expression = PsiTreeUtil.getParentOfType(parameters.position, FuncReferenceExpression::class.java)
        val originalFile = parameters.originalFile
        if (expression != null) {
//            println("parent: '${expression.identifier.text}'")
//            println("add completion expression: $expression | ${expression.text}")
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
            val psiElement = element.psiElement as? FuncFunctionParameter ?: return
            presentation.apply {
                icon = FuncIcons.PARAMETER
                itemText = element.lookupString
                isTypeGrayed = true
                typeText = psiElement.atomicType?.text
            }
        }
    }

    private class CatchVariableRenderer(
        val catchStatement: FuncCatch,
    ) : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            presentation.apply {
                icon = FuncIcons.VARIABLE
                isTypeGrayed = true
                itemText = element.lookupString
            }
        }
    }

    private class VariableRenderer(
        val variable: FuncVarExpression,
    ) : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            presentation.apply {
                icon = FuncIcons.VARIABLE
                isTypeGrayed = true
                itemText = element.lookupString
            }
        }
    }

    private class ConstRenderer(
        val variable: FuncConstVar,
    ) : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            presentation.apply {
                icon = FuncIcons.CONSTANT
                isTypeGrayed = true
                itemText = element.lookupString
                presentation.isTypeGrayed = true
                presentation.typeText = variable.intKeyword?.text ?: variable.sliceKeyword?.text
            }
        }
    }

    private class GlobalVarRenderer(
        val variable: FuncGlobalVar,
    ) : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            presentation.apply {
                icon = FuncIcons.GLOBAL_VARIABLE
                isTypeGrayed = true
                itemText = element.lookupString
                presentation.isTypeGrayed = true
                presentation.typeText = variable.type.text
            }
        }
    }

    private object FunctionRenderer : LookupElementRenderer<LookupElement>() {
        override fun renderElement(element: LookupElement, presentation: LookupElementPresentation) {
            val psiElement = element.psiElement
            if (psiElement !is FuncFunction) return
            presentation.icon = FuncIcons.FUNCTION
            presentation.itemText = element.lookupString
            presentation.tailText =
                psiElement.functionParameterList.joinToString(", ", prefix = "(", postfix = ")") { it.text }
            presentation.isTypeGrayed = true
            presentation.typeText = psiElement.type.text
        }
    }

    companion object {

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
                    var functionName = element.name ?: return null
                    val parent = originalElement.parent
                    if (parent is FuncMethodCall) {
                        if (parent.parent is FuncDotExpression && functionName.firstOrNull() == '~') {
                            return null
                        }
                        if (parent.parent is FuncModifyExpression && functionName.firstOrNull() != '~') {
                            functionName = "~$functionName"
                        }
                        val parameters = element.functionParameterList
                        if (parameters.isEmpty()) {
                            return null
                        }
                    }

                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                            .createWithSmartPointer(functionName, element)
                            .withRenderer(FunctionRenderer)
                            .withInsertHandler { context, item ->

                                val parameters = element.functionParameterList

                                if (parameters.isEmpty()) {
                                    ParenthesesInsertHandler.NO_PARAMETERS.handleInsert(context, item)
                                } else if (parameters.size == 1 && originalElement.parent is FuncMethodCall) {
                                    ParenthesesInsertHandler.NO_PARAMETERS.handleInsert(context, item)
                                } else {
//                                    println("grand grand parent: ${originalElement.parent.parent.parent.elementType} | ${originalElement.parent.parent.parent.text}")
//                                    println("grand parent: ${originalElement.parent.parent.elementType} | ${originalElement.parent.parent.text}")
//                                    println("parent: ${originalElement.parent.elementType} | ${originalElement.parent.text}")
//                                    println("child: ${originalElement.elementType} | ${originalElement.text}")
                                    // TODO: handle as NO_PARAMETERS at `a.foo/**caret**/` when `foo` - function with 1 parameter
                                    ParenthesesInsertHandler.WITH_PARAMETERS.handleInsert(context, item)
                                }
                            },
                        FUNCTION_PRIORITY
                    ).let {
                        var result = it
                        // TODO: insert semicolon if void return
                        PsiTreeUtil.treeWalkUp(originalElement, null) { scope, _ ->
//                            println("walking up: ${scope.elementType} | ${scope.text}")
                            when (scope) {
                                is FuncDotExpression -> {
                                    return@treeWalkUp false
                                }
                                is FuncBlockStatement -> {
                                    result = TailTypeDecorator.withTail(result, TailType.SEMICOLON)
                                    return@treeWalkUp false
                                }

                                is FuncTensorExpression -> {
                                    return@treeWalkUp false
                                }
                            }
                            true
                        }
                        result
                    }
                }

                is FuncFunctionParameter -> {
                    val name = element.name ?: return null
                    if (originalElement.parent is FuncMethodCall) return null
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                            .createWithSmartPointer(name, element)
                            .withRenderer(FunctionParameterRenderer),
                        VAR_PRIORITY
                    )
                }

                is FuncConstVar -> {
                    val name = element.name ?: return null
                    if (originalElement.parent is FuncMethodCall) return null
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                            .createWithSmartPointer(name, element)
                            .withRenderer(ConstRenderer(element)),
                        VAR_PRIORITY
                    )
                }

                is FuncGlobalVar -> {
                    val name = element.name ?: return null
                    if (originalElement.parent is FuncMethodCall) return null
                    PrioritizedLookupElement.withPriority(
                        LookupElementBuilder
                            .createWithSmartPointer(name, element)
                            .withRenderer(GlobalVarRenderer(element)),
                        VAR_PRIORITY
                    )
                }

                is FuncReferenceExpression -> {
                    val name = element.name ?: return null
                    val parent = originalElement.parent
                    if (parent is FuncCallExpression || parent is FuncMethodCall) return null
                    var lookupElement: LookupElement? = null
                    PsiTreeUtil.treeWalkUp(element, null) { scope, prevParent ->
                        when (scope) {
                            is FuncCatch -> if (scope.expression == prevParent) {
                                lookupElement = PrioritizedLookupElement.withPriority(
                                    LookupElementBuilder
                                        .createWithSmartPointer(name, element)
                                        .withRenderer(CatchVariableRenderer(scope)),
                                    VAR_PRIORITY
                                )
                                return@treeWalkUp false
                            }

                            is FuncAssignExpression -> {
                                when (val parentScope = scope.parent) {
                                    is FuncVarExpression -> if (scope.expressionList.firstOrNull() == prevParent) {
                                        lookupElement = PrioritizedLookupElement.withPriority(
                                            LookupElementBuilder
                                                .createWithSmartPointer(name, element)
                                                .withRenderer(VariableRenderer(parentScope)),
                                            VAR_PRIORITY
                                        )
                                        return@treeWalkUp false
                                    }

//                                    is FuncConstVariable -> if (scope.expressionList.firstOrNull() == prevParent) {
//                                        lookupElement = PrioritizedLookupElement.withPriority(
//                                            LookupElementBuilder
//                                                .createWithSmartPointer(name, element)
//                                                .withRenderer(ConstRenderer(parentScope)),
//                                            VAR_PRIORITY
//                                        )
//                                        return@treeWalkUp false
//                                    }
                                }
                            }

                            is FuncVarExpression -> if (scope.expressionList.getOrNull(1) == prevParent) {
                                lookupElement = PrioritizedLookupElement.withPriority(
                                    LookupElementBuilder
                                        .createWithSmartPointer(name, element)
                                        .withRenderer(VariableRenderer(scope)),
                                    VAR_PRIORITY
                                )
                                return@treeWalkUp false
                            }
                        }
                        return@treeWalkUp true
                    }
                    lookupElement
                }

                else -> null
            }
        }
//

//

//
//                else -> null
//            }
    }
}
