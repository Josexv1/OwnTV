package tv.own.owntv.core.util

/**
 * Lowercases ONLY the URL scheme — the substring before the first `"://"` — leaving host, path,
 * query and fragment byte-identical. A string with no `"://"` separator (relative path, malformed
 * input, empty string) is returned unchanged.
 *
 * Why this exists: FFmpeg (mpv's demuxer backend) resolves a protocol with a case-sensitive
 * `strcmp` against its registered names ("http", "https", "rtsp", ...), so a provider-supplied
 * `HTTP://host/...` URL fails with "Protocol not found" even though RFC 3986 §3.1 says schemes are
 * case-insensitive. OkHttp/[android.net.Uri] already lowercase the scheme internally, which is why
 * the exact same URL plays fine everywhere except mpv.
 */
fun normalizeUrlScheme(url: String): String {
    val end = url.indexOf("://")
    if (end <= 0) return url
    val scheme = url.substring(0, end)
    return if (scheme.any(Char::isUpperCase)) scheme.lowercase() + url.substring(end) else url
}
