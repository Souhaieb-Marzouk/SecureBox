package io.securebox.core.domain.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafeBrowsingHashTest {
    @Test
    fun prefixIsShortDeterministicHex() {
        val first = SafeBrowsingHash.prefix("https://example.com/")
        val second = SafeBrowsingHash.prefix("https://example.com/")
        assertEquals(8, first.length)
        assertEquals(first, second)
        assertTrue(first.all { it in "0123456789abcdef" })
    }

    @Test
    fun differentUrlsUsuallyHaveDifferentPrefixes() {
        assertNotEquals(
            SafeBrowsingHash.prefix("https://example.com/a"),
            SafeBrowsingHash.prefix("https://example.com/b"),
        )
    }
}
