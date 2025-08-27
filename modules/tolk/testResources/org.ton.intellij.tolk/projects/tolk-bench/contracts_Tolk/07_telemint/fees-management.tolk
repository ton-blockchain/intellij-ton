
const MIN_TONS_FOR_STORAGE = ton("0.03")
const MINTING_PRICE_UPPER_BOUND = ton("0.03")

const MY_WORKCHAIN = 0

// do NOT use this is practice — use `in.originalForwardFee` in `onInternalMessage`;
// here it's used to exactly match (slightly incorrect) original FunC implementation
@deprecated
fun calculateFwdFeeDeprecated() {
    return mulDivFloor(getFwdFeeFromOriginalMsgCell(), 3, 2)
}

@deprecated
fun getFwdFeeFromOriginalMsgCell(): coins
    asm "INMSG_FWDFEE"
