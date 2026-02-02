package org.ton.intellij.tolk.psi.reference

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.*
import org.ton.intellij.tolk.type.*
import kotlin.math.max

class TolkFieldLookupReference(
    element: TolkFieldLookup
) : TolkReferenceBase<TolkFieldLookup>(element) {
    override fun multiResolve(): List<TolkElement> =
        element.inference?.getResolvedRefs(element)?.mapNotNull { it.element as? TolkElement } ?: emptyList()

    override fun isReferenceTo(element: PsiElement): Boolean {
        return (element is TolkStructField || element is TolkEnumMember || element is TolkFunction) && super.isReferenceTo(element)
    }
}

fun resolveFieldLookupReferenceWithReceiver(
    receiverType: TolkTy,
    fieldLookup: TolkFieldLookup,
    isStaticReceiver: Boolean,
): List<Pair<TolkTypedElement, Substitution>> {
    val name = fieldLookup.referenceName ?: return emptyList()
    val unwrappedReceiverType = receiverType.unwrapTypeAlias()
    if (unwrappedReceiverType is TolkTyStruct && !isStaticReceiver) {
        val sub = Substitution.instantiate(unwrappedReceiverType.psi.declaredType, receiverType.unwrapTypeAlias())
        unwrappedReceiverType.psi.structFields.forEachIndexed { index, field ->
            if (index.toString() != name && field.name != name) {
                return@forEachIndexed
            }
            return listOf(field to sub)
        }
    }
    if (unwrappedReceiverType is TolkTyEnum) {
        unwrappedReceiverType.psi.members.forEachIndexed { index, member ->
            if (index.toString() != name && member.name != name) {
                return@forEachIndexed
            }
            return listOf(member to Substitution.empty())
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

data class MethodCallCandidate(
    val originalReceiver: TolkTy,
    val instantiatedReceiver: TolkTy,
    val method: TolkFunction,
    val substitutedTs: Substitution,
) {
    fun isGeneric() = !originalReceiver.isEquivalentTo(instantiatedReceiver)
}

enum class ShapeKind {
    GenericT,     // T
    Union,        // U|V, T?
    Primitive,    // int, slice, address, ...
    Tensor,       // (A,B,...)
    Instantiated, // Map<K,V>, Container<T>, Struct<X>, ...
}

// for every receiver, we calculate "score": how deep and specific it is;
// e.g., between `Container<T>` and `T` we choose the first;
// e.g., between `map<int8, V>` and `map<K, map<K, K>>` we choose the second;
data class ShapeScore(
    val kind: ShapeKind,
    val depth: Int,
) {
    fun isShapeBetterThan(rhs: ShapeScore): Boolean {
        if (kind != rhs.kind) {
            return kind > rhs.kind
        }
        return depth > rhs.depth
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShapeScore

        if (depth != other.depth) return false
        if (kind != other.kind) return false

        return true
    }

    override fun hashCode(): Int {
        var result = depth
        result = 31 * result + kind.hashCode()
        return result
    }
}

fun collectFunctionCandidates(
    project: Project,
    calledReceiver: TolkTy?,
    name: String,
    containingFile: TolkFile,
): List<Pair<TolkFunction, Substitution>> {
    val namedFunctionsSeq = containingFile.resolveSymbols(name).filterIsInstance<TolkFunction>()

    if (calledReceiver == null) {
        // simple call like foo()
        // so return all non-method functions
        val functions = namedFunctionsSeq.filter { !it.hasSelf && !it.hasReceiver }.map { it to EmptySubstitution }.toList()
        if (functions.isEmpty()) {
            TolkBuiltins[project].getFunction(name)?.let {
                return listOf(it to EmptySubstitution)
            }
        }
        return functions
    }

    // Since we call with receiver, filter simple functions
    val namedMethods = namedFunctionsSeq.filter { it.hasReceiver }.toList()
    if (namedMethods.isEmpty()) {
        TolkBuiltins[project].getFunction(name)?.let {
            return listOf(it to EmptySubstitution)
        }
    }

    return collectMethodCandidates(calledReceiver, namedMethods)
}

fun collectMethodCandidates(
    calledReceiver: TolkTy,
    methods: List<TolkFunction>,
    forCompletion: Boolean = false,
): List<Pair<TolkFunction, Substitution>> {
    // find all methods theoretically applicable; we'll filter them by priority;
    // for instance, if there is `T.method`, it will be instantiated with T=provided_receiver
    var viable = mutableListOf<MethodCallCandidate>()
    for (method in methods) {
        val receiver = method.receiverTy
        if (receiver.hasGenerics()) {
            // check whether exist some T to make it a valid call (probably with type coercion)
            val deducingTs = Substitution.instantiate(receiver, calledReceiver)
            val replaced = receiver.substitute(deducingTs)
            if (replaced.canRhsBeAssigned(calledReceiver)) {
                viable.add(MethodCallCandidate(receiver, replaced, method, deducingTs))
            }
        } else if (receiver.canRhsBeAssigned(calledReceiver)) {
            viable.add(MethodCallCandidate(receiver, receiver, method, Substitution.empty())) // empty?
        }
    }

    if (viable.isEmpty()) {
        if (calledReceiver is TolkTyUnknown) {
            // We cannot always infer a type of generic functions like
            // ```
            // fun getWrapperValue2<T>(c: T) {
            //    return c.value;
            // }
            // ```
            // so fallback to an initial methods list to resolve somehow
            return methods.map { it to EmptySubstitution }
        }
        return emptyList()
    }

    // if nothing found, return nothing;
    // if the only found, it's the one
    if (viable.size <= 1) {
        return viable.map { it.method to it.substitutedTs }
    }

    if (forCompletion) {
        return viable.map { it.method to EmptySubstitution }
    }

    // okay, we have multiple viable methods, and need to locate the better

    // 1) exact match candidates with equal_to()
    //    (for instance, an alias equals to its underlying type, as well as `T1|T2` equals to `T2|T1`)
    val exact = mutableListOf<MethodCallCandidate>()
    for (candidate in viable) {
        if (candidate.instantiatedReceiver.isEquivalentTo(calledReceiver)) {
            exact.add(candidate)
        }
    }
    if (exact.size == 1) {
        return exact.map { it.method to it.substitutedTs }
    }
    if (exact.isNotEmpty()) {
        viable = exact
    }

    // 2) if there are both generic and non-generic functions, filter out generic
    var nGenerics = 0
    for (candidate in viable) {
        nGenerics += if (candidate.isGeneric()) 1 else 0
    }
    if (nGenerics < viable.size) {
        val nonGeneric = mutableListOf<MethodCallCandidate>()
        for (candidate in viable) {
            if (!candidate.isGeneric()) {
                nonGeneric.add(candidate)
            }
        }

        // all the code below is dedicated to choosing between generic Ts, so return if non-generic
        return nonGeneric.map { it.method to it.substitutedTs }
    }

    // 3) better shape in terms of structural depth
    //    (prefer `Container<T>` over `T` and `map<K1, map<K2,V2>>` over `map<K,V>`)
    var bestShape = ShapeScore(ShapeKind.GenericT, -999)
    for (candidate in viable) {
        val s = calculateShapeScore(candidate.originalReceiver)
        if (s.isShapeBetterThan(bestShape)) {
            bestShape = s
        }
    }

    val bestByShape = mutableListOf<MethodCallCandidate>()
    for (candidate in viable) {
        if (calculateShapeScore(candidate.originalReceiver) == bestShape) {
            bestByShape.add(candidate)
        }
    }
    if (bestByShape.size == 1) {
        return bestByShape.map { it.method to it.substitutedTs }
    }
    if (bestByShape.isNotEmpty()) {
        viable = bestByShape
    }

    // 4) find the overload that dominates all others
    //    (prefer `Container<int>` over `Container<T>` and `map<K, slice>` over `map<K, V>`)
    var dominator: MethodCallCandidate? = null
    for (candidate in viable) {
        var dominatesAll = true
        for (other in viable) {
            if (candidate.method != other.method) {
                dominatesAll = dominatesAll && isMoreSpecificGeneric(candidate.originalReceiver, other.originalReceiver)
            }
        }
        if (dominatesAll) {
            dominator = candidate
        }
    }

    if (dominator != null && !forCompletion) {
        return listOf(dominator.method to dominator.substitutedTs)
    }

    return viable.map { it.method to EmptySubstitution }
}

// tries to find Ts in `pattern` to reach `actual`;
// example: pattern=`map<K, slice>`, actual=`map<int, slice>` => T=int
// example: pattern=`Container<T>`, actual=`Container<Container<U>>` => T=Container<U>
fun canSubstituteToReachActual(pattern: TolkTy, actual: TolkTy): Boolean {
    val subst = Substitution.instantiate(pattern, actual)
    val replaced = pattern.substitute(subst)
    return replaced.isEquivalentTo(actual)
}

// checks whether a generic typeA is more specific than typeB;
// example: `map<int,V>` dominates `map<K,V>`;
// example: `map<K, map<K,K>>` dominates `map<K, map<K,V>>` dominates `map<K1, map<K2,V>>`;
// example: `map<int,V>` and `map<K,slice>` are not comparable;
fun isMoreSpecificGeneric(typeA: TolkTy, typeB: TolkTy): Boolean {
    // exists θ: θ(B)=A && not exists φ: φ(A)=B
    return canSubstituteToReachActual(typeB, typeA) &&
            !canSubstituteToReachActual(typeA, typeB)
}

// calculate score for a receiver;
// note: it's an original receiver, with generics, not an instantiated one
fun calculateShapeScore(ty: TolkTy): ShapeScore {
    if (ty is TolkTyParam) {
        return ShapeScore(ShapeKind.GenericT, 1)
    }

    if (ty is TolkTyUnion) {
        var d = 0
        for (variant in ty.variants) {
            d = max(d, calculateShapeScore(variant).depth)
        }
        return ShapeScore(ShapeKind.Union, 1 + d)
    }

    if (ty is TolkTyTensor) {
        var d = 0
        for (element in ty.elements) {
            d = max(d, calculateShapeScore(element).depth)
        }
        return ShapeScore(ShapeKind.Tensor, 1 + d)
    }

    if (ty is TolkTyTypedTuple) {
        var d = 0
        for (element in ty.elements) {
            d = max(d, calculateShapeScore(element).depth)
        }
        return ShapeScore(ShapeKind.Tensor, 1 + d)
    }

    if (ty is TolkTyStruct) {
        var d = 0
        for (typeT in ty.typeArguments) {
            d = max(d, calculateShapeScore(typeT).depth)
        }
        return ShapeScore(ShapeKind.Instantiated, 1 + d)
    }

    if (ty is TolkTyAlias) {
        val innerShape = calculateShapeScore(ty.underlyingType)
        if (ty.typeArguments.isEmpty()) {
            return innerShape
        }
        var d = innerShape.depth
        for (typeT in ty.typeArguments) {
            d = max(d, calculateShapeScore(typeT).depth)
        }
        return ShapeScore(ShapeKind.Instantiated, 1 + d)
    }

    return ShapeScore(ShapeKind.Primitive, 1)
}
