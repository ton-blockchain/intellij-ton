package org.ton.intellij.tlb.ide

import com.intellij.lang.Commenter

class TlbCommenter : Commenter {
    override fun getLineCommentPrefix() = "// "

    override fun getBlockCommentPrefix() = "/*"
    override fun getBlockCommentSuffix() = "*/"
    override fun getCommentedBlockCommentPrefix() = null
    override fun getCommentedBlockCommentSuffix() = null
}