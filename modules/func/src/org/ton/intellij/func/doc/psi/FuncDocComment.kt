package org.ton.intellij.func.doc.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import org.ton.intellij.func.psi.FuncDocOwner
import org.ton.intellij.func.psi.FuncElement

interface FuncDocComment : PsiDocCommentBase, FuncElement {
    override fun getOwner(): FuncDocOwner?

    val codeFences: List<FuncDocCodeFence>
}

interface FuncDocGap : PsiComment

interface FuncDocElement : FuncElement {
    val containingDoc: FuncDocComment

    val markdownValue: String
}

/**
 * A [markdown code fence](https://spec.commonmark.org/0.29/#fenced-code-blocks).
 */
interface FuncDocCodeFence : FuncDocElement, PsiLanguageInjectionHost, InjectionBackgroundSuppressor {
    val start: FuncDocCodeFenceStartEnd
    val lang: FuncDocCodeFenceLang?
    val end: FuncDocCodeFenceStartEnd?
}

interface FuncDocCodeBlock : FuncDocElement

interface FuncDocLink : FuncDocElement

/**
 * ```
 * /// [link text](link_destination)
 * /// [link text](link_destination "link title")
 * ```
 */
interface FuncDocInlineLink : FuncDocLink {
    val linkText: FuncDocLinkText
    val linkDestination: FuncDocLinkDestination
}

/**
 * ```
 * /// [link label]
 * ```
 *
 * Then, the link should be defined with [FuncDocLinkDefinition]
 */
interface FuncDocLinkReferenceShort : FuncDocLink {
    val linkLabel: FuncDocLinkLabel
}

/**
 * ```
 * /// [link text][link label]
 * ```
 *
 * Then, the link should be defined with [FuncDocLinkDefinition] (identified by [linkLabel])
 */
interface FuncDocLinkReferenceFull : FuncDocLink {
    val linkText: FuncDocLinkText
    val linkLabel: FuncDocLinkLabel
}

/**
 * ```
 * /// [link label]: link_destination
 * ```
 */
interface FuncDocLinkDefinition : FuncDocLink {
    val linkLabel: FuncDocLinkLabel
    val linkDestination: FuncDocLinkDestination
}

/**
 * A `[LINK TEXT]` part of such links:
 * ```
 * /// [LINK TEXT](link_destination)
 * /// [LINK TEXT][link label]
 * ```
 * Includes brackets (`[`, `]`).
 */
interface FuncDocLinkText : FuncDocElement

/**
 * A `[LINK LABEL]` part in these contexts:
 * ```
 * /// [LINK LABEL]
 * /// [link text][LINK LABEL]
 * /// [LINK LABEL]: link_destination
 * ```
 *
 * A link label is used to match *a link reference* with *a link definition*.
 */
interface FuncDocLinkLabel : FuncDocElement

/**
 * A `LINK TITLE` (with quotes and parentheses) part in these contexts:
 * ```
 * /// [inline link](http://example.com "LINK TITLE")
 * /// [inline link](http://example.com 'LINK TITLE')
 * /// [inline link](http://example.com (LINK TITLE))
 * ```
 *
 * A child of [FuncDocInlineLink]
 */
interface FuncDocLinkTitle : FuncDocElement

/**
 * A `LINK DESTINATION` part in these contexts:
 * ```
 * /// [link text](LINK DESTINATION)
 * /// [link label]: LINK DESTINATION
 * ```
 *
 * A child of [FuncDocInlineLink] or [FuncDocLinkDefinition]
 */
interface FuncDocLinkDestination : FuncDocElement

/**
 * See [markdown HTML blocks](https://spec.commonmark.org/0.29/#html-blocks)
 */
interface FuncDocHtmlBlock : FuncDocElement

/**
 * `a code span`
 */
interface FuncDocCodeSpan : FuncDocElement

/**
 * Leading and trailing backtick or tilda sequences of [FuncDocCodeFence].
 *
 * `````
 * /// ```
 * ///  ^ this
 * /// ```
 *      ^ and this
 * `````
 */
interface FuncDocCodeFenceStartEnd : FuncDocElement

/**
 * A child of [FuncDocCodeFence]
 *
 * `````
 * /// ```func
 *        ~~~~ this text
 * `````
 */
interface FuncDocCodeFenceLang : FuncDocElement
