package org.ton.intellij.tolk.psi

import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.ton.intellij.util.stubParent

/**
 * A PSI element that holds modification tracker for some reason.
 * This is mostly used to invalidate cached type inference results.
 */
interface TolkModificationTrackerOwner : TolkElement {
    val modificationTracker: ModificationTracker

    /**
     * Increments local modification counter if needed.
     */
    fun incModificationCount(element: PsiElement): Boolean
}

fun PsiElement.findTolkModificationTrackerOwner(strict: Boolean): TolkModificationTrackerOwner? {
    var element = if (strict) stubParent else this
    while (element != null && element !is TolkModificationTrackerOwner) {
        element = element.stubParent
    }
    return element
}
