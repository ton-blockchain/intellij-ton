package org.ton.intellij.tolk.type

enum class TolkUnreachableKind {
    Unknown,
    CantHappen,
    ThrowStatement,
    ReturnStatement,
    CallNeverReturnFunction,
    InfiniteLoop,
}
