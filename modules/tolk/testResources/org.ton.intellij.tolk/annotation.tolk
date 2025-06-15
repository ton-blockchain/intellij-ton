import "tolk-stdlib/common.tolk"
fun run<TInputStruct>(input: TInputStruct, ans: slice) {
    repeat (2) {
        var s = input.toCell().beginParse();
        input = TInputStruct.fromSlice(s.assertEqDeeply(ans));
    }
    input.toCell().beginParse().skipAny<TInputStruct>().assertEnd();
}

struct (0x794) TransferParams1 {
    dest_int: address;
    amount: coins;
    dest_ext: address;
}
struct EitherLeft<T> {
    value: T
}
struct EitherRight<T> {
    value: T
}
type Either<L, R> = EitherLeft<L> | EitherRight<R>;
struct (0x9) TransferParams2 {
    intVector: (int32, coins?, uint64);
    needs_more: Cell<bool>;
}
type TransferParams = TransferParams1 | TransferParams2;
struct (0xFB3701FF) MsgTransfer {
    params: Either<TransferParams, Cell<TransferParams>>;
}

fun slice.appendRef(self, refSlice: slice): slice { return refSlice; }

fun main() {
    run<MsgTransfer>({
        params: EitherRight {
            value: {
                tvmCell: TransferParams1{
                    dest_int: address("EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c"), amount: 80000000, dest_ext: address("")
                }.toCell()
            }
        }
    }, "");
}
