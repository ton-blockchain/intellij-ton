package org.ton.intellij.tlb.inspection.fix

import com.intellij.codeInspection.LocalQuickFixOnPsiElement
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.ton.intellij.tlb.ConstructorTag
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbConstructorTag
import org.ton.intellij.tlb.psi.tlbPsiFactory

class TlbSetConstructorTagFix(constructor: TlbConstructor, private val tag: ConstructorTag) :
    LocalQuickFixOnPsiElement(constructor) {
    override fun getText(): String = "Set '$tag' constructor tag"

    override fun getFamilyName(): @IntentionFamilyName String = "Set constructor tag"

    override fun invoke(
        project: Project,
        file: PsiFile,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        startElement as? TlbConstructor ?: return
        val constructorTag = project.tlbPsiFactory.createFromText<TlbConstructorTag>("foo$tag = Foo;")
        if (constructorTag == null) {
            println("Failed to create constructor tag")
            return
        }
        val currentConstructorTag = startElement.constructorTag
        if (currentConstructorTag != null) {
            currentConstructorTag.replace(constructorTag)
        } else {
            startElement.addAfter(constructorTag, startElement.identifier)
        }
    }
}