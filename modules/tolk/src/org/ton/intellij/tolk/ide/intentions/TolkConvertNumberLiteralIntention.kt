@file:Suppress("UnstableApiUsage")

package org.ton.intellij.tolk.ide.intentions

import com.intellij.codeInsight.intention.LowPriorityAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.codeInsight.intention.preview.IntentionPreviewInfo
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.popup.PopupStep
import com.intellij.openapi.ui.popup.util.BaseListPopupStep
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.util.ThrowableRunnable
import org.ton.intellij.tolk.TolkBundle
import org.ton.intellij.tolk.psi.TolkLiteralExpression
import org.ton.intellij.tolk.psi.TolkPsiFactory
import java.math.BigInteger

class TolkConvertNumberLiteralIntention : PsiElementBaseIntentionAction(), LowPriorityAction {
    override fun getFamilyName(): String = TolkBundle.message("intention.convert.number.literal.family.name")
    override fun getText(): String = TolkBundle.message("intention.convert.number.literal.text")

    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        val literalExpression = findLiteralExpression(element) ?: return false
        return literalExpression.integerLiteral != null
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val literalExpression = findLiteralExpression(element) ?: return
        val integerLiteral = literalExpression.integerLiteral ?: return

        val value = parseIntegerLiteral(integerLiteral.text) ?: return

        val conversions = buildList {
            val originalText = integerLiteral.text
            val isNegative = value < BigInteger.ZERO
            val absValue = value.abs()

            if (!originalText.startsWith("0b") && !originalText.startsWith("0B")) {
                val binaryStr = absValue.toString(2)
                val binaryText = if (isNegative) "-0b$binaryStr" else "0b$binaryStr"
                add(
                    ConversionOption(
                        TolkBundle.message("intention.convert.number.to.binary.text", binaryText),
                        binaryText
                    )
                )
            }

            if (!originalText.startsWith("0x") && !originalText.startsWith("0X")) {
                val hexStr = absValue.toString(16)
                val hexText = if (isNegative) "-0x$hexStr" else "0x$hexStr"
                add(
                    ConversionOption(
                        TolkBundle.message("intention.convert.number.to.hex.text", hexText),
                        hexText
                    )
                )
            }

            if (originalText.startsWith("0x") || originalText.startsWith("0X") ||
                originalText.startsWith("0b") || originalText.startsWith("0B")
            ) {
                val decimalText = value.toString()
                add(
                    ConversionOption(
                        TolkBundle.message("intention.convert.number.to.decimal.text", decimalText),
                        decimalText
                    )
                )
            }
        }

        if (conversions.isEmpty()) return

        if (conversions.size == 1) {
            applyConversion(project, literalExpression, conversions.first().text)
        } else {
            showConversionPopup(project, editor, literalExpression, conversions)
        }
    }

    override fun generatePreview(project: Project, editor: Editor, file: PsiFile): IntentionPreviewInfo {
        return IntentionPreviewInfo.EMPTY
    }

    private fun findLiteralExpression(element: PsiElement): TolkLiteralExpression? {
        var current: PsiElement? = element
        while (current != null) {
            if (current is TolkLiteralExpression) {
                return current
            }
            current = current.parent
        }
        return null
    }

    private fun parseIntegerLiteral(text: String): BigInteger? {
        return try {
            var cleanText = text.replace("_", "")
            val isNegative = cleanText.startsWith("-")
            if (isNegative) {
                cleanText = cleanText.substring(1)
            }

            val value = when {
                cleanText.startsWith("0x") || cleanText.startsWith("0X") -> BigInteger(cleanText.substring(2), 16)
                cleanText.startsWith("0b") || cleanText.startsWith("0B") -> BigInteger(cleanText.substring(2), 2)
                else                                                     -> BigInteger(cleanText)
            }

            if (isNegative) value.negate() else value
        } catch (_: NumberFormatException) {
            null
        }
    }

    private fun showConversionPopup(
        project: Project,
        editor: Editor?,
        literalExpression: TolkLiteralExpression,
        conversions: List<ConversionOption>,
    ) {
        val popup = JBPopupFactory.getInstance().createListPopup(
            object : BaseListPopupStep<ConversionOption>("Convert To:", conversions) {
                override fun getTextFor(value: ConversionOption): String = value.description

                override fun onChosen(selectedValue: ConversionOption, finalChoice: Boolean): PopupStep<*>? {
                    if (finalChoice) {
                        applyConversion(project, literalExpression, selectedValue.text)
                    }
                    return null
                }
            }
        )

        if (editor != null) {
            popup.showInBestPositionFor(editor)
        } else {
            popup.showCenteredInCurrentWindow(project)
        }
    }

    private fun applyConversion(project: Project, literalExpression: TolkLiteralExpression, newText: String) {
        val factory = TolkPsiFactory[project]
        val newLiteral = factory.createExpression(newText)

        WriteCommandAction.writeCommandAction(project)
            .withName("Convert ${literalExpression.text} to ${newLiteral.text}")
            .run(ThrowableRunnable {
                literalExpression.replace(newLiteral)
            })
    }

    private data class ConversionOption(
        val description: String,
        val text: String,
    )
}
