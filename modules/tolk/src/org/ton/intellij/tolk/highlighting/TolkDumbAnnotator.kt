package org.ton.intellij.tolk.highlighting

import com.intellij.lang.annotation.AnnotationHolder
import com.intellij.lang.annotation.Annotator
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.intellij.psi.util.parentOfType
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.isMethod
import org.ton.intellij.tolk.psi.impl.isMutable
import org.ton.intellij.tolk.psi.impl.isStatic
import org.ton.intellij.util.childOfType

class TolkDumbAnnotator : Annotator, DumbAware {
    override fun annotate(element: PsiElement, holder: AnnotationHolder) {
        val elementType = element.elementType ?: return
        val color = when (elementType) {
            TolkElementTypes.IDENTIFIER -> {
                val parent = element.parent
                if (parent is TolkReferenceElement) return
                identifierColor(parent) ?: return
            }
            TolkElementTypes.GET_KEYWORD,
            TolkElementTypes.LAZY_KEYWORD -> TolkColor.KEYWORD.textAttributesKey
            else -> return
        }
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(color)
            .create()
    }

    companion object {
        fun identifierColor(element: PsiElement): TextAttributesKey? {
            return when (element) {
                is TolkTypeParameter -> TolkColor.TYPE_PARAMETER.textAttributesKey
                is TolkReferenceTypeExpression -> {
                    if (element.parentOfType<TolkFunctionReceiver>() != null) {
                        TolkColor.TYPE_PARAMETER.textAttributesKey
                    } else {
                        null
                    }
                }

                is TolkAnnotation -> TolkColor.ANNOTATION.textAttributesKey
                is TolkGlobalVar -> TolkColor.GLOBAL_VARIABLE.textAttributesKey
                is TolkConstVar -> TolkColor.CONSTANT.textAttributesKey
                is TolkStruct -> {
                    val tag = element.structConstructorTag
                    val body = element.structBody
                    if (tag == null && (body == null || body.childOfType<TolkStructField>() == null)) {
                        TolkColor.EMPTY_STRUCT.textAttributesKey
                    } else {
                        TolkColor.STRUCT.textAttributesKey
                    }
                }
                is TolkTypeDef -> TolkColor.TYPE_ALIAS.textAttributesKey
                is TolkStructField -> TolkColor.FIELD.textAttributesKey
                is TolkStructExpressionField -> TolkColor.FIELD.textAttributesKey
                is TolkVar -> if (element.isMutable) {
                    TolkColor.MUTABLE_VARIABLE.textAttributesKey
                } else {
                    TolkColor.VARIABLE.textAttributesKey
                }
                is TolkCatchParameter -> TolkColor.VARIABLE.textAttributesKey
                is TolkSelfParameter -> TolkColor.SELF_PARAMETER.textAttributesKey
                is TolkParameter -> if (element.isMutable) {
                    TolkColor.MUT_PARAMETER.textAttributesKey
                } else {
                    TolkColor.PARAMETER.textAttributesKey
                }

                is TolkFunction -> {
                    when {
                        element.isMethod -> TolkColor.METHOD.textAttributesKey
                        element.isStatic -> TolkColor.STATIC_FUNCTION.textAttributesKey
                        else -> TolkColor.FUNCTION.textAttributesKey
                    }
                }

                else -> null
            }
        }
    }
}
