package org.ton.intellij.tolk.psi.impl

import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.psi.search.SearchScope
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tolk.TolkIcons
import org.ton.intellij.tolk.psi.TolkAnnotation
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.stub.TolkTypeDefStub
import org.ton.intellij.tolk.type.TolkTy
import org.ton.intellij.tolk.type.TolkTypeAliasTy
import org.ton.intellij.tolk.type.render
import org.ton.intellij.util.recursionGuard
import javax.swing.Icon

abstract class TolkTypeMixin : TolkNamedElementImpl<TolkTypeDefStub>, TolkTypeDef, TolkTypedElement {
    constructor(node: ASTNode) : super(node)

    constructor(stub: TolkTypeDefStub, stubType: IStubElementType<*, *>) : super(stub, stubType)

    override fun getUseScope(): SearchScope {
        return super.getUseScope()
    }

    override val type: TolkTypeAliasTy
        get() {
            val typeExpressionType = recursionGuard(this) {
                typeExpression?.type
            } ?: TolkTy.Unknown
            return TolkTypeAliasTy(this, typeExpressionType)
        }

    override val isDeprecated: Boolean
        get() = greenStub?.isDeprecated ?: super.isDeprecated

    override fun getBaseIcon() = TolkIcons.TYPE_ALIAS

    override fun getIcon(flags: Int) = getBaseIcon()

    override fun getPresentation(): ItemPresentation? {
        return object : ItemPresentation {
            override fun getPresentableText(): String = buildString {
                append(name)
                type.let {
                    append(" = ")
                    it.underlyingType.render()
                }
            }

            override fun getIcon(unused: Boolean): Icon = getBaseIcon()
        }
    }
}

val TolkTypeDef.annotationList
    get() = PsiTreeUtil.getChildrenOfTypeAsList(this, TolkAnnotation::class.java)
