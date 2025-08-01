package org.ton.intellij.tlb

import com.intellij.DynamicBundle
import org.jetbrains.annotations.PropertyKey

const val TLB_BUNDLE = "messages.TlbBundle"

object TlbBundle : DynamicBundle(TLB_BUNDLE) {
    fun message(@PropertyKey(resourceBundle = TLB_BUNDLE) key: String, vararg params: Any): String =
        getMessage(key, *params)

    fun messagePointer(@PropertyKey(resourceBundle = TLB_BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
