package com.casshole.data.update

import org.json.JSONObject

/** A plain `major.minor.patch` version. Anything with a suffix (the local "0.0.0-dev") isn't one. */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    override fun toString() = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^v?(\d+)\.(\d+)\.(\d+)$""")

        fun parse(value: String): SemVer? {
            val match = PATTERN.matchEntire(value.trim()) ?: return null
            val (major, minor, patch) = match.destructured
            return SemVer(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}

data class ReleaseInfo(
    val version: SemVer,
    val apkName: String,
    val apkUrl: String,
    val apkSize: Long,
)

object ReleaseParser {
    /**
     * Parses GitHub's `GET /repos/{owner}/{repo}/releases/latest` response (which already
     * leaves drafts and pre-releases out). Null when the tag isn't a version or the release
     * has no APK attached - nothing to update to in either case.
     */
    fun parse(json: String): ReleaseInfo? {
        val release = JSONObject(json)
        val version = SemVer.parse(release.optString("tag_name")) ?: return null
        val assets = release.optJSONArray("assets") ?: return null
        val apks = (0 until assets.length())
            .map { assets.getJSONObject(it) }
            .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
        // The release workflow names it casshole-vX.Y.Z.apk; prefer that over any other APK.
        val apk = apks.firstOrNull { it.optString("name").startsWith("casshole-") }
            ?: apks.firstOrNull()
            ?: return null
        val url = apk.optString("browser_download_url").takeIf { it.startsWith("https://") }
            ?: return null
        return ReleaseInfo(
            version = version,
            apkName = apk.getString("name"),
            apkUrl = url,
            apkSize = apk.optLong("size", -1),
        )
    }

    /** The release worth offering, or null when the installed build is already as new (or newer). */
    fun newerThan(current: SemVer, release: ReleaseInfo?): ReleaseInfo? =
        release?.takeIf { it.version > current }
}
