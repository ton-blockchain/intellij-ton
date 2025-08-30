package org.ton.intellij.func.template

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import org.ton.intellij.func.psi.FuncBlockStatement
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.util.prevVisibleOrNewLine

class FuncTopLevelContextType : FuncTemplateContextType("FunC (top level)") {
    override fun isInContext(o: PsiElement): Boolean {
        val prevSibling = o.prevVisibleOrNewLine
        if (prevSibling?.textContains('\n') != true) {
            // case like:
            // int <caret>
            return false
        }

        if (o.parentOfType<FuncBlockStatement>() != null) {
            return false
        }

        return o.parentOfType<FuncFile>() != null
    }
}
