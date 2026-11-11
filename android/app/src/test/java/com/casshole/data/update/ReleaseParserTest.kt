package com.casshole.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReleaseParserTest {

    private fun release(tag: String, vararg assets: Pair<String, String>) = """
        {
          "tag_name": "$tag",
          "assets": [
            ${assets.joinToString(",") { (name, url) -> """{"name": "$name", "size": 8787151, "browser_download_url": "$url"}""" }}
          ]
        }
    """.trimIndent()

    @Test
    fun `parses tag and the casshole apk`() {
        val info = ReleaseParser.parse(
            release(
                "v1.6.1",
                "checksums.txt" to "https://github.com/x/checksums.txt",
                "casshole-v1.6.1.apk" to "https://github.com/x/casshole-v1.6.1.apk",
            ),
        )!!
        assertEquals(SemVer(1, 6, 1), info.version)
        assertEquals("casshole-v1.6.1.apk", info.apkName)
        assertEquals("https://github.com/x/casshole-v1.6.1.apk", info.apkUrl)
        assertEquals(8787151L, info.apkSize)
    }

    @Test
    fun `release without apk or with a non version tag is ignored`() {
        assertNull(ReleaseParser.parse(release("v1.6.1", "notes.txt" to "https://github.com/x/notes.txt")))
        assertNull(ReleaseParser.parse(release("nightly", "casshole.apk" to "https://github.com/x/casshole.apk")))
    }

    @Test
    fun `plain http download url is rejected`() {
        assertNull(ReleaseParser.parse(release("v2.0.0", "casshole-v2.0.0.apk" to "http://github.com/x/a.apk")))
    }

    @Test
    fun `semver parsing rejects dev builds`() {
        assertEquals(SemVer(1, 10, 0), SemVer.parse("1.10.0"))
        assertEquals(SemVer(1, 10, 0), SemVer.parse("v1.10.0"))
        assertNull(SemVer.parse("0.0.0-dev"))
        assertNull(SemVer.parse("1.2"))
    }

    @Test
    fun `compares numerically not lexically`() {
        assertTrue(SemVer(1, 10, 0) > SemVer(1, 9, 9))
        assertTrue(SemVer(2, 0, 0) > SemVer(1, 99, 99))
    }

    @Test
    fun `offers only strictly newer releases`() {
        val info = ReleaseParser.parse(release("v1.7.0", "casshole-v1.7.0.apk" to "https://github.com/x/a.apk"))
        assertEquals(info, ReleaseParser.newerThan(SemVer(1, 6, 1), info))
        assertNull(ReleaseParser.newerThan(SemVer(1, 7, 0), info))
        assertNull(ReleaseParser.newerThan(SemVer(1, 8, 0), info))
        assertNull(ReleaseParser.newerThan(SemVer(1, 6, 1), null))
    }
}
