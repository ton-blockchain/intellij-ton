package org.ton.intellij.func.psi

import org.intellij.lang.annotations.Language

object FuncStdlib {
    @Language("FunC")
    val text = """
        ;; Standard library for funC
        ;;

        {---
        # Tuple manipulation primitives
        The names and the types are mostly self-explaining.
        See [polymorhism with forall](https://ton.org/docs/#/func/functions?id=polymorphism-with-forall)
        for more info on the polymorphic functions.

        Note that currently values of atomic type `tuple` can't be cast to composite tuple type (e.g. `[int, cell]`)
        and vise versa.
        -}

        {---
        # Lisp-style lists

        Lists can be represented as nested 2-elements tuples.
        Empty list is conventionally represented as TVM `null` value (it can be obtained by calling [null()]).
        For example, tuple `(1, (2, (3, null)))` represents list `[1, 2, 3]`. Elements of a list can be of different types.
        -}

        ;;; Adds an element to the beginning of lisp-style list.
        forall X -> tuple cons(X head, tuple tail) asm "CONS";

        ;;; Extracts the head and the tail of lisp-style list.
        forall X -> (X, tuple) uncons(tuple list) asm "UNCONS";

        ;;; Extracts the tail and the head of lisp-style list. Can be used as (non-)modifying method.
        forall X -> (tuple, X) list_next(tuple list) asm( -> 1 0) "UNCONS";

        ;;; Returns the head of lisp-style list.
        forall X -> X car(tuple list) asm "CAR";

        ;;; Returns the tail of lisp-style list.
        tuple cdr(tuple list) asm "CDR";

        ;;; Creates 0-element tuple.
        tuple empty_tuple() asm "NIL";

        ;;; Appends a value `x` to a `Tuple t = (x1, ..., xn)`, but only if the resulting `Tuple t' = (x1, ..., xn, x)`
        ;;; is of length at most 255. Otherwise throws a type check exception.
        forall X -> tuple tpush(tuple t, X value) asm "TPUSH";

        ;;; Appends a value `x` to a `Tuple t = (x1, ..., xn)`, but only if the resulting `Tuple t' = (x1, ..., xn, x)`
        ;;; is of length at most 255. Otherwise throws a type check exception.
        forall X -> (tuple, ()) ~tpush(tuple t, X value) asm "TPUSH";

        ;;; Creates a singleton, i.e. a tuple of length one.
        forall X -> [X] single(X x) asm "SINGLE";

        ;;; Unpacks a singleton.
        forall X -> X unsingle([X] t) asm "UNSINGLE";

        ;;; Creates a pair.
        forall X, Y -> [X, Y] pair(X x, Y y) asm "PAIR";

        ;;; Unpacks a pair.
        forall X, Y -> (X, Y) unpair([X, Y] t) asm "UNPAIR";

        ;;; Creates a triple.
        forall X, Y, Z -> [X, Y, Z] triple(X x, Y y, Z z) asm "TRIPLE";

        ;;; Unpacks a triple.
        forall X, Y, Z -> (X, Y, Z) untriple([X, Y, Z] t) asm "UNTRIPLE";

        ;;; Creates 4-element tuple.
        forall X, Y, Z, W -> [X, Y, Z, W] tuple4(X x, Y y, Z z, W w) asm "4 TUPLE";

        ;;; Unpack 4-element tuple.
        forall X, Y, Z, W -> (X, Y, Z, W) untuple4([X, Y, Z, W] t) asm "4 UNTUPLE";

        ;;; Returns the first element of a tuple.
        forall X -> X first(tuple t) asm "FIRST";

        ;;; Returns the second element of a tuple.
        forall X -> X second(tuple t) asm "SECOND";

        ;;; Returns the third element of a tuple.
        forall X -> X third(tuple t) asm "THIRD";

        ;;; Returns the fourth element of a tuple.
        forall X -> X fourth(tuple t) asm "3 INDEX";

        ;;; Returns the first element of a pair.
        forall X, Y -> X pair_first([X, Y] p) asm "FIRST";

        ;;; Returns the second element of a pair.
        forall X, Y -> Y pair_second([X, Y] p) asm "SECOND";

        ;;; Returns the first element of a triple.
        forall X, Y, Z -> X triple_first([X, Y, Z] p) asm "FIRST";

        ;;; Returns the second element of a triple.
        forall X, Y, Z -> Y triple_second([X, Y, Z] p) asm "SECOND";

        ;;; Returns the third element of a triple.
        forall X, Y, Z -> Z triple_third([X, Y, Z] p) asm "THIRD";

        {---
        # Domain specific primitives
        Some useful information regarding smartcontract invocation can be found in c7 special register.
        Those primitives serve for convenient data extraction.
        -}

        ;;; Returns the current Unix time as an Integer
        int now() asm "NOW";

        ;;; Returns the internal address of the current smart contract as a Slice with a `MsgAddressInt`.
        ;;; If necessary, it can be parsed further using primitives such as [parse_std_addr].
        slice my_address() asm "MYADDR";

        ;;; Returns the remaining balance of the smart contract as a tuple consisting of an int
        ;;; (the remaining balance in nanotoncoins) and a `cell`
        ;;; (a dictionary with 32-bit keys representing the balance of “extra currencies”).
        ;;; Note that RAW primitives such as [send_raw_message] do not update this field.
        [int, cell] get_balance() asm "BALANCE";

        ;;; Returns the logical time of the current transaction.
        int cur_lt() asm "LTIME";

        ;;; Returns the starting logical time of the current block.
        int block_lt() asm "BLOCKLT";

        ;;; Returns the value of the global configuration parameter with integer index `i` as a `cell` or `null` value.
        cell config_param(int x) asm "CONFIGOPTPARAM";

        {---
        # Hashes
        -}

        ;;; Computes the representation hash of a `cell` [c] and returns it as a 256-bit unsigned integer `x`.
        ;;; Useful for signing and checking signatures of arbitrary entities represented by a tree of cells.
        int cell_hash(cell c) asm "HASHCU";

        ;;; Computes the hash of a `slice s` and returns it as a 256-bit unsigned integer `x`.
        ;;; The result is the same as if an ordinary cell containing only data and references from `s` had been created
        ;;; and its hash computed by [cell_hash].
        int slice_hash(slice s) asm "HASHSU";

        ;;; Computes sha256 of the data bits of `slice` [s]. If the bit length of `s` is not divisible by eight,
        ;;; throws a cell underflow exception. The hash value is returned as a 256-bit unsigned integer `x`.
        int string_hash(slice s) asm "SHA256U";

        {---
        # Signature checks
        -}

        ;;; Checks the Ed25519-`signature` of a `hash` (a 256-bit unsigned integer, usually computed as the hash of some data)
        ;;; using [public_key] (also represented by a 256-bit unsigned integer).
        ;;; The signature must contain at least 512 data bits; only the first 512 bits are used.
        ;;; The result is `−1` if the signature is valid, `0` otherwise.
        ;;; Note that `CHKSIGNU` creates a 256-bit slice with the hash and calls `CHKSIGNS`.
        ;;; That is, if [hash] is computed as the hash of some data, these data are hashed twice,
        ;;; the second hashing occurring inside `CHKSIGNS`.
        int check_signature(int hash, slice signature, int public_key) asm "CHKSIGNU";

        ;;; Checks whether [signature] is a valid Ed25519-signature of the data portion of `slice data` using `public_key`,
        ;;; similarly to [check_signature].
        ;;; If the bit length of [data] is not divisible by eight, throws a cell underflow exception.
        ;;; The verification of Ed25519 signatures is the standard one,
        ;;; with sha256 used to reduce [data] to the 256-bit number that is actually signed.
        int check_data_signature(slice data, slice signature, int public_key) asm "CHKSIGNS";

        {---
        # Computation of boc size
        The primitives below may be useful for computing storage fees of user-provided data.
        -}

        ;;; Returns `(x, y, z, -1)` or `(null, null, null, 0)`.
        ;;; Recursively computes the count of distinct cells `x`, data bits `y`, and cell references `z`
        ;;; in the DAG rooted at `cell` [c], effectively returning the total storage used by this DAG taking into account
        ;;; the identification of equal cells.
        ;;; The values of `x`, `y`, and `z` are computed by a depth-first traversal of this DAG,
        ;;; with a hash table of visited cell hashes used to prevent visits of already-visited cells.
        ;;; The total count of visited cells `x` cannot exceed non-negative [max_cells];
        ;;; otherwise the computation is aborted before visiting the `(max_cells + 1)`-st cell and
        ;;; a zero flag is returned to indicate failure. If [c] is `null`, returns `x = y = z = 0`.
        (int, int, int) compute_data_size(cell c, int max_cells) impure asm "CDATASIZE";

        ;;; Similar to [compute_data_size?], but accepting a `slice` [s] instead of a `cell`.
        ;;; The returned value of `x` does not take into account the cell that contains the `slice` [s] itself;
        ;;; however, the data bits and the cell references of [s] are accounted for in `y` and `z`.
        (int, int, int, int) slice_compute_data_size?(cell c, int max_cells) asm "SDATASIZEQ NULLSWAPIFNOT2 NULLSWAPIFNOT";

        ;;; A non-quiet version of [compute_data_size?] that throws a cell overflow exception (`8`) on failure.
        (int, int, int, int) compute_data_size?(cell c, int max_cells) asm "CDATASIZEQ NULLSWAPIFNOT2 NULLSWAPIFNOT";

        ;;; A non-quiet version of [slice_compute_data_size?] that throws a cell overflow exception (8) on failure.
        (int, int, int) slice_compute_data_size(slice s, int max_cells) impure asm "SDATASIZE";

        {---
        # Persistent storage save and load
        -}

        ;;; Returns the persistent contract storage cell. It can be parsed or modified with slice and builder primitives later.
        cell get_data() asm "c4 PUSH";

        ;;; Sets `cell` [c] as persistent contract data. You can update persistent contract storage with this primitive.
        () set_data(cell c) impure asm "c4 POP";

        {---
        # Continuation primitives
        -}

        ;;; Usually `c3` has a continuation initialized by the whole code of the contract. It is used for function calls.
        ;;; The primitive returns the current value of `c3`.
        cont get_c3() impure asm "c3 PUSH";

        ;;; Updates the current value of `c3`. Usually, it is used for updating smart contract code in run-time.
        ;;; Note that after execution of this primitive the current code
        ;;; (and the stack of recursive function calls) won't change,
        ;;; but any other function call will use a function from the new code.
        () set_c3(cont c) impure asm "c3 POP";

        ;;; Transforms a `slice` [s] into a simple ordinary continuation `c`, with `c.code = s` and an empty stack and savelist.
        cont bless(slice s) impure asm "BLESS";

        {---
        # Gas related primitives
        -}

        ;;; Sets current gas limit `gl` to its maximal allowed value `gm`, and resets the gas credit `gc` to zero,
        ;;; decreasing the value of `gr` by `gc` in the process.
        ;;; In other words, the current smart contract agrees to buy some gas to finish the current transaction.
        ;;; This action is required to process external messages, which bring no value (hence no gas) with themselves.
        ;;;
        ;;; For more details check [accept_message effects](https://ton.org/docs/#/smart-contracts/accept).
        () accept_message() impure asm "ACCEPT";

        ;;; Sets current gas limit `gl` to the minimum of limit and `gm`, and resets the gas credit `gc` to zero.
        ;;; If the gas consumed so far (including the present instruction) exceeds the resulting value of `gl`,
        ;;; an (unhandled) out of gas exception is thrown before setting new gas limits.
        ;;; Notice that [set_gas_limit] with an argument `limit ≥ 2^63 − 1` is equivalent to [accept_message].
        () set_gas_limit(int limit) impure asm "SETGASLIMIT";

        ;;; Commits the current state of registers `c4` (“persistent data”) and `c5` (“actions”)
        ;;; so that the current execution is considered “successful” with the saved values even if an exception is thrown later.
        () commit() impure asm "COMMIT";

        ;;; Computes the amount of gas that can be bought for `gram` nanotoncoins,
        ;;; and sets `gl` accordingly in the same way as [set_gas_limit].
        () buy_gas(int gram) impure asm "BUYGAS";

        {---
        # Actions primitives
        -}

        ;;; Creates an output action which would reserve exactly [amount] nanotoncoins (if [mode] = `0`),
        ;;; at most amount nanotoncoins (if [mode] = `2`),
        ;;; or all but amount nanotoncoins (if [mode] = `1` or [mode] = `3`),
        ;;; from the remaining balance of the account.
        ;;; It is roughly equivalent to creating an outbound message carrying [amount] nanotoncoins
        ;;; (or `b − amount` nanotoncoins, where `b` is the remaining balance) to oneself,
        ;;; so that the subsequent output actions would not be able to spend more money than the remainder.
        ;;;
        ;;; Bit +2 in [mode] means that the external action does not fail if the specified amount cannot be reserved;
        ;;; instead, all remaining balance is reserved.
        ;;;
        ;;; Bit +8 in [mode] means `amount <- -amount` before performing any further actions.
        ;;;
        ;;; Bit +4 in [mode] means that amount is increased by the original balance of the current account
        ;;; (before the compute phase), including all extra currencies, before performing any other checks and actions.
        ;;;
        ;;; Currently, [amount] must be a non-negative integer, and [mode] must be in the range `0..15`.
        () raw_reserve(int amount, int mode) impure asm "RAWRESERVE";

        ;;; Similar to [raw_reserve],
        ;;; but also accepts a dictionary [extra_amount] (represented by a `cell` or `null`) with extra currencies.
        ;;; In this way currencies other than TonCoin can be reserved.
        () raw_reserve_extra(int amount, cell extra_amount, int mode) impure asm "RAWRESERVEX";

        ;;; Sends a raw message contained in [msg], which should contain a correctly serialized object `Message X`,
        ;;; with the only exception that the source address is allowed to have dummy value `addr_none`
        ;;; (to be automatically replaced with the current smart contract address),
        ;;; and `ihr_fee`, `fwd_fee`, `created_lt` and `created_at` fields can have arbitrary values
        ;;; (to be rewritten with correct values during the action phase of the current transaction).
        ;;; Integer parameter [mode] contains the flags.
        ;;; Currently [mode] = `0` is used for ordinary messages;
        ;;; [mode] = `128` is used for messages that are to carry all the remaining balance of the current smart contract
        ;;; (instead of the value originally indicated in the message);
        ;;; [mode] = `64` is used for messages that carry all the remaining value of the inbound message in addition to
        ;;; the value initially indicated in the new message (if bit 0 is not set, the gas fees are deducted from this amount);
        ;;; `mode' = mode + 1` means that the sender wants to pay transfer fees separately;
        ;;; `mode' = mode + 2` means that any errors arising while processing this message during the action phase
        ;;; should be ignored.
        ;;; Finally, `mode' = mode + 32` means that the current account must be destroyed if its resulting balance is zero.
        ;;; This flag is usually employed together with `+128`.
        () send_raw_message(cell msg, int mode) impure asm "SENDRAWMSG";

        ;;; Creates an output action that would change this smart contract code to that given by cell [new_code].
        ;;; Notice that this change will take effect only after the successful termination of the current run
        ;;; of the smart contract (cf. [set_c3]).
        () set_code(cell new_code) impure asm "SETCODE";

        {---
        # Random number generator primitives
        The pseudo-random number generator uses the random seed, an unsigned 256-bit Integer,
        and (sometimes) other data kept in c7.
        The initial value of the random seed before a smart contract is executed in TON Blockchain
        is a hash of the smart contract address and the global block random seed.
        If there are several runs of the same smart contract inside a block,
        then all of these runs will have the same random seed.
        This can be fixed, for example, by running [randomize_lt] before using the pseudo-random number generator
        for the first time.
        -}

        ;;; Generates a new pseudo-random unsigned 256-bit integer `x`.
        ;;; The algorithm is as follows:
        ;;; if `r` is the old value of the random seed,
        ;;; considered as a 32-byte array (by constructing the big-endian representation of an unsigned 256-bit integer),
        ;;; then its `sha512(r)` is computed;
        ;;; the first 32 bytes of this hash are stored as the new value `r'` of the random seed,
        ;;; and the remaining 32 bytes are returned as the next random value `x`.
        int random() impure asm "RANDU256";

        ;;; Generates a new pseudo-random integer `z` in the range `0..range−1` (or `range..−1`, if `range < 0`).
        ;;; More precisely, an unsigned random value `x` is generated as in random;
        ;;; then` z := x * range / 2^256` is computed.
        int rand(int range) impure asm "RAND";

        ;;; Returns the current random seed as an unsigned 256-bit Integer.
        int get_seed() impure asm "RANDSEED";

        ;;; Sets the random seed to unsigned 256-bit seed.
        int set_seed() impure asm "SETRAND";

        ;;; Mixes unsigned 256-bit integer [x] into the random seed `r` by
        ;;; setting the random seed to sha256 of the concatenation of two 32-byte strings:
        ;;; the first with the big-endian representation of the old seed `r`,
        ;;; and the second with the big-endian representation of [x].
        () randomize(int x) impure asm "ADDRAND";

        ;;; Equivalent to `randomize(cur_lt());`.
        () randomize_lt() impure asm "LTIME ADDRAND";

        {---
        # Address manipulation primitives
        The address manipulation primitives listed below serialize and deserialize values according to the following TL-B scheme:
        ```TL-B
        addr_none${'$'}00 = MsgAddressExt;
        addr_extern${'$'}01 len:(## 8) external_address:(bits len)
                     = MsgAddressExt;
        anycast_info${'_'} depth:(#<= 30) { depth >= 1 }
          rewrite_pfx:(bits depth) = Anycast;
        addr_std${'$'}10 anycast:(Maybe Anycast)
          workchain_id:int8 address:bits256 = MsgAddressInt;
        addr_var${'$'}11 anycast:(Maybe Anycast) addr_len:(## 9)
          workchain_id:int32 address:(bits addr_len) = MsgAddressInt;
        _ _:MsgAddressInt = MsgAddress;
        _ _:MsgAddressExt = MsgAddress;

        int_msg_info${'$'}0 ihr_disabled:Bool bounce:Bool bounced:Bool
          src:MsgAddress dest:MsgAddressInt
          value:CurrencyCollection ihr_fee:Grams fwd_fee:Grams
          created_lt:uint64 created_at:uint32 = CommonMsgInfoRelaxed;
        ext_out_msg_info${'$'}11 src:MsgAddress dest:MsgAddressExt
          created_lt:uint64 created_at:uint32 = CommonMsgInfoRelaxed;
        ```
        A deserialized `MsgAddress` is represented by a tuple `t` as follows:

        - `addr_none` is represented by `t = (0)`,
          i.e., a tuple containing exactly one integer equal to zero.
        - `addr_extern` is represented by `t = (1, s)`,
          where slice `s` contains the field `external_address`. In other words, `
          t` is a pair (a tuple consisting of two entries), containing an integer equal to one and slice `s`.
        - `addr_std` is represented by `t = (2, u, x, s)`,
          where `u` is either a `null` (if `anycast` is absent) or a slice `s'` containing `rewrite_pfx` (if anycast is present).
          Next, integer `x` is the `workchain_id`, and slice `s` contains the address.
        - `addr_var` is represented by `t = (3, u, x, s)`,
          where `u`, `x`, and `s` have the same meaning as for `addr_std`.
        -}

        ;;; Loads from slice [s] the only prefix that is a valid `MsgAddress`,
        ;;; and returns both this prefix `s'` and the remainder `s''` of [s] as slices.
        (slice, slice) load_msg_addr(slice s) asm( -> 1 0) "LDMSGADDR";

        ;;; Decomposes slice [s] containing a valid `MsgAddress` into a `tuple t` with separate fields of this `MsgAddress`.
        ;;; If [s] is not a valid `MsgAddress`, a cell deserialization exception is thrown.
        tuple parse_addr(slice s) asm "PARSEMSGADDR";

        ;;; Parses slice [s] containing a valid `MsgAddressInt` (usually a `msg_addr_std`),
        ;;; applies rewriting from the anycast (if present) to the same-length prefix of the address,
        ;;; and returns both the workchain and the 256-bit address as integers.
        ;;; If the address is not 256-bit, or if [s] is not a valid serialization of `MsgAddressInt`,
        ;;; throws a cell deserialization exception.
        (int, int) parse_std_addr(slice s) asm "REWRITESTDADDR";

        ;;; A variant of [parse_std_addr] that returns the (rewritten) address as a slice [s],
        ;;; even if it is not exactly 256 bit long (represented by a `msg_addr_var`).
        (int, slice) parse_var_addr(slice s) asm "REWRITEVARADDR";

        {---
        # Debug primitives
        Currently only one function is available.
        -}

        ;;; Dumps the stack (at most the top 255 values) and shows the total stack depth.
        () dump_stack() impure asm "DUMPSTK";

        {---
        # Slice primitives

        It is said that a primitive _loads_ some data,
        if it returns the data and the remainder of the slice
        (so it can also be used as [modifying method](https://ton.org/docs/#/func/statements?id=modifying-methods)).

        It is said that a primitive _preloads_ some data, if it returns only the data
        (it can be used as [non-modifying method](https://ton.org/docs/#/func/statements?id=non-modifying-methods)).

        Unless otherwise stated, loading and preloading primitives read the data from a prefix of the slice.
        -}

        ;;; Converts a `cell` [c] into a `slice`. Notice that [c] must be either an ordinary cell,
        ;;; or an exotic cell (see [TVM.pdf](https://ton-blockchain.github.io/docs/tvm.pdf), 3.1.2)
        ;;; which is automatically loaded to yield an ordinary cell `c'`, converted into a `slice` afterwards.
        slice begin_parse(cell c) asm "CTOS";

        ;;; Checks if [s] is empty. If not, throws an exception.
        () end_parse(slice s) impure asm "ENDS";

        ;;; Loads the first reference from the slice.
        (slice, cell) load_ref(slice s) asm( -> 1 0) "LDREF";

        ;;; Preloads the first reference from the slice.
        cell preload_ref(slice s) asm "PLDREF";

        ;;; Loads a signed [len]-bit integer from a slice [s].
        (slice, int) ~load_int(slice s, int len) asm(s len -> 1 0) "LDIX";

        ;;; Loads an unsigned [len]-bit integer from a slice [s].
        (slice, int) ~load_uint(slice s, int len) asm( -> 1 0) "LDUX";

        ;;; Preloads a signed [len]-bit integer from a slice [s].
        int preload_int(slice s, int len) asm "PLDIX";

        ;;; Preloads an unsigned [len]-bit integer from a slice [s].
        int preload_uint(slice s, int len) asm "PLDUX";

        ;;; Loads the first `0 ≤ len ≤ 1023` bits from slice [s] into a separate `slice s''`.
        (slice, slice) load_bits(slice s, int len) asm(s len -> 1 0) "LDSLICEX";

        ;;; Preloads the first `0 ≤ len ≤ 1023` bits from slice [s] into a separate `slice s''`.
        slice preload_bits(slice s, int len) asm "PLDSLICEX";

        ;;; Loads serialized amount of TonCoins (any unsigned integer up to `2^128 - 1`).
        (slice, int) load_grams(slice s) asm( -> 1 0) "LDGRAMS";

        ;;; Returns all but the first `0 ≤ len ≤ 1023` bits of `slice` [s].
        slice skip_bits(slice s, int len) asm "SDSKIPFIRST";

        ;;; Returns all but the first `0 ≤ len ≤ 1023` bits of `slice` [s].
        (slice, ()) ~skip_bits(slice s, int len) asm "SDSKIPFIRST";

        ;;; Returns the first `0 ≤ len ≤ 1023` bits of `slice` [s].
        slice first_bits(slice s, int len) asm "SDCUTFIRST";

        ;;; Returns all but the last `0 ≤ len ≤ 1023` bits of `slice` [s].
        slice skip_last_bits(slice s, int len) asm "SDSKIPLAST";

        ;;; Returns all but the last `0 ≤ len ≤ 1023` bits of `slice` [s].
        (slice, ()) ~skip_last_bits(slice s, int len) asm "SDSKIPLAST";

        ;;; Returns the last `0 ≤ len ≤ 1023` bits of `slice` [s].
        slice slice_last(slice s, int len) asm "SDCUTLAST";

        ;;; Loads a dictionary `D` from `slice` [s].
        ;;; May be applied to dictionaries or to values of arbitrary `Maybe ^Y` types
        ;;; (returns `null` if `nothing` constructor is used).
        (slice, cell) load_dict(slice s) asm( -> 1 0) "LDDICT";

        ;;; Preloads a dictionary `D` from `slice` [s].
        cell preload_dict(slice s) asm "PLDDICT";

        ;;; Loads a dictionary as [load_dict], but returns only the remainder of the slice.
        slice skip_dict(slice s) asm "SKIPDICT";

        {---
        # Slice size primitives
        -}

        ;;; Returns the number of references in `slice` [s].
        int slice_refs(slice s) asm "SREFS";

        ;;; Returns the number of data bits in `slice` [s].
        int slice_bits(slice s) asm "SBITS";

        ;;; Returns both the number of data bits and the number of references in `slice` [s].
        (int, int) slice_bits_refs(slice s) asm "SBITREFS";

        ;;; Checks whether a `slice` [s] is empty (i.e., contains no bits of data and no cell references).
        int slice_empty?(slice s) asm "SEMPTY";

        ;;; Checks whether `slice` [s] has no bits of data.
        int slice_data_empty?(slice s) asm "SDEMPTY";

        ;;; Checks whether `slice` [s] has no references.
        int slice_refs_empty?(slice s) asm "SREMPTY";

        ;;; Returns the depth of `slice` [s].
        ;;; If [s] has no references, then returns `0`;
        ;;; otherwise the returned value is one plus the maximum of depths of cells referred to from [s].
        int slice_depth(slice s) asm "SDEPTH";

        {---
        # Builder primitives
        It is said that a primitive _stores_ a value `x` into a builder `b`
        if it returns a modified version of the builder `b'` with the value `x` stored at the end of it.
        It can be used as [non-modifying method](https://ton.org/docs/#/func/statements?id=non-modifying-methods).

        All the primitives below first check whether there is enough space in the `builder`,
        and only then check the range of the value being serialized.
        -}

        ;;; Creates a new empty `builder`.
        builder begin_cell() asm "NEWC";

        ;;; Converts a `builder` into an ordinary `cell`.
        cell end_cell(builder b) asm "ENDC";

        ;;; Stores a reference to `cell` [c] into `builder` [b].
        builder store_ref(builder b, cell c) asm(c b) "STREF";

        ;;; Stores an unsigned [len]-bit integer `x` into `b` for `0 ≤ len ≤ 256`.
        builder store_uint(builder b, int x, int len) asm(x b len) "STUX";

        ;;; Stores a signed [len]-bit integer `x` into `b` for` 0 ≤ len ≤ 257`.
        builder store_int(builder b, int x, int len) asm(x b len) "STIX";

        ;;; Stores `slice` [s] into `builder` [b]
        builder store_slice(builder b, slice s) asm "STSLICER";

        ;;; Stores (serializes) an integer [x] in the range `0..2^128 − 1` into `builder` [b].
        ;;; The serialization of [x] consists of a 4-bit unsigned big-endian integer `l`,
        ;;; which is the smallest integer `l ≥ 0`, such that `x < 2^8l`,
        ;;; followed by an `8l`-bit unsigned big-endian representation of [x].
        ;;; If [x] does not belong to the supported range, a range check exception is thrown.
        ;;;
        ;;; It is the usual way to store amounts of TonCoins.
        builder store_grams(builder b, int x) asm "STGRAMS";

        ;;; Stores dictionary `D` represented by `cell` [c] or `null` into `builder` [b].
        ;;; In other words, stores a `1`-bit and a reference to [c] if [c] is not `null` and `0`-bit otherwise.
        builder store_dict(builder b, cell c) asm(c b) "STDICT";

        ;;; Equivalent to [store_dict].
        builder store_maybe_ref(builder b, cell c) asm(c b) "STOPTREF";

        {---
        # Builder size primitives
        -}

        ;;; Returns the number of cell references already stored in `builder` [b].
        int builder_refs(builder b) asm "BREFS";

        ;;; Returns the number of data bits already stored in `builder` [b].
        int builder_bits(builder b) asm "BBITS";

        ;;; Returns the depth of `builder` [b].
        ;;; If no cell references are stored in [b], then returns 0;
        ;;; otherwise the returned value is one plus the maximum of depths of cells referred to from [b].
        int builder_depth(builder b) asm "BDEPTH";

        {---
        # Cell primitives
        -}

        ;;; Returns the depth of `cell` [c].
        ;;; If [c] has no references, then return `0`;
        ;;; otherwise the returned value is one plus the maximum of depths of cells referred to from [c].
        ;;; If [c] is a `null` instead of a cell, returns zero.
        int cell_depth(cell c) asm "CDEPTH";

        ;;; Checks whether `cell` [c] is a `null`.
        ;;; Usually a null-cell represents an empty dictionary.
        ;;; FunC also has polymorphic [null?] built-in, see [built-ins](https://ton.org/docs/#/func/builtins?id=other-primitives).
        int cell_null?(cell c) asm "ISNULL";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        cell udict_set(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTUSET";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        cell idict_set(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTISET";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        cell dict_set(cell dict, int key_len, slice index, slice value) asm(value index dict key_len) "DICTSET";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        (cell, ()) ~udict_set(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTUSET";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        (cell, ()) ~idict_set(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTISET";

        ;;; Sets the value associated with [key_len]-bit key index in dictionary [dict] to [value] (a slice),
        ;;; and returns the resulting dictionary.
        (cell, ()) ~dict_set(cell dict, int key_len, slice index, slice value) asm(value index dict key_len) "DICTSET";

        cell idict_set_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTISETREF";
        (cell, ()) ~idict_set_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTISETREF";
        cell udict_set_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTUSETREF";
        (cell, ()) ~udict_set_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTUSETREF";
        cell idict_get_ref(cell dict, int key_len, int index) asm(index dict key_len) "DICTIGETOPTREF";
        (cell, int) idict_get_ref?(cell dict, int key_len, int index) asm(index dict key_len) "DICTIGETREF";
        (cell, int) udict_get_ref?(cell dict, int key_len, int index) asm(index dict key_len) "DICTUGETREF";
        (cell, cell) idict_set_get_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTISETGETOPTREF";
        (cell, cell) udict_set_get_ref(cell dict, int key_len, int index, cell value) asm(value index dict key_len) "DICTUSETGETOPTREF";
        (cell, int) idict_delete?(cell dict, int key_len, int index) asm(index dict key_len) "DICTIDEL";
        (cell, int) udict_delete?(cell dict, int key_len, int index) asm(index dict key_len) "DICTUDEL";
        (slice, int) idict_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTIGET NULLSWAPIFNOT";
        (slice, int) udict_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTUGET NULLSWAPIFNOT";
        (cell, slice, int) idict_delete_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTIDELGET NULLSWAPIFNOT";
        (cell, slice, int) udict_delete_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTUDELGET NULLSWAPIFNOT";
        (cell, (slice, int)) ~idict_delete_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTIDELGET NULLSWAPIFNOT";
        (cell, (slice, int)) ~udict_delete_get?(cell dict, int key_len, int index) asm(index dict key_len) "DICTUDELGET NULLSWAPIFNOT";
        (cell, int) udict_add?(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTUADD";
        (cell, int) udict_replace?(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTUREPLACE";
        (cell, int) idict_add?(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTIADD";
        (cell, int) idict_replace?(cell dict, int key_len, int index, slice value) asm(value index dict key_len) "DICTIREPLACE";
        cell udict_set_builder(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTUSETB";
        (cell, ()) ~udict_set_builder(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTUSETB";
        cell idict_set_builder(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTISETB";
        (cell, ()) ~idict_set_builder(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTISETB";
        cell dict_set_builder(cell dict, int key_len, slice index, builder value) asm(value index dict key_len) "DICTSETB";
        (cell, ()) ~dict_set_builder(cell dict, int key_len, slice index, builder value) asm(value index dict key_len) "DICTSETB";
        (cell, int) udict_add_builder?(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTUADDB";
        (cell, int) udict_replace_builder?(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTUREPLACEB";
        (cell, int) idict_add_builder?(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTIADDB";
        (cell, int) idict_replace_builder?(cell dict, int key_len, int index, builder value) asm(value index dict key_len) "DICTIREPLACEB";
        (cell, int, slice, int) udict_delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTUREMMIN NULLSWAPIFNOT2";
        (cell, (int, slice, int)) ~udict::delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTUREMMIN NULLSWAPIFNOT2";
        (cell, int, slice, int) idict_delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTIREMMIN NULLSWAPIFNOT2";
        (cell, (int, slice, int)) ~idict::delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTIREMMIN NULLSWAPIFNOT2";
        (cell, slice, slice, int) dict_delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTREMMIN NULLSWAPIFNOT2";
        (cell, (slice, slice, int)) ~dict::delete_get_min(cell dict, int key_len) asm(-> 0 2 1 3) "DICTREMMIN NULLSWAPIFNOT2";
        (cell, int, slice, int) udict_delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTUREMMAX NULLSWAPIFNOT2";
        (cell, (int, slice, int)) ~udict::delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTUREMMAX NULLSWAPIFNOT2";
        (cell, int, slice, int) idict_delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTIREMMAX NULLSWAPIFNOT2";
        (cell, (int, slice, int)) ~idict::delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTIREMMAX NULLSWAPIFNOT2";
        (cell, slice, slice, int) dict_delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTREMMAX NULLSWAPIFNOT2";
        (cell, (slice, slice, int)) ~dict::delete_get_max(cell dict, int key_len) asm(-> 0 2 1 3) "DICTREMMAX NULLSWAPIFNOT2";
        (int, slice, int) udict_get_min?(cell dict, int key_len) asm (-> 1 0 2) "DICTUMIN NULLSWAPIFNOT2";
        (int, slice, int) udict_get_max?(cell dict, int key_len) asm (-> 1 0 2) "DICTUMAX NULLSWAPIFNOT2";
        (int, cell, int) udict_get_min_ref?(cell dict, int key_len) asm (-> 1 0 2) "DICTUMINREF NULLSWAPIFNOT2";
        (int, cell, int) udict_get_max_ref?(cell dict, int key_len) asm (-> 1 0 2) "DICTUMAXREF NULLSWAPIFNOT2";
        (int, slice, int) idict_get_min?(cell dict, int key_len) asm (-> 1 0 2) "DICTIMIN NULLSWAPIFNOT2";
        (int, slice, int) idict_get_max?(cell dict, int key_len) asm (-> 1 0 2) "DICTIMAX NULLSWAPIFNOT2";
        (int, cell, int) idict_get_min_ref?(cell dict, int key_len) asm (-> 1 0 2) "DICTIMINREF NULLSWAPIFNOT2";
        (int, cell, int) idict_get_max_ref?(cell dict, int key_len) asm (-> 1 0 2) "DICTIMAXREF NULLSWAPIFNOT2";
        (int, slice, int) udict_get_next?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTUGETNEXT NULLSWAPIFNOT2";
        (int, slice, int) udict_get_nexteq?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTUGETNEXTEQ NULLSWAPIFNOT2";
        (int, slice, int) udict_get_prev?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTUGETPREV NULLSWAPIFNOT2";
        (int, slice, int) udict_get_preveq?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTUGETPREVEQ NULLSWAPIFNOT2";
        (int, slice, int) idict_get_next?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTIGETNEXT NULLSWAPIFNOT2";
        (int, slice, int) idict_get_nexteq?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTIGETNEXTEQ NULLSWAPIFNOT2";
        (int, slice, int) idict_get_prev?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTIGETPREV NULLSWAPIFNOT2";
        (int, slice, int) idict_get_preveq?(cell dict, int key_len, int pivot) asm(pivot dict key_len -> 1 0 2) "DICTIGETPREVEQ NULLSWAPIFNOT2";

        ;;; Creates an empty dictionary, which is actually a `null` value. Special case of [null()].
        cell new_dict() asm "NEWDICT";

        ;;; Checks whether a dictionary is empty. Equivalent to [cell_null?].
        int dict_empty?(cell c) asm "DICTEMPTY";

        {---
        # Prefix dictionaries primitives
        TVM also support dictionaries with non-fixed length keys which form a prefix code
        (i.e. there is no key that is a prefix of another key).
        Learn more about them in [TVM.pdf](https://ton-blockchain.github.io/docs/tvm.pdf).
        -}

        ;;; Returns `(s', x, s'', -1)` or `(null, null, s, 0)`.
        ;;; Looks up the unique prefix of slice [key] present in the prefix code dictionary [dict].
        ;;; If found, the prefix of `s` is returned as `s'`, and the corresponding value (also a slice) as `x`.
        ;;; The remainder of `s` is returned as a slice `s''`.
        ;;; If no prefix of s is a key in prefix code dictionary [dict],
        ;;; returns the unchanged `s` and a zero flag to indicate failure.
        (slice, slice, slice, int) pfxdict_get?(cell dict, int key_len, slice key) asm(key dict key_len) "PFXDICTGETQ NULLSWAPIFNOT2";

        ;;; Similar to [dict_set], but may fail if the key is a prefix of another key presented in the dictionary.
        ;;; Returns flag, indicating success.
        (cell, int) pfxdict_set?(cell dict, int key_len, slice key, slice value) asm(value key dict key_len) "PFXDICTSET";

        ;;; Similar to [dict_delete?].
        (cell, int) pfxdict_delete?(cell dict, int key_len, slice key) asm(key dict key_len) "PFXDICTDEL";

        {---
        # Special primitives
        -}

        ;;; By the TVM type `Null` FunC represents absence of a value of some atomic type.
        ;;; So `null` can actually have any atomic type.
        forall X -> X null() asm "PUSHNULL";

        ;;; Checks whether [x] is a _Null_, and returns `-1` or `0` accordingly.
        forall X -> int null?(X x) asm "ISNULL";

        ;;; Mark a variable as used,
        ;;; such that the code which produced it won't be deleted even it is not impure.
        ;;; (c.f. [impure specifier](https://ton.org/docs/#/func/functions?id=impure-specifier))
        forall X -> (X, ()) ~impure_touch(X x) impure asm "NOP";

        ;;; Moves a variable [x] to the top of the stack
        forall X -> X touch(X x) impure;

        ;;; Moves a variable [x] to the top of the stack
        forall X -> (X, ()) ~touch(X x) impure;

        {---
        # Other primitives
        -}

        ;;; Computes the minimum of two integers [x] and [y].
        int min(int x, int y) asm "MIN";

        ;;; Computes the maximum of two integers [x] and [y].
        int max(int x, int y) asm "MAX";

        ;;; Sorts two integers.
        (int, int) minmax(int x, int y) asm "MINMAX";

        ;;; Computes the absolute value of an integer [x].
        int abs(int x) asm "ABS";

        ;;; `q=floor(x*y/z)`
        int muldiv(int x, int y, int z) asm "MULDIV";

        ;;; `q'=round(x*y/z)`
        int muldivr(int x, int y, int z) asm "MULDIVR";

        builder store_coins(builder b, int x) asm "STVARUINT16";
        (slice, int) load_coins(slice s) asm( -> 1 0) "LDVARUINT16";
        (slice, cell) load_maybe_ref(slice s) asm( -> 1 0) "LDOPTREF";
        cell preload_maybe_ref(slice s) asm "PLDOPTREF";
        () throw_if(int excno, int cond) impure asm "THROWARGIF";
        () throw_unless(int excno, int cond) impure asm "THROWARGIFNOT";
        () throw(int excno) impure asm "THROW";
    """.trimIndent()
}
