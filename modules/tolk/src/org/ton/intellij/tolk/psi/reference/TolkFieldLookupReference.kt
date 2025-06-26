package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.type.*

class TolkFieldLookupReference(
    element: TolkFieldLookup
) : TolkReferenceBase<TolkFieldLookup>(element) {
    override fun multiResolve(): List<TolkElement> =
        element.inference?.getResolvedRefs(element)?.mapNotNull { it.element as? TolkElement } ?: emptyList()

    override fun isReferenceTo(element: PsiElement): Boolean {
        return (element is TolkStructField || element is TolkFunction) && super.isReferenceTo(element)
    }
}

fun resolveFieldLookupReferenceWithReceiver(
    receiverType: TolkTy,
    fieldLookup: TolkFieldLookup,
): List<Pair<TolkTypedElement, Substitution>> {
    val name = fieldLookup.referenceName ?: return emptyList()
    val unwrappedReceiverType = receiverType.unwrapTypeAlias()
    if (unwrappedReceiverType is TolkStructTy) {
        val sub = Substitution.instantiate(unwrappedReceiverType.psi.declaredType, receiverType)
        unwrappedReceiverType.psi.structFields.forEachIndexed { index, field ->
            if (index.toString() != name && field.name != name) {
                return@forEachIndexed
            }
            return listOf(field to sub)
        }
    }

    return collectFunctionCandidates(
        fieldLookup.project,
        receiverType,
        name,
        fieldLookup.containingFile as TolkFile,
        fieldLookup.typeArgumentList
    )
}

fun collectFunctionCandidates(
    project: Project,
    calledReceiver: TolkTy?,
    name: String,
    containingFile: TolkFile,
    typeArgumentList: TolkTypeArgumentList?
): List<Pair<TolkFunction, Substitution>> {
    val functionCandidates = collectFunctionCandidates(
        project,
        calledReceiver,
        name,
        containingFile
    )
    val typeArgs = typeArgumentList?.typeExpressionList?.map { it.type }
    if (typeArgs != null) {
        return functionCandidates.map { (function, sub) ->
            var sub = sub
            val typeParams = function.typeParameterList?.typeParameterList?.map { it.type }
            if (typeParams != null && typeParams.size == typeArgs.size) {
                for (i in typeArgs.indices) {
                    val paramElement = typeParams[i] ?: continue
                    val argElement = typeArgs[i] ?: continue
                    sub = sub.deduce(paramElement, argElement)
                }
            }
            (function to sub)
        }
    }
    return functionCandidates
}

fun collectFunctionCandidates(
    project: Project,
    calledReceiver: TolkTy?,
    name: String,
    containingFile: TolkFile
): List<Pair<TolkFunction, Substitution>> {
    val namedFunctionsSeq = containingFile.resolveSymbols(name)
        .asSequence()
        .filterIsInstance<TolkFunction>()

    val namedFunctions: List<TolkFunction>
    if (calledReceiver != null) {
        namedFunctions = namedFunctionsSeq.filter { it.hasReceiver }.toList()
    } else {
        return namedFunctionsSeq.filter { !it.hasSelf && !it.hasReceiver }.map { it to EmptySubstitution }.toList()
    }

    val candidates = ArrayList<Pair<TolkFunction, Substitution>>()

    // step1: find all methods where a receiver equals to provided, e.g. `MInt.copy`
    val calledReceiverActualUnwrappedType = calledReceiver.unwrapTypeAlias().actualType()
    for (function in namedFunctions) {
        val functionReceiver = function.functionReceiver?.typeExpression ?: continue
        val functionReceiverType = functionReceiver.type?.unwrapTypeAlias() ?: continue
        if (!functionReceiverType.hasGenerics() && functionReceiverType == calledReceiverActualUnwrappedType) {
            candidates.add(function to EmptySubstitution)
        }
    }
    if (candidates.isNotEmpty()) {
        return candidates
    }

    // step2: find all methods where a receiver can accept provided, e.g. `int8.copy` / `int?.copy` / `(int|slice).copy`
    for (function in namedFunctions) {
        val functionReceiverType = function.receiverTy
        if (!functionReceiverType.hasGenerics() && functionReceiverType.canRhsBeAssigned(calledReceiver)) {
            candidates.add(function to EmptySubstitution)
        }
    }

    if (candidates.isNotEmpty()) {
        return candidates
    }

    // step 3: try to match generic receivers, e.g. `Container<T>.copy` / `(T?|slice).copy` but NOT `T.copy`
    val actualCalledReceiver = calledReceiver.actualType()
    for (function in namedFunctions) {
        val functionReceiver = function.receiverTy

        val actualFunctionReceiver = functionReceiver.actualType()
        if (functionReceiver.hasGenerics() && functionReceiver !is TolkTyParam) {
            if (actualFunctionReceiver is TolkStructTy && actualCalledReceiver is TolkStructTy) {
                if (!actualFunctionReceiver.psi.isEquivalentTo(actualCalledReceiver.psi)) {
                    continue
                }
            }

            val sub = Substitution.instantiate(functionReceiver, calledReceiver)
            candidates.add(function to sub)
        }
    }
    if (candidates.isNotEmpty()) {
        return candidates
    }

    // step 4: try to match `T.copy`
    for (function in namedFunctions) {
        val functionReceiver = function.receiverTy
        if (functionReceiver is TolkTyParam) {
            candidates.add(function to Substitution(mapOf(functionReceiver to calledReceiver)))
        }
    }

    if (candidates.isNotEmpty()) {
        return candidates
    }

    TolkBuiltins[project].getFunction(name)?.let {
        return listOf(it to EmptySubstitution)
    }

    return emptyList()
}
