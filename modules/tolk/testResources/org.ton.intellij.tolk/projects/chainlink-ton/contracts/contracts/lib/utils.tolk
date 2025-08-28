import "@stdlib/tvm-dicts.tolk";
import "@stdlib/lisp-lists"

const ERROR_WRONG_OP = 0xffff;
const ERROR_INVALID_DATA = 0x77;
const ERROR_BITMAP_OUT_OF_BOUNDS = 0x78;

/// TODO: is this enough? do we make this configurable?
///
/// The default message budget fee for messages (replies, notifications),
/// for which the value is not explicitly set, and we're not sending
/// the full excess value.
const DEFAULT_MESSAGE_BUDGET_FEE = ton("0.01");

/// Returns the global ID of the current blockchain (chain ID).
fun globalId(): int {
    // Notice: configParam(19)! will be available (non-null)
    return blockchain.configParam(19)!.beginParse().loadInt(32);
}

/// Computes the Keccak-256 hash of the given data.
@pure
fun keccak256(data: slice): uint256
    asm "1 PUSHINT HASHEXT_KECCAK256";

@inline
fun emit<T>(topic: int, body: T) {
    val emitMsg = createExternalLogMessage({
        dest: ExtOutLogBucket { topic },
        body
    });
    emitMsg.send(SEND_MODE_REGULAR);
}

// Iterates over arrays stored as snake data.
struct Iterator<T> {
    data: slice;
}

@inline @pure
fun Iterator<T>.new(data: cell): Iterator<T> {
    return Iterator<T> { data: data.beginParse() };
}

@inline @pure
fun Iterator<T>.empty(self): bool {
    return self.data.isEmpty()
}


@inline @pure
fun Iterator<T>.next(mutate self): T {
    var (bits, refs) = self.data.remainingBitsAndRefsCount();
    if (bits == 0) {
        // TODO: assert refs == 1
        // TODO: load vs preload?
        self.data = self.data.loadRef().beginParse();
    }
    return self.data.loadAny<T>();
}

// Expensive to calculate
@inline
fun Iterator<T>.countBytes(self): int {
    val maxCells = 128;
    val (cellsCount, dataBitsSize, cellRefs) = self.data.calculateSizeStrict(maxCells);
    assert(dataBitsSize % 8 == 0, ERROR_INVALID_DATA);
    return dataBitsSize / 8;
}

// For address snake data
@inline
fun countAddresses(data: cell): int {
    val maxCells = 128;
    val (cellsCount, dataBitsSize, cellRefs) = data.calculateSizeStrict(maxCells);
    assert(dataBitsSize % ADDR_KEY_LEN == 0, ERROR_INVALID_DATA);
    return dataBitsSize / ADDR_KEY_LEN;
}

@inline
fun Iterator<T>.has(mutate self, elem: T): bool {
    while (!self.empty()) {
        val current = self.next();
        if (current == elem) {
            return true;
        }
    }
    return false;
}

@inline
fun has<T>(element: T, tuple: tuple) {
    val size = tuple.size();
    var i = 0;
    while(i < size){
        if (tuple.get<T>(i) == element) {
            return true;
        }
    }
    return false;
}

@inline
fun Iterator<T>.size(self, elemSize: int): int {
    return self.countBytes() / elemSize
}

// https://github.com/ton-org/ton-core/blob/213fed91d6f63d978c27588509e15ab53c8bafdb/src/dict/Dictionary.ts#L425
const ADDR_KEY_LEN = 267;

// NOTE: this assumes the slice keys are address values
struct Map<V> {
    value: dict;
}

@inline @pure
fun Map<V>.new(): Map<V> {
    return Map { value: createEmptyDict() }
}

@inline @pure
fun Map<V>.isEmpty(self): bool {
    return self.value.dictIsEmpty();
}

@inline @pure
fun Map<V>.get(self, key: address): (V?, bool) {
    val (value, exists) = self.value.sDictGet(ADDR_KEY_LEN, key as slice);
    if (!exists) {
        return (null, false);
    }
    return (V.fromSlice(value!), true);
}

@inline
fun Map<V>.mustGet(self, key: address, errorCode: int): V {
    val (value, exists) = self.value.sDictGet(ADDR_KEY_LEN, key as slice);
    assert(exists, errorCode);
    return V.fromSlice(value!);
}

@inline @pure
fun Map<V>.set(mutate self, key: address, value: V): void {
    return self.value.sDictSet(ADDR_KEY_LEN, key as slice, value.toCell().beginParse());
}

@inline @pure
fun Map<V>.has(self, key: address): bool {
    val (_, exists) = self.value.sDictGet(ADDR_KEY_LEN, key as slice);
    return exists;
}

//Returns a lisp-list with the keys of the map, destroys the dictionary in the process
@inline
fun Map<V>.keysDestructs(mutate self): tuple? {  
    var list: tuple? = null;
    do {
        var (key: slice?, _value: slice?, found: bool) = self.value.sDictDeleteFirstAndGet(ADDR_KEY_LEN);
        if (found) {
            var address = key! as address;
            list = listPrepend(address, list);
        }
    } while (found);
    return list;
}

// Notice: currently not available in tolk 1.0/stdlib
// Already available
// @pure
// fun dict.sDictGetNext(self, keyLen: int, pivot: slice): (slice?, slice?, bool)
//     asm(pivot self keyLen -> 1 0 2) "DICTGETNEXT" "NULLSWAPIFNOT2";

@inline @pure
fun Map<V>.min(self): (address?, V?, bool) {
    val (key, value, exists) = self.value.sDictGetFirst(ADDR_KEY_LEN);
    if (!exists) {
        return (null, null, false);
    }
    return (key as address?, V.fromSlice(value!), true);
}

@inline @pure
fun Map<V>.next(self, key: address): (address?, V?, bool) {
    val (nextKey, value, exists) = self.value.sDictGetNext(ADDR_KEY_LEN, key as slice);
    if (!exists) {
        return (null, null, false);
    }
    return (nextKey as address?, V.fromSlice(value!), true);
}

@inline @pure
fun Map<V>.delete(mutate self, key: address): bool {
    return self.value.sDictDelete(ADDR_KEY_LEN, key as slice);
}

// TODO: mark functions as @pure
struct UMap<K, V> {
    value: dict;
    keyLen: uint16; // TODO: get rid of keylen to save storage space, wish we could us a type param
}

@inline
fun UMap<K, V>.new(keyLen: int): UMap<K, V> {
    return UMap { value: createEmptyDict(), keyLen }
}

@inline
fun UMap<K, V>.isEmpty(self): bool {
    return self.value.dictIsEmpty();
}

@inline
fun UMap<K, V>.has(self, key: K): bool {
    val (_, exists) = self.value.uDictGet(self.keyLen, key);
    return exists;
}

@inline
fun UMap<K, V>.get(self, key: K): (V?, bool) {
    val (value, exists) = self.value.uDictGet(self.keyLen, key);
    if (!exists) {
        return (null, false);
    }
    return (V.fromSlice(value!), true);
}

@inline
fun UMap<K, V>.mustGet(self, key: K, errorCode: int): V {
    val (maybeValue, exists) = self.value.uDictGet(self.keyLen, key);
    assert(exists, errorCode);
    return V.fromSlice(maybeValue!);
}

@inline
fun UMap<K, V>.set(mutate self, key: K, value: V): void {
    return (self.value.uDictSet(self.keyLen, key, value.toCell().beginParse()));
}

@inline
fun UMap<K, V>.getRef(self, key: K): (V?, bool) {
    val (value, exists) = self.value.uDictGetRef(self.keyLen, key);
    if (!exists) {
        return (null, false);
    }
    return (V.fromCell(value!), true);
}

@inline
fun UMap<K, V>.mustGetRef(self, key: K, errorCode: int): V {
    val (maybeValue, exists) = self.value.uDictGetRef(self.keyLen, key);
    assert(exists, errorCode);
    return V.fromCell(maybeValue!);
}

@inline
fun UMap<K, V>.setRef(mutate self, key: K, value: V): void {
    return (self.value.uDictSetRef(self.keyLen, key, value.toCell()));
}

@inline @pure
fun UMap<K, V>.min(self): (K?, V?, bool) {
    val (key, value, exists) = self.value.uDictGetFirst(ADDR_KEY_LEN);
    if (!exists) {
        return (null, null, false);
    }
    return (key, V.fromSlice(value!), true);
}

@inline @pure
fun UMap<K, V>.next(self, key: K): (K?, V?, bool) {
    val (nextKey, value, exists) = self.value.uDictGetNext(self.keyLen, key);
    if (!exists) {
        return (null, null, false);
    }
    return (nextKey, V.fromSlice(value!), true);
}

@inline @pure
fun UMap<K, V>.replace(mutate self, key: K, value: V): bool {
    return (self.value.uDictSetIfExists(self.keyLen, key, value.toCell().beginParse()));
}

@inline @pure
fun UMap<K, V>.delete(mutate self, key: K): bool {
    return self.value.sDictDelete(self.keyLen, key);
}

//Returns a lisp-list with the keys of the map, destroys the dictionary in the process
@inline
fun UMap<K, V>.keysDestructs(mutate self): tuple? {
    var list: tuple? = null;
    do {
        var (key: int?, _value: slice?, found: bool) = self.value.uDictDeleteFirstAndGet(self.keyLen);
        if (found) {
            var value = key! as K;
            list = listPrepend(value, list);
        }
    } while (found);
    return list;
}


struct TupleIterator<T> {
    pos: int = 0;
    len: int; // TLEN is 26 gas, we cache the value for minimal savings
    data: tuple;
}

@inline @pure
fun TupleIterator<T>.new(data: tuple) {
    return TupleIterator<T> { data, len: data.size() };
}

@inline @pure
fun TupleIterator<T>.empty(self): bool {
    // TODO: should we cache data.size()?
    return self.pos >= self.len
}


@inline @pure
fun TupleIterator<T>.next(mutate self): T {
    var offset = self.pos;
    self.pos += 1;
    return self.data.get<T>(offset);
}

@inline @pure
fun TupleIterator<T>.size(self, elemSize: int): int {
    // elemSize is ignored since we know exact size,
    // param is there for interface compatibility
    return self.len;
}

struct Bitmap256 {
    bits: uint256;
}

@inline
fun Bitmap256.new(): Bitmap256 {
    return Bitmap256 { bits: 0 };
}

@inline
fun Bitmap256.set(mutate self, index: int): void {
    assert(index >= 0 && index < 256, ERROR_BITMAP_OUT_OF_BOUNDS);
    self.bits = self.bits | (1 << index);
}

@inline
fun Bitmap256.has(self, index: int): bool {
    assert(index >= 0 && index < 256, ERROR_BITMAP_OUT_OF_BOUNDS);
    return (self.bits & (1 << index)) != 0;
}
