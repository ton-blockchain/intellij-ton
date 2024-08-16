package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.stub.TolkNamedStub
import javax.swing.Icon

abstract class TolkNamedElementImpl<T : TolkNamedStub<*>> : TolkStubbedElementImpl<T>, TolkNamedElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun setName(name: String): PsiElement {
        identifier?.replace(TolkPsiFactory[project].createIdentifier(name))
        return this
    }

    override fun getTextOffset(): Int = identifier?.textOffset ?: 0

    override fun getName(): String? = stub?.name ?: identifier?.text

    override fun getNameIdentifier(): PsiElement? = identifier

    override fun getPresentation(): ItemPresentation? {
        return object : ItemPresentation {
            override fun getPresentableText(): String? = name

            override fun getLocationString(): String {
                val fileName = containingFile.name
                return "in $fileName"
            }

            override fun getIcon(unused: Boolean): Icon? = when (this@TolkNamedElementImpl) {
                is TolkFunction -> TolkIcons.FUNCTION
                is TolkFunctionParameter -> TolkIcons.PARAMETER
                is TolkConstVar -> TolkIcons.CONSTANT
                is TolkGlobalVar -> TolkIcons.GLOBAL_VARIABLE
                else -> null
            }
        }
    }
}
