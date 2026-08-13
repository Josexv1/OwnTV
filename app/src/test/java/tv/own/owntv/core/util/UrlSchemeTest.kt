package tv.own.owntv.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlSchemeTest {
    @Test
    fun `uppercase scheme is lowercased, host path and query preserved`() {
        assertEquals("http://Host/Path?q=A", normalizeUrlScheme("HTTP://Host/Path?q=A"))
    }

    @Test
    fun `mixed-case scheme is lowercased`() {
        assertEquals("https://x", normalizeUrlScheme("hTtPs://x"))
    }

    @Test
    fun `already-lowercase scheme is unchanged`() {
        assertEquals("http://x", normalizeUrlScheme("http://x"))
    }

    @Test
    fun `string with no scheme separator is unchanged`() {
        assertEquals("not a url", normalizeUrlScheme("not a url"))
    }

    @Test
    fun `only the scheme is lowercased, host case is preserved`() {
        assertEquals("rtsp://X", normalizeUrlScheme("rtsp://X"))
    }

    @Test
    fun `empty string is unchanged`() {
        assertEquals("", normalizeUrlScheme(""))
    }
}
