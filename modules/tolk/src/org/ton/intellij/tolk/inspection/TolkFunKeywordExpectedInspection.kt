package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.*
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.openapi.project.Project
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.TolkFunction
import org.ton.intellij.tolk.psi.TolkVisitor
import org.ton.intellij.tolk.psi.impl.getKeyword
import org.ton.intellij.tolk.psi.impl.isGetMethod
import org.ton.intellij.util.document
import org.ton.intellij.util.textRangeInParent

class TolkFunKeywordExpectedInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(holder: ProblemsHolder, session: LocalInspectionToolSession): TolkVisitor {
        return object : TolkVisitor() {
            override fun visitFunction(o: TolkFunction) {
                if (!o.isGetMethod || o.funKeyword != null) return
                val getKeyword = o.getKeyword ?: return
                val nextAfterGet = getKeyword.treeNext ?: getKeyword
                holder.registerProblem(
                    o,
                    TolkBundle.message("inspection.message.fun_keyword_expected"),
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    nextAfterGet.textRangeInParent,
                    AddFunKeywordQuickFix()
                )
            }
        }
    }

    private class AddFunKeywordQuickFix : LocalQuickFix {
        override fun getFamilyName(): @IntentionFamilyName String =
            TolkBundle.message("intention.family.name.add_fun_keyword")

        override fun applyFix(
            project: Project,
            descriptor: ProblemDescriptor
        ) {
            val function = descriptor.psiElement as? TolkFunction ?: return
            val getKeyword = function.getKeyword ?: return
//            val anchorBefore = (function.functionReceiver ?: function.identifier)?.node?.let {
//                val prev = it.treePrev
//                if (prev != null && prev.elementType == TokenType.WHITE_SPACE) {
//                    prev
//                } else {
//                    it
//                }
//            } ?: return
//            val functionNode = function.node
            val document = function.containingFile.document ?: return
            document.insertString(getKeyword.startOffset + getKeyword.textLength, " fun")
        }
    }
}
