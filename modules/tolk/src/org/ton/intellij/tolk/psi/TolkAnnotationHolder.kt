package org.ton.intellij.tolk.psi

import com.intellij.psi.util.PsiTreeUtil

interface TolkAnnotationHolder : TolkElement {
    val annotationList: List<TolkAnnotation>
        get() = PsiTreeUtil.getChildrenOfTypeAsList(this, TolkAnnotation::class.java)

    val isDeprecated: Boolean
        get() = annotationList.hasDeprecatedAnnotation()
}

fun List<TolkAnnotation>.findAnnotation(name: String): TolkAnnotation? {
    return find { it.identifier?.text == name }
}

fun List<TolkAnnotation>.hasDeprecatedAnnotation(): Boolean = findAnnotation("deprecated") != null
