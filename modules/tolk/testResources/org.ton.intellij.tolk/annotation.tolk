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
    var (x1: (int8, int16), x2: Pair2_v1, x3: Pair2_v2) = ((1,2), (3,4), (5,6));
    // __expect_type(analyzeTensor1(x1), "(int8, int16)");
    __expect_type(analyzeTensor1(x2), "(int, int)");
    // __expect_type(analyzeTensor1(x3), "(MInt, MInt)");
    // __expect_type(analyzeTensor2(x1), "(int8, int16)?");
    // __expect_type(analyzeTensor2(x2), "(int, int)?");
    // __expect_type(analyzeTensor2(x3), "(MInt, MInt)?");
}
