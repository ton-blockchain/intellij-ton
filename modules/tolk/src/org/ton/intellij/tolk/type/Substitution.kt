package org.ton.intellij.tolk.type


open class Substitution(
    val typeSubst: Map<TyTypeParameter, TolkTy> = emptyMap()
) {
    operator fun plus(other: Substitution): Substitution =
        Substitution(
            typeSubst + other.typeSubst
        )

    operator fun get(key: TyTypeParameter) = typeSubst[key]

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Substitution
        return typeSubst == other.typeSubst
    }

    override fun hashCode(): Int = typeSubst.hashCode()
}

object EmptySubstitution : Substitution()

fun Map<TyTypeParameter, TolkTy>.toSubstitution() = Substitution(this)
