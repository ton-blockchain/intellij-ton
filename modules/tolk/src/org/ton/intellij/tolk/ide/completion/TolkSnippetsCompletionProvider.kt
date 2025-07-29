package org.ton.intellij.tolk.ide.completion

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.codeInsight.template.impl.ConstantNode
import com.intellij.openapi.project.DumbAware
import com.intellij.patterns.ElementPattern
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import org.ton.intellij.tolk.ide.completion.TolkLookupElementData.KeywordKind.KEYWORD

object TolkSnippetsCompletionProvider : TolkCompletionProvider(), DumbAware {
    override val elementPattern: ElementPattern<out PsiElement>
        get() = TolkCompletionPatterns.inBlock()

    data class Snippet(
        val keyword: String,
        val text: String,
        val presentation: String,
        val variables: List<Pair<String, String>>,
    )

    val snippets = listOf(
        Snippet(
            keyword = "val",
            presentation = " name = 10;",
            text = "val \$name$ = \$value$;\$END$",
            variables = listOf("name" to "name", "value" to "0")
        ),
        Snippet(
            keyword = "valt",
            presentation = " name: int = 10;",
            text = "val \$name$: \$type$ = \$value$;\$END$",
            variables = listOf("name" to "name", "type" to "int", "value" to "0")
        ),
        Snippet(
            keyword = "var",
            presentation = " name = 10;",
            text = "var \$name$ = \$value$;\$END$",
            variables = listOf("name" to "name", "value" to "0")
        ),
        Snippet(
            keyword = "vart",
            presentation = " name: int = 10;",
            text = "var \$name$: \$type$ = \$value$;\$END$",
            variables = listOf("name" to "name", "type" to "int", "value" to "0")
        ),
        Snippet(
            keyword = "if",
            presentation = " (cond) {}",
            text = "if (\$condition$) {\n\t\$END$\n\t}",
            variables = listOf("condition" to "true")
        ),
        Snippet(
            keyword = "ife",
            presentation = " (cond) {} else {}",
            text = "if (\$condition$) {\n\t\$then$\n\t} else {\n\t\$END$\n\t}",
            variables = listOf("condition" to "true", "then" to "")
        ),
        Snippet(
            keyword = "while",
            presentation = " (cond) {}",
            text = "while (\$condition$) {\n\t\$END$\n\t}",
            variables = listOf("condition" to "true")
        ),
        Snippet(
            keyword = "do",
            presentation = " {} while (cond)",
            text = "do {\n\t\$END$\n\t} while (\$condition$);",
            variables = listOf("condition" to "true")
        ),
        Snippet(
            keyword = "repeat",
            presentation = " (count) {}",
            text = "repeat (\$count$) {\n\t\$END$\n\t}",
            variables = listOf("count" to "10")
        ),
        Snippet(
            keyword = "try",
            presentation = " {}",
            text = "try {\n\t\$END$\n\t}",
            variables = listOf()
        ),
        Snippet(
            keyword = "tryc",
            presentation = " {} catch {}",
            text = "try {\n\t\$body$\n\t} catch (\$error$) {\n\t\$END$\n\t}",
            variables = listOf("body" to "", "error" to "e")
        ),
        Snippet(
            keyword = "match",
            presentation = " (value) {}",
            text = "match (\$value$) {\n\t\$END$\n\t}",
            variables = listOf("value" to "true")
        ),
        Snippet(
            keyword = "assert",
            presentation = " (cond) throw EXIT_CODE;",
            text = "assert (\$condition$) throw \$exit_code$;\$END$",
            variables = listOf("condition" to "false", "exit_code" to "5")
        ),
        Snippet(
            keyword = "throw",
            presentation = " EXIT_CODE;",
            text = "throw \$exit_code$;\$END$",
            variables = listOf("exit_code" to "5")
        ),
    )

    override fun addCompletions(
        parameters: CompletionParameters,
        context: ProcessingContext,
        result: CompletionResultSet,
    ) {
        for (snippet in snippets) {
            val builder = LookupElementBuilder.create(snippet.keyword).bold()
                .withTailText(snippet.presentation, true)
                .withInsertHandler { context, item ->
                    // since we are defining snippets like `vart`, we need to remove the automatically inserted `vart`
                    // and insert the correct text instead
                    val document = context.document
                    val start = context.startOffset
                    document.deleteString(start, start + snippet.keyword.length)
                    TemplateStringInsertHandler(
                        snippet.text,
                        true,
                        *snippet.variables.map { it.first to ConstantNode(it.second) }.toTypedArray()
                    ).handleInsert(context, item)
                }
                .toTolkLookupElement(
                    TolkLookupElementData(keywordKind = KEYWORD)
                )

            result.addElement(builder)
        }
    }
}
