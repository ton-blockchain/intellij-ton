package org.ton.intellij.tact

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

const val TACT_BUNDLE = "messages.TactBundle"

object TactBundle : DynamicBundle(TACT_BUNDLE) {
    fun message(@PropertyKey(resourceBundle = TACT_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun messagePointer(@PropertyKey(resourceBundle = TACT_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
