package org.ton.intellij.tolk.diagnostics

import com.intellij.codeInspection.ProblemHighlightType
import org.ton.intellij.tolk.inspection.TolkInspectionBase
import org.ton.intellij.tolk.psi.TolkElement
import org.ton.intellij.tolk.type.TolkType
import org.ton.intellij.util.PreparedAnnotation

class TolkTypeMismatchDiagnostic(
    element: TolkElement,
    val expected: TolkType,
    val actual: TolkType?,
) : TolkDiagnostic(element) {
    override fun prepare(): PreparedAnnotation {
        return PreparedAnnotation(
            ProblemHighlightType.ERROR,
            "Type mismatch: expected $expected, found $actual",
        )
    }

    override fun canApply(inspection: TolkInspectionBase): Boolean {
        return true
    }
}