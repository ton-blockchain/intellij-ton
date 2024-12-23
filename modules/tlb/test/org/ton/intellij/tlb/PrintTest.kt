package org.ton.intellij.tlb

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.intellij.lang.annotations.Language
import org.ton.intellij.tlb.psi.TlbConstructor
import org.ton.intellij.tlb.psi.tlbPsiFactory

class PrintTest : BasePlatformTestCase() {

    fun testTags() {
        checkTag("unit\$_ = Unit;", "#1853ad91")
        checkTag("true\$_ = True;", "#3fedd339")
        checkTag("bool_false\$0 = Bool;", "#e95dd78d")
        checkTag("bool_true\$1 = Bool;", "#b814e002")
        checkTag("bool_false\$0 = BoolFalse;", "#f3214771")
        checkTag("bool_true\$1 = BoolTrue;", "#b5311773")
        checkTag("nothing\$0 {X:Type} = Maybe X;", "#db8ec3e2")
        checkTag("just\$1 {X:Type} value:X = Maybe X;", "#45eec617")
        checkTag("left\$0 {X:Type} {Y:Type} value:X = Either X Y;", "#0a29cd5d")
        checkTag("right\$1 {X:Type} {Y:Type} value:Y = Either X Y;", "#df3ecb3b")
        checkTag("pair\$_ {X:Type} {Y:Type} first:X second:Y = Both X Y;", "#e03721eb")
        checkTag("bit\$_ (## 1) = Bit;", "#12acf7f6")
        checkTag("hm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) + l} node:(HashmapNode m X) = Hashmap n X;", "#2002a049")
        checkTag("hmn_leaf#_ {X:Type} value:X = HashmapNode 0 X;", "#87f847b7")
        checkTag("hmn_fork#_ {n:#} {X:Type} left:^(Hashmap n X) right:^(Hashmap n X) = HashmapNode (n + 1) X;", "#3b92e2d6")
        checkTag("hml_short\$0 {m:#} {n:#} len:(Unary ~n) {n <= m} s:(n * Bit) = HmLabel ~n m;", "#f23b17a1")
        checkTag("hml_long\$10 {m:#} n:(#<= m) s:(n * Bit) = HmLabel ~n m;", "#5d5ff77a")
        checkTag("hml_same\$11 {m:#} v:Bit n:(#<= m) = HmLabel ~n m;", "#1d8307c6")
        checkTag("unary_zero\$0 = Unary ~0;", "#aebc9392")
        checkTag("unary_succ\$1 {n:#} x:(Unary ~n) = Unary ~(n + 1);", "#c0e89234")
        checkTag("hme_empty\$0 {n:#} {X:Type} = HashmapE n X;", "#40b92161")
        checkTag("hme_root\$1 {n:#} {X:Type} root:^(Hashmap n X) = HashmapE n X;", "#1cc05be9")
        checkTag("ahm_edge#_ {n:#} {X:Type} {Y:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) + l} node:(HashmapAugNode m X Y) = HashmapAug n X Y;", "#f92ab7ac")
        checkTag("ahmn_leaf#_ {X:Type} {Y:Type} extra:Y value:X = HashmapAugNode 0 X Y;", "#c55ee841")
        checkTag("ahmn_fork#_ {n:#} {X:Type} {Y:Type} left:^(HashmapAug n X Y) right:^(HashmapAug n X Y) extra:Y = HashmapAugNode (n + 1) X Y;", "#86a053f1")
        checkTag("ahme_empty\$0 {n:#} {X:Type} {Y:Type} extra:Y = HashmapAugE n X Y;", "#af55dae6")
        checkTag("ahme_root\$1 {n:#} {X:Type} {Y:Type} root:^(HashmapAug n X Y) extra:Y = HashmapAugE n X Y;", "#e135d248")
        checkTag("vhm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) + l} node:(VarHashmapNode m X) = VarHashmap n X;", "#a6983b07")
        checkTag("vhmn_leaf\$00 {n:#} {X:Type} value:X = VarHashmapNode n X;", "#a0bd0298")
        checkTag("vhmn_fork\$01 {n:#} {X:Type} left:^(VarHashmap n X) right:^(VarHashmap n X) value:(Maybe X) = VarHashmapNode (n + 1) X;", "#0f725d4b")
        checkTag("vhmn_cont\$1 {n:#} {X:Type} branch:Bit child:^(VarHashmap n X) value:X = VarHashmapNode (n + 1) X;", "#4b086da5")
        checkTag("vhme_empty\$0 {n:#} {X:Type} = VarHashmapE n X;", "#98c3d231")
        checkTag("vhme_root\$1 {n:#} {X:Type} root:^(VarHashmap n X) = VarHashmapE n X;", "#76fc36fa")
        checkTag("phm_edge#_ {n:#} {X:Type} {l:#} {m:#} label:(HmLabel ~l n) {n = (~m) + l} node:(PfxHashmapNode m X) = PfxHashmap n X;", "#5eaa8af5")
        checkTag("phmn_leaf\$0 {n:#} {X:Type} value:X = PfxHashmapNode n X;", "#efd29692")
        checkTag("phmn_fork\$1 {n:#} {X:Type} left:^(PfxHashmap n X) right:^(PfxHashmap n X) = PfxHashmapNode (n + 1) X;", "#31cbadd5")
        checkTag("phme_empty\$0 {n:#} {X:Type} = PfxHashmapE n X;", "#99ecf592")
        checkTag("phme_root\$1 {n:#} {X:Type} root:^(PfxHashmap n X) = PfxHashmapE n X;", "#86026190")

        checkTag("addr_none\$00 = MsgAddressExt;", "#9ccb7139")
        checkTag("addr_extern\$01 len:(## 9) external_address:(bits len) = MsgAddressExt;", "#ee7b72a3")
        checkTag("anycast_info\$_ depth:(#<= 30) { depth >= 1 } rewrite_pfx:(bits depth) = Anycast;", "#9843f64b")
        checkTag("addr_std\$10 anycast:(Maybe Anycast) workchain_id:int8 address:bits256 = MsgAddressInt;", "#ca70d9f6")
        checkTag("addr_var\$11 anycast:(Maybe Anycast) addr_len:(## 9) workchain_id:int32 address:(bits addr_len) = MsgAddressInt;", "#9bb90082")
        checkTag("var_uint\$_ {n:#} len:(#< n) value:(uint (len * 8)) = VarUInteger n;", "#988e36b3")
        checkTag("var_int\$_ {n:#} len:(#< n) value:(int (len * 8)) = VarInteger n;", "#225aaee0")
        checkTag("nanograms\$_ amount:(VarUInteger 16) = Grams;", "#31468450")
        checkTag("extra_currencies\$_ dict:(HashmapE 32 (VarUInteger 32)) = ExtraCurrencyCollection;", "#99662f55")
        checkTag("currencies\$_ grams:Grams other:ExtraCurrencyCollection = CurrencyCollection;", "#54dfb0fb")
    }

    private fun checkTag(
        @Language("TLB")
        text: String,
        expected: String
    ) {
        val constructor = createConstructor(text)
        val tag = constructor.computeTag()
        assertEquals(expected, tag.toString())
    }

    private fun createConstructor(
        @Language("TLB")
        text: String
    ) = project.tlbPsiFactory.createFromText<TlbConstructor>(text)!!
}