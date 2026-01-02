package org.ton.intellij.tolk.codeInsight

import org.ton.intellij.tolk.ide.configurable.tolkSettings
import org.ton.intellij.tolk.inspection.TolkCallArgumentsCountMismatchInspection
import org.ton.intellij.tolk.inspection.TolkExpectTypeBuiltinInspection
import org.ton.intellij.tolk.inspection.TolkStructInitializationInspection
import org.ton.intellij.tolk.inspection.TolkUnresolvedReferenceInspection

class TolkCodeInsightTest : TolkCodeInsightBaseTest() {
    override fun getTestDataPath(): String = "${super.testDataPath}/tests"

    override fun setUp() {
        super.setUp()
        val file = myFixture.copyDirectoryToProject("../tolk-stdlib", "tolk-stdlib")
        project.tolkSettings.stdlibPath = file.url
        myFixture.copyDirectoryToProject("imports", "imports")
        myFixture.enableInspections(TolkCallArgumentsCountMismatchInspection::class.java)
        myFixture.enableInspections(TolkExpectTypeBuiltinInspection::class.java)
        myFixture.enableInspections(TolkStructInitializationInspection::class.java)
        myFixture.enableInspections(TolkUnresolvedReferenceInspection::class.java)
    }

    fun `test a-tests`() = doTest()
    fun `test allow-post-modification`() = doTest()
    fun `test annotations-tests`() = doTest()
    fun `test asm-arg-order`() = doTest()
    fun `test assignment-tests`() = doTest()
    fun `test bit-operators`() = doTest()
    fun `test build-addr-tests`() = doTest()
    fun `test bytesN-tests`() = doTest()
    fun `test calls-tests`() = doTest()
    fun `test cells-slices`() = doTest()
    fun `test codegen-check-demo`() = doTest()
    fun `test comments`() = doTest()
    fun `test comments-tests`() = doTest()
    fun `test constants-tests`() = doTest()
    fun `test dicts-demo`() = doTest()
    fun `test generics-1`() = doTest()
    fun `test generics-2`() = doTest() // todo: fix test4
    fun `test generics-3`() = doTest()
    fun `test generics-4`() = doTest()
    fun `test handle-msg-1`() = doTest()
    fun `test handle-msg-2`() = doTest()
    fun `test handle-msg-3`() = doTest()
    fun `test handle-msg-4`() = doTest()
    fun `test handle-msg-5`() = doTest()
    fun `test handle-msg-6`() = doTest()
    fun `test handle-msg-7`() = doTest()
    fun `test if-else-tests`() = doTest()
    fun `test imports-tests`() = doTest()
    fun `test indexed-access`() = doTest()
    fun `test inference-tests`() = doTest()
    fun `test inline-tests`() = doTest()
    fun `test intN-tests`() = doTest()
    fun `test lazy-algo-tests`() = doTest()
    fun `test lazy-load-tests`() = doTest()
    fun `test logical-operators`() = doTest()
    fun `test match-by-expr-tests`() = doTest()
    fun `test meaningful-1`() = doTest()
    fun `test methods-tests`() = doTest()
    fun `test mutate-methods`() = doTest()
    fun `test never-type-tests`() = doTest()
    fun `test no-spaces`() = doTest()
    fun `test null-keyword`() = doTest()
    fun `test nullable-tensors`() = doTest()
    fun `test nullable-types`() = doTest()
    fun `test numbers-tests`() = doTest()
    fun `test op-priority`() = doTest()
    fun `test pack-unpack-1`() = doTest()
    fun `test pack-unpack-2`() = doTest() // todo: fix test_IntAndMaybeMaybe8
    fun `test pack-unpack-3`() = doTest() // todo: fix test_MsgTransfer
    fun `test pack-unpack-4`() = doTest()
    fun `test pack-unpack-5`() = doTest()
    fun `test pack-unpack-6`() = doTest()
    fun `test pack-unpack-7`() = doTest()
    fun `test parse-address`() = doTest()
    fun `test pure-functions`() = doTest()
    fun `test remove-unused-functions`() = doTest()
    fun `test self-keyword`() = doTest()
    fun `test send-msg-1`() = doTest()
    fun `test send-msg-2`() = doTest()
    fun `test send-msg-3`() = doTest()
    fun `test smart-cast-tests`() = doTest()
    fun `test some-tests-1`() = doTest()
    fun `test some-tests-2`() = doTest()
    fun `test some-tests-3`() = doTest()
    fun `test special-fun-names`() = doTest()
    fun `test strings-tests`() = doTest()
    fun `test struct-tests`() = doTest()
    fun `test ternary-tests`() = doTest()
    fun `test test-math`() = doTest()
    fun `test try-catch-tests`() = doTest()
    fun `test type-aliases-tests`() = doTest()
    fun `test unbalanced-ret`() = doTest()
    fun `test unbalanced-ret-loops`() = doTest()
    fun `test union-types-tests`() = doTest()
    fun `test unreachable-5`() = doTest()
    fun `test use-before-declare`() = doTest()
    fun `test var-apply-tests`() = doTest()

    fun `test enums-tests`() = doTest()
    fun `test never-return-functions`() = doTest()
    fun `test maps-tests`() = doTest()
    fun `test overloads-tests`() = doTest() // TODO: uncomment some tests
    fun `test pack-unpack-8`() = doTest()
    fun `test send-msg-4`() = doTest()
    fun `test some-tests-4`() = doTest()
}
