package org.ton.intellij.tolk.doc

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import org.ton.intellij.tolk.ide.colors.TolkColor
import org.ton.intellij.tolk.psi.TolkFile
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkStruct
import org.ton.intellij.tolk.psi.TolkTypeDef
import org.ton.intellij.tolk.psi.TolkTypedElement
import org.ton.intellij.tolk.psi.impl.hasReceiver
import org.ton.intellij.tolk.psi.impl.receiverTy
import org.ton.intellij.tolk.psi.impl.structFields
import org.ton.intellij.tolk.stub.index.TolkNamedElementIndex
import org.ton.intellij.util.doc.DocumentationUtils

object TolkDocumentationUtils : DocumentationUtils() {
    val asKeyword get() = loadKey(TolkColor.KEYWORD.textAttributesKey)
    val asIdentifier get() = loadKey(DefaultLanguageHighlighterColors.IDENTIFIER)
    val asParameter get() = loadKey(TolkColor.PARAMETER.textAttributesKey)
    val asTypeParameter get() = loadKey(TolkColor.TYPE_PARAMETER.textAttributesKey)
    val asConstant get() = loadKey(TolkColor.CONSTANT.textAttributesKey)
    val asGlobalVariable get() = loadKey(TolkColor.GLOBAL_VARIABLE.textAttributesKey)
    val asString get() = loadKey(TolkColor.STRING.textAttributesKey)
    val asNumber get() = loadKey(TolkColor.NUMBER.textAttributesKey)
    val asField get() = loadKey(TolkColor.FIELD.textAttributesKey)
    val asParen get() = loadKey(TolkColor.PARENTHESES.textAttributesKey)
    val asComma get() = loadKey(TolkColor.COMMA.textAttributesKey)
    val asDot get() = loadKey(TolkColor.DOT.textAttributesKey)

    val asFunction get() = loadKey(TolkColor.FUNCTION.textAttributesKey)
    val asAnnotation get() = loadKey(TolkColor.ANNOTATION.textAttributesKey)
    val asStruct get() = loadKey(TolkColor.STRUCT.textAttributesKey)
    val asTypeAlias get() = loadKey(TolkColor.TYPE_ALIAS.textAttributesKey)
    val asPrimitive get() = loadKey(TolkColor.PRIMITIVE.textAttributesKey)
}

fun resolveDocumentationReference(name: String, owner: PsiElement): PsiElement? {
    val containingFile = owner.containingFile as? TolkFile ?: return null

    if (name.contains(".")) {
        // `Foo.name`, field or method
        val parts = name.split(".")
        if (parts.size != 2) {
            // unexpected
            return null
        }

        val typeName = parts[0]
        val methodOrFieldName = parts[1]

        val typeReference = resolveDocumentationTypeReference(containingFile, typeName) ?: return null
        if (typeReference is TolkStruct) {
            val fields = typeReference.structFields
            for (field in fields) {
                if (field.name == methodOrFieldName) {
                    return field
                }
            }
        }

        val searchReceiverType = (typeReference as? TolkTypedElement)?.type ?: return null

        var foundElement: PsiElement? = null

        TolkNamedElementIndex.processAllElements(owner.project, GlobalSearchScope.allScope(owner.project), Processor { element ->
            if (element !is TolkFunction) return@Processor true
            if (!element.hasReceiver) return@Processor true
            if (element.name != methodOrFieldName) return@Processor true

            val receiverType = element.receiverTy
            if (receiverType.isEquivalentTo(searchReceiverType)) {
                foundElement = element
                return@Processor false
            }

            true
        })

        return foundElement
    }

    if (owner is TolkFunction) {
        val params = owner.parameterList?.parameterList ?: emptyList()
        for (param in params) {
            if (param.name == name) {
                return param
            }
        }
        val typeParams = owner.typeParameterList?.typeParameterList ?: emptyList()
        for (param in typeParams) {
            if (param.name == name) {
                return param
            }
        }
    }

    if (owner is TolkStruct) {
        val typeParams = owner.typeParameterList?.typeParameterList ?: emptyList()
        for (param in typeParams) {
            if (param.name == name) {
                return param
            }
        }

        val fields = owner.structFields
        for (field in fields) {
            if (field.name == name) {
                return field
            }
        }
    }

    if (owner is TolkTypeDef) {
        val typeParams = owner.typeParameterList?.typeParameterList ?: emptyList()
        for (param in typeParams) {
            if (param.name == name) {
                return param
            }
        }
    }

    for (func in containingFile.functions) {
        if (func.name == name) {
            return func
        }
    }
    for (constant in containingFile.constVars) {
        if (constant.name == name) {
            return constant
        }
    }
    for (global in containingFile.globalVars) {
        if (global.name == name) {
            return global
        }
    }

    val typeReference = resolveDocumentationTypeReference(containingFile, name)
    if (typeReference != null) {
        return typeReference
    }

    return null
}

private fun resolveDocumentationTypeReference(containingFile: TolkFile, name: String): PsiElement? {
    for (alias in containingFile.typeDefs) {
        if (alias.name == name) {
            return alias
        }
    }
    for (struct in containingFile.structs) {
        if (struct.name == name) {
            return struct
        }
    }
    return null
}
