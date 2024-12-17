package org.ton.intellij.fift.ide

import com.intellij.lang.Commenter

class FiftCommenter : Commenter {
    override fun getLineCommentPrefix() = "// "

    override fun getBlockCommentPrefix() = "/*"
    override fun getBlockCommentSuffix() = "*/"
    override fun getCommentedBlockCommentPrefix() = null
    override fun getCommentedBlockCommentSuffix() = null
}