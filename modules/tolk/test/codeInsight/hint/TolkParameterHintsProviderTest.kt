package org.ton.intellij.tolk.codeInsight.hint

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TolkParameterHintsProviderTest {
    @Test
    fun `test suppress expect value hint`() {
        assertTrue(isSuppressedParameterHint("expect", "value"))
    }

    @Test
    fun `test suppress send sendMode hint`() {
        assertTrue(isSuppressedParameterHint("send", "sendMode"))
    }

    @Test
    fun `test keep other send parameter hints`() {
        assertFalse(isSuppressedParameterHint("send", "payload"))
        assertFalse(isSuppressedParameterHint("send", "sender"))
    }

    @Test
    fun `test keep sendMode hints for other functions`() {
        assertFalse(isSuppressedParameterHint("sendAndEstimateFee", "sendMode"))
        assertFalse(isSuppressedParameterHint(null, "sendMode"))
    }
}
