package org.ton.intellij.util

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

private const val INSPECTION_BUNDLE = "messages.InspectionBundle"

object InspectionBundle : DynamicBundle(INSPECTION_BUNDLE) {
    fun message(@PropertyKey(resourceBundle = INSPECTION_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun messagePointer(@PropertyKey(resourceBundle = INSPECTION_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
