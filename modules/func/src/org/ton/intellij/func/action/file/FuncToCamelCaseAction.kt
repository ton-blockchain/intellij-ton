package org.ton.intellij.func.action.file

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.util.PsiTreeUtil
import org.ton.intellij.func.psi.FuncFile
import org.ton.intellij.func.psi.FuncFunction
import org.ton.intellij.func.psi.FuncPsiFactory
import org.ton.intellij.func.psi.FuncReferenceExpression

class FuncToCamelCaseAction : AnAction() {
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val file = e.getData(CommonDataKeys.PSI_FILE)
        e.presentation.isEnabledAndVisible = file is FuncFile
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val file = e.getData(CommonDataKeys.PSI_FILE) as? FuncFile ?: return

        WriteCommandAction.runWriteCommandAction(project) {
            PsiTreeUtil.collectElementsOfType(file, FuncFunction::class.java).forEach {
                val name = it.name
                if (name != null) {
                    val newName = transformString(name, 1)
                    if (newName != name) {
                        it.identifier.replace(FuncPsiFactory[project].createIdentifier(newName))
                    }
                }
            }
            PsiTreeUtil.collectElementsOfType(file, FuncReferenceExpression::class.java).forEach {
                val name = it.name
                if (name != null) {
                    val newName = transformString(name, 1)
                    if (newName != name) {
                        it.identifier.replace(FuncPsiFactory[project].createIdentifier(newName))
                    }
                }
            }
        }
    }
}

private val snakeMarks: String = "~_?!:'"
private val builtins = listOf(
    "divmod", "~divmod", "moddiv", "~moddiv", "muldiv", "muldivc", "muldivr", "muldivmod",
    "true", "false", "null", "nil", "Nil", "throw", "at",
    "touch", "~touch", "touch2", "~touch2", "~dump", "~strdump",
    "run_method0", "run_method1", "run_method2", "run_method3", "->"
)
private val snakeReplaceMap = mapOf(
    // MAIN
    "receiveInternalMessage" to "recv_internal",
    "receiveExternalMessage" to "recv_external",
    // CHECKS
    "isSliceEmpty" to "slice_empty?",
    "isSliceDataEmpty" to "slice_data_empty?",
    "isSliceRefsEmpty" to "slice_refs_empty?",
    "isDictEmpty" to "dict_empty?",
    "isCellNull" to "cell_null?",
    "isAddrNone" to "addr_none?",
    "isWorkchainsEqual" to "workchains_equal?",
    "isWorkchainMatch" to "workchain_match?",
    "isBasechainAddr" to "basechain_addr?",
    "isMasterchainAddr" to "masterchain_addr?",
    // QUIET FUNCS
    "tryComputeDataSize" to "compute_data_size?",
    "trySliceComputeDataSize" to "slice_compute_data_size?",
    "isSliceBeginsWith" to "slice_begins_with?",
    // DICTS
    // load
    "tryLoadDict" to "load_dict?",
    "~tryLoadDict" to "~load_dict?",
    "tryPreloadDict" to "preload_dict?",
    // get
    "tryDictGet" to "dict_get?",
    "tryUdictGet" to "udict_get?",
    "tryIdictGet" to "idict_get?",
    "tryDictGetRef" to "dict_get_ref?",
    "tryUdictGetRef" to "udict_get_ref?",
    "tryIdictGetRef" to "idict_get_ref?",
    // setget
    "tryDictSetGet" to "dict_set_get?",
    "tryUdictSetGet" to "udict_set_get?",
    "tryIdictSetGet" to "idict_set_get?",
    "~tryDictSetGet" to "~dict_set_get?",
    "~tryUdictSetGet" to "~udict_set_get?",
    "~tryIdictSetGet" to "~idict_set_get?",
    "tryDictSetGetRef" to "dict_set_get_ref?",
    "tryUdictSetGetRef" to "udict_set_get_ref?",
    "tryIdictSetGetRef" to "idict_set_get_ref?",
    "~tryDictSetGetRef" to "~dict_set_get_ref?",
    "~tryUdictSetGetRef" to "~udict_set_get_ref?",
    "~tryIdictSetGetRef" to "~idict_set_get_ref?",
    "tryDictSetGetBuilder" to "dict_set_get_builder?",
    "tryUdictSetGetBuilder" to "udict_set_get_builder?",
    "tryIdictSetGetBuilder" to "idict_set_get_builder?",
    "~tryDictSetGetBuilder" to "~dict_set_get_builder?",
    "~tryUdictSetGetBuilder" to "~udict_set_get_builder?",
    "~tryIdictSetGetBuilder" to "~idict_set_get_builder?",
    // delete
    "tryDictDelete" to "dict_delete?",
    "tryUdictDelete" to "udict_delete?",
    "tryIdictDelete" to "idict_delete?",
    "tryDictDeleteGet" to "dict_delete_get?",
    "tryUdictDeleteGet" to "udict_delete_get?",
    "tryIdictDeleteGet" to "idict_delete_get?",
    "~tryDictDeleteGet" to "~dict_delete_get?",
    "~tryUdictDeleteGet" to "~udict_delete_get?",
    "~tryIdictDeleteGet" to "~idict_delete_get?",
    "tryDictDeleteGetRef" to "dict_delete_get_ref?",
    "tryUdictDeleteGetRef" to "udict_delete_get_ref?",
    "tryIdictDeleteGetRef" to "idict_delete_get_ref?",
    "~tryDictDeleteGetRef" to "~dict_delete_get_ref?",
    "~tryUdictDeleteGetRef" to "~udict_delete_get_ref?",
    "~tryIdictDeleteGetRef" to "~idict_delete_get_ref?",
    "tryDictDeleteGetMin" to "dict_delete_get_min?",
    "tryUdictDeleteGetMin" to "udict_delete_get_min?",
    "tryIdictDeleteGetMin" to "idict_delete_get_min?",
    "~tryDictDeleteGetMin" to "~dict_delete_get_min?",
    "~tryUdictDeleteGetMin" to "~udict_delete_get_min?",
    "~tryIdictDeleteGetMin" to "~idict_delete_get_min?",
    "tryDictDeleteGetMax" to "dict_delete_get_max?",
    "tryUdictDeleteGetMax" to "udict_delete_get_max?",
    "tryIdictDeleteGetMax" to "idict_delete_get_max?",
    "~tryDictDeleteGetMax" to "~dict_delete_get_max?",
    "~tryUdictDeleteGetMax" to "~udict_delete_get_max?",
    "~tryIdictDeleteGetMax" to "~idict_delete_get_max?",
    "tryDictDeleteGetMinRef" to "dict_delete_get_min_ref?",
    "tryUdictDeleteGetMinRef" to "udict_delete_get_min_ref?",
    "tryIdictDeleteGetMinRef" to "idict_delete_get_min_ref?",
    "~tryDictDeleteGetMinRef" to "~dict_delete_get_min_ref?",
    "~tryUdictDeleteGetMinRef" to "~udict_delete_get_min_ref?",
    "~tryIdictDeleteGetMinRef" to "~idict_delete_get_min_ref?",
    "tryDictDeleteGetMaxRef" to "dict_delete_get_max_ref?",
    "tryUdictDeleteGetMaxRef" to "udict_delete_get_max_ref?",
    "tryIdictDeleteGetMaxRef" to "idict_delete_get_max_ref?",
    "~tryDictDeleteGetMaxRef" to "~dict_delete_get_max_ref?",
    "~tryUdictDeleteGetMaxRef" to "~udict_delete_get_max_ref?",
    "~tryIdictDeleteGetMaxRef" to "~idict_delete_get_max_ref?",
    // add
    "tryDictAdd" to "dict_add?",
    "tryUdictAdd" to "udict_add?",
    "tryIdictAdd" to "idict_add?",
    "tryDictAddBuilder" to "dict_add_builder?",
    "tryUdictAddBuilder" to "udict_add_builder?",
    "tryIdictAddBuilder" to "idict_add_builder?",
    "tryDictAddRef" to "dict_add_ref?",
    "tryUdictAddRef" to "udict_add_ref?",
    "tryIdictAddRef" to "idict_add_ref?",
    "tryDictAddGet" to "dict_add_get?",
    "tryUdictAddGet" to "udict_add_get?",
    "tryIdictAddGet" to "idict_add_get?",
    "~tryDictAddGet" to "~dict_add_get?",
    "~tryUdictAddGet" to "~udict_add_get?",
    "~tryIdictAddGet" to "~idict_add_get?",
    "tryDictAddGetRef" to "dict_add_get_ref?",
    "tryUdictAddGetRef" to "udict_add_get_ref?",
    "tryIdictAddGetRef" to "idict_add_get_ref?",
    "~tryDictAddGetRef" to "~dict_add_get_ref?",
    "~tryUdictAddGetRef" to "~udict_add_get_ref?",
    "~tryIdictAddGetRef" to "~idict_add_get_ref?",
    "tryDictAddGetBuilder" to "dict_add_get_builder?",
    "tryUdictAddGetBuilder" to "udict_add_get_builder?",
    "tryIdictAddGetBuilder" to "idict_add_get_builder?",
    "~tryDictAddGetBuilder" to "~dict_add_get_builder?",
    "~tryUdictAddGetBuilder" to "~udict_add_get_builder?",
    "~tryIdictAddGetBuilder" to "~idict_add_get_builder?",
    // replace
    "tryDictReplace" to "dict_replace?",
    "tryUdictReplace" to "udict_replace?",
    "tryIdictReplace" to "idict_replace?",
    "tryDictReplaceBuilder" to "dict_replace_builder?",
    "tryUdictReplaceBuilder" to "udict_replace_builder?",
    "tryIdictReplaceBuilder" to "idict_replace_builder?",
    "tryDictReplaceRef" to "dict_replace_ref?",
    "tryUdictReplaceRef" to "udict_replace_ref?",
    "tryIdictReplaceRef" to "idict_replace_ref?",
    "tryDictReplaceGet" to "dict_replace_get?",
    "tryUdictReplaceGet" to "udict_replace_get?",
    "tryIdictReplaceGet" to "idict_replace_get?",
    "~tryDictReplaceGet" to "~dict_replace_get?",
    "~tryUdictReplaceGet" to "~udict_replace_get?",
    "~tryIdictReplaceGet" to "~idict_replace_get?",
    "tryDictReplaceGetRef" to "dict_replace_get_ref?",
    "tryUdictReplaceGetRef" to "udict_replace_get_ref?",
    "tryIdictReplaceGetRef" to "idict_replace_get_ref?",
    "~tryDictReplaceGetRef" to "~dict_replace_get_ref?",
    "~tryUdictReplaceGetRef" to "~udict_replace_get_ref?",
    "~tryIdictReplaceGetRef" to "~idict_replace_get_ref?",
    "tryDictReplaceGetBuilder" to "dict_replace_get_builder?",
    "tryUdictReplaceGetBuilder" to "udict_replace_get_builder?",
    "tryIdictReplaceGetBuilder" to "idict_replace_get_builder?",
    "~tryDictReplaceGetBuilder" to "~dict_replace_get_builder?",
    "~tryUdictReplaceGetBuilder" to "~udict_replace_get_builder?",
    "~tryIdictReplaceGetBuilder" to "~idict_replace_get_builder?",
    // get min/max
    "tryDictGetMin" to "dict_get_min?",
    "tryUdictGetMin" to "udict_get_min?",
    "tryIdictGetMin" to "idict_get_min?",
    "tryDictGetMinRef" to "dict_get_min_ref?",
    "tryUdictGetMinRef" to "udict_get_min_ref?",
    "tryIdictGetMinRef" to "idict_get_min_ref?",
    "tryDictGetMax" to "dict_get_max?",
    "tryUdictGetMax" to "udict_get_max?",
    "tryIdictGetMax" to "idict_get_max?",
    "tryDictGetMaxRef" to "dict_get_max_ref?",
    "tryUdictGetMaxRef" to "udict_get_max_ref?",
    "tryIdictGetMaxRef" to "idict_get_max_ref?",
    // get next/prev
    "tryDictGetNext" to "dict_get_next?",
    "tryUdictGetNext" to "udict_get_next?",
    "tryIdictGetNext" to "idict_get_next?",
    "tryDictGetNexteq" to "dict_get_nexteq?",
    "tryUdictGetNexteq" to "udict_get_nexteq?",
    "tryIdictGetNexteq" to "idict_get_nexteq?",
    "tryDictGetPrev" to "dict_get_prev?",
    "tryUdictGetPrev" to "udict_get_prev?",
    "tryIdictGetPrev" to "idict_get_prev?",
    "tryDictGetPreveq" to "dict_get_preveq?",
    "tryUdictGetPreveq" to "udict_get_preveq?",
    "tryIdictGetPreveq" to "idict_get_preveq?",
    // pfx
    "tryPfxdictGet" to "pfxdict_get?",
    "tryPfxdictSet" to "pfxdict_set?",
    "tryPfxdictDelete" to "pfxdict_delete?"
)
private val camelReplaceMap = snakeReplaceMap.entries.associate { (key, value) -> value to key }

fun isSnakeCase(inputStr: String): Boolean {
    return inputStr.all { it.isLowerCase() || it.isDigit() || snakeMarks.contains(it) }
}

fun isCamelCase(inputStr: String): Boolean {
    val sanitizedStr = inputStr.filter { it.isLetterOrDigit() }
    return sanitizedStr.firstOrNull()?.isLowerCase() == true || sanitizedStr.any { it.isUpperCase() }
}

private fun snakeToCamel(inputStr: String): String {
    val words = inputStr.split("_")
    val camelCaseWords = listOf(words[0].toLowerCase()) + words.drop(1).map { it.capitalize() }
    return camelCaseWords.joinToString("")
}

private fun camelToSnake(inputStr: String): String {
    return inputStr.replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").toLowerCase()
}

private fun transformQuestionMark(inputStr: String): String {
    return if (!inputStr.endsWith("?")) {
        inputStr
    } else if ("_" in inputStr) {
        inputStr.dropLast(1)
    } else {
        "is_${inputStr.dropLast(1)}"
    }
}

private fun transformExclamationMark(inputStr: String): String {
    return if (!inputStr.endsWith("!")) {
        inputStr
    } else {
        "force_${inputStr.dropLast(1)}"
    }
}

private fun transformApostrophe(inputStr: String): String {
    return if (!inputStr.endsWith("'")) {
        inputStr
    } else {
        "modified_${inputStr.dropLast(1)}"
    }
}

private fun transformIsWord(inputStr: String): String {
    return if (!(inputStr.startsWith("is") && inputStr.count { it.isUpperCase() } == 1)) {
        inputStr
    } else {
        "${inputStr.substring(2)}?"
    }
}

private fun transformForceWord(inputStr: String): String {
    return if (!(inputStr.startsWith("force") && inputStr[5].isUpperCase())) {
        inputStr
    } else {
        "${inputStr.substring(5)}!"
    }
}

private fun transformModifiedWord(inputStr: String): String {
    return if (!(inputStr.startsWith("modified") && inputStr[8].isUpperCase())) {
        inputStr
    } else {
        "${inputStr.substring(8)}'"
    }
}

private fun transformStringToCamelCase(inputStr: String): String {
    if (!isSnakeCase(inputStr) || inputStr in builtins) {
        return inputStr
    }
    if (inputStr in camelReplaceMap.keys) {
        return camelReplaceMap[inputStr] ?: inputStr
    }

    var result = transformQuestionMark(inputStr)
    result = transformExclamationMark(result)
    result = transformApostrophe(result)
    result = snakeToCamel(result)
    return result
}

private fun transformStringToSnakeCase(inputStr: String): String {
    if (!isCamelCase(inputStr) || inputStr in builtins) {
        return inputStr
    }
    if (inputStr in snakeReplaceMap.keys) {
        return snakeReplaceMap[inputStr] ?: inputStr
    }

    var result = transformIsWord(inputStr)
    result = transformForceWord(result)
    result = transformModifiedWord(result)
    result = camelToSnake(result)
    return result
}

fun transformString(inputStr: String, mode: Int): String {
    return when (mode) {
        1 -> transformStringToCamelCase(inputStr)
        2 -> transformStringToSnakeCase(inputStr)
        else -> inputStr
    }
}
