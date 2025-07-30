package org.ton.intellij.tlb.psi

import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.Iconable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.ton.intellij.tlb.TlbIcons
import javax.swing.Icon

interface TlbElement : PsiElement
abstract class TlbElementImpl(node: ASTNode) : ASTWrapperPsiElement(node)

interface TlbNamedElement : TlbElement, PsiNameIdentifierOwner {
    val identifier: PsiElement?

    override fun getNameIdentifier(): PsiElement? = identifier
}


abstract class TlbNamedElementImpl(node: ASTNode) : TlbElementImpl(node), TlbNamedElement {
    override fun getName(): String? = nameIdentifier?.text

    override fun setName(name: String): PsiElement = apply {
        nameIdentifier?.replace(project.tlbPsiFactory.createIdentifier(name))
    }

    override fun getTextOffset(): Int = nameIdentifier?.textOffset ?: super.getTextOffset()

    override fun getIcon(flags: Int): Icon? {
        return when (this) {
            is TlbResultType  -> TlbIcons.TYPE
            is TlbField       -> TlbIcons.FIELD
            is TlbConstructor -> TlbIcons.CONSTRUCTOR
            else              -> return super.getIcon(flags)
        }
    }

    override fun getPresentation(): ItemPresentation? {
        return object : TlbItemPresentation<TlbNamedElement>(this) {
            override fun getPresentableText(): String? {
                if (element is TlbResultType) {
                    val declarationLines = element.parent.text.split("\n")
                    if (declarationLines.isEmpty()) {
                        return element.name
                    }

                    if (declarationLines.size == 1) {
                        // action_delete_ext#03 addr:MsgAddressInt = ExtendedAction;
                        return declarationLines.first()
                    }

                    // action_add_ext#02... = ExtendedAction;
                    val declarationText = declarationLines.first()
                    return declarationText + "... = " + element.name
                }

                return runReadAction {
                    element.name
                }
            }
        }
    }
}

abstract class TlbItemPresentation<T : TlbNamedElement>(protected val element: T) : ItemPresentation {
    private var locationString: String? = null

    override fun getLocationString(): String {
        return if (locationString != null) locationString!! else getLocationStringInner().also { locationString = it }
    }

    private fun getLocationStringInner(): String {
        val file = element.containingFile
        val fileName = file.name
        return "in $fileName"
    }

    override fun getIcon(b: Boolean): Icon =
        element.getIcon(Iconable.ICON_FLAG_VISIBILITY)
}


interface TlbFieldListOwner : TlbElement {
    val fieldList: TlbFieldList?
}

//interface TlbNaturalTypeExpression : TlbTypeExpression {
//
//}

fun TlbTypeExpression.unwrap(): TlbTypeExpression? {
    var current: TlbTypeExpression? = this
    while (current is TlbParenTypeExpression) {
        current = current.typeExpression
    }
    return current
}

fun TlbTypeExpression.naturalValue(): Int? {
    return when (this) {
        is TlbIntTypeExpression -> naturalValue()
        is TlbParenTypeExpression -> naturalValue()
        else -> null
    }
}

fun TlbTypeExpression.isNatural(): Boolean {
    when (val unwrapped = unwrap()) {
        is TlbApplyTypeExpression -> return unwrapped.isNatural()
        is TlbIntTypeExpression -> return true
        is TlbParamTypeExpression -> return unwrapped.isNatural()
        else -> {
            val naturalValue = unwrapped?.naturalValue()
            if (naturalValue == null) {
                return false
            }
            return false
        }
    }
}

fun TlbApplyTypeExpression.isNatural(): Boolean {
    return typeExpression.isNatural()
}

fun TlbParamTypeExpression.isNatural(): Boolean {
    return doubleTag != null ||
            natLeq != null ||
            natLess != null ||
            tag != null ||
            (reference?.resolve() as? TlbImplicitField)?.tag != null ||
            (identifier?.text?.let {
                it.startsWith("uint") || it.startsWith("int")
            } == true)
}

fun TlbParenTypeExpression.naturalValue(): Int? {
    return unwrap()?.naturalValue()
}

fun TlbIntTypeExpression.naturalValue(): Int? {
    return number.text.toIntOrNull()
}

fun TlbTypeExpression.isNegated(): Boolean {
    return unwrap() is TlbNegatedTypeExpression
}