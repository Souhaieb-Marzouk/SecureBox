package io.securebox.core.domain.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsBlocklistTest {
    @Test
    fun builtInDomainAndSubdomainAreBlocked() {
        assertTrue(DnsBlocklist.isBlocked("doubleclick.net"))
        assertTrue(DnsBlocklist.isBlocked("ads.doubleclick.net"))
        assertFalse(DnsBlocklist.isBlocked("example.com"))
    }

    @Test
    fun signedFeedDomainAndSubdomainAreBlocked() {
        val feed = setOf("ads.example.com", "tracker.example.net")
        assertTrue(DnsBlocklist.isBlocked("ads.example.com", feed))
        assertTrue(DnsBlocklist.isBlocked("cdn.ads.example.com", feed))
        assertFalse(DnsBlocklist.isBlocked("example.com", feed))
    }

    @Test
    fun blankFeedEntriesAreIgnored() {
        assertFalse(DnsBlocklist.isBlocked("example.com", setOf("", "  ")))
    }
}
