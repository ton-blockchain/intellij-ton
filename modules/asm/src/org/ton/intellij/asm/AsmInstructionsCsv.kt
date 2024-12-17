package org.ton.intellij.asm

object AsmInstructionsCsv {
    val INSTRUCTIONS = SOURCE.lines().drop(1).map {
        val split = it.split(",")
        Instruction(
            split[0],
            split[1],
            split[2],
            split[3],
            split[4],
            split[5],
            split[6],
            split[7],
            split[8]
        ).also {
            println("parsed: $it")
        }
    }

    data class Instruction(
        val name: String,
        val aliasOf: String,
        val tlb: String,
        val docCategory: String,
        val docOpcode: String,
        val docFift: String,
        val docStack: String,
        val docGas: String,
        val docDescription: String
    )
}

@Suppress("HardCodedStringLiteral")
private val SOURCE = """
    name,alias_of,tlb,doc_category,doc_opcode,doc_fift,doc_stack,doc_gas,doc_description
    NOP,null,#00,stack_basic,00,NOP,-,18,Does nothing.
    SWAP,XCHG_0I,#01,stack_basic,01,SWAP,x y - y x,18,Same as `s1 XCHG0`.
    XCHG_0I,null,#0 i:(## 4) {1 <= i},stack_basic,0i,s[i] XCHG0,null,18,Interchanges `s0` with `s[i]`, `1 <= i <= 15`.
    XCHG_IJ,null,#10 i:(## 4) j:(## 4) {1 <= i} {i + 1 <= j},stack_basic,10ij,s[i] s[j] XCHG,null,26,Interchanges `s[i]` with `s[j]`, `1 <= i < j <= 15`.
    XCHG_0I_LONG,null,#11 ii:uint8,stack_basic,11ii,s0 [ii] s() XCHG,null,26,Interchanges `s0` with `s[ii]`, `0 <= ii <= 255`.
    XCHG_1I,null,#1 i:(## 4) {2 <= i},stack_basic,1i,s1 s[i] XCHG,null,18,Interchanges `s1` with `s[i]`, `2 <= i <= 15`.
    PUSH,null,#2 i:uint4,stack_basic,2i,s[i] PUSH,null,18,Pushes a copy of the old `s[i]` into the stack.
    DUP,PUSH,#20,stack_basic,20,DUP,x - x x,18,Same as `s0 PUSH`.
    OVER,PUSH,#21,stack_basic,21,OVER,x y - x y x,18,Same as `s1 PUSH`.
    POP,null,#3 i:uint4,stack_basic,3i,s[i] POP,null,18,Pops the old `s0` value into the old `s[i]`.
    DROP,POP,#30,stack_basic,30,DROP,x -,18,Same as `s0 POP`, discards the top-of-stack value.
    NIP,POP,#31,stack_basic,31,NIP,x y - y,18,Same as `s1 POP`.
    XCHG3,null,#4 i:uint4 j:uint4 k:uint4,stack_complex,4ijk,s[i] s[j] s[k] XCHG3,null,26,Equivalent to `s2 s[i] XCHG` `s1 s[j] XCHG` `s[k] XCHG0`.
    XCHG2,null,#50 i:uint4 j:uint4,stack_complex,50ij,s[i] s[j] XCHG2,null,26,Equivalent to `s1 s[i] XCHG` `s[j] XCHG0`.
    XCPU,null,#51 i:uint4 j:uint4,stack_complex,51ij,s[i] s[j] XCPU,null,26,Equivalent to `s[i] XCHG0` `s[j] PUSH`.
    PUXC,null,#52 i:uint4 j:uint4,stack_complex,52ij,s[i] s[j-1] PUXC,null,26,Equivalent to `s[i] PUSH` `SWAP` `s[j] XCHG0`.
    PUSH2,null,#53 i:uint4 j:uint4,stack_complex,53ij,s[i] s[j] PUSH2,null,26,Equivalent to `s[i] PUSH` `s[j+1] PUSH`.
    XCHG3_ALT,null,#540 i:uint4 j:uint4 k:uint4,stack_complex,540ijk,s[i] s[j] s[k] XCHG3_l,null,34,Long form of `XCHG3`.
    XC2PU,null,#541 i:uint4 j:uint4 k:uint4,stack_complex,541ijk,s[i] s[j] s[k] XC2PU,null,34,Equivalent to `s[i] s[j] XCHG2` `s[k] PUSH`.
    XCPUXC,null,#542 i:uint4 j:uint4 k:uint4,stack_complex,542ijk,s[i] s[j] s[k-1] XCPUXC,null,34,Equivalent to `s1 s[i] XCHG` `s[j] s[k-1] PUXC`.
    XCPU2,null,#543 i:uint4 j:uint4 k:uint4,stack_complex,543ijk,s[i] s[j] s[k] XCPU2,null,34,Equivalent to `s[i] XCHG0` `s[j] s[k] PUSH2`.
    PUXC2,null,#544 i:uint4 j:uint4 k:uint4,stack_complex,544ijk,s[i] s[j-1] s[k-1] PUXC2,null,34,Equivalent to `s[i] PUSH` `s2 XCHG0` `s[j] s[k] XCHG2`.
    PUXCPU,null,#545 i:uint4 j:uint4 k:uint4,stack_complex,545ijk,s[i] s[j-1] s[k-1] PUXCPU,null,34,Equivalent to `s[i] s[j-1] PUXC` `s[k] PUSH`.
    PU2XC,null,#546 i:uint4 j:uint4 k:uint4,stack_complex,546ijk,s[i] s[j-1] s[k-2] PU2XC,null,34,Equivalent to `s[i] PUSH` `SWAP` `s[j] s[k-1] PUXC`.
    PUSH3,null,#547 i:uint4 j:uint4 k:uint4,stack_complex,547ijk,s[i] s[j] s[k] PUSH3,null,34,Equivalent to `s[i] PUSH` `s[j+1] s[k+1] PUSH2`.
    BLKSWAP,null,#55 i:uint4 j:uint4,stack_complex,55ij,[i+1] [j+1] BLKSWAP,null,26,Permutes two blocks `s[j+i+1] … s[j+1]` and `s[j] … s0`.\n`0 <= i,j <= 15`\nEquivalent to `[i+1] [j+1] REVERSE` `[j+1] 0 REVERSE` `[i+j+2] 0 REVERSE`.
    ROT2,BLKSWAP,#5513,stack_complex,5513,ROT2\n2ROT,a b c d e f - c d e f a b,26,Rotates the three topmost pairs of stack entries.
    ROLL,BLKSWAP,#550 i:uint4,stack_complex,550i,[i+1] ROLL,null,26,Rotates the top `i+1` stack entries.\nEquivalent to `1 [i+1] BLKSWAP`.
    ROLLREV,BLKSWAP,#55 i:uint4 zero:(## 4) {zero = 0},stack_complex,55i0,[i+1] -ROLL\n[i+1] ROLLREV,null,26,Rotates the top `i+1` stack entries in the other direction.\nEquivalent to `[i+1] 1 BLKSWAP`.
    PUSH_LONG,null,#56 ii:uint8,stack_complex,56ii,[ii] s() PUSH,null,26,Pushes a copy of the old `s[ii]` into the stack.\n`0 <= ii <= 255`
    POP_LONG,null,#57 ii:uint8,stack_complex,57ii,[ii] s() POP,null,26,Pops the old `s0` value into the old `s[ii]`.\n`0 <= ii <= 255`
    ROT,null,#58,stack_complex,58,ROT,a b c - b c a,18,Equivalent to `1 2 BLKSWAP` or to `s2 s1 XCHG2`.
    ROTREV,null,#59,stack_complex,59,ROTREV\n-ROT,a b c - c a b,18,Equivalent to `2 1 BLKSWAP` or to `s2 s2 XCHG2`.
    SWAP2,null,#5A,stack_complex,5A,SWAP2\n2SWAP,a b c d - c d a b,18,Equivalent to `2 2 BLKSWAP` or to `s3 s2 XCHG2`.
    DROP2,null,#5B,stack_complex,5B,DROP2\n2DROP,a b - ,18,Equivalent to `DROP` `DROP`.
    DUP2,null,#5C,stack_complex,5C,DUP2\n2DUP,a b - a b a b,18,Equivalent to `s1 s0 PUSH2`.
    OVER2,null,#5D,stack_complex,5D,OVER2\n2OVER,a b c d - a b c d a b,18,Equivalent to `s3 s2 PUSH2`.
    REVERSE,null,#5E i:uint4 j:uint4,stack_complex,5Eij,[i+2] [j] REVERSE,null,26,Reverses the order of `s[j+i+1] … s[j]`.
    BLKDROP,null,#5F0 i:uint4,stack_complex,5F0i,[i] BLKDROP,null,26,Equivalent to `DROP` performed `i` times.
    BLKPUSH,null,#5F i:(## 4) j:uint4 {1 <= i},stack_complex,5Fij,[i] [j] BLKPUSH,null,26,Equivalent to `PUSH s(j)` performed `i` times.\n`1 <= i <= 15`, `0 <= j <= 15`.
    PICK,null,#60,stack_complex,60,PICK\nPUSHX,null,18,Pops integer `i` from the stack, then performs `s[i] PUSH`.
    ROLLX,null,#61,stack_complex,61,ROLLX,null,18,Pops integer `i` from the stack, then performs `1 [i] BLKSWAP`.
    -ROLLX,null,#62,stack_complex,62,-ROLLX\nROLLREVX,null,18,Pops integer `i` from the stack, then performs `[i] 1 BLKSWAP`.
    BLKSWX,null,#63,stack_complex,63,BLKSWX,null,18,Pops integers `i`,`j` from the stack, then performs `[i] [j] BLKSWAP`.
    REVX,null,#64,stack_complex,64,REVX,null,18,Pops integers `i`,`j` from the stack, then performs `[i] [j] REVERSE`.
    DROPX,null,#65,stack_complex,65,DROPX,null,18,Pops integer `i` from the stack, then performs `[i] BLKDROP`.
    TUCK,null,#66,stack_complex,66,TUCK,a b - b a b,18,Equivalent to `SWAP` `OVER` or to `s1 s1 XCPU`.
    XCHGX,null,#67,stack_complex,67,XCHGX,null,18,Pops integer `i` from the stack, then performs `s[i] XCHG`.
    DEPTH,null,#68,stack_complex,68,DEPTH,- depth,18,Pushes the current depth of the stack.
    CHKDEPTH,null,#69,stack_complex,69,CHKDEPTH,i -,18/58,Pops integer `i` from the stack, then checks whether there are at least `i` elements, generating a stack underflow exception otherwise.
    ONLYTOPX,null,#6A,stack_complex,6A,ONLYTOPX,null,18,Pops integer `i` from the stack, then removes all but the top `i` elements.
    ONLYX,null,#6B,stack_complex,6B,ONLYX,null,18,Pops integer `i` from the stack, then leaves only the bottom `i` elements. Approximately equivalent to `DEPTH` `SWAP` `SUB` `DROPX`.
    BLKDROP2,null,#6C i:(## 4) j:uint4 {1 <= i},stack_complex,6Cij,[i] [j] BLKDROP2,null,26,Drops `i` stack elements under the top `j` elements.\n`1 <= i <= 15`, `0 <= j <= 15`\nEquivalent to `[i+j] 0 REVERSE` `[i] BLKDROP` `[j] 0 REVERSE`.
    NULL,null,#6D,tuple,6D,NULL\nPUSHNULL, - null,18,Pushes the only value of type _Null_.
    ISNULL,null,#6E,tuple,6E,ISNULL,x - ?,18,Checks whether `x` is a _Null_, and returns `-1` or `0` accordingly.
    TUPLE,null,#6F0 n:uint4,tuple,6F0n,[n] TUPLE,x_1 ... x_n - t,26+n,Creates a new _Tuple_ `t=(x_1, … ,x_n)` containing `n` values `x_1`,..., `x_n`.\n`0 <= n <= 15`
    NIL,TUPLE,#6F00,tuple,6F00,NIL,- t,26,Pushes the only _Tuple_ `t=()` of length zero.
    SINGLE,TUPLE,#6F01,tuple,6F01,SINGLE,x - t,27,Creates a singleton `t:=(x)`, i.e., a _Tuple_ of length one.
    PAIR,TUPLE,#6F02,tuple,6F02,PAIR\nCONS,x y - t,28,Creates pair `t:=(x,y)`.
    TRIPLE,TUPLE,#6F03,tuple,6F03,TRIPLE,x y z - t,29,Creates triple `t:=(x,y,z)`.
    INDEX,null,#6F1 k:uint4,tuple,6F1k,[k] INDEX,t - x,26,Returns the `k`-th element of a _Tuple_ `t`.\n`0 <= k <= 15`.
    FIRST,INDEX,#6F10,tuple,6F10,FIRST\nCAR,t - x,26,Returns the first element of a _Tuple_.
    SECOND,INDEX,#6F11,tuple,6F11,SECOND\nCDR,t - y,26,Returns the second element of a _Tuple_.
    THIRD,INDEX,#6F12,tuple,6F12,THIRD,t - z,26,Returns the third element of a _Tuple_.
    UNTUPLE,null,#6F2 n:uint4,tuple,6F2n,[n] UNTUPLE,t - x_1 ... x_n,26+n,Unpacks a _Tuple_ `t=(x_1,...,x_n)` of length equal to `0 <= n <= 15`.\nIf `t` is not a _Tuple_, or if `|t| != n`, a type check exception is thrown.
    UNSINGLE,UNTUPLE,#6F21,tuple,6F21,UNSINGLE,t - x,27,Unpacks a singleton `t=(x)`.
    UNPAIR,UNTUPLE,#6F22,tuple,6F22,UNPAIR\nUNCONS,t - x y,28,Unpacks a pair `t=(x,y)`.
    UNTRIPLE,UNTUPLE,#6F23,tuple,6F23,UNTRIPLE,t - x y z,29,Unpacks a triple `t=(x,y,z)`.
    UNPACKFIRST,null,#6F3 k:uint4,tuple,6F3k,[k] UNPACKFIRST,t - x_1 ... x_k,26+k,Unpacks first `0 <= k <= 15` elements of a _Tuple_ `t`.\nIf `|t|<k`, throws a type check exception.
    CHKTUPLE,UNPACKFIRST,#6F30,tuple,6F30,CHKTUPLE,t -,26,Checks whether `t` is a _Tuple_. If not, throws a type check exception.
    EXPLODE,null,#6F4 n:uint4,tuple,6F4n,[n] EXPLODE,t - x_1 ... x_m m,26+m,Unpacks a _Tuple_ `t=(x_1,...,x_m)` and returns its length `m`, but only if `m <= n <= 15`. Otherwise throws a type check exception.
    SETINDEX,null,#6F5 k:uint4,tuple,6F5k,[k] SETINDEX,t x - t',26+|t|,Computes _Tuple_ `t'` that differs from `t` only at position `t'_{k+1}`, which is set to `x`.\n`0 <= k <= 15`\nIf `k >= |t|`, throws a range check exception.
    SETFIRST,SETINDEX,#6F50,tuple,6F50,SETFIRST,t x - t',26+|t|,Sets the first component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    SETSECOND,SETINDEX,#6F51,tuple,6F51,SETSECOND,t x - t',26+|t|,Sets the second component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    SETTHIRD,SETINDEX,#6F52,tuple,6F52,SETTHIRD,t x - t',26+|t|,Sets the third component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    INDEXQ,null,#6F6 k:uint4,tuple,6F6k,[k] INDEXQ,t - x,26,Returns the `k`-th element of a _Tuple_ `t`, where `0 <= k <= 15`. In other words, returns `x_{k+1}` if `t=(x_1,...,x_n)`. If `k>=n`, or if `t` is _Null_, returns a _Null_ instead of `x`.
    FIRSTQ,INDEXQ,#6F60,tuple,6F60,FIRSTQ\nCARQ,t - x,26,Returns the first element of a _Tuple_.
    SECONDQ,INDEXQ,#6F61,tuple,6F61,SECONDQ\nCDRQ,t - y,26,Returns the second element of a _Tuple_.
    THIRDQ,INDEXQ,#6F62,tuple,6F62,THIRDQ,t - z,26,Returns the third element of a _Tuple_.
    SETINDEXQ,null,#6F7 k:uint4,tuple,6F7k,[k] SETINDEXQ,t x - t',26+|t’|,Sets the `k`-th component of _Tuple_ `t` to `x`, where `0 <= k < 16`, and returns the resulting _Tuple_ `t'`.\nIf `|t| <= k`, first extends the original _Tuple_ to length `n’=k+1` by setting all new components to _Null_. If the original value of `t` is _Null_, treats it as an empty _Tuple_. If `t` is not _Null_ or _Tuple_, throws an exception. If `x` is _Null_ and either `|t| <= k` or `t` is _Null_, then always returns `t'=t` (and does not consume tuple creation gas).
    SETFIRSTQ,SETINDEXQ,#6F70,tuple,6F70,SETFIRSTQ,t x - t',26+|t’|,Sets the first component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    SETSECONDQ,SETINDEXQ,#6F71,tuple,6F71,SETSECONDQ,t x - t',26+|t’|,Sets the second component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    SETTHIRDQ,SETINDEXQ,#6F72,tuple,6F72,SETTHIRDQ,t x - t',26+|t’|,Sets the third component of _Tuple_ `t` to `x` and returns the resulting _Tuple_ `t'`.
    TUPLEVAR,null,#6F80,tuple,6F80,TUPLEVAR,x_1 ... x_n n - t,26+n,Creates a new _Tuple_ `t` of length `n` similarly to `TUPLE`, but with `0 <= n <= 255` taken from the stack.
    INDEXVAR,null,#6F81,tuple,6F81,INDEXVAR,t k - x,26,Similar to `k INDEX`, but with `0 <= k <= 254` taken from the stack.
    UNTUPLEVAR,null,#6F82,tuple,6F82,UNTUPLEVAR,t n - x_1 ... x_n,26+n,Similar to `n UNTUPLE`, but with `0 <= n <= 255` taken from the stack.
    UNPACKFIRSTVAR,null,#6F83,tuple,6F83,UNPACKFIRSTVAR,t n - x_1 ... x_n,26+n,Similar to `n UNPACKFIRST`, but with `0 <= n <= 255` taken from the stack.
    EXPLODEVAR,null,#6F84,tuple,6F84,EXPLODEVAR,t n - x_1 ... x_m m,26+m,Similar to `n EXPLODE`, but with `0 <= n <= 255` taken from the stack.
    SETINDEXVAR,null,#6F85,tuple,6F85,SETINDEXVAR,t x k - t',26+|t’|,Similar to `k SETINDEX`, but with `0 <= k <= 254` taken from the stack.
    INDEXVARQ,null,#6F86,tuple,6F86,INDEXVARQ,t k - x,26,Similar to `n INDEXQ`, but with `0 <= k <= 254` taken from the stack.
    SETINDEXVARQ,null,#6F87,tuple,6F87,SETINDEXVARQ,t x k - t',26+|t’|,Similar to `k SETINDEXQ`, but with `0 <= k <= 254` taken from the stack.
    TLEN,null,#6F88,tuple,6F88,TLEN,t - n,26,Returns the length of a _Tuple_.
    QTLEN,null,#6F89,tuple,6F89,QTLEN,t - n or -1,26,Similar to `TLEN`, but returns `-1` if `t` is not a _Tuple_.
    ISTUPLE,null,#6F8A,tuple,6F8A,ISTUPLE,t - ?,26,Returns `-1` or `0` depending on whether `t` is a _Tuple_.
    LAST,null,#6F8B,tuple,6F8B,LAST,t - x,26,Returns the last element of a non-empty _Tuple_ `t`.
    TPUSH,null,#6F8C,tuple,6F8C,TPUSH\nCOMMA,t x - t',26+|t’|,Appends a value `x` to a _Tuple_ `t=(x_1,...,x_n)`, but only if the resulting _Tuple_ `t'=(x_1,...,x_n,x)` is of length at most 255. Otherwise throws a type check exception.
    TPOP,null,#6F8D,tuple,6F8D,TPOP,t - t' x,26+|t’|,Detaches the last element `x=x_n` from a non-empty _Tuple_ `t=(x_1,...,x_n)`, and returns both the resulting _Tuple_ `t'=(x_1,...,x_{n-1})` and the original last element `x`.
    NULLSWAPIF,null,#6FA0,tuple,6FA0,NULLSWAPIF,x - x or null x,26,Pushes a _Null_ under the topmost _Integer_ `x`, but only if `x!=0`.
    NULLSWAPIFNOT,null,#6FA1,tuple,6FA1,NULLSWAPIFNOT,x - x or null x,26,Pushes a _Null_ under the topmost _Integer_ `x`, but only if `x=0`. May be used for stack alignment after quiet primitives such as `PLDUXQ`.
    NULLROTRIF,null,#6FA2,tuple,6FA2,NULLROTRIF,x y - x y or null x y,26,Pushes a _Null_ under the second stack entry from the top, but only if the topmost _Integer_ `y` is non-zero.
    NULLROTRIFNOT,null,#6FA3,tuple,6FA3,NULLROTRIFNOT,x y - x y or null x y,26,Pushes a _Null_ under the second stack entry from the top, but only if the topmost _Integer_ `y` is zero. May be used for stack alignment after quiet primitives such as `LDUXQ`.
    NULLSWAPIF2,null,#6FA4,tuple,6FA4,NULLSWAPIF2,x - x or null null x,26,Pushes two nulls under the topmost _Integer_ `x`, but only if `x!=0`.\nEquivalent to `NULLSWAPIF` `NULLSWAPIF`.
    NULLSWAPIFNOT2,null,#6FA5,tuple,6FA5,NULLSWAPIFNOT2,x - x or null null x,26,Pushes two nulls under the topmost _Integer_ `x`, but only if `x=0`.\nEquivalent to `NULLSWAPIFNOT` `NULLSWAPIFNOT`.
    NULLROTRIF2,null,#6FA6,tuple,6FA6,NULLROTRIF2,x y - x y or null null x y,26,Pushes two nulls under the second stack entry from the top, but only if the topmost _Integer_ `y` is non-zero.\nEquivalent to `NULLROTRIF` `NULLROTRIF`.
    NULLROTRIFNOT2,null,#6FA7,tuple,6FA7,NULLROTRIFNOT2,x y - x y or null null x y,26,Pushes two nulls under the second stack entry from the top, but only if the topmost _Integer_ `y` is zero.\nEquivalent to `NULLROTRIFNOT` `NULLROTRIFNOT`.
    INDEX2,null,#6FB i:uint2 j:uint2,tuple,6FBij,[i] [j] INDEX2,t - x,26,Recovers `x=(t_{i+1})_{j+1}` for `0 <= i,j <= 3`.\nEquivalent to `[i] INDEX` `[j] INDEX`.
    CADR,INDEX2,#6FB4,tuple,6FB4,CADR,t - x,26,Recovers `x=(t_2)_1`.
    CDDR,INDEX2,#6FB5,tuple,6FB5,CDDR,t - x,26,Recovers `x=(t_2)_2`.
    INDEX3,null,#6FE_ i:uint2 j:uint2 k:uint2,tuple,6FE_ijk,[i] [j] [k] INDEX3,t - x,26,Recovers `x=t_{i+1}_{j+1}_{k+1}`.\n`0 <= i,j,k <= 3`\nEquivalent to `[i] [j] INDEX2` `[k] INDEX`.
    CADDR,INDEX3,#6FD4,tuple,6FD4,CADDR,t - x,26,Recovers `x=t_2_2_1`.
    CDDDR,INDEX3,#6FD5,tuple,6FD5,CDDDR,t - x,26,Recovers `x=t_2_2_2`.
    PUSHINT_4,null,#7 i:uint4,const_int,7i,[x] PUSHINT\n[x] INT,- x,18,Pushes integer `x` into the stack. `-5 <= x <= 10`.\nHere `i` equals four lower-order bits of `x` (`i=x mod 16`).
    ZERO,PUSHINT_4,#70,const_int,70,ZERO\nFALSE,- 0,18,null
    ONE,PUSHINT_4,#71,const_int,71,ONE,- 1,18,null
    TWO,PUSHINT_4,#72,const_int,72,TWO,- 2,18,null
    TEN,PUSHINT_4,#7A,const_int,7A,TEN,- 10,18,null
    TRUE,PUSHINT_4,#7F,const_int,7F,TRUE,- -1,18,null
    PUSHINT_8,null,#80 xx:int8,const_int,80xx,[xx] PUSHINT\n[xx] INT,- xx,26,Pushes integer `xx`. `-128 <= xx <= 127`.
    PUSHINT_16,null,#81 xxxx:int16,const_int,81xxxx,[xxxx] PUSHINT\n[xxxx] INT,- xxxx,34,Pushes integer `xxxx`. `-2^15 <= xx < 2^15`.
    PUSHINT_LONG,null,#82 l:(## 5) xxx:(int (8 * l + 19)),const_int,82lxxx,[xxx] PUSHINT\n[xxx] INT,- xxx,23,Pushes integer `xxx`.\n_Details:_ 5-bit `0 <= l <= 30` determines the length `n=8l+19` of signed big-endian integer `xxx`.\nThe total length of this instruction is `l+4` bytes or `n+13=8l+32` bits.
    PUSHPOW2,null,#83 xx:uint8,const_int,83xx,[xx+1] PUSHPOW2,- 2^(xx+1),26,(Quietly) pushes `2^(xx+1)` for `0 <= xx <= 255`.\n`2^256` is a `NaN`.
    PUSHNAN,PUSHPOW2,#83FF,const_int,83FF,PUSHNAN,- NaN,26,Pushes a `NaN`.
    PUSHPOW2DEC,null,#84 xx:uint8,const_int,84xx,[xx+1] PUSHPOW2DEC,- 2^(xx+1)-1,26,Pushes `2^(xx+1)-1` for `0 <= xx <= 255`.
    PUSHNEGPOW2,null,#85 xx:uint8,const_int,85xx,[xx+1] PUSHNEGPOW2,- -2^(xx+1),26,Pushes `-2^(xx+1)` for `0 <= xx <= 255`.
    PUSHREF,null,#88 c:^Cell,const_data,88,[ref] PUSHREF,- c,18,Pushes the reference `ref` into the stack.\n_Details:_ Pushes the first reference of `cc.code` into the stack as a _Cell_ (and removes this reference from the current continuation).
    PUSHREFSLICE,null,#89 c:^Cell,const_data,89,[ref] PUSHREFSLICE,- s,118/43,Similar to `PUSHREF`, but converts the cell into a _Slice_.
    PUSHREFCONT,null,#8A c:^Cell,const_data,8A,[ref] PUSHREFCONT,- cont,118/43,Similar to `PUSHREFSLICE`, but makes a simple ordinary _Continuation_ out of the cell.
    PUSHSLICE,null,#8B x:(## 4) sss:((8 * x + 4) * Bit),const_data,8Bxsss,[slice] PUSHSLICE\n[slice] SLICE,- s,22,Pushes the slice `slice` into the stack.\n_Details:_ Pushes the (prefix) subslice of `cc.code` consisting of its first `8x+4` bits and no references (i.e., essentially a bitstring), where `0 <= x <= 15`.\nA completion tag is assumed, meaning that all trailing zeroes and the last binary one (if present) are removed from this bitstring.\nIf the original bitstring consists only of zeroes, an empty slice will be pushed.
    PUSHSLICE_REFS,null,#8C r:(## 2) xx:(## 5) c:((r + 1) * ^Cell) ssss:((8 * xx + 1) * Bit),const_data,8Crxxssss,[slice] PUSHSLICE\n[slice] SLICE,- s,25,Pushes the slice `slice` into the stack.\n_Details:_ Pushes the (prefix) subslice of `cc.code` consisting of its first `1 <= r+1 <= 4` references and up to first `8xx+1` bits of data, with `0 <= xx <= 31`.\nA completion tag is also assumed.
    PUSHSLICE_LONG,null,#8D r:(#<= 4) xx:(## 7) c:(r * ^Cell) ssss:((8 * xx + 6) * Bit),const_data,8Drxxsssss,[slice] PUSHSLICE\n[slice] SLICE,- s,28,Pushes the slice `slice` into the stack.\n_Details:_ Pushes the subslice of `cc.code` consisting of `0 <= r <= 4` references and up to `8xx+6` bits of data, with `0 <= xx <= 127`.\nA completion tag is assumed.
    null,null,null,const_data,null,x{} PUSHSLICE\nx{ABCD1234} PUSHSLICE\nb{01101} PUSHSLICE,- s,null,Examples of `PUSHSLICE`.\n`x{}` is an empty slice. `x{...}` is a hexadecimal literal. `b{...}` is a binary literal.\nMore on slice literals [here](https://github.com/Piterden/TON-docs/blob/master/Fift.%20A%20Brief%20Introduction.md#user-content-51-slice-literals).\nNote that the assembler can replace `PUSHSLICE` with `PUSHREFSLICE` in certain situations (e.g. if there’s not enough space in the current continuation).
    null,null,null,const_data,null,<b x{AB12} s, b> PUSHREF\n<b x{AB12} s, b> PUSHREFSLICE,- c/s,null,Examples of `PUSHREF` and `PUSHREFSLICE`.\nMore on building cells in fift [here](https://github.com/Piterden/TON-docs/blob/master/Fift.%20A%20Brief%20Introduction.md#user-content-52-builder-primitives).
    PUSHCONT,null,#8F_ r:(## 2) xx:(## 7) c:(r * ^Cell) ssss:((8 * xx) * Bit),const_data,8F_rxxcccc,[builder] PUSHCONT\n[builder] CONT,- c,26,Pushes a continuation made from `builder`.\n_Details:_ Pushes the simple ordinary continuation `cccc` made from the first `0 <= r <= 3` references and the first `0 <= xx <= 127` bytes of `cc.code`.
    PUSHCONT_SHORT,null,#9 x:(## 4) ssss:((8 * x) * Bit),const_data,9xccc,[builder] PUSHCONT\n[builder] CONT,- c,18,Pushes a continuation made from `builder`.\n_Details:_ Pushes an `x`-byte continuation for `0 <= x <= 15`.
    null,null,null,const_data,null,<{ code }> PUSHCONT\n<{ code }> CONT\nCONT:<{ code }>,- c,null,Pushes a continuation with code `code`.\nNote that the assembler can replace `PUSHCONT` with `PUSHREFCONT` in certain situations (e.g. if there’s not enough space in the current continuation).
    ADD,null,#A0,arithm_basic,A0,ADD,x y - x+y,18,null
    SUB,null,#A1,arithm_basic,A1,SUB,x y - x-y,18,null
    SUBR,null,#A2,arithm_basic,A2,SUBR,x y - y-x,18,Equivalent to `SWAP` `SUB`.
    NEGATE,null,#A3,arithm_basic,A3,NEGATE,x - -x,18,Equivalent to `-1 MULCONST` or to `ZERO SUBR`.\nNotice that it triggers an integer overflow exception if `x=-2^256`.
    INC,null,#A4,arithm_basic,A4,INC,x - x+1,18,Equivalent to `1 ADDCONST`.
    DEC,null,#A5,arithm_basic,A5,DEC,x - x-1,18,Equivalent to `-1 ADDCONST`.
    ADDCONST,null,#A6 cc:int8,arithm_basic,A6cc,[cc] ADDCONST\n[cc] ADDINT\n[-cc] SUBCONST\n[-cc] SUBINT,x - x+cc,26,`-128 <= cc <= 127`.
    MULCONST,null,#A7 cc:int8,arithm_basic,A7cc,[cc] MULCONST\n[cc] MULINT,x - x*cc,26,`-128 <= cc <= 127`.
    MUL,null,#A8,arithm_basic,A8,MUL,x y - x*y,18,null
    DIV_BASE,null,#A9 m:uint1 s:uint2 cdft:(Either [ d:uint2 f:uint2 ] [ d:uint2 f:uint2 tt:uint8 ]),arithm_div,A9mscdf,null,null,26,This is the general encoding of division, with an optional pre-multiplication and an optional replacement of the division or multiplication by a shift. Variable fields are as follows:\n`0 <= m <= 1`  -  Indicates whether there is pre-multiplication (`MULDIV` and its variants), possibly replaced by a left shift.\n`0 <= s <= 2`  -  Indicates whether either the multiplication or the division have been replaced by shifts: `s=0` - no replacement, `s=1` - division replaced by a right shift, `s=2` - multiplication replaced by a left shift (possible only for `m=1`).\n`0 <= c <= 1`  -  Indicates whether there is a constant one-byte argument `tt` for the shift operator (if `s!=0`). For `s=0`, `c=0`. If `c=1`, then `0 <= tt <= 255`, and the shift is performed by `tt+1` bits. If `s!=0` and `c=0`, then the shift amount is provided to the instruction as a top-of-stack _Integer_ in range `0...256`.\n`1 <= d <= 3`  -  Indicates which results of division are required: `1` - only the quotient, `2` - only the remainder, `3` - both.\n`0 <= f <= 2`  -  Rounding mode: `0` - floor, `1` - nearest integer, `2` - ceiling.\nAll instructions below are variants of this.
    DIV,DIV_BASE,#A904,arithm_div,A904,DIV,x y - q,26,`q=floor(x/y)`, `r=x-y*q`
    DIVR,DIV_BASE,#A905,arithm_div,A905,DIVR,x y - q’,26,`q’=round(x/y)`, `r’=x-y*q’`
    DIVC,DIV_BASE,#A906,arithm_div,A906,DIVC,x y - q'',26,`q’’=ceil(x/y)`, `r’’=x-y*q’’`
    MOD,DIV_BASE,#A908,arithm_div,A908,MOD,x y - r,26,null
    DIVMOD,DIV_BASE,#A90C,arithm_div,A90C,DIVMOD,x y - q r,26,null
    DIVMODR,DIV_BASE,#A90D,arithm_div,A90D,DIVMODR,x y - q' r',26,null
    DIVMODC,DIV_BASE,#A90E,arithm_div,A90E,DIVMODC,x y - q'' r'',26,null
    RSHIFTR_VAR,DIV_BASE,#A925,arithm_div,A925,RSHIFTR,x y - round(x/2^y),26,null
    RSHIFTC_VAR,DIV_BASE,#A926,arithm_div,A926,RSHIFTC,x y - ceil(x/2^y),34,null
    RSHIFTR,DIV_BASE,#A935 tt:uint8,arithm_div,A935tt,[tt+1] RSHIFTR#,x y - round(x/2^(tt+1)),34,null
    RSHIFTC,DIV_BASE,#A936 tt:uint8,arithm_div,A936tt,[tt+1] RSHIFTC#,x y - ceil(x/2^(tt+1)),34,null
    MODPOW2,DIV_BASE,#A938 tt:uint8,arithm_div,A938tt,[tt+1] MODPOW2#,x - x mod 2^(tt+1),26,null
    MULDIV,DIV_BASE,#A984,arithm_div,A98,MULDIV,x y z - q,26,`q=floor(x*y/z)`
    MULDIVR,DIV_BASE,#A985,arithm_div,A985,MULDIVR,x y z - q',26,`q'=round(x*y/z)`
    MULDIVMOD,DIV_BASE,#A98C,arithm_div,A98C,MULDIVMOD,x y z - q r,26,`q=floor(x*y/z)`, `r=x*y-z*q`
    MULRSHIFT_VAR,DIV_BASE,#A9A4,arithm_div,A9A4,MULRSHIFT,x y z - floor(x*y/2^z),26,`0 <= z <= 256`
    MULRSHIFTR_VAR,DIV_BASE,#A9A5,arithm_div,A9A5,MULRSHIFTR,x y z - round(x*y/2^z),26,`0 <= z <= 256`
    MULRSHIFTC_VAR,DIV_BASE,#A9A6,arithm_div,A9A6,MULRSHIFTC,x y z - ceil(x*y/2^z),34,`0 <= z <= 256`
    MULRSHIFT,DIV_BASE,#A9B4 tt:uint8,arithm_div,A9B4tt,[tt+1] MULRSHIFT#,x y - floor(x*y/2^(tt+1)),34,null
    MULRSHIFTR,DIV_BASE,#A9B5 tt:uint8,arithm_div,A9B5tt,[tt+1] MULRSHIFTR#,x y - round(x*y/2^(tt+1)),34,null
    MULRSHIFTC,DIV_BASE,#A9B6 tt:uint8,arithm_div,A9B6tt,[tt+1] MULRSHIFTC#,x y - ceil(x*y/2^(tt+1)),26,null
    LSHIFTDIV_VAR,DIV_BASE,#A9C4,arithm_div,A9C4,LSHIFTDIV,x y z - floor(2^z*x/y),26,`0 <= z <= 256`
    LSHIFTDIVR_VAR,DIV_BASE,#A9C5,arithm_div,A9C5,LSHIFTDIVR,x y z - round(2^z*x/y),26,`0 <= z <= 256`
    LSHIFTDIVC_VAR,DIV_BASE,#A9C6,arithm_div,A9C6,LSHIFTDIVC,x y z - ceil(2^z*x/y),34,`0 <= z <= 256`
    LSHIFTDIV,DIV_BASE,#A9D4 tt:uint8,arithm_div,A9D4tt,[tt+1] LSHIFT#DIV,x y - floor(2^(tt+1)*x/y),34,null
    LSHIFTDIVR,DIV_BASE,#A9D5 tt:uint8,arithm_div,A9D5tt,[tt+1] LSHIFT#DIVR,x y - round(2^(tt+1)*x/y),34,null
    LSHIFTDIVC,DIV_BASE,#A9D6 tt:uint8,arithm_div,A9D6tt,[tt+1] LSHIFT#DIVC,x y - ceil(2^(tt+1)*x/y),26,null
    LSHIFT,null,#AA cc:uint8,arithm_logical,AAcc,[cc+1] LSHIFT#,x - x*2^(cc+1),26,`0 <= cc <= 255`
    RSHIFT,null,#AB cc:uint8,arithm_logical,ABcc,[cc+1] RSHIFT#,x - floor(x/2^(cc+1)),18,`0 <= cc <= 255`
    LSHIFT_VAR,null,#AC,arithm_logical,AC,LSHIFT,x y - x*2^y,18,`0 <= y <= 1023`
    RSHIFT_VAR,null,#AD,arithm_logical,AD,RSHIFT,x y - floor(x/2^y),18,`0 <= y <= 1023`
    POW2,null,#AE,arithm_logical,AE,POW2,y - 2^y,18,`0 <= y <= 1023`\nEquivalent to `ONE` `SWAP` `LSHIFT`.
    AND,null,#B0,arithm_logical,B0,AND,x y - x&y,18,Bitwise and of two signed integers `x` and `y`, sign-extended to infinity.
    OR,null,#B1,arithm_logical,B1,OR,x y - x|y,18,Bitwise or of two integers.
    XOR,null,#B2,arithm_logical,B2,XOR,x y - x xor y,18,Bitwise xor of two integers.
    NOT,null,#B3,arithm_logical,B3,NOT,x - ~x,26,Bitwise not of an integer.
    FITS,null,#B4 cc:uint8,arithm_logical,B4cc,[cc+1] FITS,x - x,26/76,Checks whether `x` is a `cc+1`-bit signed integer for `0 <= cc <= 255` (i.e., whether `-2^cc <= x < 2^cc`).\nIf not, either triggers an integer overflow exception, or replaces `x` with a `NaN` (quiet version).
    CHKBOOL,FITS,#B400,arithm_logical,B400,CHKBOOL,x - x,26/76,Checks whether `x` is a “boolean value'' (i.e., either 0 or -1).
    UFITS,null,#B5 cc:uint8,arithm_logical,B5cc,[cc+1] UFITS,x - x,26/76,Checks whether `x` is a `cc+1`-bit unsigned integer for `0 <= cc <= 255` (i.e., whether `0 <= x < 2^(cc+1)`).
    CHKBIT,UFITS,#B500,arithm_logical,B500,CHKBIT,x - x,26/76,Checks whether `x` is a binary digit (i.e., zero or one).
    FITSX,null,#B600,arithm_logical,B600,FITSX,x c - x,26/76,Checks whether `x` is a `c`-bit signed integer for `0 <= c <= 1023`.
    UFITSX,null,#B601,arithm_logical,B601,UFITSX,x c - x,26/76,Checks whether `x` is a `c`-bit unsigned integer for `0 <= c <= 1023`.
    BITSIZE,null,#B602,arithm_logical,B602,BITSIZE,x - c,26,Computes smallest `c >= 0` such that `x` fits into a `c`-bit signed integer (`-2^(c-1) <= c < 2^(c-1)`).
    UBITSIZE,null,#B603,arithm_logical,B603,UBITSIZE,x - c,26,Computes smallest `c >= 0` such that `x` fits into a `c`-bit unsigned integer (`0 <= x < 2^c`), or throws a range check exception.
    MIN,null,#B608,arithm_logical,B608,MIN,x y - x or y,26,Computes the minimum of two integers `x` and `y`.
    MAX,null,#B609,arithm_logical,B609,MAX,x y - x or y,26,Computes the maximum of two integers `x` and `y`.
    MINMAX,null,#B60A,arithm_logical,B60A,MINMAX\nINTSORT2,x y - x y or y x,26,Sorts two integers. Quiet version of this operation returns two `NaN`s if any of the arguments are `NaN`s.
    ABS,null,#B60B,arithm_logical,B60B,ABS,x - |x|,26,Computes the absolute value of an integer `x`.
    QADD,null,#B7A0,arithm_quiet,B7A0,QADD,x y - x+y,26,null
    QSUB,null,#B7A1,arithm_quiet,B7A1,QSUB,x y - x-y,26,null
    QSUBR,null,#B7A2,arithm_quiet,B7A2,QSUBR,x y - y-x,26,null
    QNEGATE,null,#B7A3,arithm_quiet,B7A3,QNEGATE,x - -x,26,null
    QINC,null,#B7A4,arithm_quiet,B7A4,QINC,x - x+1,26,null
    QDEC,null,#B7A5,arithm_quiet,B7A5,QDEC,x - x-1,26,null
    QMUL,null,#B7A8,arithm_quiet,B7A8,QMUL,x y - x*y,26,null
    QDIV,null,#B7A904,arithm_quiet,B7A904,QDIV,x y - q,34,Division returns `NaN` if `y=0`.
    QDIVR,null,#B7A905,arithm_quiet,B7A905,QDIVR,x y - q’,34,null
    QDIVC,null,#B7A906,arithm_quiet,B7A906,QDIVC,x y - q'',34,null
    QMOD,null,#B7A908,arithm_quiet,B7A908,QMOD,x y - r,34,null
    QDIVMOD,null,#B7A90C,arithm_quiet,B7A90C,QDIVMOD,x y - q r,34,null
    QDIVMODR,null,#B7A90D,arithm_quiet,B7A90D,QDIVMODR,x y - q' r',34,null
    QDIVMODC,null,#B7A90E,arithm_quiet,B7A90E,QDIVMODC,x y - q'' r'',34,null
    QMULDIVR,null,#B7A985,arithm_quiet,B7A985,QMULDIVR,x y z - q',34,null
    QMULDIVMOD,null,#B7A98C,arithm_quiet,B7A98C,QMULDIVMOD,x y z - q r,34,null
    QLSHIFT,null,#B7AC,arithm_quiet,B7AC,QLSHIFT,x y - x*2^y,26,null
    QRSHIFT,null,#B7AD,arithm_quiet,B7AD,QRSHIFT,x y - floor(x/2^y),26,null
    QPOW2,null,#B7AE,arithm_quiet,B7AE,QPOW2,y - 2^y,26,null
    QAND,null,#B7B0,arithm_quiet,B7B0,QAND,x y - x&y,26,null
    QOR,null,#B7B1,arithm_quiet,B7B1,QOR,x y - x|y,26,null
    QXOR,null,#B7B2,arithm_quiet,B7B2,QXOR,x y - x xor y,26,null
    QNOT,null,#B7B3,arithm_quiet,B7B3,QNOT,x - ~x,26,null
    QFITS,null,#B7B4 cc:uint8,arithm_quiet,B7B4cc,[cc+1] QFITS,x - x,34,Replaces `x` with a `NaN` if x is not a `cc+1`-bit signed integer, leaves it intact otherwise.
    QUFITS,null,#B7B5 cc:uint8,arithm_quiet,B7B5cc,[cc+1] QUFITS,x - x,34,Replaces `x` with a `NaN` if x is not a `cc+1`-bit unsigned integer, leaves it intact otherwise.
    QFITSX,null,#B7B600,arithm_quiet,B7B600,QFITSX,x c - x,34,Replaces `x` with a `NaN` if x is not a c-bit signed integer, leaves it intact otherwise.
    QUFITSX,null,#B7B601,arithm_quiet,B7B601,QUFITSX,x c - x,34,Replaces `x` with a `NaN` if x is not a c-bit unsigned integer, leaves it intact otherwise.
    SGN,null,#B8,compare_int,B8,SGN,x - sgn(x),18,Computes the sign of an integer `x`:\n`-1` if `x<0`, `0` if `x=0`, `1` if `x>0`.
    LESS,null,#B9,compare_int,B9,LESS,x y - x<y,18,Returns `-1` if `x<y`, `0` otherwise.
    EQUAL,null,#BA,compare_int,BA,EQUAL,x y - x=y,18,Returns `-1` if `x=y`, `0` otherwise.
    LEQ,null,#BB,compare_int,BB,LEQ,x y - x<=y,18,null
    GREATER,null,#BC,compare_int,BC,GREATER,x y - x>y,18,null
    NEQ,null,#BD,compare_int,BD,NEQ,x y - x!=y,18,Equivalent to `EQUAL` `NOT`.
    GEQ,null,#BE,compare_int,BE,GEQ,x y - x>=y,18,Equivalent to `LESS` `NOT`.
    CMP,null,#BF,compare_int,BF,CMP,x y - sgn(x-y),18,Computes the sign of `x-y`:\n`-1` if `x<y`, `0` if `x=y`, `1` if `x>y`.\nNo integer overflow can occur here unless `x` or `y` is a `NaN`.
    EQINT,null,#C0 yy:int8,compare_int,C0yy,[yy] EQINT,x - x=yy,26,Returns `-1` if `x=yy`, `0` otherwise.\n`-2^7 <= yy < 2^7`.
    ISZERO,EQINT,#C000,compare_int,C000,ISZERO,x - x=0,26,Checks whether an integer is zero. Corresponds to Forth's `0=`.
    LESSINT,null,#C1 yy:int8,compare_int,C1yy,[yy] LESSINT\n[yy-1] LEQINT,x - x<yy,26,Returns `-1` if `x<yy`, `0` otherwise.\n`-2^7 <= yy < 2^7`.
    ISNEG,LESSINT,#C100,compare_int,C100,ISNEG,x - x<0,26,Checks whether an integer is negative. Corresponds to Forth's `0<`.
    ISNPOS,LESSINT,#C101,compare_int,C101,ISNPOS,x - x<=0,26,Checks whether an integer is non-positive.
    GTINT,null,#C2 yy:int8,compare_int,C2yy,[yy] GTINT\n[yy+1] GEQINT,x - x>yy,26,Returns `-1` if `x>yy`, `0` otherwise.\n`-2^7 <= yy < 2^7`.
    ISPOS,GTINT,#C200,compare_int,C200,ISPOS,x - x>0,26,Checks whether an integer is positive. Corresponds to Forth's `0>`.
    ISNNEG,GTINT,#C2FF,compare_int,C2FF,ISNNEG,x - x >=0,26,Checks whether an integer is non-negative.
    NEQINT,null,#C3 yy:int8,compare_int,C3yy,[yy] NEQINT,x - x!=yy,26,Returns `-1` if `x!=yy`, `0` otherwise.\n`-2^7 <= yy < 2^7`.
    ISNAN,null,#C4,compare_int,C4,ISNAN,x - x=NaN,18,Checks whether `x` is a `NaN`.
    CHKNAN,null,#C5,compare_int,C5,CHKNAN,x - x,18/68,Throws an arithmetic overflow exception if `x` is a `NaN`.
    SEMPTY,null,#C700,compare_other,C700,SEMPTY,s - ?,26,Checks whether a _Slice_ `s` is empty (i.e., contains no bits of data and no cell references).
    SDEMPTY,null,#C701,compare_other,C701,SDEMPTY,s - ?,26,Checks whether _Slice_ `s` has no bits of data.
    SREMPTY,null,#C702,compare_other,C702,SREMPTY,s - ?,26,Checks whether _Slice_ `s` has no references.
    SDFIRST,null,#C703,compare_other,C703,SDFIRST,s - ?,26,Checks whether the first bit of _Slice_ `s` is a one.
    SDLEXCMP,null,#C704,compare_other,C704,SDLEXCMP,s s' - x,26,Compares the data of `s` lexicographically with the data of `s'`, returning `-1`, 0, or 1 depending on the result.
    SDEQ,null,#C705,compare_other,C705,SDEQ,s s' - ?,26,Checks whether the data parts of `s` and `s'` coincide, equivalent to `SDLEXCMP` `ISZERO`.
    SDPFX,null,#C708,compare_other,C708,SDPFX,s s' - ?,26,Checks whether `s` is a prefix of `s'`.
    SDPFXREV,null,#C709,compare_other,C709,SDPFXREV,s s' - ?,26,Checks whether `s'` is a prefix of `s`, equivalent to `SWAP` `SDPFX`.
    SDPPFX,null,#C70A,compare_other,C70A,SDPPFX,s s' - ?,26,Checks whether `s` is a proper prefix of `s'` (i.e., a prefix distinct from `s'`).
    SDPPFXREV,null,#C70B,compare_other,C70B,SDPPFXREV,s s' - ?,26,Checks whether `s'` is a proper prefix of `s`.
    SDSFX,null,#C70C,compare_other,C70C,SDSFX,s s' - ?,26,Checks whether `s` is a suffix of `s'`.
    SDSFXREV,null,#C70D,compare_other,C70D,SDSFXREV,s s' - ?,26,Checks whether `s'` is a suffix of `s`.
    SDPSFX,null,#C70E,compare_other,C70E,SDPSFX,s s' - ?,26,Checks whether `s` is a proper suffix of `s'`.
    SDPSFXREV,null,#C70F,compare_other,C70F,SDPSFXREV,s s' - ?,26,Checks whether `s'` is a proper suffix of `s`.
    SDCNTLEAD0,null,#C710,compare_other,C710,SDCNTLEAD0,s - n,26,Returns the number of leading zeroes in `s`.
    SDCNTLEAD1,null,#C711,compare_other,C711,SDCNTLEAD1,s - n,26,Returns the number of leading ones in `s`.
    SDCNTTRAIL0,null,#C712,compare_other,C712,SDCNTTRAIL0,s - n,26,Returns the number of trailing zeroes in `s`.
    SDCNTTRAIL1,null,#C713,compare_other,C713,SDCNTTRAIL1,s - n,26,Returns the number of trailing ones in `s`.
    NEWC,null,#C8,cell_build,C8,NEWC,- b,18,Creates a new empty _Builder_.
    ENDC,null,#C9,cell_build,C9,ENDC,b - c,518,Converts a _Builder_ into an ordinary _Cell_.
    STI,null,#CA cc:uint8,cell_build,CAcc,[cc+1] STI,x b - b',26,Stores a signed `cc+1`-bit integer `x` into _Builder_ `b` for `0 <= cc <= 255`, throws a range check exception if `x` does not fit into `cc+1` bits.
    STU,null,#CB cc:uint8,cell_build,CBcc,[cc+1] STU,x b - b',26,Stores an unsigned `cc+1`-bit integer `x` into _Builder_ `b`. In all other respects it is similar to `STI`.
    STREF,null,#CC,cell_build,CC,STREF,c b - b',18,Stores a reference to _Cell_ `c` into _Builder_ `b`.
    STBREFR,null,#CD,cell_build,CD,STBREFR\nENDCST,b b'' - b,518,Equivalent to `ENDC` `SWAP` `STREF`.
    STSLICE,null,#CE,cell_build,CE,STSLICE,s b - b',18,Stores _Slice_ `s` into _Builder_ `b`.
    STIX,null,#CF00,cell_build,CF00,STIX,x b l - b',26,Stores a signed `l`-bit integer `x` into `b` for `0 <= l <= 257`.
    STUX,null,#CF01,cell_build,CF01,STUX,x b l - b',26,Stores an unsigned `l`-bit integer `x` into `b` for `0 <= l <= 256`.
    STIXR,null,#CF02,cell_build,CF02,STIXR,b x l - b',26,Similar to `STIX`, but with arguments in a different order.
    STUXR,null,#CF03,cell_build,CF03,STUXR,b x l - b',26,Similar to `STUX`, but with arguments in a different order.
    STIXQ,null,#CF04,cell_build,CF04,STIXQ,x b l - x b f or b' 0,26,A quiet version of `STIX`. If there is no space in `b`, sets `b'=b` and `f=-1`.\nIf `x` does not fit into `l` bits, sets `b'=b` and `f=1`.\nIf the operation succeeds, `b'` is the new _Builder_ and `f=0`.\nHowever, `0 <= l <= 257`, with a range check exception if this is not so.
    STUXQ,null,#CF05,cell_build,CF05,STUXQ,x b l - x b f or b' 0,26,A quiet version of `STUX`.
    STIXRQ,null,#CF06,cell_build,CF06,STIXRQ,b x l - b x f or b' 0,26,A quiet version of `STIXR`.
    STUXRQ,null,#CF07,cell_build,CF07,STUXRQ,b x l - b x f or b' 0,26,A quiet version of `STUXR`.
    STI_ALT,null,#CF08 cc:uint8,cell_build,CF08cc,[cc+1] STI_l,x b - b',34,A longer version of `[cc+1] STI`.
    STU_ALT,null,#CF09 cc:uint8,cell_build,CF09cc,[cc+1] STU_l,x b - b',34,A longer version of `[cc+1] STU`.
    STIR,null,#CF0A cc:uint8,cell_build,CF0Acc,[cc+1] STIR,b x - b',34,Equivalent to `SWAP` `[cc+1] STI`.
    STUR,null,#CF0B cc:uint8,cell_build,CF0Bcc,[cc+1] STUR,b x - b',34,Equivalent to `SWAP` `[cc+1] STU`.
    STIQ,null,#CF0C cc:uint8,cell_build,CF0Ccc,[cc+1] STIQ,x b - x b f or b' 0,34,A quiet version of `STI`.
    STUQ,null,#CF0D cc:uint8,cell_build,CF0Dcc,[cc+1] STUQ,x b - x b f or b' 0,34,A quiet version of `STU`.
    STIRQ,null,#CF0E cc:uint8,cell_build,CF0Ecc,[cc+1] STIRQ,b x - b x f or b' 0,34,A quiet version of `STIR`.
    STURQ,null,#CF0F cc:uint8,cell_build,CF0Fcc,[cc+1] STURQ,b x - b x f or b' 0,34,A quiet version of `STUR`.
    STREF_ALT,null,#CF10,cell_build,CF10,STREF_l,c b - b',26,A longer version of `STREF`.
    STBREF,null,#CF11,cell_build,CF11,STBREF,b' b - b'',526,Equivalent to `SWAP` `STBREFR`.
    STSLICE_ALT,null,#CF12,cell_build,CF12,STSLICE_l,s b - b',26,A longer version of `STSLICE`.
    STB,null,#CF13,cell_build,CF13,STB,b' b - b'',26,Appends all data from _Builder_ `b'` to _Builder_ `b`.
    STREFR,null,#CF14,cell_build,CF14,STREFR,b c - b',26,Equivalent to `SWAP` `STREF`.
    STBREFR_ALT,null,#CF15,cell_build,CF15,STBREFR_l,b b' - b'',526,A longer encoding of `STBREFR`.
    STSLICER,null,#CF16,cell_build,CF16,STSLICER,b s - b',26,Equivalent to `SWAP` `STSLICE`.
    STBR,null,#CF17,cell_build,CF17,STBR\nBCONCAT,b b' - b'',26,Concatenates two builders.\nEquivalent to `SWAP` `STB`.
    STREFQ,null,#CF18,cell_build,CF18,STREFQ,c b - c b -1 or b' 0,26,Quiet version of `STREF`.
    STBREFQ,null,#CF19,cell_build,CF19,STBREFQ,b' b - b' b -1 or b'' 0,526,Quiet version of `STBREF`.
    STSLICEQ,null,#CF1A,cell_build,CF1A,STSLICEQ,s b - s b -1 or b' 0,26,Quiet version of `STSLICE`.
    STBQ,null,#CF1B,cell_build,CF1B,STBQ,b' b - b' b -1 or b'' 0,26,Quiet version of `STB`.
    STREFRQ,null,#CF1C,cell_build,CF1C,STREFRQ,b c - b c -1 or b' 0,26,Quiet version of `STREFR`.
    STBREFRQ,null,#CF1D,cell_build,CF1D,STBREFRQ,b b' - b b' -1 or b'' 0,526,Quiet version of `STBREFR`.
    STSLICERQ,null,#CF1E,cell_build,CF1E,STSLICERQ,b s - b s -1 or b'' 0,26,Quiet version of `STSLICER`.
    STBRQ,null,#CF1F,cell_build,CF1F,STBRQ\nBCONCATQ,b b' - b b' -1 or b'' 0,26,Quiet version of `STBR`.
    STREFCONST,null,#CF20 c:^Cell,cell_build,CF20,[ref] STREFCONST,b - b’,26,Equivalent to `PUSHREF` `STREFR`.
    STREF2CONST,null,#CF21 c1:^Cell c2:^Cell,cell_build,CF21,[ref] [ref] STREF2CONST,b - b’,26,Equivalent to `STREFCONST` `STREFCONST`.
    ENDXC,null,#CF23,cell_build,CF23,null,b x - c,526,If `x!=0`, creates a _special_ or _exotic_ cell from _Builder_ `b`.\nThe type of the exotic cell must be stored in the first 8 bits of `b`.\nIf `x=0`, it is equivalent to `ENDC`. Otherwise some validity checks on the data and references of `b` are performed before creating the exotic cell.
    STILE4,null,#CF28,cell_build,CF28,STILE4,x b - b',26,Stores a little-endian signed 32-bit integer.
    STULE4,null,#CF29,cell_build,CF29,STULE4,x b - b',26,Stores a little-endian unsigned 32-bit integer.
    STILE8,null,#CF2A,cell_build,CF2A,STILE8,x b - b',26,Stores a little-endian signed 64-bit integer.
    STULE8,null,#CF2B,cell_build,CF2B,STULE8,x b - b',26,Stores a little-endian unsigned 64-bit integer.
    BDEPTH,null,#CF30,cell_build,CF30,BDEPTH,b - x,26,Returns the depth of _Builder_ `b`. If no cell references are stored in `b`, then `x=0`; otherwise `x` is one plus the maximum of depths of cells referred to from `b`.
    BBITS,null,#CF31,cell_build,CF31,BBITS,b - x,26,Returns the number of data bits already stored in _Builder_ `b`.
    BREFS,null,#CF32,cell_build,CF32,BREFS,b - y,26,Returns the number of cell references already stored in `b`.
    BBITREFS,null,#CF33,cell_build,CF33,BBITREFS,b - x y,26,Returns the numbers of both data bits and cell references in `b`.
    BREMBITS,null,#CF35,cell_build,CF35,BREMBITS,b - x',26,Returns the number of data bits that can still be stored in `b`.
    BREMREFS,null,#CF36,cell_build,CF36,BREMREFS,b - y',26,Returns the number of references that can still be stored in `b`.
    BREMBITREFS,null,#CF37,cell_build,CF37,BREMBITREFS,b - x' y',26,Returns the numbers of both data bits and references that can still be stored in `b`.
    BCHKBITS,null,#CF38 cc:uint8,cell_build,CF38cc,[cc+1] BCHKBITS#,b -,34/84,Checks whether `cc+1` bits can be stored into `b`, where `0 <= cc <= 255`.
    BCHKBITS_VAR,null,#CF39,cell_build,CF39,BCHKBITS,b x - ,26/76,Checks whether `x` bits can be stored into `b`, `0 <= x <= 1023`. If there is no space for `x` more bits in `b`, or if `x` is not within the range `0...1023`, throws an exception.
    BCHKREFS,null,#CF3A,cell_build,CF3A,BCHKREFS,b y - ,26/76,Checks whether `y` references can be stored into `b`, `0 <= y <= 7`.
    BCHKBITREFS,null,#CF3B,cell_build,CF3B,BCHKBITREFS,b x y - ,26/76,Checks whether `x` bits and `y` references can be stored into `b`, `0 <= x <= 1023`, `0 <= y <= 7`.
    BCHKBITSQ,null,#CF3C cc:uint8,cell_build,CF3Ccc,[cc+1] BCHKBITSQ#,b - ?,34,Checks whether `cc+1` bits can be stored into `b`, where `0 <= cc <= 255`.
    BCHKBITSQ_VAR,null,#CF3D,cell_build,CF3D,BCHKBITSQ,b x - ?,26,Checks whether `x` bits can be stored into `b`, `0 <= x <= 1023`.
    BCHKREFSQ,null,#CF3E,cell_build,CF3E,BCHKREFSQ,b y - ?,26,Checks whether `y` references can be stored into `b`, `0 <= y <= 7`.
    BCHKBITREFSQ,null,#CF3F,cell_build,CF3F,BCHKBITREFSQ,b x y - ?,26,Checks whether `x` bits and `y` references can be stored into `b`, `0 <= x <= 1023`, `0 <= y <= 7`.
    STZEROES,null,#CF40,cell_build,CF40,STZEROES,b n - b',26,Stores `n` binary zeroes into _Builder_ `b`.
    STONES,null,#CF41,cell_build,CF41,STONES,b n - b',26,Stores `n` binary ones into _Builder_ `b`.
    STSAME,null,#CF42,cell_build,CF42,STSAME,b n x - b',26,Stores `n` binary `x`es (`0 <= x <= 1`) into _Builder_ `b`.
    STSLICECONST,null,#CFC0_ x:(## 2) y:(## 3) c:(x * ^Cell) sss:((8 * y + 2) * Bit),cell_build,CFC0_xysss,[slice] STSLICECONST,b - b',24,Stores a constant subslice `sss`.\n_Details:_ `sss` consists of `0 <= x <= 3` references and up to `8y+2` data bits, with `0 <= y <= 7`. Completion bit is assumed.\nNote that the assembler can replace `STSLICECONST` with `PUSHSLICE` `STSLICER` if the slice is too big.
    STZERO,STSLICECONST,#CF81,cell_build,CF81,STZERO,b - b',24,Stores one binary zero.
    STONE,STSLICECONST,#CF83,cell_build,CF83,STONE,b - b',24,Stores one binary one.
    CTOS,null,#D0,cell_parse,D0,CTOS,c - s,118/43,Converts a _Cell_ into a _Slice_. Notice that `c` must be either an ordinary cell, or an exotic cell which is automatically _loaded_ to yield an ordinary cell `c'`, converted into a _Slice_ afterwards.
    ENDS,null,#D1,cell_parse,D1,ENDS,s - ,18/68,Removes a _Slice_ `s` from the stack, and throws an exception if it is not empty.
    LDI,null,#D2 cc:uint8,cell_parse,D2cc,[cc+1] LDI,s - x s',26,Loads (i.e., parses) a signed `cc+1`-bit integer `x` from _Slice_ `s`, and returns the remainder of `s` as `s'`.
    LDU,null,#D3 cc:uint8,cell_parse,D3cc,[cc+1] LDU,s - x s',26,Loads an unsigned `cc+1`-bit integer `x` from _Slice_ `s`.
    LDREF,null,#D4,cell_parse,D4,LDREF,s - c s',18,Loads a cell reference `c` from `s`.
    LDREFRTOS,null,#D5,cell_parse,D5,LDREFRTOS,s - s' s'',118/43,Equivalent to `LDREF` `SWAP` `CTOS`.
    LDSLICE,null,#D6 cc:uint8,cell_parse,D6cc,[cc+1] LDSLICE,s - s'' s',26,Cuts the next `cc+1` bits of `s` into a separate _Slice_ `s''`.
    LDIX,null,#D700,cell_parse,D700,LDIX,s l - x s',26,Loads a signed `l`-bit (`0 <= l <= 257`) integer `x` from _Slice_ `s`, and returns the remainder of `s` as `s'`.
    LDUX,null,#D701,cell_parse,D701,LDUX,s l - x s',26,Loads an unsigned `l`-bit integer `x` from (the first `l` bits of) `s`, with `0 <= l <= 256`.
    PLDIX,null,#D702,cell_parse,D702,PLDIX,s l - x,26,Preloads a signed `l`-bit integer from _Slice_ `s`, for `0 <= l <= 257`.
    PLDUX,null,#D703,cell_parse,D703,PLDUX,s l - x,26,Preloads an unsigned `l`-bit integer from `s`, for `0 <= l <= 256`.
    LDIXQ,null,#D704,cell_parse,D704,LDIXQ,s l - x s' -1 or s 0,26,Quiet version of `LDIX`: loads a signed `l`-bit integer from `s` similarly to `LDIX`, but returns a success flag, equal to `-1` on success or to `0` on failure (if `s` does not have `l` bits), instead of throwing a cell underflow exception.
    LDUXQ,null,#D705,cell_parse,D705,LDUXQ,s l - x s' -1 or s 0,26,Quiet version of `LDUX`.
    PLDIXQ,null,#D706,cell_parse,D706,PLDIXQ,s l - x -1 or 0,26,Quiet version of `PLDIX`.
    PLDUXQ,null,#D707,cell_parse,D707,PLDUXQ,s l - x -1 or 0,26,Quiet version of `PLDUX`.
    LDI_ALT,null,#D708 cc:uint8,cell_parse,D708cc,[cc+1] LDI_l,s - x s',34,A longer encoding for `LDI`.
    LDU_ALT,null,#D709 cc:uint8,cell_parse,D709cc,[cc+1] LDU_l,s - x s',34,A longer encoding for `LDU`.
    PLDI,null,#D70A cc:uint8,cell_parse,D70Acc,[cc+1] PLDI,s - x,34,Preloads a signed `cc+1`-bit integer from _Slice_ `s`.
    PLDU,null,#D70B cc:uint8,cell_parse,D70Bcc,[cc+1] PLDU,s - x,34,Preloads an unsigned `cc+1`-bit integer from `s`.
    LDIQ,null,#D70C cc:uint8,cell_parse,D70Ccc,[cc+1] LDIQ,s - x s' -1 or s 0,34,A quiet version of `LDI`.
    LDUQ,null,#D70D cc:uint8,cell_parse,D70Dcc,[cc+1] LDUQ,s - x s' -1 or s 0,34,A quiet version of `LDU`.
    PLDIQ,null,#D70E cc:uint8,cell_parse,D70Ecc,[cc+1] PLDIQ,s - x -1 or 0,34,A quiet version of `PLDI`.
    PLDUQ,null,#D70F cc:uint8,cell_parse,D70Fcc,[cc+1] PLDUQ,s - x -1 or 0,34,A quiet version of `PLDU`.
    PLDUZ,null,#D714_ c:uint3,cell_parse,D714_c,[32(c+1)] PLDUZ,s - s x,26,Preloads the first `32(c+1)` bits of _Slice_ `s` into an unsigned integer `x`, for `0 <= c <= 7`. If `s` is shorter than necessary, missing bits are assumed to be zero. This operation is intended to be used along with `IFBITJMP` and similar instructions.
    LDSLICEX,null,#D718,cell_parse,D718,LDSLICEX,s l - s'' s',26,Loads the first `0 <= l <= 1023` bits from _Slice_ `s` into a separate _Slice_ `s''`, returning the remainder of `s` as `s'`.
    PLDSLICEX,null,#D719,cell_parse,D719,PLDSLICEX,s l - s'',26,Returns the first `0 <= l <= 1023` bits of `s` as `s''`.
    LDSLICEXQ,null,#D71A,cell_parse,D71A,LDSLICEXQ,s l - s'' s' -1 or s 0,26,A quiet version of `LDSLICEX`.
    PLDSLICEXQ,null,#D71B,cell_parse,D71B,PLDSLICEXQ,s l - s' -1 or 0,26,A quiet version of `LDSLICEXQ`.
    LDSLICE_ALT,null,#D71C cc:uint8,cell_parse,D71Ccc,[cc+1] LDSLICE_l,s - s'' s',34,A longer encoding for `LDSLICE`.
    PLDSLICE,null,#D71D cc:uint8,cell_parse,D71Dcc,[cc+1] PLDSLICE,s - s'',34,Returns the first `0 < cc+1 <= 256` bits of `s` as `s''`.
    LDSLICEQ,null,#D71E cc:uint8,cell_parse,D71Ecc,[cc+1] LDSLICEQ,s - s'' s' -1 or s 0,34,A quiet version of `LDSLICE`.
    PLDSLICEQ,null,#D71F cc:uint8,cell_parse,D71Fcc,[cc+1] PLDSLICEQ,s - s'' -1 or 0,34,A quiet version of `PLDSLICE`.
    SDCUTFIRST,null,#D720,cell_parse,D720,SDCUTFIRST,s l - s',26,Returns the first `0 <= l <= 1023` bits of `s`. It is equivalent to `PLDSLICEX`.
    SDSKIPFIRST,null,#D721,cell_parse,D721,SDSKIPFIRST,s l - s',26,Returns all but the first `0 <= l <= 1023` bits of `s`. It is equivalent to `LDSLICEX` `NIP`.
    SDCUTLAST,null,#D722,cell_parse,D722,SDCUTLAST,s l - s',26,Returns the last `0 <= l <= 1023` bits of `s`.
    SDSKIPLAST,null,#D723,cell_parse,D723,SDSKIPLAST,s l - s',26,Returns all but the last `0 <= l <= 1023` bits of `s`.
    SDSUBSTR,null,#D724,cell_parse,D724,SDSUBSTR,s l l' - s',26,Returns `0 <= l' <= 1023` bits of `s` starting from offset `0 <= l <= 1023`, thus extracting a bit substring out of the data of `s`.
    SDBEGINSX,null,#D726,cell_parse,D726,SDBEGINSX,s s' - s'',26,Checks whether `s` begins with (the data bits of) `s'`, and removes `s'` from `s` on success. On failure throws a cell deserialization exception. Primitive `SDPFXREV` can be considered a quiet version of `SDBEGINSX`.
    SDBEGINSXQ,null,#D727,cell_parse,D727,SDBEGINSXQ,s s' - s'' -1 or s 0,26,A quiet version of `SDBEGINSX`.
    SDBEGINS,null,#D72A_ x:(## 7) sss:((8 * x + 3) * Bit),cell_parse,D72A_xsss,[slice] SDBEGINS,s - s'',31,Checks whether `s` begins with constant bitstring `sss` of length `8x+3` (with continuation bit assumed), where `0 <= x <= 127`, and removes `sss` from `s` on success.
    SDBEGINSQ,null,#D72E_ x:(## 7) sss:((8 * x + 3) * Bit),cell_parse,D72E_xsss,[slice] SDBEGINSQ,s - s'' -1 or s 0,31,A quiet version of `SDBEGINS`.
    SCUTFIRST,null,#D730,cell_parse,D730,SCUTFIRST,s l r - s',26,Returns the first `0 <= l <= 1023` bits and first `0 <= r <= 4` references of `s`.
    SSKIPFIRST,null,#D731,cell_parse,D731,SSKIPFIRST,s l r - s',26,Returns all but the first `l` bits of `s` and `r` references of `s`.
    SCUTLAST,null,#D732,cell_parse,D732,SCUTLAST,s l r - s',26,Returns the last `0 <= l <= 1023` data bits and last `0 <= r <= 4` references of `s`.
    SSKIPLAST,null,#D733,cell_parse,D733,SSKIPLAST,s l r - s',26,Returns all but the last `l` bits of `s` and `r` references of `s`.
    SUBSLICE,null,#D734,cell_parse,D734,SUBSLICE,s l r l' r' - s',26,Returns `0 <= l' <= 1023` bits and `0 <= r' <= 4` references from _Slice_ `s`, after skipping the first `0 <= l <= 1023` bits and first `0 <= r <= 4` references.
    SPLIT,null,#D736,cell_parse,D736,SPLIT,s l r - s' s'',26,Splits the first `0 <= l <= 1023` data bits and first `0 <= r <= 4` references from `s` into `s'`, returning the remainder of `s` as `s''`.
    SPLITQ,null,#D737,cell_parse,D737,SPLITQ,s l r - s' s'' -1 or s 0,26,A quiet version of `SPLIT`.
    XCTOS,null,#D739,cell_parse,D739,null,c - s ?,null,Transforms an ordinary or exotic cell into a _Slice_, as if it were an ordinary cell. A flag is returned indicating whether `c` is exotic. If that be the case, its type can later be deserialized from the first eight bits of `s`.
    XLOAD,null,#D73A,cell_parse,D73A,null,c - c',null,Loads an exotic cell `c` and returns an ordinary cell `c'`. If `c` is already ordinary, does nothing. If `c` cannot be loaded, throws an exception.
    XLOADQ,null,#D73B,cell_parse,D73B,null,c - c' -1 or c 0,null,Loads an exotic cell `c` and returns an ordinary cell `c'`. If `c` is already ordinary, does nothing. If `c` cannot be loaded, returns 0.
    SCHKBITS,null,#D741,cell_parse,D741,SCHKBITS,s l - ,26/76,Checks whether there are at least `l` data bits in _Slice_ `s`. If this is not the case, throws a cell deserialisation (i.e., cell underflow) exception.
    SCHKREFS,null,#D742,cell_parse,D742,SCHKREFS,s r - ,26/76,Checks whether there are at least `r` references in _Slice_ `s`.
    SCHKBITREFS,null,#D743,cell_parse,D743,SCHKBITREFS,s l r - ,26/76,Checks whether there are at least `l` data bits and `r` references in _Slice_ `s`.
    SCHKBITSQ,null,#D745,cell_parse,D745,SCHKBITSQ,s l - ?,26,Checks whether there are at least `l` data bits in _Slice_ `s`.
    SCHKREFSQ,null,#D746,cell_parse,D746,SCHKREFSQ,s r - ?,26,Checks whether there are at least `r` references in _Slice_ `s`.
    SCHKBITREFSQ,null,#D747,cell_parse,D747,SCHKBITREFSQ,s l r - ?,26,Checks whether there are at least `l` data bits and `r` references in _Slice_ `s`.
    PLDREFVAR,null,#D748,cell_parse,D748,PLDREFVAR,s n - c,26,Returns the `n`-th cell reference of _Slice_ `s` for `0 <= n <= 3`.
    SBITS,null,#D749,cell_parse,D749,SBITS,s - l,26,Returns the number of data bits in _Slice_ `s`.
    SREFS,null,#D74A,cell_parse,D74A,SREFS,s - r,26,Returns the number of references in _Slice_ `s`.
    SBITREFS,null,#D74B,cell_parse,D74B,SBITREFS,s - l r,26,Returns both the number of data bits and the number of references in `s`.
    PLDREFIDX,null,#D74E_ n:uint2,cell_parse,D74E_n,[n] PLDREFIDX,s - c,26,Returns the `n`-th cell reference of _Slice_ `s`, where `0 <= n <= 3`.
    PLDREF,PLDREFIDX,#D74C,cell_parse,D74C,PLDREF,s - c,26,Preloads the first cell reference of a _Slice_.
    LDILE4,null,#D750,cell_parse,D750,LDILE4,s - x s',26,Loads a little-endian signed 32-bit integer.
    LDULE4,null,#D751,cell_parse,D751,LDULE4,s - x s',26,Loads a little-endian unsigned 32-bit integer.
    LDILE8,null,#D752,cell_parse,D752,LDILE8,s - x s',26,Loads a little-endian signed 64-bit integer.
    LDULE8,null,#D753,cell_parse,D753,LDULE8,s - x s',26,Loads a little-endian unsigned 64-bit integer.
    PLDILE4,null,#D754,cell_parse,D754,PLDILE4,s - x,26,Preloads a little-endian signed 32-bit integer.
    PLDULE4,null,#D755,cell_parse,D755,PLDULE4,s - x,26,Preloads a little-endian unsigned 32-bit integer.
    PLDILE8,null,#D756,cell_parse,D756,PLDILE8,s - x,26,Preloads a little-endian signed 64-bit integer.
    PLDULE8,null,#D757,cell_parse,D757,PLDULE8,s - x,26,Preloads a little-endian unsigned 64-bit integer.
    LDILE4Q,null,#D758,cell_parse,D758,LDILE4Q,s - x s' -1 or s 0,26,Quietly loads a little-endian signed 32-bit integer.
    LDULE4Q,null,#D759,cell_parse,D759,LDULE4Q,s - x s' -1 or s 0,26,Quietly loads a little-endian unsigned 32-bit integer.
    LDILE8Q,null,#D75A,cell_parse,D75A,LDILE8Q,s - x s' -1 or s 0,26,Quietly loads a little-endian signed 64-bit integer.
    LDULE8Q,null,#D75B,cell_parse,D75B,LDULE8Q,s - x s' -1 or s 0,26,Quietly loads a little-endian unsigned 64-bit integer.
    PLDILE4Q,null,#D75C,cell_parse,D75C,PLDILE4Q,s - x -1 or 0,26,Quietly preloads a little-endian signed 32-bit integer.
    PLDULE4Q,null,#D75D,cell_parse,D75D,PLDULE4Q,s - x -1 or 0,26,Quietly preloads a little-endian unsigned 32-bit integer.
    PLDILE8Q,null,#D75E,cell_parse,D75E,PLDILE8Q,s - x -1 or 0,26,Quietly preloads a little-endian signed 64-bit integer.
    PLDULE8Q,null,#D75F,cell_parse,D75F,PLDULE8Q,s - x -1 or 0,26,Quietly preloads a little-endian unsigned 64-bit integer.
    LDZEROES,null,#D760,cell_parse,D760,LDZEROES,s - n s',26,Returns the count `n` of leading zero bits in `s`, and removes these bits from `s`.
    LDONES,null,#D761,cell_parse,D761,LDONES,s - n s',26,Returns the count `n` of leading one bits in `s`, and removes these bits from `s`.
    LDSAME,null,#D762,cell_parse,D762,LDSAME,s x - n s',26,Returns the count `n` of leading bits equal to `0 <= x <= 1` in `s`, and removes these bits from `s`.
    SDEPTH,null,#D764,cell_parse,D764,SDEPTH,s - x,26,Returns the depth of _Slice_ `s`. If `s` has no references, then `x=0`; otherwise `x` is one plus the maximum of depths of cells referred to from `s`.
    CDEPTH,null,#D765,cell_parse,D765,CDEPTH,c - x,26,Returns the depth of _Cell_ `c`. If `c` has no references, then `x=0`; otherwise `x` is one plus the maximum of depths of cells referred to from `c`. If `c` is a _Null_ instead of a _Cell_, returns zero.
    EXECUTE,null,#D8,cont_basic,D8,EXECUTE\nCALLX,c - ,18,_Calls_, or _executes_, continuation `c`.
    JMPX,null,#D9,cont_basic,D9,JMPX,c - ,18,_Jumps_, or transfers control, to continuation `c`.\nThe remainder of the previous current continuation `cc` is discarded.
    CALLXARGS,null,#DA p:uint4 r:uint4,cont_basic,DApr,[p] [r] CALLXARGS,c - ,26,_Calls_ continuation `c` with `p` parameters and expecting `r` return values\n`0 <= p <= 15`, `0 <= r <= 15`
    CALLXARGS_VAR,null,#DB0 p:uint4,cont_basic,DB0p,[p] -1 CALLXARGS,c - ,26,_Calls_ continuation `c` with `0 <= p <= 15` parameters, expecting an arbitrary number of return values.
    JMPXARGS,null,#DB1 p:uint4,cont_basic,DB1p,[p] JMPXARGS,c - ,26,_Jumps_ to continuation `c`, passing only the top `0 <= p <= 15` values from the current stack to it (the remainder of the current stack is discarded).
    RETARGS,null,#DB2 r:uint4,cont_basic,DB2r,[r] RETARGS,null,26,_Returns_ to `c0`, with `0 <= r <= 15` return values taken from the current stack.
    RET,null,#DB30,cont_basic,DB30,RET\nRETTRUE,null,26,_Returns_ to the continuation at `c0`. The remainder of the current continuation `cc` is discarded.\nApproximately equivalent to `c0 PUSHCTR` `JMPX`.
    RETALT,null,#DB31,cont_basic,DB31,RETALT\nRETFALSE,null,26,_Returns_ to the continuation at `c1`.\nApproximately equivalent to `c1 PUSHCTR` `JMPX`.
    BRANCH,null,#DB32,cont_basic,DB32,BRANCH\nRETBOOL,f - ,26,Performs `RETTRUE` if integer `f!=0`, or `RETFALSE` if `f=0`.
    CALLCC,null,#DB34,cont_basic,DB34,CALLCC,c - ,26,_Call with current continuation_, transfers control to `c`, pushing the old value of `cc` into `c`'s stack (instead of discarding it or writing it into new `c0`).
    JMPXDATA,null,#DB35,cont_basic,DB35,JMPXDATA,c - ,26,Similar to `CALLCC`, but the remainder of the current continuation (the old value of `cc`) is converted into a _Slice_ before pushing it into the stack of `c`.
    CALLCCARGS,null,#DB36 p:uint4 r:uint4,cont_basic,DB36pr,[p] [r] CALLCCARGS,c - ,34,Similar to `CALLXARGS`, but pushes the old value of `cc` (along with the top `0 <= p <= 15` values from the original stack) into the stack of newly-invoked continuation `c`, setting `cc.nargs` to `-1 <= r <= 14`.
    CALLXVARARGS,null,#DB38,cont_basic,DB38,CALLXVARARGS,c p r - ,26,Similar to `CALLXARGS`, but takes `-1 <= p,r <= 254` from the stack. The next three operations also take `p` and `r` from the stack, both in the range `-1...254`.
    RETVARARGS,null,#DB39,cont_basic,DB39,RETVARARGS,p r - ,26,Similar to `RETARGS`.
    JMPXVARARGS,null,#DB3A,cont_basic,DB3A,JMPXVARARGS,c p r - ,26,Similar to `JMPXARGS`.
    CALLCCVARARGS,null,#DB3B,cont_basic,DB3B,CALLCCVARARGS,c p r - ,26,Similar to `CALLCCARGS`.
    CALLREF,null,#DB3C c:^Cell,cont_basic,DB3C,[ref] CALLREF,null,126/51,Equivalent to `PUSHREFCONT` `CALLX`.
    JMPREF,null,#DB3D c:^Cell,cont_basic,DB3D,[ref] JMPREF,null,126/51,Equivalent to `PUSHREFCONT` `JMPX`.
    JMPREFDATA,null,#DB3E c:^Cell,cont_basic,DB3E,[ref] JMPREFDATA,null,126/51,Equivalent to `PUSHREFCONT` `JMPXDATA`.
    RETDATA,null,#DB3F,cont_basic,DB3F,RETDATA,null,26,Equivalent to `c0 PUSHCTR` `JMPXDATA`. In this way, the remainder of the current continuation is converted into a _Slice_ and returned to the caller.
    IFRET,null,#DC,cont_conditional,DC,IFRET\nIFNOT:,f - ,18,Performs a `RET`, but only if integer `f` is non-zero. If `f` is a `NaN`, throws an integer overflow exception.
    IFNOTRET,null,#DD,cont_conditional,DD,IFNOTRET\nIF:,f - ,18,Performs a `RET`, but only if integer `f` is zero.
    IF,null,#DE,cont_conditional,DE,IF,f c - ,18,Performs `EXECUTE` for `c` (i.e., _executes_ `c`), but only if integer `f` is non-zero. Otherwise simply discards both values.
    null,null,null,cont_conditional,DE,IF:<{ code }>\n<{ code }>IF,f -,null,Equivalent to `<{ code }> CONT` `IF`.
    IFNOT,null,#DF,cont_conditional,DF,IFNOT,f c - ,18,Executes continuation `c`, but only if integer `f` is zero. Otherwise simply discards both values.
    null,null,null,cont_conditional,DF,IFNOT:<{ code }>\n<{ code }>IFNOT,f -,null,Equivalent to `<{ code }> CONT` `IFNOT`.
    IFJMP,null,#E0,cont_conditional,E0,IFJMP,f c - ,18,Jumps to `c` (similarly to `JMPX`), but only if `f` is non-zero.
    null,null,null,cont_conditional,E0,IFJMP:<{ code }>,f -,null,Equivalent to `<{ code }> CONT` `IFJMP`.
    IFNOTJMP,null,#E1,cont_conditional,E1,IFNOTJMP,f c - ,18,Jumps to `c` (similarly to `JMPX`), but only if `f` is zero.
    null,null,null,cont_conditional,E1,IFNOTJMP:<{ code }>,f -,null,Equivalent to `<{ code }> CONT` `IFNOTJMP`.
    IFELSE,null,#E2,cont_conditional,E2,IFELSE,f c c' - ,18,If integer `f` is non-zero, executes `c`, otherwise executes `c'`. Equivalent to `CONDSELCHK` `EXECUTE`.
    null,null,null,cont_conditional,E2,IF:<{ code1 }>ELSE<{ code2 }>,f -,null,Equivalent to `<{ code1 }> CONT` `<{ code2 }> CONT` `IFELSE`.
    IFREF,null,#E300 c:^Cell,cont_conditional,E300,[ref] IFREF,f - ,26/126/51,Equivalent to `PUSHREFCONT` `IF`, with the optimization that the cell reference is not actually loaded into a _Slice_ and then converted into an ordinary _Continuation_ if `f=0`.\nGas consumption of this primitive depends on whether `f=0` and whether the reference was loaded before.\nSimilar remarks apply other primitives that accept a continuation as a reference.
    IFNOTREF,null,#E301 c:^Cell,cont_conditional,E301,[ref] IFNOTREF,f - ,26/126/51,Equivalent to `PUSHREFCONT` `IFNOT`.
    IFJMPREF,null,#E302 c:^Cell,cont_conditional,E302,[ref] IFJMPREF,f - ,26/126/51,Equivalent to `PUSHREFCONT` `IFJMP`.
    IFNOTJMPREF,null,#E303 c:^Cell,cont_conditional,E303,[ref] IFNOTJMPREF,f - ,26/126/51,Equivalent to `PUSHREFCONT` `IFNOTJMP`.
    CONDSEL,null,#E304,cont_conditional,E304,CONDSEL,f x y - x or y,26,If integer `f` is non-zero, returns `x`, otherwise returns `y`. Notice that no type checks are performed on `x` and `y`; as such, it is more like a conditional stack operation. Roughly equivalent to `ROT` `ISZERO` `INC` `ROLLX` `NIP`.
    CONDSELCHK,null,#E305,cont_conditional,E305,CONDSELCHK,f x y - x or y,26,Same as `CONDSEL`, but first checks whether `x` and `y` have the same type.
    IFRETALT,null,#E308,cont_conditional,E308,IFRETALT,f -,26,Performs `RETALT` if integer `f!=0`.
    IFNOTRETALT,null,#E309,cont_conditional,E309,IFNOTRETALT,f -,26,Performs `RETALT` if integer `f=0`.
    IFREFELSE,null,#E30D c:^Cell,cont_conditional,E30D,[ref] IFREFELSE,f c -,26/126/51,Equivalent to `PUSHREFCONT` `SWAP` `IFELSE`, with the optimization that the cell reference is not actually loaded into a _Slice_ and then converted into an ordinary _Continuation_ if `f=0`. Similar remarks apply to the next two primitives: cells are converted into continuations only when necessary.
    IFELSEREF,null,#E30E c:^Cell,cont_conditional,E30E,[ref] IFELSEREF,f c -,26/126/51,Equivalent to `PUSHREFCONT` `IFELSE`.
    IFREFELSEREF,null,#E30F c1:^Cell c2:^Cell,cont_conditional,E30F,[ref] [ref] IFREFELSEREF,f -,126/51,Equivalent to `PUSHREFCONT` `PUSHREFCONT` `IFELSE`.
    IFBITJMP,null,#E39_ n:uint5,cont_conditional,E39_n,[n] IFBITJMP,x c - x,26,Checks whether bit `0 <= n <= 31` is set in integer `x`, and if so, performs `JMPX` to continuation `c`. Value `x` is left in the stack.
    IFNBITJMP,null,#E3B_ n:uint5,cont_conditional,E3B_n,[n] IFNBITJMP,x c - x,26,Jumps to `c` if bit `0 <= n <= 31` is not set in integer `x`.
    IFBITJMPREF,null,#E3D_ n:uint5 c:^Cell,cont_conditional,E3D_n,[ref] [n] IFBITJMPREF,x - x,126/51,Performs a `JMPREF` if bit `0 <= n <= 31` is set in integer `x`.
    IFNBITJMPREF,null,#E3F_ n:uint5 c:^Cell,cont_conditional,E3F_n,[ref] [n] IFNBITJMPREF,x - x,126/51,Performs a `JMPREF` if bit `0 <= n <= 31` is not set in integer `x`.
    REPEAT,null,#E4,cont_loops,E4,REPEAT,n c - ,18,Executes continuation `c` `n` times, if integer `n` is non-negative. If `n>=2^31` or `n<-2^31`, generates a range check exception.\nNotice that a `RET` inside the code of `c` works as a `continue`, not as a `break`. One should use either alternative (experimental) loops or alternative `RETALT` (along with a `SETEXITALT` before the loop) to `break` out of a loop.
    null,null,null,cont_loops,E4,REPEAT:<{ code }>\n<{ code }>REPEAT,n -,null,Equivalent to `<{ code }> CONT` `REPEAT`.
    REPEATEND,null,#E5,cont_loops,E5,REPEATEND\nREPEAT:,n - ,18,Similar to `REPEAT`, but it is applied to the current continuation `cc`.
    UNTIL,null,#E6,cont_loops,E6,UNTIL,c - ,18,Executes continuation `c`, then pops an integer `x` from the resulting stack. If `x` is zero, performs another iteration of this loop. The actual implementation of this primitive involves an extraordinary continuation `ec_until` with its arguments set to the body of the loop (continuation `c`) and the original current continuation `cc`. This extraordinary continuation is then saved into the savelist of `c` as `c.c0` and the modified `c` is then executed. The other loop primitives are implemented similarly with the aid of suitable extraordinary continuations.
    null,null,null,cont_loops,E6,UNTIL:<{ code }>\n<{ code }>UNTIL,-,null,Equivalent to `<{ code }> CONT` `UNTIL`.
    UNTILEND,null,#E7,cont_loops,E7,UNTILEND\nUNTIL:,-,18,Similar to `UNTIL`, but executes the current continuation `cc` in a loop. When the loop exit condition is satisfied, performs a `RET`.
    WHILE,null,#E8,cont_loops,E8,WHILE,c' c - ,18,Executes `c'` and pops an integer `x` from the resulting stack. If `x` is zero, exists the loop and transfers control to the original `cc`. If `x` is non-zero, executes `c`, and then begins a new iteration.
    null,null,null,cont_loops,E8,WHILE:<{ cond }>DO<{ code }>,-,null,Equivalent to `<{ cond }> CONT` `<{ code }> CONT` `WHILE`.
    WHILEEND,null,#E9,cont_loops,E9,WHILEEND,c' - ,18,Similar to `WHILE`, but uses the current continuation `cc` as the loop body.
    AGAIN,null,#EA,cont_loops,EA,AGAIN,c - ,18,Similar to `REPEAT`, but executes `c` infinitely many times. A `RET` only begins a new iteration of the infinite loop, which can be exited only by an exception, or a `RETALT` (or an explicit `JMPX`).
    null,null,null,cont_loops,EA,AGAIN:<{ code }>\n<{ code }>AGAIN,-,null,Equivalent to `<{ code }> CONT` `AGAIN`.
    AGAINEND,null,#EB,cont_loops,EB,AGAINEND\nAGAIN:,-,18,Similar to `AGAIN`, but performed with respect to the current continuation `cc`.
    REPEATBRK,null,#E314,cont_loops,E314,REPEATBRK,n c -,26,Similar to `REPEAT`, but also sets `c1` to the original `cc` after saving the old value of `c1` into the savelist of the original `cc`. In this way `RETALT` could be used to break out of the loop body.
    null,null,null,cont_loops,E314,REPEATBRK:<{ code }>\n<{ code }>REPEATBRK,n -,null,Equivalent to `<{ code }> CONT` `REPEATBRK`.
    REPEATENDBRK,null,#E315,cont_loops,E315,REPEATENDBRK,n -,26,Similar to `REPEATEND`, but also sets `c1` to the original `c0` after saving the old value of `c1` into the savelist of the original `c0`. Equivalent to `SAMEALTSAVE` `REPEATEND`.
    UNTILBRK,null,#E316,cont_loops,E316,UNTILBRK,c -,26,Similar to `UNTIL`, but also modifies `c1` in the same way as `REPEATBRK`.
    null,null,null,cont_loops,E316,UNTILBRK:<{ code }>,-,null,Equivalent to `<{ code }> CONT` `UNTILBRK`.
    UNTILENDBRK,null,#E317,cont_loops,E317,UNTILENDBRK\nUNTILBRK:,-,26,Equivalent to `SAMEALTSAVE` `UNTILEND`.
    WHILEBRK,null,#E318,cont_loops,E318,WHILEBRK,c' c -,26,Similar to `WHILE`, but also modifies `c1` in the same way as `REPEATBRK`.
    null,null,null,cont_loops,E318,WHILEBRK:<{ cond }>DO<{ code }>,-,null,Equivalent to `<{ cond }> CONT` `<{ code }> CONT` `WHILEBRK`.
    WHILEENDBRK,null,#E319,cont_loops,E319,WHILEENDBRK,c -,26,Equivalent to `SAMEALTSAVE` `WHILEEND`.
    AGAINBRK,null,#E31A,cont_loops,E31A,AGAINBRK,c -,26,Similar to `AGAIN`, but also modifies `c1` in the same way as `REPEATBRK`.
    null,null,null,cont_loops,E31A,AGAINBRK:<{ code }>,-,null,Equivalent to `<{ code }> CONT` `AGAINBRK`.
    AGAINENDBRK,null,#E31B,cont_loops,E31B,AGAINENDBRK\nAGAINBRK:,-,26,Equivalent to `SAMEALTSAVE` `AGAINEND`.
    SETCONTARGS_N,null,#EC r:uint4 n:(#<= 14),cont_stack,ECrn,[r] [n] SETCONTARGS,x_1 x_2...x_r c - c',26+s,Similar to `[r] -1 SETCONTARGS`, but sets `c.nargs` to the final size of the stack of `c'` plus `n`. In other words, transforms `c` into a _closure_ or a _partially applied function_, with `0 <= n <= 14` arguments missing.
    SETNUMARGS,SETCONTARGS_N,#EC0 n:(#<= 14),cont_stack,EC0n,[n] SETNUMARGS,c - c',26,Sets `c.nargs` to `n` plus the current depth of `c`'s stack, where `0 <= n <= 14`. If `c.nargs` is already set to a non-negative value, does nothing.
    SETCONTARGS,null,#EC r:uint4 n:(## 4) {n = 15},cont_stack,ECrF,[r] -1 SETCONTARGS,x_1 x_2...x_r c - c',26+s,Pushes `0 <= r <= 15` values `x_1...x_r` into the stack of (a copy of) the continuation `c`, starting with `x_1`. If the final depth of `c`'s stack turns out to be greater than `c.nargs`, a stack overflow exception is generated.
    RETURNARGS,null,#ED0 p:uint4,cont_stack,ED0p,[p] RETURNARGS,-,26+s,Leaves only the top `0 <= p <= 15` values in the current stack (somewhat similarly to `ONLYTOPX`), with all the unused bottom values not discarded, but saved into continuation `c0` in the same way as `SETCONTARGS` does.
    RETURNVARARGS,null,#ED10,cont_stack,ED10,RETURNVARARGS,p -,26+s,Similar to `RETURNARGS`, but with Integer `0 <= p <= 255` taken from the stack.
    SETCONTVARARGS,null,#ED11,cont_stack,ED11,SETCONTVARARGS,x_1 x_2...x_r c r n - c',26+s,Similar to `SETCONTARGS`, but with `0 <= r <= 255` and `-1 <= n <= 255` taken from the stack.
    SETNUMVARARGS,null,#ED12,cont_stack,ED12,SETNUMVARARGS,c n - c',26,`-1 <= n <= 255`\nIf `n=-1`, this operation does nothing (`c'=c`).\nOtherwise its action is similar to `[n] SETNUMARGS`, but with `n` taken from the stack.
    BLESS,null,#ED1E,cont_create,ED1E,BLESS,s - c,26,Transforms a _Slice_ `s` into a simple ordinary continuation `c`, with `c.code=s` and an empty stack and savelist.
    BLESSVARARGS,null,#ED1F,cont_create,ED1F,BLESSVARARGS,x_1...x_r s r n - c,26+s,Equivalent to `ROT` `BLESS` `ROTREV` `SETCONTVARARGS`.
    BLESSARGS,null,#EE r:uint4 n:uint4,cont_create,EErn,[r] [n] BLESSARGS,x_1...x_r s - c,26,`0 <= r <= 15`, `-1 <= n <= 14`\nEquivalent to `BLESS` `[r] [n] SETCONTARGS`.\nThe value of `n` is represented inside the instruction by the 4-bit integer `n mod 16`.
    BLESSNUMARGS,BLESSARGS,#EE0 n:uint4,cont_create,EE0n,[n] BLESSNUMARGS,s - c,26,Also transforms a _Slice_ `s` into a _Continuation_ `c`, but sets `c.nargs` to `0 <= n <= 14`.
    PUSHCTR,null,#ED4 i:uint4,cont_registers,ED4i,c[i] PUSHCTR\nc[i] PUSH,- x,26,Pushes the current value of control register `c(i)`. If the control register is not supported in the current codepage, or if it does not have a value, an exception is triggered.
    PUSHROOT,PUSHCTR,#ED44,cont_registers,ED44,c4 PUSHCTR\nc4 PUSH,- x,26,Pushes the “global data root'' cell reference, thus enabling access to persistent smart-contract data.
    POPCTR,null,#ED5 i:uint4,cont_registers,ED5i,c[i] POPCTR\nc[i] POP,x - ,26,Pops a value `x` from the stack and stores it into control register `c(i)`, if supported in the current codepage. Notice that if a control register accepts only values of a specific type, a type-checking exception may occur.
    POPROOT,POPCTR,#ED54,cont_registers,ED54,c4 POPCTR\nc4 POP,x -,26,Sets the “global data root'' cell reference, thus allowing modification of persistent smart-contract data.
    SETCONTCTR,null,#ED6 i:uint4,cont_registers,ED6i,c[i] SETCONT\nc[i] SETCONTCTR,x c - c',26,Stores `x` into the savelist of continuation `c` as `c(i)`, and returns the resulting continuation `c'`. Almost all operations with continuations may be expressed in terms of `SETCONTCTR`, `POPCTR`, and `PUSHCTR`.
    SETRETCTR,null,#ED7 i:uint4,cont_registers,ED7i,c[i] SETRETCTR,x - ,26,Equivalent to `c0 PUSHCTR` `c[i] SETCONTCTR` `c0 POPCTR`.
    SETALTCTR,null,#ED8 i:uint4,cont_registers,ED8i,c[i] SETALTCTR,x - ,26,Equivalent to `c1 PUSHCTR` `c[i] SETCONTCTR` `c1 POPCTR`.
    POPSAVE,null,#ED9 i:uint4,cont_registers,ED9i,c[i] POPSAVE\nc[i] POPCTRSAVE,x -,26,Similar to `c[i] POPCTR`, but also saves the old value of `c[i]` into continuation `c0`.\nEquivalent (up to exceptions) to `c[i] SAVECTR` `c[i] POPCTR`.
    SAVE,null,#EDA i:uint4,cont_registers,EDAi,c[i] SAVE\nc[i] SAVECTR,null,26,Saves the current value of `c(i)` into the savelist of continuation `c0`. If an entry for `c[i]` is already present in the savelist of `c0`, nothing is done. Equivalent to `c[i] PUSHCTR` `c[i] SETRETCTR`.
    SAVEALT,null,#EDB i:uint4,cont_registers,EDBi,c[i] SAVEALT\nc[i] SAVEALTCTR,null,26,Similar to `c[i] SAVE`, but saves the current value of `c[i]` into the savelist of `c1`, not `c0`.
    SAVEBOTH,null,#EDC i:uint4,cont_registers,EDCi,c[i] SAVEBOTH\nc[i] SAVEBOTHCTR,null,26,Equivalent to `c[i] SAVE` `c[i] SAVEALT`.
    PUSHCTRX,null,#EDE0,cont_registers,EDE0,PUSHCTRX,i - x,26,Similar to `c[i] PUSHCTR`, but with `i`, `0 <= i <= 255`, taken from the stack.\nNotice that this primitive is one of the few “exotic'' primitives, which are not polymorphic like stack manipulation primitives, and at the same time do not have well-defined types of parameters and return values, because the type of `x` depends on `i`.
    POPCTRX,null,#EDE1,cont_registers,EDE1,POPCTRX,x i - ,26,Similar to `c[i] POPCTR`, but with `0 <= i <= 255` from the stack.
    SETCONTCTRX,null,#EDE2,cont_registers,EDE2,SETCONTCTRX,x c i - c',26,Similar to `c[i] SETCONTCTR`, but with `0 <= i <= 255` from the stack.
    COMPOS,null,#EDF0,cont_registers,EDF0,COMPOS\nBOOLAND,c c' - c'',26,Computes the composition `compose0(c, c’)`, which has the meaning of “perform `c`, and, if successful, perform `c'`'' (if `c` is a boolean circuit) or simply “perform `c`, then `c'`''. Equivalent to `SWAP` `c0 SETCONT`.
    COMPOSALT,null,#EDF1,cont_registers,EDF1,COMPOSALT\nBOOLOR,c c' - c'',26,Computes the alternative composition `compose1(c, c’)`, which has the meaning of “perform `c`, and, if not successful, perform `c'`'' (if `c` is a boolean circuit). Equivalent to `SWAP` `c1 SETCONT`.
    COMPOSBOTH,null,#EDF2,cont_registers,EDF2,COMPOSBOTH,c c' - c'',26,Computes composition `compose1(compose0(c, c’), c’)`, which has the meaning of “compute boolean circuit `c`, then compute `c'`, regardless of the result of `c`''.
    ATEXIT,null,#EDF3,cont_registers,EDF3,ATEXIT,c - ,26,Sets `c0` to `compose0(c, c0)`. In other words, `c` will be executed before exiting current subroutine.
    null,null,null,cont_registers,EDF3,ATEXIT:<{ code }>\n<{ code }>ATEXIT,-,null,Equivalent to `<{ code }> CONT` `ATEXIT`.
    ATEXITALT,null,#EDF4,cont_registers,EDF4,ATEXITALT,c - ,26,Sets `c1` to `compose1(c, c1)`. In other words, `c` will be executed before exiting current subroutine by its alternative return path.
    null,null,null,cont_registers,EDF4,ATEXITALT:<{ code }>\n<{ code }>ATEXITALT,-,null,Equivalent to `<{ code }> CONT` `ATEXITALT`.
    SETEXITALT,null,#EDF5,cont_registers,EDF5,SETEXITALT,c - ,26,Sets `c1` to `compose1(compose0(c, c0), c1)`,\nIn this way, a subsequent `RETALT` will first execute `c`, then transfer control to the original `c0`. This can be used, for instance, to exit from nested loops.
    THENRET,null,#EDF6,cont_registers,EDF6,THENRET,c - c',26,Computes `compose0(c, c0)`.
    THENRETALT,null,#EDF7,cont_registers,EDF7,THENRETALT,c - c',26,Computes `compose0(c, c1)`
    INVERT,null,#EDF8,cont_registers,EDF8,INVERT,-,26,Interchanges `c0` and `c1`.
    BOOLEVAL,null,#EDF9,cont_registers,EDF9,BOOLEVAL,c - ?,26,Performs `cc:=compose1(compose0(c, compose0(-1 PUSHINT, cc)), compose0(0 PUSHINT, cc))`. If `c` represents a boolean circuit, the net effect is to evaluate it and push either `-1` or `0` into the stack before continuing.
    SAMEALT,null,#EDFA,cont_registers,EDFA,SAMEALT,-,26,Sets `c1` to `c0`. Equivalent to `c0 PUSHCTR` `c1 POPCTR`.
    SAMEALTSAVE,null,#EDFB,cont_registers,EDFB,SAMEALTSAVE,-,26,Sets `c1` to `c0`, but first saves the old value of `c1` into the savelist of `c0`.\nEquivalent to `c1 SAVE` `SAMEALT`.
    CALLDICT,null,#F0 n:uint8,cont_dict,F0nn,[nn] CALL\n[nn] CALLDICT,- nn,null,Calls the continuation in `c3`, pushing integer `0 <= nn <= 255` into its stack as an argument.\nApproximately equivalent to `[nn] PUSHINT` `c3 PUSHCTR` `EXECUTE`.
    CALLDICT_LONG,null,#F12_ n:uint14,cont_dict,F12_n,[n] CALL\n[n] CALLDICT,- n,null,For `0 <= n < 2^14`, an encoding of `[n] CALL` for larger values of `n`.
    JMPDICT,null,#F16_ n:uint14,cont_dict,F16_n,[n] JMP, - n,null,Jumps to the continuation in `c3`, pushing integer `0 <= n < 2^14` as its argument.\nApproximately equivalent to `n PUSHINT` `c3 PUSHCTR` `JMPX`.
    PREPAREDICT,null,#F1A_ n:uint14,cont_dict,F1A_n,[n] PREPARE\n[n] PREPAREDICT, - n c,null,Equivalent to `n PUSHINT` `c3 PUSHCTR`, for `0 <= n < 2^14`.\nIn this way, `[n] CALL` is approximately equivalent to `[n] PREPARE` `EXECUTE`, and `[n] JMP` is approximately equivalent to `[n] PREPARE` `JMPX`.\nOne might use, for instance, `CALLXARGS` or `CALLCC` instead of `EXECUTE` here.
    THROW_SHORT,null,#F22_ n:uint6,exceptions,F22_n,[n] THROW, - 0 n,76,Throws exception `0 <= n <= 63` with parameter zero.\nIn other words, it transfers control to the continuation in `c2`, pushing `0` and `n` into its stack, and discarding the old stack altogether.
    THROWIF_SHORT,null,#F26_ n:uint6,exceptions,F26_n,[n] THROWIF,f - ,26/76,Throws exception `0 <= n <= 63` with  parameter zero only if integer `f!=0`.
    THROWIFNOT_SHORT,null,#F2A_ n:uint6,exceptions,F2A_n,[n] THROWIFNOT,f - ,26/76,Throws exception `0 <= n <= 63` with parameter zero only if integer `f=0`.
    THROW,null,#F2C4_ n:uint11,exceptions,F2C4_n,[n] THROW,- 0 nn,84,For `0 <= n < 2^11`, an encoding of `[n] THROW` for larger values of `n`.
    THROWARG,null,#F2CC_ n:uint11,exceptions,F2CC_n,[n] THROWARG,x - x nn,84,Throws exception `0 <= n <  2^11` with parameter `x`, by copying `x` and `n` into the stack of `c2` and transferring control to `c2`.
    THROWIF,null,#F2D4_ n:uint11,exceptions,F2D4_n,[n] THROWIF,f - ,34/84,For `0 <= n < 2^11`, an encoding of `[n] THROWIF` for larger values of `n`.
    THROWARGIF,null,#F2DC_ n:uint11,exceptions,F2DC_n,[n] THROWARGIF,x f - ,34/84,Throws exception `0 <= nn < 2^11` with parameter `x` only if integer `f!=0`.
    THROWIFNOT,null,#F2E4_ n:uint11,exceptions,F2E4_n,[n] THROWIFNOT,f - ,34/84,For `0 <= n < 2^11`, an encoding of `[n] THROWIFNOT` for larger values of `n`.
    THROWARGIFNOT,null,#F2EC_ n:uint11,exceptions,F2EC_n,[n] THROWARGIFNOT,x f - ,34/84,Throws exception `0 <= n < 2^11` with parameter `x` only if integer `f=0`.
    THROWANY,null,#F2F0,exceptions,F2F0,THROWANY,n - 0 n,76,Throws exception `0 <= n < 2^16` with parameter zero.\nApproximately equivalent to `ZERO` `SWAP` `THROWARGANY`.
    THROWARGANY,null,#F2F1,exceptions,F2F1,THROWARGANY,x n - x n,76,Throws exception `0 <= n < 2^16` with parameter `x`, transferring control to the continuation in `c2`.\nApproximately equivalent to `c2 PUSHCTR` `2 JMPXARGS`.
    THROWANYIF,null,#F2F2,exceptions,F2F2,THROWANYIF,n f - ,26/76,Throws exception `0 <= n < 2^16` with parameter zero only if `f!=0`.
    THROWARGANYIF,null,#F2F3,exceptions,F2F3,THROWARGANYIF,x n f - ,26/76,Throws exception `0 <= n<2^16` with parameter `x` only if `f!=0`.
    THROWANYIFNOT,null,#F2F4,exceptions,F2F4,THROWANYIFNOT,n f - ,26/76,Throws exception `0 <= n<2^16` with parameter zero only if `f=0`.
    THROWARGANYIFNOT,null,#F2F5,exceptions,F2F5,THROWARGANYIFNOT,x n f - ,26/76,Throws exception `0 <= n<2^16` with parameter `x` only if `f=0`.
    TRY,null,#F2FF,exceptions,F2FF,TRY,c c' - ,26,Sets `c2` to `c'`, first saving the old value of `c2` both into the savelist of `c'` and into the savelist of the current continuation, which is stored into `c.c0` and `c'.c0`. Then runs `c` similarly to `EXECUTE`. If `c` does not throw any exceptions, the original value of `c2` is automatically restored on return from `c`. If an exception occurs, the execution is transferred to `c'`, but the original value of `c2` is restored in the process, so that `c'` can re-throw the exception by `THROWANY` if it cannot handle it by itself.
    null,null,null,exceptions,F2FF,TRY:<{ code1 }>CATCH<{ code2 }>,-,null,Equivalent to `<{ code1 }> CONT` `<{ code2 }> CONT` `TRY`.
    TRYARGS,null,#F3 p:uint4 r:uint4,exceptions,F3pr,[p] [r] TRYARGS,c c' - ,26,Similar to `TRY`, but with `[p] [r] CALLXARGS` internally used instead of `EXECUTE`.\nIn this way, all but the top `0 <= p <= 15` stack elements will be saved into current continuation's stack, and then restored upon return from either `c` or `c'`, with the top `0 <= r <= 15` values of the resulting stack of `c` or `c'` copied as return values.
    NEWDICT,NULL,#6D,dict_create,6D,NEWDICT, - D,18,Returns a new empty dictionary.\nIt is an alternative mnemonics for `PUSHNULL`.
    DICTEMPTY,ISNULL,#6E,dict_create,6E,DICTEMPTY,D - ?,18,Checks whether dictionary `D` is empty, and returns `-1` or `0` accordingly.\nIt is an alternative mnemonics for `ISNULL`.
    STDICTS,STSLICE,#CE,dict_serial,CE,STDICTS\n,s b - b',18,Stores a _Slice_-represented dictionary `s` into _Builder_ `b`.\nIt is actually a synonym for `STSLICE`.
    STDICT,null,#F400,dict_serial,F400,STDICT\nSTOPTREF,D b - b',26,Stores dictionary `D` into _Builder_ `b`, returing the resulting _Builder_ `b'`.\nIn other words, if `D` is a cell, performs `STONE` and `STREF`; if `D` is _Null_, performs `NIP` and `STZERO`; otherwise throws a type checking exception.
    SKIPDICT,null,#F401,dict_serial,F401,SKIPDICT\nSKIPOPTREF,s - s',26,Equivalent to `LDDICT` `NIP`.
    LDDICTS,null,#F402,dict_serial,F402,LDDICTS,s - s' s'',26,Loads (parses) a (_Slice_-represented) dictionary `s'` from _Slice_ `s`, and returns the remainder of `s` as `s''`.\nThis is a “split function'' for all `HashmapE(n,X)` dictionary types.
    PLDDICTS,null,#F403,dict_serial,F403,PLDDICTS,s - s',26,Preloads a (_Slice_-represented) dictionary `s'` from _Slice_ `s`.\nApproximately equivalent to `LDDICTS` `DROP`.
    LDDICT,null,#F404,dict_serial,F404,LDDICT\nLDOPTREF,s - D s',26,Loads (parses) a dictionary `D` from _Slice_ `s`, and returns the remainder of `s` as `s'`. May be applied to dictionaries or to values of arbitrary `(^Y)?` types.
    PLDDICT,null,#F405,dict_serial,F405,PLDDICT\nPLDOPTREF,s - D,26,Preloads a dictionary `D` from _Slice_ `s`.\nApproximately equivalent to `LDDICT` `DROP`.
    LDDICTQ,null,#F406,dict_serial,F406,LDDICTQ,s - D s' -1 or s 0,26,A quiet version of `LDDICT`.
    PLDDICTQ,null,#F407,dict_serial,F407,PLDDICTQ,s - D -1 or 0,26,A quiet version of `PLDDICT`.
    DICTGET,null,#F40A,dict_get,F40A,DICTGET,k D n - x -1 or 0,null,Looks up key `k` (represented by a _Slice_, the first `0 <= n <= 1023` data bits of which are used as a key) in dictionary `D` of type `HashmapE(n,X)` with `n`-bit keys.\nOn success, returns the value found as a _Slice_ `x`.
    DICTGETREF,null,#F40B,dict_get,F40B,DICTGETREF,k D n - c -1 or 0,null,Similar to `DICTGET`, but with a `LDREF` `ENDS` applied to `x` on success.\nThis operation is useful for dictionaries of type `HashmapE(n,^Y)`.
    DICTIGET,null,#F40C,dict_get,F40C,DICTIGET,i D n - x -1 or 0,null,Similar to `DICTGET`, but with a signed (big-endian) `n`-bit _Integer_ `i` as a key. If `i` does not fit into `n` bits, returns `0`. If `i` is a `NaN`, throws an integer overflow exception.
    DICTIGETREF,null,#F40D,dict_get,F40D,DICTIGETREF,i D n - c -1 or 0,null,Combines `DICTIGET` with `DICTGETREF`: it uses signed `n`-bit _Integer_ `i` as a key and returns a _Cell_ instead of a _Slice_ on success.
    DICTUGET,null,#F40E,dict_get,F40E,DICTUGET,i D n - x -1 or 0,null,Similar to `DICTIGET`, but with _unsigned_ (big-endian) `n`-bit _Integer_ `i` used as a key.
    DICTUGETREF,null,#F40F,dict_get,F40F,DICTUGETREF,i D n - c -1 or 0,null,Similar to `DICTIGETREF`, but with an unsigned `n`-bit _Integer_ key `i`.
    DICTSET,null,#F412,dict_set,F412,DICTSET,x k D n - D',null,Sets the value associated with `n`-bit key `k` (represented by a _Slice_ as in `DICTGET`) in dictionary `D` (also represented by a _Slice_) to value `x` (again a _Slice_), and returns the resulting dictionary as `D'`.
    DICTSETREF,null,#F413,dict_set,F413,DICTSETREF,c k D n - D',null,Similar to `DICTSET`, but with the value set to a reference to _Cell_ `c`.
    DICTISET,null,#F414,dict_set,F414,DICTISET,x i D n - D',null,Similar to `DICTSET`, but with the key represented by a (big-endian) signed `n`-bit integer `i`. If `i` does not fit into `n` bits, a range check exception is generated.
    DICTISETREF,null,#F415,dict_set,F415,DICTISETREF,c i D n - D',null,Similar to `DICTSETREF`, but with the key a signed `n`-bit integer as in `DICTISET`.
    DICTUSET,null,#F416,dict_set,F416,DICTUSET,x i D n - D',null,Similar to `DICTISET`, but with `i` an _unsigned_ `n`-bit integer.
    DICTUSETREF,null,#F417,dict_set,F417,DICTUSETREF,c i D n - D',null,Similar to `DICTISETREF`, but with `i` unsigned.
    DICTSETGET,null,#F41A,dict_set,F41A,DICTSETGET,x k D n - D' y -1 or D' 0,null,Combines `DICTSET` with `DICTGET`: it sets the value corresponding to key `k` to `x`, but also returns the old value `y` associated with the key in question, if present.
    DICTSETGETREF,null,#F41B,dict_set,F41B,DICTSETGETREF,c k D n - D' c' -1 or D' 0,null,Combines `DICTSETREF` with `DICTGETREF` similarly to `DICTSETGET`.
    DICTISETGET,null,#F41C,dict_set,F41C,DICTISETGET,x i D n - D' y -1 or D' 0,null,`DICTISETGET`, but with `i` a signed `n`-bit integer.
    DICTISETGETREF,null,#F41D,dict_set,F41D,DICTISETGETREF,c i D n - D' c' -1 or D' 0,null,`DICTISETGETREF`, but with `i` a signed `n`-bit integer.
    DICTUSETGET,null,#F41E,dict_set,F41E,DICTUSETGET,x i D n - D' y -1 or D' 0,null,`DICTISETGET`, but with `i` an unsigned `n`-bit integer.
    DICTUSETGETREF,null,#F41F,dict_set,F41F,DICTUSETGETREF,c i D n - D' c' -1 or D' 0,null,`DICTISETGETREF`, but with `i` an unsigned `n`-bit integer.
    DICTREPLACE,null,#F422,dict_set,F422,DICTREPLACE,x k D n - D' -1 or D 0,null,A _Replace_ operation, which is similar to `DICTSET`, but sets the value of key `k` in dictionary `D` to `x` only if the key `k` was already present in `D`.
    DICTREPLACEREF,null,#F423,dict_set,F423,DICTREPLACEREF,c k D n - D' -1 or D 0,null,A _Replace_ counterpart of `DICTSETREF`.
    DICTIREPLACE,null,#F424,dict_set,F424,DICTIREPLACE,x i D n - D' -1 or D 0,null,`DICTREPLACE`, but with `i` a signed `n`-bit integer.
    DICTIREPLACEREF,null,#F425,dict_set,F425,DICTIREPLACEREF,c i D n - D' -1 or D 0,null,`DICTREPLACEREF`, but with `i` a signed `n`-bit integer.
    DICTUREPLACE,null,#F426,dict_set,F426,DICTUREPLACE,x i D n - D' -1 or D 0,null,`DICTREPLACE`, but with `i` an unsigned `n`-bit integer.
    DICTUREPLACEREF,null,#F427,dict_set,F427,DICTUREPLACEREF,c i D n - D' -1 or D 0,null,`DICTREPLACEREF`, but with `i` an unsigned `n`-bit integer.
    DICTREPLACEGET,null,#F42A,dict_set,F42A,DICTREPLACEGET,x k D n - D' y -1 or D 0,null,A _Replace_ counterpart of `DICTSETGET`: on success, also returns the old value associated with the key in question.
    DICTREPLACEGETREF,null,#F42B,dict_set,F42B,DICTREPLACEGETREF,c k D n - D' c' -1 or D 0,null,A _Replace_ counterpart of `DICTSETGETREF`.
    DICTIREPLACEGET,null,#F42C,dict_set,F42C,DICTIREPLACEGET,x i D n - D' y -1 or D 0,null,`DICTREPLACEGET`, but with `i` a signed `n`-bit integer.
    DICTIREPLACEGETREF,null,#F42D,dict_set,F42D,DICTIREPLACEGETREF,c i D n - D' c' -1 or D 0,null,`DICTREPLACEGETREF`, but with `i` a signed `n`-bit integer.
    DICTUREPLACEGET,null,#F42E,dict_set,F42E,DICTUREPLACEGET,x i D n - D' y -1 or D 0,null,`DICTREPLACEGET`, but with `i` an unsigned `n`-bit integer.
    DICTUREPLACEGETREF,null,#F42F,dict_set,F42F,DICTUREPLACEGETREF,c i D n - D' c' -1 or D 0,null,`DICTREPLACEGETREF`, but with `i` an unsigned `n`-bit integer.
    DICTADD,null,#F432,dict_set,F432,DICTADD,x k D n - D' -1 or D 0,null,An _Add_ counterpart of `DICTSET`: sets the value associated with key `k` in dictionary `D` to `x`, but only if it is not already present in `D`.
    DICTADDREF,null,#F433,dict_set,F433,DICTADDREF,c k D n - D' -1 or D 0,null,An _Add_ counterpart of `DICTSETREF`.
    DICTIADD,null,#F434,dict_set,F434,DICTIADD,x i D n - D' -1 or D 0,null,`DICTADD`, but with `i` a signed `n`-bit integer.
    DICTIADDREF,null,#F435,dict_set,F435,DICTIADDREF,c i D n - D' -1 or D 0,null,`DICTADDREF`, but with `i` a signed `n`-bit integer.
    DICTUADD,null,#F436,dict_set,F436,DICTUADD,x i D n - D' -1 or D 0,null,`DICTADD`, but with `i` an unsigned `n`-bit integer.
    DICTUADDREF,null,#F437,dict_set,F437,DICTUADDREF,c i D n - D' -1 or D 0,null,`DICTADDREF`, but with `i` an unsigned `n`-bit integer.
    DICTADDGET,null,#F43A,dict_set,F43A,DICTADDGET,x k D n - D' -1 or D y 0,null,An _Add_ counterpart of `DICTSETGET`: sets the value associated with key `k` in dictionary `D` to `x`, but only if key `k` is not already present in `D`. Otherwise, just returns the old value `y` without changing the dictionary.
    DICTADDGETREF,null,#F43B,dict_set,F43B,DICTADDGETREF,c k D n - D' -1 or D c' 0,null,An _Add_ counterpart of `DICTSETGETREF`.
    DICTIADDGET,null,#F43C,dict_set,F43C,DICTIADDGET,x i D n - D' -1 or D y 0,null,`DICTADDGET`, but with `i` a signed `n`-bit integer.
    DICTIADDGETREF,null,#F43D,dict_set,F43D,DICTIADDGETREF,c i D n - D' -1 or D c' 0,null,`DICTADDGETREF`, but with `i` a signed `n`-bit integer.
    DICTUADDGET,null,#F43E,dict_set,F43E,DICTUADDGET,x i D n - D' -1 or D y 0,null,`DICTADDGET`, but with `i` an unsigned `n`-bit integer.
    DICTUADDGETREF,null,#F43F,dict_set,F43F,DICTUADDGETREF,c i D n - D' -1 or D c' 0,null,`DICTADDGETREF`, but with `i` an unsigned `n`-bit integer.
    DICTSETB,null,#F441,dict_set_builder,F441,DICTSETB,b k D n - D',null,null
    DICTISETB,null,#F442,dict_set_builder,F442,DICTISETB,b i D n - D',null,null
    DICTUSETB,null,#F443,dict_set_builder,F443,DICTUSETB,b i D n - D',null,null
    DICTSETGETB,null,#F445,dict_set_builder,F445,DICTSETGETB,b k D n - D' y -1 or D' 0,null,null
    DICTISETGETB,null,#F446,dict_set_builder,F446,DICTISETGETB,b i D n - D' y -1 or D' 0,null,null
    DICTUSETGETB,null,#F447,dict_set_builder,F447,DICTUSETGETB,b i D n - D' y -1 or D' 0,null,null
    DICTREPLACEB,null,#F449,dict_set_builder,F449,DICTREPLACEB,b k D n - D' -1 or D 0,null,null
    DICTIREPLACEB,null,#F44A,dict_set_builder,F44A,DICTIREPLACEB,b i D n - D' -1 or D 0,null,null
    DICTUREPLACEB,null,#F44B,dict_set_builder,F44B,DICTUREPLACEB,b i D n - D' -1 or D 0,null,null
    DICTREPLACEGETB,null,#F44D,dict_set_builder,F44D,DICTREPLACEGETB,b k D n - D' y -1 or D 0,null,null
    DICTIREPLACEGETB,null,#F44E,dict_set_builder,F44E,DICTIREPLACEGETB,b i D n - D' y -1 or D 0,null,null
    DICTUREPLACEGETB,null,#F44F,dict_set_builder,F44F,DICTUREPLACEGETB,b i D n - D' y -1 or D 0,null,null
    DICTADDB,null,#F451,dict_set_builder,F451,DICTADDB,b k D n - D' -1 or D 0,null,null
    DICTIADDB,null,#F452,dict_set_builder,F452,DICTIADDB,b i D n - D' -1 or D 0,null,null
    DICTUADDB,null,#F453,dict_set_builder,F453,DICTUADDB,b i D n - D' -1 or D 0,null,null
    DICTADDGETB,null,#F455,dict_set_builder,F455,DICTADDGETB,b k D n - D' -1 or D y 0,null,null
    DICTIADDGETB,null,#F456,dict_set_builder,F456,DICTIADDGETB,b i D n - D' -1 or D y 0,null,null
    DICTUADDGETB,null,#F457,dict_set_builder,F457,DICTUADDGETB,b i D n - D' -1 or D y 0,null,null
    DICTDEL,null,#F459,dict_delete,F459,DICTDEL,k D n - D' -1 or D 0,null,Deletes `n`-bit key, represented by a _Slice_ `k`, from dictionary `D`. If the key is present, returns the modified dictionary `D'` and the success flag `-1`. Otherwise, returns the original dictionary `D` and `0`.
    DICTIDEL,null,#F45A,dict_delete,F45A,DICTIDEL,i D n - D' ?,null,A version of `DICTDEL` with the key represented by a signed `n`-bit _Integer_ `i`. If `i` does not fit into `n` bits, simply returns `D` `0` (“key not found, dictionary unmodified'').
    DICTUDEL,null,#F45B,dict_delete,F45B,DICTUDEL,i D n - D' ?,null,Similar to `DICTIDEL`, but with `i` an unsigned `n`-bit integer.
    DICTDELGET,null,#F462,dict_delete,F462,DICTDELGET,k D n - D' x -1 or D 0,null,Deletes `n`-bit key, represented by a _Slice_ `k`, from dictionary `D`. If the key is present, returns the modified dictionary `D'`, the original value `x` associated with the key `k` (represented by a _Slice_), and the success flag `-1`. Otherwise, returns the original dictionary `D` and `0`.
    DICTDELGETREF,null,#F463,dict_delete,F463,DICTDELGETREF,k D n - D' c -1 or D 0,null,Similar to `DICTDELGET`, but with `LDREF` `ENDS` applied to `x` on success, so that the value returned `c` is a _Cell_.
    DICTIDELGET,null,#F464,dict_delete,F464,DICTIDELGET,i D n - D' x -1 or D 0,null,`DICTDELGET`, but with `i` a signed `n`-bit integer.
    DICTIDELGETREF,null,#F465,dict_delete,F465,DICTIDELGETREF,i D n - D' c -1 or D 0,null,`DICTDELGETREF`, but with `i` a signed `n`-bit integer.
    DICTUDELGET,null,#F466,dict_delete,F466,DICTUDELGET,i D n - D' x -1 or D 0,null,`DICTDELGET`, but with `i` an unsigned `n`-bit integer.
    DICTUDELGETREF,null,#F467,dict_delete,F467,DICTUDELGETREF,i D n - D' c -1 or D 0,null,`DICTDELGETREF`, but with `i` an unsigned `n`-bit integer.
    DICTGETOPTREF,null,#F469,dict_mayberef,F469,DICTGETOPTREF,k D n - c^?,null,A variant of `DICTGETREF` that returns _Null_ instead of the value `c^?` if the key `k` is absent from dictionary `D`.
    DICTIGETOPTREF,null,#F46A,dict_mayberef,F46A,DICTIGETOPTREF,i D n - c^?,null,`DICTGETOPTREF`, but with `i` a signed `n`-bit integer. If the key `i` is out of range, also returns _Null_.
    DICTUGETOPTREF,null,#F46B,dict_mayberef,F46B,DICTUGETOPTREF,i D n - c^?,null,`DICTGETOPTREF`, but with `i` an unsigned `n`-bit integer. If the key `i` is out of range, also returns _Null_.
    DICTSETGETOPTREF,null,#F46D,dict_mayberef,F46D,DICTSETGETOPTREF,c^? k D n - D' ~c^?,null,A variant of both `DICTGETOPTREF` and `DICTSETGETREF` that sets the value corresponding to key `k` in dictionary `D` to `c^?` (if `c^?` is _Null_, then the key is deleted instead), and returns the old value `~c^?` (if the key `k` was absent before, returns _Null_ instead).
    DICTISETGETOPTREF,null,#F46E,dict_mayberef,F46E,DICTISETGETOPTREF,c^? i D n - D' ~c^?,null,Similar to primitive `DICTSETGETOPTREF`, but using signed `n`-bit _Integer_ `i` as a key. If `i` does not fit into `n` bits, throws a range checking exception.
    DICTUSETGETOPTREF,null,#F46F,dict_mayberef,F46F,DICTUSETGETOPTREF,c^? i D n - D' ~c^?,null,Similar to primitive `DICTSETGETOPTREF`, but using unsigned `n`-bit _Integer_ `i` as a key.
    PFXDICTSET,null,#F470,dict_prefix,F470,PFXDICTSET,x k D n - D' -1 or D 0,null,null
    PFXDICTREPLACE,null,#F471,dict_prefix,F471,PFXDICTREPLACE,x k D n - D' -1 or D 0,null,null
    PFXDICTADD,null,#F472,dict_prefix,F472,PFXDICTADD,x k D n - D' -1 or D 0,null,null
    PFXDICTDEL,null,#F473,dict_prefix,F473,PFXDICTDEL,k D n - D' -1 or D 0,null,null
    DICTGETNEXT,null,#F474,dict_next,F474,DICTGETNEXT,k D n - x' k' -1 or 0,null,Computes the minimal key `k'` in dictionary `D` that is lexicographically greater than `k`, and returns `k'` (represented by a _Slice_) along with associated value `x'` (also represented by a _Slice_).
    DICTGETNEXTEQ,null,#F475,dict_next,F475,DICTGETNEXTEQ,k D n - x' k' -1 or 0,null,Similar to `DICTGETNEXT`, but computes the minimal key `k'` that is lexicographically greater than or equal to `k`.
    DICTGETPREV,null,#F476,dict_next,F476,DICTGETPREV,k D n - x' k' -1 or 0,null,Similar to `DICTGETNEXT`, but computes the maximal key `k'` lexicographically smaller than `k`.
    DICTGETPREVEQ,null,#F477,dict_next,F477,DICTGETPREVEQ,k D n - x' k' -1 or 0,null,Similar to `DICTGETPREV`, but computes the maximal key `k'` lexicographically smaller than or equal to `k`.
    DICTIGETNEXT,null,#F478,dict_next,F478,DICTIGETNEXT,i D n - x' i' -1 or 0,null,Similar to `DICTGETNEXT`, but interprets all keys in dictionary `D` as big-endian signed `n`-bit integers, and computes the minimal key `i'` that is larger than _Integer_ `i` (which does not necessarily fit into `n` bits).
    DICTIGETNEXTEQ,null,#F479,dict_next,F479,DICTIGETNEXTEQ,i D n - x' i' -1 or 0,null,Similar to `DICTGETNEXTEQ`, but interprets keys as signed `n`-bit integers.
    DICTIGETPREV,null,#F47A,dict_next,F47A,DICTIGETPREV,i D n - x' i' -1 or 0,null,Similar to `DICTGETPREV`, but interprets keys as signed `n`-bit integers.
    DICTIGETPREVEQ,null,#F47B,dict_next,F47B,DICTIGETPREVEQ,i D n - x' i' -1 or 0,null,Similar to `DICTGETPREVEQ`, but interprets keys as signed `n`-bit integers.
    DICTUGETNEXT,null,#F47C,dict_next,F47C,DICTUGETNEXT,i D n - x' i' -1 or 0,null,Similar to `DICTGETNEXT`, but interprets all keys in dictionary `D` as big-endian unsigned `n`-bit integers, and computes the minimal key `i'` that is larger than _Integer_ `i` (which does not necessarily fit into `n` bits, and is not necessarily non-negative).
    DICTUGETNEXTEQ,null,#F47D,dict_next,F47D,DICTUGETNEXTEQ,i D n - x' i' -1 or 0,null,Similar to `DICTGETNEXTEQ`, but interprets keys as unsigned `n`-bit integers.
    DICTUGETPREV,null,#F47E,dict_next,F47E,DICTUGETPREV,i D n - x' i' -1 or 0,null,Similar to `DICTGETPREV`, but interprets keys as unsigned `n`-bit integers.
    DICTUGETPREVEQ,null,#F47F,dict_next,F47F,DICTUGETPREVEQ,i D n - x' i' -1 or 0,null,Similar to `DICTGETPREVEQ`, but interprets keys a unsigned `n`-bit integers.
    DICTMIN,null,#F482,dict_min,F482,DICTMIN,D n - x k -1 or 0,null,Computes the minimal key `k` (represented by a _Slice_ with `n` data bits) in dictionary `D`, and returns `k` along with the associated value `x`.
    DICTMINREF,null,#F483,dict_min,F483,DICTMINREF,D n - c k -1 or 0,null,Similar to `DICTMIN`, but returns the only reference in the value as a _Cell_ `c`.
    DICTIMIN,null,#F484,dict_min,F484,DICTIMIN,D n - x i -1 or 0,null,Similar to `DICTMIN`, but computes the minimal key `i` under the assumption that all keys are big-endian signed `n`-bit integers. Notice that the key and value returned may differ from those computed by `DICTMIN` and `DICTUMIN`.
    DICTIMINREF,null,#F485,dict_min,F485,DICTIMINREF,D n - c i -1 or 0,null,Similar to `DICTIMIN`, but returns the only reference in the value.
    DICTUMIN,null,#F486,dict_min,F486,DICTUMIN,D n - x i -1 or 0,null,Similar to `DICTMIN`, but returns the key as an unsigned `n`-bit _Integer_ `i`.
    DICTUMINREF,null,#F487,dict_min,F487,DICTUMINREF,D n - c i -1 or 0,null,Similar to `DICTUMIN`, but returns the only reference in the value.
    DICTMAX,null,#F48A,dict_min,F48A,DICTMAX,D n - x k -1 or 0,null,Computes the maximal key `k` (represented by a _Slice_ with `n` data bits) in dictionary `D`, and returns `k` along with the associated value `x`.
    DICTMAXREF,null,#F48B,dict_min,F48B,DICTMAXREF,D n - c k -1 or 0,null,Similar to `DICTMAX`, but returns the only reference in the value.
    DICTIMAX,null,#F48C,dict_min,F48C,DICTIMAX,D n - x i -1 or 0,null,Similar to `DICTMAX`, but computes the maximal key `i` under the assumption that all keys are big-endian signed `n`-bit integers. Notice that the key and value returned may differ from those computed by `DICTMAX` and `DICTUMAX`.
    DICTIMAXREF,null,#F48D,dict_min,F48D,DICTIMAXREF,D n - c i -1 or 0,null,Similar to `DICTIMAX`, but returns the only reference in the value.
    DICTUMAX,null,#F48E,dict_min,F48E,DICTUMAX,D n - x i -1 or 0,null,Similar to `DICTMAX`, but returns the key as an unsigned `n`-bit _Integer_ `i`.
    DICTUMAXREF,null,#F48F,dict_min,F48F,DICTUMAXREF,D n - c i -1 or 0,null,Similar to `DICTUMAX`, but returns the only reference in the value.
    DICTREMMIN,null,#F492,dict_min,F492,DICTREMMIN,D n - D' x k -1 or D 0,null,Computes the minimal key `k` (represented by a _Slice_ with `n` data bits) in dictionary `D`, removes `k` from the dictionary, and returns `k` along with the associated value `x` and the modified dictionary `D'`.
    DICTREMMINREF,null,#F493,dict_min,F493,DICTREMMINREF,D n - D' c k -1 or D 0,null,Similar to `DICTREMMIN`, but returns the only reference in the value as a _Cell_ `c`.
    DICTIREMMIN,null,#F494,dict_min,F494,DICTIREMMIN,D n - D' x i -1 or D 0,null,Similar to `DICTREMMIN`, but computes the minimal key `i` under the assumption that all keys are big-endian signed `n`-bit integers. Notice that the key and value returned may differ from those computed by `DICTREMMIN` and `DICTUREMMIN`.
    DICTIREMMINREF,null,#F495,dict_min,F495,DICTIREMMINREF,D n - D' c i -1 or D 0,null,Similar to `DICTIREMMIN`, but returns the only reference in the value.
    DICTUREMMIN,null,#F496,dict_min,F496,DICTUREMMIN,D n - D' x i -1 or D 0,null,Similar to `DICTREMMIN`, but returns the key as an unsigned `n`-bit _Integer_ `i`.
    DICTUREMMINREF,null,#F497,dict_min,F497,DICTUREMMINREF,D n - D' c i -1 or D 0,null,Similar to `DICTUREMMIN`, but returns the only reference in the value.
    DICTREMMAX,null,#F49A,dict_min,F49A,DICTREMMAX,D n - D' x k -1 or D 0,null,Computes the maximal key `k` (represented by a _Slice_ with `n` data bits) in dictionary `D`, removes `k` from the dictionary, and returns `k` along with the associated value `x` and the modified dictionary `D'`.
    DICTREMMAXREF,null,#F49B,dict_min,F49B,DICTREMMAXREF,D n - D' c k -1 or D 0,null,Similar to `DICTREMMAX`, but returns the only reference in the value as a _Cell_ `c`.
    DICTIREMMAX,null,#F49C,dict_min,F49C,DICTIREMMAX,D n - D' x i -1 or D 0,null,Similar to `DICTREMMAX`, but computes the minimal key `i` under the assumption that all keys are big-endian signed `n`-bit integers. Notice that the key and value returned may differ from those computed by `DICTREMMAX` and `DICTUREMMAX`.
    DICTIREMMAXREF,null,#F49D,dict_min,F49D,DICTIREMMAXREF,D n - D' c i -1 or D 0,null,Similar to `DICTIREMMAX`, but returns the only reference in the value.
    DICTUREMMAX,null,#F49E,dict_min,F49E,DICTUREMMAX,D n - D' x i -1 or D 0,null,Similar to `DICTREMMAX`, but returns the key as an unsigned `n`-bit _Integer_ `i`.
    DICTUREMMAXREF,null,#F49F,dict_min,F49F,DICTUREMMAXREF,D n - D' c i -1 or D 0,null,Similar to `DICTUREMMAX`, but returns the only reference in the value.
    DICTIGETJMP,null,#F4A0,dict_special,F4A0,DICTIGETJMP,i D n - ,null,Similar to `DICTIGET`, but with `x` `BLESS`ed into a continuation with a subsequent `JMPX` to it on success. On failure, does nothing. This is useful for implementing `switch`/`case` constructions.
    DICTUGETJMP,null,#F4A1,dict_special,F4A1,DICTUGETJMP,i D n - ,null,Similar to `DICTIGETJMP`, but performs `DICTUGET` instead of `DICTIGET`.
    DICTIGETEXEC,null,#F4A2,dict_special,F4A2,DICTIGETEXEC,i D n - ,null,Similar to `DICTIGETJMP`, but with `EXECUTE` instead of `JMPX`.
    DICTUGETEXEC,null,#F4A3,dict_special,F4A3,DICTUGETEXEC,i D n - ,null,Similar to `DICTUGETJMP`, but with `EXECUTE` instead of `JMPX`.
    DICTPUSHCONST,null,#F4A6_ d:^Cell n:uint10,dict_special,F4A6_n,[ref] [n] DICTPUSHCONST, - D n,34,Pushes a non-empty constant dictionary `D` (as a `Cell^?`) along with its key length `0 <= n <= 1023`, stored as a part of the instruction. The dictionary itself is created from the first of remaining references of the current continuation. In this way, the complete `DICTPUSHCONST` instruction can be obtained by first serializing `xF4A4_`, then the non-empty dictionary itself (one `1` bit and a cell reference), and then the unsigned 10-bit integer `n` (as if by a `STU 10` instruction). An empty dictionary can be pushed by a `NEWDICT` primitive instead.
    PFXDICTGETQ,null,#F4A8,dict_special,F4A8,PFXDICTGETQ,s D n - s' x s'' -1 or s 0,null,Looks up the unique prefix of _Slice_ `s` present in the prefix code dictionary represented by `Cell^?` `D` and `0 <= n <= 1023`. If found, the prefix of `s` is returned as `s'`, and the corresponding value (also a _Slice_) as `x`. The remainder of `s` is returned as a _Slice_ `s''`. If no prefix of `s` is a key in prefix code dictionary `D`, returns the unchanged `s` and a zero flag to indicate failure.
    PFXDICTGET,null,#F4A9,dict_special,F4A9,PFXDICTGET,s D n - s' x s'',null,Similar to `PFXDICTGET`, but throws a cell deserialization failure exception on failure.
    PFXDICTGETJMP,null,#F4AA,dict_special,F4AA,PFXDICTGETJMP,s D n - s' s'' or s,null,Similar to `PFXDICTGETQ`, but on success `BLESS`es the value `x` into a _Continuation_ and transfers control to it as if by a `JMPX`. On failure, returns `s` unchanged and continues execution.
    PFXDICTGETEXEC,null,#F4AB,dict_special,F4AB,PFXDICTGETEXEC,s D n - s' s'',null,Similar to `PFXDICTGETJMP`, but `EXEC`utes the continuation found instead of jumping to it. On failure, throws a cell deserialization exception.
    PFXDICTCONSTGETJMP,null,#F4AE_ d:^Cell n:uint10,dict_special,F4AE_n,[ref] [n] PFXDICTCONSTGETJMP\n[ref] [n] PFXDICTSWITCH,s - s' s'' or s,null,Combines `[n] DICTPUSHCONST` for `0 <= n <= 1023` with `PFXDICTGETJMP`.
    DICTIGETJMPZ,null,#F4BC,dict_special,F4BC,DICTIGETJMPZ,i D n - i or nothing,null,A variant of `DICTIGETJMP` that returns index `i` on failure.
    DICTUGETJMPZ,null,#F4BD,dict_special,F4BD,DICTUGETJMPZ,i D n - i or nothing,null,A variant of `DICTUGETJMP` that returns index `i` on failure.
    DICTIGETEXECZ,null,#F4BE,dict_special,F4BE,DICTIGETEXECZ,i D n - i or nothing,null,A variant of `DICTIGETEXEC` that returns index `i` on failure.
    DICTUGETEXECZ,null,#F4BF,dict_special,F4BF,DICTUGETEXECZ,i D n - i or nothing,null,A variant of `DICTUGETEXEC` that returns index `i` on failure.
    SUBDICTGET,null,#F4B1,dict_sub,F4B1,SUBDICTGET,k l D n - D',null,Constructs a subdictionary consisting of all keys beginning with prefix `k` (represented by a _Slice_, the first `0 <= l <= n <= 1023` data bits of which are used as a key) of length `l` in dictionary `D` of type `HashmapE(n,X)` with `n`-bit keys. On success, returns the new subdictionary of the same type `HashmapE(n,X)` as a _Slice_ `D'`.
    SUBDICTIGET,null,#F4B2,dict_sub,F4B2,SUBDICTIGET,x l D n - D',null,Variant of `SUBDICTGET` with the prefix represented by a signed big-endian `l`-bit _Integer_ `x`, where necessarily `l <= 257`.
    SUBDICTUGET,null,#F4B3,dict_sub,F4B3,SUBDICTUGET,x l D n - D',null,Variant of `SUBDICTGET` with the prefix represented by an unsigned big-endian `l`-bit _Integer_ `x`, where necessarily `l <= 256`.
    SUBDICTRPGET,null,#F4B5,dict_sub,F4B5,SUBDICTRPGET,k l D n - D',null,Similar to `SUBDICTGET`, but removes the common prefix `k` from all keys of the new dictionary `D'`, which becomes of type `HashmapE(n-l,X)`.
    SUBDICTIRPGET,null,#F4B6,dict_sub,F4B6,SUBDICTIRPGET,x l D n - D',null,Variant of `SUBDICTRPGET` with the prefix represented by a signed big-endian `l`-bit _Integer_ `x`, where necessarily `l <= 257`.
    SUBDICTURPGET,null,#F4B7,dict_sub,F4B7,SUBDICTURPGET,x l D n - D',null,Variant of `SUBDICTRPGET` with the prefix represented by an unsigned big-endian `l`-bit _Integer_ `x`, where necessarily `l <= 256`.
    ACCEPT,null,#F800,app_gas,F800,ACCEPT,-,26,Sets current gas limit `g_l` to its maximal allowed value `g_m`, and resets the gas credit `g_c` to zero, decreasing the value of `g_r` by `g_c` in the process.\nIn other words, the current smart contract agrees to buy some gas to finish the current transaction. This action is required to process external messages, which bring no value (hence no gas) with themselves.
    SETGASLIMIT,null,#F801,app_gas,F801,SETGASLIMIT,g - ,26,Sets current gas limit `g_l` to the minimum of `g` and `g_m`, and resets the gas credit `g_c` to zero. If the gas consumed so far (including the present instruction) exceeds the resulting value of `g_l`, an (unhandled) out of gas exception is thrown before setting new gas limits. Notice that `SETGASLIMIT` with an argument `g >= 2^63-1` is equivalent to `ACCEPT`.
    COMMIT,null,#F80F,app_gas,F80F,COMMIT,-,26,Commits the current state of registers `c4` (“persistent data'') and `c5` (“actions'') so that the current execution is considered “successful'' with the saved values even if an exception is thrown later.
    RANDU256,null,#F810,app_rnd,F810,RANDU256,- x,26+|c7|+|c1_1|,Generates a new pseudo-random unsigned 256-bit _Integer_ `x`. The algorithm is as follows: if `r` is the old value of the random seed, considered as a 32-byte array (by constructing the big-endian representation of an unsigned 256-bit integer), then its `sha512(r)` is computed; the first 32 bytes of this hash are stored as the new value `r'` of the random seed, and the remaining 32 bytes are returned as the next random value `x`.
    RAND,null,#F811,app_rnd,F811,RAND,y - z,26+|c7|+|c1_1|,Generates a new pseudo-random integer `z` in the range `0...y-1` (or `y...-1`, if `y<0`). More precisely, an unsigned random value `x` is generated as in `RAND256U`; then `z:=floor(x*y/2^256)` is computed.\nEquivalent to `RANDU256` `256 MULRSHIFT`.
    SETRAND,null,#F814,app_rnd,F814,SETRAND,x - ,26+|c7|+|c1_1|,Sets the random seed to unsigned 256-bit _Integer_ `x`.
    ADDRAND,null,#F815,app_rnd,F815,ADDRAND\nRANDOMIZE,x - ,26,Mixes unsigned 256-bit _Integer_ `x` into the random seed `r` by setting the random seed to `Sha` of the concatenation of two 32-byte strings: the first with the big-endian representation of the old seed `r`, and the second with the big-endian representation of `x`.
    GETPARAM,null,#F82 i:uint4,app_config,F82i,[i] GETPARAM, - x,26,Returns the `i`-th parameter from the _Tuple_ provided at `c7` for `0 <= i <= 15`. Equivalent to `c7 PUSHCTR` `FIRST` `[i] INDEX`.\nIf one of these internal operations fails, throws an appropriate type checking or range checking exception.
    NOW,GETPARAM,#F823,app_config,F823,NOW, - x,26,Returns the current Unix time as an _Integer_. If it is impossible to recover the requested value starting from `c7`, throws a type checking or range checking exception as appropriate.\nEquivalent to `3 GETPARAM`.
    BLOCKLT,GETPARAM,#F824,app_config,F824,BLOCKLT, - x,26,Returns the starting logical time of the current block.\nEquivalent to `4 GETPARAM`.
    LTIME,GETPARAM,#F825,app_config,F825,LTIME, - x,26,Returns the logical time of the current transaction.\nEquivalent to `5 GETPARAM`.
    RANDSEED,GETPARAM,#F826,app_config,F826,RANDSEED, - x,26,Returns the current random seed as an unsigned 256-bit _Integer_.\nEquivalent to `6 GETPARAM`.
    BALANCE,GETPARAM,#F827,app_config,F827,BALANCE, - t,26,Returns the remaining balance of the smart contract as a _Tuple_ consisting of an _Integer_ (the remaining Gram balance in nanograms) and a _Maybe Cell_ (a dictionary with 32-bit keys representing the balance of “extra currencies'').\nEquivalent to `7 GETPARAM`.\nNote that `RAW` primitives such as `SENDRAWMSG` do not update this field.
    MYADDR,GETPARAM,#F828,app_config,F828,MYADDR, - s,26,Returns the internal address of the current smart contract as a _Slice_ with a `MsgAddressInt`. If necessary, it can be parsed further using primitives such as `PARSEMSGADDR` or `REWRITESTDADDR`.\nEquivalent to `8 GETPARAM`.
    CONFIGROOT,GETPARAM,#F829,app_config,F829,CONFIGROOT, - D,26,Returns the _Maybe Cell_ `D` with the current global configuration dictionary. Equivalent to `9 GETPARAM `.
    CONFIGDICT,null,#F830,app_config,F830,CONFIGDICT, - D 32,26,Returns the global configuration dictionary along with its key length (32).\nEquivalent to `CONFIGROOT` `32 PUSHINT`.
    CONFIGPARAM,null,#F832,app_config,F832,CONFIGPARAM,i - c -1 or 0,null,Returns the value of the global configuration parameter with integer index `i` as a _Cell_ `c`, and a flag to indicate success.\nEquivalent to `CONFIGDICT` `DICTIGETREF`.
    CONFIGOPTPARAM,null,#F833,app_config,F833,CONFIGOPTPARAM,i - c^?,null,Returns the value of the global configuration parameter with integer index `i` as a _Maybe Cell_ `c^?`.\nEquivalent to `CONFIGDICT` `DICTIGETOPTREF`.
    GETGLOBVAR,null,#F840,app_global,F840,GETGLOBVAR,k - x,26,Returns the `k`-th global variable for `0 <= k < 255`.\nEquivalent to `c7 PUSHCTR` `SWAP` `INDEXVARQ`.
    GETGLOB,null,#F85_ k:(## 5) {1 <= k},app_global,F85_k,[k] GETGLOB, - x,26,Returns the `k`-th global variable for `1 <= k <= 31`.\nEquivalent to `c7 PUSHCTR` `[k] INDEXQ`.
    SETGLOBVAR,null,#F860,app_global,F860,SETGLOBVAR,x k - ,26+|c7’|,Assigns `x` to the `k`-th global variable for `0 <= k < 255`.\nEquivalent to `c7 PUSHCTR` `ROTREV` `SETINDEXVARQ` `c7 POPCTR`.
    SETGLOB,null,#F87_ k:(## 5) {1 <= k},app_global,F87_k,[k] SETGLOB,x - ,26+|c7’|,Assigns `x` to the `k`-th global variable for `1 <= k <= 31`.\nEquivalent to `c7 PUSHCTR` `SWAP` `k SETINDEXQ` `c7 POPCTR`.
    HASHCU,null,#F900,app_crypto,F900,HASHCU,c - x,26,Computes the representation hash of a _Cell_ `c` and returns it as a 256-bit unsigned integer `x`. Useful for signing and checking signatures of arbitrary entities represented by a tree of cells.
    HASHSU,null,#F901,app_crypto,F901,HASHSU,s - x,526,Computes the hash of a _Slice_ `s` and returns it as a 256-bit unsigned integer `x`. The result is the same as if an ordinary cell containing only data and references from `s` had been created and its hash computed by `HASHCU`.
    SHA256U,null,#F902,app_crypto,F902,SHA256U,s - x,26,Computes `Sha` of the data bits of _Slice_ `s`. If the bit length of `s` is not divisible by eight, throws a cell underflow exception. The hash value is returned as a 256-bit unsigned integer `x`.
    CHKSIGNU,null,#F910,app_crypto,F910,CHKSIGNU,h s k - ?,26,Checks the Ed25519-signature `s` of a hash `h` (a 256-bit unsigned integer, usually computed as the hash of some data) using public key `k` (also represented by a 256-bit unsigned integer).\nThe signature `s` must be a _Slice_ containing at least 512 data bits; only the first 512 bits are used. The result is `-1` if the signature is valid, `0` otherwise.\nNotice that `CHKSIGNU` is equivalent to `ROT` `NEWC` `256 STU` `ENDC` `ROTREV` `CHKSIGNS`, i.e., to `CHKSIGNS` with the first argument `d` set to 256-bit _Slice_ containing `h`. Therefore, if `h` is computed as the hash of some data, these data are hashed _twice_, the second hashing occurring inside `CHKSIGNS`.
    CHKSIGNS,null,#F911,app_crypto,F911,CHKSIGNS,d s k - ?,26,Checks whether `s` is a valid Ed25519-signature of the data portion of _Slice_ `d` using public key `k`, similarly to `CHKSIGNU`. If the bit length of _Slice_ `d` is not divisible by eight, throws a cell underflow exception. The verification of Ed25519 signatures is the standard one, with `Sha` used to reduce `d` to the 256-bit number that is actually signed.
    CDATASIZEQ,null,#F940,app_misc,F940,CDATASIZEQ,c n - x y z -1 or 0,null,Recursively computes the count of distinct cells `x`, data bits `y`, and cell references `z` in the dag rooted at _Cell_ `c`, effectively returning the total storage used by this dag taking into account the identification of equal cells. The values of `x`, `y`, and `z` are computed by a depth-first traversal of this dag, with a hash table of visited cell hashes used to prevent visits of already-visited cells. The total count of visited cells `x` cannot exceed non-negative _Integer_ `n`; otherwise the computation is aborted before visiting the `(n+1)`-st cell and a zero is returned to indicate failure. If `c` is _Null_, returns `x=y=z=0`.
    CDATASIZE,null,#F941,app_misc,F941,CDATASIZE,c n - x y z,null,A non-quiet version of `CDATASIZEQ` that throws a cell overflow exception (8) on failure.
    SDATASIZEQ,null,#F942,app_misc,F942,SDATASIZEQ,s n - x y z -1 or 0,null,Similar to `CDATASIZEQ`, but accepting a _Slice_ `s` instead of a _Cell_. The returned value of `x` does not take into account the cell that contains the slice `s` itself; however, the data bits and the cell references of `s` are accounted for in `y` and `z`.
    SDATASIZE,null,#F943,app_misc,F943,SDATASIZE,s n - x y z,null,A non-quiet version of `SDATASIZEQ` that throws a cell overflow exception (8) on failure.
    LDGRAMS,null,#FA00,app_currency,FA00,LDGRAMS\nLDVARUINT16,s - x s',26,Loads (deserializes) a `Gram` or `VarUInteger 16` amount from _Slice_ `s`, and returns the amount as _Integer_ `x` along with the remainder `s'` of `s`. The expected serialization of `x` consists of a 4-bit unsigned big-endian integer `l`, followed by an `8l`-bit unsigned big-endian representation of `x`.\nThe net effect is approximately equivalent to `4 LDU` `SWAP` `3 LSHIFT#` `LDUX`.
    LDVARINT16,null,#FA01,app_currency,FA01,LDVARINT16,s - x s',26,Similar to `LDVARUINT16`, but loads a _signed_ _Integer_ `x`.\nApproximately equivalent to `4 LDU` `SWAP` `3 LSHIFT#` `LDIX`.
    STGRAMS,null,#FA02,app_currency,FA02,STGRAMS\nSTVARUINT16,b x - b',26,Stores (serializes) an _Integer_ `x` in the range `0...2^120-1` into _Builder_ `b`, and returns the resulting _Builder_ `b'`. The serialization of `x` consists of a 4-bit unsigned big-endian integer `l`, which is the smallest integer `l>=0`, such that `x<2^(8l)`, followed by an `8l`-bit unsigned big-endian representation of `x`. If `x` does not belong to the supported range, a range check exception is thrown.
    STVARINT16,null,#FA03,app_currency,FA03,STVARINT16,b x - b',26,Similar to `STVARUINT16`, but serializes a _signed_ _Integer_ `x` in the range `-2^119...2^119-1`.
    LDMSGADDR,null,#FA40,app_addr,FA40,LDMSGADDR,s - s' s'',26,Loads from _Slice_ `s` the only prefix that is a valid `MsgAddress`, and returns both this prefix `s'` and the remainder `s''` of `s` as slices.
    LDMSGADDRQ,null,#FA41,app_addr,FA41,LDMSGADDRQ,s - s' s'' -1 or s 0,26,A quiet version of `LDMSGADDR`: on success, pushes an extra `-1`; on failure, pushes the original `s` and a zero.
    PARSEMSGADDR,null,#FA42,app_addr,FA42,PARSEMSGADDR,s - t,26,Decomposes _Slice_ `s` containing a valid `MsgAddress` into a _Tuple_ `t` with separate fields of this `MsgAddress`. If `s` is not a valid `MsgAddress`, a cell deserialization exception is thrown.
    PARSEMSGADDRQ,null,#FA43,app_addr,FA43,PARSEMSGADDRQ,s - t -1 or 0,26,A quiet version of `PARSEMSGADDR`: returns a zero on error instead of throwing an exception.
    REWRITESTDADDR,null,#FA44,app_addr,FA44,REWRITESTDADDR,s - x y,26,Parses _Slice_ `s` containing a valid `MsgAddressInt` (usually a `msg_addr_std`), applies rewriting from the `anycast` (if present) to the same-length prefix of the address, and returns both the workchain `x` and the 256-bit address `y` as integers. If the address is not 256-bit, or if `s` is not a valid serialization of `MsgAddressInt`, throws a cell deserialization exception.
    REWRITESTDADDRQ,null,#FA45,app_addr,FA45,REWRITESTDADDRQ,s - x y -1 or 0,26,A quiet version of primitive `REWRITESTDADDR`.
    REWRITEVARADDR,null,#FA46,app_addr,FA46,REWRITEVARADDR,s - x s',26,A variant of `REWRITESTDADDR` that returns the (rewritten) address as a _Slice_ `s`, even if it is not exactly 256 bit long (represented by a `msg_addr_var`).
    REWRITEVARADDRQ,null,#FA47,app_addr,FA47,REWRITEVARADDRQ,s - x s' -1 or 0,26,A quiet version of primitive `REWRITEVARADDR`.
    SENDRAWMSG,null,#FB00,app_actions,FB00,SENDRAWMSG,c x - ,526,Sends a raw message contained in _Cell `c`_, which should contain a correctly serialized object `Message X`, with the only exception that the source address is allowed to have dummy value `addr_none` (to be automatically replaced with the current smart-contract address), and `ihr_fee`, `fwd_fee`, `created_lt` and `created_at` fields can have arbitrary values (to be rewritten with correct values during the action phase of the current transaction). Integer parameter `x` contains the flags. Currently `x=0` is used for ordinary messages; `x=128` is used for messages that are to carry all the remaining balance of the current smart contract (instead of the value originally indicated in the message); `x=64` is used for messages that carry all the remaining value of the inbound message in addition to the value initially indicated in the new message (if bit 0 is not set, the gas fees are deducted from this amount); `x'=x+1` means that the sender wants to pay transfer fees separately; `x'=x+2` means that any errors arising while processing this message during the action phase should be ignored. Finally, `x'=x+32` means that the current account must be destroyed if its resulting balance is zero. This flag is usually employed together with `+128`.
    RAWRESERVE,null,#FB02,app_actions,FB02,RAWRESERVE,x y - ,526,Creates an output action which would reserve exactly `x` nanograms (if `y=0`), at most `x` nanograms (if `y=2`), or all but `x` nanograms (if `y=1` or `y=3`), from the remaining balance of the account. It is roughly equivalent to creating an outbound message carrying `x` nanograms (or `b-x` nanograms, where `b` is the remaining balance) to oneself, so that the subsequent output actions would not be able to spend more money than the remainder. Bit `+2` in `y` means that the external action does not fail if the specified amount cannot be reserved; instead, all remaining balance is reserved. Bit `+8` in `y` means `x:=-x` before performing any further actions. Bit `+4` in `y` means that `x` is increased by the original balance of the current account (before the compute phase), including all extra currencies, before performing any other checks and actions. Currently `x` must be a non-negative integer, and `y` must be in the range `0...15`.
    RAWRESERVEX,null,#FB03,app_actions,FB03,RAWRESERVEX,x D y - ,526,Similar to `RAWRESERVE`, but also accepts a dictionary `D` (represented by a _Cell_ or _Null_) with extra currencies. In this way currencies other than Grams can be reserved.
    SETCODE,null,#FB04,app_actions,FB04,SETCODE,c - ,526,Creates an output action that would change this smart contract code to that given by _Cell_ `c`. Notice that this change will take effect only after the successful termination of the current run of the smart contract.
    SETLIBCODE,null,#FB06,app_actions,FB06,SETLIBCODE,c x - ,526,Creates an output action that would modify the collection of this smart contract libraries by adding or removing library with code given in _Cell_ `c`. If `x=0`, the library is actually removed if it was previously present in the collection (if not, this action does nothing). If `x=1`, the library is added as a private library, and if `x=2`, the library is added as a public library (and becomes available to all smart contracts if the current smart contract resides in the masterchain); if the library was present in the collection before, its public/private status is changed according to `x`. Also, `16` can be added to `x` to enable bounce transaction on failure. Values of `x` other than `0...2 (+16 possible)` are invalid.
    CHANGELIB,null,#FB07,app_actions,FB07,CHANGELIB,h x - ,526,Creates an output action similarly to `SETLIBCODE`, but instead of the library code accepts its hash as an unsigned 256-bit integer `h`. If `x!=0` and the library with hash `h` is absent from the library collection of this smart contract, this output action will fail.
    DEBUG,null,#FE nn:(#<= 239),debug,FEnn,{nn} DEBUG,-,26,`0 <= nn < 240`
    DEBUGSTR,null,#FEF n:(## 4) ssss:((n * 8 + 8) * Bit),debug,FEFnssss,{string} DEBUGSTR\n{string} {x} DEBUGSTRI,-,26,`0 <= n < 16`. Length of `ssss` is `n+1` bytes.\n`{string}` is a [string literal](https://github.com/Piterden/TON-docs/blob/master/Fift.%20A%20Brief%20Introduction.md#user-content-29-string-literals).\n`DEBUGSTR`: `ssss` is the given string.\n`DEBUGSTRI`: `ssss` is one-byte integer `0 <= x <= 255` followed by the given string.
    DUMPSTK,DEBUG,#FE00,debug,FE00,DUMPSTK,-,26,Dumps the stack (at most the top 255 values) and shows the total stack depth.
    DUMP,DEBUG,#FE2 i:uint4,debug,FE2i,s[i] DUMP,-,26,Dumps `s[i]`.
    SETCP,null,#FF nn:(#<= 239),codepage,FFnn,[nn] SETCP,-,26,Selects TVM codepage `0 <= nn < 240`. If the codepage is not supported, throws an invalid opcode exception.
    SETCP0,SETCP,#FF00,codepage,FF00,SETCP0,-,26,Selects TVM (test) codepage zero as described in this document.
    SETCP_SPECIAL,null,#FFF z:(## 4) {1 <= z},codepage,FFFz,[z-16] SETCP,-,26,Selects TVM codepage `z-16` for `1 <= z <= 15`. Negative codepages `-13...-1` are reserved for restricted versions of TVM needed to validate runs of TVM in other codepages. Negative codepage `-14` is reserved for experimental codepages, not necessarily compatible between different TVM implementations, and should be disabled in the production versions of TVM.
    SETCPX,null,#FFF0,codepage,FFF0,SETCPX,c - ,26,Selects codepage `c` with `-2^15 <= c < 2^15` passed in the top of the stack.
    MYCODE,null,#F8210,app_config,F8210,MYCODE,- c,26,Retrieves code of smart-contract from c7. Equivalent to `10 GETPARAM`.
    INCOMINGVALUE,null,#F8211,app_config,F8211,INCOMINGVALUE,- t,26,Retrieves value of incoming message from c7. Equivalent to `11 GETPARAM`.
    STORAGEFEES,null,#F8212,app_config,F8212,STORAGEFEES,- i,26,Retrieves value of storage phase fees from c7. Equivalent to `12 GETPARAM`.
    PREVBLOCKSINFOTUPLE,null,#F8213,app_config,F8213,PREVBLOCKSINFOTUPLE,- t,26,Retrives PrevBlocksInfo: `[last_mc_blocks, prev_key_block]` from c7. Equivalent to `13 GETPARAM`.
    PREVMCBLOCKS,null,#F83400,app_config,F83400,PREVMCBLOCKS,- t,34,Retrives `last_mc_blocks` part of PrevBlocksInfo from c7 (parameter 13).
    PREVKEYBLOCK,null,#F83401,app_config,F83401,PREVKEYBLOCK,- t,34,Retrives `prev_key_block` part of PrevBlocksInfo from c7 (parameter 13).
    GLOBALID,null,#F835,app_config,F835,GLOBALID,- i,26,Retrieves global_id from 19 network config.
    GASCONSUMED,null,#F807,app_gas,F807,GASCONSUMED,- g_c,26,Returns gas consumed by VM so far (including this instruction).
    MULADDDIVMOD,null,#A980,arithm_div,A980,MULADDDIVMOD,x y w z - q=floor((xy+w)/z) r=(xy+w)-zq,26,Performs multiplication, addition, division, and modulo in one step. Calculates q as floor((xy+w)/z) and r as (xy+w)-zq.
    MULADDDIVMODR,null,#A981,arithm_div,A981,MULADDDIVMODR,x y w z - q=round((xy+w)/z) r=(xy+w)-zq,26,Similar to MULADDDIVMOD but calculates q as round((xy+w)/z).
    MULADDDIVMODC,null,#A982,arithm_div,A982,MULADDDIVMODC,x y w z - q=ceil((xy+w)/z) r=(xy+w)-zq,26,Similar to MULADDDIVMOD but calculates q as ceil((xy+w)/z).
    ADDDIVMOD,null,#A900,arithm_div,A900,ADDDIVMOD,x w z - q=floor((x+w)/z) r=(x+w)-zq,26,Performs addition, division, and modulo in one step. Calculates q as floor((x+w)/z) and r as (x+w)-zq.
    ADDDIVMODR,null,#A901,arithm_div,A901,ADDDIVMODR,x w z - q=round((x+w)/z) r=(x+w)-zq,26,Similar to ADDDIVMOD but calculates q as round((x+w)/z).
    ADDDIVMODC,null,#A902,arithm_div,A902,ADDDIVMODC,x w y - q=ceil((x+w)/z) r=(x+w)-zq,26,Similar to ADDDIVMOD but calculates q as ceil((x+w)/z). Incorrect stack description in the provided data; assumed typo for 'z' instead of 'y' in the input stack.
    ADDRSHIFTMOD,null,#A920,arithm_div,A920,ADDRSHIFTMOD,x w z - q=floor((x+w)/2^z) r=(x+w)-q*2^z,26,Performs addition, right shift, and modulo in one step. Calculates q as floor((x+w)/2^z) and r as (x+w)-q*2^z.
    ADDRSHIFTMODR,null,#A921,arithm_div,A921,ADDRSHIFTMODR,x w z - q=round((x+w)/2^z) r=(x+w)-q*2^z,26,Similar to ADDRSHIFTMOD but calculates q as round((x+w)/2^z).
    ADDRSHIFTMODC,null,#A922,arithm_div,A922,ADDRSHIFTMODC,x w z - q=ceil((x+w)/2^z) r=(x+w)-q*2^z,26,Similar to ADDRSHIFTMOD but calculates q as ceil((x+w)/2^z).
    MULADDRSHIFTMOD,null,#A9A0,arithm_div,A9A0,MULADDRSHIFTMOD,x y w z - q=floor((xy+w)/2^z) r=(xy+w)-q*2^z,26,Combines multiplication, addition, right shift, and modulo. Calculates q as floor((xy+w)/2^z) and r as (xy+w)-q*2^z.
    MULADDRSHIFTRMOD,null,#A9A1,arithm_div,A9A1,MULADDRSHIFTRMOD,x y w z - q=round((xy+w)/2^z) r=(xy+w)-q*2^z,26,Similar to MULADDRSHIFTMOD but calculates q as round((xy+w)/2^z).
    MULADDRSHIFTCMOD,null,#A9A2,arithm_div,A9A2,MULADDRSHIFTCMOD,x y w z - q=ceil((xy+w)/2^z) r=(xy+w)-q*2^z,26,Similar to MULADDRSHIFTMOD but calculates q as ceil((xy+w)/2^z).
    LSHIFTADDDIVMOD,null,#A9D0 tt:uint8,arithm_div,A9D0tt,[tt+1] LSHIFT#ADDDIVMOD,x w z - q=floor((x*2^y+w)/z) r=(x*2^y+w)-zq,34,Performs left shift on x, adds w, then divides by z, rounding down for q and calculates remainder r.
    LSHIFTADDDIVMODR,null,#A9D1 tt:uint8,arithm_div,A9D1tt,[tt+1] LSHIFT#ADDDIVMODR,x w z - q=round((x*2^y+w)/z) r=(x*2^y+w)-zq,34,Similar to LSHIFTADDDIVMOD but rounds q to the nearest integer.
    LSHIFTADDDIVMODC,null,#A9D2 tt:uint8,arithm_div,A9D2tt,[tt+1] LSHIFT#ADDDIVMODC,x w z - q=ceil((x*2^y+w)/z) r=(x*2^y+w)-zq,34,Similar to LSHIFTADDDIVMOD but rounds q up to the nearest integer.
    HASHEXT_SHA256,null,#F90400,app_crypto,F90400,HASHEXT_SHA256,s_1 ... s_n n - h,1/33 gas per byte,Calculates and returns hash of the concatenation of slices (or builders) `s_1...s_n`.
    HASHEXT_SHA512,null,#F90401,app_crypto,F90401,HASHEXT_SHA512,s_1 ... s_n n - h,1/16 gas per byte,Calculates and returns hash of the concatenation of slices (or builders) `s_1...s_n`.
    HASHEXT_BLAKE2B,null,#F90402,app_crypto,F90402,HASHEXT_BLAKE2B,s_1 ... s_n n - h,1/19 gas per byte,Calculates and returns hash of the concatenation of slices (or builders) `s_1...s_n`.
    HASHEXT_KECCAK256,null,#F90403,app_crypto,F90403,HASHEXT_KECCAK256,s_1 ... s_n n - h,1/11 gas per byte,Calculates and returns hash of the concatenation of slices (or builders) `s_1...s_n`.
    HASHEXT_KECCAK512,null,#F90404,app_crypto,F90404,HASHEXT_KECCAK512,s_1 ... s_n n - h,1/19 gas per byte,Calculates and returns hash of the concatenation of slices (or builders) `s_1...s_n`.
    HASHEXTR_SHA256,null,#F90500,app_crypto,F90500,HASHEXTR_SHA256,s_n ... s_1 n - h,1/33 gas per byte,Same as `HASHEXT_`, but arguments are given in reverse order.
    HASHEXTR_SHA512,null,#F90501,app_crypto,F90501,HASHEXTR_SHA512,s_n ... s_1 n - h,1/16 gas per byte,Same as `HASHEXT_`, but arguments are given in reverse order.
    HASHEXTR_BLAKE2B,null,#F90502,app_crypto,F90502,HASHEXTR_BLAKE2B,s_n ... s_1 n - h,1/19 gas per byte,Same as `HASHEXT_`, but arguments are given in reverse order.
    HASHEXTR_KECCAK256,null,#F90503,app_crypto,F90503,HASHEXTR_SHA256,s_n ... s_1 n - h,1/11 gas per byte,Same as `HASHEXT_`, but arguments are given in reverse order.
    HASHEXTR_KECCAK512,null,#F90504,app_crypto,F90504,HASHEXTR_KECCAK512,s_n ... s_1 n - h,1/19 gas per byte,Same as `HASHEXT_`, but arguments are given in reverse order.
    HASHEXTA_SHA256,null,#F90600,app_crypto,F90600,HASHEXTA_SHA256,b s_1 ... s_n n - b',1/33 gas per byte,Appends the resulting hash to a builder `b` instead of pushing it to the stack.
    HASHEXTA_SHA512,null,#F90601,app_crypto,F90601,HASHEXTA_SHA512,b s_1 ... s_n n - b',1/16 gas per byte,Appends the resulting hash to a builder `b` instead of pushing it to the stack.
    HASHEXTA_BLAKE2B,null,#F90602,app_crypto,F90602,HASHEXTA_BLAKE2B,b s_1 ... s_n n - b',1/19 gas per byte,Appends the resulting hash to a builder `b` instead of pushing it to the stack.
    HASHEXTA_KECCAK256,null,#F90603,app_crypto,F90603,HASHEXTA_KECCAK256,b s_1 ... s_n n - b',1/11 gas per byte,Appends the resulting hash to a builder `b` instead of pushing it to the stack.
    HASHEXTA_KECCAK512,null,#F90604,app_crypto,F90604,HASHEXTA_KECCAK512,b s_1 ... s_n n - b',1/6 gas per byte,Appends the resulting hash to a builder `b` instead of pushing it to the stack.
    HASHEXTAR_SHA256,null,#F90700,app_crypto,F90700,HASHEXTAR_SHA256,b s_n ... s_1 n - b',1/33 gas per byte,Arguments are given in reverse order, appends hash to builder.
    HASHEXTAR_SHA512,null,#F90701,app_crypto,F90701,HASHEXTAR_SHA512,b s_n ... s_1 n - b',1/16 gas per byte,Arguments are given in reverse order, appends hash to builder.
    HASHEXTAR_BLAKE2B,null,#F90702,app_crypto,F90702,HASHEXTAR_BLAKE2B,b s_n ... s_1 n - b',1/19 gas per byte,Arguments are given in reverse order, appends hash to builder.
    HASHEXTAR_KECCAK256,null,#F90703,app_crypto,F90703,HASHEXTAR_KECCAK256,b s_n ... s_1 n - b',1/11 gas per byte,Arguments are given in reverse order, appends hash to builder.
    HASHEXTAR_KECCAK512,null,#F90704,app_crypto,F90704,HASHEXTAR_KECCAK512,b s_n ... s_1 n - b',1/6 gas per byte,Arguments are given in reverse order, appends hash to builder.
    ECRECOVER,null,#F912,app_crypto,F912,ECRECOVER,hash v r s - 0 or h x1 x2 -1,1526,Recovers public key from signature, identical to Bitcoin/Ethereum operations.
    P256_CHKSIGNS,null,#F915,app_crypto,F915,P256_CHKSIGNS,d sig k - ?,3526,Checks seck256r1-signature `sig` of data portion of slice `d` and public key `k`. Returns -1 on success, 0 on failure.
    P256_CHKSIGNU,null,#F914,app_crypto,F914,P256_CHKSIGNU,h sig k - ?,3526,Same as P256_CHKSIGNS, but the signed data is 32-byte encoding of 256-bit unsigned integer h.
    RIST255_FROMHASH,null,#F920,app_crypto,F920,RIST255_FROMHASH,h1 h2 - x,626,Deterministically generates a valid point `x` from a 512-bit hash (given as two 256-bit integers).
    RIST255_VALIDATE,null,#F921,app_crypto,F921,RIST255_VALIDATE,x - ,226,Checks that integer `x` is a valid representation of some curve point. Throws `range_chk` on error.
    RIST255_ADD,null,#F922,app_crypto,F922,RIST255_ADD,x y - x+y,626,Addition of two points on a curve.
    RIST255_SUB,null,#F923,app_crypto,F923,RIST255_SUB,x y - x-y,626,Subtraction of two points on curve.
    RIST255_MUL,null,#F924,app_crypto,F924,RIST255_MUL,x n - x*n,2026,Multiplies point `x` by a scalar `n`. Any `n` is valid, including negative.
    RIST255_MULBASE,null,#F925,app_crypto,F925,RIST255_MULBASE,n - g*n,776,Multiplies the generator point `g` by a scalar `n`. Any `n` is valid, including negative.
    RIST255_PUSHL,null,#F926,app_crypto,F926,RIST255_PUSHL,- l,26,Pushes integer `l=2^252+27742317777372353535851937790883648493`, which is the order of the group.
    RIST255_QVALIDATE,null,#B7F921,app_crypto,B7F921,RIST255_QVALIDATE,x - 0 or -1,234,Quiet version of `RIST255_VALIDATE`.
    RIST255_QADD,null,#B7F922,app_crypto,B7F922,RIST255_QADD,x y - 0 or x+y -1,634,Quiet version of `RIST255_ADD`.
    RIST255_QSUB,null,#B7F923,app_crypto,B7F923,RIST255_QSUB,x y - 0 or x-y -1,634,Quiet version of `RIST255_SUB`.
    RIST255_QMUL,null,#B7F924,app_crypto,B7F924,RIST255_QMUL,x n - 0 or x*n -1,2034,Quiet version of `RIST255_MUL`.
    RIST255_QMULBASE,null,#B7F925,app_crypto,B7F925,RIST255_QMULBASE,n - 0 or g*n -1,784,Quiet version of `RIST255_MULBASE`
    RUNVM,null,#DB4 flags:(## 12),cont_basic,DB4fff,RUNVM,x_1 ... x_n n code [r] [c4] [c7] [g_l] [g_m] - x'_1 ... x'_m exitcode [data'] [c4'] [c5] [g_c],66+x,Runs child VM with code `code` and stack `x_1...x_n`. Returns the resulting stack `x'_1...x'_m` and exitcode. Other arguments and return values are enabled by flags.
    RUNVMX,null,#DB50,cont_basic,DB50,RUNVMX,x_1 ... x_n n code [r] [c4] [c7] [g_l] [g_m] flags - x'_1 ... x'_m exitcode [data'] [c4'] [c5] [g_c],66+x,Same as `RUNVM`, but pops flags from stack.
    GETGASFEE,null,#F836,app_config,F836,GETGASFEE,gas_used is_mc - price,null,Calculates gas fee
    GETSTORAGEFEE,null,#F837,app_config,F837,GETSTORAGEFEE,cells bits seconds is_mc - price,null,Calculates storage fees in nanotons for contract based on current storage prices. `cells` and `bits` are the size of the [AccountState](https://github.com/ton-blockchain/ton/blob/8a9ff339927b22b72819c5125428b70c406da631/crypto/block/block.tlb#L247) (with deduplication, including root cell).
    GETFORWARDFEE,null,#F838,app_config,F838,GETFORWARDFEE,cells bits is_mc - price,null,Calculates forward fees in nanotons for outgoing message. `is_mc` is true if the source or the destination is in masterchain, false if both are in basechain. Note, cells and bits in Message should be counted with account for deduplication and root-is-not-counted rules.
    GETPRECOMPILEDGAS,null,#F839,app_config,F839,GETPRECOMPILEDGAS,- x,null,reserved, currently returns null. Will return cost of contract execution in gas units if this contract is precompiled
    GETORIGINALFWDFEE,null,#F83A,app_config,F83A,GETORIGINALFWDFEE,fwd_fee is_mc - orig_fwd_fee,null,calculate `fwd_fee * 2^16 / first_frac`. Can be used to get the original `fwd_fee` of the message (as replacement for hardcoded values like [this](https://github.com/ton-blockchain/token-contract/blob/21e7844fa6dbed34e0f4c70eb5f0824409640a30/ft/jetton-wallet.fc#L224C17-L224C46)) from `fwd_fee` parsed from incoming message. `is_mc` is true if the source or the destination is in masterchain, false if both are in basechain.
    GETGASFEESIMPLE,null,#F83B,app_config,F83B,GETGASFEESIMPLE,gas_used is_mc - price,null,Same as `GETGASFEE`, but without flat price (just `(gas_used * price) / 2^16)`.
    GETFORWARDFEESIMPLE,null,#F83C,app_config,F83C,GETFORWARDFEESIMPLE,cells bits is_mc - price,null,Calculates additional forward cost in nanotons for message that contains additional `cells` and `bits`. In other words, same as `GETFORWARDFEE`, but without lump price (just `(bits*bit_price + cells*cell_price) / 2^16)`.
    UNPACKEDCONFIGTUPLE,null,#F82E,app_config,F82E,UNPACKEDCONFIGTUPLE,- c,26,Retrieves tuple of configs slices from c7
    DUEPAYMENT,null,#F82F,app_config,F82F,DUEPAYMENT,- i,26,Retrieves value of due payment from c7
    GLOBALID,null,#F835,app_config,F835,GLOBALID,- i,26,Now retrieves `ConfigParam 19` from from c7, ton form config dict.
    SENDMSG,null,#FB08,app_config,FB08,SENDMSG,msg mode - i,null,Now retrieves `ConfigParam 24/25` (message prices) and `ConfigParam 43` (`max_msg_cells`) from c7, not from config dict.
    CLEVEL,null,#D766,cell_parse,D766,CLEVEL,cell - level,26,Returns level of the cell
    CLEVELMASK,null,#D767,cell_parse,D767,CLEVELMASK,cell - level_mask,26,Returns level mask of the cell
    CHASHIX,null,#D770,cell_parse,D770,CHASHIX,cell i - depth,26,Returns ith hash of the cell (i is in range 0..3)
    CDEPTHIX,null,#D771,cell_parse,D771,CDEPTHIX,cell i - depth,26,Returns ith depth of the cell (i is in range 0..3)
""".trimIndent()
