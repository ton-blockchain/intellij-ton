package org.ton.intellij.tact.type

import com.intellij.openapi.project.Project
import org.ton.intellij.tact.psi.TactReferencedType
import org.ton.intellij.tact.psi.TactType
import org.ton.intellij.tact.psi.TactTypeDeclarationElement
import org.ton.intellij.tact.stub.index.TactTypesIndex

interface TactTy {
    override fun toString(): String

    fun isAssignable(other: TactTy): Boolean

    companion object {
        fun search(project: Project, name: String): List<TactTy> {
            return TactTypesIndex.findElementsByName(project, name).map {
                it.declaredTy
            }
        }
    }
}

val TactType.ty: TactTy?
    get() {
        val type = reference?.resolve() as? TactTypeDeclarationElement ?: return null
        val declaredTy = type.declaredTy
        if (this is TactReferencedType && this.q != null) {
            return TactTyNullable(declaredTy)
        }
        return declaredTy
    }

sealed interface TactTyRuntime : TactTy

data object TactTyUnknown : TactTy {
    override fun toString(): String {
        return "???"
    }

    override fun isAssignable(other: TactTy): Boolean {
        return false
    }
}

data class TactTyRef(
    val item: TactTypeDeclarationElement
) : TactTy {
    override fun toString(): String = item.name ?: item.toString()

    override fun isAssignable(other: TactTy): Boolean {
        return other is TactTyRef && item == other.item
    }
}

data class TactTyNullable(
    val inner: TactTy
) : TactTy {
    override fun toString(): String = "$inner?"

    override fun isAssignable(other: TactTy): Boolean {
        return (other is TactTyNullable && inner.isAssignable(other.inner)) || inner.isAssignable(other)
    }
}

data class TactTyMap(
    val key: TactTy,
    val value: TactTy
) : TactTy {
    override fun toString(): String = "map<$key, $value>"

    override fun isAssignable(other: TactTy): Boolean {
        return other is TactTyMap && key.isAssignable(other.key) && value.isAssignable(other.value)
    }
}

data class TactBounced(
    val inner: TactTy
) : TactTy, TactTyRuntime {
    override fun toString(): String = "bounced<$inner>"

    override fun isAssignable(other: TactTy): Boolean {
        return other is TactBounced && inner.isAssignable(other.inner)
    }
}

object TactTyVoid : TactTy {
    override fun toString(): String = "<void>"
    override fun isAssignable(other: TactTy): Boolean = false
}

object TactTyNull : TactTy, TactTyRuntime {
    override fun toString(): String = "<null>"

    override fun isAssignable(other: TactTy): Boolean = other == TactTyNull || other is TactTyNullable
}
