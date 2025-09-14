package org.ton.intellij.tolk.inspection

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import org.ton.intellij.tolk.psi.*
import org.ton.intellij.tolk.psi.impl.hasPrivateFields
import org.ton.intellij.tolk.type.TolkTyNever
import org.ton.intellij.tolk.type.TolkTyParam
import org.ton.intellij.tolk.type.TolkTyStruct

class TolkStructInitializationInspection : TolkInspectionBase() {
    override fun buildTolkVisitor(
        holder: ProblemsHolder,
        session: LocalInspectionToolSession,
    ): TolkVisitor = object : TolkVisitor() {
        override fun visitStructExpression(expression: TolkStructExpression) {
            super.visitStructExpression(expression)
            checkStructInitialization(expression, holder)
        }
    }

    private fun checkStructInitialization(expression: TolkStructExpression, holder: ProblemsHolder) {
        val structDecl = resolveStructDeclaration(expression) ?: return

        if (structDecl.hasPrivateFields) {
            // Don't add an error in this case
            return
        }

        val requiredFields = structDecl.structBody?.structFieldList?.filter { !canFieldBeOmitted(it) } ?: emptyList()
        val initializedFields = getInitializedFields(expression)

        val missingFields = requiredFields.filter { field ->
            !initializedFields.contains(field.identifier.text.removeSurrounding("`"))
        }

        if (missingFields.isNotEmpty()) {
            val message = createErrorMessage(missingFields)
            // Foo { ... }
            // ^^^ this
            // or
            // { ... }
            // ^ this
            val anchor = expression.referenceTypeExpression ?: expression.firstChild?.firstChild ?: expression

            holder.registerProblem(anchor, message, ProblemHighlightType.GENERIC_ERROR)
        }
    }

    private fun resolveStructDeclaration(expression: TolkStructExpression): TolkStruct? {
        val type = expression.type?.actualType()?.unwrapTypeAlias() as? TolkTyStruct
        return type?.psi
    }

    private fun canFieldBeOmitted(field: TolkStructField): Boolean {
        if (field.expression != null) {
            return true
        }
        val fieldType = field.type
        if (fieldType is TolkTyNever) {
            return true
        }
        if (fieldType is TolkTyParam) {
            val defaultType = (fieldType.parameter as? TolkTyParam.NamedTypeParameter)?.psi?.defaultTypeParameter?.typeExpression?.type
            if (defaultType is TolkTyNever) {
                return true
            }
        }
        return false
    }

    private fun getInitializedFields(expression: TolkStructExpression): Set<String> {
        val fields = mutableSetOf<String>()
        val structBody = expression.structExpressionBody

        structBody.structExpressionFieldList.forEach { field ->
            val fieldName = field.identifier.text.removeSurrounding("`")
            fields.add(fieldName)
        }

        return fields
    }

    private fun createErrorMessage(missingFields: List<TolkStructField>): String {
        return when (missingFields.size) {
            1    -> "Field '${missingFields[0].identifier.text}' missed in initialization"
            else -> {
                val fieldNames = missingFields.joinToString(", ") { "'${it.identifier.text}'" }
                "Fields $fieldNames missed in initialization"
            }
        }
    }
}
