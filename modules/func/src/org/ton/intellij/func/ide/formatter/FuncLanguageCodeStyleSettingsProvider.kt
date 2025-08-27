package org.ton.intellij.func.ide.formatter

import com.intellij.application.options.SmartIndentOptionsEditor
import com.intellij.lang.Language
import com.intellij.psi.codeStyle.CommonCodeStyleSettings
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider
import org.ton.intellij.func.FuncLanguage

class FuncLanguageCodeStyleSettingsProvider : LanguageCodeStyleSettingsProvider() {
    override fun getLanguage(): Language = FuncLanguage

    override fun getCodeSample(settingsType: SettingsType): String = when (settingsType) {
        SettingsType.SPACING_SETTINGS, SettingsType.WRAPPING_AND_BRACES_SETTINGS -> SPACING_SAMPLE
        SettingsType.INDENT_SETTINGS                                             -> INDENT_SAMPLE
        else                                                                     -> GENERAL_SAMPLE
    }

    override fun getIndentOptionsEditor() = SmartIndentOptionsEditor()

    override fun customizeDefaults(
        commonSettings: CommonCodeStyleSettings,
        indentOptions: CommonCodeStyleSettings.IndentOptions,
    ) {
        commonSettings.RIGHT_MARGIN = 100

        commonSettings.LINE_COMMENT_AT_FIRST_COLUMN = false
        commonSettings.LINE_COMMENT_ADD_SPACE = true
        commonSettings.BLOCK_COMMENT_AT_FIRST_COLUMN = false

        // Configure indent options
        indentOptions.INDENT_SIZE = 4
        indentOptions.CONTINUATION_INDENT_SIZE = 8
        indentOptions.TAB_SIZE = 4
        indentOptions.USE_TAB_CHARACTER = false
        indentOptions.SMART_TABS = false
        indentOptions.KEEP_INDENTS_ON_EMPTY_LINES = false
    }
}

private val GENERAL_SAMPLE = """
    ;; Example FunC code
    int factorial(int n) {
        if (n <= 1) {
            return 1;
        } else {
            return n * factorial(n - 1);
        }
    }

    () recv_internal(int my_balance, int msg_value, cell in_msg_full, slice in_msg_body) impure {
        var cs = in_msg_full.begin_parse();
        var flags = cs~load_uint(4);
        
        if (flags & 1) {
            return ();
        }
        
        slice sender_address = cs~load_msg_addr();
        int op = in_msg_body~load_uint(32);
        
        if (op == 1) {
            ;; Handle operation 1
            process_operation_1(sender_address, in_msg_body);
        } elseif (op == 2) {
            ;; Handle operation 2  
            process_operation_2(sender_address, in_msg_body);
        }
    }
""".trimIndent()

private val INDENT_SAMPLE = """
    int example_function(int a, int b) {
        if (a > b) {
            return a;
        } else {
            if (b > 0) {
                return b;
            } else {
                return 0;
            }
        }
    }
""".trimIndent()

private val SPACING_SAMPLE = """
    int sum(int a, int b) {
        return a + b;
    }
    
    (int, int) divmod(int x, int y) {
        return (x / y, x % y);
    }
""".trimIndent()
