package org.ton.intellij.func.ide

import com.intellij.lang.Commenter

class FuncCommenter : Commenter {
    override fun getLineCommentPrefix() = ";; "

    override fun getBlockCommentPrefix() = "{-"
    override fun getBlockCommentSuffix() = "-}"
    override fun getCommentedBlockCommentPrefix() = null
    override fun getCommentedBlockCommentSuffix() = null
}