fun alwaysThrows() {
    throw 10
}

fun alwaysThrowsWithConditions(a: bool) {
    if (a) {
        throw 10;
    }
    throw 1 ;
}

fun lastThrowAndConditionalReturn(a: bool) {
    if (a) {
        return 10;
    }
    throw 1 ;
}

fun conditionallyThrow(a: bool) {
    if (a) {
        throw 1
    }
    return 10
}

fun main() {
    __expect_type(alwaysThrows(), "never");
    __expect_type(alwaysThrowsWithConditions(true), "never");
    __expect_type(lastThrowAndConditionalReturn(true), "int");
    __expect_type(conditionallyThrow(true), "int");
}
