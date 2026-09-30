// Based on Tolk 1.5 smart-cast-tests.tolk in ton-blockchain/ton.
type BalanceDict = dict

type TensorLcaFirstSlotSmart = (int, int) | (slice, int)

fun test83(case: int) {
    var x: TensorLcaFirstSlotSmart = case == 0 ? (10, 1) : (beginCell().endCell().beginParse(), 2);
    if (x is (int, int)) {
        __expect_type(x, "(int, int)");
    } else {
        __expect_type(x, "(slice, int)");
    }
    __expect_type(x, "TensorLcaFirstSlotSmart");
    return match (x) {
        (int, int) => x.0 + x.1,
        (slice, int) => x.0.loadInt(32) + x.1,
    };
}

struct ReanchorMsg1 {
    value: int
}

struct ReanchorMsg2 {
    value: int
}

type OneMessage = ReanchorMsg1
type AllowedMessages = ReanchorMsg1 | ReanchorMsg2
type MaybeCell = cell?

struct ReanchorHolder {
    maybeCell: MaybeCell
    oneMessage: OneMessage
}

struct OtherHolder {
    other: int
}

struct DeepLeaf {
    before1: int
    before2: int
    before3: int
    sub: MaybeCell
}

struct DeepOtherLeaf {
    other: int
}

type DeepNode = DeepLeaf | DeepOtherLeaf
type DeepField = (DeepNode, int)?

struct DeepHolder {
    before1: int
    before2: int
    field: DeepField
    after: int
}

type AnyDeepHolder = DeepHolder | OtherHolder

struct DictTensorHolder {
    before: int
    pair: (dict, BalanceDict)
}


fun testIfReanchor(d: MaybeCell, msg: OneMessage) {
    // d recovers to an alias
    if (d == null) {
        __expect_type(d, "null");
    } else {
        __expect_type(d, "cell");
    }
    __expect_type(d, "MaybeCell");

    // msg recovers to an alias
    if (msg is ReanchorMsg1) {
        __expect_type(msg, "ReanchorMsg1");
    } else {
        __expect_type(msg, "never");
    }
    __expect_type(msg, "OneMessage");

    match (msg) {
        ReanchorMsg1 => {
            __expect_type(msg, "ReanchorMsg1");
        }
    }
    __expect_type(msg, "OneMessage");
}

fun testUnionMatchReanchor(msg: AllowedMessages) {
    match (msg) {
        ReanchorMsg1 => { __expect_type(msg, "ReanchorMsg1"); }
        ReanchorMsg2 => { __expect_type(msg, "ReanchorMsg2"); }
    }
    __expect_type(msg, "AllowedMessages");

    match (msg) {
        ReanchorMsg1 => { return }
        ReanchorMsg2 => { __expect_type(msg, "ReanchorMsg2"); }
    }
    __expect_type(msg, "ReanchorMsg2");
}

fun testLogicalReanchor(d: MaybeCell, flag: bool) {
    d == null && false;
    __expect_type(d, "MaybeCell");

    d != null || true;
    __expect_type(d, "MaybeCell");

    if (d != null && flag) {
        __expect_type(d, "cell");
    } else {
        __expect_type(d, "MaybeCell");
    }
    __expect_type(d, "MaybeCell");

    if (d == null || flag) {
        __expect_type(d, "MaybeCell");
    } else {
        __expect_type(d, "cell");
    }
    __expect_type(d, "MaybeCell");
}

fun testTernaryReanchor(d: MaybeCell, msg: OneMessage) {
    d == null ? 1 : 2;
    __expect_type(d, "MaybeCell");

    (msg is ReanchorMsg1) ? 1 : 2;
    __expect_type(msg, "OneMessage");
}

fun testNullCoalesceReanchor(d: MaybeCell) {
    d ?? beginCell().endCell();
    __expect_type(d, "MaybeCell");
    __expect_type(d ?? createEmptyCell(), "cell");
}

fun testFieldReanchor(holder: ReanchorHolder) {
    if (holder.maybeCell == null) {
        __expect_type(holder.maybeCell, "null");
    } else {
        __expect_type(holder.maybeCell, "cell");
    }
    __expect_type(holder.maybeCell, "MaybeCell");

    match (holder.oneMessage) {
        ReanchorMsg1 => { __expect_type(holder.oneMessage, "ReanchorMsg1"); }
    }
    __expect_type(holder.oneMessage, "OneMessage");
}

fun testFieldOfSmartCastedParent(holder: ReanchorHolder | OtherHolder) {
    if (holder is ReanchorHolder) {
        if (holder.maybeCell == null) {
            __expect_type(holder.maybeCell, "null");
        } else {
            __expect_type(holder.maybeCell, "cell");
        }
        __expect_type(holder.maybeCell, "MaybeCell");
    }
}

fun testFieldNarrowingSurvivesWithParent(holder: ReanchorHolder | OtherHolder) {
    if (holder is ReanchorHolder) {
        if (holder.maybeCell == null) {
            return;
        }
    } else {
        return;
    }
    __expect_type(holder, "ReanchorHolder");
    __expect_type(holder.maybeCell, "cell");
}

fun testDeepIndexPath(obj: AnyDeepHolder) {
    if (obj is DeepHolder) {
        __expect_type(obj, "DeepHolder");
        if (obj.field != null) {
            __expect_type(obj.field, "(DeepNode, int)");
            if (obj.field.0 is DeepLeaf) {
                __expect_type(obj.field.0, "DeepLeaf");
                if (obj.field.0.sub == null) {
                    __expect_type(obj.field.0.sub, "null");
                } else {
                    __expect_type(obj.field.0.sub, "cell");
                }
                __expect_type(obj.field.0.sub, "MaybeCell");
                __expect_type(obj.field.0, "DeepLeaf");
                __expect_type(obj.field, "(DeepNode, int)");
                __expect_type(obj, "DeepHolder");
            } else {
                __expect_type(obj.field.0, "DeepOtherLeaf");
            }
            __expect_type(obj.field.0, "DeepNode");
            __expect_type(obj.field, "(DeepNode, int)");
            __expect_type(obj, "DeepHolder");
        } else {
            __expect_type(obj.field, "null");
        }
        __expect_type(obj.field, "DeepField");
        __expect_type(obj, "DeepHolder");
    } else {
        __expect_type(obj, "OtherHolder");
    }
    __expect_type(obj, "AnyDeepHolder");
}

fun testNestedReanchor(msg: OneMessage, flag: bool) {
    if (msg is ReanchorMsg1) {
        __expect_type(msg, "ReanchorMsg1");
        if (flag) {
        } else {
        }
        __expect_type(msg, "ReanchorMsg1");
    }
    __expect_type(msg, "OneMessage");
}

fun testTryCatchReanchor(d: MaybeCell) {
    try {
        d = null;
    } catch (_) {
        d = beginCell().endCell();
    }
    __expect_type(d, "MaybeCell");
}

fun testTensorAssignmentAfterEarlyReturns(d: dict, b: BalanceDict) {
    if (d == null) { return; }
    __expect_type(d, "cell");
    __expect_type(b, "BalanceDict");
    if (b != null) { return; }
    __expect_type(d, "cell");
    __expect_type(b, "null");

    if (10 > 3) {
        (b, d) = (beginCell().endCell(), null);
    }
    __expect_type(b, "BalanceDict");
    __expect_type(d, "dict");
}

fun testNestedAssignmentAfterEarlyReturns(holder: DictTensorHolder) {
    if (holder.pair.0 == null) { return; }
    if (holder.pair.1 != null) { return; }

    if (10 > 3) {
        (holder.pair.1, holder.pair.0) = (beginCell().endCell(), null);
    }
    __expect_type(holder.pair.1, "BalanceDict");
    __expect_type(holder.pair.0, "dict");
}

fun testConstantTernary() {
    var value: int? = true ? 1 : null;
    __expect_type(value, "int?");
}

fun testTupleLca(flag: bool, left: [int, int], right: [int, cell]) {
    val value: [int, int] | [int, cell] = flag ? left : right;
    __expect_type(value, "[int, int] | [int, cell]");
}

fun testArrayLca(flag: bool, left: array<int>, right: array<cell>) {
    val value: array<int> | array<cell> = flag ? left : right;
    __expect_type(value, "array<int> | array<cell>");
}

fun main() {}
type OptionalCell = cell?;
type Flag = bool;
struct Box<T> { value: T }

fun checkGenericField(holder: Box<OptionalCell>) {
    if (holder.value != null) {}
    __expect_type(holder.value, "OptionalCell");
}

fun checkBoolMatch(flag: Flag, value: int?) {
    match (flag) {
        true => { value = 1; }
        false => { value = 2; }
    }
    __expect_type(value, "int");
}

fun checkCoalesceAssignment(value: int?) {
    var alwaysNull: int? = null;
    alwaysNull ?? (value = 1);
    __expect_type(value, "int");
    value = null;
    1 ?? (value = 2);
    __expect_type(value, "null");
}


fun checkPartialHint(flag: bool) {
    val (a: int, b) = flag ? (1, 2) : (3, 4);
    __expect_type(a, "int");
    __expect_type(b, "int");
}
