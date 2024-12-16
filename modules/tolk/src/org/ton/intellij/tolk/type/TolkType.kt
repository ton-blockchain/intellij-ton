package org.ton.intellij.tolk.type


import org.ton.intellij.tolk.psi.TolkTypeParameter
import org.ton.intellij.tolk.psi.TolkTypeParameterListOwner
import org.ton.intellij.util.parentOfType

sealed interface TolkType {
    class ParameterType(
        val psiElement: TolkTypeParameter
    ) : TolkNamedType {
        override val name: String
            get() = psiElement.name ?: ""

        val uniqueId by lazy {
            psiElement.parentOfType<TolkTypeParameterListOwner>()?.let {
                it.containingFile.name + "@" + it.name + "$" + name
            } ?: "$name@${psiElement.hashCode()}"
        }


        override fun toString(): String = name
    }

    class Function(
        val inputType: TolkType,
        val returnType: TolkType
    ) : TolkType {

        override fun toString(): String = "$inputType -> $returnType"
    }

    class Tensor constructor(
        val elements: List<TolkType>
    ) : TolkType {
        override fun toString(): String = "(${elements.joinToString()})"
    }

    class TypedTuple(
        val elements: List<TolkType>
    ) : TolkType {
        override fun toString(): String = "[${elements.joinToString()}]"
    }

    class HoleType(
//        type: TolkType? = null
    ) : TolkType {
//        var type: TolkType? = type
//            set(value) {
//                if (field == null) {
//                } else if (field != value) {
//                    field = value
//                } else {
//                    throw IllegalStateException("Rebinding auto type")
//                }
//            }
//
//        override fun removeHoles(): TolkType {
//            return type ?: throw IllegalStateException("Hole type is not bound")
//        }

        //        override fun toString(): String = type?.toString() ?: "_"
    }

    class UnionType(
        val elements: List<TolkType>
    ) : TolkType {
        override fun toString(): String = elements.joinToString(" | ")
    }

    companion object {
        val Int = TolkPrimitiveType.Int
        val Cell = TolkPrimitiveType.Cell
        val Builder = TolkPrimitiveType.Builder
        val Slice = TolkPrimitiveType.Slice
        val Continuation = TolkPrimitiveType.Continuation
        val Tuple = TolkPrimitiveType.Tuple
        val Unit = TolkPrimitiveType.Unit
        val Bool = TolkPrimitiveType.Bool

        fun create(vararg elements: TolkType): TolkType {
            return create(elements.toList())
        }

        fun create(elements: Collection<TolkType>): TolkType {
            return when {
                elements.isEmpty() -> Unit
                elements.size == 1 -> {
                    val element = elements.first()
                    if (element is Tensor) {
                        create(element.elements)
                    } else element
                }

                else -> Tensor(elements.toList())
            }
        }
    }
}

interface TolkNamedType : TolkType {
    val name: String
}

sealed class TolkPrimitiveType(
    override val name: String
) : TolkNamedType {
    object Int : TolkPrimitiveType("int")
    object Cell : TolkPrimitiveType("cell")
    object Builder : TolkPrimitiveType("builder")
    object Slice : TolkPrimitiveType("slice")
    object Continuation : TolkPrimitiveType("continuation")
    object Tuple : TolkPrimitiveType("tuple")
    object Unit : TolkPrimitiveType("()")
    object Bool : TolkPrimitiveType("bool")

    override fun toString(): String = name
}



