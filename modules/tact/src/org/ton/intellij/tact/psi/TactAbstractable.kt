package org.ton.intellij.tact.psi

import org.ton.intellij.util.parentOfType
import javax.swing.Icon

interface TactAbstractable : TactNameIdentifierOwner {
    val isAbstract: Boolean

    override fun getIcon(flags: Int): Icon
}

val TactAbstractable.superItem: TactAbstractable?
    get() {
        val name = name ?: return null
        val owner = parentOfType<TactTypeDeclarationElement>() ?: return null

        val filter = when (this) {
            is TactFunction -> {
                { element: TactNamedElement -> element is TactFunction && element.name == name }
            }

            is TactConstant -> {
                { element: TactNamedElement -> element is TactConstant && element.name == name }
            }

            else -> error("unreachable")
        }
        for (superTrait in owner.superTraits) {
            for (member in superTrait.members) {
                if (filter(member)) {
                    return member as TactAbstractable
                }
            }
        }

        return null
    }
