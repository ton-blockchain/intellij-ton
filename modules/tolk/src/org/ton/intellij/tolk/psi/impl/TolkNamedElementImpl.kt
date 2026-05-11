@file:Suppress("DEPRECATION")

package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkNamedStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyFunction
import org.ton.intellij.tolk.type.render
import javax.swing.Icon

abstract class TolkNamedElementImpl<T : TolkNamedStub<*>> :
    TolkStubbedElementImpl<T>,
    TolkNamedElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun setName(name: String): PsiElement {
        identifier?.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: 0

    override fun getName(): String? = greenStub?.name ?: identifier?.text?.removeSurrounding("`")

    override val rawName: String? get() = greenStub?.name ?: identifier?.text

    override val isDeprecated: Boolean
        get() {
            val stub = stub
            if (stub != null) {
                return stub.isDeprecated
            }
            return (this as? TolkAnnotationHolder)?.annotations?.hasDeprecatedAnnotation() ?: false
        }

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun getIcon(flags: Int): Icon? = super.getIcon(flags) ?: super.getBaseIcon()

    override fun toString(): String = "${greenStub?.stubType ?: node.elementType}:$name"

    override fun getPresentation(): ItemPresentation? = TolkNamedElementPresentation(this)
}

private class TolkNamedElementPresentation(private val element: TolkNamedElementImpl<*>) : ItemPresentation {
    override fun getLocationString(): String = element.containingFile.name

    override fun getPresentableText(): String? = when (element) {
        is TolkFunction -> buildString {
            if (element.hasReceiver) {
                append(element.receiverTy.render())
                append(".")
            }
            append(element.name)
            append("(")
            var separator = ""
            element.parameterList?.parameterList?.forEach { parameter ->
                append(separator)
                append((parameter.type ?: TolkTy.Unknown).render())
                separator = ", "
            }
            (element.type as? TolkTyFunction)?.returnType?.let {
                append("): ")
                append(it.render())
            } ?: append("): void")
        }

        is TolkConstVar -> buildString {
            append(element.name)
            append(": ")
            append((element.type ?: TolkTy.Unknown).render())
        }

        is TolkGlobalVar -> buildString {
            append(element.name)
            append(": ")
            append((element.type ?: TolkTy.Unknown).render())
        }

        is TolkStructField -> buildString {
            append(element.name)
            append(": ")
            append((element.type ?: TolkTy.Unknown).render())
        }

        is TolkEnumMember -> buildString {
            append(element.name)

            if (element.expression != null) {
                append(" = ")
                append(element.expression?.text)
            }
        }

        is TolkTypeDef -> element.name
        else -> element.name
    }

    override fun getIcon(unused: Boolean): Icon? = element.getIcon(0)
}
