package org.ton.intellij.func

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

const val FUNC_BUNDLE = "messages.FuncBundle"

object FuncBundle : DynamicBundle(FUNC_BUNDLE) {
    fun message(@PropertyKey(resourceBundle = FUNC_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun messagePointer(@PropertyKey(resourceBundle = FUNC_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
