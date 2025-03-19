// builtin operators
// they are internally stored as functions, because at IR level, there is no difference
// between calling `userAdd(a,b)` and `_+_(a,b)`
// since they are registered in a global symtable, technically, they can even be referenced from Tolk code,
// though it's a "hidden feature" and won't work well for overloads (`==` for int and bool, for example)

// unary operators

@pure
fun `-_`(x: int): int builtin;

@pure
fun `+_`(x: int): int builtin;

@pure
fun `!_`(x: int): int builtin;

@pure
fun `!b_`(x: bool): bool builtin;

@pure
fun `~_`(x: int): int builtin;

// binary operators

@pure
fun `_+_`(x: int, y: int): int builtin;

@pure
fun `_-_`(x: int, y: int): int builtin;

@pure
fun `_*_`(x: int, y: int): int builtin;

@pure
fun `_/_`(x: int, y: int): int builtin;

@pure
fun `_~/_`(x: int, y: int): int builtin;

@pure
fun `_^/_`(x: int, y: int): int builtin;

@pure
fun `_%_`(x: int, y: int): int builtin;

@pure
fun `_<<_`(x: int, y: int): int builtin;

@pure
fun `_>>_`(x: int, y: int): int builtin;

@pure
fun `_~>>_`(x: int, y: int): int builtin;

@pure
fun `_^>>_`(x: int, y: int): int builtin;

@pure
fun `_&_`(x: int, y: int): int builtin;

@pure
fun `_&_`(x: bool, y: bool): bool builtin;

@pure
fun `_|_`(x: int, y: int): int builtin;

@pure
fun `_|_`(x: bool, y: bool): bool builtin;

@pure
fun `_^_`(x: int, y: int): int builtin;

@pure
fun `_^_`(x: bool, y: bool): bool builtin;

@pure
fun `_==_`(x: int, y: int): bool builtin;

@pure
fun `_==_`(x: bool, y: bool): bool builtin;

@pure
fun `_!=_`(x: int, y: int): bool builtin;

@pure
fun `_!=_`(x: bool, y: bool): bool builtin;

@pure
fun `_<_`(x: int, y: int): bool builtin;

@pure
fun `_>_`(x: int, y: int): bool builtin;

@pure
fun `_<=_`(x: int, y: int): bool builtin;

@pure
fun `_>=_`(x: int, y: int): bool builtin;

@pure
fun `_<=>_`(x: int, y: int): int builtin;

// functions not presented in stdlib at all
// used in tolk-tester to check/expose internal compiler state
// each of them is handled in a special way, search by its name
@pure
fun __expect_type<T>(actual: T, expect: slice) builtin;
