package org.ton.intellij.tolk.psi

import org.ton.intellij.tolk.type.TyTypeParameter

interface TolkTypeParameterListOwner : TolkNamedElement {
    val typeParameterList: TolkTypeParameterList?

    fun resolveGenericType(name: String): TyTypeParameter? {
        val typeParam = typeParameterList?.typeParameterList?.firstOrNull { it.name == name }
        if (typeParam != null) {
            return TyTypeParameter.create(typeParam)
        }
        if (this !is TolkFunction) return null
        functionReceiver?.let { functionReceiver ->
            val visitor = GenericVisitor()
            functionReceiver.accept(visitor)
            val element = visitor.unresolvedAsGenerics[name]
            if (element != null) {
                return TyTypeParameter.create(element)
            }
        }
        return null
    }

    private class GenericVisitor(
        val unresolvedAsGenerics: MutableMap<String, TolkReferenceTypeExpression> = mutableMapOf(),
    )  : TolkRecursiveElementWalkingVisitor() {
        override fun visitReferenceTypeExpression(o: TolkReferenceTypeExpression) {
            val reference = o.reference ?: return
            if (reference.resolve() != null) return
            unresolvedAsGenerics[o.identifier.text.removeSurrounding("`")] = o
        }
    }
}
