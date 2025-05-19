package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkNamedStub
import org.ton.intellij.tolk.type.TolkFunctionTy
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.render
import javax.swing.Icon

abstract class TolkNamedElementImpl<T : TolkNamedStub<*>> : TolkStubbedElementImpl<T>, TolkNamedElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun setName(name: String): PsiElement {
        identifier?.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: 0

    override fun getName(): String? = greenStub?.name ?: identifier?.text?.removeSurrounding("`")

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun getIcon(flags: Int): Icon? {
        return super.getIcon(flags) ?: super.getBaseIcon()
    }

    override fun toString(): String = "$elementType:$name"

    override fun getPresentation(): ItemPresentation? {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = when(this@TolkNamedElementImpl) {
                is TolkFunction -> buildString {
                    append(name)
                    append("(")
                    var separator = ""
                    parameterList?.parameterList?.forEach { parameter ->
                        append(separator)
                        append((parameter.type ?: TolkTy.Unknown).render())
                        separator = ", "
                    }
                    (type as? TolkFunctionTy)?.returnType?.let {
                        append("): ")
                        append(it.render())
                    } ?: append("): void")
                }
                is TolkConstVar -> buildString {
                    append(name)
                    append(": ")
                    append((type ?: TolkTy.Unknown).render())
                }
                is TolkGlobalVar -> buildString {
                    append(name)
                    append(": ")
                    append((type ?: TolkTy.Unknown).render())
                }
                is TolkTypeDef -> name
                else -> name
            }

//            override fun getIcon(unused: Boolean): Icon? = when (this@TolkNamedElementImpl) {
//                is TolkFunction -> TolkIcons.FUNCTION
//                is TolkParameter -> TolkIcons.PARAMETER
//                is TolkTypeParameter -> TolkIcons.PARAMETER
//                is TolkConstVar -> TolkIcons.CONSTANT
//                is TolkGlobalVar -> TolkIcons.GLOBAL_VARIABLE
//                else -> null
//            }

            override fun getIcon(unused: Boolean): Icon? = this@TolkNamedElementImpl.getIcon(0)
        }
    }
}
