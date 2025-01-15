package org.ton.intellij.tolk.doc.psi

import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocCommentBase
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.impl.source.tree.injected.InjectionBackgroundSuppressor
import org.ton.intellij.tolk.psi.TolkDocOwner
import org.ton.intellij.tolk.psi.TolkElement

interface TolkDocComment : PsiDocCommentBase, TolkElement {
    override fun getOwner(): TolkDocOwner?

    val codeFences: List<TolkDocCodeFence>
}

interface TolkDocGap : PsiComment

interface TolkDocElement : TolkElement {
    val containingDoc: TolkDocComment

    val markdownValue: String
}

/**
 * A [markdown code fence](https://spec.commonmark.org/0.29/#fenced-code-blocks).
 */
interface TolkDocCodeFence : TolkDocElement, PsiLanguageInjectionHost, InjectionBackgroundSuppressor {
    val start: TolkDocCodeFenceStartEnd
    val lang: TolkDocCodeFenceLang?
    val end: TolkDocCodeFenceStartEnd?
}

interface TolkDocCodeBlock : TolkDocElement

interface TolkDocLink : TolkDocElement

/**
 * ```
 * /// [link text](link_destination)
 * /// [link text](link_destination "link title")
 * ```
 */
interface TolkDocInlineLink : TolkDocLink {
    val linkText: TolkDocLinkText
    val linkDestination: TolkDocLinkDestination
}

/**
 * ```
 * /// [link label]
 * ```
 *
 * Then, the link should be defined with [TolkDocLinkDefinition]
 */
interface TolkDocLinkReferenceShort : TolkDocLink {
    val linkLabel: TolkDocLinkLabel
}

/**
 * ```
 * /// [link text][link label]
 * ```
 *
 * Then, the link should be defined with [TolkDocLinkDefinition] (identified by [linkLabel])
 */
interface TolkDocLinkReferenceFull : TolkDocLink {
    val linkText: TolkDocLinkText
    val linkLabel: TolkDocLinkLabel
}

/**
 * ```
 * /// [link label]: link_destination
 * ```
 */
interface TolkDocLinkDefinition : TolkDocLink {
    val linkLabel: TolkDocLinkLabel
    val linkDestination: TolkDocLinkDestination
}

/**
 * A `[LINK TEXT]` part of such links:
 * ```
 * /// [LINK TEXT](link_destination)
 * /// [LINK TEXT][link label]
 * ```
 * Includes brackets (`[`, `]`).
 */
interface TolkDocLinkText : TolkDocElement

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
interface TolkDocLinkLabel : TolkDocElement

/**
 * A `LINK TITLE` (with quotes and parentheses) part in these contexts:
 * ```
 * /// [inline link](http://example.com "LINK TITLE")
 * /// [inline link](http://example.com 'LINK TITLE')
 * /// [inline link](http://example.com (LINK TITLE))
 * ```
 *
 * A child of [TolkDocInlineLink]
 */
interface TolkDocLinkTitle : TolkDocElement

/**
 * A `LINK DESTINATION` part in these contexts:
 * ```
 * /// [link text](LINK DESTINATION)
 * /// [link label]: LINK DESTINATION
 * ```
 *
 * A child of [TolkDocInlineLink] or [TolkDocLinkDefinition]
 */
interface TolkDocLinkDestination : TolkDocElement

/**
 * See [markdown HTML blocks](https://spec.commonmark.org/0.29/#html-blocks)
 */
interface TolkDocHtmlBlock : TolkDocElement

/**
 * `a code span`
 */
interface TolkDocCodeSpan : TolkDocElement

/**
 * Leading and trailing backtick or tilda sequences of [TolkDocCodeFence].
 *
 * `````
 * /// ```
 * ///  ^ this
 * /// ```
 *      ^ and this
 * `````
 */
interface TolkDocCodeFenceStartEnd : TolkDocElement

/**
 * A child of [TolkDocCodeFence]
 *
 * `````
 * /// ```func
 *        ~~~~ this text
 * `````
 */
interface TolkDocCodeFenceLang : TolkDocElement
