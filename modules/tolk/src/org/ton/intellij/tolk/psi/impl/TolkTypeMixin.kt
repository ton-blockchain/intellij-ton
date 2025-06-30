package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.doc.psi.TolkDocComment
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.stub.TolkTypeDefStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTyAlias
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.childOfType
import javax.swing.Icon

abstract class TolkTypeMixin : TolkNamedElementImpl<TolkTypeDefStub>, TolkTypeDef, TolkTypedElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkTypeDefStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getUseScope(): SearchScope {
        return super.getUseScope()
    }

    override val type: TolkTy get() = TolkTyAlias.create(this)

    override val doc: TolkDocComment? get() = childOfType()

    override fun getIcon(flags: Int) = TolkIcons.TYPE_ALIAS

    override fun getPresentation(): ItemPresentation? {
        return object : ItemPresentation {
            override fun getPresentableText(): String = buildString {
                append(name)
                type.let {
                    append(" = ")
                    val underlyingType = when(it) {
                        is TolkTyAlias -> it.underlyingType.render()
                        else -> it.render()
                    }
                    append(underlyingType)
                }
            }

            override fun getIcon(unused: Boolean): Icon = getIcon(0)
        }
    }
}

val TolkTypeDef.declaredType: TolkTy
    get() = TolkTyAlias.create(this)

val TolkTypeDef.annotationList
    get() = PsiTreeUtil.getChildrenOfTypeAsList(this, TolkAnnotation::class.java)
