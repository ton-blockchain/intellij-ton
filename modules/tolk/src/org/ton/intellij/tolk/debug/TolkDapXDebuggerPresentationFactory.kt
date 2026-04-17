@file:Suppress("UnstableApiUsage")

package org.ton.intellij.tolk.debug

import com.intellij.icons.AllIcons
import com.intellij.platform.dap.DapCommandProcessor
import com.intellij.platform.dap.DapLazyVariable
import com.intellij.platform.dap.DapScope
import com.intellij.platform.dap.DapStackFrame
import com.intellij.platform.dap.DapStructuredVariable
import com.intellij.platform.dap.DapThread
import com.intellij.platform.dap.DapVariable
import com.intellij.platform.dap.StackFrameType
import com.intellij.platform.dap.ValueKind
import com.intellij.platform.dap.xdebugger.AbstractDapXValue
import com.intellij.platform.dap.xdebugger.DapXDebuggerPresentationFactory
import com.intellij.platform.dap.xdebugger.DapXSuspendContext
import com.intellij.platform.dap.xdebugger.DefaultDapXExecutionStack
import com.intellij.platform.dap.xdebugger.DefaultDapXScope
import com.intellij.platform.dap.xdebugger.DefaultDapXStackFrame
import com.intellij.platform.dap.xdebugger.DefaultDapXSuspendContext
import com.intellij.ui.ColoredTextContainer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.xdebugger.XSourcePosition
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator
import com.intellij.xdebugger.frame.XCompositeNode
import com.intellij.xdebugger.frame.XExecutionStack
import com.intellij.xdebugger.frame.XFullValueEvaluator
import com.intellij.xdebugger.frame.XNamedValue
import com.intellij.xdebugger.frame.XStackFrame
import com.intellij.xdebugger.frame.XValueGroup
import com.intellij.xdebugger.frame.XValueNode
import com.intellij.xdebugger.frame.XValuePlace
import com.intellij.xdebugger.frame.presentation.XRegularValuePresentation
import com.intellij.xdebugger.frame.presentation.XValuePresentation
import org.ton.intellij.tolk.TolkIcons
import javax.swing.Icon

internal object TolkDapXDebuggerPresentationFactory : DapXDebuggerPresentationFactory {
    override fun createSuspendContext(
        commandProcessor: DapCommandProcessor,
        threads: List<DapThread>,
        activeThread: DapThread?,
    ): DapXSuspendContext {
        val resolvedActiveThread = activeThread ?: threads.firstOrNull()
            ?: error("Active DAP thread is not available")
        return DefaultDapXSuspendContext(this, commandProcessor, threads, resolvedActiveThread)
    }

    override fun createExecutionStack(
        commandProcessor: DapCommandProcessor,
        thread: DapThread,
        isActive: Boolean,
    ): XExecutionStack = DefaultDapXExecutionStack(this, commandProcessor, thread, isActive)

    override fun createStackFrame(
        commandProcessor: DapCommandProcessor,
        thread: DapThread,
        frame: DapStackFrame,
    ): XStackFrame = TolkDapXStackFrame(this, commandProcessor, thread, frame)

    override fun createScope(commandProcessor: DapCommandProcessor, scope: DapScope, index: Int): XValueGroup =
        TolkDapXScope(this, commandProcessor, scope, index)

    override fun createValue(commandProcessor: DapCommandProcessor, variable: DapVariable, icon: Icon?): XNamedValue =
        TolkDapXValue(this, commandProcessor, variable, icon)
}

private class TolkDapXScope(
    factory: DapXDebuggerPresentationFactory,
    commandProcessor: DapCommandProcessor,
    private val scope: DapScope,
    private val index: Int,
) : XValueGroup(scope.name) {
    private val delegate = DefaultDapXScope(factory, commandProcessor, scope, index)

    override fun computeChildren(node: XCompositeNode) {
        delegate.computeChildren(node)
    }

    override fun isAutoExpand(): Boolean = TolkDapPresentationFormatter.shouldAutoExpandScope(
        name = scope.name,
        index = index,
        expensive = scope.isExpensive,
    )
}

internal class TolkDapXStackFrame(
    factory: DapXDebuggerPresentationFactory,
    private val commandProcessor: DapCommandProcessor,
    private val thread: DapThread,
    private val frame: DapStackFrame,
) : XStackFrame() {
    private val delegate = DefaultDapXStackFrame(factory, commandProcessor, thread, frame)

    override fun getEvaluator(): XDebuggerEvaluator = delegate.evaluator

    override fun computeChildren(node: XCompositeNode) {
        delegate.computeChildren(node)
    }

    override fun customizePresentation(component: ColoredTextContainer) {
        val nameAttributes =
            if (frame.type ==
                StackFrameType.Label
            ) {
                SimpleTextAttributes.GRAYED_ATTRIBUTES
            } else {
                SimpleTextAttributes.REGULAR_ATTRIBUTES
            }
        val locationAttributes = SimpleTextAttributes.GRAYED_ATTRIBUTES
        val frameName = TolkDapPresentationFormatter.formatFrameName(frame.name)
        val location = TolkDapPresentationFormatter.formatFrameLocation(
            sourceName = frame.source?.name,
            line = frame.startPosition.line,
            column = frame.startPosition.column,
        )

        component.append(frameName, nameAttributes)
        if (location != null) {
            component.append("  $location", locationAttributes)
        }
        component.setIcon(
            when (frame.type) {
                StackFrameType.Normal -> TolkIcons.FUNCTION
                StackFrameType.Label -> AllIcons.Nodes.Tag
            },
        )
    }

    override fun getSourcePosition(): XSourcePosition? = delegate.sourcePosition

    override fun getEqualityObject(): Any? = delegate.equalityObject
}

private class TolkDapXValue(
    factory: DapXDebuggerPresentationFactory,
    commandProcessor: DapCommandProcessor,
    variable: DapVariable,
    icon: Icon?,
) : AbstractDapXValue(factory, commandProcessor, variable, icon) {
    override fun computePresentation(node: XValueNode, place: XValuePlace) {
        val dapVariable = variable
        val state = createPresentationState(
            variable = dapVariable,
            hasChildren = dapVariable is DapStructuredVariable,
        )
        node.setPresentation(
            icon ?: resolveDefaultIcon(dapVariable),
            createValuePresentation(dapVariable, state.hasChildren, dapVariable is DapLazyVariable),
            state.hasChildren,
        )

        if (dapVariable is DapLazyVariable) return

        val fullValueText = state.fullValueText
        if (fullValueText != null) {
            node.setFullValueEvaluator(TolkFullValueEvaluator(fullValueText))
        }
    }

    override fun createValuePresentation(
        variable: DapVariable,
        hasChildren: Boolean,
        canNavigateToSource: Boolean,
    ): XValuePresentation {
        val state = createPresentationState(variable, hasChildren)

        return when {
            state.commentOnly ||
                state.commentSuffix != null ||
                state.numericValue ||
                state.stringValue ||
                state.keywordValue -> TolkStyledValuePresentation(
                valueText = state.valueText,
                typeText = state.typeText,
                commentSuffix = state.commentSuffix,
                commentOnly = state.commentOnly,
                numericValue = state.numericValue,
                stringValue = state.stringValue,
                keywordValue = state.keywordValue,
            )
            state.typeText.isNullOrEmpty() -> XRegularValuePresentation(state.valueText, null)
            state.valueText.isEmpty() -> XRegularValuePresentation(state.typeText, null)
            else -> XRegularValuePresentation(state.valueText, state.typeText)
        }
    }

    private fun createPresentationState(variable: DapVariable, hasChildren: Boolean): TolkValuePresentationState {
        val structuredVariable = variable as? DapStructuredVariable
        val display = TolkDapPresentationFormatter.formatVariableDisplay(
            rawValue = variable.value,
            hasChildren = hasChildren,
            namedChildren = structuredVariable?.namedVariables,
            indexedChildren = structuredVariable?.indexedVariables,
        )
        val typeText = TolkDapPresentationFormatter.formatVariableType(variable.type)
        val stringValue = TolkDapPresentationFormatter.isStringValue(display.valueText)
        val numericValue = TolkDapPresentationFormatter.isNumericValue(display.valueText)
        val keywordValue = TolkDapPresentationFormatter.isKeywordValue(display.valueText)
        val valueText =
            if (!stringValue && display.valueText.length > XValueNode.MAX_VALUE_LENGTH) {
                truncateInlineValue(display.valueText)
            } else {
                display.valueText
            }
        val fullValueText = variable.value?.takeIf {
            !hasChildren &&
                !display.commentOnly &&
                when {
                    stringValue -> display.valueText.length > XValueNode.MAX_VALUE_LENGTH + 2
                    else -> display.valueText.length > XValueNode.MAX_VALUE_LENGTH
                }
        }

        return TolkValuePresentationState(
            valueText = valueText,
            typeText = typeText,
            commentSuffix = display.commentSuffix,
            commentOnly = display.commentOnly,
            numericValue = numericValue,
            stringValue = stringValue,
            keywordValue = keywordValue,
            fullValueText = fullValueText,
            hasChildren = hasChildren,
        )
    }

    private fun truncateInlineValue(valueText: String): String {
        if (valueText.length <= XValueNode.MAX_VALUE_LENGTH) return valueText
        return valueText.take(XValueNode.MAX_VALUE_LENGTH - INLINE_VALUE_SUFFIX.length) + INLINE_VALUE_SUFFIX
    }

    private fun resolveDefaultIcon(variable: DapVariable): Icon = when (variable.kind) {
        is ValueKind.Method -> AllIcons.Nodes.Lambda
        is ValueKind.Class -> AllIcons.Nodes.Class
        is ValueKind.Property -> AllIcons.Nodes.Field
        else -> when {
            variable is DapStructuredVariable && variable.indexedVariables > 0 -> AllIcons.Debugger.Db_array
            variable is DapStructuredVariable -> AllIcons.Debugger.Value
            else -> AllIcons.Debugger.Db_primitive
        }
    }
}

private class TolkStyledValuePresentation(
    private val valueText: String,
    private val typeText: String?,
    private val commentSuffix: String?,
    private val commentOnly: Boolean,
    private val numericValue: Boolean,
    private val stringValue: Boolean,
    private val keywordValue: Boolean,
) : XValuePresentation() {
    override fun renderValue(renderer: XValueTextRenderer) {
        if (commentOnly) {
            renderer.renderComment(valueText)
            return
        }

        if (stringValue) {
            renderer.renderStringValue(
                valueText.substring(1, valueText.length - 1),
                "\"\\",
                XValueNode.MAX_VALUE_LENGTH,
            )
        } else if (numericValue) {
            renderer.renderNumericValue(valueText)
        } else if (keywordValue) {
            renderer.renderKeywordValue(valueText)
        } else {
            renderer.renderValue(valueText)
        }
        if (commentSuffix != null) {
            renderer.renderComment(commentSuffix)
        }
    }

    override fun getType(): String? = typeText
}

private class TolkFullValueEvaluator(private val fullValueText: String) :
    XFullValueEvaluator(XValueNode.MAX_VALUE_LENGTH) {
    override fun startEvaluation(callback: XFullValueEvaluationCallback) {
        callback.evaluated(fullValueText)
    }
}

private data class TolkValuePresentationState(
    val valueText: String,
    val typeText: String?,
    val commentSuffix: String?,
    val commentOnly: Boolean,
    val numericValue: Boolean,
    val stringValue: Boolean,
    val keywordValue: Boolean,
    val fullValueText: String?,
    val hasChildren: Boolean,
)

private const val INLINE_VALUE_SUFFIX = "..."
