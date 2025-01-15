package org.ton.intellij.tolk.ide

// TODO: fix
//class TolkParameterInfoHandler : ParameterInfoHandler<TolkApplyExpression, List<String>> {
//    override fun findElementForParameterInfo(context: CreateParameterInfoContext): TolkApplyExpression? {
//        val element = findTolkApplyExpression(context.file, context.offset) ?: return null
//        val left = element.left as? TolkReferenceExpression ?: return null
//        val leftName = left.name ?: return null
//        val resolved = left.reference?.resolve() as? TolkFunction ?: return null
//        val specialCall = leftName.startsWith('.') || leftName.startsWith('~')
//        val params = resolved.functionParameterList.map { it.text }.let {
//            if (specialCall) it.drop(1) else it
//        }
//        context.itemsToShow = arrayOf(params)
//        return element
//    }
//
//    override fun findElementForUpdatingParameterInfo(context: UpdateParameterInfoContext): TolkApplyExpression? {
//        return findTolkApplyExpression(context.file, context.offset)
//    }
//
//    override fun updateUI(p: List<String>, context: ParameterInfoUIContext) {
//        val range = getArgumentRange(p, context.currentParameterIndex)
//        updateUI(presentText(p), range, context)
//    }
//
//    override fun updateParameterInfo(parameterOwner: TolkApplyExpression, context: UpdateParameterInfoContext) {
//        if (context.parameterOwner != parameterOwner) {
//            context.removeHint()
//            return
//        }
//        val currentParameterIndex = if (parameterOwner.startOffset == context.offset) {
//            -1
//        } else {
//            val right = parameterOwner.right
//            if (right != null) {
//                ParameterInfoUtils.getCurrentParameterIndex(right.node, context.offset, TolkElementTypes.COMMA)
//            } else {
//                -1
//            }
//        }
//        context.setCurrentParameter(currentParameterIndex)
//    }
//
//    override fun showParameterInfo(element: TolkApplyExpression, context: CreateParameterInfoContext) {
//        context.showHint(element, element.textRange.startOffset, this)
//    }
//
//    private fun findTolkApplyExpression(file: PsiFile, offset: Int): TolkApplyExpression? {
//        return file.findElementAt(offset)?.ancestorStrict<TolkApplyExpression>()
//    }
//
//    private fun updateUI(text: String, range: TextRange, context: ParameterInfoUIContext) {
//        context.setupUIComponentPresentation(
//            text,
//            range.startOffset,
//            range.endOffset,
//            !context.isUIComponentEnabled,
//            false,
//            false,
//            context.defaultParameterColor
//        )
//    }
//
//    private fun getArgumentRange(arguments: List<String>, index: Int): TextRange {
//        if (index < 0 || index >= arguments.size) return TextRange.EMPTY_RANGE
//        val start = arguments.take(index).sumOf { it.length + 2 }
//        val end = start + arguments[index].length
//        return TextRange(start, end)
//    }
//
//    private fun presentText(params: List<String>): String {
//        return if (params.isEmpty()) "<no arguments>" else params.joinToString(", ")
//    }
//}
