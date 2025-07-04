package org.ton.intellij.tolk.psi

import com.intellij.psi.util.siblings

interface TolkAnnotationHolder : TolkNamedElement {
    val annotations get() = TolkAnnotationQuery(this)
}

class TolkAnnotationQuery(
    private val rawAnnotations: Sequence<TolkAnnotation>
) {
    constructor(holder: TolkAnnotationHolder) : this(
        holder.firstChild?.siblings()?.filterIsInstance<TolkAnnotation>() ?: emptySequence()
    )

    fun hasDeprecatedAnnotation(): Boolean = hasAnnotation("deprecated")

    fun deprecatedAnnotation(): TolkAnnotation? {
        return annotationByName("deprecated").firstOrNull()
    }

    fun hasAnnotation(annotationName: String): Boolean {
        val annotation = annotationByName(annotationName)
        return annotation.any()
    }

    fun annotationByName(name: String) = rawAnnotations.filter { it.identifier?.text == name }
}
