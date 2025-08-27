@pure
fun dict.isUdictReplaceGet(self, keyLen: int, index: int, value: slice): (cell, slice, int)
    asm(value index self keyLen) "DICTUREPLACEGET" "NULLSWAPIFNOT";

@pure
fun cell.beginParseSpecial(self): (slice, int)
    asm "x{D739} s,";

@pure
fun castToS2I(c: continuation): (slice -> int)
    asm "NOP";

@pure
fun proofHash(c: cell): int
    asm "0 CHASHI";

@pure
fun builder.storeVarUInt32(mutate self, x: int): self
    asm "STVARUINT32";

@pure
fun slice.loadVarUInt32(mutate self): int
    asm(-> 1 0) "LDVARUINT32";

const STATE_UNINITED = 0
const STATE_OPEN = 1
const STATE_CLOSURE_STARTED = 2
const STATE_SETTLING_CONDITIONALS = 3
const STATE_AWAITING_FINALIZATION = 4
