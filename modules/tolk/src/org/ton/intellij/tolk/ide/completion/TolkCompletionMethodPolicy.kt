package org.ton.intellij.tolk.ide.completion

internal fun isHiddenMethodFromCompletion(name: String): Boolean = name.startsWith("__")

internal fun isLowLevelMethodName(name: String): Boolean = when (name) {
    "forceLoadLazyObject",
    "stackMoveToTop",
    "typeName",
    "fromTuple",
    "toTuple",
         -> true

    else -> false
}
