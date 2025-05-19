package org.ton.intellij.tolk.ide.completion

object TolkCompletionPriorities {
    const val KEYWORD = 2000.0
    const val LOCAL_VAR = 1000.0
    const val PARAMETER = 900.0
    const val FUNCTION = 800.0
    const val NOT_IMPORTED_FUNCTION = 700.0
    const val IMPORTED_TYPE = 600.0
    const val NOT_IMPORTED_TYPE = 500.0

    const val INSTANCE_FIELD = 950.0
    const val INSTANCE_METHOD = 940.0
    const val STATIC_FUNCTION = 910.0
    const val DEPRECATED = 100.0
}
