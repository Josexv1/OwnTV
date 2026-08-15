package tv.own.owntv.features.setup

/**
 * Free public sample playlist used only by debug builds so local emulator/TV testing can skip
 * typing a source. Not packaged into release UX — OwnTV ships without content.
 *
 * Source: iptv-org country list (public streams; availability varies by region/network).
 */
internal object DemoCatalog {
    const val PROFILE_NAME = "Demo"
    const val SOURCE_NAME = "Free sample (iptv-org NL)"
    const val M3U_URL = "https://iptv-org.github.io/iptv/countries/nl.m3u"
}
