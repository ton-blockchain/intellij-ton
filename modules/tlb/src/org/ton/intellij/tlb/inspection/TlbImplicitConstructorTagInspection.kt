package org.ton.intellij.tlb.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.tlb.ConstructorTag
import org.ton.intellij.tlb.computeTag
import org.ton.intellij.tlb.inspection.fix.TlbSetConstructorTagFix
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.TlbVisitor

class TlbImplicitConstructorTagInspection : TlbInspectionBase() {
    override fun buildTlbVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession
    ): TlbVisitor = object : TlbVisitor() {
        override fun visitConstructor(o: TlbConstructor) {
            val constructorTag = o.constructorTag
            if (constructorTag != null) {
                return
            }
            if (o.name == "_") {
                return
            }
            if (PsiTreeUtil.hasErrorElements(o)) {
                return
            }

            val fixes = ArrayList<LocalQuickFix>(2)
            fixes.add(TlbSetConstructorTagFix(o, ConstructorTag.EMPTY))

            val computedTag = o.computeTag()
            if (computedTag != null) {
                fixes.add(TlbSetConstructorTagFix(o, computedTag))
            }

            holder.registerProblem(
                o.identifier,
                "Constructor tag not defined",
                *fixes.toTypedArray()
            )
        }
    }
}