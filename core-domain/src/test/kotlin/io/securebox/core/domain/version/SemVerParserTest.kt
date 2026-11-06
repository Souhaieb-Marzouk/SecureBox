package io.securebox.core.domain.version

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SemVerParserTest {
    @Test
    fun parsesPlainAndPrefixedVersions() {
        assertEquals(SemVer(1, 2, 3), SemVerParser.parse("1.2.3"))
        assertEquals(SemVer(1, 2, 3), SemVerParser.parse("v1.2.3"))
        assertEquals(SemVer(2, 0, 0), SemVerParser.parse("2"))
        assertEquals(SemVer(2, 5, 0), SemVerParser.parse("2.5"))
    }

    @Test
    fun parsesBuildMetadataAsVersionCode() {
        assertEquals(21, SemVerParser.parse("v2.1.0+21")?.build)
        assertEquals(21, SemVerParser.versionCodeFromTag("v2.1.0+21"))
        assertEquals(1, SemVerParser.versionCodeFromTag("v0.1.0+1"))
        assertNull(SemVerParser.versionCodeFromTag("v0.1.0"))
    }

    @Test
    fun ignoresPrereleaseSuffix() {
        val parsed = SemVerParser.parse("v1.4.0-rc.1")
        assertEquals(SemVer(1, 4, 0), parsed)
    }

    @Test
    fun rejectsInputThatIsNotAVersion() {
        assertNull(SemVerParser.parse(null))
        assertNull(SemVerParser.parse(""))
        assertNull(SemVerParser.parse("latest"))
        assertNull(SemVerParser.parse("v1.2.3.4"))
        assertNull(SemVerParser.parse("1.2.3-beta!?"))
    }

    @Test
    fun comparesNumericallyNotLexicographically() {
        assertTrue(SemVer(1, 10, 0) > SemVer(1, 9, 9))
        assertTrue(SemVer(2, 0, 0, 5) > SemVer(2, 0, 0, 4))
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 99, 99))
        assertEquals(0, SemVer(1, 2, 3).compareTo(SemVer(1, 2, 3)))
    }
}
