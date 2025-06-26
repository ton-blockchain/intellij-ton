struct JustInt {
    value: int;
}

struct JustIntWrapper {
    int: JustInt;
}

type MIntN = MInt?;
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
fun getNullableInt(): int? { return 5; }

fun test3(x: MIntN, y: MInt?) {
    if (x != null) {
    }
    var (z1, z2) = (x, x!, );
    __expect_type(z1, "MInt?");
    __expect_type(z2, "MInt");
}
