package org.ton.intellij.fift

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

const val FIFT_BUNDLE = "messages.FiftBundle"

object FiftBundle : DynamicBundle(FIFT_BUNDLE) {
    fun message(@PropertyKey(resourceBundle = FIFT_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun messagePointer(@PropertyKey(resourceBundle = FIFT_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
