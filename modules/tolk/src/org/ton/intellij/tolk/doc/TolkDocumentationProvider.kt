package org.ton.intellij.tolk.doc

import com.intellij.model.Pointer
import com.intellij.openapi.util.NlsContexts
import com.intellij.platform.backend.documentation.DocumentationResult
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.platform.backend.presentation.TargetPresentation
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkNamedElement

class TolkDocumentationProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {
        if (element is TolkFunction) {
            return TolkDocumentationTarget(element, originalElement)
        }
        return null
    }
}

@Suppress("UnstableApiUsage")
class TolkDocumentationTarget(
    val element: PsiElement,
    val originalElement: PsiElement?
) : DocumentationTarget {
    override fun createPointer(): Pointer<out DocumentationTarget> {
        val elementPtr = element.createSmartPointer()
        val originalElementPtr = originalElement?.createSmartPointer()
        return Pointer {
            val element = elementPtr.dereference() ?: return@Pointer null
            TolkDocumentationTarget(element, originalElementPtr?.dereference())
        }
    }

    override fun computePresentation(): TargetPresentation {
        val presentableText = (element as? TolkNamedElement)?.name ?: element.text
        return TargetPresentation.builder(presentableText)
            .icon(element.getIcon(0))
            .presentation()
    }

    override fun computeDocumentationHint(): @NlsContexts.HintText String? {
        return computeLocalDocumentation(element, originalElement, true)
    }

    override fun computeDocumentation(): DocumentationResult? {
        val html = computeLocalDocumentation(element, originalElement, false) ?: return null
        return DocumentationResult.documentation(html)
    }
}

private fun computeLocalDocumentation(
    element: PsiElement,
    originalElement: PsiElement?,
    quickNavigation: Boolean
): String? {
    when(element) {
        is TolkFunction -> {
            val doc = element.doc ?: return null
            return doc.text.replace("* ", "").removeSurrounding("/**", " */").trimIndent()
        }
    }

    return null
}
