package org.ton.intellij.acton.ide

import com.google.gson.Gson
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class ActonExternalLinterHighlightTest : BasePlatformTestCase() {
    fun testRelatedDeclarationIsInformationWhileMutationIsAnError() {
        val file = myFixture.configureByText("wallet.tolk", "val storage = 0;\nstorage += 1;")
        val result = ActonExternalLinterResult(
            """
                {
                  "diagnostics": [{
                    "file": ${Gson().toJson(file.virtualFile.path)},
                    "name": "compiler-error",
                    "severity": "error",
                    "message": "modifying immutable variable `storage`",
                    "annotations": [
                      {
                        "is_primary": true,
                        "range": {"start": {"line": 1, "character": 0}, "end": {"line": 1, "character": 7}}
                      },
                      {
                        "is_primary": false,
                        "message": "declared as `val` here; use `var` to allow modifications",
                        "range": {"start": {"line": 0, "character": 4}, "end": {"line": 0, "character": 11}}
                      }
                    ]
                  }]
                }
            """.trimIndent(),
            0,
        )
        val highlights = mutableListOf<HighlightInfo>()
        highlights.addHighlightsForFile(file, result)

        assertEquals(2, highlights.size)
        assertEquals(HighlightSeverity.ERROR, highlights.single { it.startOffset == 17 }.severity)
        val declaration = highlights.single { it.startOffset == 4 }
        assertEquals(HighlightSeverity.INFORMATION, declaration.severity)
        assertEquals("declared as `val` here; use `var` to allow modifications", declaration.description)
    }

    fun testRelatedLocationInAnotherFileRemainsInformational() {
        val file = myFixture.configureByText("types.tolk", "struct Record { value: int }")
        val result = ActonExternalLinterResult(
            """
                {
                  "diagnostics": [{
                    "file": ${Gson().toJson(file.virtualFile.path)},
                    "name": "compiler-error",
                    "severity": "info",
                    "message": "struct declared here",
                    "annotations": [{
                      "is_primary": true,
                      "range": {"start": {"line": 0, "character": 7}, "end": {"line": 0, "character": 13}}
                    }]
                  }]
                }
            """.trimIndent(),
            0,
        )
        val highlights = mutableListOf<HighlightInfo>()
        highlights.addHighlightsForFile(file, result)

        assertEquals(1, highlights.size)
        assertEquals(HighlightSeverity.INFORMATION, highlights.single().severity)
        assertEquals("struct declared here", highlights.single().description)
    }
}
