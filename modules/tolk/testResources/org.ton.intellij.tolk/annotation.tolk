struct JustInt {
    value: int;
}

struct JustIntWrapper {
    int: JustInt;
}

type MInt = int;
type MNull = null;
type JustIntAlias = JustInt;
type JustIntAlias2 = JustIntAlias;
type Pair2_v1 = (int, int);
type Pair2_v2 = (MInt, MInt);

fun eqUnion2<T1, T2>(v: T1 | T2): T2 | T1 { return v }

type IntOrInt8OrInt16 = int | int8 | int16;

fun analyzeTensor1<T1, T2>(a: (T1, T2)): (T1, T2) { return a; }
fun analyzeTensor2<T1, T2>(a: (T1, T2)?): (T1, T2)? { return a; }

fun test11() {
    var bc = beginCell() as builder | cell;
    // var mi1 = 5 as MInt | int8;
    // var mi2 = 5 as int8 | MInt;
    // var mi3 = (5 as int8) as int8 | MInt;
    __expect_type(eqUnion2(bc), "cell | builder");
    // __expect_type(eqUnion2(mi1), "int8 | MInt");
}
