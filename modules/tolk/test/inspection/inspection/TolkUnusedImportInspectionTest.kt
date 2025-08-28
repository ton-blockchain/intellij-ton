package org.ton.intellij.tolk.inspection.inspection

import org.ton.intellij.tolk.inspection.TolkInspectionTestBase
import org.ton.intellij.tolk.inspection.TolkUnusedImportInspection

class TolkUnusedImportInspectionTest : TolkInspectionTestBase() {
    fun `test unused stdlib import`() {
        doInspectionTest(
            """
                <warning descr="Unused import '@stdlib/tvm-dicts'">import "@stdlib/tvm-dicts"</warning>
                
                fun main() {}
            """.trimIndent(),
            TolkUnusedImportInspection(),
            checkWarnings = true,
        )
    }

    fun `test used stdlib import`() {
        doInspectionTest(
            """
                import "@stdlib/tvm-dicts"
                
                fun main() {
                    createEmptyDict();
                }
            """.trimIndent(),
            TolkUnusedImportInspection(),
            checkWarnings = true,
        )
    }

    fun `test used stdlib import via method call`() {
        doInspectionTest(
            """
                import "@stdlib/tvm-dicts"
                
                fun main() {
                    val d: dict;
                    d.dictIsEmpty();
                }
            """.trimIndent(),
            TolkUnusedImportInspection(),
            checkWarnings = true,
        )
    }
}
