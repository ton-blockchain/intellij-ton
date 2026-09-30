// Nullable break and continue cases follow break-continue-tests.tolk in Tolk 1.5.
fun flow_break_nullable(x: int?, flag: int): int {
    while (x != null) {
        __expect_type(x, "int");
        if (flag != 0) {
            break;
        }
        x = null;
    }
    __expect_type(x, "int?");
    return x == null ? 0 : x;
}

fun flow_continue_nullable(x: int?, flag: int): int {
    var n = 0;
    while (x != null) {
        __expect_type(x, "int");
        if (flag != 0) {
            flag = 0;
            continue;
        }
        n += x;
        x = null;
    }
    __expect_type(x, "null");
    return n;
}

fun while_break_initializes(): int {
    var x: int? = null;
    while (true) {
        x = 1;
        break;
    }
    __expect_type(x, "int");
    return x;
}

fun while_continue_fixed_point(flag: int) {
    var a: int? = 1;
    var b: int? = 2;
    var c: int? = 3;
    while (flag != 0) {
        __expect_type(a, "int?");
        a = b;
        b = c;
        c = null;
        continue;
    }
    __expect_type(a, "int?");
}

fun do_continue_reaches_condition(flag: int) {
    var x: int? = null;
    do {
        x = 1;
        continue;
    } while (flag != 0);
    __expect_type(x, "int");
}

fun do_break_preserves_exit(flag: int) {
    var x: int? = null;
    do {
        if (flag != 0) {
            break;
        }
        x = 1;
    } while (false);
    __expect_type(x, "int?");
}

fun repeat_break_preserves_exit(n: int) {
    var x: int? = 1;
    repeat (n) {
        x = null;
        break;
    }
    __expect_type(x, "int?");
}

fun repeat_continue_fixed_point(n: int) {
    var a: int? = 1;
    var b: int? = 2;
    var c: int? = 3;
    repeat (n) {
        __expect_type(a, "int?");
        a = b;
        b = c;
        c = null;
        continue;
    }
    __expect_type(a, "int?");
}

fun do_continue_fixed_point(flag: int) {
    var a: int? = 1;
    var b: int? = 2;
    var c: int? = 3;
    do {
        __expect_type(a, "int?");
        a = b;
        b = c;
        c = null;
        continue;
    } while (flag != 0);
    __expect_type(a, "int?");
}

fun nested_break_stays_in_inner_loop(x: int?) {
    while (x != null) {
        while (true) {
            break;
        }
        x = null;
    }
    __expect_type(x, "null");
}

fun nested_continue_stays_in_inner_loop(n: int) {
    var x: int? = 1;
    repeat (n) {
        repeat (n) {
            x = null;
            continue;
        }
        x = 1;
        continue;
    }
    __expect_type(x, "int");
}

fun multiple_break_paths(flag: int) {
    var x: int? = null;
    while (true) {
        if (flag != 0) {
            x = 1;
            break;
        }
        break;
    }
    __expect_type(x, "int?");
}

fun unreachable_break_is_ignored(x: int?) {
    while (x != null) {
        if (false) {
            break;
        }
        x = null;
    }
    __expect_type(x, "null");
}

fun lambda_with_loop() {
    val callback = fun (x: int?) {
        while (true) {
            if (x == null) {
                x = 1;
            }
            break;
        }
        return x;
    };
    __expect_type(callback(null), "int");
}

fun main() {}
