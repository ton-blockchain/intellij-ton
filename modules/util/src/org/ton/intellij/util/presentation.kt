package org.ton.intellij.util

import com.intellij.codeInsight.hints.declarative.InlayActionData
import com.intellij.codeInsight.hints.declarative.PresentationTreeBuilder
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionNavigationHandler
import com.intellij.codeInsight.hints.declarative.PsiPointerInlayActionPayload
import com.intellij.psi.PsiElement
import com.intellij.psi.createSmartPointer

fun PresentationTreeBuilder.printPsi(element: PsiElement, name: String) {
    text(
        name,
        InlayActionData(
            PsiPointerInlayActionPayload(element.createSmartPointer()),
            PsiPointerInlayActionNavigationHandler.HANDLER_ID
        )
    )
}