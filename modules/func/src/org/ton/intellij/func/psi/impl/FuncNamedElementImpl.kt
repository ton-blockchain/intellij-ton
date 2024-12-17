package org.ton.intellij.func.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import org.ton.intellij.func.FuncIcons
import org.ton.intellij.func.psi.*
import org.ton.intellij.func.stub.FuncNamedStub
import javax.swing.Icon

abstract class FuncNamedElementImpl<T : FuncNamedStub<*>> : FuncStubbedElementImpl<T>, FuncNamedElement {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)
    constructor(node: ASTNode) : super(node)

    override fun setName(name: String): PsiElement {
        identifier?.replace(FuncPsiFactory[project].createIdentifier(name))
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

            override fun getIcon(unused: Boolean): Icon? = when (this@FuncNamedElementImpl) {
                is FuncFunction -> FuncIcons.FUNCTION
                is FuncFunctionParameter -> FuncIcons.PARAMETER
                is FuncConstVar -> FuncIcons.CONSTANT
                is FuncGlobalVar -> FuncIcons.GLOBAL_VARIABLE
                else -> null
            }
        }
    }
}
